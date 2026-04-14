package com.claims.service;

import com.claims.dto.request.ClaimReserveRequest;
import com.claims.dto.response.ClaimReserveResponse;
import com.claims.entity.ClaimReserve;
import com.claims.exception.ResourceNotFoundException;
import com.claims.repository.ClaimReserveRepository;
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
public class ClaimReserveServiceImpl implements ClaimReserveService {

    private final ClaimReserveRepository claimReserveRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public ClaimReserveServiceImpl(ClaimReserveRepository claimReserveRepository, ModelMapper modelMapper) {
        this.claimReserveRepository = claimReserveRepository;
        this.modelMapper = modelMapper;
    }

    // -------------------------------------------------------
    // CRUD Operations
    // -------------------------------------------------------

    @Override
    public ClaimReserveResponse createClaimReserve(ClaimReserveRequest request) {
        ClaimReserve reserve = modelMapper.map(request, ClaimReserve.class);
        ClaimReserve saved = claimReserveRepository.save(reserve);
        return modelMapper.map(saved, ClaimReserveResponse.class);
    }

    @Override
    public ClaimReserveResponse getClaimReserveById(Long reserveId) {
        ClaimReserve reserve = findReserveByIdOrThrow(reserveId);
        return modelMapper.map(reserve, ClaimReserveResponse.class);
    }

    @Override
    public List<ClaimReserveResponse> getAllClaimReserves() {
        return claimReserveRepository.findAll()
                .stream()
                .map(reserve -> modelMapper.map(reserve, ClaimReserveResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public ClaimReserveResponse updateClaimReserve(Long reserveId, ClaimReserveRequest request) {
        ClaimReserve existing = findReserveByIdOrThrow(reserveId);
        modelMapper.map(request, existing);
        ClaimReserve updated = claimReserveRepository.save(existing);
        return modelMapper.map(updated, ClaimReserveResponse.class);
    }

    @Override
    public void deleteClaimReserve(Long reserveId) {
        ClaimReserve existing = findReserveByIdOrThrow(reserveId);
        claimReserveRepository.delete(existing);
    }

    // -------------------------------------------------------
    // Query Operations
    // -------------------------------------------------------

    @Override
    public List<ClaimReserveResponse> getReservesByClaimId(String claimId) {
        List<ClaimReserve> reserves = claimReserveRepository.findByClaimId(claimId);
        if (reserves.isEmpty()) {
            throw new ResourceNotFoundException("No reserve records found for claimId: " + claimId);
        }
        return reserves.stream()
                .map(reserve -> modelMapper.map(reserve, ClaimReserveResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<ClaimReserveResponse> getReservesByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        List<ClaimReserve> reserves = claimReserveRepository.findByUpdatedDateBetween(startDate, endDate);
        if (reserves.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No reserve records found between " + startDate + " and " + endDate);
        }
        return reserves.stream()
                .map(reserve -> modelMapper.map(reserve, ClaimReserveResponse.class))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------
    // Analytics
    // -------------------------------------------------------

    @Override
    public ClaimReserveResponse getLatestReserveForClaim(String claimId) {
        List<ClaimReserve> reserves = claimReserveRepository.findLatestReserveByClaimId(claimId);
        if (reserves.isEmpty()) {
            throw new ResourceNotFoundException("No reserve records found for claimId: " + claimId);
        }
        return modelMapper.map(reserves.get(0), ClaimReserveResponse.class);
    }

    @Override
    public BigDecimal getTotalReserveAmount() {
        return claimReserveRepository.getTotalReserveAmount()
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public List<ClaimReserveResponse> getReserveHistoryByClaimId(String claimId) {
        List<ClaimReserve> reserves = claimReserveRepository.findByClaimId(claimId);
        if (reserves.isEmpty()) {
            throw new ResourceNotFoundException("No reserve records found for claimId: " + claimId);
        }
        return reserves.stream()
                .sorted((a, b) -> a.getUpdatedDate().compareTo(b.getUpdatedDate()))
                .map(reserve -> modelMapper.map(reserve, ClaimReserveResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, BigDecimal> getLatestReserveSummaryAllClaims() {
        List<ClaimReserve> allReserves = claimReserveRepository.findAll();
        if (allReserves.isEmpty()) {
            throw new ResourceNotFoundException("No reserve records found in the system");
        }
        return allReserves.stream()
                .collect(Collectors.toMap(
                        ClaimReserve::getClaimId,
                        ClaimReserve::getReserveAmount,
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public Map<String, BigDecimal> getMonthlyReserveTrendByClaimId(String claimId) {
        List<ClaimReserve> reserves = claimReserveRepository.findByClaimId(claimId);
        if (reserves.isEmpty()) {
            throw new ResourceNotFoundException("No reserve records found for claimId: " + claimId);
        }
        return reserves.stream()
                .sorted((a, b) -> a.getUpdatedDate().compareTo(b.getUpdatedDate()))
                .collect(Collectors.toMap(
                        r -> r.getUpdatedDate().getYear() + "-"
                                + String.format("%02d", r.getUpdatedDate().getMonthValue()),
                        ClaimReserve::getReserveAmount,
                        (existing, replacement) -> replacement,
                        LinkedHashMap::new
                ));
    }

    @Override
    public Map<String, Object> getReserveAdequacyForClaim(String claimId, BigDecimal totalCost) {
        List<ClaimReserve> reserves = claimReserveRepository.findLatestReserveByClaimId(claimId);
        if (reserves.isEmpty()) {
            throw new ResourceNotFoundException("No reserve records found for claimId: " + claimId);
        }
        BigDecimal latestReserve = reserves.get(0).getReserveAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal variance = latestReserve.subtract(totalCost).setScale(2, RoundingMode.HALF_UP);
        String adequacyStatus = variance.compareTo(BigDecimal.ZERO) >= 0 ? "ADEQUATE" : "UNDER_RESERVED";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("claimId", claimId);
        result.put("latestReserve", latestReserve);
        result.put("totalCost", totalCost.setScale(2, RoundingMode.HALF_UP));
        result.put("variance", variance);
        result.put("adequacyStatus", adequacyStatus);
        return result;
    }

    // -------------------------------------------------------
    // Private Helper
    // -------------------------------------------------------

    private ClaimReserve findReserveByIdOrThrow(Long reserveId) {
        return claimReserveRepository.findById(reserveId)
                .orElseThrow(() -> new ResourceNotFoundException("ClaimReserve", "reserveId", reserveId));
    }
}



