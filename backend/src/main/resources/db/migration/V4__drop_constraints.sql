ALTER TABLE public.tick DROP CONSTRAINT tick_discipline_check;
ALTER TABLE public.tick DROP CONSTRAINT tick_grade_system_check;
ALTER TABLE public.tick DROP CONSTRAINT tick_rope_style_check;
ALTER TABLE public.tick DROP CONSTRAINT tick_source_app_check;
ALTER TABLE public.tick DROP CONSTRAINT tick_tick_type_check;
ALTER TABLE public.grade_mapping DROP CONSTRAINT grade_mapping_discipline_check;
ALTER TABLE public.grade_mapping DROP CONSTRAINT grade_mapping_grade_system_check;
ALTER TABLE public.import_batch DROP CONSTRAINT import_batch_source_app_check;