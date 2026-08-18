create table if not exists review_decision (
    id bigint not null auto_increment primary key,
    transaction_id varchar(100) not null,
    status varchar(20) not null,
    decided_at varchar(40) not null,
    index idx_review_decision_transaction_id (transaction_id)
);
