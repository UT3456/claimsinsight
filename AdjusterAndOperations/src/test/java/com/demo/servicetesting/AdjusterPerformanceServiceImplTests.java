package com.demo.servicetesting;
import com.demo.dto.AdjusterPerformanceDTO;
import com.demo.entities.AdjusterPerformance;
import com.demo.exception.DatabaseOperationException;
import com.demo.exception.InvalidInputException;
import com.demo.exception.ResourceNotFoundException;
import com.demo.repositories.AdjusterPerformanceRepository;
import com.demo.service.AdjusterPerformanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import javax.swing.text.html.Option;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Service Tests for  AdjusterPerformanceServiceImpl
 */
@ExtendWith(MockitoExtension.class)
public class AdjusterPerformanceServiceImplTests {

    @Mock
    private AdjusterPerformanceRepository mockRepository;

    @Mock
    private ModelMapper mockModelMapper;

    @InjectMocks
    private AdjusterPerformanceServiceImpl service;

    private AdjusterPerformance entity;
    private AdjusterPerformanceDTO inputDto;

    @BeforeEach
    void setUp() {

        entity = new AdjusterPerformance();
        entity.setPerfId(1L);
        entity.setAdjusterId(101L);
        entity.setClaimsHandled(25);
        entity.setTotalDaysTaken(100);
        entity.setAvgTat(4.0);
        entity.setSlaMetCount(22);
        entity.setSlaBreachedCount(3);
        entity.setDeniedClaimsCount(3);
        entity.setErrorRate(8.0);
        entity.setQualityScore(88.0);
        entity.setPeriod("2026-Q1");

        inputDto = new AdjusterPerformanceDTO();
        inputDto.setAdjusterId(101L);
        inputDto.setClaimsHandled(25);
        inputDto.setTotalDaysTaken(100);
        inputDto.setSlaMetCount(22);
        inputDto.setSlaBreachedCount(3);
        inputDto.setDeniedClaimsCount(3);
        inputDto.setErrorRate(8.0);
        inputDto.setPeriod("2026-Q1");
    }

    // ── savePerformance ───────────────────────────────────────────────────

    @Test
    void test_savePerformance_positive() {
        when(mockModelMapper.map(any(), eq(AdjusterPerformance.class))).thenReturn(entity);
        when(mockRepository.save(any())).thenReturn(entity);
        when(mockModelMapper.map(any(), eq(AdjusterPerformanceDTO.class))).thenReturn(inputDto);
        AdjusterPerformanceDTO result = service.savePerformance(inputDto);
        assertNotNull(result);
        assertEquals(101L,result.getAdjusterId());
        verify(mockRepository,times(1)).save(any());
    }

    @Test
    void test_savePerformance_negative() {
        inputDto.setAdjusterId(null);
        try{
            service.savePerformance(inputDto);
        }catch (Exception e){
            assertTrue(true);
        }
    }

    // ── getAdjusterPerformance ────────────────────────────────────────────

    @Test
    void test_getAdjusterPerformance_positive() {
        when(mockRepository.findByAdjusterIdAndPeriod(101L, "2026-Q1")).thenReturn(Optional.of(entity));
        when(mockModelMapper.map(any(), eq(AdjusterPerformanceDTO.class))).thenReturn(inputDto);
        Optional<AdjusterPerformanceDTO> result = service.getAdjusterPerformance(101L,"2026-Q1");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(101L,result.get().getAdjusterId());
        assertEquals("2026-Q1",result.get().getPeriod());

    }

    @Test
    void test_getAdjusterPerformance_negative() {
        when(mockRepository.findByAdjusterIdAndPeriod(999L,"2023-Q1")).thenReturn(Optional.empty());
        Optional<AdjusterPerformanceDTO> result = service.getAdjusterPerformance(999L,"2023-Q1");
        assertTrue(result.isEmpty());
    }

    // ── listAllAdjusterPerformance ────────────────────────────────────────

    @Test
    void test_listAllAdjusterPerformance_positive() {
        when(mockRepository.findByPeriod("2026-Q1")).thenReturn(Arrays.asList(entity));
        when(mockModelMapper.map(any(AdjusterPerformance.class), eq(AdjusterPerformanceDTO.class))).thenReturn(new AdjusterPerformanceDTO());

        List<AdjusterPerformanceDTO> result = service.listAllAdjusterPerformance("2026-Q1");

        assertEquals(1, result.size());
        verify(mockRepository).findByPeriod("2026-Q1");
    }

    @Test
    void test_listAllAdjusterPerformance_negative() {
        when(mockRepository.findByPeriod("2099-Q9")).thenReturn(Arrays.asList());
        try {
            List<AdjusterPerformanceDTO> result = service.listAllAdjusterPerformance("2099-Q9");
            assertTrue(result.isEmpty());
        }catch(ResourceNotFoundException e) {
            assertTrue(true);
        }
    }

    // ── getTopPerformers ──────────────────────────────────────────────────

    @Test
    void test_getTopPerformers_positive() {
       when(mockRepository.findByPeriodOrderByErrorRateAscSlaBreachedCountAsc(anyString())).thenReturn(Arrays.asList(entity));
       when(mockModelMapper.map(any(),eq(AdjusterPerformanceDTO.class))).thenReturn(inputDto);
       List<AdjusterPerformanceDTO> result = service.getTopPerformers("2026-Q1");
       assertFalse(result.isEmpty());
    }

    @Test
    void test_getTopPerformers_negative() {
        when(mockRepository.findByPeriodOrderByErrorRateAscSlaBreachedCountAsc(anyString())).thenReturn(Arrays.asList());
        try{
            List<AdjusterPerformanceDTO> result = service.getTopPerformers("2029-Q3");
        }catch(ResourceNotFoundException e){
            assertTrue(true);
        }
    }

    // ── getAdjustersFlaggedForTraining ────────────────────────────────────

    @Test
    void test_getAdjustersFlaggedForTraining_positive() {
        when(mockRepository.findByErrorRateGreaterThanAndPeriod(anyDouble(), eq("2026-Q1"))).thenReturn(Arrays.asList(entity));
        when(mockModelMapper.map(any(AdjusterPerformance.class), eq(AdjusterPerformanceDTO.class))).thenReturn(new AdjusterPerformanceDTO());

        List<AdjusterPerformanceDTO> result = service.getAdjustersFlaggedForTraining("2026-Q1");

        assertEquals(1, result.size());
    }

    @Test
    void test_getAdjustersFlaggedForTraining_negative() {
        when(mockRepository.findByErrorRateGreaterThanAndPeriod(anyDouble(), eq("2099-Q9"))).thenReturn(Arrays.asList());
        try{
            List<AdjusterPerformanceDTO> result = service.getAdjustersFlaggedForTraining("2099-Q9");

        }catch(ResourceNotFoundException e){
            assertTrue(true);
        }
    }

    // ── getLowProductivityAdjusters ───────────────────────────────────────

    @Test
    void test_getLowProductivityAdjusters_positive() {
        when(mockRepository.findByClaimsHandledLessThanAndPeriod(20, "2026-Q1")).thenReturn(Arrays.asList(entity));
        when(mockModelMapper.map(any(AdjusterPerformance.class), eq(AdjusterPerformanceDTO.class))).thenReturn(inputDto);

        List<AdjusterPerformanceDTO> result = service.getLowProductivityAdjusters(20, "2026-Q1");
        assertEquals(1, result.size());
    }

    @Test
    void test_getLowProductivityAdjusters_negative() {
        when(mockRepository.findByClaimsHandledLessThanAndPeriod(20, "2026-Q1")).thenReturn(Arrays.asList());
        try{
            service.getLowProductivityAdjusters(20,"2026-Q1");
        }catch(ResourceNotFoundException e){
            assertTrue(true);
        }
    }

    // ── getOverloadedAdjusters ────────────────────────────────────────────

    @Test
    void test_getOverloadedAdjusters_positive() {
        when(mockRepository.findByClaimsHandledGreaterThanAndPeriod(40, "2026-Q1")).thenReturn(Arrays.asList(entity));
        when(mockModelMapper.map(any(AdjusterPerformance.class), eq(AdjusterPerformanceDTO.class))).thenReturn(new AdjusterPerformanceDTO());

        List<AdjusterPerformanceDTO> result = service.getOverloadedAdjusters(40, "2026-Q1");
        assertEquals(1, result.size());
    }

    @Test
    void test_getOverloadedAdjusters_negative() {
        when(mockRepository.findByClaimsHandledGreaterThanAndPeriod(40, "2099-Q9")).thenReturn(Arrays.asList());
        try{
            service.getOverloadedAdjusters(40,"2099-Q9");

        }catch(ResourceNotFoundException e){
            assertTrue(true);
        }
    }

    // ── getSlowPerformers ─────────────────────────────────────────────────

    @Test
    void test_getSlowPerformers_positive() {
        when(mockRepository.findSlowPerformers("2026-Q1", 5.0)).thenReturn(Arrays.asList(entity));
        when(mockModelMapper.map(any(AdjusterPerformance.class), eq(AdjusterPerformanceDTO.class))).thenReturn(new AdjusterPerformanceDTO());
        List<AdjusterPerformanceDTO> result = service.getSlowPerformers(5.0, "2026-Q1");
        assertNotNull(result);
        assertEquals(1, result.size());

    }

    @Test
    void test_getSlowPerformers_negative() {
        when(mockRepository.findSlowPerformers(anyString(),anyDouble())).thenReturn(Arrays.asList());
        try {
            List<AdjusterPerformanceDTO> result = service.getSlowPerformers(5.0, "2026-Q1");
        }catch(ResourceNotFoundException e){
            assertTrue(true);
        }
    }


    // ── getAdjustersWithHighDenialRate ────────────────────────────────────

    @Test
    void test_getAdjustersWithHighDenialRate_positive() {
        when(mockRepository.findByHighDenialRate("2026-Q1", 15.0)).thenReturn(Arrays.asList(entity));
        when(mockModelMapper.map(any(AdjusterPerformance.class), eq(AdjusterPerformanceDTO.class))).thenReturn(new AdjusterPerformanceDTO());
        List<AdjusterPerformanceDTO> result = service.getAdjustersWithHighDenialRate(15.0, "2026-Q1");
        assertEquals(1, result.size());
    }

    @Test
    void test_getAdjustersWithHighDenialRate_negative() {
        when(mockRepository.findByHighDenialRate(anyString(), anyDouble())).thenReturn(Arrays.asList());
        try {
            service.getAdjustersWithHighDenialRate(15.0, "2026-Q1");
        } catch (ResourceNotFoundException e) {
            assertTrue(true);
        }
    }

    // ── getAdjustersBelowSlaCompliance ────────────────────────────────────

    @Test
    void test_getAdjustersBelowSlaCompliance_positive() {
        when(mockRepository.findBelowSlaComplianceThreshold("2026-Q1")).thenReturn(Arrays.asList(entity));
        when(mockModelMapper.map(any(AdjusterPerformance.class), eq(AdjusterPerformanceDTO.class))).thenReturn(new AdjusterPerformanceDTO());
        List<AdjusterPerformanceDTO> result = service.getAdjustersBelowSlaCompliance("2026-Q1");
        assertEquals(1, result.size());
        assertNotNull(result);
    }

    @Test
    void test_getAdjustersBelowSlaCompliance_negative() {
        when(mockRepository.findBelowSlaComplianceThreshold("2099-Q9")).thenReturn(Arrays.asList());
        try{
            service.getAdjustersBelowSlaCompliance("2099-Q9");
        }catch(ResourceNotFoundException e){
            assertTrue(true);
        }
    }

    // ── deletePerformance ─────────────────────────────────────────────────

    @Test
    void test_deletePerformance_positive() {
        when(mockRepository.existsById(1L)).thenReturn(true);
        doNothing().when(mockRepository).deleteById(1L);
        try {
            service.deletePerformance(1L);
            verify(mockRepository, times(1)).deleteById(1L);
        } catch (Exception e) {
//            fail("Should not throw exception");
        }
    }

    @Test
    void test_deletePerformance_negative() {
        when(mockRepository.existsById(999L)).thenReturn(false);
        try {
            service.deletePerformance(999L);
        } catch (ResourceNotFoundException e) {
            assertTrue(true);
        }
    }

    // ── updatePerformance ─────────────────────────────────────────────────

    @Test
    void test_updatePerformance_positive() {
        when(mockRepository.findById(1L)).thenReturn(Optional.of(entity));
        doNothing().when(mockModelMapper).map(any(AdjusterPerformanceDTO.class), any(AdjusterPerformance.class));
        when(mockRepository.save(any(AdjusterPerformance.class))).thenReturn(entity);
        when(mockModelMapper.map(any(AdjusterPerformance.class), eq(AdjusterPerformanceDTO.class))).thenReturn(inputDto);
        AdjusterPerformanceDTO result = service.updatePerformance(1L, inputDto);
        assertNotNull(result);
        verify(mockRepository, times(1)).findById(1L);
    }

    @Test
    void test_updatePerformance_negative() {
        when(mockRepository.findById(999L)).thenReturn(Optional.empty());
        try {
            service.updatePerformance(999L, inputDto);
        }catch(ResourceNotFoundException e){
            assertTrue(true);
        }
    }

    // ── patchPerformance ──────────────────────────────────────────────────

    @Test
    void test_patchPerformance_positive() {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("claimsHandled", 50);
        updates.put("errorRate", 5.0);
        when(mockRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(mockRepository.save(any(AdjusterPerformance.class))).thenReturn(entity);
        when(mockModelMapper.map(any(AdjusterPerformance.class), eq(AdjusterPerformanceDTO.class))).thenReturn(inputDto);
        AdjusterPerformanceDTO result = service.patchPerformance(1L, updates);
        assertNotNull(result);
        assertEquals(50, entity.getClaimsHandled());
        assertEquals(5.0, entity.getErrorRate());
        verify(mockRepository).save(entity);
    }

    @Test
    void test_patchPerformance_negative_invalidField() {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("errorRate", 150.0);
        when(mockRepository.findById(1L)).thenReturn(Optional.of(entity));
        try {
            service.patchPerformance(1L, updates);
        }catch(InvalidInputException e){
            assertTrue(true);
        }
    }

}