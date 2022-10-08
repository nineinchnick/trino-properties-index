#!/usr/bin/env bash

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
cd "$SCRIPT_DIR" || exit 1

log_pattern=$'^(.*)\t(.*)\t(.*)\t(.*)$'
bootstrap_pattern='(PROPERTY[[:space:]]+)(DEFAULT[[:space:]]+)(RUNTIME[[:space:]]+)DESCRIPTION.*'
propfile=properties.csv
echo >"$propfile" "version,name,default_value"
for logfile in ../logs/*.log; do
    echo "Processing $logfile"
    version=$(basename "$logfile" .log)
    property_length=0
    default_offset=0
    default_length=0
    runtime_offset=0
    runtime_length=0
    while read -r line; do
        if ! [[ $line =~ $log_pattern ]]; then
            continue
        fi
        #timestamp="${BASH_REMATCH[1]}"
        #level="${BASH_REMATCH[2]}"
        logger="${BASH_REMATCH[3]}"
        if [ "$logger" != "Bootstrap" ]; then
            continue
        fi
        message="${BASH_REMATCH[4]}"
        if [[ $message == PROPERTY* ]]; then
            if [[ $message =~ $bootstrap_pattern ]]; then
                property_length="${#BASH_REMATCH[1]}"
                default_offset=$property_length
                default_length="${#BASH_REMATCH[2]}"
                runtime_offset=$((default_offset + default_length))
                runtime_length="${#BASH_REMATCH[3]}"
                continue
            fi
        fi

        if [ "$property_length" -eq 0 ] || [ "${#message}" -lt "$runtime_offset" ]; then
            continue
        fi
        name="${message:0:property_length}"
        # remove trailing whitespace characters
        name="${name%"${name##*[![:space:]]}"}"

        if [ -z "$name" ]; then
            continue
        fi

        default_value="${message:default_offset:default_length}"
        default_value="${default_value%"${default_value##*[![:space:]]}"}"

        value=
        if [ "${#message}" -gt "$runtime_offset" ]; then
            value="${message:runtime_offset:runtime_length}"
            value="${value%"${value##*[![:space:]]}"}"
        fi

        # escape double quotes
        echo >>"$propfile" "$version,\"${name//\"/\\\"}\",\"${default_value//\"/\\\"}\""
    done <"$logfile"
done
