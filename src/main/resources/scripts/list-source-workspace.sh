#!/usr/bin/env bash

set -e

echo "List files under: $(workspaces.pack-workspace.path)"
ls -la $(workspaces.pack-workspace.path)/

ls -la $(workspaces.pack-workspace.path)/.docker/
cat $(workspaces.pack-workspace.path)/.docker/config.json

echo "List files under: $(workspaces.source-dir.path)"
ls -la $(workspaces.source-dir.path)/