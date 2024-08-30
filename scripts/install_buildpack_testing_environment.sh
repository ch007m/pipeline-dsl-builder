#!/usr/bin/env bash
set -euo pipefail

######################
## Functions ##
######################

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

function print::colored_msg() {
    local color="$1"
    local message="$2"
    local reset='\033[0m'

    # Calculate the length of the message plus the extra decorations
    local message_length=${#message}
    local border_length=$((message_length + 6))

    # Create the top and bottom border
    local border=$(printf "%${border_length}s" | tr ' ' '#')

    # Display the message with the color and borders
    echo -e "${color}${border}"
    echo -e "${color}## ${message} ##"
    echo -e "${border}${reset}"
}

function print::error() {
    local msg="$1"
    local RED='\033[0;31m'
    echo -e "${RED}Error: ${msg}${RESET}" >&2
}
######################
## Variables ##
######################
# Define colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
RESET='\033[0m' # Reset color to default

BINARY_DIR="./tmp"
BUILDPACK_DIR="buildpack"
SOURCE_PATH="."
BUILDPACK_TEST_DIR="test-buildpack"
## TODO: To be defined as parameter
PACKAGE="quay.io/ch007m"

print::colored_msg "${GREEN}" "Clean up folders"
rm -rf ${BUILDPACK_TEST_DIR}
rm -rf $HOME/bin
rm -rf ${BINARY_DIR}
rm -rf ${BUILDPACK_DIR}

mkdir -p ${BUILDPACK_TEST_DIR}/${BINARY_DIR}
mkdir -p ${BINARY_DIR}
mkdir -p $HOME/bin
mkdir -p ${BUILDPACK_DIR}

cd ${BUILDPACK_TEST_DIR}

os=$(util::tools::os)
arch=$(util::tools::arch)

print::colored_msg "${GREEN}" "Git clone the paketo repositories ..."
repos=(
  https://github.com/paketo-community/builder-ubi-base.git
  https://github.com/paketo-community/builder-ubi-buildpackless-base.git
  https://github.com/paketo-community/ubi-base-stack.git
  https://github.com/paketo-buildpacks/java.git
  https://github.com/paketo-buildpacks/quarkus.git
)

for repo in "${repos[@]}"
do
  git clone $repo
done

# Install Jam CLI
JAM_VERSION="v2.7.3"
print::colored_msg "${GREEN}" "Installing jam: ${JAM_VERSION}"

curl_args=(
  "--fail"
  "--silent"
  "--location"
  "--output" "${BINARY_DIR}/jam"
)
curl "https://github.com/paketo-buildpacks/jam/releases/download/${JAM_VERSION}/jam-${os}-${arch}" \
  "${curl_args[@]}"
chmod +x ${BINARY_DIR}/jam; sudo mv ${BINARY_DIR}/jam /usr/local/bin
jam version

# Install Pack CLI
PACK_CLI_VERSION="v0.35.1"

print::colored_msg "${GREEN}" "Installing pack: ${PACK_CLI_VERSION}"
curl -sSL "https://github.com/buildpacks/pack/releases/download/${PACK_CLI_VERSION}/pack-${PACK_CLI_VERSION}-linux.tgz" | tar -C ${BINARY_DIR} --no-same-owner -xzv pack
sudo mv ${BINARY_DIR}/pack /usr/local/bin

echo "Checking pack ..."
pack --version
pack config experimental true

print::colored_msg "${GREEN}" "Installing go framework."
curl -sSL "https://go.dev/dl/go1.23.0.linux-amd64.tar.gz" | tar -C ${BINARY_DIR} -xzv go
sudo chown -R $USER:$(id -g -n) ${BINARY_DIR}/go
mkdir -p $HOME/bin/go
mv ${BINARY_DIR}/go $HOME/bin
chmod +x $HOME/bin/go

mkdir -p $HOME/workspace
export GOPATH=$HOME/workspace
export GOROOT=$HOME/bin/go
export PATH=$PATH:$GOROOT/bin:$GOPATH/bin
go version

print::colored_msg "${GREEN}" "Installing libpak/create-package."
go install -ldflags="-s -w" github.com/paketo-buildpacks/libpak/cmd/create-package@latest

print::colored_msg "${CYAN}" "Test case:: Build the ubi builder image using pack. "
cd builder-ubi-base
export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock

pack builder create builder \
  --config \
  ${SOURCE_PATH}/builder.toml
cd ..

print::colored_msg "${CYAN}" "Test case:: Build the ubi buildpackless builder image using pack. "
cd builder-ubi-buildpackless-base
export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock

pack builder create builder \
  --config \
  ${SOURCE_PATH}/builder.toml
cd ..

print::colored_msg "${CYAN}" "Test case:: Build the ubi base stack image. "
cd ubi-base-stack

export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock

cat ${SOURCE_PATH}/images.json | jq -c '.images[]' | while read -r image; do
  NAME=$(echo "$image" | jq -r '.name')
  CONFIG_DIR=$(echo "$image" | jq -r '.config_dir')
  OUTPUT_DIR=$(echo "$image" | jq -r '.output_dir')
  BUILD_IMAGE=$(echo "$image" | jq -r '.build_image')
  RUN_IMAGE=$(echo "$image" | jq -r '.run_image')

  build_receipt_filename=$(echo "$image" | jq -r '.build_receipt_filename')
  run_receipt_filename=$(echo "$image" | jq -r '.run_receipt_filename')

  echo "Name: ${NAME}"
  echo "Config Dir: ${CONFIG_DIR}"
  echo "Output Dir: ${OUTPUT_DIR}"

  echo "Build Image: ${BUILD_IMAGE}"
  echo "Run Image: ${RUN_IMAGE}"

  echo "Build Receipt Filename: $build_receipt_filename"
  echo "Run Receipt Filename: $run_receipt_filename"
  echo "----"

  STACK_DIR=${SOURCE_PATH}/${CONFIG_DIR}
  mkdir -p "${STACK_DIR}/${OUTPUT_DIR}"

  args=(
    --config "${STACK_DIR}/stack.toml"
    --build-output "${STACK_DIR}/${OUTPUT_DIR}/build.oci"
    --run-output "${STACK_DIR}/${OUTPUT_DIR}/run.oci"
  )
  echo "jam create-stack \"${args[@]}\""
  jam create-stack "${args[@]}" || echo "The command failed !!!!"
  trap 'print::error "Jam command failed with exit code $? at line ${LINENO}"' ERR
done
cd ..

print::colored_msg "${CYAN}" "Test case:: Build the java meta/composite buildpack image. "
cd java

export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock

# TODO: Grab the version from git tag/ref/etc and pass OS as env var
VERSION="v0.1.0"
OS="linux"

COMPILED_BUILDPACK="${HOME}/buildpack"

create-package \
   --source "${SOURCE_PATH}" \
   --destination "${COMPILED_BUILDPACK}" \
   --version "${VERSION}"

PACKAGE_FILE="${SOURCE_PATH}/package.toml"
if [ -f "${PACKAGE_FILE}" ]; then
  cp "${PACKAGE_FILE}" "${COMPILED_BUILDPACK}/package.toml"
  printf '[buildpack]\nuri = "%s"\n\n[platform]\nos = "%s"\n' "${COMPILED_BUILDPACK}" "${OS}" >> "${COMPILED_BUILDPACK}/package.toml"
fi

# create-package puts the buildpack here, we need to run from that directory
cd "${COMPILED_BUILDPACK}"

print::colored_msg "${CYAN}" "Show buildpack.toml content for java buildpack"
cat buildpack.toml

print::colored_msg "${CYAN}" "Show buildpack.toml content for java buildpack"
cat package.toml

export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock

pack -v buildpack package \
  "${PACKAGE}:${VERSION}" \
  --config "${COMPILED_BUILDPACK}/package.toml" # --publish
cd ..

print::colored_msg "${CYAN}" "Test case:: Build the Quarkus Buildpack image."
cd cd ${BUILDPACK_TEST_DIR}/quarkus

# TODO: Grab the version from git tag/ref/etc and pass OS as env var
VERSION="v0.1.0"
OS="linux"

COMPILED_BUILDPACK="${HOME}/buildpack"

create-package \
   --source "${SOURCE_PATH}" \
   --destination "${COMPILED_BUILDPACK}" \
   --version "${VERSION}"

cd "${COMPILED_BUILDPACK}"

print::colored_msg "${CYAN}" "Show buildpack.toml content for Quarkus buildpack"
cat buildpack.toml

export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock
pack -v buildpack package \
  "${PACKAGE}:${VERSION}" \
  --config "${COMPILED_BUILDPACK}/package.toml" # --publish




