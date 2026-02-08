package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.entity.StatementMetadata;
import com.ametsa.smartbachat.entity.TransactionEntity;
import com.ametsa.smartbachat.parser.HdfcPdfParser;
import com.ametsa.smartbachat.repository.StatementMetadataRepository;
import com.ametsa.smartbachat.repository.TransactionRepository;
import com.ametsa.smartbachat.util.BankDetectorUtil;
import com.ametsa.smartbachat.util.PdfParserStrategy;
import com.ametsa.smartbachat.util.PdfUtil;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ParserWorker {

    private static final Logger log = LoggerFactory.getLogger(ParserWorker.class);
    private final Storage storage;
    private final StatementMetadataRepository metadataRepository;
    private final TransactionRepository transactionRepository;
    private final ParserFactory parserFactory;

    public ParserWorker(Storage storage,
                        StatementMetadataRepository metadataRepository,
                        TransactionRepository transactionRepository,
                        ParserFactory parserFactory) {
        this.storage = storage;
        this.metadataRepository = metadataRepository;
        this.transactionRepository = transactionRepository;
        this.parserFactory = parserFactory;
    }

    /**
     * This method is the core worker that is invoked when a Pub/Sub message is received.
     * It streams the PDF from GCS, checks for encryption, selects parser, extracts transactions,
     * batch inserts them into Yugabyte (Postgres-compatible).
     *
     * Note: in production, do this asynchronously and with backoff, retries and proper error handling.
     */
    public void processJob(String jobIdStr, String objectPath) {
        UUID jobId = UUID.fromString(jobIdStr);
        StatementMetadata meta = metadataRepository.findById(jobId).orElseThrow();

        meta.setStatus("PROCESSING");
        meta.setUpdatedAt(Instant.now());
        metadataRepository.save(meta);

        Blob blob = storage.get(meta.getObjectPath(), meta.getBucketName());
        if (blob == null) {
            meta.setStatus("FAILED");
            meta.setErrorMessage("Object not found: " + meta.getObjectPath());
            metadataRepository.save(meta);
            return;
        }

        byte[] pdfBytes = blob.getContent();
        try (InputStream pdfStream = new ByteArrayInputStream(pdfBytes)) {
            // Use PDFBox streaming load with temp-file spill
            PDDocument doc;
            try {
                doc = Loader.loadPDF((RandomAccessRead) pdfStream);
            } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException ipe) {
                meta.setStatus("PASSWORD_REQUIRED");
                meta.setUpdatedAt(Instant.now());
                metadataRepository.save(meta);
                return;
            }

            String firstPages = PdfUtil.extractTextFromPages(doc, 1, Math.min(3, doc.getNumberOfPages()));
            String bank = BankDetectorUtil.detectBank(firstPages);
            PdfParserStrategy parser = parserFactory.getParser(bank);

            List<TransactionEntity> buffer = new ArrayList<>();
            int total = doc.getNumberOfPages();
            meta.setUpdatedAt(Instant.now());
            metadataRepository.save(meta);

            Long openingBalancePaisa = null;
            String documentText = PdfUtil.extractTextFromPages(doc, 1, total);
            if (parser.extractOpeningBalance(documentText) != null) {
                openingBalancePaisa = parser.extractOpeningBalance(documentText);
                log.info("[GCS job] openingBalancePaisa for bank {}: {}", bank, openingBalancePaisa);
            }

            if (parser.requiresFullDocumentText()) {
                List<TransactionEntity> txns = parser.parse(documentText, openingBalancePaisa);
                for (TransactionEntity t : txns) {
                    t.setStatementId(jobId);
                    t.setUserId(meta.getUserId());
                    t.setProfileId(meta.getProfileId());
                    t.setCreatedAt(Instant.now());
                    if (t.getId() == null) t.setId(UUID.randomUUID());
                    buffer.add(t);
                    if (buffer.size() >= 200) {
                        transactionRepository.saveAll(buffer);
                        buffer.clear();
                    }
                }
            } else {
                for (int i = 0; i < total; i++) {
                    String pageText = PdfUtil.extractTextFromPages(doc, i + 1, i + 1);
                    List<TransactionEntity> txns = parser.parse(pageText, openingBalancePaisa);
                    for (TransactionEntity t : txns) {
                        t.setStatementId(jobId);
                        t.setUserId(meta.getUserId());
                        t.setProfileId(meta.getProfileId());
                        t.setCreatedAt(Instant.now());
                        if (t.getId() == null) t.setId(UUID.randomUUID());
                        buffer.add(t);
                    }
                    if (buffer.size() >= 200) {
                        transactionRepository.saveAll(buffer);
                        buffer.clear();
                    }
                }
            }

            if (!buffer.isEmpty()) transactionRepository.saveAll(buffer);

            meta.setStatus("DONE");
            meta.setUpdatedAt(Instant.now());
            metadataRepository.save(meta);
            doc.close();
        } catch (Exception ex) {
            meta.setStatus("FAILED");
            meta.setErrorMessage(ex.getMessage());
            meta.setUpdatedAt(Instant.now());
            metadataRepository.save(meta);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Process a PDF file from local filesystem directly (without password).
     * Delegates to the password-aware overload with null password.
     */
    public UUID processLocalFile(String filePath, UUID userId, UUID profileId, String filename) throws Exception {
        return processLocalFile(filePath, userId, profileId, filename, null);
    }

    /**
     * Process a PDF file from local filesystem directly.
     * Parses the PDF, extracts transactions, and stores them in the database.
     *
     * @param filePath Path to the local PDF file
     * @param userId User ID from UAM service
     * @param profileId Profile ID for the transactions
     * @param filename Original filename
     * @param password Optional password for encrypted PDFs (can be null)
     * @return UUID of the created statement/job
     */
    public UUID processLocalFile(String filePath, UUID userId, UUID profileId, String filename, String password) throws Exception {
        UUID jobId = UUID.randomUUID();

        // Create metadata entry
        StatementMetadata meta = new StatementMetadata();
        meta.setId(jobId);
        meta.setUserId(userId);
        meta.setUploadId("LOCAL");
        meta.setObjectPath(filePath);
        meta.setProfileId(profileId);
        meta.setFilename(filename);
        meta.setStatus("PROCESSING");
        meta.setCreatedAt(Instant.now());
        meta.setUpdatedAt(Instant.now());
        metadataRepository.save(meta);

        try {
            // Load PDF from local file
            File pdfFile = new File(filePath);
            if (!pdfFile.exists()) {
                throw new Exception("File not found: " + filePath);
            }

            PDDocument doc;
            try {
                // Try loading with password if provided, otherwise without
                if (password != null && !password.isEmpty()) {
                    doc = Loader.loadPDF(pdfFile, password);
                    log.info("[Local job] PDF loaded with password for file: {}", filename);
                } else {
                    doc = Loader.loadPDF(pdfFile);
                }
            } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException ipe) {
                meta.setStatus("PASSWORD_REQUIRED");
                meta.setErrorMessage("PDF is password protected. Please provide the correct password.");
                meta.setUpdatedAt(Instant.now());
                metadataRepository.save(meta);
                throw new Exception("PDF is password protected or incorrect password provided");
            }

            // Detect bank from first few pages
            String firstPages = PdfUtil.extractTextFromPages(doc, 1, Math.min(3, doc.getNumberOfPages()));
            String bank = BankDetectorUtil.detectBank(firstPages);
            PdfParserStrategy parser = parserFactory.getParser(bank);
            List<TransactionEntity> buffer = new ArrayList<>();
            int total = doc.getNumberOfPages();

            String documentText = PdfUtil.extractTextFromPages(doc, 1, total);
            Long openingBalancePaisa = parser.extractOpeningBalance(documentText);
            log.info("[Local job] openingBalancePaisa for bank {}: {}", bank, openingBalancePaisa);

            if (parser.requiresFullDocumentText()) {
                List<TransactionEntity> txns = parser.parse(documentText, openingBalancePaisa);
                for (TransactionEntity t : txns) {
                    t.setStatementId(jobId);
                    t.setUserId(userId);
                    t.setProfileId(profileId);
                    t.setCreatedAt(Instant.now());
                    if (t.getId() == null) t.setId(UUID.randomUUID());
                    buffer.add(t);
                    if (buffer.size() >= 200) {
                        transactionRepository.saveAll(buffer);
                        buffer.clear();
                    }
                }
            } else {
                // Fallback for parsers that operate on a page-by-page basis
                for (int i = 0; i < total; i++) {
                    String pageText = PdfUtil.extractTextFromPages(doc, i + 1, i + 1);
                    List<TransactionEntity> txns = parser.parse(pageText, openingBalancePaisa);
                    for (TransactionEntity t : txns) {
                        t.setStatementId(jobId);
                        t.setUserId(userId);
                        t.setProfileId(profileId);
                        t.setCreatedAt(Instant.now());
                        if (t.getId() == null) t.setId(UUID.randomUUID());
                        buffer.add(t);
                    }
                    // Batch insert for performance
                    if (buffer.size() >= 200) {
                        transactionRepository.saveAll(buffer);
                        buffer.clear();
                    }
                }
            }

            // Save remaining transactions
            if (!buffer.isEmpty()) {
                transactionRepository.saveAll(buffer);
            }

            // Update metadata status
            meta.setStatus("DONE");
            meta.setUpdatedAt(Instant.now());
            metadataRepository.save(meta);

            doc.close();

            return jobId;

        } catch (Exception ex) {
            meta.setStatus("FAILED");
            meta.setErrorMessage(ex.getMessage());
            meta.setUpdatedAt(Instant.now());
            metadataRepository.save(meta);
            throw ex;
        }
    }
}
