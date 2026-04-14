package com.claiminsight.metrics.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.claiminsight.metrics.dto.ClaimKpiRequestDTO;
import com.claiminsight.metrics.dto.ClaimKpiResponseDTO;
import com.claiminsight.metrics.exception.InvalidMetricException;
import com.claiminsight.metrics.exception.ResourceNotFoundException;
import com.claiminsight.metrics.mapper.ClaimKpiMapper;
import com.claiminsight.metrics.model.ClaimKPI;
import com.claiminsight.metrics.model.MetricName;
import com.claiminsight.metrics.repository.ClaimKpiRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service layer for KPI metric management. Results are cached in the 'kpis' cache. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimKpiService {

    private final ClaimKpiRepository claimKpiRepository;
    private final ClaimKpiMapper claimKpiMapper;

    /** Saves a new KPI metric. Clears the kpis cache. */
    @CacheEvict(value = "kpis", allEntries = true)
    public ClaimKpiResponseDTO createKpi(ClaimKpiRequestDTO request) {
        MetricName name = parseMetricName(request.getMetricName());
        ClaimKPI saved  = claimKpiRepository.save(claimKpiMapper.toEntity(request, name));
        log.info("KPI created with ID: {}", saved.getKpiId());
        return claimKpiMapper.toResponseDTO(saved);
    }

    /** Returns all KPI records. Result is cached. */
    @Cacheable(value = "kpis", key = "'all'")
    public List<ClaimKpiResponseDTO> getAllKpis() {
        return claimKpiRepository.findAll()
                .stream().map(claimKpiMapper::toResponseDTO).collect(Collectors.toList());
    }

    /** Returns a KPI by ID. Throws 404 if not found. */
    @Cacheable(value = "kpis", key = "#kpiId")
    public ClaimKpiResponseDTO getKpiById(Long kpiId) {
        return claimKpiMapper.toResponseDTO(findOrThrow(kpiId));
    }

    /** Returns all KPIs for a specific claim. Result is cached. */
    @Cacheable(value = "kpis", key = "'claim-' + #claimId")
    public List<ClaimKpiResponseDTO> getKpisByClaimId(String claimId) {
        return claimKpiRepository.findByClaimId(claimId)
                .stream().map(claimKpiMapper::toResponseDTO).collect(Collectors.toList());
    }

    /** Returns all KPIs of a metric type. Throws 400 if invalid name. */
    @Cacheable(value = "kpis", key = "'metric-' + #metricName.toUpperCase()")
    public List<ClaimKpiResponseDTO> getKpisByMetricName(String metricName) {
        return claimKpiRepository.findByMetricName(parseMetricName(metricName))
                .stream().map(claimKpiMapper::toResponseDTO).collect(Collectors.toList());
    }

    /** Returns KPIs filtered by claimId and metric type. */
    @Cacheable(value = "kpis", key = "'claim-' + #claimId + '-' + #metricName.toUpperCase()")
    public List<ClaimKpiResponseDTO> getKpisByClaimIdAndMetricName(String claimId, String metricName) {
        return claimKpiRepository.findByClaimIdAndMetricName(claimId, parseMetricName(metricName))
                .stream().map(claimKpiMapper::toResponseDTO).collect(Collectors.toList());
    }

    /** Returns KPIs within a date range. Result is cached. */
    @Cacheable(value = "kpis", key = "'date-' + #start + '-' + #end")
    public List<ClaimKpiResponseDTO> getKpisByDateRange(LocalDate start, LocalDate end) {
        return claimKpiRepository.findByMetricDateBetween(start, end)
                .stream().map(claimKpiMapper::toResponseDTO).collect(Collectors.toList());
    }

    /** Deletes a KPI by ID. Clears the kpis cache. Throws 404 if not found. */
    @CacheEvict(value = "kpis", allEntries = true)
    public void deleteKpi(Long kpiId) {
        findOrThrow(kpiId);
        claimKpiRepository.deleteById(kpiId);
        log.info("KPI {} deleted", kpiId);
    }

    private ClaimKPI findOrThrow(Long kpiId) {
        return claimKpiRepository.findById(kpiId)
                .orElseThrow(() -> new ResourceNotFoundException("ClaimKPI with ID " + kpiId + " not found"));
    }

    private MetricName parseMetricName(String value) {
        try {
            return MetricName.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidMetricException("Invalid metricName: '" + value + "'. Allowed: TAT, CYCLE_TIME, SEVERITY, FREQUENCY, LOSS_RATIO, SETTLEMENT_TIME");
        }
    }
}
