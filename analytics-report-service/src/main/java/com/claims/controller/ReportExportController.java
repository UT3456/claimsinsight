package com.claims.controller;

import com.claims.dto.response.AnalyticsReportResponse;
import com.claims.service.AnalyticsReportServiceImpl;
import com.claims.service.ReportExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

/**
 * File-download endpoints for analytics reports.
 *   GET /api/reports/{id}/export?format=pdf   -> application/pdf
 *   GET /api/reports/{id}/export?format=csv   -> text/csv
 *
 * Content-Disposition: attachment so browsers trigger a real download.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportExportController {

    private final AnalyticsReportServiceImpl analyticsReportService;
    private final ReportExportService exportService;

    @GetMapping("/{reportId}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable Long reportId,
            @RequestParam(defaultValue = "pdf") String format) {

        AnalyticsReportResponse report = analyticsReportService.getReportById(reportId);
        String fmt = format == null ? "pdf" : format.toLowerCase();

        return switch (fmt) {
            case "pdf" -> {
                byte[] pdf = exportService.toPdf(report);
                yield ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"report-" + reportId + ".pdf\"")
                        .body(pdf);
            }
            case "csv" -> {
                byte[] csv = exportService.toCsv(report).getBytes(StandardCharsets.UTF_8);
                yield ResponseEntity.ok()
                        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"report-" + reportId + ".csv\"")
                        .body(csv);
            }
            default -> throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Unsupported format: " + format + " (use pdf or csv)");
        };
    }
}
