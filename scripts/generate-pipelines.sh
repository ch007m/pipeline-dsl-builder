#!/usr/bin/env bash

CMD="java -jar target/quarkus-app/quarkus-run.jar builder"
OUTPUT_PATH=out/flows

rm -rf $OUTPUT_PATH
cfg_files=(configurations/**/*)

for file in "${cfg_files[@]}"; do
  $CMD -o $OUTPUT_PATH -c $file
  [ $? -eq 0 ]  || exit 1
done