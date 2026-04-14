package com.demo.controller;

import com.demo.client.ClaimsMetricsServiceClient;
import com.demo.client.dto.ClaimKpiDTO;
import com.demo.dto.AdjusterPerformanceDTO;
import com.demo.service.AdjusterPerformanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for managing and analyzing Adjuster Performance metrics.
 */
@EnableAspectJAutoProxy
@RestController
@RequestMapping("/api/adjusters")
public class AdjusterPerformanceController {

    private final AdjusterPerformanceService adjusterPerformanceService;
    private final ClaimsMetricsServiceClient claimsMetricsServiceClient;

    @Autowired
    public AdjusterPerformanceController(AdjusterPerformanceService adjusterPerformanceService,
                                         ClaimsMetricsServiceClient claimsMetricsServiceClient) {
        this.adjusterPerformanceService = adjusterPerformanceService;
        this.claimsMetricsServiceClient = claimsMetricsServiceClient;
    }

    @PostMapping("/performance")
    public ResponseEntity<?> savePerformance(@Valid @RequestBody AdjusterPerformanceDTO dto) {
        AdjusterPerformanceDTO savedDto = adjusterPerformanceService.savePerformance(dto);
        if (savedDto != null) {
            return ResponseEntity.ok(savedDto);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Performance Resource Creation Failed");

        }
    }

    @GetMapping("/{id}/performance")
    public ResponseEntity<AdjusterPerformanceDTO> getPerformance(@PathVariable Long id, @RequestParam String period) {
        Optional<AdjusterPerformanceDTO> performance = adjusterPerformanceService.getAdjusterPerformance(id, period);
        if (performance.isPresent()) {
            return new ResponseEntity<>(performance.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/performance")
    public ResponseEntity<List<AdjusterPerformanceDTO>> getAllPerformance(@RequestParam(required = false) String period) {
        List<AdjusterPerformanceDTO> list = adjusterPerformanceService.listAllAdjusterPerformance(period);
        if (!list.isEmpty()) {
            return new ResponseEntity<>(list, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }    }

    @GetMapping("/top-performers")
    public ResponseEntity<List<AdjusterPerformanceDTO>> getTopPerformers(@RequestParam String period) {
        List<AdjusterPerformanceDTO> list = adjusterPerformanceService.getTopPerformers(period);
        if (!list.isEmpty()) {
            return new ResponseEntity<>(list, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    @GetMapping("/training-flagged")
    public ResponseEntity<List<AdjusterPerformanceDTO>> getTrainingFlagged(@RequestParam String period) {
        List<AdjusterPerformanceDTO> list = adjusterPerformanceService.getAdjustersFlaggedForTraining(period);
        if (!list.isEmpty()) {
            return new ResponseEntity<>(list, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }    }

    @GetMapping("/low-productivity")
    public ResponseEntity<List<AdjusterPerformanceDTO>> getLowProductivity(@RequestParam String period, @RequestParam(defaultValue = "20") int threshold) {
        List<AdjusterPerformanceDTO> list = adjusterPerformanceService.getLowProductivityAdjusters(threshold,period);
        if(!list.isEmpty()){
            return new ResponseEntity<>(list,HttpStatus.OK);
        }
        else{
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    @GetMapping("/overloaded")
    public ResponseEntity<List<AdjusterPerformanceDTO>> getOverloaded(@RequestParam String period, @RequestParam(defaultValue = "40") int threshold) {
        List<AdjusterPerformanceDTO> list = adjusterPerformanceService.getOverloadedAdjusters(threshold,period);
        if(!list.isEmpty()){
            return new ResponseEntity<>(list,HttpStatus.OK);
        }else{
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    @GetMapping("/slow-performers")
    public ResponseEntity<List<AdjusterPerformanceDTO>> getSlowPerformers(@RequestParam String period, @RequestParam(defaultValue = "5.0") double tatTarget) {
        List<AdjusterPerformanceDTO> list = adjusterPerformanceService.getSlowPerformers(tatTarget,period);
        if(!list.isEmpty()){
            return new ResponseEntity<>(list,HttpStatus.OK);
        }else{
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    @GetMapping("/high-denial-rate")
    public ResponseEntity<List<AdjusterPerformanceDTO>> getHighDenialRate(@RequestParam String period, @RequestParam(defaultValue = "15.0") double benchmark) {
        List<AdjusterPerformanceDTO> list = adjusterPerformanceService.getAdjustersWithHighDenialRate(benchmark,period);
        if(!list.isEmpty()){
            return new ResponseEntity<>(list,HttpStatus.OK);
        }else{
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    @PutMapping("/performance/{perfId}")
    public ResponseEntity<AdjusterPerformanceDTO> updatePerformance(
            @PathVariable Long perfId,
            @Valid @RequestBody AdjusterPerformanceDTO dto) {

        AdjusterPerformanceDTO updated = adjusterPerformanceService.updatePerformance(perfId, dto);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/performance/{perfId}")
    public ResponseEntity<AdjusterPerformanceDTO> partialUpdatePerformance(@PathVariable Long perfId, @RequestBody Map<String, Object> updates) {

        AdjusterPerformanceDTO updated = adjusterPerformanceService.patchPerformance(perfId, updates);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/sla-compliance-risk")
    public ResponseEntity<List<AdjusterPerformanceDTO>> getSlaComplianceRisk(@RequestParam String period) {
        List<AdjusterPerformanceDTO> list = adjusterPerformanceService.getAdjustersBelowSlaCompliance(period);
        if(!list.isEmpty()){
            return new ResponseEntity<>(list,HttpStatus.OK);
        }else{
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    @DeleteMapping("/performance/{perfId}")
    public ResponseEntity<String> deletePerformance(@PathVariable Long perfId) {
        try {
            adjusterPerformanceService.deletePerformance(perfId);
            return new ResponseEntity<>("Performance Resource Deleted", HttpStatus.ACCEPTED);
        } catch (Exception e) {
            return new ResponseEntity<>("Performance Resource Not Deleted", HttpStatus.NOT_FOUND);
        }
    }

    /**
     * GET /api/adjusters/claim-kpis/{claimId}
     * Fetches KPI metrics from claims-metrics-service for a specific claim,
     * giving adjusters visibility into TAT, cycle time, severity and loss ratio.
     */
    @GetMapping("/claim-kpis/{claimId}")
    public ResponseEntity<List<ClaimKpiDTO>> getClaimKpis(@PathVariable String claimId) {
        List<ClaimKpiDTO> kpis = claimsMetricsServiceClient.getKpisByClaimId(claimId);
        return ResponseEntity.ok(kpis);
    }
}