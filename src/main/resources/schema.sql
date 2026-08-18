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

create table if not exists review_decision (
    id bigint not null auto_increment primary key,
    transaction_id varchar(100) not null,
    status varchar(20) not null,
    decided_at varchar(40) not null,
    index idx_review_decision_transaction_id (transaction_id)
);
