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

| Script                            | Copies                                                   | Variables                               |
|-----------------------------------|----------------------------------------------------------|-----------------------------------------|
| `migrate-personopplysning.sql`    | the kodeverk adapter's one shared list, once per org     | `old_schema`, `new_schema`, `orgs`      |
| `migrate-samtykke.sql`            | one org's behandling, samtykke and tjeneste rows         | `old_schema`, `new_schema`, `org`       |

Both are idempotent (`on conflict do nothing`), so they can be re-run after the old adapters have taken new
writes, and both run in one transaction. Example for api, giving rogfk and trondelagfylke the personopplysning
list:

```
psql "$FINT_CORE_DB" \
  -v old_schema=fint_personvern_kodeverk_adapter_pg \
  -v new_schema=<schema of the fint-personvern-adapter-db user> \
  -v orgs="'rogfk.no','trondelagfylke.no'" \
  -f scripts/migrate-personopplysning.sql
```

Replace the schema names with the ones the query above returns. Run the new adapter at least once before
copying, so Flyway has created the tables (V1 to V3).
