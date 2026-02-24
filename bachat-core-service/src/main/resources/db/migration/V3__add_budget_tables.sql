-- Budget Templates table
CREATE TABLE IF NOT EXISTS budget_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    template_type VARCHAR(50) NOT NULL,
    description TEXT,
    category_allocations TEXT,
    is_system BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    icon VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_budget_template_type ON budget_templates(template_type);
CREATE INDEX IF NOT EXISTS idx_budget_template_active ON budget_templates(is_active);

-- User Budgets table
CREATE TABLE IF NOT EXISTS user_budgets (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL,
    template_id UUID,
    budget_month VARCHAR(7) NOT NULL,
    total_income BIGINT,
    total_budget BIGINT,
    total_spent BIGINT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_user_budget_profile_month UNIQUE (profile_id, budget_month)
);

CREATE INDEX IF NOT EXISTS idx_user_budget_profile ON user_budgets(profile_id);
CREATE INDEX IF NOT EXISTS idx_user_budget_month ON user_budgets(budget_month);
CREATE INDEX IF NOT EXISTS idx_user_budget_status ON user_budgets(status);

-- Budget Categories table
CREATE TABLE IF NOT EXISTS budget_categories (
    id UUID PRIMARY KEY,
    budget_id UUID NOT NULL,
    category VARCHAR(50) NOT NULL,
    budget_limit BIGINT NOT NULL,
    percentage DOUBLE PRECISION,
    spent_amount BIGINT DEFAULT 0,
    icon VARCHAR(50),
    color VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_budget_category_budget ON budget_categories(budget_id);
CREATE INDEX IF NOT EXISTS idx_budget_category_name ON budget_categories(category);

-- Insert default budget templates
INSERT INTO budget_templates (id, name, template_type, description, category_allocations, is_system, is_active, icon)
VALUES 
(
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    '50/30/20 Rule',
    'RULE_50_30_20',
    'A popular budgeting method: 50% for needs, 30% for wants, 20% for savings.',
    '[{"category":"ESSENTIALS","percentage":50,"icon":"home","color":"#4CAF50"},{"category":"LIFESTYLE","percentage":30,"icon":"shopping_bag","color":"#2196F3"},{"category":"SAVINGS","percentage":20,"icon":"savings","color":"#FF9800"}]',
    TRUE,
    TRUE,
    'pie_chart'
),
(
    'b2c3d4e5-f6a7-8901-bcde-f12345678901',
    'Zero-Based Budget',
    'ZERO_BASED',
    'Every rupee has a job. Allocate 100% of income to specific categories.',
    '[{"category":"HOUSING","percentage":25,"icon":"home","color":"#9C27B0"},{"category":"FOOD","percentage":15,"icon":"restaurant","color":"#E91E63"},{"category":"TRANSPORT","percentage":10,"icon":"directions_car","color":"#00BCD4"},{"category":"UTILITIES","percentage":10,"icon":"bolt","color":"#FFC107"},{"category":"HEALTHCARE","percentage":5,"icon":"local_hospital","color":"#F44336"},{"category":"ENTERTAINMENT","percentage":10,"icon":"movie","color":"#3F51B5"},{"category":"SAVINGS","percentage":15,"icon":"savings","color":"#4CAF50"},{"category":"MISCELLANEOUS","percentage":10,"icon":"more_horiz","color":"#607D8B"}]',
    TRUE,
    TRUE,
    'calculate'
),
(
    'c3d4e5f6-a7b8-9012-cdef-123456789012',
    'Aggressive Saver',
    'AGGRESSIVE_SAVER',
    'For those who want to maximize savings. 40% savings, 40% needs, 20% wants.',
    '[{"category":"ESSENTIALS","percentage":40,"icon":"home","color":"#4CAF50"},{"category":"LIFESTYLE","percentage":20,"icon":"shopping_bag","color":"#2196F3"},{"category":"SAVINGS","percentage":40,"icon":"savings","color":"#FF9800"}]',
    TRUE,
    TRUE,
    'trending_up'
),
(
    'd4e5f6a7-b8c9-0123-defa-234567890123',
    'Debt Payoff',
    'DEBT_PAYOFF',
    'Focus on paying off debt while maintaining essentials.',
    '[{"category":"ESSENTIALS","percentage":50,"icon":"home","color":"#4CAF50"},{"category":"DEBT_PAYMENT","percentage":30,"icon":"credit_card","color":"#F44336"},{"category":"LIFESTYLE","percentage":10,"icon":"shopping_bag","color":"#2196F3"},{"category":"EMERGENCY_FUND","percentage":10,"icon":"shield","color":"#FF9800"}]',
    TRUE,
    TRUE,
    'credit_score'
);

