#!/usr/bin/env bash

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
cd "$SCRIPT_DIR" || exit 1

# This is arbitrary - it is the first version that has a health probe
version=372
image=trinodb/trino:$version
while docker pull "$image"; do
    logfile="$version".log
    if [ ! -f "$logfile" ]; then
        container_id=$(docker run --rm -d "$image")
        # TODO this needs error handling, a container can fail to start because of lack of disk space and it's quite likely as we run a lot of different images here
        until docker inspect "$container_id" --format "{{json .State.Health.Status }}" | grep -q '"healthy"'; do sleep 1; done
        docker logs "$container_id" >"$logfile" 2>&1
        docker rm --force "$container_id"
    fi
    ((version++))
    image=trinodb/trino:$version
done
