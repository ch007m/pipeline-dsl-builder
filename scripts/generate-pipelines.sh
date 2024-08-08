#!/usr/bin/env bash

CMD="java -jar target/quarkus-app/quarkus-run.jar"
OUTPUT_PATH=out/flows

rm -rf $OUTPUT_PATH
cfg_files=(configurations/**/*)

for file in "${cfg_files[@]}"; do
  $CMD -o $OUTPUT_PATH -c $file
done

printf '%s\n\n' "# Scenario" > TEMP.md

for f in "${cfg_files[@]}"
do
  type=$(cat "$f" | yq -r ".type")
  domain=$(cat "$f" | yq -r ".domain")
  title=$(cat "$f" | yq -r ".job.description")
  fileName=$(cat "$f" | yq -r ".job.name")
  resourceType=$(cat "$f" | yq -r ".job.resourceType")

  printf '%s\n\n' "## Provider: $type" >> TEMP.md
  printf '%s\n\n' "### $title" >> TEMP.md
  printf '%s\n' "Command to be executed: " >> TEMP.md
  printf '%s\n' "\`\`\`bash" >> TEMP.md
  printf '%s\n' "$CMD -o out/flows -c $f" >> TEMP.md
  printf '%s\n' "\`\`\`"  >> TEMP.md

  printf '%s\n' "using as configuration: " >> TEMP.md
  printf '%s\n' "\`\`\`yaml" >> TEMP.md
  printf '%s\n' "# $f" >> TEMP.md
  printf '%s\n' "\`\`\`"  >> TEMP.md

  printf '%s\n' "Generated file: " >> TEMP.md
  printf '%s\n' "\`\`\`yaml" >> TEMP.md
  generate_path="generated/$type/$domain/$(echo $resourceType | awk '{print tolower($0)}')-$fileName.yaml"
  printf '%s\n' "# $generate_path" >> TEMP.md
  printf '%s\n' "\`\`\`"  >> TEMP.md
done

