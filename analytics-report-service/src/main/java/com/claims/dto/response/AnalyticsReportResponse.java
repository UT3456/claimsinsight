package com.claims.dto.response;

import com.claims.enums.ReportScope;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsReportResponse {

    private Long reportId;
    private ReportScope scope;
    private String scopeValue;
    private String metrics;
    private LocalDate generatedDate;
    private String generatedBy;
    private String reportData;
}
