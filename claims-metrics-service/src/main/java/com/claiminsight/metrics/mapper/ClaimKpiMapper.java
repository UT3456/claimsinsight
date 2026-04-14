package com.claiminsight.metrics.mapper;

import com.claiminsight.metrics.dto.ClaimKpiRequestDTO;
import com.claiminsight.metrics.dto.ClaimKpiResponseDTO;
import com.claiminsight.metrics.model.ClaimKPI;
import com.claiminsight.metrics.model.MetricName;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

/** Maps between ClaimKpiRequestDTO/ClaimKpiResponseDTO and the ClaimKPI entity using ModelMapper. */
@Component
@RequiredArgsConstructor
public class ClaimKpiMapper {

    private final ModelMapper modelMapper;

    // DTO → Entity: ModelMapper copies claimId, metricValue, metricDate automatically.
    // metricName is set manually because it is an enum (String in DTO, Enum in entity).
    /** Converts a request DTO to a ClaimKPI entity. Sets metricName enum manually. */
    public ClaimKPI toEntity(ClaimKpiRequestDTO dto, MetricName metricName) {
        ClaimKPI kpi = modelMapper.map(dto, ClaimKPI.class);
        kpi.setMetricName(metricName);
        kpi.setKpiId(null);
        return kpi;
    }

    // Entity → DTO: ModelMapper copies kpiId, claimId, metricValue, metricDate automatically.
    // metricName is set manually to convert Enum → String.
    /** Converts a ClaimKPI entity to a response DTO. Converts metricName enum to String. */
    public ClaimKpiResponseDTO toResponseDTO(ClaimKPI kpi) {
        ClaimKpiResponseDTO dto = modelMapper.map(kpi, ClaimKpiResponseDTO.class);
        dto.setMetricName(kpi.getMetricName().name());
        return dto;
    }
}
