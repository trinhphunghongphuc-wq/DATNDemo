ALTER TABLE public.records
    ADD CONSTRAINT uk_records_batch_leaf_index
    UNIQUE (batch_id, leaf_index);

ALTER TABLE public.records
    ADD CONSTRAINT uk_records_batch_stage_leaf_index
    UNIQUE (batch_id, record_stage, stage_leaf_index);