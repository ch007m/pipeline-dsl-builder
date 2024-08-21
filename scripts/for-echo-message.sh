#!/usr/bin/env bash

members=("$(params.members)")

for member in "${members[@]}"; do
  echo "Say hello to: $member"
done