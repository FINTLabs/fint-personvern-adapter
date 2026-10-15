-- Personopplysning moved here from fint-personvern-kodeverk-adapter, which created the table
-- (personopplysning_entity) with Hibernate ddl-auto. Same columns, new table name.
create table personopplysning
(
    identifikator_verdi             varchar(255) not null,
    navn                            varchar(255) not null,
    kode                            varchar(255),
    passiv                          boolean      not null default false,
    start_gyldighetsdato            timestamp(6),
    end_gyldighetsdato              timestamp(6),
    beskrivelse_gyldighets_periode  varchar(255),
    start_identifikator_dato        timestamp(6),
    end_identifikator_dato          timestamp(6),
    primary key (identifikator_verdi)
);
