# Data move from the per-org adapters

Done by hand with psql after the multi-tenant adapter is deployed and has run Flyway; nothing in the
application touches another schema. All FINT adapters share the Postgres database `fint-core`, each with a
user and a schema named after its `PGUser`, so the move is a copy between schemas in the same database by a
role that can read the old schemas and write the new one.

Find the schemas first:

```sql
select table_schema, table_name
from information_schema.tables
where table_name in ('personopplysning_entity', 'personopplysning', 'behandling', 'samtykke', 'tjeneste')
order by 1, 2;
```

| Script                            | Copies                                                   | Variables                                         |
|-----------------------------------|----------------------------------------------------------|---------------------------------------------------|
| `migrate-personopplysning.sql`    | the kodeverk adapter's one shared list, once per org     | all optional: `old_schema`, `new_schema`, `orgs`  |
| `migrate-samtykke.sql`            | one org's behandling, samtykke and tjeneste rows         | `old_schema`, `new_schema`, `org`                 |

Both are idempotent (`on conflict do nothing`), so they can be re-run after the old adapters have taken new
writes, and both run in one transaction. Run the new adapter at least once before copying, so Flyway has
created the tables (V1 to V3).

`migrate-personopplysning.sql` finds the schemas itself (the one holding `personopplysning_entity` and the one
holding `personopplysning` with an `org_id` column), prints the old list and the per-org counts, and asks
`Commit? (yes/no)` before committing. Anything but yes rolls back. Orgs default to rogfk.no and
trondelagfylke.no:

```
psql "$FINT_CORE_DB" -f scripts/migrate-personopplysning.sql
psql "$FINT_CORE_DB" -v orgs="'rogfk.no'" -f scripts/migrate-personopplysning.sql   # other orgs
```

`migrate-samtykke.sql` takes the schema names from the query above:

```
psql "$FINT_CORE_DB" -v old_schema=<rogfk schema> -v new_schema=<new schema> -v org=rogfk.no \
  -f scripts/migrate-samtykke.sql
```

Without psql installed, run it from the postgres image:

```
docker run --rm -it -v "$PWD/scripts:/scripts:ro" postgres:17 \
  psql "postgresql://<admin user>:<password>@<host>:<port>/fint-core?sslmode=require" \
  -f /scripts/migrate-personopplysning.sql
```

The database is the Aiven Postgres of the environment (beta: `pg-beta-fintlabs.aivencloud.com:16649`); the
per-app users from the `*-db` secrets only see their own schema, so use the admin user from the Aiven console.
