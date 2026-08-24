ALTER TABLE public.tick
    ALTER COLUMN notes TYPE text,
    ALTER COLUMN climb_name TYPE text,
    ALTER COLUMN location TYPE text,
    ALTER COLUMN source_url TYPE text;

ALTER TABLE public.import_batch
    ALTER COLUMN original_filename TYPE text;