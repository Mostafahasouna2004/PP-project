package com.etl.bigdata.controller;

import com.etl.bigdata.entity.ProcessingJob;
import com.etl.bigdata.service.AnalyticsService;
import com.etl.bigdata.service.DataGeneratorService;
import com.etl.bigdata.service.EtlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Optional;

@Controller
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private final EtlService etlService;
    private final AnalyticsService analyticsService;
    private final DataGeneratorService dataGeneratorService;

    public MainController(EtlService etlService,
                         AnalyticsService analyticsService,
                         DataGeneratorService dataGeneratorService) {
        this.etlService = etlService;
        this.analyticsService = analyticsService;
        this.dataGeneratorService = dataGeneratorService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("jobs", analyticsService.getAllJobs());
        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                            @RequestParam(value = "multiThreaded", defaultValue = "true") boolean multiThreaded,
                            RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a CSV file to upload.");
                return "redirect:/";
            }

            ProcessingJob job = etlService.runPipeline(file.getInputStream(), file.getOriginalFilename(), multiThreaded);
            redirectAttributes.addFlashAttribute("success",
                    "Processing completed! Batch ID: " + job.getBatchId());
            return "redirect:/results/" + job.getBatchId();

        } catch (Exception e) {
            logger.error("Upload failed: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Processing failed: " + e.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/generate-and-process")
    public String generateAndProcess(@RequestParam(value = "recordCount", defaultValue = "10000") int recordCount,
                                     @RequestParam(value = "multiThreaded", defaultValue = "true") boolean multiThreaded,
                                     RedirectAttributes redirectAttributes) {
        try {
            byte[] csvData = dataGeneratorService.generateCsv(recordCount);
            ProcessingJob job = etlService.runPipeline(
                    new ByteArrayInputStream(csvData),
                    "generated_" + recordCount + "_records.csv",
                    multiThreaded);

            redirectAttributes.addFlashAttribute("success",
                    "Generated and processed " + recordCount + " records! Batch ID: " + job.getBatchId());
            return "redirect:/results/" + job.getBatchId();

        } catch (Exception e) {
            logger.error("Generate failed: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Generation failed: " + e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/results/{batchId}")
    public String results(@PathVariable String batchId, Model model) {
        Optional<ProcessingJob> jobOpt = analyticsService.getJob(batchId);
        if (jobOpt.isEmpty()) {
            return "redirect:/";
        }

        ProcessingJob job = jobOpt.get();
        model.addAttribute("job", job);

        if ("COMPLETED".equals(job.getStatus())) {
            Map<String, Object> analytics = analyticsService.getAnalytics(batchId);
            model.addAttribute("analytics", analytics);
        }

        return "results";
    }

    @GetMapping("/performance")
    public String performance(Model model) {
        Map<String, Object> comparison = analyticsService.getPerformanceComparison();
        model.addAttribute("comparison", comparison);
        model.addAttribute("jobs", analyticsService.getAllJobs());
        return "performance";
    }

    @GetMapping("/generate-csv")
    public ResponseEntity<byte[]> downloadGeneratedCsv(@RequestParam(defaultValue = "1000") int count) {
        try {
            byte[] csv = dataGeneratorService.generateCsv(count);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sample_data_" + count + ".csv")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(csv);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
