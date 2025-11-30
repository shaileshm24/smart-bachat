package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.entity.StatementMetadata;
import com.ametsa.smartbachat.entity.TransactionEntity;
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
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ParserWorker {

    private final Storage storage;
    private final StatementMetadataRepository metadataRepository;
    private final TransactionRepository transactionRepository;
    private final ParserFactory parserFactory;

    public ParserWorker(Storage storage, StatementMetadataRepository metadataRepository, TransactionRepository transactionRepository, ParserFactory parserFactory) {
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

            for (int i = 0; i < total; i++) {
                String pageText = PdfUtil.extractTextFromPages(doc, i+1, i+1);
                List<TransactionEntity> txns = parser.parse(pageText);
                for (TransactionEntity t : txns) {
                    t.setStatementId(jobId);
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
}
