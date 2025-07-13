#!/bin/bash

REPO_URL="https://github.com/MMRLApp/WebUI-X-Portable"
MAX_CHARS="${1:-4000}"

get_latest_tag() {
    git describe --tags --abbrev=0 master 2>/dev/null
}

get_commits_since() {
    local tag="$1"
    local rev_range
    if [ -n "$tag" ]; then
        rev_range="$tag..master"
    else
        rev_range="master"
    fi

    # Get commit messages and filter out undesired ones
    git log --format=%s "$rev_range" | grep -v -e '^.$' \
        -e "^Merge branch 'master'" -e "^Merge pull request #"
}

format_markdown_list() {
    local commits=("$@")
    local result=""
    local total_len=0
    local max_len=$MAX_CHARS

    local compare_link=""
    if [ -n "$latest_tag" ]; then
        compare_link="[See all changes here](${REPO_URL}/compare/${latest_tag}...master)"
    fi

    # Reserve space for compare_link + newline (1 char)
    local reserved_len=$(( ${#compare_link} + 1 ))

    local line formatted len

    for line in "${commits[@]}"; do
        formatted="- $line"$'\n'
        len=${#formatted}

        if (( total_len + len + reserved_len > max_len )); then
            # No room to add this line without exceeding limit after adding compare link
            break
        fi

        result+="$formatted"
        total_len=$(( total_len + len ))
    done

    # Always add compare link (if present) after newline
    if [ -n "$compare_link" ]; then
        result+=$'\n'"$compare_link"
    fi

    printf "%s" "$result"
}

main() {
    latest_tag=$(get_latest_tag)
    mapfile -t commits < <(get_commits_since "$latest_tag")

    if [ ${#commits[@]} -eq 0 ]; then
        echo "No commits found since the latest tag."
        exit 0
    fi

    changelog_md=$(format_markdown_list "${commits[@]}")
    echo "$changelog_md"
}

main
