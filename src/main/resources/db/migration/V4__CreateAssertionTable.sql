CREATE TABLE assertion
(
    id                      UUID DEFAULT gen_random_uuid(),
    name                    VARCHAR(255),
    successful_statements   TEXT,
    unsuccessful_statements TEXT,
    task_id                 BIGINT,
    CONSTRAINT assertion_pk PRIMARY KEY (id),
    CONSTRAINT assertion_task_fk FOREIGN KEY (task_id) REFERENCES task (id)
        ON DELETE CASCADE
);
