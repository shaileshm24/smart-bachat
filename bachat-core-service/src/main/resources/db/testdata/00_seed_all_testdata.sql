-- ============================================================================
-- SmartBachat Test Data - Master Seed Script
-- ============================================================================
-- 
-- This script inserts 6 months of realistic Indian transaction data for testing.
-- 
-- BEFORE RUNNING:
-- 1. Get your profile_id and user_id from the API:
--    GET /api/profiles/me (with your JWT token)
-- 
-- 2. Replace the placeholder values below with your actual IDs
-- 
-- HOW TO RUN:
--    psql -h localhost -p 5433 -U yugabyte -d smartbachat -f 00_seed_all_testdata.sql
--
-- Or connect to psql and run:
--    \i 00_seed_all_testdata.sql
--
-- ============================================================================

-- ============================================================================
-- CONFIGURATION - REPLACE THESE WITH YOUR ACTUAL IDs!
-- ============================================================================
\set profile_id '''YOUR_PROFILE_ID_HERE'''
\set user_id '''YOUR_USER_ID_HERE'''

-- Example (uncomment and use your actual IDs):
-- \set profile_id '''550e8400-e29b-41d4-a716-446655440001'''
-- \set user_id '''550e8400-e29b-41d4-a716-446655440000'''

-- ============================================================================
-- CLEANUP EXISTING TEST DATA (optional - uncomment to clear first)
-- ============================================================================
-- DELETE FROM transactions WHERE source_type = 'TEST_DATA' AND profile_id = :profile_id::uuid;
-- DELETE FROM savings_goals WHERE profile_id = :profile_id::uuid;

-- ============================================================================
-- DATA SUMMARY
-- ============================================================================
-- 
-- Monthly Income:  ₹77,000 - ₹80,000 (+ ₹40,000 bonus in December)
-- Monthly Expenses: ~₹50,000 - ₹60,000
-- Savings Capacity: ~₹20,000 - ₹30,000/month
--
-- Categories covered:
-- - SALARY (monthly + bonus)
-- - RENT (₹18,000/month)
-- - INVESTMENT (SIP ₹10,000/month)
-- - UTILITIES (Electricity, Gas, Internet)
-- - ENTERTAINMENT (Netflix, Spotify subscriptions)
-- - FOOD (Swiggy, Zomato, restaurants - 10-12 orders/month)
-- - TRANSPORT (Uber, Ola, Fuel - 8-10 rides + 2 fuel fills/month)
-- - SHOPPING (Amazon, Flipkart, Myntra - 3-5 orders/month)
-- - TRANSFER (Mom ₹5,000/month, friends)
--
-- Recurring patterns for testing:
-- - UPI to 9876543210@upi (Mom) - monthly
-- - UPI to priya.sharma@okicici (Friend) - occasional
-- - Netflix/Spotify - monthly subscriptions
-- - Rent to ramesh.kumar@okaxis - monthly
--
-- ============================================================================

\echo '============================================'
\echo 'Starting SmartBachat Test Data Seed...'
\echo '============================================'
\echo ''
\echo 'Profile ID:' :profile_id
\echo 'User ID:' :user_id
\echo ''

-- Run individual scripts
\ir 01_test_transactions.sql
\ir 02_test_transactions_month2.sql
\ir 03_test_transactions_months3to6.sql
\ir 04_test_goals.sql

-- ============================================================================
-- SUMMARY
-- ============================================================================
\echo ''
\echo '============================================'
\echo 'Test Data Seed Complete!'
\echo '============================================'

SELECT 'Transactions' as entity, COUNT(*) as count 
FROM transactions 
WHERE profile_id = :profile_id::uuid AND source_type = 'TEST_DATA'
UNION ALL
SELECT 'Savings Goals' as entity, COUNT(*) as count 
FROM savings_goals 
WHERE profile_id = :profile_id::uuid;

\echo ''
\echo 'Monthly breakdown:'
SELECT 
    TO_CHAR(txn_date, 'YYYY-MM') as month,
    COUNT(*) as transactions,
    SUM(CASE WHEN direction = 'CREDIT' THEN amount ELSE 0 END)/100 as income_rupees,
    SUM(CASE WHEN direction = 'DEBIT' THEN amount ELSE 0 END)/100 as expense_rupees
FROM transactions 
WHERE profile_id = :profile_id::uuid AND source_type = 'TEST_DATA'
GROUP BY TO_CHAR(txn_date, 'YYYY-MM')
ORDER BY month DESC;

\echo ''
\echo 'Now test the APIs:'
\echo '  GET /api/transactions'
\echo '  GET /api/advisor/savings-capacity'
\echo '  POST /api/advisor/goal-recommendation'
\echo '  GET /api/advisor/insights'
\echo ''

