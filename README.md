

## Source Configuration
- opsmgr:
    - url
    - username
    - password
- create-opsman-pipeline:
- upgrade-opsman-pipeline:

## Behavior

### check

hash of the opsman configuration manfiest

### in

- foundation.yml
- version

### out

- foundation.yml

This only makes changes if what's there differs, things not specified are assumed floating
