package com.claims.service;

import com.claims.dto.request.ClaimCostRequest;
import com.claims.dto.response.ClaimCostResponse;
import com.claims.entity.ClaimCost;
import com.claims.enums.CostType;
import com.claims.exception.ResourceNotFoundException;
import com.claims.repository.ClaimCostRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClaimCostServiceImpl implements ClaimCostService {

    private final ClaimCostRepository claimCostRepository;
    private final ModelMapper modelMapper;

    public ClaimCostServiceImpl(ClaimCostRepository claimCostRepository, ModelMapper modelMapper) {
        this.claimCostRepository = claimCostRepository;
        this.modelMapper = modelMapper;
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
                .sorted((a, b) -> a.getCostDate().compareTo(b.getCostDate()))
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





    private ClaimCost findCostByIdOrThrow(Long costId) {
        return claimCostRepository.findById(costId)
                .orElseThrow(() -> new ResourceNotFoundException("ClaimCost", "costId", costId));
    }
}

