CREATE TABLE check_constraint
(
    id                             UUID DEFAULT gen_random_uuid(),
    check_condition                VARCHAR(255),
    number_successful_statements   INT,
    number_unsuccessful_statements INT,
    insert_statements              TEXT,
    task_id                        BIGINT,
    CONSTRAINT check_constraint_pk PRIMARY KEY (id),
    CONSTRAINT check_constraint_task_fk FOREIGN KEY (task_id) REFERENCES task (id)
        ON DELETE CASCADE
);
