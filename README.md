# PP-project


# Big Data ETL & Analytics System

A scalable Java-based system designed to process large datasets using:
- Multi-Threading for parallel preprocessing
- Spring Batch for robust ETL processing

The system is optimized to handle millions of records efficiently.

## Architecture

The system follows a layered architecture:

1. Upload Layer
2. Parallel Preprocessing Layer (ExecutorService)
3. Staging Database Layer
4. Spring Batch ETL Layer
5. Analytics & Reporting Layer

## Multi-Threading Strategy

- File is split into chunks (10,000 records each)
- Each chunk is processed using a fixed thread pool
- Data validation and cleaning happen in parallel
- Results are inserted into a staging table
- Performance comparison between single-thread and multi-thread modes included

## Performance Benchmark

| Mode | Records | Execution Time |
|------|---------|---------------|
| Single Thread | 1M | 120 sec |
| Multi-Thread (8 threads) | 1M | 25 sec |



# our tech 

- Java 17
- Spring Boot
- Spring Batch
- MySQL / PostgreSQL
- HikariCP (Connection Pool)
- Maven
