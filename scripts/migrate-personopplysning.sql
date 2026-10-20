-- Copies the kodeverk adapter's personopplysning list (one shared table, personopplysning_entity) into the
-- multi-tenant adapter's personopplysning table, once per org, so each org owns its own copy.
--
-- Finds both schemas by itself: the old one is the schema holding personopplysning_entity, the new one the
-- schema holding personopplysning with an org_id column. Shows what it found and what it is about to copy,
-- then asks before committing. Idempotent: rows already present in the new schema are left alone.
--
-- psql variables (all optional):
--   old_schema  override the discovered kodeverk schema
--   new_schema  override the discovered multi-tenant schema
--   orgs        quoted, comma-separated org ids; default 'rogfk.no','trondelagfylke.no'
--
-- Run with a role that can read the old schema and write the new one, e.g.
--   psql "$FINT_CORE_DB" -f scripts/migrate-personopplysning.sql

\set ON_ERROR_STOP on
\pset footer off

\if :{?orgs}
\else
  \set orgs '''rogfk.no'',''trondelagfylke.no'''
\endif

-- 1. Where is the old list? Fails if there is none or more than one candidate.
\if :{?old_schema}
\else
  select table_schema as old_schema
  from information_schema.tables
  where table_name = 'personopplysning_entity'
  \gset
\endif

-- 2. Where is the new table? The multi-tenant schema is the one that has both personopplysning with an
--    org_id column and the samtykke tables. Fails if there is none or more than one candidate.
\if :{?new_schema}
\else
  select c.table_schema as new_schema
  from information_schema.columns c
  join information_schema.tables t on t.table_schema = c.table_schema and t.table_name = 'behandling'
  where c.table_name = 'personopplysning' and c.column_name = 'org_id'
  \gset
\endif

\echo
\echo old schema (fint-personvern-kodeverk-adapter): :old_schema
\echo new schema (fint-personvern-adapter):          :new_schema
\echo orgs:                                          :orgs
\echo

\echo == personopplysning in the old schema
select identifikator_verdi as id, navn, kode, passiv, start_gyldighetsdato, end_gyldighetsdato
from :"old_schema".personopplysning_entity
order by identifikator_verdi;

\echo == already in the new schema, per org
select org_id, count(*) as personopplysninger
from :"new_schema".personopplysning
group by org_id
order by org_id;

begin;

\echo == copying
insert into :"new_schema".personopplysning
    (org_id, id, navn, kode, passiv,
     start_gyldighetsdato, end_gyldighetsdato, beskrivelse_gyldighets_periode,
     start_identifikator_dato, end_identifikator_dato)
select org.org_id,
       old.identifikator_verdi,
       old.navn,
       old.kode,
       coalesce(old.passiv, false),
       old.start_gyldighetsdato,
       old.end_gyldighetsdato,
       old.beskrivelse_gyldighets_periode,
       old.start_identifikator_dato,
       old.end_identifikator_dato
from :"old_schema".personopplysning_entity old
cross join (select unnest(array[:orgs]) as org_id) org
on conflict (org_id, id) do nothing;

\echo == in the new schema after the copy, per org (not yet committed)
select org_id, count(*) as personopplysninger
from :"new_schema".personopplysning
where org_id in (:orgs)
group by org_id
order by org_id;

\prompt 'Commit? (yes/no) ' answer
\if :answer
  commit;
  \echo committed
\else
  rollback;
  \echo rolled back, nothing changed
\endif
