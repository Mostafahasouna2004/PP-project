package com.etl.bigdata.service;

import com.etl.bigdata.batch.EtlBatchConfig;
import com.etl.bigdata.entity.ProcessingJob;
import com.etl.bigdata.repository.ProcessingJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Orchestrates the full ETL pipeline:
 * 1. Upload & parse CSV
 * 2. Parallel preprocessing (validation + cleaning)
 * 3. Spring Batch ETL (transform + load)
 */
@Service
public class EtlService {

    private static final Logger logger = LoggerFactory.getLogger(EtlService.class);

    private final ParallelPreprocessingService preprocessingService;
    private final EtlBatchConfig etlBatchConfig;
    private final ProcessingJobRepository processingJobRepository;

    public EtlService(ParallelPreprocessingService preprocessingService,
                      EtlBatchConfig etlBatchConfig,
                      ProcessingJobRepository processingJobRepository) {
        this.preprocessingService = preprocessingService;
        this.etlBatchConfig = etlBatchConfig;
        this.processingJobRepository = processingJobRepository;
    }

    /**
     * Run the full pipeline: preprocess (parallel or single) then ETL batch job.
     */
    public ProcessingJob runPipeline(InputStream csvStream, String fileName, boolean multiThreaded) {
        String batchId = UUID.randomUUID().toString().substring(0, 8);

        // Create job record
        ProcessingJob job = new ProcessingJob();
        job.setBatchId(batchId);
        job.setFileName(fileName);
        job.setProcessingMode(multiThreaded ? "MULTI_THREAD" : "SINGLE_THREAD");
        job.setStartedAt(LocalDateTime.now());
        job.setStatus("PREPROCESSING");
        processingJobRepository.save(job);

        try {
            // Step 1: Preprocessing (parallel or single-threaded)
            logger.info("=== Starting preprocessing for batch {} ({}) ===",
                    batchId, multiThreaded ? "MULTI-THREAD" : "SINGLE-THREAD");

            ParallelPreprocessingService.PreprocessingResult prepResult;
            if (multiThreaded) {
                prepResult = preprocessingService.processMultiThreaded(csvStream, batchId);
            } else {
                prepResult = preprocessingService.processSingleThreaded(csvStream, batchId);
            }

            job.setTotalRecords(prepResult.getTotalRecords());
            job.setValidRecords(prepResult.getValidRecords());
            job.setInvalidRecords(prepResult.getInvalidRecords());
            job.setPreprocessingTimeMs(prepResult.getDurationMs());
            job.setThreadCount(prepResult.getThreadCount());
            job.setStatus("ETL_PROCESSING");
            processingJobRepository.save(job);

            // Step 2: Spring Batch ETL
            logger.info("=== Starting Spring Batch ETL for batch {} ===", batchId);
            long etlStart = System.currentTimeMillis();

            JobExecution execution = etlBatchConfig.runEtl(batchId);

            long etlDuration = System.currentTimeMillis() - etlStart;
            job.setEtlTimeMs(etlDuration);

            if (execution.getStatus() == BatchStatus.COMPLETED) {
                job.setStatus("COMPLETED");
                logger.info("=== Pipeline completed for batch {} | Preprocessing: {}ms | ETL: {}ms ===",
                        batchId, prepResult.getDurationMs(), etlDuration);
            } else {
                job.setStatus("FAILED");
                job.setErrorMessage("Batch job status: " + execution.getStatus());
            }

            job.setTotalTimeMs(prepResult.getDurationMs() + etlDuration);
            job.setCompletedAt(LocalDateTime.now());
            processingJobRepository.save(job);

        } catch (Exception e) {
            logger.error("Pipeline failed for batch {}: {}", batchId, e.getMessage(), e);
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            processingJobRepository.save(job);
        }

        return job;
    }
}
