#!/usr/bin/env bash

set -e

echo "Copy pack config.toml to $(workspaces.pack-workspace.path)"
cp $(workspaces.data-store.path)/config.toml $(workspaces.pack-workspace.path)

echo "Copy .dockerconfigjson to $(workspaces.pack-workspace.path)/.docker/config.json"
mkdir -p $(workspaces.pack-workspace.path)/.docker
cp $(workspaces.data-store.path)/.dockerconfigjson $(workspaces.pack-workspace.path)/.docker/config.json