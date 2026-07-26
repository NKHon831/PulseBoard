-- `amount` stays the source of truth, always in the base currency (MYR).
-- These columns record what the user actually typed and the rate applied at
-- that moment, so a conversion can always be explained after the fact.
ALTER TABLE expenses
    ADD COLUMN original_amount   NUMERIC(14, 4),
    ADD COLUMN original_currency VARCHAR(3),
    ADD COLUMN exchange_rate     NUMERIC(18, 8);

-- Rows created before multi-currency entry hold a base-currency amount, which
-- is by definition what was entered, at a rate of 1.
UPDATE expenses
SET original_amount   = amount,
    original_currency = 'MYR',
    exchange_rate     = 1
WHERE original_currency IS NULL;

ALTER TABLE expenses
    ALTER COLUMN original_amount   SET NOT NULL,
    ALTER COLUMN original_currency SET NOT NULL,
    ALTER COLUMN exchange_rate     SET NOT NULL;

ALTER TABLE expenses
    ADD CONSTRAINT chk_expenses_original_amount_positive CHECK (original_amount > 0),
    ADD CONSTRAINT chk_expenses_exchange_rate_positive CHECK (exchange_rate > 0);
