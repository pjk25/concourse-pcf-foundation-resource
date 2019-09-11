#!/usr/bin/env bash

cd "$(dirname "$0")/.."

set -euxo pipefail

clojure -m concourse-pcf-foundation-resource.cli "$@"
