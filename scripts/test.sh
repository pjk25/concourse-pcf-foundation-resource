#!/usr/bin/env bash

set -euxo pipefail

clojure -Atest "$@"