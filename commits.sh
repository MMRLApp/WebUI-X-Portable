#!/bin/bash

REPO_URL="https://github.com/MMRLApp/WebUI-X-Portable"
MAX_CHARS=4000

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
    local line
    local total_len=0

    for line in "${commits[@]}"; do
        local formatted="- $line"
        local len=$(( ${#result} + ${#formatted} + 1 )) # +1 for newline
        if (( len > MAX_CHARS )); then
            break
        fi
        result+="$formatted"$'\n'
    done

    # Check if we have more commits than included
    local all_len=0
    for line in "${commits[@]}"; do
        all_len=$(( all_len + ${#line} + 3 )) # "- " + line + "\n"
    done

    if (( all_len > ${#result} )); then
        if [ -n "$latest_tag" ]; then
            local compare_link="\n[See all changes here](${REPO_URL}/compare/${latest_tag}...master)"
            if (( ${#result} + ${#compare_link} <= MAX_CHARS )); then
                result+="$compare_link"
            else
                result+=$'\n'"$compare_link"
            fi
        fi
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
