# concourse-pcf-foundation-resource

Models the deployed configuration of a PCF Foundation, via OpsMan.
Leverages the om cli to do so.

Can be used with https://github.com/pjk25/incrementalizer to apply foundations in a control loop that changes the smallest number of tiles in a given deploy, subject to constraints (i.e. Pivotal Application Service cannot be deployed before the director tile) such as:

```clojure
[{:selector {:product-name "cf"
             :version "2.6.0"}
  :requires [{:product-name "p-bosh"
              :version "2.6.0"}]}
 {:selector {:product-name "apmPostgres"
             :version "1.6.1"}
  :requires [{:product-name "cf"
              :version "2.3.0"}]}]
```

## About versions

If there are any changes being applied, or there are saved changes not yet applied, the version is indeterminate, and check will fail. This is because we cannot retrieve deployed product config via the OpsMan API; therefore staged product config (that we can retrieve) only represents what is deployed under the aforementioned conditions.

Additionally, while it may be feasible to cache past versions in the resource container, since historic configs are not available through the OpsMan API, this resource only reports the current version.

Foundation configurations are described by the [specs](https://clojure.org/guides/spec) in https://github.com/pjk25/foundation-lib

## Source Configuration
```yaml
opsmgr:
    url: pcf.staging.company.com
    username: admin
    password: the-admin-password
    skip_ssl_validation: true
```

`skip_ssl_validation` defaults to `false`.

## Behavior

### check

hash of the opsman configuration manfiest

versions look like this:

```json
{
    "version": {
        "hash": "abcd1234"
    },
    "metadata": []
}
```


### in

- configuration.yml

Example configuration.yml:

```yaml
---
opsman-version: 2.5.0
director-config:
    properties-configuration:
    director_configuration:
      allow_legacy_agents: true
      blobstore_type: local
products:
- product-name: cf
  version: 2.5.0
  stemcells:
  - version: "250.48"
    os: ubuntu-xenial
  product-properties:
    .cloud_controller.apps_domain:
      value: apps.my-foundation.com
```

### out

params:

```yaml
file: path-to-foundation-config-yaml`
dry_run: true
```

`dry_run` defaults to `false`.

This only makes changes if what's there differs, things not specified are assumed floating.

Additionally, the configuration that is `put` contains slightly more information than what is `get`, namely the source for a tile and stemcell, in the event they are not already available to OpsMan. Source effectively provides the arguments for `om download-product`, so pivnet, s3 and GCS is supported. For example:

```yaml
director-config:
    properties-configuration:
    director_configuration:
      allow_legacy_agents: true
      blobstore_type: local
- product-name: cf
  version: 2.5.4
  source:
    pivnet-file-glob: srt-*.pivotal
    pivnet-api-token: YOUR_PIVNET_TOKEN
  stemcells:
  - version: "250.48"
    os: ubuntu-xenial
    source:
      pivnet-product-slug: stemcells-ubuntu-xenial
      pivnet-file-glob: "*google*"
      pivnet-api-token: YOUR_PIVNET_TOKEN
  product-properties:
    .cloud_controller.apps_domain:
      value: apps.my-foundation.com
```

## example pipeline

```yaml
resource_types:
- name: foundation
  type: docker-image
  source:
    repository: pkuryloski/concourse-pcf-foundation-resource
  check_every: 10m
- name: gcs-resource
  type: docker-image
  source:
    repository: frodenas/gcs-resource

resources:
- name: config-source
  type: git
  icon: github-circle
  source:
    branch: master
    uri: git@github.com:pjk25/cpfr-reference-pipelines.git
    private_key: ((github-key))
- name: incremental-config
  type: gcs-resource
  source:
    bucket: sbx-config
    json_key: ((gcs-key))
    versioned_file: foundation-config.yml
- name: foundation
  type: foundation
  source:
    opsmgr:
      url: pcf.my-foundation.com
      username: ((opsman.username))
      password: ((opsman.password))
      skip_ssl_validation: true

jobs:
- name: inception-button
  serial: true
  plan:
  - get: foundation
  - task: rename-file
    config:
      platform: linux
      image_resource:
        type: docker-image
        source:
          repository: pkuryloski/incrementalizer
      inputs:
      - name: foundation
      outputs:
      - name: incremental-config
      run:
        path: /bin/bash
        args:
        - -xc
        - |
          cp foundation/configuration.yml incremental-config/foundation-config.yml
  - put: incremental-config
    params:
      file: incremental-config/foundation-config.yml

- name: generate-incremental-config
  serial: true
  plan:
  - in_parallel:
    - get: foundation
      trigger: true
    - get: config-source
      trigger: true
    - get: incremental-config
  - task: compute-config
    config:
      platform: linux
      image_resource:
        type: docker-image
        source:
          repository: pkuryloski/incrementalizer
      inputs:
      - name: foundation
      - name: config-source
      outputs:
      - name: computed-config
      run:
        path: /bin/bash
        args:
        - -xc
        - |
          incrementalizer --debug min \
            config-source/constraints.edn \
            foundation/configuration.yml \
            config-source/environments/gcp/sbx/foundation-config.yml > \
            computed-config/foundation-config.yml
  - try:
      do:
      - task: check-for-changes
        config:
          platform: linux
          image_resource:
            type: docker-image
            source:
              repository: pkuryloski/incrementalizer
          inputs:
          - name: computed-config
          - name: incremental-config
          outputs:
          - name: new-config
          run:
            path: /bin/bash
            args:
            - -c
            - |
              diff --report-identical-files \
                  incremental-config/foundation-config.yml \
                  computed-config/foundation-config.yml > /dev/null 2>&1
              if [ $? -eq 0 ]; then
                echo "A config change was expected, but not produced."
                exit 1
              fi
              set -e
              cp computed-config/foundation-config.yml new-config
      - put: incremental-config
        params:
          file: new-config/foundation-config.yml

- name: apply-incremental-config
  serial: true
  plan:
  - get: incremental-config
    trigger: true
  - put: foundation
    params:
      file: incremental-config/foundation-config.yml
```
