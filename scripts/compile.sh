#!/usr/bin/env bash

cd "$(dirname "$0")/.."

set -euxo pipefail

mkdir -p classes

clojure -J-Dclojure.compiler.direct-linking=true \
  -e "(compile 'concourse-pcf-foundation-resource.cli)"

clojure -A:uberjar

zip -d target/concourse-pcf-foundation-resource.jar "*.clj"
