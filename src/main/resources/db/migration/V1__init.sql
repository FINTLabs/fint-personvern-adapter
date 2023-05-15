create table behandling
(
    id                 varchar(255) not null,
    last_modified_date timestamp,
    org_id             varchar(255),
    resource           json,
    primary key (id)
);
create table samtykke
(
    id                 varchar(255) not null,
    last_modified_date timestamp,
    org_id             varchar(255),
    resource           json,
    primary key (id)
);
create table tjeneste
(
    id                 varchar(255) not null,
    last_modified_date timestamp,
    org_id             varchar(255),
    resource           json,
    primary key (id)
);
