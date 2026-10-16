ALTER TABLE evaluation_results
    ADD COLUMN IF NOT EXISTS sample_index integer;

ALTER TABLE batches
    ADD COLUMN IF NOT EXISTS retailer_recording_closed
    boolean NOT NULL DEFAULT false;

UPDATE batches b
SET retailer_recording_closed = true
WHERE EXISTS (
    SELECT 1
    FROM stage_anchor_transactions sat
    WHERE sat.batch_id = b.id
      AND sat.record_stage = 'RETAILER'
);

ALTER TABLE evaluation_results
    ADD COLUMN IF NOT EXISTS sample_index integer;

ALTER TABLE batches
    ADD COLUMN IF NOT EXISTS retailer_recording_closed
    boolean NOT NULL DEFAULT false;