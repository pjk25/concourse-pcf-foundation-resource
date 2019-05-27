# concourse-pcf-foundation-resource

Models the deployed configuration of a PCF Foundation, via OpsMan.
Leverages the om cli to do so.

About versions:

If there are any changes being applied, or there are saved changes not yet applied, the version is indeterminate, and check will fail. This is because we cannot retrieve deployed product config via the OpsMan API; therefore staged product config (that we can retrieve) only represents what is deployed under the aforementioned conditions.

Additionally, while it may be feasible to cache past versions in the resource container, since historic configs are not available through the OpsMan API, this resource only reports the current version.

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

### in

- foundation.yml

Example foundation.yml:

```yaml
---
foo: to-the-bar
```

Example response:

```json
{
    "version": {
        "opsman_version": "2.5.4-build.189",
        "configuration_hash": ""
    },
    "metadata": []
}
```

If this is a fresh OpsMan with nothing deployed, then `configuration_hash` will be omitted, and `configuration.yml` will not be written.

### out

- foundation.yml

This only makes changes if what's there differs, things not specified are assumed floating
