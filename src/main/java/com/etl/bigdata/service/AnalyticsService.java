package com.etl.bigdata.service;

import com.etl.bigdata.entity.ProcessingJob;
import com.etl.bigdata.repository.ProcessedRecordRepository;
import com.etl.bigdata.repository.ProcessingJobRepository;
import com.etl.bigdata.repository.StagingRecordRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Analytics service for generating reports and statistics from processed data.
 */
@Service
public class AnalyticsService {

    private final ProcessedRecordRepository processedRecordRepository;
    private final StagingRecordRepository stagingRecordRepository;
    private final ProcessingJobRepository processingJobRepository;

    public AnalyticsService(ProcessedRecordRepository processedRecordRepository,
                           StagingRecordRepository stagingRecordRepository,
                           ProcessingJobRepository processingJobRepository) {
        this.processedRecordRepository = processedRecordRepository;
        this.stagingRecordRepository = stagingRecordRepository;
        this.processingJobRepository = processingJobRepository;
    }

    public Map<String, Object> getAnalytics(String batchId) {
        Map<String, Object> analytics = new LinkedHashMap<>();

        // Department stats
        List<Object[]> deptStats = processedRecordRepository.getStatsByDepartment(batchId);
        List<Map<String, Object>> departmentData = new ArrayList<>();
        for (Object[] row : deptStats) {
            Map<String, Object> dept = new LinkedHashMap<>();
            dept.put("department", row[0]);
            dept.put("count", row[1]);
            dept.put("avgSalary", Math.round((Double) row[2] * 100.0) / 100.0);
            dept.put("minSalary", Math.round((Double) row[3] * 100.0) / 100.0);
            dept.put("maxSalary", Math.round((Double) row[4] * 100.0) / 100.0);
            departmentData.add(dept);
        }
        analytics.put("departmentStats", departmentData);

        // Salary grade distribution
        List<Object[]> gradeStats = processedRecordRepository.countBySalaryGrade(batchId);
        Map<String, Long> gradeData = new LinkedHashMap<>();
        for (Object[] row : gradeStats) {
            gradeData.put((String) row[0], (Long) row[1]);
        }
        analytics.put("salaryGrades", gradeData);

        // Country distribution
        List<Object[]> countryStats = processedRecordRepository.countByCountry(batchId);
        Map<String, Long> countryData = new LinkedHashMap<>();
        for (Object[] row : countryStats) {
            countryData.put((String) row[0], (Long) row[1]);
        }
        analytics.put("countryDistribution", countryData);

        // Average salary by department (for chart)
        List<Object[]> avgSalary = processedRecordRepository.avgSalaryByDepartment(batchId);
        Map<String, Double> avgSalaryData = new LinkedHashMap<>();
        for (Object[] row : avgSalary) {
            avgSalaryData.put((String) row[0], Math.round((Double) row[1] * 100.0) / 100.0);
        }
        analytics.put("avgSalaryByDepartment", avgSalaryData);

        // Total processed records
        analytics.put("totalProcessedRecords", processedRecordRepository.countByBatchId(batchId));

        return analytics;
    }

    public List<ProcessingJob> getAllJobs() {
        return processingJobRepository.findAllByOrderByStartedAtDesc();
    }

    public Optional<ProcessingJob> getJob(String batchId) {
        return processingJobRepository.findByBatchId(batchId);
    }

    /**
     * Get performance comparison between single-thread and multi-thread jobs.
     */
    public Map<String, Object> getPerformanceComparison() {
        Map<String, Object> comparison = new LinkedHashMap<>();

        List<ProcessingJob> allJobs = processingJobRepository.findAllByOrderByStartedAtDesc();

        List<Map<String, Object>> jobSummaries = new ArrayList<>();
        for (ProcessingJob job : allJobs) {
            if (!"COMPLETED".equals(job.getStatus())) continue;
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("batchId", job.getBatchId());
            summary.put("fileName", job.getFileName());
            summary.put("mode", job.getProcessingMode());
            summary.put("threads", job.getThreadCount());
            summary.put("totalRecords", job.getTotalRecords());
            summary.put("preprocessingMs", job.getPreprocessingTimeMs());
            summary.put("etlMs", job.getEtlTimeMs());
            summary.put("totalMs", job.getTotalTimeMs());
            summary.put("recordsPerSecond", job.getTotalRecords() != null && job.getTotalTimeMs() != null && job.getTotalTimeMs() > 0
                    ? Math.round(job.getTotalRecords() * 1000.0 / job.getTotalTimeMs()) : 0);
            jobSummaries.add(summary);
        }
        comparison.put("jobs", jobSummaries);

        return comparison;
    }
}
