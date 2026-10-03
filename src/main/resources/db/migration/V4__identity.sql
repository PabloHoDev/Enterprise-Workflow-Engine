-- Módulo Identity: contas de usuário, papéis e proteção contra tentativas de login (ADR-007).

create table app_user (
    id                    uuid         primary key,
    username              varchar(64)  not null,
    display_name          varchar(120) not null,
    password_hash         varchar(255) not null,
    enabled               boolean      not null,
    failed_login_attempts integer      not null,
    locked_until          timestamptz,
    last_login_at         timestamptz,
    password_changed_at   timestamptz  not null,
    created_at            timestamptz  not null,
    updated_at            timestamptz  not null,
    lock_version          bigint       not null,
    constraint uk_app_user_username unique (username),
    constraint ck_app_user_failed_attempts check (failed_login_attempts >= 0)
);

create table app_user_role (
    user_id uuid        not null,
    role    varchar(32) not null,
    constraint pk_app_user_role primary key (user_id, role),
    constraint fk_app_user_role_user foreign key (user_id) references app_user (id) on delete cascade
);
