package com.demo.service;

import com.demo.dto.AdjusterPerformanceDTO;
import com.demo.entities.AdjusterPerformance;
import com.demo.exception.DatabaseOperationException;
import com.demo.exception.InvalidInputException;
import com.demo.exception.ResourceNotFoundException;
import com.demo.repositories.AdjusterPerformanceRepository;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service Implementation for managing Adjuster Performance.
 */
@Service
@CacheConfig(cacheNames = "adjusters")
public class AdjusterPerformanceServiceImpl implements AdjusterPerformanceService {

    private static final int    LOW_PRODUCTIVITY_THRESHOLD  = 20;
    private static final int    OVERLOADED_THRESHOLD        = 40;
    private static final double DEFAULT_SLA_TAT_TARGET      = 5.0;
    private static final double TRAINING_QUALITY_THRESHOLD  = 70.0;
    private static final double EXCELLENT_QUALITY_THRESHOLD = 90.0;
    private static final double ERROR_RATE_WEIGHT           = 1.5;

    private final AdjusterPerformanceRepository repository;
    private final ModelMapper modelMapper;

    public AdjusterPerformanceServiceImpl(AdjusterPerformanceRepository repository, ModelMapper modelMapper) {
        this.repository = repository;
        this.modelMapper = modelMapper;
    }

    @Override
    @CacheEvict(allEntries = true)
    public AdjusterPerformanceDTO savePerformance(AdjusterPerformanceDTO dto) {
        try {
            validatePerformanceInput(dto);
            AdjusterPerformance entity = modelMapper.map(dto, AdjusterPerformance.class);

            // Calculating Average Turnaround Time
            double rawAvgTat = entity.getClaimsHandled() > 0
                    ? (double) entity.getTotalDaysTaken() / entity.getClaimsHandled()
                    : 0.0;
            entity.setAvgTat(Math.round(rawAvgTat * 100.0) / 100.0);

            // Calculating Quality Score
            double rawQualityScore = Math.max(0, Math.min(100, 100 - (entity.getErrorRate() * ERROR_RATE_WEIGHT)));
            entity.setQualityScore(Math.round(rawQualityScore * 100.0) / 100.0);

            AdjusterPerformance saved = repository.save(entity);
            return computeAndEnrich(saved);
        } catch (InvalidInputException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseOperationException("Error saving performance record: " + e.getMessage());
        }
    }

    /**
     * Fetches a single performance record
     */
    @Override
    @Cacheable(key = "#adjusterId + '-' + #period")
    public Optional<AdjusterPerformanceDTO> getAdjusterPerformance(Long adjusterId, String period) {
        try {
            if (adjusterId == null || period == null || period.isBlank()) {
                throw new InvalidInputException("Adjuster ID and period must not be null or empty.");
            }
            Optional<AdjusterPerformance> entityOpt = repository.findByAdjusterIdAndPeriod(adjusterId, period);
            if (entityOpt.isPresent()) {
                return Optional.of(computeAndEnrich(entityOpt.get()));
            }
            return Optional.empty();
        } catch (InvalidInputException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseOperationException("Error fetching performance: " + e.getMessage());
        }
    }

    @Override
    @Cacheable(key = "'all-' + #period")
    public List<AdjusterPerformanceDTO> listAllAdjusterPerformance(String period) {
        try {
            List<AdjusterPerformance> entities = (period == null || period.isBlank())
                    ? repository.findAll()
                    : repository.findByPeriod(period);
            return mapAndEnrichList(entities, period != null ? period : "all");
        } catch (Exception e) {
            handleException(e, "listing performance records");
            return null;
        }
    }

    @Override
    @Cacheable(key = "'top-' + #period")
    public List<AdjusterPerformanceDTO> getTopPerformers(String period) {
        try {
            validatePeriod(period);
            List<AdjusterPerformance> entities = repository.findByPeriodOrderByErrorRateAscSlaBreachedCountAsc(period);
            return mapAndEnrichList(entities, period);
        } catch (Exception e) {
            handleException(e, "fetching top performers");
            return null;
        }
    }

    @Override
    @Cacheable(key = "'training-' + #period")
    public List<AdjusterPerformanceDTO> getAdjustersFlaggedForTraining(String period) {
        try {
            validatePeriod(period);
            double errorThreshold = (100 - TRAINING_QUALITY_THRESHOLD) / ERROR_RATE_WEIGHT;
            List<AdjusterPerformance> entities = repository.findByErrorRateGreaterThanAndPeriod(errorThreshold, period);
            return mapAndEnrichList(entities, period);
        } catch (Exception e) {
            handleException(e, "fetching training-flagged adjusters");
            return null;
        }
    }

    @Override
    @Cacheable(key = "'low-' + #threshold + '-' + #period")
    public List<AdjusterPerformanceDTO> getLowProductivityAdjusters(int threshold, String period) {
        try {
            validatePeriod(period);
            if (threshold <= 0) throw new InvalidInputException("Threshold must be > 0.");
            List<AdjusterPerformance> entities = repository.findByClaimsHandledLessThanAndPeriod(threshold, period);
            return mapAndEnrichList(entities, period);
        } catch (Exception e) {
            handleException(e, "fetching low-productivity adjusters");
            return null;
        }
    }

    @Override
    @Cacheable(key = "'over-' + #threshold + '-' + #period")
    public List<AdjusterPerformanceDTO> getOverloadedAdjusters(int threshold, String period) {
        try {
            validatePeriod(period);
            if (threshold <= 0) throw new InvalidInputException("Threshold must be > 0.");
            List<AdjusterPerformance> entities = repository.findByClaimsHandledGreaterThanAndPeriod(threshold, period);
            return mapAndEnrichList(entities, period);
        } catch (Exception e) {
            handleException(e, "fetching overloaded adjusters");
            return null;
        }
    }

    @Override
    @Cacheable(key = "'slow-' + #slaTatTarget + '-' + #period")
    public List<AdjusterPerformanceDTO> getSlowPerformers(double slaTatTarget, String period) {
        try {
            validatePeriod(period);
            if (slaTatTarget <= 0) throw new InvalidInputException("SLA TAT target must be > 0.");
            List<AdjusterPerformance> entities = repository.findSlowPerformers(period, slaTatTarget);
            return mapAndEnrichList(entities, period);
        } catch (Exception e) {
            handleException(e, "fetching slow performers");
            return null;
        }
    }

    @Override
    @Cacheable(key = "'denial-' + #benchmark + '-' + #period")
    public List<AdjusterPerformanceDTO> getAdjustersWithHighDenialRate(double benchmark, String period) {
        try {
            validatePeriod(period);
            if (benchmark < 0 || benchmark > 100) throw new InvalidInputException("Benchmark must be 0-100.");
            List<AdjusterPerformance> entities = repository.findByHighDenialRate(period, benchmark);
            return mapAndEnrichList(entities, period);
        } catch (Exception e) {
            handleException(e, "fetching high denial rate adjusters");
            return null;
        }
    }

    /**
     * Updates an existing performance record by recalculating persistent metrics.
     */
    @CacheEvict(allEntries = true)
    @Override
    public AdjusterPerformanceDTO updatePerformance(long perfId, AdjusterPerformanceDTO dto) {
        try {
            validatePerformanceInput(dto);
            AdjusterPerformance existingEntity = repository.findById(perfId).orElseThrow(() -> new ResourceNotFoundException("Performance record not found for ID: " + perfId));
            modelMapper.map(dto, existingEntity);
            existingEntity.setPerfId(perfId);
            double rawAvgTat = existingEntity.getClaimsHandled() > 0
                    ? (double) existingEntity.getTotalDaysTaken() / existingEntity.getClaimsHandled()
                    : 0.0;
            existingEntity.setAvgTat(Math.round(rawAvgTat * 100.0) / 100.0);
            double rawQualityScore = Math.max(0, Math.min(100, 100 - (existingEntity.getErrorRate() * ERROR_RATE_WEIGHT)));
            existingEntity.setQualityScore(Math.round(rawQualityScore * 100.0) / 100.0);
            AdjusterPerformance updatedEntity = repository.save(existingEntity);
            return computeAndEnrich(updatedEntity);
        } catch (ResourceNotFoundException | InvalidInputException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseOperationException("Error updating performance record: " + e.getMessage());
        }
    }

    @Override
    @CacheEvict(allEntries = true)
    public AdjusterPerformanceDTO patchPerformance(Long perfId, Map<String, Object> updates) {
        try {
            // 1. Retrieve the existing entity
            AdjusterPerformance entity = repository.findById(perfId).orElseThrow(() -> new ResourceNotFoundException("Performance record not found: " + perfId));

            updates.forEach((key, value) -> {
                switch (key) {
                    case "claimsHandled" -> entity.setClaimsHandled((Integer) value);
                    case "totalDaysTaken" -> entity.setTotalDaysTaken((Integer) value);
                    case "errorRate" -> entity.setErrorRate(((Number) value).doubleValue());
                    case "deniedClaimsCount" -> entity.setDeniedClaimsCount((Integer) value);
                    case "slaMetCount" -> entity.setSlaMetCount((Integer) value);
                    case "slaBreachedCount" -> entity.setSlaBreachedCount((Integer) value);
                    case "period" -> entity.setPeriod((String) value);
                }
            });
            validatePerformanceAfterPatch(entity);
            double rawAvgTat = entity.getClaimsHandled() > 0
                    ? (double) entity.getTotalDaysTaken() / entity.getClaimsHandled()
                    : 0.0;
            entity.setAvgTat(Math.round(rawAvgTat * 100.0) / 100.0);
            double rawQualityScore = Math.max(0, Math.min(100, 100 - (entity.getErrorRate() * ERROR_RATE_WEIGHT)));
            entity.setQualityScore(Math.round(rawQualityScore * 100.0) / 100.0);
            AdjusterPerformance updated = repository.save(entity);
            return computeAndEnrich(updated);

        } catch (ResourceNotFoundException | InvalidInputException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseOperationException("Failed to patch performance record: " + e.getMessage());
        }
    }

    private void validatePerformanceAfterPatch(AdjusterPerformance entity) {
        if (entity.getErrorRate() < 0 || entity.getErrorRate() > 100) {
            throw new InvalidInputException("Error rate must be between 0 and 100.");
        }
        if (entity.getDeniedClaimsCount() > entity.getClaimsHandled()) {
            throw new InvalidInputException("Denied claims cannot exceed total claims handled.");
        }
    }

    @Override
    @Cacheable(key = "'sla-risk-' + #period")
    public List<AdjusterPerformanceDTO> getAdjustersBelowSlaCompliance(String period) {
        try {
            validatePeriod(period);
            List<AdjusterPerformance> entities = repository.findBelowSlaComplianceThreshold(period);
            return mapAndEnrichList(entities, period);
        } catch (Exception e) {
            handleException(e, "fetching SLA non-compliant adjusters");
            return null;
        }
    }

    @CacheEvict(allEntries = true)
    @Override
    public void deletePerformance(Long perfId) {
        if (perfId == null) throw new InvalidInputException("ID must not be null.");
        if (!repository.existsById(perfId)) throw new ResourceNotFoundException("Record not found.");
        repository.deleteById(perfId);
    }

    /**
     * Core logic to enrich a persisted entity with runtime-calculated fields.
     */
    private AdjusterPerformanceDTO computeAndEnrich(AdjusterPerformance entity) {
        AdjusterPerformanceDTO dto = modelMapper.map(entity, AdjusterPerformanceDTO.class);

        double avgTat       = entity.getAvgTat();
        double qualityScore = entity.getQualityScore();

        // 1. Denial Rate Calculation
        double rawDenialRate = entity.getClaimsHandled() > 0
                ? ((double) entity.getDeniedClaimsCount() / entity.getClaimsHandled()) * 100
                : 0.0;
        double denialRate = Math.round(rawDenialRate * 100.0) / 100.0;

        // 2. SLA Compliance Rate Calculation
        int totalSla = entity.getSlaMetCount() + entity.getSlaBreachedCount();
        double rawSlaRate = totalSla > 0
                ? ((double) entity.getSlaMetCount() / totalSla) * 100
                : 100.0;
        double slaComplianceRate = Math.round(rawSlaRate * 100.0) / 100.0;

        // 3. TAT Score (Normalized to 100)
        double tatScore = Math.max(0, 100 - (avgTat * 10));

        // 4. Balanced Performance Index calculation
        double rawIndex = (qualityScore * 0.35)
                + (slaComplianceRate * 0.30)
                + ((100 - denialRate) * 0.20)
                + (tatScore * 0.15);
        double performanceIndex = Math.round(rawIndex * 100.0) / 100.0;

        // 5. Categorize Productivity
        String productivityFlag = (entity.getClaimsHandled() < LOW_PRODUCTIVITY_THRESHOLD) ? "LOW_PRODUCTIVITY" :
                (entity.getClaimsHandled() > OVERLOADED_THRESHOLD) ? "OVERLOADED" : "NORMAL";

        // 6. Categorize Overall Performance
        String performanceFlag;
        if (qualityScore < TRAINING_QUALITY_THRESHOLD) performanceFlag = "TRAINING_REQUIRED";
        else if (avgTat > DEFAULT_SLA_TAT_TARGET) performanceFlag = "SLOW_PERFORMER";
        else if (qualityScore >= EXCELLENT_QUALITY_THRESHOLD) performanceFlag = "EXCELLENT";
        else performanceFlag = "GOOD";

        dto.setDenialRate(denialRate);
        dto.setSlaComplianceRate(slaComplianceRate);
        dto.setPerformanceIndex(performanceIndex);
        dto.setProductivityFlag(productivityFlag);
        dto.setPerformanceFlag(performanceFlag);

        return dto;
    }



    // ── Helper Methods ───────────────────────────────────────────────────

    private void validatePeriod(String period) {
        if (period == null || period.isBlank()) throw new InvalidInputException("Period required.");
    }

    private List<AdjusterPerformanceDTO> mapAndEnrichList(List<AdjusterPerformance> entities, String period) {
        if (entities.isEmpty()) throw new ResourceNotFoundException("No records for: " + period);
        List<AdjusterPerformanceDTO> dtos = new ArrayList<>();
        for (AdjusterPerformance entity : entities) dtos.add(computeAndEnrich(entity));
        return dtos;
    }

    private void handleException(Exception e, String action) {
        if (e instanceof InvalidInputException || e instanceof ResourceNotFoundException) throw (RuntimeException) e;
        throw new DatabaseOperationException("Error " + action + ": " + e.getMessage());
    }

    private void validatePerformanceInput(AdjusterPerformanceDTO dto) {
        if (dto == null) throw new InvalidInputException("Data must not be null.");
        if (dto.getAdjusterId() == null) throw new InvalidInputException("Adjuster ID required.");
        if (dto.getClaimsHandled() < 0 || dto.getErrorRate() < 0 || dto.getErrorRate() > 100) {
            throw new InvalidInputException("Numeric values out of valid range.");
        }
        int totalSlaAccountedFor = dto.getSlaMetCount() + dto.getSlaBreachedCount();
        if (dto.getClaimsHandled() < totalSlaAccountedFor) {
            throw new InvalidInputException("Claims handled must be equal to or greater than the sum of SLA met and breached counts");
        }

        // Rule 2: Claims Handled vs Denied Claims
        if (dto.getDeniedClaimsCount() > dto.getClaimsHandled()) {
            throw new InvalidInputException("Denied claims count cannot exceed total claims handled");
        }

        // Rule 3: Ensure totalDaysTaken aligns with claimsHandled
        if (dto.getClaimsHandled() == 0 && dto.getTotalDaysTaken() > 0) {
            throw new InvalidInputException("Total days taken must be 0 if no claims were handled.");
        }
    }




}