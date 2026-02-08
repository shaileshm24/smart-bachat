-- ============================================================================
-- SmartBachat Test Data - Month 2 (December 2025)
-- ============================================================================
-- Run after 01_test_transactions.sql

\set profile_id '''YOUR_PROFILE_ID'''
\set user_id '''YOUR_USER_ID'''

-- ============================================================================
-- MONTH 2: December 2025 (1-2 months ago)
-- ============================================================================

-- Salary Credit (₹78,500)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '60 days', 7850000, 'CREDIT', 'INR', 'NEFT', 'NEFT-TECHCORP INDIA PVT LTD-SALARY DEC 2025', 'TECHCORP INDIA', 'SALARY', 'SALARY', 'TEST_DATA', NOW());

-- Rent (₹18,000)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, counterparty_name, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '55 days', 1800000, 'DEBIT', 'INR', 'UPI', 'UPI/ramesh.kumar@okaxis/Rent Payment Dec', 'Ramesh Kumar', 'RENT', 'RENT', 'Ramesh Kumar', 'TEST_DATA', NOW());

-- SIP (₹10,000)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '55 days', 1000000, 'DEBIT', 'INR', 'NACH', 'GROWW SIP AXIS BLUECHIP FUND', 'GROWW', 'INVESTMENT', 'MUTUAL_FUND', 'TEST_DATA', NOW());

-- Utilities
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '52 days', 175000, 'DEBIT', 'INR', 'BILLPAY', 'BESCOM ELECTRICITY BILL PAYMENT', 'BESCOM', 'UTILITIES', 'ELECTRICITY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '50 days', 49900, 'DEBIT', 'INR', 'UPI', 'JIO FIBER MONTHLY RECHARGE', 'JIO', 'UTILITIES', 'INTERNET', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '48 days', 88000, 'DEBIT', 'INR', 'BILLPAY', 'MAHANAGAR GAS BILL PAYMENT', 'MAHANAGAR GAS', 'UTILITIES', 'GAS', 'TEST_DATA', NOW());

-- Subscriptions
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, is_recurring, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '45 days', 19900, 'DEBIT', 'INR', 'CARD', 'NETFLIX SUBSCRIPTION', 'NETFLIX', 'ENTERTAINMENT', 'SUBSCRIPTION', true, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '45 days', 14900, 'DEBIT', 'INR', 'CARD', 'SPOTIFY PREMIUM MONTHLY', 'SPOTIFY', 'ENTERTAINMENT', 'SUBSCRIPTION', true, 'TEST_DATA', NOW());

-- UPI to Mom (₹5,000)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, counterparty_name, is_recurring, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '50 days', 500000, 'DEBIT', 'INR', 'UPI', 'UPI/9876543210@upi/Mom monthly support', 'Mom', 'TRANSFER', 'FAMILY', 'Mom', true, 'TEST_DATA', NOW());

-- Food Orders
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '58 days', 32000, 'DEBIT', 'INR', 'UPI', 'SWIGGY ORDER #SW901234', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '56 days', 28000, 'DEBIT', 'INR', 'UPI', 'ZOMATO ORDER #ZM012345', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '53 days', 45000, 'DEBIT', 'INR', 'UPI', 'SWIGGY ORDER #SW012345', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '51 days', 38000, 'DEBIT', 'INR', 'UPI', 'DOMINOS PIZZA ORDER', 'DOMINOS', 'FOOD', 'RESTAURANT', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '49 days', 29500, 'DEBIT', 'INR', 'UPI', 'ZOMATO ORDER #ZM123456', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '46 days', 41000, 'DEBIT', 'INR', 'UPI', 'SWIGGY ORDER #SW123456', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '44 days', 52000, 'DEBIT', 'INR', 'CARD', 'STARBUCKS HSR LAYOUT', 'STARBUCKS', 'FOOD', 'CAFE', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '41 days', 33000, 'DEBIT', 'INR', 'UPI', 'ZOMATO ORDER #ZM234567', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '38 days', 27000, 'DEBIT', 'INR', 'UPI', 'SWIGGY ORDER #SW234567', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '35 days', 36500, 'DEBIT', 'INR', 'UPI', 'KFC BRIGADE ROAD', 'KFC', 'FOOD', 'RESTAURANT', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '33 days', 31000, 'DEBIT', 'INR', 'UPI', 'ZOMATO ORDER #ZM345678', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '31 days', 39000, 'DEBIT', 'INR', 'UPI', 'SWIGGY ORDER #SW345678', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW());

-- Transport
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '57 days', 19000, 'DEBIT', 'INR', 'UPI', 'UBER TRIP PAYMENT', 'UBER', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '54 days', 23500, 'DEBIT', 'INR', 'UPI', 'OLA CABS RIDE', 'OLA', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '52 days', 255000, 'DEBIT', 'INR', 'CARD', 'HP PETROL PUMP HSR', 'HP PETROL', 'TRANSPORT', 'FUEL', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '49 days', 17500, 'DEBIT', 'INR', 'UPI', 'UBER TRIP PAYMENT', 'UBER', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '46 days', 21000, 'DEBIT', 'INR', 'UPI', 'OLA CABS RIDE', 'OLA', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '43 days', 18500, 'DEBIT', 'INR', 'UPI', 'UBER TRIP PAYMENT', 'UBER', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '40 days', 240000, 'DEBIT', 'INR', 'CARD', 'INDIAN OIL BTM LAYOUT', 'INDIAN OIL', 'TRANSPORT', 'FUEL', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '37 days', 24000, 'DEBIT', 'INR', 'UPI', 'OLA CABS RIDE', 'OLA', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '34 days', 16000, 'DEBIT', 'INR', 'UPI', 'RAPIDO BIKE TAXI', 'RAPIDO', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '32 days', 20500, 'DEBIT', 'INR', 'UPI', 'UBER TRIP PAYMENT', 'UBER', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW());

-- Shopping (December - more shopping due to holidays)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '55 days', 459900, 'DEBIT', 'INR', 'UPI', 'AMAZON ORDER #AMZ234567', 'AMAZON', 'SHOPPING', 'ONLINE', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '50 days', 289900, 'DEBIT', 'INR', 'UPI', 'FLIPKART ORDER #FK890123', 'FLIPKART', 'SHOPPING', 'ONLINE', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '45 days', 549900, 'DEBIT', 'INR', 'CARD', 'CROMA ELECTRONICS', 'CROMA', 'SHOPPING', 'ELECTRONICS', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '40 days', 199900, 'DEBIT', 'INR', 'UPI', 'MYNTRA ORDER #MYN456789', 'MYNTRA', 'SHOPPING', 'CLOTHING', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '35 days', 349900, 'DEBIT', 'INR', 'CARD', 'RELIANCE DIGITAL', 'RELIANCE DIGITAL', 'SHOPPING', 'ELECTRONICS', 'TEST_DATA', NOW());

-- Friend Transfer
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, counterparty_name, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '42 days', 300000, 'DEBIT', 'INR', 'UPI', 'UPI/priya.sharma@okicici/Christmas party share', 'Priya Sharma', 'TRANSFER', 'FRIENDS', 'Priya Sharma', 'TEST_DATA', NOW());

-- Bonus Credit (December bonus - ₹40,000)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '35 days', 4000000, 'CREDIT', 'INR', 'NEFT', 'NEFT-TECHCORP INDIA-PERFORMANCE BONUS Q4', 'TECHCORP INDIA', 'SALARY', 'BONUS', 'TEST_DATA', NOW());

SELECT 'Month 2 (December) transactions inserted' AS status;

