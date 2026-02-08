-- ============================================================================
-- SmartBachat Test Data - Months 3-6 (Nov, Oct, Sep, Aug 2025)
-- ============================================================================

\set profile_id '''YOUR_PROFILE_ID'''
\set user_id '''YOUR_USER_ID'''

-- ============================================================================
-- MONTH 3: November 2025 (2-3 months ago)
-- ============================================================================

-- Salary (₹79,000)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES (gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '90 days', 7900000, 'CREDIT', 'INR', 'NEFT', 'NEFT-TECHCORP INDIA PVT LTD-SALARY NOV 2025', 'TECHCORP INDIA', 'SALARY', 'SALARY', 'TEST_DATA', NOW());

-- Fixed expenses
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, counterparty_name, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '85 days', 1800000, 'DEBIT', 'INR', 'UPI', 'UPI/ramesh.kumar@okaxis/Rent Nov', 'Ramesh Kumar', 'RENT', 'RENT', 'Ramesh Kumar', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '85 days', 1000000, 'DEBIT', 'INR', 'NACH', 'GROWW SIP AXIS BLUECHIP FUND', 'GROWW', 'INVESTMENT', 'MUTUAL_FUND', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '80 days', 500000, 'DEBIT', 'INR', 'UPI', 'UPI/9876543210@upi/Mom monthly', 'Mom', 'TRANSFER', 'FAMILY', 'Mom', 'TEST_DATA', NOW());

-- Utilities & Subscriptions
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '82 days', 168000, 'DEBIT', 'INR', 'BILLPAY', 'BESCOM ELECTRICITY BILL', 'BESCOM', 'UTILITIES', 'ELECTRICITY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '80 days', 49900, 'DEBIT', 'INR', 'UPI', 'JIO FIBER RECHARGE', 'JIO', 'UTILITIES', 'INTERNET', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '75 days', 19900, 'DEBIT', 'INR', 'CARD', 'NETFLIX SUBSCRIPTION', 'NETFLIX', 'ENTERTAINMENT', 'SUBSCRIPTION', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '75 days', 14900, 'DEBIT', 'INR', 'CARD', 'SPOTIFY PREMIUM', 'SPOTIFY', 'ENTERTAINMENT', 'SUBSCRIPTION', 'TEST_DATA', NOW());

-- Food (10 orders)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '88 days', 34000, 'DEBIT', 'INR', 'UPI', 'SWIGGY ORDER', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '85 days', 29000, 'DEBIT', 'INR', 'UPI', 'ZOMATO ORDER', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '82 days', 42000, 'DEBIT', 'INR', 'UPI', 'SWIGGY ORDER', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '79 days', 31000, 'DEBIT', 'INR', 'UPI', 'ZOMATO ORDER', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '76 days', 38000, 'DEBIT', 'INR', 'UPI', 'SWIGGY ORDER', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '73 days', 27000, 'DEBIT', 'INR', 'UPI', 'ZOMATO ORDER', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '70 days', 35000, 'DEBIT', 'INR', 'UPI', 'SWIGGY ORDER', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '67 days', 44000, 'DEBIT', 'INR', 'CARD', 'STARBUCKS', 'STARBUCKS', 'FOOD', 'CAFE', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '64 days', 32000, 'DEBIT', 'INR', 'UPI', 'ZOMATO ORDER', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '61 days', 39000, 'DEBIT', 'INR', 'UPI', 'SWIGGY ORDER', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', 'TEST_DATA', NOW());

-- Transport (8 rides + 2 fuel)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '87 days', 18000, 'DEBIT', 'INR', 'UPI', 'UBER TRIP', 'UBER', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '84 days', 22000, 'DEBIT', 'INR', 'UPI', 'OLA CABS', 'OLA', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '81 days', 235000, 'DEBIT', 'INR', 'CARD', 'HP PETROL', 'HP PETROL', 'TRANSPORT', 'FUEL', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '78 days', 19500, 'DEBIT', 'INR', 'UPI', 'UBER TRIP', 'UBER', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '75 days', 24000, 'DEBIT', 'INR', 'UPI', 'OLA CABS', 'OLA', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '72 days', 17000, 'DEBIT', 'INR', 'UPI', 'UBER TRIP', 'UBER', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '69 days', 228000, 'DEBIT', 'INR', 'CARD', 'INDIAN OIL', 'INDIAN OIL', 'TRANSPORT', 'FUEL', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '66 days', 21000, 'DEBIT', 'INR', 'UPI', 'OLA CABS', 'OLA', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '63 days', 16500, 'DEBIT', 'INR', 'UPI', 'RAPIDO', 'RAPIDO', 'TRANSPORT', 'CAB', 'TEST_DATA', NOW());

-- Shopping (3 orders)
INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, source_type, created_at)
VALUES 
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '83 days', 279900, 'DEBIT', 'INR', 'UPI', 'AMAZON ORDER', 'AMAZON', 'SHOPPING', 'ONLINE', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '74 days', 189900, 'DEBIT', 'INR', 'UPI', 'FLIPKART ORDER', 'FLIPKART', 'SHOPPING', 'ONLINE', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '65 days', 149900, 'DEBIT', 'INR', 'UPI', 'MYNTRA ORDER', 'MYNTRA', 'SHOPPING', 'CLOTHING', 'TEST_DATA', NOW());

-- ============================================================================
-- MONTH 4: October 2025 (3-4 months ago)
-- ============================================================================

INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, counterparty_name, source_type, created_at)
VALUES 
-- Salary
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '120 days', 7800000, 'CREDIT', 'INR', 'NEFT', 'NEFT-TECHCORP INDIA-SALARY OCT 2025', 'TECHCORP INDIA', 'SALARY', 'SALARY', NULL, 'TEST_DATA', NOW()),
-- Fixed
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '115 days', 1800000, 'DEBIT', 'INR', 'UPI', 'UPI/ramesh.kumar@okaxis/Rent Oct', 'Ramesh Kumar', 'RENT', 'RENT', 'Ramesh Kumar', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '115 days', 1000000, 'DEBIT', 'INR', 'NACH', 'GROWW SIP', 'GROWW', 'INVESTMENT', 'MUTUAL_FUND', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '110 days', 500000, 'DEBIT', 'INR', 'UPI', 'UPI/9876543210@upi/Mom', 'Mom', 'TRANSFER', 'FAMILY', 'Mom', 'TEST_DATA', NOW()),
-- Utilities
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '112 days', 172000, 'DEBIT', 'INR', 'BILLPAY', 'BESCOM ELECTRICITY', 'BESCOM', 'UTILITIES', 'ELECTRICITY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '110 days', 49900, 'DEBIT', 'INR', 'UPI', 'JIO FIBER', 'JIO', 'UTILITIES', 'INTERNET', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '105 days', 19900, 'DEBIT', 'INR', 'CARD', 'NETFLIX', 'NETFLIX', 'ENTERTAINMENT', 'SUBSCRIPTION', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '105 days', 14900, 'DEBIT', 'INR', 'CARD', 'SPOTIFY', 'SPOTIFY', 'ENTERTAINMENT', 'SUBSCRIPTION', NULL, 'TEST_DATA', NOW()),
-- Food (8 orders)
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '118 days', 33000, 'DEBIT', 'INR', 'UPI', 'SWIGGY', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '114 days', 28000, 'DEBIT', 'INR', 'UPI', 'ZOMATO', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '110 days', 41000, 'DEBIT', 'INR', 'UPI', 'SWIGGY', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '106 days', 35000, 'DEBIT', 'INR', 'UPI', 'ZOMATO', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '102 days', 29000, 'DEBIT', 'INR', 'UPI', 'SWIGGY', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '98 days', 37000, 'DEBIT', 'INR', 'UPI', 'ZOMATO', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '94 days', 32000, 'DEBIT', 'INR', 'UPI', 'SWIGGY', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '91 days', 26000, 'DEBIT', 'INR', 'UPI', 'ZOMATO', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
-- Transport
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '116 days', 17500, 'DEBIT', 'INR', 'UPI', 'UBER', 'UBER', 'TRANSPORT', 'CAB', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '112 days', 21000, 'DEBIT', 'INR', 'UPI', 'OLA', 'OLA', 'TRANSPORT', 'CAB', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '108 days', 242000, 'DEBIT', 'INR', 'CARD', 'HP PETROL', 'HP PETROL', 'TRANSPORT', 'FUEL', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '104 days', 19000, 'DEBIT', 'INR', 'UPI', 'UBER', 'UBER', 'TRANSPORT', 'CAB', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '100 days', 23000, 'DEBIT', 'INR', 'UPI', 'OLA', 'OLA', 'TRANSPORT', 'CAB', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '96 days', 225000, 'DEBIT', 'INR', 'CARD', 'INDIAN OIL', 'INDIAN OIL', 'TRANSPORT', 'FUEL', NULL, 'TEST_DATA', NOW()),
-- Shopping
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '113 days', 319900, 'DEBIT', 'INR', 'UPI', 'AMAZON', 'AMAZON', 'SHOPPING', 'ONLINE', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '103 days', 169900, 'DEBIT', 'INR', 'UPI', 'FLIPKART', 'FLIPKART', 'SHOPPING', 'ONLINE', NULL, 'TEST_DATA', NOW());

-- ============================================================================
-- MONTH 5: September 2025 (4-5 months ago)
-- ============================================================================

INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, counterparty_name, source_type, created_at)
VALUES
-- Salary
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '150 days', 7750000, 'CREDIT', 'INR', 'NEFT', 'NEFT-TECHCORP INDIA-SALARY SEP 2025', 'TECHCORP INDIA', 'SALARY', 'SALARY', NULL, 'TEST_DATA', NOW()),
-- Fixed
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '145 days', 1800000, 'DEBIT', 'INR', 'UPI', 'UPI/ramesh.kumar@okaxis/Rent Sep', 'Ramesh Kumar', 'RENT', 'RENT', 'Ramesh Kumar', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '145 days', 1000000, 'DEBIT', 'INR', 'NACH', 'GROWW SIP', 'GROWW', 'INVESTMENT', 'MUTUAL_FUND', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '140 days', 500000, 'DEBIT', 'INR', 'UPI', 'UPI/9876543210@upi/Mom', 'Mom', 'TRANSFER', 'FAMILY', 'Mom', 'TEST_DATA', NOW()),
-- Utilities
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '142 days', 165000, 'DEBIT', 'INR', 'BILLPAY', 'BESCOM ELECTRICITY', 'BESCOM', 'UTILITIES', 'ELECTRICITY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '140 days', 49900, 'DEBIT', 'INR', 'UPI', 'JIO FIBER', 'JIO', 'UTILITIES', 'INTERNET', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '135 days', 19900, 'DEBIT', 'INR', 'CARD', 'NETFLIX', 'NETFLIX', 'ENTERTAINMENT', 'SUBSCRIPTION', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '135 days', 14900, 'DEBIT', 'INR', 'CARD', 'SPOTIFY', 'SPOTIFY', 'ENTERTAINMENT', 'SUBSCRIPTION', NULL, 'TEST_DATA', NOW()),
-- Food
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '148 days', 31000, 'DEBIT', 'INR', 'UPI', 'SWIGGY', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '144 days', 27000, 'DEBIT', 'INR', 'UPI', 'ZOMATO', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '140 days', 38000, 'DEBIT', 'INR', 'UPI', 'SWIGGY', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '136 days', 33000, 'DEBIT', 'INR', 'UPI', 'ZOMATO', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '132 days', 29000, 'DEBIT', 'INR', 'UPI', 'SWIGGY', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '128 days', 35000, 'DEBIT', 'INR', 'UPI', 'ZOMATO', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '124 days', 42000, 'DEBIT', 'INR', 'CARD', 'STARBUCKS', 'STARBUCKS', 'FOOD', 'CAFE', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '121 days', 28000, 'DEBIT', 'INR', 'UPI', 'SWIGGY', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
-- Transport
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '146 days', 18500, 'DEBIT', 'INR', 'UPI', 'UBER', 'UBER', 'TRANSPORT', 'CAB', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '142 days', 22500, 'DEBIT', 'INR', 'UPI', 'OLA', 'OLA', 'TRANSPORT', 'CAB', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '138 days', 238000, 'DEBIT', 'INR', 'CARD', 'HP PETROL', 'HP PETROL', 'TRANSPORT', 'FUEL', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '134 days', 17000, 'DEBIT', 'INR', 'UPI', 'UBER', 'UBER', 'TRANSPORT', 'CAB', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '130 days', 20000, 'DEBIT', 'INR', 'UPI', 'OLA', 'OLA', 'TRANSPORT', 'CAB', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '126 days', 232000, 'DEBIT', 'INR', 'CARD', 'INDIAN OIL', 'INDIAN OIL', 'TRANSPORT', 'FUEL', NULL, 'TEST_DATA', NOW()),
-- Shopping
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '143 days', 259900, 'DEBIT', 'INR', 'UPI', 'AMAZON', 'AMAZON', 'SHOPPING', 'ONLINE', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '133 days', 179900, 'DEBIT', 'INR', 'UPI', 'FLIPKART', 'FLIPKART', 'SHOPPING', 'ONLINE', NULL, 'TEST_DATA', NOW());

-- ============================================================================
-- MONTH 6: August 2025 (5-6 months ago)
-- ============================================================================

INSERT INTO transactions (id, user_id, profile_id, txn_date, amount, direction, currency, txn_type, description, merchant, category, sub_category, counterparty_name, source_type, created_at)
VALUES
-- Salary
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '180 days', 7700000, 'CREDIT', 'INR', 'NEFT', 'NEFT-TECHCORP INDIA-SALARY AUG 2025', 'TECHCORP INDIA', 'SALARY', 'SALARY', NULL, 'TEST_DATA', NOW()),
-- Fixed
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '175 days', 1800000, 'DEBIT', 'INR', 'UPI', 'UPI/ramesh.kumar@okaxis/Rent Aug', 'Ramesh Kumar', 'RENT', 'RENT', 'Ramesh Kumar', 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '175 days', 1000000, 'DEBIT', 'INR', 'NACH', 'GROWW SIP', 'GROWW', 'INVESTMENT', 'MUTUAL_FUND', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '170 days', 500000, 'DEBIT', 'INR', 'UPI', 'UPI/9876543210@upi/Mom', 'Mom', 'TRANSFER', 'FAMILY', 'Mom', 'TEST_DATA', NOW()),
-- Utilities
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '172 days', 158000, 'DEBIT', 'INR', 'BILLPAY', 'BESCOM ELECTRICITY', 'BESCOM', 'UTILITIES', 'ELECTRICITY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '170 days', 49900, 'DEBIT', 'INR', 'UPI', 'JIO FIBER', 'JIO', 'UTILITIES', 'INTERNET', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '165 days', 19900, 'DEBIT', 'INR', 'CARD', 'NETFLIX', 'NETFLIX', 'ENTERTAINMENT', 'SUBSCRIPTION', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '165 days', 14900, 'DEBIT', 'INR', 'CARD', 'SPOTIFY', 'SPOTIFY', 'ENTERTAINMENT', 'SUBSCRIPTION', NULL, 'TEST_DATA', NOW()),
-- Food
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '178 days', 30000, 'DEBIT', 'INR', 'UPI', 'SWIGGY', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '174 days', 26000, 'DEBIT', 'INR', 'UPI', 'ZOMATO', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '170 days', 36000, 'DEBIT', 'INR', 'UPI', 'SWIGGY', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '166 days', 32000, 'DEBIT', 'INR', 'UPI', 'ZOMATO', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '162 days', 28000, 'DEBIT', 'INR', 'UPI', 'SWIGGY', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '158 days', 34000, 'DEBIT', 'INR', 'UPI', 'ZOMATO', 'ZOMATO', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '154 days', 40000, 'DEBIT', 'INR', 'CARD', 'STARBUCKS', 'STARBUCKS', 'FOOD', 'CAFE', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '151 days', 25000, 'DEBIT', 'INR', 'UPI', 'SWIGGY', 'SWIGGY', 'FOOD', 'FOOD_DELIVERY', NULL, 'TEST_DATA', NOW()),
-- Transport
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '176 days', 17000, 'DEBIT', 'INR', 'UPI', 'UBER', 'UBER', 'TRANSPORT', 'CAB', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '172 days', 21000, 'DEBIT', 'INR', 'UPI', 'OLA', 'OLA', 'TRANSPORT', 'CAB', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '168 days', 230000, 'DEBIT', 'INR', 'CARD', 'HP PETROL', 'HP PETROL', 'TRANSPORT', 'FUEL', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '164 days', 16000, 'DEBIT', 'INR', 'UPI', 'UBER', 'UBER', 'TRANSPORT', 'CAB', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '160 days', 19500, 'DEBIT', 'INR', 'UPI', 'OLA', 'OLA', 'TRANSPORT', 'CAB', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '156 days', 225000, 'DEBIT', 'INR', 'CARD', 'INDIAN OIL', 'INDIAN OIL', 'TRANSPORT', 'FUEL', NULL, 'TEST_DATA', NOW()),
-- Shopping
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '173 days', 239900, 'DEBIT', 'INR', 'UPI', 'AMAZON', 'AMAZON', 'SHOPPING', 'ONLINE', NULL, 'TEST_DATA', NOW()),
(gen_random_uuid(), :user_id::uuid, :profile_id::uuid, CURRENT_DATE - INTERVAL '163 days', 159900, 'DEBIT', 'INR', 'UPI', 'FLIPKART', 'FLIPKART', 'SHOPPING', 'ONLINE', NULL, 'TEST_DATA', NOW());

SELECT 'All 6 months of transactions inserted!' AS status;

