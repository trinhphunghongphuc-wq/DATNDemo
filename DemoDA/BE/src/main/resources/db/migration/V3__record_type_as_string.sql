-- Dừng migration nếu gặp cặp ordinal/prefix chưa được đối chiếu.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM records r
        WHERE NOT EXISTS (
            SELECT 1
            FROM (
                VALUES
                    ('PROD', 0),
                    ('HARV', 1),
                    ('PACK', 2),
                    ('REJC', 3),
                    ('TRAN', 3), -- TRANSPORT tạo trước khi chèn enum mới
                    ('RTRN', 4),
                    ('WARE', 4), -- WAREHOUSE tạo trước khi chèn enum mới
                    ('RETA', 5), -- RETAIL tạo trước khi chèn enum mới
                    ('TRAN', 5),
                    ('WARE', 6),
                    ('RETA', 7)
            ) AS expected(prefix, ordinal_value)
            WHERE expected.prefix = split_part(r.record_key, '-', 1)
              AND expected.ordinal_value = r.record_type
        )
    ) THEN
        RAISE EXCEPTION
            'Unexpected record_type/record_key pair; inspect records before migration';
    END IF;
END $$;

ALTER TABLE records
    DROP CONSTRAINT IF EXISTS records_record_type_check;

ALTER TABLE records
    ALTER COLUMN record_type TYPE varchar(32)
    USING CASE split_part(record_key, '-', 1)
        WHEN 'PROD' THEN 'PRODUCTION'
        WHEN 'HARV' THEN 'HARVEST'
        WHEN 'PACK' THEN 'PACKAGING'
        WHEN 'REJC' THEN 'DELIVERY_REJECTION'
        WHEN 'RTRN' THEN 'RETURN_RECEIPT'
        WHEN 'TRAN' THEN 'TRANSPORT'
        WHEN 'WARE' THEN 'WAREHOUSE'
        WHEN 'RETA' THEN 'RETAIL'
    END;

ALTER TABLE records
    ADD CONSTRAINT records_record_type_check
    CHECK (record_type IN (
        'PRODUCTION',
        'HARVEST',
        'PACKAGING',
        'DELIVERY_REJECTION',
        'RETURN_RECEIPT',
        'TRANSPORT',
        'WAREHOUSE',
        'RETAIL'
    ));