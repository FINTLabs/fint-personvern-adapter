-- Copies the kodeverk adapter's personopplysning list (one shared table) into the multi-tenant adapter,
-- once per org, so each org owns its own copy.
--
-- psql variables:
--   old_schema  schema of fint-personvern-kodeverk-adapter   (table personopplysning_entity)
--   new_schema  schema of fint-personvern-adapter             (table personopplysning, keyed by org_id + id)
--   orgs        quoted, comma-separated org ids, e.g. 'rogfk.no','trondelagfylke.no'

\set ON_ERROR_STOP on
\echo copying personopplysning from :old_schema to :new_schema for orgs :orgs

begin;

insert into :new_schema.personopplysning
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
from :old_schema.personopplysning_entity old
cross join (select unnest(array[:orgs]) as org_id) org
on conflict (org_id, id) do nothing;

select org_id, count(*) as personopplysninger
from :new_schema.personopplysning
where org_id in (:orgs)
group by org_id
order by org_id;

commit;
