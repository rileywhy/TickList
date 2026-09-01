ALTER TABLE tick ADD COLUMN stiffness INTEGER;
ALTER TABLE tick ADD COLUMN hold_color VARCHAR(255);
ALTER TABLE tick ADD COLUMN indoor BOOLEAN;
ALTER TABLE tick ADD COLUMN tick_timestamp timestamp with time zone;

ALTER TABLE tick ADD CONSTRAINT uk_tick_user_source_external UNIQUE (user_id, source_app, external_id);