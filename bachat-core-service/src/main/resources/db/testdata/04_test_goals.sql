-- ============================================================================
-- SmartBachat Test Data - Savings Goals
-- ============================================================================

\set profile_id '''YOUR_PROFILE_ID'''

-- ============================================================================
-- SAVINGS GOALS
-- ============================================================================

-- Goal 1: Goa Trip - HIGH priority, 5 months away
-- Target: ₹50,000, Current: ₹10,000 (20% progress)
INSERT INTO savings_goals (
    id, profile_id, name, goal_type, target_amount, current_amount, 
    deadline, priority, status, icon, color, notes,
    suggested_monthly_saving, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    :profile_id::uuid,
    'Goa Trip',
    'TRAVEL',
    5000000,      -- ₹50,000 in paisa
    1000000,      -- ₹10,000 in paisa
    CURRENT_DATE + INTERVAL '5 months',
    'HIGH',
    'ACTIVE',
    '🏖️',
    '#FF6B6B',
    'Beach vacation with friends. Need to book flights and hotel.',
    800000,       -- Suggested: ₹8,000/month
    NOW(),
    NOW()
);

-- Goal 2: Emergency Fund - MEDIUM priority, 12 months
-- Target: ₹3,00,000, Current: ₹75,000 (25% progress)
INSERT INTO savings_goals (
    id, profile_id, name, goal_type, target_amount, current_amount, 
    deadline, priority, status, icon, color, notes,
    suggested_monthly_saving, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    :profile_id::uuid,
    'Emergency Fund',
    'EMERGENCY',
    30000000,     -- ₹3,00,000 in paisa
    7500000,      -- ₹75,000 in paisa
    CURRENT_DATE + INTERVAL '12 months',
    'MEDIUM',
    'ACTIVE',
    '🛡️',
    '#4ECDC4',
    '6 months of expenses as safety net',
    1875000,      -- Suggested: ₹18,750/month
    NOW(),
    NOW()
);

-- Goal 3: MacBook Pro - LOW priority, 3 months
-- Target: ₹1,50,000, Current: ₹50,000 (33% progress)
INSERT INTO savings_goals (
    id, profile_id, name, goal_type, target_amount, current_amount, 
    deadline, priority, status, icon, color, notes,
    suggested_monthly_saving, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    :profile_id::uuid,
    'MacBook Pro M3',
    'GADGET',
    15000000,     -- ₹1,50,000 in paisa
    5000000,      -- ₹50,000 in paisa
    CURRENT_DATE + INTERVAL '3 months',
    'LOW',
    'ACTIVE',
    '💻',
    '#45B7D1',
    'Upgrade for better productivity. M3 Pro 14-inch.',
    3333333,      -- Suggested: ₹33,333/month
    NOW(),
    NOW()
);

-- Goal 4: Wedding Fund - MEDIUM priority, 18 months
-- Target: ₹5,00,000, Current: ₹1,00,000 (20% progress)
INSERT INTO savings_goals (
    id, profile_id, name, goal_type, target_amount, current_amount, 
    deadline, priority, status, icon, color, notes,
    suggested_monthly_saving, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    :profile_id::uuid,
    'Wedding Fund',
    'WEDDING',
    50000000,     -- ₹5,00,000 in paisa
    10000000,     -- ₹1,00,000 in paisa
    CURRENT_DATE + INTERVAL '18 months',
    'MEDIUM',
    'ACTIVE',
    '💍',
    '#F7DC6F',
    'Saving for wedding expenses',
    2222222,      -- Suggested: ₹22,222/month
    NOW(),
    NOW()
);

-- Goal 5: Completed Goal (for testing completed state)
-- Target: ₹20,000, Current: ₹20,000 (100% progress)
INSERT INTO savings_goals (
    id, profile_id, name, goal_type, target_amount, current_amount, 
    deadline, priority, status, icon, color, notes,
    suggested_monthly_saving, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    :profile_id::uuid,
    'New Phone',
    'GADGET',
    2000000,      -- ₹20,000 in paisa
    2000000,      -- ₹20,000 in paisa (completed!)
    CURRENT_DATE - INTERVAL '1 month',
    'HIGH',
    'COMPLETED',
    '📱',
    '#2ECC71',
    'Bought iPhone 15! Goal achieved.',
    0,
    NOW() - INTERVAL '3 months',
    NOW() - INTERVAL '1 month'
);

SELECT 'Savings goals inserted!' AS status;
SELECT id, name, goal_type, 
       target_amount/100 as target_rupees, 
       current_amount/100 as current_rupees,
       status 
FROM savings_goals 
WHERE profile_id = :profile_id::uuid
ORDER BY created_at DESC;

