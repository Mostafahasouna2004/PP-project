package com.etl.bigdata.repository;

import com.etl.bigdata.entity.StagingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StagingRecordRepository extends JpaRepository<StagingRecord, Long> {

    List<StagingRecord> findByBatchId(String batchId);

    List<StagingRecord> findByBatchIdAndIsValidTrue(String batchId);

    List<StagingRecord> findByBatchIdAndIsValidFalse(String batchId);

    long countByBatchId(String batchId);

    long countByBatchIdAndIsValidTrue(String batchId);

    long countByBatchIdAndIsValidFalse(String batchId);

    @Query("SELECT s.department, COUNT(s) FROM StagingRecord s WHERE s.batchId = ?1 AND s.isValid = true GROUP BY s.department")
    List<Object[]> countByDepartmentAndBatchId(String batchId);

    @Query("SELECT s.city, COUNT(s) FROM StagingRecord s WHERE s.batchId = ?1 AND s.isValid = true GROUP BY s.city ORDER BY COUNT(s) DESC")
    List<Object[]> countByCityAndBatchId(String batchId);
}
