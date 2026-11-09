ALTER TABLE retail_units
    ADD COLUMN price_updated_at timestamptz;

CREATE TABLE retail_unit_price_history (
    id bigserial PRIMARY KEY,
    retail_unit_id bigint NOT NULL
        REFERENCES retail_units(id),
    retailer_id bigint NOT NULL
        REFERENCES users(id),
    old_price numeric(15, 2) NOT NULL,
    new_price numeric(15, 2) NOT NULL,
    changed_at timestamptz NOT NULL
);

CREATE INDEX idx_retail_unit_price_history_unit
    ON retail_unit_price_history(retail_unit_id, changed_at DESC);