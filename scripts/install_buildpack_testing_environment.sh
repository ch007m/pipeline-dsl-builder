#!/usr/bin/env bash
set -e

OUTPUT_DIR="./tmp"
TOML_DIR="."
OCI_BUILD_DIR="./oci"
BP_DIR=test-buildpack

rm -rf $BP_DIR
mkdir -p $BP_DIR/tmp; cd $BP_DIR

function util::tools::os() {
  case "$(uname)" in
  "Darwin")
  echo "${1:-darwin}"
  ;;

  "Linux")
  echo "linux"
  ;;

  *)
  util::print::error "Unknown OS \"$(uname)\""
  exit 1
  esac
}

function util::tools::arch() {
  case "$(uname -m)" in
    arm64|aarch64)
      echo "arm64"
      ;;

    amd64|x86_64)
      if [[ "${1:-}" == "--blank-amd64" ]]; then
        echo ""
      elif [[ "${1:-}" == "--uname-format-amd64" ]]; then
        echo "x86_64"
      else
        echo "amd64"
      fi
      ;;

    *)
      util::print::error "Unknown Architecture \"$(uname -m)\""
      exit 1
  esac
}

curl_args=(
  "--fail"
  "--silent"
  "--location"
  "--output" "${OUTPUT_DIR}/jam"
)

os=$(util::tools::os)
arch=$(util::tools::arch)

echo "Git clone the paketo repositories ..."
repos=(
  https://github.com/paketo-community/builder-ubi-base.git
  https://github.com/paketo-community/ubi-base-stack.git
)

for repo in "${repos[@]}"
do
  git clone $repo
done

# Install Jam CLI
JAM_VERSION="v2.7.3"
echo "Installing jam: ${JAM_VERSION}"

curl "https://github.com/paketo-buildpacks/jam/releases/download/${JAM_VERSION}/jam-${os}-${arch}" \
  "${curl_args[@]}"
chmod +x ${OUTPUT_DIR}/jam; sudo mv ${OUTPUT_DIR}/jam /usr/local/bin
jam version

# Install Pack CLI
PACK_CLI_VERSION="v0.35.1"

echo "Installing pack ..."
set -x
curl -sSL "https://github.com/buildpacks/pack/releases/download/${PACK_CLI_VERSION}/pack-${PACK_CLI_VERSION}-linux.tgz" | tar -C ./tmp --no-same-owner -xzv pack
sudo mv tmp/pack /usr/local/bin

echo "Checking pack ..."
pack --version
pack config experimental true

echo "Test case: Build the ubi builder image using pack"
cd builder-ubi-base
export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock
echo "pack builder create builder \
        --config \
        builder.toml"
pack builder create builder \
  --config \
  ${TOML_DIR}/builder.toml
cd ..

echo "Test case: Build the ubi base stack image"
cd ubi-base-stack
args=(
  --config "${TOML_DIR}/stack.toml"
  --build-output "${OCI_BUILD_DIR}/build.oci"
  --run-output "${OCI_BUILD_DIR}/run.oci"
)
echo "jam create-stack \"${args[@]}\""
jam create-stack "${args[@]}"
cd ..

