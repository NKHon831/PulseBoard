-- Rates in force when the expense was entered, so displaying it later never
-- re-prices it at today's rate. Encoded as "JPY=40.045" (see RateSnapshotConverter).
ALTER TABLE expenses ADD COLUMN rate_snapshot VARCHAR(255);

-- Expenses entered in a non-base currency already recorded the rate that was
-- applied, so their snapshot can be reconstructed exactly.
UPDATE expenses
SET rate_snapshot = original_currency || '=' || exchange_rate
WHERE original_currency <> 'MYR'
  AND rate_snapshot IS NULL;

-- Rows entered in the base currency have no historical foreign rate to recover;
-- they stay NULL and fall back to the current rate when shown in a foreign
-- currency. Left nullable for that reason.
