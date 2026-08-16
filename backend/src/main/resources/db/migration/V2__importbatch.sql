ALTER TABLE public.tick
    ADD COLUMN import_batch_id bigint;

ALTER TABLE ONLY public.tick
    ADD CONSTRAINT fk_tick_import_batch
    FOREIGN KEY (import_batch_id) REFERENCES public.import_batch(id);

ALTER TABLE public.import_batch
    DROP COLUMN total_rows;