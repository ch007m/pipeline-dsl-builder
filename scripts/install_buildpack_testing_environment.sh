#!/usr/bin/env bash
set -e

BP_DIR=test-buildpack
rm -rf $BP_DIR
mkdir -p $BP_DIR/tmp; cd $BP_DIR

repos=(
  https://github.com/paketo-community/builder-ubi-base.git
)

echo "Git clone the paketo repositories ..."
for repo in "${repos[@]}"
do
  git clone $repo
done

# INSTALL PACK

PACK_CLI_VERSION="v0.35.1"

echo "Installing pack ..."
set -x
curl -sSL "https://github.com/buildpacks/pack/releases/download/${PACK_CLI_VERSION}/pack-${PACK_CLI_VERSION}-linux.tgz" | tar -C ./tmp --no-same-owner -xzv pack
sudo mv tmp/pack /usr/local/bin

echo "Checking pack ..."
pack --version
pack config experimental true

# Build the ubi builder image
cd builder-ubi-base

export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock

pack builder create builder \
  --config \
  builder.toml

cd ..