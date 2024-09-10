#!/usr/bin/env bash
# set -o verbose
set -eu
set -o pipefail
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
mkdir -p scripts

echo "### List files under /ssh"
ls -la /ssh

echo "$BUILD_DIR"
ssh $SSH_ARGS "$SSH_HOST"  mkdir -p "$BUILD_DIR/workspaces" "$BUILD_DIR/scripts" "$BUILD_DIR/volumes"

cat >scripts/script-build.sh <<'REMOTESSHEOF'
#!/bin/sh
TEMP_DIR="$HOME/tmp"
USER_BIN_DIR="$HOME/bin"
BUILDPACK_PROJECTS="$HOME/buildpack-repo"

PACK_CLI_VERSION="v0.35.1"
GO_VERSION="1.23.0"

mkdir -p ${TEMP_DIR}
mkdir -p ${USER_BIN_DIR}
mkdir -p ${BUILDPACK_PROJECTS}

export PATH=$PATH:${USER_BIN_DIR}

curl -sSL "https://github.com/buildpacks/pack/releases/download/${PACK_CLI_VERSION}/pack-${PACK_CLI_VERSION}-linux.tgz" | tar -C ${TEMP_DIR} --no-same-owner -xzv pack
mv ${TEMP_DIR}/pack ${USER_BIN_DIR}

echo "### Pack version ###"
pack --version
pack config experimental true

echo "### Podman version ###"
podman version
podman info

echo "## Status of the service ##"
systemctl status podman.socket
systemctl --user start podman.socket
systemctl status podman.socket
ls -la $XDG_RUNTIME_DIR/podman

echo "### Go version ###"
curl -sSL "https://go.dev/dl/go${GO_VERSION}.linux-amd64.tar.gz" | tar -C ${TEMP_DIR} -xz go
mkdir -p ${USER_BIN_DIR}/go
mv ${TEMP_DIR}/go ${USER_BIN_DIR}
chmod +x ${USER_BIN_DIR}/go

mkdir -p $HOME/workspace
export GOPATH=$HOME/workspace
export GOROOT=${USER_BIN_DIR}/go
export PATH=$PATH:$GOROOT/bin:$GOPATH/bin
go version

# echo "### Git version ###"
# sudo yum install git => NOT ALLOWED
# yum install git => This command has to be run with superuser privileges
# git version

echo "### Build the builder image using pack"
curl -sSL https://github.com/paketo-community/builder-ubi-base/tarball/main | tar -xz -C ${TEMP_DIR}
mv ${TEMP_DIR}/paketo-community-builder-ubi-base-* ${BUILDPACK_PROJECTS}/builder-ubi-base
cd ${BUILDPACK_PROJECTS}/builder-ubi-base
ls

export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock
pack builder create builder --config builder.toml -vv

REMOTESSHEOF
chmod +x scripts/script-build.sh
ssh $SSH_ARGS "$SSH_HOST" "bash -s" <scripts/script-build.sh

echo -n "URL of the image build is : quarkus-hello:1.0" | tee "$(results.IMAGE_URL.path)"
echo -n "sha256ddddddddddddddddddddd" | tee "$(results.IMAGE_DIGEST.path)"
echo -n "sha256eeeeeeeeeeeeeeeeeeeeee" | tee "$(results.BASE_IMAGES_DIGESTS.path)"