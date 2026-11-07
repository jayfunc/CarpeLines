#!/usr/bin/env bash
# Generates release notes from conventional commits between the previous release of the same
# channel and HEAD. Usage: generate-changelog.sh <channel> <tag> <output-file>
set -euo pipefail

channel=${1:?channel}
tag=${2:?tag}
out=${3:?output file}

repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"
repo_url="${GITHUB_SERVER_URL:-https://github.com}/${GITHUB_REPOSITORY:-jayfunc/CarpeLines}"

# Previous published tag of this channel among HEAD's ancestors. New-format tags look like
# v1.0.0-beta.20261106.1357 (older ones use the commit count, e.g. v1.0.0-beta.57-3f9a1c2);
# legacy tags are v0.5.1 and v0.3.0-alpha.16.
prev=""
while IFS= read -r candidate; do
  [ "$candidate" = "$tag" ] && continue
  subject=${candidate#v}
  case "$channel" in
    stable)
      if [[ "$subject" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] ||
         [[ "$subject" =~ ^[0-9]+\.[0-9]+\.[0-9]+-stable\. ]]; then
        prev=$candidate
        break
      fi
      ;;
    beta)
      if [[ "$subject" =~ ^[0-9]+\.[0-9]+\.[0-9]+-beta\. ]]; then
        prev=$candidate
        break
      fi
      ;;
    alpha)
      if [[ "$subject" =~ ^[0-9]+\.[0-9]+\.[0-9]+-alpha\. ]]; then
        prev=$candidate
        break
      fi
      ;;
  esac
done < <(git tag --merged HEAD --sort=-creatordate)

# First build of a channel: fall back to the newest published release of any channel so the
# notes do not start with the entire repository history.
if [ -z "$prev" ]; then
  prev=$(git describe --tags --abbrev=0 HEAD^ 2>/dev/null || true)
fi

if [ -n "$prev" ]; then
  range="$prev..HEAD"
  compare="$repo_url/compare/$prev...$tag"
else
  range="HEAD"
  compare=""
fi

declare -A titles=(
  [feat]="Features"
  [fix]="Fixes"
  [perf]="Performance"
  [refactor]="Refactoring"
  [docs]="Documentation"
  [build]="Build"
  [ci]="CI"
  [test]="Tests"
  [revert]="Reverts"
  [chore]="Chores"
  [other]="Other changes"
)
order=(feat fix perf refactor docs build ci test revert chore other)
declare -A lines
conventional='^([a-z]+)(\(([^)]+)\))?!?: (.*)$'

while IFS=$'\x1f' read -r short subject; do
  [ -z "$subject" ] && continue
  type=other
  scope=""
  description=$subject
  if [[ "$subject" =~ $conventional ]]; then
    case "${BASH_REMATCH[1]}" in
      feat|feature) type=feat ;;
      fix|bugfix) type=fix ;;
      perf) type=perf ;;
      refactor) type=refactor ;;
      docs) type=docs ;;
      build) type=build ;;
      ci) type=ci ;;
      test|tests) type=test ;;
      revert) type=revert ;;
      chore|style) type=chore ;;
    esac
    scope=${BASH_REMATCH[3]}
    description=${BASH_REMATCH[4]}
  fi
  if [ -n "$scope" ]; then
    description="**$scope:** $description"
  fi
  lines[$type]="${lines[$type]:-}- $description ([\`$short\`]($repo_url/commit/$short))"$'\n'
done < <(git log "$range" --no-merges --abbrev=7 --pretty=format:'%h%x1f%s%n')

{
  echo "## What's changed"
  echo
  wrote=0
  for type in "${order[@]}"; do
    if [ -n "${lines[$type]:-}" ]; then
      wrote=1
      echo "### ${titles[$type]}"
      printf '%s' "${lines[$type]}"
      echo
    fi
  done
  if [ "$wrote" -eq 0 ]; then
    echo "No user-facing commits in this build."
    echo
  fi
  if [ -n "$compare" ]; then
    echo "**Full changelog:** [$prev...$tag]($compare)"
  fi
} >> "$out"
