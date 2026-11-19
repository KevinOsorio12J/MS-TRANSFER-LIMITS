CREATE TABLE account_channel_limits (
    account_id VARCHAR(64) NOT NULL,
    channel VARCHAR(24) NOT NULL,
    daily_limit NUMERIC(19, 2) NOT NULL CHECK (daily_limit > 0),
    per_operation_limit NUMERIC(19, 2) NOT NULL CHECK (per_operation_limit > 0),
    consumed_amount NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (consumed_amount >= 0),
    usage_date DATE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (account_id, channel),
    CHECK (per_operation_limit <= daily_limit)
);
