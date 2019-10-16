# concourse-pcf-foundation-resource

Models the deployed configuration of a PCF Foundation, via OpsMan.
Leverages the om cli to do so.

About versions:

If there are any changes being applied, or there are saved changes not yet applied, the version is indeterminate, and check will fail. This is because we cannot retrieve deployed product config via the OpsMan API; therefore staged product config (that we can retrieve) only represents what is deployed under the aforementioned conditions.

Additionally, while it may be feasible to cache past versions in the resource container, since historic configs are not available through the OpsMan API, this resource only reports the current version.

Foundation configurations are described by the specs in https://github.com/pjk25/foundation-lib

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
  source:
    pivnet-file-glob: "srt*.pivotal"
    pivnet-api-token: YOUR-PIVNET-TOKEN
  product-properties:
    .cloud_controller.apps_domain:
      value: apps.my-foundation.com
```

### out

params:

file: path-to-foundation-config-yaml
dry_run: false|true

This only makes changes if what's there differs, things not specified are assumed floating

## example pipeline

```yaml
resource_types:
- name: foundation
  type: docker-image
  source:
    repository: pkuryloski/concourse-pcf-foundation-resource
  check_every: 10m

resources:
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

- name: apply-incremental-config
  serial: true
  plan:
  - get: incremental-config
    trigger: true
  - put: foundation
    params:
      file: incremental-config/foundation-config.yml
```