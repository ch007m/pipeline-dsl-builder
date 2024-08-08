#!/usr/bin/env bash

CMD="java -jar target/quarkus-app/quarkus-run.jar"
OUTPUT_PATH=out/flows

rm -rf $OUTPUT_PATH
cfg_files=(configurations/**/*)

# for file in "${cfg_files[@]}"; do
#   $CMD -o $OUTPUT_PATH -c $file
# done

fence="\`\`\`"
printf '%s\n\n' "# Scenario" > SCENARIO.md

for f in "${cfg_files[@]}"
do
  type=$(cat "$f" | yq -r ".type")
  domain=$(cat "$f" | yq -r ".domain")
  title=$(cat "$f" | yq -r ".job.description")
  printf '%s\n\n' "## Provider: $type" >> SCENARIO.md
  printf '%s\n\n' "### $title" >> SCENARIO.md
  printf '%s\n' "Command to be executed: " >> SCENARIO.md
  printf '%s\n' "$fence bash" >> SCENARIO.md
  printf '%s\n' "$CMD -o out/flows -c $f" >> SCENARIO.md
  printf '%s\n' "$fence"  >> SCENARIO.md

  printf '%s\n' "using as configuration: " >> SCENARIO.md
  printf '%s\n' "$fence yaml" >> SCENARIO.md
  printf '%s\n' "# $f" >> SCENARIO.md
  printf '%s\n' "$fence"  >> SCENARIO.md

  printf '%s\n' "Generated file: " >> SCENARIO.md
  printf '%s\n' "$fence yaml" >> SCENARIO.md
  generate_path="generated/$type/$domain/$(basename $f)"
  printf '%s\n' "# $generate_path" >> SCENARIO.md
  printf '%s\n' "$fence"  >> SCENARIO.md
done

