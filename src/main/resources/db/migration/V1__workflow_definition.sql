-- Módulo Workflow Definition: como um processo funciona.

create table workflow_definition (
    id             uuid          primary key,
    definition_key varchar(64)   not null,
    name           varchar(120)  not null,
    description    varchar(1000),
    created_at     timestamptz   not null,
    updated_at     timestamptz   not null,
    lock_version   bigint        not null,
    constraint uk_workflow_definition_key unique (definition_key)
);

create table workflow_definition_version (
    id             uuid        primary key,
    definition_id  uuid        not null,
    version_number integer     not null,
    status         varchar(16) not null,
    created_at     timestamptz not null,
    constraint fk_definition_version_definition foreign key (definition_id) references workflow_definition (id),
    constraint uk_definition_version_number unique (definition_id, version_number),
    constraint ck_definition_version_status check (status in ('DRAFT', 'ACTIVE', 'INACTIVE'))
);

-- Localiza a versão ativa de uma definição ao criar um Workflow.
create index ix_definition_version_active on workflow_definition_version (definition_id) where status = 'ACTIVE';

create table workflow_state (
    version_id uuid        not null,
    ordinal    integer     not null,
    name       varchar(64) not null,
    state_type varchar(16) not null,
    constraint pk_workflow_state primary key (version_id, ordinal),
    constraint fk_workflow_state_version foreign key (version_id) references workflow_definition_version (id),
    constraint uk_workflow_state_name unique (version_id, name),
    constraint ck_workflow_state_type check (state_type in ('INITIAL', 'INTERMEDIATE', 'TERMINAL'))
);

create table workflow_transition (
    id            uuid        primary key,
    version_id    uuid        not null,
    ordinal       integer     not null,
    action        varchar(64) not null,
    from_state    varchar(64) not null,
    to_state      varchar(64) not null,
    required_role varchar(64),
    constraint fk_workflow_transition_version foreign key (version_id) references workflow_definition_version (id),
    constraint uk_workflow_transition_action unique (version_id, from_state, action),
    -- Adiadas para o commit: os States da versão são gravados na mesma transação, depois das Transitions.
    constraint fk_workflow_transition_from foreign key (version_id, from_state)
        references workflow_state (version_id, name) deferrable initially deferred,
    constraint fk_workflow_transition_to foreign key (version_id, to_state)
        references workflow_state (version_id, name) deferrable initially deferred
);

create table workflow_transition_rule (
    transition_id  uuid         not null,
    ordinal        integer      not null,
    field_name     varchar(128) not null,
    rule_operator  varchar(32)  not null,
    expected_value varchar(255),
    constraint pk_workflow_transition_rule primary key (transition_id, ordinal),
    constraint fk_workflow_transition_rule_transition foreign key (transition_id) references workflow_transition (id)
);
