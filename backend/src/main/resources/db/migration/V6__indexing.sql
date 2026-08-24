CREATE INDEX idx_tick_user ON public.tick (user_id);
CREATE INDEX idx_tick_import_batch ON public.tick (import_batch_id);
CREATE INDEX idx_import_batch_user ON public.import_batch (user_id);
CREATE INDEX idx_skipped_row_import_batch ON public.skipped_row (import_batch_id);
ALTER TABLE public.tick ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE public.import_batch ALTER COLUMN user_id SET NOT NULL;
CREATE UNIQUE INDEX idx_app_users_lower_email ON public.app_users (lower(email));
