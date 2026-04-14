package com.claim360.denialleakage.service;

import com.claim360.denialleakage.dto.DenialPatternRequest;
import com.claim360.denialleakage.dto.DenialPatternResponse;

import java.util.List;

public interface DenialPatternService {

    //new denial pattern
    DenialPatternResponse createDenialPattern(DenialPatternRequest request);

    // Getby ID
    DenialPatternResponse getDenialPatternById(Long patternId);

    // Get all
    List<DenialPatternResponse> getAllDenialPatterns();

    // forspecific claim
    List<DenialPatternResponse> getDenialPatternsByClaimId(String claimId);

    // bydenial code
    List<DenialPatternResponse> getDenialPatternsByDenialCode(String denialCode);

    // reason keyword
    List<DenialPatternResponse> getDenialPatternsByReasonKeyword(String keyword);

    //byclaim and denial code
    List<DenialPatternResponse> getDenialPatternsByClaimIdAndDenialCode(String claimId, String denialCode);

    // Update
    DenialPatternResponse updateDenialPattern(Long patternId, DenialPatternRequest request);

    // Delete
    void deleteDenialPattern(Long patternId);
}