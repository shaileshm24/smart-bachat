-- ============================================================================
-- SmartBachat Test Data - Cleanup Script
-- ============================================================================
-- 
-- This script removes all test data for a specific profile.
-- Only removes transactions with source_type = 'TEST_DATA'
-- 
-- HOW TO RUN:
--    psql -h localhost -p 5433 -U yugabyte -d smartbachat -f 99_cleanup_testdata.sql
--
-- ============================================================================

-- CONFIGURATION - REPLACE WITH YOUR ACTUAL PROFILE ID
\set profile_id '''YOUR_PROFILE_ID_HERE'''

\echo '============================================'
\echo 'Cleaning up test data...'
\echo '============================================'
\echo ''
\echo 'Profile ID:' :profile_id

-- Show what will be deleted
\echo ''
\echo 'Data to be deleted:'
SELECT 'Transactions' as entity, COUNT(*) as count 
FROM transactions 
WHERE profile_id = :profile_id::uuid AND source_type = 'TEST_DATA'
UNION ALL
SELECT 'Savings Goals' as entity, COUNT(*) as count 
FROM savings_goals 
WHERE profile_id = :profile_id::uuid;

-- Delete test transactions
DELETE FROM transactions 
WHERE profile_id = :profile_id::uuid 
AND source_type = 'TEST_DATA';

-- Delete savings goals (all goals for this profile)
DELETE FROM savings_goals 
WHERE profile_id = :profile_id::uuid;

\echo ''
\echo 'Cleanup complete!'
\echo ''

-- Verify deletion
SELECT 'Remaining Transactions' as entity, COUNT(*) as count 
FROM transactions 
WHERE profile_id = :profile_id::uuid
UNION ALL
SELECT 'Remaining Goals' as entity, COUNT(*) as count 
FROM savings_goals 
WHERE profile_id = :profile_id::uuid;

