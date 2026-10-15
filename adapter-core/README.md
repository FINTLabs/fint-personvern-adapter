# adapter-core

Tenant-aware runtime for a FINT adapter. Configuration decides *what* and *when*; the adapter only says
*where the data comes from* and *how a write is handled*.

## What the adapter implements

| Bean type                                       | Purpose                                                     | Required |
|-------------------------------------------------|-------------------------------------------------------------|----------|
| `ResourceSource<T>`                             | Full-sync data for one resource, per org                    | one per capability |
| `EventHandler<T>`                               | Answer events for one resource                              | optional |

Both carry `resourceName` (the key under `fint.adapter.capabilities`), `resourceType` and `identifier(resource)`.
An `EventHandler` also declares `operations` (default CREATE and UPDATE); they are registered as the contract's
event capabilities, so the gateway only serves those operations. A capability without a `ResourceSource`, or a
bean without a capability, fails startup.

## Configuration

```yaml
fint:
  adapter:
    base-url: https://api.felleskomponent.no
    token-uri: https://idp.felleskomponent.no/nidp/oauth/nam/token
    heartbeat-interval: 1m          # also what is promised in the contract
    event-poll-interval: 1s
    full-sync-initial-delay: 10s
    full-sync-interval:             # optional override of every capability's fullSyncIntervalInDays
    page-size: 100
    debug: false                    # validate sync pages and log problems
    capabilities:
      behandling:
        domain-name: personvern
        package-name: samtykke
        resource-name: behandling
        full-sync-interval-in-days: 1
        delta-sync-interval: IMMEDIATE
    tenants:                        # one entry per adapter credential
      - orgs: [novari.no, side.novari.no, test.novari.no]   # every org this credential serves
        id: https://vigoiks.no/novari-no/personvern
        username: ...
        password: ...
        client-id: ...
        client-secret: ...
      - orgs: [rogfk.no]
        id: https://vigoiks.no/rogfk-no/personvern
        username: ...
        ...
```

A tenant is one credential. For each of its orgs it registers a contract (the gateway keys contracts on
username + orgId), sends a heartbeat and is full-synced for every capability. Events are polled once per
credential, because the gateway answers with the pending events of every org the credential has a contract
for, and each answer carries the event's own orgId. The credential's token must list every configured org
among its assets, and all orgs of one credential must belong to the same main org (one gateway).

A tenant list given through the environment (`FINT_ADAPTER_TENANTS_0_ORGS=novari.no,side.novari.no`,
`..._CLIENTID`, ...) replaces the list in YAML entirely; Spring Boot drops the dashes of `client-id` in
environment variable names.

## Beans you can replace

Everything is defined with `@ConditionalOnMissingBean`: `PasswordGrantTokenClient`, the gateway
`ClientHttpConnector` (bean name `adapterClientHttpConnector`), `TenantRegistry`, `FullSyncService`,
`EventService`, `RegistrationService`, `HeartbeatService`, the `adapterTaskScheduler` and `AdapterScheduler`.

## Relation to fint-core-adapter-common

This module replaces `no.fintlabs:fint-core-adapter-common` for this service. It keeps the wire format from
`fint-core-infra-models` and the gateway paths, and drops the per-resource Publisher/Subscriber class
quartets and the single-tenant `AdapterProperties`. Once proven here it can be published to reposilite and
used by other adapters.
