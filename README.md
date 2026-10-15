# Personvern Adapter

FINT adapter for the `personvern` domain. It persists and serves both packages:

| Package    | Resources                                   | Storage                                   |
|------------|---------------------------------------------|-------------------------------------------|
| `samtykke` | `behandling`, `samtykke`, `tjeneste`        | Postgres, one JSON column per resource    |
| `kodeverk` | `behandlingsgrunnlag`, `personopplysning`   | Fixed code list / Postgres table          |

The `kodeverk` package was previously served by `fint-personvern-kodeverk-adapter`; that adapter is folded into
this service. The adapter credential needs the component roles for both `personvern_samtykke` and
`personvern_kodeverk`.

Built with Kotlin on Spring Boot 4 and `fint-core-adapter-common`. Schema is managed by Flyway
(`src/main/resources/db/migration`).

## Running locally

```
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'
```

The `local` profile expects a FINT adapter gateway on `http://localhost:8081` and a Postgres on `localhost:5432`.
Tests need Docker for Testcontainers.
