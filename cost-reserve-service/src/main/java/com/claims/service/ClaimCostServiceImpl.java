package com.claims.service;

import com.claims.dto.request.AgingRecordRequest;
import com.claims.dto.request.ClaimCostRequest;
import com.claims.dto.request.ClaimReserveRequest;
import com.claims.dto.response.ClaimCostResponse;
import com.claims.dto.response.ClaimReserveResponse;
import com.claims.entity.ClaimCost;
import com.claims.enums.AgingBucket;
import com.claims.enums.CostType;
import com.claims.exception.ResourceNotFoundException;
import com.claims.repository.ClaimCostRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClaimCostServiceImpl implements ClaimCostService {

    private static final Logger log = LoggerFactory.getLogger(ClaimCostServiceImpl.class);

    private final ClaimCostRepository claimCostRepository;
    private final ModelMapper modelMapper;
    private final ClaimReserveService claimReserveService;
    private final AgingRecordService agingRecordService;

    public ClaimCostServiceImpl(ClaimCostRepository claimCostRepository,
                                ModelMapper modelMapper,
                                ClaimReserveService claimReserveService,
                                AgingRecordService agingRecordService) {
        this.claimCostRepository = claimCostRepository;
        this.modelMapper = modelMapper;
        this.claimReserveService = claimReserveService;
        this.agingRecordService = agingRecordService;
    }




    @Override
    public ClaimCostResponse createClaimCost(ClaimCostRequest request) {
        ClaimCost claimCost = modelMapper.map(request, ClaimCost.class);
        ClaimCost saved = claimCostRepository.save(claimCost);
        return modelMapper.map(saved, ClaimCostResponse.class);
    }

    @Override
    public ClaimCostResponse getClaimCostById(Long costId) {
        ClaimCost claimCost = findCostByIdOrThrow(costId);
        return modelMapper.map(claimCost, ClaimCostResponse.class);
    }

    @Override
    public List<ClaimCostResponse> getAllClaimCosts() {
        return claimCostRepository.findAll()
                .stream()
                .map(cost -> modelMapper.map(cost, ClaimCostResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public ClaimCostResponse updateClaimCost(Long costId, ClaimCostRequest request) {
        ClaimCost existing = findCostByIdOrThrow(costId);
        modelMapper.map(request, existing);
        ClaimCost updated = claimCostRepository.save(existing);
        return modelMapper.map(updated, ClaimCostResponse.class);
    }

    @Override
    public void deleteClaimCost(Long costId) {
        ClaimCost existing = findCostByIdOrThrow(costId);
        claimCostRepository.delete(existing);
    }





    @Override
    public List<ClaimCostResponse> getCostsByClaimId(String claimId) {
        List<ClaimCost> costs = claimCostRepository.findByClaimId(claimId);
        if (costs.isEmpty()) {
            throw new ResourceNotFoundException("No cost records found for claimId: " + claimId);
        }
        return costs.stream()
                .map(cost -> modelMapper.map(cost, ClaimCostResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<ClaimCostResponse> getCostsByCostType(CostType costType) {
        List<ClaimCost> costs = claimCostRepository.findByCostType(costType);
        if (costs.isEmpty()) {
            throw new ResourceNotFoundException("No cost records found for costType: " + costType);
        }
        return costs.stream()
                .map(cost -> modelMapper.map(cost, ClaimCostResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<ClaimCostResponse> getCostsByClaimIdAndType(String claimId, CostType costType) {
        List<ClaimCost> costs = claimCostRepository.findByClaimIdAndCostType(claimId, costType);
        if (costs.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No cost records found for claimId: " + claimId + " and costType: " + costType);
        }
        return costs.stream()
                .map(cost -> modelMapper.map(cost, ClaimCostResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<ClaimCostResponse> getCostsByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        List<ClaimCost> costs = claimCostRepository.findByCostDateBetween(startDate, endDate);
        if (costs.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No cost records found between " + startDate + " and " + endDate);
        }
        return costs.stream()
                .map(cost -> modelMapper.map(cost, ClaimCostResponse.class))
                .collect(Collectors.toList());
    }


    @Override
    public BigDecimal getTotalCostByClaimId(String claimId) {
        List<ClaimCost> costs = claimCostRepository.findByClaimId(claimId);
        if (costs.isEmpty()) {
            throw new ResourceNotFoundException("No cost records found for claimId: " + claimId);
        }
        return claimCostRepository.getTotalAmountByClaimId(claimId)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public Map<String, BigDecimal> getCostBreakdownByTypeForClaim(String claimId) {
        List<ClaimCost> costs = claimCostRepository.findByClaimId(claimId);
        if (costs.isEmpty()) {
            throw new ResourceNotFoundException("No cost records found for claimId: " + claimId);
        }
        return costs.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCostType().name(),
                        Collectors.reducing(BigDecimal.ZERO, ClaimCost::getAmount, BigDecimal::add)
                ));
    }

    @Override
    public Map<String, BigDecimal> getOverallCostSummaryByType() {
        List<ClaimCost> allCosts = claimCostRepository.findAll();
        if (allCosts.isEmpty()) {
            throw new ResourceNotFoundException("No cost records found in the system");
        }
        return allCosts.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCostType().name(),
                        Collectors.reducing(BigDecimal.ZERO, ClaimCost::getAmount, BigDecimal::add)
                ));
    }

    @Override
    public Map<String, BigDecimal> getMonthlyCostTrendByClaimId(String claimId) {
        List<ClaimCost> costs = claimCostRepository.findByClaimId(claimId);
        if (costs.isEmpty()) {
            throw new ResourceNotFoundException("No cost records found for claimId: " + claimId);
        }
        return costs.stream()
                .sorted(Comparator.comparing(ClaimCost::getCostDate))
                .collect(Collectors.groupingBy(
                        c -> c.getCostDate().getYear() + "-"
                                + String.format("%02d", c.getCostDate().getMonthValue()),
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, ClaimCost::getAmount, BigDecimal::add)
                ));
    }

    @Override
    public Map<String, Object> getHighestCostClaim() {
        List<ClaimCost> allCosts = claimCostRepository.findAll();
        if (allCosts.isEmpty()) {
            throw new ResourceNotFoundException("No cost records found in the system");
        }
        Map<String, BigDecimal> totalPerClaim = allCosts.stream()
                .collect(Collectors.groupingBy(
                        ClaimCost::getClaimId,
                        Collectors.reducing(BigDecimal.ZERO, ClaimCost::getAmount, BigDecimal::add)
                ));
        String topClaimId = totalPerClaim.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new ResourceNotFoundException("Could not determine highest cost claim"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("claimId", topClaimId);
        result.put("totalCost", totalPerClaim.get(topClaimId).setScale(2, RoundingMode.HALF_UP));
        return result;
    }





    @Override
    public Map<String, Object> autoInitializeClaim(String claimId, String payloadJson) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("claimId", claimId);

        // ── 1. ClaimCost — SETTLEMENT type seeded with claimAmount from payload ──
        double claimAmount = extractDouble(payloadJson, "claimAmount");
        if (claimAmount > 0) {
            ClaimCostRequest costReq = new ClaimCostRequest();
            costReq.setClaimId(claimId);
            costReq.setCostType(CostType.SETTLEMENT);
            costReq.setAmount(BigDecimal.valueOf(claimAmount).setScale(2, java.math.RoundingMode.HALF_UP));
            costReq.setCostDate(LocalDate.now());
            var costResp = createClaimCost(costReq);
            result.put("costId", costResp.getCostId());
            result.put("costAmount", claimAmount);
        } else {
            result.put("costSkipped", "claimAmount not found in payload");
        }

        // ── 2. ClaimReserve — portfolio-median reserve factor (fallback 1.20) ───
        if (claimAmount > 0) {
            double reserveFactor = computePortfolioMedianReserveFactor();
            ClaimReserveRequest reserveReq = new ClaimReserveRequest();
            reserveReq.setClaimId(claimId);
            reserveReq.setReserveAmount(
                    BigDecimal.valueOf(claimAmount * reserveFactor)
                            .setScale(2, java.math.RoundingMode.HALF_UP));
            reserveReq.setUpdatedDate(LocalDate.now());
            var reserveResp = claimReserveService.createClaimReserve(reserveReq);
            result.put("reserveId",     reserveResp.getReserveId());
            result.put("reserveAmount", claimAmount * reserveFactor);
            result.put("reserveFactor", reserveFactor);
        }

        // ── 3. AgingRecord — compute days from incidentDate/filedDate ───────────
        String dateStr = extractString(payloadJson, "incidentDate", "filedDate", "admissionDate");
        int agingDays = 1;
        if (dateStr != null) {
            try {
                agingDays = (int) Math.max(1, ChronoUnit.DAYS.between(
                        LocalDate.parse(dateStr), LocalDate.now()));
            } catch (Exception ignored) {}
        }
        AgingRecordRequest agingReq = new AgingRecordRequest();
        agingReq.setClaimId(claimId);
        agingReq.setAgingDays(agingDays);
        agingReq.setAgingBucket(deriveBucket(agingDays));
        var agingResp = agingRecordService.createAgingRecord(agingReq);
        result.put("agingId", agingResp.getAgingId());
        result.put("agingDays", agingDays);
        result.put("agingBucket", agingResp.getAgingBucket());

        return result;
    }

    // ── Portfolio Reserve Factor ─────────────────────────────────────────────────

    /**
     * Computes the median (ClaimReserve / ClaimCost) ratio from existing SETTLEMENT
     * cost records in the portfolio. This gives a data-driven reserve multiplier that
     * reflects what the organisation has historically needed to hold per dollar of
     * settlement cost.
     *
     * <p>Requires ≥ 3 matched cost–reserve pairs; returns {@code 1.20} (the default
     * 120 % estimate) when the portfolio is too thin or a computation error occurs.</p>
     *
     * <p>The result is clamped to [1.0, 2.5] so extreme outliers cannot produce
     * unrealistic reserve amounts.</p>
     */
    private double computePortfolioMedianReserveFactor() {
        try {
            List<ClaimCost> settlementCosts = claimCostRepository.findByCostType(CostType.SETTLEMENT);
            if (settlementCosts.size() < 3) return 1.2;

            List<Double> ratios = new ArrayList<>();
            for (ClaimCost cost : settlementCosts) {
                if (cost.getAmount() == null || cost.getAmount().compareTo(BigDecimal.ZERO) <= 0) continue;
                try {
                    List<ClaimReserveResponse> reserves = claimReserveService
                            .getReservesByClaimId(cost.getClaimId());
                    if (reserves.isEmpty()) continue;
                    // Use the most recent reserve (latest updatedDate)
                    ClaimReserveResponse latest = reserves.stream()
                            .max(Comparator.comparing(ClaimReserveResponse::getUpdatedDate))
                            .orElse(null);
                    if (latest.getReserveAmount() == null
                            || latest.getReserveAmount().compareTo(BigDecimal.ZERO) <= 0) continue;
                    double ratio = latest.getReserveAmount().doubleValue()
                            / cost.getAmount().doubleValue();
                    ratios.add(ratio);
                } catch (Exception ignored) {
                    // No reserve for this cost record — skip
                }
            }

            if (ratios.size() < 3) return 1.2;

            ratios.sort(Double::compareTo);
            int mid = ratios.size() / 2;
            double median = (ratios.size() % 2 == 0)
                    ? (ratios.get(mid - 1) + ratios.get(mid)) / 2.0
                    : ratios.get(mid);

            double factor = Math.max(1.0, Math.min(2.5, median));
            log.debug("Portfolio median reserve factor computed from {} pairs: {}", ratios.size(), factor);
            return factor;
        } catch (Exception e) {
            log.warn("Portfolio reserve factor computation failed, using default 1.20: {}", e.getMessage());
            return 1.2;
        }
    }

    // ── Payload parsing helpers ──────────────────────────────────────────────────

    private double extractDouble(String json, String... keys) {
        if (json == null) return 0;
        for (String key : keys) {
            try {
                String marker = "\"" + key + "\":";
                int idx = json.indexOf(marker);
                if (idx < 0) continue;
                int start = idx + marker.length();
                while (start < json.length() && json.charAt(start) == ' ') start++;
                int end = start;
                while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) end++;
                if (end > start) return Double.parseDouble(json.substring(start, end));
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private String extractString(String json, String... keys) {
        if (json == null) return null;
        for (String key : keys) {
            try {
                String marker = "\"" + key + "\":\"";
                int idx = json.indexOf(marker);
                if (idx < 0) continue;
                int start = idx + marker.length();
                int end   = json.indexOf('"', start);
                if (end > start) return json.substring(start, end);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private AgingBucket deriveBucket(int agingDays) {
        if (agingDays <= 30)  return AgingBucket.BUCKET_0_30;
        if (agingDays <= 60)  return AgingBucket.BUCKET_31_60;
        if (agingDays <= 90)  return AgingBucket.BUCKET_61_90;
        return AgingBucket.BUCKET_90_PLUS;
    }

    private ClaimCost findCostByIdOrThrow(Long costId) {
        return claimCostRepository.findById(costId)
                .orElseThrow(() -> new ResourceNotFoundException("ClaimCost", "costId", costId));
    }
}

