#!/usr/bin/env bash
#
set -euo pipefail

usage() {
    cat <<EOF >&2
Usage: $0 [-h] -s <SOURCE_VERSION> -t <TARGET_VERSION>
Extract Trino configuration properties

-h       Display help
-s       Source version
-t       Target version
EOF
}

SOURCE=372
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

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
cd "$SCRIPT_DIR" || exit 1

local_repo=$(mvn -B help:evaluate -Dexpression=settings.localRepository -q -DforceStdout)
cmd=(../target/trino-properties-index-*-executable.jar)
if [ "${#cmd[@]}" -eq 0 ]; then
    echo >&2 "ERROR: missing properties scanner executable, run mvn package"
    exit 2
elif [ "${#cmd[@]}" -gt 1 ]; then
    echo >&2 "ERROR: multiple properties scanner executable found, run mvn clean package"
    exit 2
fi

version=$SOURCE
artifact=io.trino:trino-server:${version}:tar.gz
while [ -z "$TARGET" ] || [ "$version" -le "$TARGET" ]; do
    if ! mvn -q -C dependency:get -Dtransitive=false "-Dartifact=$artifact"; then
        break
    fi
    properties="$version".csv
    if [ ! -f "$properties" ]; then
        trino_server="$local_repo/io/trino/trino-server/${version}/trino-server-${version}.tar.gz"
        ${cmd[0]} "$trino_server" >"$properties"
    fi
    ((version++))
    artifact=io.trino:trino-server:${version}:tar.gz
done

# combine all properties into one file
properties=properties.csv
echo >"$properties" "version,jar,plugin,config,description,is_deprecated"
for file in ???.csv; do
    tail -n +2 "$file" >>"$properties"
done
