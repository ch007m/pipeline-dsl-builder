#!/usr/bin/env bash

CMD="java -jar target/quarkus-app/quarkus-run.jar"
OUTPUT_PATH=out/flows

rm -rf $OUTPUT_PATH

for file in configurations/**/*; do
  $CMD -o $OUTPUT_PATH -c $file
done