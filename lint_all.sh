#!/usr/bin/env bash

set -euxo pipefail

pushd foundation-lib
./scripts/lint.sh
popd

pushd concourse-pcf-foundation-resource
./scripts/lint.sh
popd

pushd incrementalizer
./scripts/lint.sh
popd
