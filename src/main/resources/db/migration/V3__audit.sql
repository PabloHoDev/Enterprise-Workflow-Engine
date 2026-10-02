-- Módulo Audit: quem fez o quê, quando e qual foi o resultado.

create table audit_record (
    id            uuid          primary key,
    actor_id      varchar(128)  not null,
    operation     varchar(64)   not null,
    resource_type varchar(64)   not null,
    resource_id   varchar(128)  not null,
    outcome       varchar(16)   not null,
    detail        varchar(1000),
    occurred_at   timestamptz   not null,
    constraint ck_audit_record_outcome check (outcome in ('SUCCESS', 'REJECTED'))
);

create index ix_audit_record_resource on audit_record (resource_type, resource_id);
create index ix_audit_record_actor on audit_record (actor_id);
create index ix_audit_record_occurred_at on audit_record (occurred_at);
