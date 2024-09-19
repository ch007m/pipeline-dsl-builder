#!/usr/bin/env bash
set -eu
set -o pipefail

echo "##########################################################################################"
echo "### Step 1 :: Configure SSH and rsync folders from tekton to the VM"
echo "##########################################################################################"
mkdir -p ~/.ssh
if [ -e "/ssh/error" ]; then
  #no server could be provisioned
  cat /ssh/error
exit 1
elif [ -e "/ssh/otp" ]; then
  curl --cacert /ssh/otp-ca -XPOST -d @/ssh/otp $(cat /ssh/otp-server) >~/.ssh/id_rsa
  echo "" >> ~/.ssh/id_rsa
else
  cp /ssh/id_rsa ~/.ssh
fi
chmod 0400 ~/.ssh/id_rsa

export SSH_HOST=$(cat /ssh/host)
export BUILD_DIR=$(cat /ssh/user-dir)
export SSH_ARGS="-o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=10"

echo "### Export different variables which are used within the script like args, repository to fetch, etc"
export REPOSITORY_TO_FETCH=${REPOSITORY_TO_FETCH}
export BUILD_ARGS="$@"

ssh $SSH_ARGS "$SSH_HOST" mkdir -p "$BUILD_DIR/workspaces" "$BUILD_DIR/scripts" "$BUILD_DIR/volumes"

echo "### rsync folders from pod to VM ..."
rsync -ra /var/workdir/ "$SSH_HOST:$BUILD_DIR/volumes/workdir/"
rsync -ra "/tekton/results/" "$SSH_HOST:$BUILD_DIR/results/"

echo "##########################################################################################"
echo "### Step 2 :: Create the bash script to be executed within the VM"
echo "##########################################################################################"
mkdir -p scripts
cat >scripts/script-build.sh <<'REMOTESSHEOF'
#!/bin/sh

TEMP_DIR="$HOME/tmp"
USER_BIN_DIR="$HOME/bin"
BUILDPACK_PROJECTS="$HOME/buildpack-repo"

mkdir -p ${TEMP_DIR}
mkdir -p ${USER_BIN_DIR}
mkdir -p ${BUILDPACK_PROJECTS}

export PATH=$PATH:${USER_BIN_DIR}

echo "### Podman info ###"
podman version

echo "### Start podman.socket ##"
systemctl --user start podman.socket
systemctl status podman.socket

echo "### Installing jq ..."
curl -sSL https://github.com/jqlang/jq/releases/download/jq-1.7.1/jq-linux-amd64 > ${USER_BIN_DIR}/jq
chmod +x ${USER_BIN_DIR}/jq

echo "### Install tomlq tool ..."
curl -sSL https://github.com/cryptaliagy/tomlq/releases/download/0.1.6/tomlq.amd64.tgz | tar -vxz tq
mv tq ${USER_BIN_DIR}/tq

echo "### Install syft"
curl -sSfL https://raw.githubusercontent.com/anchore/syft/main/install.sh | sh -s --
# Not needed as syft is already saved under bin/syft => mv bin/syft ${USER_BIN_DIR}/syft
syft --version

echo "### Install cosign"
curl -O -sL https://github.com/sigstore/cosign/releases/latest/download/cosign-linux-amd64
mv cosign-linux-amd64 ${USER_BIN_DIR}/cosign
chmod +x ${USER_BIN_DIR}/cosign
cosign version

echo "### Install pack ###"
curl -sSL "https://github.com/buildpacks/pack/releases/download/${PACK_CLI_VERSION}/pack-${PACK_CLI_VERSION}-linux.tgz" | tar -C ${TEMP_DIR} --no-same-owner -xzv pack
mv ${TEMP_DIR}/pack ${USER_BIN_DIR}

echo "### Pack version ###"
pack --version
pack config experimental true

echo "### Fetch the tarball of the buildpack project to build"
echo "### Git repo: ${REPOSITORY_TO_FETCH}"
curl -sSL "${REPOSITORY_TO_FETCH}/tarball/main" | tar -xz -C ${TEMP_DIR}
mv ${TEMP_DIR}/redhat-buildpacks-builder-ubi-base-* ${BUILDPACK_PROJECTS}/builder-ubi-base
cd ${BUILDPACK_PROJECTS}/builder-ubi-base

echo "### Build the builder image using pack"
for build_arg in "${BUILD_ARGS[@]}"; do
  PACK_ARGS+=" $build_arg"
done

echo "### Pack extra args: $PACK_ARGS"

echo "### Execute: pack builder create ..."
export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock
echo "pack builder create ${IMAGE} --config builder.toml ${PACK_ARGS}"
pack builder create ${IMAGE} --config builder.toml ${PACK_ARGS}

echo "### Export the image as OCI"
podman push "$IMAGE" "oci:konflux-final-image:$IMAGE"

echo "###########################################################"
echo "### Export: IMAGE_URL, IMAGE_DIGEST & BASE_IMAGES_DIGESTS under: $BUILD_DIR/volumes/workdir/"
echo "###########################################################"
echo -n "$IMAGE" > $BUILD_DIR/volumes/workdir/IMAGE_URL

BASE_IMAGE=$(tq -f builder.toml -o json 'stack' | jq -r '."build-image"')
podman inspect ${BASE_IMAGE} | jq -r '.[].Digest' > $BUILD_DIR/volumes/workdir/BASE_IMAGES_DIGESTS

echo "### Push the image produced and get its digest: $IMAGE"
podman push \
   --digestfile $BUILD_DIR/volumes/workdir/IMAGE_DIGEST \
   "$IMAGE"

echo "########################################"
echo "### Running syft on the image filesystem"
echo "########################################"
syft -v scan oci-dir:konflux-final-image -o cyclonedx-json > $BUILD_DIR/volumes/workdir/sbom-image.json

echo "### Show the content of the sbom file"
cat $BUILD_DIR/volumes/workdir/sbom-image.json # | jq -r '.'

{
  echo -n "${IMAGE}@"
  cat "$BUILD_DIR/volumes/workdir/IMAGE_DIGEST"
} > $BUILD_DIR/volumes/workdir/IMAGE_REF
echo "Image reference: $(cat $BUILD_DIR/volumes/workdir/IMAGE_REF)"

echo "########################################"
echo "### Add the SBOM to the image"
echo "########################################"
cosign attach sbom --sbom $BUILD_DIR/volumes/workdir/sbom-image.json --type cyclonedx $(cat $BUILD_DIR/volumes/workdir/IMAGE_REF)

REMOTESSHEOF
chmod +x scripts/script-build.sh

echo "##########################################################################################"
echo "### Step 3 :: Execute the bash script on the VM"
echo "##########################################################################################"
rsync -ra scripts "$SSH_HOST:$BUILD_DIR"
rsync -ra "$HOME/.docker/" "$SSH_HOST:$BUILD_DIR/.docker/"

ssh $SSH_ARGS "$SSH_HOST" \
  "REPOSITORY_TO_FETCH=${REPOSITORY_TO_FETCH} BUILDER_IMAGE=$BUILDER_IMAGE PLATFORM=$PLATFORM IMAGE=$IMAGE PACK_CLI_VERSION=$PACK_CLI_VERSION BUILD_ARGS=$BUILD_ARGS" BUILD_DIR=$BUILD_DIR \
   scripts/script-build.sh

echo "### rsync folders from VM to pod"
rsync -ra "$SSH_HOST:$BUILD_DIR/volumes/workdir/" "/var/workdir/"
rsync -ra "$SSH_HOST:$BUILD_DIR/results/"         "/tekton/results/"

echo "##########################################################################################"
echo "### Step 4 :: Export results to Tekton"
echo "##########################################################################################"

echo "### Export the tekton results"
echo "### IMAGE_URL: $(cat /var/workdir/IMAGE_URL)"
cat /var/workdir/IMAGE_URL > "$(results.IMAGE_URL.path)"

echo "### IMAGE_DIGEST: $(cat /var/workdir/IMAGE_DIGEST)"
cat /var/workdir/IMAGE_DIGEST > "$(results.IMAGE_DIGEST.path)"

echo "### IMAGE_REF: $(cat /var/workdir/IMAGE_REF)"
cat /var/workdir/IMAGE_REF > "$(results.IMAGE_REF.path)"

echo "### BASE_IMAGES_DIGESTS: $(cat /var/workdir/BASE_IMAGES_DIGESTS)"
cat /var/workdir/BASE_IMAGES_DIGESTS > "$(results.BASE_IMAGES_DIGESTS.path)"

SBOM_REPO="${IMAGE%:*}"
SBOM_DIGEST="$(sha256sum /var/workdir/sbom-image.json | cut -d' ' -f1)"
echo "### SBOM_BLOB_URL: ${SBOM_REPO}@sha256:${SBOM_DIGEST}"
echo -n "${SBOM_REPO}@sha256:${SBOM_DIGEST}" | tee "$(results.SBOM_BLOB_URL.path)"