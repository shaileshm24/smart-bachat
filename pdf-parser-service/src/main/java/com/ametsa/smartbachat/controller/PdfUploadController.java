package com.ametsa.smartbachat.controller;

import com.ametsa.smartbachat.dto.StartRequestDto;
import com.ametsa.smartbachat.dto.StartResponseDto;
import com.ametsa.smartbachat.dto.TransactionDto;
import com.ametsa.smartbachat.dto.StatementSummaryDto;
import com.ametsa.smartbachat.dto.TransactionsResponseDto;
import com.ametsa.smartbachat.dto.UploadResponseDto;

import com.ametsa.smartbachat.entity.TransactionEntity;
import com.ametsa.smartbachat.repository.TransactionRepository;
import com.ametsa.smartbachat.security.UserPrincipal;
import com.ametsa.smartbachat.service.GcsUploadService;
import com.ametsa.smartbachat.service.JobService;
import com.ametsa.smartbachat.service.ParserWorker;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/ingest")
public class PdfUploadController {

	    private final GcsUploadService gcsUploadService;
	    private final JobService jobService;
	    private final ParserWorker parserWorker;
	    private final TransactionRepository transactionRepository;

	    public PdfUploadController(GcsUploadService gcsUploadService,
	                               JobService jobService,
	                               ParserWorker parserWorker,
	                               TransactionRepository transactionRepository) {
	        this.gcsUploadService = gcsUploadService;
	        this.jobService = jobService;
	        this.parserWorker = parserWorker;
	        this.transactionRepository = transactionRepository;
	    }

    /**
     * Get the profile ID from the authenticated user.
     */
    private UUID getProfileId(UserPrincipal principal) {
        if (principal == null || principal.getProfileId() == null) {
            throw new RuntimeException("User profile not found. Please complete your profile setup.");
        }
        return principal.getProfileId();
    }

    @PostMapping(value = "/request-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponseDto> requestUpload(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String profileId = getProfileId(principal).toString();
        // Use original filename or generate unique name
        String filename = file.getOriginalFilename();

        // Call your GCS service to upload the file and return a signed URL
        UploadResponseDto resp = gcsUploadService.uploadFileAndCreateSignedUrl(file, filename, profileId);

        return ResponseEntity.ok(resp);
    }

    @PostMapping(value = "/parse-local-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StartResponseDto> parseLocalFile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        return parseLocalFileWithPassword(principal, file, null);
    }

    /**
     * Parse a PDF file with optional password support.
     * Accepts a file and optional password for password-protected PDFs.
     *
     * @param principal The authenticated user
     * @param file The PDF file to parse
     * @param password Optional password for encrypted PDFs
     * @return StartResponseDto with the job ID
     */
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StartResponseDto> parseLocalFileWithPassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false) String password
    ) throws Exception {
        // Validate the file
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        String filename = file.getOriginalFilename();

        // Get profile ID from authenticated user
        UUID profileId = getProfileId(principal);

        // Save file temporarily to local filesystem
        String tempDir = System.getProperty("java.io.tmpdir");
        String tempFilePath = tempDir + File.separator + UUID.randomUUID() + "_" + filename;
        File tempFile = new File(tempFilePath);

        try {
            // Transfer uploaded file to temp location
            file.transferTo(tempFile);

            // Parse the PDF directly and store transactions to DB
            UUID jobId = parserWorker.processLocalFile(tempFilePath, profileId, filename, password);

            return ResponseEntity.ok(new StartResponseDto(jobId));
        } finally {
            // Clean up temp file
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }



	    @PostMapping("/start")
	    public ResponseEntity<StartResponseDto> startIngest(@Validated @RequestBody StartRequestDto req) throws Exception {
	        StartResponseDto resp = jobService.startJob(req.getUploadId(), req.getObjectName(), req.getProfileId(), req.getFilename());
	        return ResponseEntity.accepted().body(resp);
	    }

	    @GetMapping("/status/{jobId}")
	    public ResponseEntity<JobService.JobStatusDto> status(@PathVariable UUID jobId) {
	        JobService.JobStatusDto s = jobService.getStatus(jobId);
	        return ResponseEntity.ok(s);
	    }

	    @PostMapping("/{jobId}/unlock")
	    public ResponseEntity<Void> unlock(@PathVariable UUID jobId, @RequestBody String password) throws Exception {
	        jobService.submitPassword(jobId, password);
	        return ResponseEntity.accepted().build();
	    }

		    @GetMapping("/statements/{statementId}/transactions")
		    public ResponseEntity<TransactionsResponseDto> getTransactionsForStatement(@PathVariable UUID statementId) {
		        List<TransactionEntity> entities =
		                transactionRepository.findByStatementIdOrderByTxnDateAscCreatedAtAsc(statementId);

		        List<TransactionDto> dtos = new java.util.ArrayList<>();
		        int creditCount = 0;
		        int debitCount = 0;
		        Long previousBalance = null;
		        for (TransactionEntity e : entities) {
		            TransactionDto dto = toDto(e);
		            Long currentBalance = e.getBalance();
		            Long amountPaisa = e.getAmount();

		            // Start with direction as parsed from the PDF (if any)
		            String direction = (e.getDirection() != null && !e.getDirection().isEmpty())
		                    ? e.getDirection()
		                    : null;

		            // Prefer using closing-balance movement when it cleanly matches the
		            // transaction amount. This follows the rule you described: compare
		            // previous vs current closing balance to decide debit/credit.
		            if (currentBalance != null
		                    && previousBalance != null
		                    && amountPaisa != null
		                    && amountPaisa > 0) {
		                long delta = currentBalance - previousBalance;
		                long absDelta = Math.abs(delta);
		                // Match by magnitude (allowing 1 paisa rounding diff just in case)
		                if (absDelta == amountPaisa || Math.abs(absDelta - amountPaisa) <= 1) {
		                    direction = (delta > 0) ? "CREDIT" : "DEBIT";
		                }
		            }

		            // Final fallback for any legacy/edge data with no reliable balances:
		            // infer from sign of amount (for very old records that may still
		            // store negative amounts for debits).
		            if (direction == null && amountPaisa != null) {
		                if (amountPaisa > 0) {
		                    direction = "CREDIT";
		                } else if (amountPaisa < 0) {
		                    direction = "DEBIT";
		                }
		            }

		            dto.setDirection(direction);
		            if ("CREDIT".equalsIgnoreCase(direction)) {
		                creditCount++;
		            } else if ("DEBIT".equalsIgnoreCase(direction)) {
		                debitCount++;
		            }
		            dtos.add(dto);
		            if (currentBalance != null) {
		                previousBalance = currentBalance;
		            }
		        }

		        StatementSummaryDto summary = new StatementSummaryDto();
		        summary.setStatementId(statementId);
		        summary.setCreditCount(creditCount);
		        summary.setDebitCount(debitCount);
		        summary.setTotalCount(dtos.size());

		        TransactionsResponseDto response = new TransactionsResponseDto();
		        response.setSummary(summary);
		        response.setTransactions(dtos);

		        return ResponseEntity.ok(response);
		    }

		    private TransactionDto toDto(TransactionEntity e) {
		        TransactionDto dto = new TransactionDto();
		        dto.setId(e.getId());
		        dto.setStatementId(e.getStatementId());
		        dto.setProfileId(e.getProfileId());
		        dto.setTxnDate(e.getTxnDate());
		        // Convert stored paisa (Long) back to rupees (Double) for API consumers,
		        // expose amounts as positive numbers; direction is a separate field.
		        if (e.getAmount() != null) {
		            dto.setAmount(Math.abs(e.getAmount()) / 100.0);
		        }
		        dto.setCurrency(e.getCurrency());
		        dto.setTxnType(e.getTxnType());
		        dto.setDescription(e.getDescription());
		        dto.setMerchant(e.getMerchant());
		        if (e.getBalance() != null) {
		            dto.setBalance(e.getBalance() / 100.0);
		        }
		        return dto;
		    }
		}
