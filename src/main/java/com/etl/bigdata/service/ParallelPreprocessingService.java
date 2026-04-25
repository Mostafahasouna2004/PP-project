package com.etl.bigdata.service;

import com.etl.bigdata.entity.StagingRecord;
import com.etl.bigdata.repository.StagingRecordRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Core parallel preprocessing service.
 * Splits CSV files into chunks and processes them using a fixed thread pool (ExecutorService).
 * Supports both single-thread and multi-thread modes for performance comparison.
 */
@Service
public class ParallelPreprocessingService {

    private static final Logger logger = LoggerFactory.getLogger(ParallelPreprocessingService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[git worktree listA-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9\\-\\s]{7,15}$");

    @Value("${etl.thread-pool.size:8}")
    private int threadPoolSize;

    @Value("${etl.chunk-size:10000}")
    private int chunkSize;

    private final StagingRecordRepository stagingRecordRepository;

    public ParallelPreprocessingService(StagingRecordRepository stagingRecordRepository) {
        this.stagingRecordRepository = stagingRecordRepository;
    }

    /**
     * Process a CSV file using multiple threads (parallel mode).
     * The file is split into chunks of configurable size, and each chunk
     * is submitted to an ExecutorService for concurrent processing.
     */
    public PreprocessingResult processMultiThreaded(InputStream inputStream, String batchId) throws Exception {
        logger.info("Starting MULTI-THREADED preprocessing with {} threads, chunk size: {}", threadPoolSize, chunkSize);
        long startTime = System.currentTimeMillis();

        // Step 1: Read all CSV lines
        List<String[]> allRows = readCsvRows(inputStream);
        logger.info("Read {} records from CSV", allRows.size());

        // Step 2: Split into chunks
        List<List<String[]>> chunks = splitIntoChunks(allRows, chunkSize);
        logger.info("Split into {} chunks", chunks.size());

        // Step 3: Create fixed thread pool and submit tasks
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        List<Future<ChunkResult>> futures = new ArrayList<>();
        AtomicInteger chunkCounter = new AtomicInteger(0);

        for (List<String[]> chunk : chunks) {
            int chunkNum = chunkCounter.incrementAndGet();
            futures.add(executorService.submit(() -> processChunk(chunk, batchId, chunkNum)));
        }

        // Step 4: Collect results from all threads
        int totalValid = 0;
        int totalInvalid = 0;
        List<StagingRecord> allRecords = new ArrayList<>();

        for (Future<ChunkResult> future : futures) {
            ChunkResult result = future.get(); // blocks until chunk is done
            totalValid += result.validCount;
            totalInvalid += result.invalidCount;
            allRecords.addAll(result.records);
        }

        // Step 5: Shutdown thread pool
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.MINUTES);

        // Step 6: Batch insert into staging table
        stagingRecordRepository.saveAll(allRecords);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("MULTI-THREADED preprocessing completed in {} ms. Valid: {}, Invalid: {}",
                duration, totalValid, totalInvalid);

        return new PreprocessingResult(allRows.size(), totalValid, totalInvalid, duration, threadPoolSize);
    }

    /**
     * Process a CSV file using a single thread (sequential mode).
     * Used for performance comparison with multi-threaded mode.
     */
    public PreprocessingResult processSingleThreaded(InputStream inputStream, String batchId) throws Exception {
        logger.info("Starting SINGLE-THREADED preprocessing");
        long startTime = System.currentTimeMillis();

        // Read all CSV lines
        List<String[]> allRows = readCsvRows(inputStream);
        logger.info("Read {} records from CSV", allRows.size());

        // Process all records sequentially in one chunk
        ChunkResult result = processChunk(allRows, batchId, 1);

        // Insert into staging table
        stagingRecordRepository.saveAll(result.records);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("SINGLE-THREADED preprocessing completed in {} ms. Valid: {}, Invalid: {}",
                duration, result.validCount, result.invalidCount);

        return new PreprocessingResult(allRows.size(), result.validCount, result.invalidCount, duration, 1);
    }

    /**
     * Process a single chunk of CSV rows.
     * This is the unit of work submitted to each thread.
     * Performs validation and data cleaning on each record.
     */
    private ChunkResult processChunk(List<String[]> rows, String batchId, int chunkNum) {
        logger.debug("Thread {} processing chunk #{} with {} records",
                Thread.currentThread().getName(), chunkNum, rows.size());

        List<StagingRecord> records = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;

        for (String[] row : rows) {
            StagingRecord record = new StagingRecord();
            record.setBatchId(batchId);
            record.setProcessedAt(LocalDateTime.now());

            List<String> errors = new ArrayList<>();

            try {
                // Parse and clean fields
                String recordId = getField(row, 0);
                String firstName = cleanString(getField(row, 1));
                String lastName = cleanString(getField(row, 2));
                String email = cleanString(getField(row, 3)).toLowerCase();
                String phone = cleanString(getField(row, 4));
                String city = cleanString(getField(row, 5));
                String country = cleanString(getField(row, 6));
                String salaryStr = cleanString(getField(row, 7));
                String department = cleanString(getField(row, 8));

                record.setRecordId(recordId);
                record.setFirstName(firstName);
                record.setLastName(lastName);
                record.setEmail(email);
                record.setPhone(phone);
                record.setCity(city);
                record.setCountry(country);
                record.setDepartment(department);

                // Validate fields
                if (firstName.isEmpty()) errors.add("Missing first name");
                if (lastName.isEmpty()) errors.add("Missing last name");
                if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
                    errors.add("Invalid email format");
                }
                if (email.isEmpty()) errors.add("Missing email");
                if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
                    errors.add("Invalid phone format");
                }

                // Parse salary
                if (!salaryStr.isEmpty()) {
                    try {
                        double salary = Double.parseDouble(salaryStr.replace(",", ""));
                        if (salary < 0) errors.add("Negative salary");
                        record.setSalary(salary);
                    } catch (NumberFormatException e) {
                        errors.add("Invalid salary format");
                    }
                } else {
                    errors.add("Missing salary");
                }

                if (department.isEmpty()) errors.add("Missing department");

            } catch (Exception e) {
                errors.add("Parse error: " + e.getMessage());
            }

            record.setIsValid(errors.isEmpty());
            record.setValidationErrors(errors.isEmpty() ? null : String.join("; ", errors));

            if (errors.isEmpty()) {
                validCount++;
            } else {
                invalidCount++;
            }

            records.add(record);
        }

        logger.debug("Chunk #{} complete: {} valid, {} invalid", chunkNum, validCount, invalidCount);
        return new ChunkResult(records, validCount, invalidCount);
    }

    private List<String[]> readCsvRows(InputStream inputStream) throws IOException, CsvValidationException {
        List<String[]> rows = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            // Skip header
            reader.readNext();
            String[] line;
            while ((line = reader.readNext()) != null) {
                rows.add(line);
            }
        }
        return rows;
    }

    private List<List<String[]>> splitIntoChunks(List<String[]> list, int size) {
        List<List<String[]>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return chunks;
    }

    private String getField(String[] row, int index) {
        if (index < row.length) {
            return row[index] != null ? row[index] : "";
        }
        return "";
    }

    private String cleanString(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", " ");
    }

    // Inner classes for results
    public static class ChunkResult {
        final List<StagingRecord> records;
        final int validCount;
        final int invalidCount;

        ChunkResult(List<StagingRecord> records, int validCount, int invalidCount) {
            this.records = records;
            this.validCount = validCount;
            this.invalidCount = invalidCount;
        }
    }

    public static class PreprocessingResult {
        private final int totalRecords;
        private final int validRecords;
        private final int invalidRecords;
        private final long durationMs;
        private final int threadCount;

        public PreprocessingResult(int totalRecords, int validRecords, int invalidRecords, long durationMs, int threadCount) {
            this.totalRecords = totalRecords;
            this.validRecords = validRecords;
            this.invalidRecords = invalidRecords;
            this.durationMs = durationMs;
            this.threadCount = threadCount;
        }

        public int getTotalRecords() { return totalRecords; }
        public int getValidRecords() { return validRecords; }
        public int getInvalidRecords() { return invalidRecords; }
        public long getDurationMs() { return durationMs; }
        public int getThreadCount() { return threadCount; }
    }
}
