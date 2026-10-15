# Personvern Adapter

FINT adapter for the `personvern` domain. It persists and serves both packages:

| Package    | Resources                                   | Storage                                   |
|------------|---------------------------------------------|-------------------------------------------|
| `samtykke` | `behandling`, `samtykke`, `tjeneste`        | Postgres, one JSON column per resource    |
| `kodeverk` | `behandlingsgrunnlag`, `personopplysning`   | Fixed code list / Postgres table          |

The `kodeverk` package was previously served by `fint-personvern-kodeverk-adapter`; that adapter is folded into
this service. The adapter credential needs the component roles for both `personvern_samtykke` and
`personvern_kodeverk`.

Built with Kotlin on Spring Boot 4. The adapter runtime (registration, heartbeat, full sync, events) is the
`adapter-core` module in this repo, see [adapter-core/README.md](adapter-core/README.md). Schema is managed by
Flyway (`src/main/resources/db/migration`).

## Multi-tenant

One instance serves any number of orgs. A *tenant* under `fint.adapter.tenants` is one adapter credential and
the orgs it serves (a main org and its sub-orgs, or just one org); data is keyed by `(org_id, id)` in every
table. Tenants are never in `application.yml` (YAML entries would merge with, not be replaced by, the environment);
a deployment sets them through `FINT_ADAPTER_TENANTS_<NAME>_ORGS` (comma-separated), `_USERNAME`, `_PASSWORD`, `_CLIENTID`,
`_CLIENTSECRET` and optionally `_ID` (Spring Boot drops the dashes of `client-id` and friends in environment
variable names).

Full sync runs every `full-sync-interval-in-days` of each capability (default one day), heartbeat every
minute, event polling every second.

## Running locally

```
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'
```

The `local` profile expects a FINT adapter gateway on `http://localhost:8081` and a Postgres on `localhost:5432`.
Tests need Docker for Testcontainers.

## Deployment

One `Application` per environment in the `novari-no` namespace (`kustomize/overlays/<env>`). Everything
secret comes from one 1Password item per environment, `vaults/aks-<env>-vault/items/fint-personvern-adapter`;
the flaiserator turns it into the secret `fint-personvern-adapter-op` and injects every field as an environment
variable. The database user and its `fint.database.*` variables come from the Application's `database` setting.

Fields in the 1Password item, one block per adapter credential. `<NAME>` is any label you like (`NOVARI`,
`ROGFK`, ...); it only identifies the credential in logs:

| Field                                      | Value                                                                 |
|--------------------------------------------|-----------------------------------------------------------------------|
| `FINT_ADAPTER_TENANTS_<NAME>_ORGS`         | orgs the credential serves, comma-separated, e.g. `novari.no,side.novari.no` |
| `FINT_ADAPTER_TENANTS_<NAME>_USERNAME`     | the adapter credential's username                                     |
| `FINT_ADAPTER_TENANTS_<NAME>_PASSWORD`     | password                                                              |
| `FINT_ADAPTER_TENANTS_<NAME>_CLIENTID`     | OAuth2 client id                                                      |
| `FINT_ADAPTER_TENANTS_<NAME>_CLIENTSECRET` | OAuth2 client secret                                                  |
| `FINT_ADAPTER_TENANTS_<NAME>_ID`           | optional adapter id; default `https://vigoiks.no/<first org>/personvern` |

Each credential needs the `personvern_samtykke` and `personvern_kodeverk` component roles and must list all of
its orgs among its assets. To add an org: add a block (or an org to an existing block's `_ORGS`) in the item;
the adapter picks it up on restart. The `FintAdapter` operator is scaled to zero, so credentials are not
created from Kubernetes resources.

Branches: `develop` deploys to beta and `main` to api on push; alpha or a re-deploy is started by hand from
the CD workflow. Moving from the per-org deployments is a manual psql step, see [scripts/README.md](scripts/README.md).
