#!/usr/bin/env bash
set -e

BP_DIR=test-buildpack
rm -rf $BP_DIR
mkdir -p $BP_DIR; cd $BP_DIR

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
curl -sSL "https://github.com/buildpacks/pack/releases/download/${PACK_CLI_VERSION}/pack-${PACK_CLI_VERSION}-linux.tgz" | tar -C /usr/local/bin/ --no-same-owner -xzv pack

echo "Checking pack ..."
pack --version
pack config experimental true

# Build the ubi builder image
cd builder-ubi-base
pack builder create builder \
  --config \
  builder.toml

cd ..