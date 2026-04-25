package com.etl.bigdata.repository;

import com.etl.bigdata.entity.ProcessedRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessedRecordRepository extends JpaRepository<ProcessedRecord, Long> {

    List<ProcessedRecord> findByBatchId(String batchId);

    long countByBatchId(String batchId);

    @Query("SELECT p.department, COUNT(p), AVG(p.salary), MIN(p.salary), MAX(p.salary) FROM ProcessedRecord p WHERE p.batchId = ?1 GROUP BY p.department")
    List<Object[]> getStatsByDepartment(String batchId);

    @Query("SELECT p.salaryGrade, COUNT(p) FROM ProcessedRecord p WHERE p.batchId = ?1 GROUP BY p.salaryGrade ORDER BY p.salaryGrade")
    List<Object[]> countBySalaryGrade(String batchId);

    @Query("SELECT p.country, COUNT(p) FROM ProcessedRecord p WHERE p.batchId = ?1 GROUP BY p.country ORDER BY COUNT(p) DESC")
    List<Object[]> countByCountry(String batchId);

    @Query("SELECT p.department, AVG(p.salary) FROM ProcessedRecord p WHERE p.batchId = ?1 GROUP BY p.department ORDER BY AVG(p.salary) DESC")
    List<Object[]> avgSalaryByDepartment(String batchId);
}
