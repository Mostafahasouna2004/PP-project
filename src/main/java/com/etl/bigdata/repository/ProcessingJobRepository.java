package com.etl.bigdata.repository;

import com.etl.bigdata.entity.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, Long> {

    Optional<ProcessingJob> findByBatchId(String batchId);

    List<ProcessingJob> findAllByOrderByStartedAtDesc();

    List<ProcessingJob> findByStatus(String status);
}
