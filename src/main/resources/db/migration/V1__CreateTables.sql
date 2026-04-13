CREATE TYPE task_status AS ENUM ('draft', 'ready_for_approval', 'approved');
CREATE TYPE submission_mode AS ENUM ('run', 'diagnose', 'submit');

CREATE CAST (CHARACTER VARYING as task_status) WITH INOUT AS IMPLICIT;
CREATE CAST (CHARACTER VARYING as submission_mode) WITH INOUT AS IMPLICIT;


CREATE TABLE task
(
    id                BIGINT        NOT NULL,
    max_points        NUMERIC(7, 2) NOT NULL,
    status            TASK_STATUS   NOT NULL,
    solution          TEXT          NOT NULL,
    executed_solution JSONB         NOT NULL,
    table_points      INT           NOT NULL,
    primarykey_points INT           NOT NULL,
    foreignkey_points INT           NOT NULL,
    constraint_points INT           NOT NULL,
    whitelist         TEXT          NOT NULL,
    created           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT task_pk PRIMARY KEY (id)
);

CREATE TABLE submission
(
    id                UUID                     DEFAULT gen_random_uuid(),
    user_id           VARCHAR(255),
    assignment_id     VARCHAR(255),
    task_id           BIGINT,
    submission_time   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    language          VARCHAR(2)      NOT NULL DEFAULT 'en',
    mode              submission_mode NOT NULL,
    feedback_level    INT             NOT NULL,
    evaluation_result JSONB,
    submission        TEXT,
    CONSTRAINT submission_pk PRIMARY KEY (id),
    CONSTRAINT submission_task_fk FOREIGN KEY (task_id) REFERENCES task (id)
        ON DELETE CASCADE
);
