#!/usr/bin/env bash

# Usage
# ./scripts/generate-pipelines.sh
# ./scripts/generate-pipelines.sh configurations/tekton/basic1-cfg.yaml

CMD="java -jar target/quarkus-app/quarkus-run.jar builder"
OUTPUT_PATH=out/flows

rm -rf $OUTPUT_PATH
cfg_files=(configurations/**/*)

# Check if a parameter (file name) is passed
if [ $# -gt 0 ]; then
  file="$1"
  $CMD -o $OUTPUT_PATH -c $file
  exit
else
  for file in "${cfg_files[@]}"; do
    $CMD -o $OUTPUT_PATH -c $file
    [ $? -eq 0 ]  || exit 1
  done
fi