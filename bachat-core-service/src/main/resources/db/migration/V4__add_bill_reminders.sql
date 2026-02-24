-- Bill Reminders table
CREATE TABLE IF NOT EXISTS bill_reminders (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50),
    amount BIGINT,
    frequency VARCHAR(20) NOT NULL,
    custom_days INTEGER,
    next_due_date DATE NOT NULL,
    reminder_days_before INTEGER DEFAULT 3,
    is_active BOOLEAN DEFAULT TRUE,
    is_auto_pay BOOLEAN DEFAULT FALSE,
    payee VARCHAR(255),
    account_reference VARCHAR(100),
    icon VARCHAR(50),
    color VARCHAR(20),
    notes TEXT,
    last_paid_date DATE,
    last_paid_amount BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_bill_reminder_profile ON bill_reminders(profile_id);
CREATE INDEX IF NOT EXISTS idx_bill_reminder_due_date ON bill_reminders(next_due_date);
CREATE INDEX IF NOT EXISTS idx_bill_reminder_active ON bill_reminders(is_active);
CREATE INDEX IF NOT EXISTS idx_bill_reminder_category ON bill_reminders(category);

