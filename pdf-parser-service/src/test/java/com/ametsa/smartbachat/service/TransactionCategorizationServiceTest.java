package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.entity.TransactionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionCategorizationServiceTest {

    private TransactionCategorizationService service;

    @BeforeEach
    void setUp() {
        service = new TransactionCategorizationService();
    }

    @Nested
    @DisplayName("Food Category Tests")
    class FoodCategoryTests {
        @ParameterizedTest
        @CsvSource({
            "SWIGGY ORDER 12345, FOOD", "Zomato Payment, FOOD", "DOMINOS PIZZA, FOOD",
            "McDonald's Restaurant, FOOD", "KFC INDIA, FOOD", "Starbucks Coffee, FOOD"
        })
        void shouldCategorizeFoodTransactions(String description, String expectedCategory) {
            assertEquals(expectedCategory, service.categorize(createTransaction(description)));
        }

        @Test
        void shouldReturnFoodDeliverySubCategory() {
            TransactionEntity txn = createTransaction("SWIGGY ORDER 12345");
            service.categorizeAndUpdate(txn);
            assertEquals("FOOD", txn.getCategory());
            assertEquals("FOOD_DELIVERY", txn.getSubCategory());
        }
    }

    @Nested
    @DisplayName("Transport Category Tests")
    class TransportCategoryTests {
        @ParameterizedTest
        @CsvSource({
            "UBER TRIP, TRANSPORT", "OLA CABS, TRANSPORT", "HP PETROL PUMP, TRANSPORT",
            "INDIAN OIL, TRANSPORT", "FASTAG RECHARGE, TRANSPORT", "METRO CARD, TRANSPORT"
        })
        void shouldCategorizeTransportTransactions(String description, String expectedCategory) {
            assertEquals(expectedCategory, service.categorize(createTransaction(description)));
        }

        @Test
        void shouldReturnCabSubCategory() {
            TransactionEntity txn = createTransaction("UBER TRIP PAYMENT");
            service.categorizeAndUpdate(txn);
            assertEquals("CAB", txn.getSubCategory());
        }

        @Test
        void shouldReturnFuelSubCategory() {
            TransactionEntity txn = createTransaction("HP PETROL PUMP");
            service.categorizeAndUpdate(txn);
            assertEquals("FUEL", txn.getSubCategory());
        }
    }

    @Nested
    @DisplayName("Utilities Category Tests")
    class UtilitiesCategoryTests {
        @ParameterizedTest
        @CsvSource({
            "BESCOM ELECTRICITY BILL, UTILITIES", "TATA POWER PAYMENT, UTILITIES",
            "MAHANAGAR GAS BILL, UTILITIES", "JIO RECHARGE, UTILITIES", "ACT FIBERNET, UTILITIES"
        })
        void shouldCategorizeUtilityTransactions(String description, String expectedCategory) {
            assertEquals(expectedCategory, service.categorize(createTransaction(description)));
        }

        @Test
        void shouldReturnElectricitySubCategory() {
            TransactionEntity txn = createTransaction("BESCOM ELECTRICITY BILL");
            service.categorizeAndUpdate(txn);
            assertEquals("ELECTRICITY", txn.getSubCategory());
        }
    }

    @Nested
    @DisplayName("Financial Category Tests")
    class FinancialCategoryTests {
        @ParameterizedTest
        @CsvSource({
            "SALARY CREDIT, SALARY", "NEFT SAL COMPANY, SALARY", "MUTUAL FUND SIP, INVESTMENT",
            "ZERODHA TRADING, INVESTMENT", "LIC PREMIUM, INSURANCE", "HOME LOAN EMI, EMI"
        })
        void shouldCategorizeFinancialTransactions(String description, String expectedCategory) {
            assertEquals(expectedCategory, service.categorize(createTransaction(description)));
        }
    }

    @Nested
    @DisplayName("Other Categories Tests")
    class OtherCategoriesTests {
        @ParameterizedTest
        @CsvSource({
            "ATM CASH WITHDRAWAL, ATM", "NETFLIX SUBSCRIPTION, ENTERTAINMENT",
            "APOLLO HOSPITAL, HEALTH", "SCHOOL FEES, EDUCATION", "HOUSE RENT, RENT",
            "AMAZON PAY, SHOPPING", "FLIPKART ORDER, SHOPPING", "NEFT TRANSFER, TRANSFER"
        })
        void shouldCategorizeOtherTransactions(String description, String expectedCategory) {
            assertEquals(expectedCategory, service.categorize(createTransaction(description)));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {
        @Test
        void shouldReturnOtherForUnknownDescription() {
            assertEquals("OTHER", service.categorize(createTransaction("RANDOM XYZ123")));
        }

        @Test
        void shouldReturnOtherForNullDescription() {
            TransactionEntity txn = new TransactionEntity();
            txn.setId(UUID.randomUUID());
            assertEquals("OTHER", service.categorize(txn));
        }

        @Test
        void shouldNotOverwriteExistingCategory() {
            TransactionEntity txn = createTransaction("SWIGGY ORDER");
            txn.setCategory("CUSTOM");
            service.categorizeAndUpdate(txn);
            assertEquals("CUSTOM", txn.getCategory());
        }

        @Test
        void shouldBeCaseInsensitive() {
            assertEquals("FOOD", service.categorize(createTransaction("swiggy order")));
            assertEquals("FOOD", service.categorize(createTransaction("SWIGGY ORDER")));
        }

        @Test
        void shouldUseMerchantField() {
            TransactionEntity txn = new TransactionEntity();
            txn.setId(UUID.randomUUID());
            txn.setMerchant("SWIGGY");
            assertEquals("FOOD", service.categorize(txn));
        }
    }

    private TransactionEntity createTransaction(String description) {
        TransactionEntity txn = new TransactionEntity();
        txn.setId(UUID.randomUUID());
        txn.setDescription(description);
        return txn;
    }
}

