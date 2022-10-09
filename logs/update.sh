#!/usr/bin/env bash

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
cd "$SCRIPT_DIR" || exit 1

# Start depenedencies
# TODO ZK is not enough, we need a real Accumulo instance to register in it, use docker-compose?
#zk_container=$(docker run -d zookeeper)
#zk_ip=$(docker inspect -f '{{.NetworkSettings.IPAddress}}' "$zk_container")
zk_ip=host1.invalid
export zk_ip

# This is arbitrary - it is the first version that has a health probe
version=372
image=trinodb/trino:$version
while docker pull "$image"; do
    logfile="$version".log
    if [ ! -f "$logfile" ]; then
        # enable all supported connectors by creating one catalog for each connector plugin
        catalogs=$(mktemp -d)
        # list all plugins, excluding known non-connector plugins
        mapfile -t plugins < <(docker run --rm "$image" ls -1 /usr/lib/trino/plugin | grep -v 'exchange\|geospatial\|http-event-listener\|ml\|password-authenticators\|resource-group-managers\|session-property-managers\|teradata-functions')
        echo >&2 "Preparing catalogs for plugins: ${plugins[*]}"
        for plugin in "${plugins[@]}"; do
            catalog="../catalogs/$plugin.properties"
            if [ ! -f "$catalog" ]; then
                echo >&2 "WARNING: no catalog for $plugin"
                continue
            fi
            #shellcheck disable=SC2016
            envsubst '$zk_ip' <"$catalog" >"$catalogs/$plugin.properties"
        done
        cp ../catalogs/*.{json,txt} "$catalogs/"
        if ! container_id=$(docker run --rm -v "$catalogs:/etc/trino/catalog" -d "$image"); then
            echo >&2 "Failed to start a container using $image (and $catalogs): $container_id"
            exit 1
        fi
        while true; do
            if ! status=$(docker inspect "$container_id" --format "{{json .State.Health.Status }}"); then
                echo >&2 "Failed to get status for $Container_id: $status"
                exit 1
            fi
            if grep -q '"healthy"' <<<"$status"; then
                break
            fi
            sleep 1
        done
        docker logs "$container_id" >"$logfile" 2>&1
        docker rm --force "$container_id"
    fi
    ((version++))
    image=trinodb/trino:$version
done

#docker rm --force "$zk_container"
