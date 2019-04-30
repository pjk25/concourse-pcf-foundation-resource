#!/usr/bin/env bash

set -euxo pipefail

clojure -m concourse-pcf-foundation-resource.cli "$@"
