package com.etl.bigdata.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "processing_jobs")
public class ProcessingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", unique = true)
    private String batchId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "valid_records")
    private Integer validRecords;

    @Column(name = "invalid_records")
    private Integer invalidRecords;

    @Column(name = "processing_mode")
    private String processingMode; // SINGLE_THREAD or MULTI_THREAD

    @Column(name = "thread_count")
    private Integer threadCount;

    @Column(name = "preprocessing_time_ms")
    private Long preprocessingTimeMs;

    @Column(name = "etl_time_ms")
    private Long etlTimeMs;

    @Column(name = "total_time_ms")
    private Long totalTimeMs;

    @Column(name = "status")
    private String status; // UPLOADING, PREPROCESSING, ETL_PROCESSING, COMPLETED, FAILED

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    public ProcessingJob() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Integer getTotalRecords() { return totalRecords; }
    public void setTotalRecords(Integer totalRecords) { this.totalRecords = totalRecords; }

    public Integer getValidRecords() { return validRecords; }
    public void setValidRecords(Integer validRecords) { this.validRecords = validRecords; }

    public Integer getInvalidRecords() { return invalidRecords; }
    public void setInvalidRecords(Integer invalidRecords) { this.invalidRecords = invalidRecords; }

    public String getProcessingMode() { return processingMode; }
    public void setProcessingMode(String processingMode) { this.processingMode = processingMode; }

    public Integer getThreadCount() { return threadCount; }
    public void setThreadCount(Integer threadCount) { this.threadCount = threadCount; }

    public Long getPreprocessingTimeMs() { return preprocessingTimeMs; }
    public void setPreprocessingTimeMs(Long preprocessingTimeMs) { this.preprocessingTimeMs = preprocessingTimeMs; }

    public Long getEtlTimeMs() { return etlTimeMs; }
    public void setEtlTimeMs(Long etlTimeMs) { this.etlTimeMs = etlTimeMs; }

    public Long getTotalTimeMs() { return totalTimeMs; }
    public void setTotalTimeMs(Long totalTimeMs) { this.totalTimeMs = totalTimeMs; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
