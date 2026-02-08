-- ============================================================================
-- SmartBachat Test Data - Transactions
-- ============================================================================
-- USAGE: 
--   1. Replace 'YOUR_PROFILE_ID' with your actual profile UUID
--   2. Replace 'YOUR_USER_ID' with your actual user UUID  
--   3. Run: psql -h localhost -p 5433 -U yugabyte -d smartbachat -f 01_test_transactions.sql
-- ============================================================================

-- Set your IDs here (get from /api/profiles/me endpoint)
\set profile_id '''YOUR_PROFILE_ID'''
\set user_id '''YOUR_USER_ID'''

-- Or use these sample UUIDs for testing:
-- \set profile_id '''550e8400-e29b-41d4-a716-446655440001'''
-- \set user_id '''550e8400-e29b-41d4-a716-446655440000'''

-- ============================================================================
-- MONTH 1: Current Month (January 2026)
-- ============================================================================

-- Salary Credit - 1st of month (₹80,000)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '29 days', 8000000, 'CREDIT', 'INR', 'NEFT', 'NEFT-TECHCORP INDIA PVT LTD-SALARY JAN 2026', 'TECHCORP INDIA', 'SALARY', 'SALARY', 'TEST_DATA', NOW());

-- Rent - 5th (₹18,000)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, counterparty_name, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '25 days', 1800000, 'DEBIT', 'INR', 'UPI', 'UPI/ramesh.kumar@okaxis/Rent Payment Jan', 'Ramesh Kumar', 'RENT', 'RENT', 'Ramesh Kumar', 'TEST_DATA', NOW());

-- SIP Investment - 5th (₹10,000)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '25 days', 1000000, 'DEBIT', 'INR', 'NACH', 'GROWW SIP AXIS BLUECHIP FUND', 'GROWW', 'INVESTMENT', 'MUTUAL_FUND', 'TEST_DATA', NOW());

-- Electricity Bill (₹1,850)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '22 days', 185000, 'DEBIT', 'INR', 'BILLPAY', 'BESCOM ELECTRICITY BILL PAYMENT', 'BESCOM', 'UTILITIES', 'ELECTRICITY', 'TEST_DATA', NOW());

-- Internet Bill (₹499)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '20 days', 49900, 'DEBIT', 'INR', 'UPI', 'JIO FIBER MONTHLY RECHARGE', 'JIO', 'UTILITIES', 'INTERNET', 'TEST_DATA', NOW());

-- Gas Bill (₹950)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '18 days', 95000, 'DEBIT', 'INR', 'BILLPAY', 'MAHANAGAR GAS BILL PAYMENT', 'MAHANAGAR GAS', 'UTILITIES', 'GAS', 'TEST_DATA', NOW());

-- Netflix (₹199)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, is_recurring, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '15 days', 19900, 'DEBIT', 'INR', 'CARD', 'NETFLIX SUBSCRIPTION', 'NETFLIX', 'ENTERTAINMENT', 'SUBSCRIPTION', true, 'TEST_DATA', NOW());

-- Spotify (₹149)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, is_recurring, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '15 days', 14900, 'DEBIT', 'INR', 'CARD', 'SPOTIFY PREMIUM MONTHLY', 'SPOTIFY', 'ENTERTAINMENT', 'SUBSCRIPTION', true, 'TEST_DATA', NOW());

-- UPI to Mom - Monthly (₹5,000)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, counterparty_name, is_recurring, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '20 days', 500000, 'DEBIT', 'INR', 'UPI', 'UPI/9876543210@upi/Mom monthly support', 'Mom', 'TRANSFER', 'FAMILY', 'Mom', true, 'TEST_DATA', NOW());

-- Food Orders (Swiggy, Zomato)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '28 days', 35000, 'DEBIT', 'INR', 'UPI', 'SWIGGY ORDER #SW123456', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '26 days', 28500, 'DEBIT', 'INR', 'UPI', 'ZOMATO ORDER #ZM789012', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '24 days', 42000, 'DEBIT', 'INR', 'UPI', 'SWIGGY ORDER #SW234567', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '21 days', 31500, 'DEBIT', 'INR', 'UPI', 'DOMINOS PIZZA ORDER', 'DOMINOS', 'FOOD', 'RESTAURANT', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '19 days', 25000, 'DEBIT', 'INR', 'UPI', 'ZOMATO ORDER #ZM345678', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '16 days', 38000, 'DEBIT', 'INR', 'UPI', 'SWIGGY ORDER #SW456789', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '14 days', 45000, 'DEBIT', 'INR', 'CARD', 'STARBUCKS KORAMANGALA', 'STARBUCKS', 'FOOD', 'CAFE', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '11 days', 29000, 'DEBIT', 'INR', 'UPI', 'ZOMATO ORDER #ZM567890', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '8 days', 33500, 'DEBIT', 'INR', 'UPI', 'SWIGGY ORDER #SW678901', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '5 days', 27500, 'DEBIT', 'INR', 'UPI', 'MCDONALDS INDIRANAGAR', 'MCDONALDS', 'FOOD', 'RESTAURANT', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '3 days', 36000, 'DEBIT', 'INR', 'UPI', 'ZOMATO ORDER #ZM678901', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '1 day', 41000, 'DEBIT', 'INR', 'UPI', 'SWIGGY ORDER #SW789012', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW());

-- Transport (Uber, Ola, Fuel)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '27 days', 18500, 'DEBIT', 'INR', 'UPI', 'UBER TRIP PAYMENT', 'UBER', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '25 days', 22000, 'DEBIT', 'INR', 'UPI', 'OLA CABS RIDE', 'OLA', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '23 days', 245000, 'DEBIT', 'INR', 'CARD', 'HP PETROL PUMP KORAMANGALA', 'HP PETROL', 'TRANSPORT', 'FUEL', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '20 days', 19500, 'DEBIT', 'INR', 'UPI', 'UBER TRIP PAYMENT', 'UBER', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '17 days', 25000, 'DEBIT', 'INR', 'UPI', 'OLA CABS RIDE', 'OLA', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '13 days', 17000, 'DEBIT', 'INR', 'UPI', 'UBER TRIP PAYMENT', 'UBER', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '10 days', 230000, 'DEBIT', 'INR', 'CARD', 'INDIAN OIL INDIRANAGAR', 'INDIAN OIL', 'TRANSPORT', 'FUEL', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '7 days', 21500, 'DEBIT', 'INR', 'UPI', 'OLA CABS RIDE', 'OLA', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '4 days', 16500, 'DEBIT', 'INR', 'UPI', 'UBER TRIP PAYMENT', 'UBER', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '2 days', 23000, 'DEBIT', 'INR', 'UPI', 'RAPIDO BIKE TAXI', 'RAPIDO', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW());

-- Shopping
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '22 days', 289900, 'DEBIT', 'INR', 'UPI', 'AMAZON ORDER #AMZ123456', 'AMAZON', 'SHOPPING', 'ONLINE', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '15 days', 159900, 'DEBIT', 'INR', 'UPI', 'FLIPKART ORDER #FK789012', 'FLIPKART', 'SHOPPING', 'ONLINE', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '9 days', 349900, 'DEBIT', 'INR', 'CARD', 'MYNTRA ORDER #MYN345678', 'MYNTRA', 'SHOPPING', 'CLOTHING', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '6 days', 199900, 'DEBIT', 'INR', 'UPI', 'DECATHLON SPORTS', 'DECATHLON', 'SHOPPING', 'SPORTS', 'TEST_DATA', NOW());

-- Friend Transfer (recurring pattern for testing)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, counterparty_name, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '12 days', 250000, 'DEBIT', 'INR', 'UPI', 'UPI/priya.sharma@okicici/Shared dinner expense', 'Priya Sharma', 'TRANSFER', 'FRIENDS', 'Priya Sharma', 'TEST_DATA', NOW());

SELECT 'Month 1 (Current) transactions inserted' AS status;

