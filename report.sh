#!/usr/bin/env bash

set -euo pipefail

usage() {
    cat <<EOF >&2
Usage: $0 [-h] -s <SOURCE_VERSION> -t <TARGET_VERSION>
List changes in Trino properties when upgrading from one version to another

-h       Display help
-s       Source version
-t       Target version
EOF
}

SOURCE=
TARGET=

while getopts ":s:t:h" OPTKEY; do
    case "${OPTKEY}" in
        s)
            SOURCE="$OPTARG"
            ;;
        t)
            TARGET="$OPTARG"
            ;;
        h)
            usage
            exit 0
            ;;
        '?')
            echo >&2 "ERROR: INVALID OPTION -- ${OPTARG}"
            usage
            exit 1
            ;;

        ':')
            echo >&2 "MISSING ARGUMENT for option -- ${OPTARG}"
            usage
            exit 1
            ;;
        *)
            echo >&2 "ERROR: UNKNOWN OPTION -- ${OPTARG}"
            usage
            exit 1
            ;;
    esac
done
shift $((OPTIND - 1))
[[ ${1:-} == "--" ]] && shift

if [ -z "$SOURCE" ]; then
    echo >&2 "ERROR: Option '-s <SOURCE_VERSION>' is required."
    usage
    exit 1
fi

if [ -z "$TARGET" ]; then
    echo >&2 "ERROR: Option '-t <TARGET_VERSION>' is required."
    usage
    exit 1
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
cd "$SCRIPT_DIR" || exit 1

# substitute params in a query and execute it
export SOURCE TARGET
echo "Changes per version:"
# shellcheck disable=SC2016
envsubst '$SOURCE $TARGET' <sql/changes-per-version.sql | sqlite3 --line 2>&1 | grep -v 'unescaped " character'
echo ""

echo "Changes per status:"
# shellcheck disable=SC2016
envsubst '$SOURCE $TARGET' <sql/changes-per-status.sql | sqlite3 --line 2>&1 | grep -v 'unescaped " character'
echo ""
