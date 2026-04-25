package com.etl.bigdata.batch;

import com.etl.bigdata.entity.ProcessedRecord;
import com.etl.bigdata.entity.StagingRecord;
import com.etl.bigdata.repository.ProcessedRecordRepository;
import com.etl.bigdata.repository.StagingRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Batch ETL Configuration.
 * Creates a fresh Job for each batch run to read from staging, transform, and write to processed table.
 * Uses the classic Reader -> Processor -> Writer pattern.
 */
@Component
public class EtlBatchConfig {

    private static final Logger logger = LoggerFactory.getLogger(EtlBatchConfig.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobLauncher jobLauncher;
    private final StagingRecordRepository stagingRecordRepository;
    private final ProcessedRecordRepository processedRecordRepository;

    public EtlBatchConfig(JobRepository jobRepository,
                          PlatformTransactionManager transactionManager,
                          JobLauncher jobLauncher,
                          StagingRecordRepository stagingRecordRepository,
                          ProcessedRecordRepository processedRecordRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.jobLauncher = jobLauncher;
        this.stagingRecordRepository = stagingRecordRepository;
        this.processedRecordRepository = processedRecordRepository;
    }

    /**
     * Runs the ETL job for a specific batch.
     * Creates a fresh Job with a new ListItemReader each time,
     * ensuring correct data is loaded from the staging table.
     */
    public JobExecution runEtl(String batchId) throws Exception {
        // Load valid records from staging
        List<StagingRecord> validRecords = stagingRecordRepository.findByBatchIdAndIsValidTrue(batchId);
        logger.info("ETL: Loaded {} valid staging records for batch {}", validRecords.size(), batchId);

        // READER: fresh ListItemReader with this batch's data
        ListItemReader<StagingRecord> reader = new ListItemReader<>(validRecords);

        // PROCESSOR: transform staging -> processed
        ItemProcessor<StagingRecord, ProcessedRecord> processor = staging -> {
            ProcessedRecord processed = new ProcessedRecord();
            processed.setRecordId(staging.getRecordId());
            processed.setFullName(staging.getFirstName() + " " + staging.getLastName());
            processed.setEmail(staging.getEmail());
            processed.setPhone(staging.getPhone());
            processed.setCity(staging.getCity());
            processed.setCountry(staging.getCountry());
            processed.setSalary(staging.getSalary());
            processed.setDepartment(staging.getDepartment());
            processed.setBatchId(staging.getBatchId());
            processed.setEtlTimestamp(LocalDateTime.now());

            // Transform: assign salary grade
            if (staging.getSalary() != null) {
                processed.setSalaryGrade(calculateSalaryGrade(staging.getSalary()));
            }

            return processed;
        };

        // WRITER: persist processed records
        ItemWriter<ProcessedRecord> writer = items -> processedRecordRepository.saveAll(items);

        // Build Step
        Step etlStep = new StepBuilder("etlStep-" + batchId, jobRepository)
                .<StagingRecord, ProcessedRecord>chunk(1000, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();

        // Build Job
        Job etlJob = new JobBuilder("etlJob-" + batchId, jobRepository)
                .start(etlStep)
                .build();

        // Launch with unique parameters
        JobParameters params = new JobParametersBuilder()
                .addString("batchId", batchId)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        return jobLauncher.run(etlJob, params);
    }

    private String calculateSalaryGrade(double salary) {
        if (salary < 30000) return "Junior";
        if (salary < 60000) return "Mid-Level";
        if (salary < 100000) return "Senior";
        if (salary < 150000) return "Lead";
        return "Executive";
    }
}
