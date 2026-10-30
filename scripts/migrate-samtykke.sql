-- Copies one org's behandling, samtykke and tjeneste rows from its per-org personvern adapter into the
-- multi-tenant adapter. Rows that already carry an org_id keep it; rows without one get :org.
--
-- psql variables:
--   old_schema  schema of that org's fint-personvern-adapter   (tables behandling, samtykke, tjeneste, keyed by id)
--   new_schema  schema of the multi-tenant adapter              (same tables, keyed by org_id + id)
--   org         the org id, e.g. rogfk.no

\set ON_ERROR_STOP on
\echo copying samtykke tables from :old_schema to :new_schema for org :'org'

begin;

insert into :new_schema.behandling (org_id, id, resource, last_modified_date)
select coalesce(org_id, :'org'), id, resource, last_modified_date
from :old_schema.behandling
on conflict (org_id, id) do nothing;

insert into :new_schema.tjeneste (org_id, id, resource, last_modified_date)
select coalesce(org_id, :'org'), id, resource, last_modified_date
from :old_schema.tjeneste
on conflict (org_id, id) do nothing;

insert into :new_schema.samtykke (org_id, id, resource, last_modified_date)
select coalesce(org_id, :'org'), id, resource, last_modified_date
from :old_schema.samtykke
on conflict (org_id, id) do nothing;

select 'behandling' as tabell, count(*) from :new_schema.behandling where org_id = :'org'
union all
select 'tjeneste', count(*) from :new_schema.tjeneste where org_id = :'org'
union all
select 'samtykke', count(*) from :new_schema.samtykke where org_id = :'org';

commit;
