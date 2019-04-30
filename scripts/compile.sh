#!/usr/bin/env bash

set -euxo pipefail

echo ${GRAALVM_HOME}

clojure -A:native-image