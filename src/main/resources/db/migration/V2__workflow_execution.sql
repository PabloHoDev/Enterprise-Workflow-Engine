-- Módulo Workflow Execution: o que está acontecendo em uma execução específica.

create table workflow (
    id                        uuid        primary key,
    definition_key            varchar(64) not null,
    definition_version_id     uuid        not null,
    definition_version_number integer     not null,
    status                    varchar(16) not null,
    current_state             varchar(64) not null,
    variables                 jsonb       not null,
    created_at                timestamptz not null,
    updated_at                timestamptz not null,
    lock_version              bigint      not null,
    constraint fk_workflow_definition_version foreign key (definition_version_id)
        references workflow_definition_version (id),
    -- BR-014: o State atual sempre existe na versão à qual a execução está presa.
    constraint fk_workflow_current_state foreign key (definition_version_id, current_state)
        references workflow_state (version_id, name),
    constraint ck_workflow_status check (status in ('CREATED', 'RUNNING', 'COMPLETED', 'CANCELLED'))
);

create index ix_workflow_definition_key on workflow (definition_key);
create index ix_workflow_status on workflow (status);
create index ix_workflow_created_at on workflow (created_at);

create table workflow_history (
    id              uuid         primary key,
    workflow_id     uuid         not null,
    sequence_number integer      not null,
    event_type      varchar(16)  not null,
    action          varchar(64),
    from_state      varchar(64),
    to_state        varchar(64)  not null,
    actor_id        varchar(128) not null,
    occurred_at     timestamptz  not null,
    comment_text    varchar(500),
    constraint fk_workflow_history_workflow foreign key (workflow_id) references workflow (id),
    -- BR-030: a ordem cronológica é inequívoca; também barra dois registros concorrentes na mesma posição.
    constraint uk_workflow_history_sequence unique (workflow_id, sequence_number),
    constraint ck_workflow_history_event_type check (event_type in ('CREATED', 'STARTED', 'TRANSITIONED', 'CANCELLED'))
);
