#!/usr/bin/env bash

# substitute params in a query and execute it
# shellcheck disable=SC2016
env source=372 target=399 envsubst '$source $target' <sql/changes-per-version.sql | sqlite3 --line
