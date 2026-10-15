-- One service now serves several orgs, so org_id becomes part of every primary key.
-- Rows without an org_id (none are expected: every row came from an event that carried one) are
-- assigned the first configured tenant, which in a per-org deployment is the org the database belongs to.

update behandling set org_id = '${defaultOrgId}' where org_id is null;
alter table behandling alter column org_id set not null;
alter table behandling drop constraint behandling_pkey;
alter table behandling add primary key (org_id, id);

update samtykke set org_id = '${defaultOrgId}' where org_id is null;
alter table samtykke alter column org_id set not null;
alter table samtykke drop constraint samtykke_pkey;
alter table samtykke add primary key (org_id, id);

update tjeneste set org_id = '${defaultOrgId}' where org_id is null;
alter table tjeneste alter column org_id set not null;
alter table tjeneste drop constraint tjeneste_pkey;
alter table tjeneste add primary key (org_id, id);

alter table personopplysning rename column identifikator_verdi to id;
alter table personopplysning add column org_id varchar(255);
update personopplysning set org_id = '${defaultOrgId}' where org_id is null;
alter table personopplysning alter column org_id set not null;
alter table personopplysning drop constraint personopplysning_pkey;
alter table personopplysning add primary key (org_id, id);
