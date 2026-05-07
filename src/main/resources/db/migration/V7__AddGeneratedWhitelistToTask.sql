ALTER TABLE task
    ADD COLUMN generated_whitelist TEXT;

UPDATE task
SET generated_whitelist = whitelist
WHERE generated_whitelist IS NULL;

ALTER TABLE task
    ALTER COLUMN generated_whitelist SET NOT NULL;

ALTER TABLE task
    ALTER COLUMN whitelist DROP NOT NULL;
