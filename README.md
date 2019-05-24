# concourse-pcf-foundation-resource

About versions:

If there are any changes being applied, or there are saved changes not yet applied, the version is indeterminate, and check will fail. This is because we cannot retrieve deployed product config via the OpsMan API; therefore staged product config (that we can retrieve) only represents what is deployed under the aforementioned conditions.

Additionally, while it may be feasible to cache past versions in the resource container, since historic configs are not available through the OpsMan API, this resource only reports the current version.

## Source Configuration
```yaml
- opsmgr:
    - url
    - username
    - password
    - skip_ssl_validation: true|false (default false)
```

## Behavior

### check

hash of the opsman configuration manfiest

### in

- foundation.yml
- version

### out

- foundation.yml

This only makes changes if what's there differs, things not specified are assumed floating
