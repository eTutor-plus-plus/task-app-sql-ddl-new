ALTER TABLE task
    ALTER COLUMN table_points TYPE NUMERIC(7, 2) USING table_points::NUMERIC(7, 2),
    ALTER COLUMN primarykey_points TYPE NUMERIC(7, 2) USING primarykey_points::NUMERIC(7, 2),
    ALTER COLUMN foreignkey_points TYPE NUMERIC(7, 2) USING foreignkey_points::NUMERIC(7, 2),
    ALTER COLUMN constraint_points TYPE NUMERIC(7, 2) USING constraint_points::NUMERIC(7, 2),
    ALTER COLUMN assertion_points TYPE NUMERIC(7, 2) USING assertion_points::NUMERIC(7, 2);

ALTER TABLE task
    ALTER COLUMN assertion_points SET DEFAULT 0.00;
