create table if not exists app_user (
    id bigint not null auto_increment primary key,
    email varchar(255) not null unique,
    password_hash varchar(100) not null,
    role varchar(20) not null,
    created_at timestamp not null default current_timestamp
);

create table if not exists policy_document (
    id bigint not null auto_increment primary key,
    title varchar(200) not null,
    version_no int not null,
    original_filename varchar(255) not null,
    content_type varchar(100) not null,
    content longblob not null,
    status varchar(20) not null,
    registered_by varchar(255) not null,
    created_at timestamp not null default current_timestamp,
    unique key uk_policy_document_title_version (title, version_no)
);

create table if not exists review_batch (
    id bigint not null auto_increment primary key,
    created_by bigint not null,
    original_filename varchar(255) not null,
    status varchar(20) not null,
    rule_version varchar(20) not null,
    total_count int not null default 0,
    created_at timestamp not null default current_timestamp,
    foreign key (created_by) references app_user(id)
);

create table if not exists settlement_transaction (
    id bigint not null auto_increment primary key,
    batch_id bigint not null,
    external_transaction_id varchar(100) not null,
    transaction_date date not null,
    merchant varchar(200) not null,
    amount decimal(19, 2) not null,
    receipt_number varchar(100),
    review_status varchar(20) not null default 'PENDING',
    foreign key (batch_id) references review_batch(id),
    unique key uk_settlement_transaction_batch_external (batch_id, external_transaction_id)
);

create table if not exists review_violation (
    id bigint not null auto_increment primary key,
    transaction_id bigint not null,
    rule_code varchar(50) not null,
    reason varchar(500) not null,
    foreign key (transaction_id) references settlement_transaction(id)
);

create table if not exists review_ai_explanation (
    id bigint not null auto_increment primary key,
    transaction_id bigint not null unique,
    summary text not null,
    citations_json text not null,
    generated_by varchar(30) not null,
    foreign key (transaction_id) references settlement_transaction(id)
);

create table if not exists transaction_decision (
    id bigint not null auto_increment primary key,
    transaction_id bigint not null,
    status varchar(20) not null,
    reason varchar(500),
    decided_by bigint not null,
    decided_at timestamp not null default current_timestamp,
    foreign key (transaction_id) references settlement_transaction(id),
    foreign key (decided_by) references app_user(id),
    index idx_transaction_decision_transaction_id (transaction_id)
);

create table if not exists review_decision (
    id bigint not null auto_increment primary key,
    transaction_id varchar(100) not null,
    status varchar(20) not null,
    decided_at varchar(40) not null,
    index idx_review_decision_transaction_id (transaction_id)
);
