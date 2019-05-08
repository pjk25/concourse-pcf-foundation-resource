#!/usr/bin/env bash

cd "$(dirname "$0")/.."

set -euxo pipefail

jq -n "{
    \"source\": {
        \"opsmgr\": {
            \"url\": \"${OM_TARGET}\",
            \"username\": \"${OM_USERNAME}\",
            \"password\": \"${OM_PASSWORD}\"
        }
    },
    \"version\": \"a-made-up-version\"
}" | ./scripts/run.sh check
