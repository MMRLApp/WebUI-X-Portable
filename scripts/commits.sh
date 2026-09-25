#!/bin/bash
set -euo pipefail

# Requires fzf for interactive, arrow-key-driven commit selection
if ! command -v fzf >/dev/null 2>&1; then
    echo "Error: fzf is required for interactive commit selection."
    echo "Install it (e.g. 'apt install fzf' or 'brew install fzf') and try again."
    exit 1
fi

# TO always points at the latest commit on the current branch
TO=$(git rev-parse --short HEAD)

# Let the user browse the log and pick the starting commit with arrow keys.
# The list is newest-first, so anything picked is automatically <= TO.
FROM=$(git log --pretty=format:'%h  %ad  %s' --date=format:'%Y-%m-%d %H:%M' \
    | fzf --prompt="Select starting commit (FROM, newest at top): " \
          --height=60% --reverse --no-multi \
    | awk '{print $1}')

if [ -z "$FROM" ]; then
    echo "No commit selected. Aborting."
    exit 1
fi

if ! git rev-parse --verify "${FROM}^{commit}" >/dev/null 2>&1; then
    echo "Error: starting commit '$FROM' does not exist"
    exit 1
fi

if ! git rev-parse --verify "${TO}^{commit}" >/dev/null 2>&1; then
    echo "Error: ending commit '$TO' does not exist"
    exit 1
fi

OUTPUT="build/commits-${FROM}-to-${TO}.txt"

git log --reverse "${FROM}^..${TO}" \
    --date=format:'%Y-%m-%d %H:%M' \
    --pretty=format:'[%ad] %h%n%s%n%b%n' \
    > "$OUTPUT"

echo "Written to $OUTPUT"