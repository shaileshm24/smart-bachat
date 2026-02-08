package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.entity.PatternType;
import com.ametsa.smartbachat.entity.TransactionEntity;
import com.ametsa.smartbachat.entity.UserCategoryMapping;
import com.ametsa.smartbachat.repository.UserCategoryMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for auto-categorizing transactions based on narration/merchant.
 * Uses user-defined mappings first, then falls back to keyword matching.
 */
@Service
public class TransactionCategorizationService {

    private static final Logger log = LoggerFactory.getLogger(TransactionCategorizationService.class);

    private final UserCategoryMappingRepository userMappingRepository;

    // Patterns for extracting UPI details
    private static final Pattern MOBILE_NUMBER_PATTERN = Pattern.compile("\\b([6-9]\\d{9})\\b");
    private static final Pattern UPI_ID_PATTERN = Pattern.compile("([a-zA-Z0-9._-]+@[a-zA-Z0-9]+)");

    public TransactionCategorizationService(UserCategoryMappingRepository userMappingRepository) {
        this.userMappingRepository = userMappingRepository;
    }

    // Category constants
    public static final String CATEGORY_FOOD = "FOOD";
    public static final String CATEGORY_GROCERIES = "GROCERIES";
    public static final String CATEGORY_TRANSPORT = "TRANSPORT";
    public static final String CATEGORY_UTILITIES = "UTILITIES";
    public static final String CATEGORY_ENTERTAINMENT = "ENTERTAINMENT";
    public static final String CATEGORY_SHOPPING = "SHOPPING";
    public static final String CATEGORY_HEALTH = "HEALTH";
    public static final String CATEGORY_EDUCATION = "EDUCATION";
    public static final String CATEGORY_INVESTMENT = "INVESTMENT";
    public static final String CATEGORY_TRANSFER = "TRANSFER";
    public static final String CATEGORY_SALARY = "SALARY";
    public static final String CATEGORY_ATM = "ATM";
    public static final String CATEGORY_EMI = "EMI";
    public static final String CATEGORY_INSURANCE = "INSURANCE";
    public static final String CATEGORY_RENT = "RENT";
    public static final String CATEGORY_SUBSCRIPTION = "SUBSCRIPTION";
    public static final String CATEGORY_OTHER = "OTHER";

    // Keyword patterns for each category (order matters - first match wins)
    private static final Map<String, Pattern> CATEGORY_PATTERNS = new LinkedHashMap<>();

    static {
        // Food & Dining
        CATEGORY_PATTERNS.put(CATEGORY_FOOD, Pattern.compile(
                "(swiggy|zomato|dominos|pizza|mcdonald|burger|kfc|starbucks|cafe|restaurant|" +
                "food|dining|eatery|biryani|hotel|dhaba|kitchen|meals|tiffin|canteen|mess)",
                Pattern.CASE_INSENSITIVE));

        // Groceries
        CATEGORY_PATTERNS.put(CATEGORY_GROCERIES, Pattern.compile(
                "(bigbasket|grofers|blinkit|zepto|dmart|reliance\\s*fresh|more\\s*supermarket|" +
                "grocery|supermarket|kirana|vegetables|fruits|provisions|general\\s*store)",
                Pattern.CASE_INSENSITIVE));

        // Transport
        CATEGORY_PATTERNS.put(CATEGORY_TRANSPORT, Pattern.compile(
                "(uber|ola|rapido|metro|irctc|railway|redbus|makemytrip|goibibo|" +
                "petrol|fuel|hp\\s*petrol|indian\\s*oil|bharat\\s*petroleum|shell|" +
                "parking|toll|fastag|cab|taxi|auto)",
                Pattern.CASE_INSENSITIVE));

        // Utilities
        CATEGORY_PATTERNS.put(CATEGORY_UTILITIES, Pattern.compile(
                "(electricity|power|bescom|tata\\s*power|adani|reliance\\s*energy|" +
                "water|gas|piped\\s*gas|mahanagar\\s*gas|indane|bharat\\s*gas|hp\\s*gas|" +
                "broadband|internet|jio|airtel|vodafone|vi|bsnl|act\\s*fibernet|" +
                "mobile\\s*recharge|dth|tata\\s*sky|dish\\s*tv)",
                Pattern.CASE_INSENSITIVE));

        // Entertainment
        CATEGORY_PATTERNS.put(CATEGORY_ENTERTAINMENT, Pattern.compile(
                "(netflix|amazon\\s*prime|hotstar|disney|spotify|youtube|" +
                "bookmyshow|pvr|inox|cinema|movie|theatre|gaming|playstation|xbox|steam)",
                Pattern.CASE_INSENSITIVE));

        // Shopping
        CATEGORY_PATTERNS.put(CATEGORY_SHOPPING, Pattern.compile(
                "(amazon|flipkart|myntra|ajio|nykaa|meesho|snapdeal|" +
                "shoppers\\s*stop|lifestyle|westside|pantaloons|max|h&m|zara|" +
                "decathlon|croma|reliance\\s*digital|vijay\\s*sales)",
                Pattern.CASE_INSENSITIVE));

        // Health
        CATEGORY_PATTERNS.put(CATEGORY_HEALTH, Pattern.compile(
                "(hospital|clinic|doctor|medical|pharmacy|apollo|fortis|max\\s*hospital|" +
                "medplus|netmeds|pharmeasy|1mg|practo|diagnostic|lab|pathology|" +
                "gym|fitness|cult\\.fit|healthify)",
                Pattern.CASE_INSENSITIVE));

        // Education
        CATEGORY_PATTERNS.put(CATEGORY_EDUCATION, Pattern.compile(
                "(school|college|university|tuition|coaching|byjus|unacademy|vedantu|" +
                "upgrad|coursera|udemy|books|stationery|education|fees)",
                Pattern.CASE_INSENSITIVE));

        // Investment
        CATEGORY_PATTERNS.put(CATEGORY_INVESTMENT, Pattern.compile(
                "(mutual\\s*fund|sip|zerodha|groww|upstox|angel|icicidirect|hdfc\\s*securities|" +
                "nps|ppf|fd|fixed\\s*deposit|rd|recurring|investment|dividend|" +
                "stock|share|equity|demat)",
                Pattern.CASE_INSENSITIVE));

        // Insurance
        CATEGORY_PATTERNS.put(CATEGORY_INSURANCE, Pattern.compile(
                "(insurance|lic|hdfc\\s*life|icici\\s*prudential|sbi\\s*life|max\\s*life|" +
                "policy|premium|health\\s*insurance|term\\s*plan|motor\\s*insurance)",
                Pattern.CASE_INSENSITIVE));

        // EMI/Loan
        CATEGORY_PATTERNS.put(CATEGORY_EMI, Pattern.compile(
                "(emi|loan|bajaj\\s*finserv|home\\s*credit|capital\\s*first|" +
                "hdfc\\s*bank\\s*emi|icici\\s*emi|sbi\\s*emi|personal\\s*loan|home\\s*loan|" +
                "car\\s*loan|education\\s*loan|credit\\s*card\\s*payment)",
                Pattern.CASE_INSENSITIVE));

        // Rent
        CATEGORY_PATTERNS.put(CATEGORY_RENT, Pattern.compile(
                "(rent|house\\s*rent|flat\\s*rent|pg|paying\\s*guest|hostel|" +
                "maintenance|society|apartment)",
                Pattern.CASE_INSENSITIVE));

        // Subscription
        CATEGORY_PATTERNS.put(CATEGORY_SUBSCRIPTION, Pattern.compile(
                "(subscription|membership|annual\\s*fee|renewal)",
                Pattern.CASE_INSENSITIVE));

        // ATM
        CATEGORY_PATTERNS.put(CATEGORY_ATM, Pattern.compile(
                "(atm|cash\\s*withdrawal|atw|self\\s*withdrawal)",
                Pattern.CASE_INSENSITIVE));

        // Salary
        CATEGORY_PATTERNS.put(CATEGORY_SALARY, Pattern.compile(
                "(salary|sal\\s*cr|neft\\s*sal|wages|payroll|stipend)",
                Pattern.CASE_INSENSITIVE));

        // Transfer (generic - should be last)
        CATEGORY_PATTERNS.put(CATEGORY_TRANSFER, Pattern.compile(
                "(neft|imps|rtgs|fund\\s*transfer)",
                Pattern.CASE_INSENSITIVE));
    }

    /**
     * Categorize a transaction based on its description/narration.
     * First checks user-defined mappings, then falls back to keyword patterns.
     */
    public String categorize(TransactionEntity transaction) {
        return categorizeWithProfile(transaction, null);
    }

    /**
     * Categorize a transaction with user-defined mappings support.
     * @param transaction The transaction to categorize
     * @param profileId The user's profile ID for looking up custom mappings
     * @return The category string
     */
    public String categorizeWithProfile(TransactionEntity transaction, UUID profileId) {
        String text = buildSearchText(transaction);
        if (text == null || text.isEmpty()) {
            return CATEGORY_OTHER;
        }

        // Use profileId from transaction if not provided
        UUID effectiveProfileId = profileId != null ? profileId : transaction.getProfileId();

        // 1. Check user-defined mappings first (if profile available)
        if (effectiveProfileId != null) {
            Optional<UserCategoryMapping> userMapping = findMatchingUserMapping(transaction, effectiveProfileId, text);
            if (userMapping.isPresent()) {
                UserCategoryMapping mapping = userMapping.get();
                mapping.incrementMatchCount();
                userMappingRepository.save(mapping);
                log.debug("Used user mapping '{}' for transaction {}", mapping.getDisplayName(), transaction.getId());
                return mapping.getCategory();
            }
        }

        // 2. Fall back to keyword-based categorization
        for (Map.Entry<String, Pattern> entry : CATEGORY_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(text).find()) {
                return entry.getKey();
            }
        }

        return CATEGORY_OTHER;
    }

    /**
     * Find a matching user-defined category mapping for the transaction.
     */
    private Optional<UserCategoryMapping> findMatchingUserMapping(
            TransactionEntity transaction, UUID profileId, String searchText) {

        List<UserCategoryMapping> mappings = userMappingRepository.findByProfileIdOrderByMatchCountDesc(profileId);
        if (mappings.isEmpty()) {
            return Optional.empty();
        }

        String lowerText = searchText.toLowerCase();

        // Extract UPI details from transaction
        String mobileNumber = extractMobileNumber(searchText);
        String upiId = extractUpiId(searchText);

        for (UserCategoryMapping mapping : mappings) {
            String patternValue = mapping.getPatternValue().toLowerCase();

            switch (mapping.getPatternType()) {
                case MOBILE_NUMBER:
                    if (mobileNumber != null && mobileNumber.equals(mapping.getPatternValue())) {
                        return Optional.of(mapping);
                    }
                    break;
                case UPI_ID:
                    if (upiId != null && upiId.toLowerCase().equals(patternValue)) {
                        return Optional.of(mapping);
                    }
                    break;
                case COUNTERPARTY_NAME:
                    if (transaction.getCounterpartyName() != null &&
                        transaction.getCounterpartyName().toLowerCase().contains(patternValue)) {
                        return Optional.of(mapping);
                    }
                    break;
                case MERCHANT:
                    if (transaction.getMerchant() != null &&
                        transaction.getMerchant().toLowerCase().contains(patternValue)) {
                        return Optional.of(mapping);
                    }
                    break;
                case DESCRIPTION_KEYWORD:
                    if (lowerText.contains(patternValue)) {
                        return Optional.of(mapping);
                    }
                    break;
            }
        }

        return Optional.empty();
    }

    /**
     * Extract mobile number from transaction text.
     * Indian mobile numbers: 10 digits starting with 6-9
     */
    public String extractMobileNumber(String text) {
        if (text == null) return null;
        Matcher matcher = MOBILE_NUMBER_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extract UPI ID from transaction text.
     * Format: username@bankhandle (e.g., name@okaxis, 9876543210@upi)
     */
    public String extractUpiId(String text) {
        if (text == null) return null;
        Matcher matcher = UPI_ID_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Categorize and update the transaction entity.
     */
    public void categorizeAndUpdate(TransactionEntity transaction) {
        if (transaction.getCategory() == null || transaction.getCategory().isEmpty()) {
            String category = categorizeWithProfile(transaction, transaction.getProfileId());
            transaction.setCategory(category);

            String subCategory = getSubCategory(transaction, category);
            if (subCategory != null) {
                transaction.setSubCategory(subCategory);
            }

            log.debug("Categorized transaction {} as {}/{}",
                    transaction.getId(), category, subCategory);
        }
    }

    /**
     * Build search text from transaction fields.
     */
    private String buildSearchText(TransactionEntity transaction) {
        StringBuilder sb = new StringBuilder();
        if (transaction.getDescription() != null) {
            sb.append(transaction.getDescription()).append(" ");
        }
        if (transaction.getMerchant() != null) {
            sb.append(transaction.getMerchant()).append(" ");
        }
        if (transaction.getCounterpartyName() != null) {
            sb.append(transaction.getCounterpartyName()).append(" ");
        }
        if (transaction.getRawText() != null) {
            sb.append(transaction.getRawText());
        }
        return sb.toString().trim();
    }

    /**
     * Get sub-category based on category and transaction details.
     */
    public String getSubCategory(TransactionEntity transaction, String category) {
        String text = buildSearchText(transaction);
        if (text == null) return null;
        String lowerText = text.toLowerCase();

        switch (category) {
            case CATEGORY_FOOD:
                if (lowerText.contains("swiggy") || lowerText.contains("zomato")) return "FOOD_DELIVERY";
                if (lowerText.contains("cafe") || lowerText.contains("starbucks")) return "CAFE";
                return "RESTAURANT";
            case CATEGORY_TRANSPORT:
                if (lowerText.contains("uber") || lowerText.contains("ola")) return "CAB";
                if (lowerText.contains("petrol") || lowerText.contains("fuel")) return "FUEL";
                if (lowerText.contains("metro") || lowerText.contains("railway")) return "PUBLIC_TRANSPORT";
                return "OTHER_TRANSPORT";
            case CATEGORY_UTILITIES:
                if (lowerText.contains("electricity") || lowerText.contains("power")) return "ELECTRICITY";
                if (lowerText.contains("gas")) return "GAS";
                if (lowerText.contains("water")) return "WATER";
                if (lowerText.contains("internet") || lowerText.contains("broadband")) return "INTERNET";
                if (lowerText.contains("mobile") || lowerText.contains("recharge")) return "MOBILE";
                return "OTHER_UTILITY";
            default:
                return null;
        }
    }
}
