#!/usr/bin/env bash

set -euxo pipefail

clj -A:user -C:test -r
