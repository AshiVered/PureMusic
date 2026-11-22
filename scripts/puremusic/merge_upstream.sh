#!/usr/bin/env bash
# Merge Metrolist's main into the current branch, keeping PureMusic's changes.
#
# Usage: merge_upstream.sh <upstream-ref>
#
# Conflicts PureMusic always resolves the same way are handled here:
#   - .github/workflows: Metrolist's workflows are never kept (only PureMusic's own run in this repo).
#   - .github/FUNDING.yml, README.md: PureMusic's version wins.
# Any other conflict stops the merge (exit 2) and lists the files in conflicts.txt,
# because guessing could silently drop a PureMusic restriction.
#
# Exit codes: 0 merged (commit created), 1 nothing to merge, 2 unresolved conflicts.
set -euo pipefail

UPSTREAM_REF="${1:?upstream ref required}"
OWN_WORKFLOWS_REGEX='^\.github/workflows/(upstream-sync\.yml)$'

if [ "$(git rev-list --count "HEAD..$UPSTREAM_REF")" = "0" ]; then
  echo "Already up to date with $UPSTREAM_REF"
  exit 1
fi

rm -f conflicts.txt
git merge --no-ff --no-commit "$UPSTREAM_REF" -m "Merge Metrolist $(git rev-parse --short "$UPSTREAM_REF")" || true

# Files the merge left unresolved.
mapfile -t unresolved < <(git diff --name-only --diff-filter=U)

remaining=()
for f in "${unresolved[@]}"; do
  case "$f" in
    .github/workflows/*)
      if [[ "$f" =~ $OWN_WORKFLOWS_REGEX ]]; then
        git checkout --ours -- "$f" && git add -- "$f"
      else
        git rm -q --cached -- "$f" 2>/dev/null || true
        rm -f -- "$f"
      fi
      ;;
    .github/FUNDING.yml)
      git rm -q --cached -- "$f" 2>/dev/null || true
      rm -f -- "$f"
      ;;
    README.md)
      git checkout --ours -- "$f" && git add -- "$f"
      ;;
    *)
      remaining+=("$f")
      ;;
  esac
done

# Workflows and funding that Metrolist added without a conflict are removed as well.
while IFS= read -r f; do
  [ -z "$f" ] && continue
  if [[ ! "$f" =~ $OWN_WORKFLOWS_REGEX ]]; then
    git rm -q -f -- "$f"
  fi
done < <(git ls-files .github/workflows)
if git ls-files --error-unmatch .github/FUNDING.yml >/dev/null 2>&1; then
  git rm -q -f -- .github/FUNDING.yml
fi

if [ "${#remaining[@]}" -gt 0 ]; then
  printf '%s\n' "${remaining[@]}" > conflicts.txt
  echo "Unresolved conflicts:"
  cat conflicts.txt
  git merge --abort
  exit 2
fi

git commit -q --no-edit
echo "Merged $UPSTREAM_REF"
