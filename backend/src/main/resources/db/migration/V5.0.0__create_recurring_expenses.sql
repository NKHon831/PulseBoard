-- Templates for expenses that repeat on a schedule, e.g. weekday parking.
--
-- Money is held exactly as it is on `expenses`: `amount` is the base currency,
-- and the rates in force when the template was saved are frozen onto it. Each
-- generated expense copies those figures rather than re-converting, so a fixed
-- RM 5 fee stays RM 5 instead of drifting with the exchange rate.
CREATE TABLE recurring_expenses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    original_amount NUMERIC(14, 4) NOT NULL CHECK (original_amount > 0),
    original_currency VARCHAR(3) NOT NULL,
    exchange_rate NUMERIC(18, 8) NOT NULL CHECK (exchange_rate > 0),
    rate_snapshot VARCHAR(255),
    category VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    -- DAILY | WEEKDAYS | WEEKENDS
    frequency VARCHAR(20) NOT NULL,
    -- Paused rather than deleted, so a template can stop generating without
    -- losing its history.
    active BOOLEAN NOT NULL DEFAULT true,
    start_date DATE NOT NULL,
    -- How far generation has caught up; NULL means nothing generated yet.
    last_generated_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_recurring_expenses_user_id ON recurring_expenses (user_id, created_at DESC);

-- ON DELETE SET NULL, not CASCADE: deleting a template must not delete the
-- expenses it already generated. Those are money that was actually spent.
ALTER TABLE expenses
    ADD COLUMN recurring_id UUID REFERENCES recurring_expenses(id) ON DELETE SET NULL;

-- Makes generation idempotent: running the catch-up twice, or two requests
-- racing each other, cannot charge the same day twice.
CREATE UNIQUE INDEX uq_expenses_recurring_day
    ON expenses (recurring_id, expense_date) WHERE recurring_id IS NOT NULL;
