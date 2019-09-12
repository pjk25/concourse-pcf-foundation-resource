#!/usr/bin/env bash

set -euxo pipefail

pushd foundation-lib
./scripts/test.sh
popd

pushd concourse-pcf-foundation-resource
./scripts/test.sh
popd

pushd incrementalizer
./scripts/test.sh
popd
