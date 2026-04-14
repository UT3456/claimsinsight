package com.demo.service;

import com.demo.dto.AdjusterPerformanceDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service Interface for Managing and Analyzing Adjuster Performance.
 */
public interface AdjusterPerformanceService {

    AdjusterPerformanceDTO savePerformance(AdjusterPerformanceDTO dto);

    Optional<AdjusterPerformanceDTO> getAdjusterPerformance(Long adjusterId, String period);

    List<AdjusterPerformanceDTO> listAllAdjusterPerformance(String period);

    List<AdjusterPerformanceDTO> getTopPerformers(String period);

    List<AdjusterPerformanceDTO> getAdjustersFlaggedForTraining(String period);

    List<AdjusterPerformanceDTO> getLowProductivityAdjusters(int threshold, String period);

    List<AdjusterPerformanceDTO> getOverloadedAdjusters(int threshold, String period);

    List<AdjusterPerformanceDTO> getSlowPerformers(double slaTatTarget, String period);

    List<AdjusterPerformanceDTO> getAdjustersWithHighDenialRate(double benchmark, String period);


    AdjusterPerformanceDTO updatePerformance(long perfId, AdjusterPerformanceDTO dto);

    AdjusterPerformanceDTO patchPerformance(Long perfId, Map<String, Object> updates);

    List<AdjusterPerformanceDTO> getAdjustersBelowSlaCompliance(String period);

    void deletePerformance(Long perfId);
}