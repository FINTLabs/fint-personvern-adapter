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

One instance serves any number of orgs. Each org is a *tenant* with its own adapter credential under
`fint.adapter.tenants`; data is keyed by `(org_id, id)` in every table. By default a single tenant is built
from the `fint.core.oauth2.*` secret keys and `fint.adapter.org-id` / `fint.adapter.id`, which is what a per-org
deployment provides. A deployment with several tenants sets the whole list through
`FINT_ADAPTER_TENANTS_<n>_ORGID`, `_ID`, `_USERNAME`, `_PASSWORD`, `_CLIENTID`, `_CLIENTSECRET`
(Spring Boot drops the dashes of `org-id` and friends in environment variable names).

Full sync runs every `full-sync-interval-in-days` of each capability (default one day), heartbeat every
minute, event polling every second.

## Running locally

```
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'
```

The `local` profile expects a FINT adapter gateway on `http://localhost:8081` and a Postgres on `localhost:5432`.
Tests need Docker for Testcontainers.
