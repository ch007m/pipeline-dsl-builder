#!/usr/bin/env bash
set -e
mkdir -p ~/.ssh
if [ -e "/ssh/error" ]; then
  #no server could be provisioned
  cat /ssh/error
  exit 1
fi
export SSH_HOST=$(cat /ssh/host)
cp /ssh/id_rsa ~/.ssh

chmod 0400 ~/.ssh/id_rsa
export BUILD_DIR=$(cat /ssh/user-dir)
export SSH_ARGS="-o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=10"
echo "$BUILD_DIR"
ssh $SSH_ARGS "$SSH_HOST"  mkdir -p "$BUILD_DIR/workspaces" "$BUILD_DIR/scripts" "$BUILD_DIR/volumes"

## TO BE REVIEWED ==>
echo "Installing pack ..."
curl -sSL "https://github.com/buildpacks/pack/releases/download/$(params.PACK_CLI_VERSION)/pack-$(params.PACK_CLI_VERSION)-linux.tgz" | tar -C /usr/local/bin/ --no-same-owner -xzv pack

echo "Checking pack ..."
pack --version
pack config experimental true

#export DOCKER_HOST=tcp://$(params.DOCKER_HOST):2376
#echo "DOCKER_HOST=tcp://$(params.DOCKER_HOST):2376"

# We cannot get the array from the params PACK_CMD_FLAGS within the bash script as substitution don't work in this case !!
echo "Getting the arguments ..."
for cmd_arg in "$@"; do
  CLI_ARGS+=("$cmd_arg")
done

echo "Here are the arguments to be passed to the pack CLI"
for i in "$CLI_ARGS[@]"; do
  echo "arg: $i"
done

echo "Building the builder image ..."
echo "pack ${CLI_ARGS[@]}"
pack "${CLI_ARGS[@]}"

echo -n "URL of the image build is : quarkus-hello:1.0" | tee "$(results.IMAGE_URL.path)"
echo -n "sha256ddddddddddddddddddddd" | tee "$(results.IMAGE_DIGEST.path)"
echo -n "sha256eeeeeeeeeeeeeeeeeeeeee" | tee "$(results.BASE_IMAGES_DIGESTS.path)"