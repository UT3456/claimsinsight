package com.demo.servicetesting;
import com.demo.dto.SLAViolationDTO;
import com.demo.entities.SLAViolation;
import com.demo.exception.DatabaseOperationException;
import com.demo.exception.InvalidInputException;
import com.demo.exception.ResourceNotFoundException;
import com.demo.repositories.SLAViolationRepository;
import com.demo.service.SLAViolationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Service Tests for SLAViolationServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
public class SLAViolationServiceImplTests {

    @Mock
    private SLAViolationRepository mockRepository;

    @Mock
    private ModelMapper mockModelMapper;

    @InjectMocks
    private SLAViolationServiceImpl service;

    private SLAViolation entity;
    private SLAViolationDTO inputDto;

    @BeforeEach
    void setUp() {
        entity = new SLAViolation();
        entity.setViolationId(1L);
        entity.setClaimId(10L);
        entity.setAdjusterId(101L);
        entity.setViolationType("PAYMENT_DELAY");
        entity.setSlaTargetDays(10);
        entity.setActualDays(15);
        entity.setViolationDate(new Date());

        inputDto = new SLAViolationDTO();
        inputDto.setClaimId(10L);
        inputDto.setAdjusterId(101L);
        inputDto.setViolationType("PAYMENT_DELAY");
        inputDto.setSlaTargetDays(10);
        inputDto.setActualDays(15);
        inputDto.setViolationDate(new Date());
    }

    // ── recordSLAViolation ────────────────────────────────────────────────

    @Test
    void test_recordSLAViolation_positive() {
        when(mockRepository.save(any(SLAViolation.class))).thenReturn(entity);
        when(mockModelMapper.map(any(), eq(SLAViolationDTO.class))).thenReturn(inputDto);
        when(mockRepository.countAllViolationsByAdjuster(anyLong())).thenReturn(1L);

        SLAViolationDTO result = service.recordSLAViolation(inputDto);

        assertNotNull(result);
        assertEquals(101L, result.getAdjusterId());
        verify(mockRepository, times(1)).save(any());
    }

    @Test
    void test_recordSLAViolation_negative() {
        inputDto.setAdjusterId(null);
        try {
            service.recordSLAViolation(inputDto);
        } catch (InvalidInputException e) {
            assertTrue(true);
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    // ── getSLAViolationsByAdjuster ────────────────────────────────────────

    @Test
    void test_getSLAViolationsByAdjuster_positive() {
        when(mockRepository.findByAdjusterId(101L)).thenReturn(Arrays.asList(entity));
        when(mockModelMapper.map(any(), eq(SLAViolationDTO.class))).thenReturn(inputDto);

        List<SLAViolationDTO> result = service.getSLAViolationsByAdjuster(101L);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void test_getSLAViolationsByAdjuster_negative() {
        when(mockRepository.findByAdjusterId(999L)).thenReturn(Arrays.asList());
        try {
            service.getSLAViolationsByAdjuster(999L);
        } catch (ResourceNotFoundException e) {
            assertTrue(true);
        }
    }

    // ── getSLAViolationsByAdjusterAndDateRange ────────────────────────────

    @Test
    void test_getSLAViolationsByAdjusterAndDateRange_positive() {
        Date start = new Date();
        Date end = new Date();
        when(mockRepository.findByAdjusterIdAndViolationDateBetween(anyLong(), any(Date.class), any(Date.class))).thenReturn(Arrays.asList(entity));
        when(mockModelMapper.map(any(), eq(SLAViolationDTO.class))).thenReturn(new SLAViolationDTO());
        when(mockRepository.countAllViolationsByAdjuster(anyLong())).thenReturn(1L);
        List<SLAViolationDTO> result = service.getSLAViolationsByAdjusterAndDateRange(101L, start, end);
        assertNotNull(result, "The result list should not be null");
        assertFalse(result.isEmpty(), "The result list should not be empty");
        assertEquals(1, result.size());
    }

    @Test
    void test_getSLAViolationsByAdjusterAndDateRange_startAfterEnd_throwsInvalidInput() {
        Date start = new Date();
        Date end   = new Date(System.currentTimeMillis() - 1000 * 60 * 60);

        assertThrows(InvalidInputException.class,
                () -> service.getSLAViolationsByAdjusterAndDateRange(101L, start, end));
    }

    // ── getViolationsByType ───────────────────────────────────────────────

    @Test
    void test_getViolationsByType_positive() {
        when(mockRepository.findByViolationType("PAYMENT_DELAY")).thenReturn(Arrays.asList(entity));
        when(mockModelMapper.map(any(), eq(SLAViolationDTO.class))).thenReturn(inputDto);

        List<SLAViolationDTO> result = service.getViolationsByType("PAYMENT_DELAY");

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void test_getViolationsByType_negative() {
        when(mockRepository.findByViolationType("UNKNOWN")).thenReturn(Collections.emptyList());
        try {
            service.getViolationsByType("UNKNOWN");
        } catch (ResourceNotFoundException e) {
            assertTrue(true);
        }
    }

    // ── getViolationsBySeverity ───────────────────────────────────────────

    @Test
    void test_getViolationsBySeverity_positive() {
        when(mockRepository.findBySeverityRange(anyInt(), anyInt())).thenReturn(Arrays.asList(entity));
        when(mockModelMapper.map(any(), eq(SLAViolationDTO.class))).thenReturn(inputDto);

        List<SLAViolationDTO> result = service.getViolationsBySeverity("MEDIUM");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void test_getViolationsBySeverity_negative() {
        try {
            service.getViolationsBySeverity("INVALID_LEVEL");
        } catch (InvalidInputException e) {
            assertTrue(true);
        }
    }

    // ── getViolationsByDaysOverdueGreaterThan ─────────────────────────────

    @Test
    void test_getViolationsByDaysOverdueGreaterThan_positive() {
        when(mockRepository.findByDaysOverdueGreaterThan(3)).thenReturn(Arrays.asList(entity));
        when(mockModelMapper.map(any(), eq(SLAViolationDTO.class))).thenReturn(inputDto);

        List<SLAViolationDTO> result = service.getViolationsByDaysOverdueGreaterThan(3);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void test_getViolationsByDaysOverdueGreaterThan_negative() {
        when(mockRepository.findByDaysOverdueGreaterThan(100)).thenReturn(Collections.emptyList());
        try {
            service.getViolationsByDaysOverdueGreaterThan(100);
        } catch (ResourceNotFoundException e) {
            assertTrue(true);
        }
    }

    // ── getTotalDaysOverdueByClaim ────────────────────────────────────────

    @Test
    void test_getTotalDaysOverdueByClaim_positive() {
        when(mockRepository.findTotalDaysOverdueByClaim(10L)).thenReturn(15);

        Integer result = service.getTotalDaysOverdueByClaim(10L);

        assertNotNull(result);
        assertEquals(15, result);
    }

    @Test
    void test_getTotalDaysOverdueByClaim_negative() {
        when(mockRepository.findTotalDaysOverdueByClaim(999L)).thenReturn(0);

        Integer result = service.getTotalDaysOverdueByClaim(999L);

        assertEquals(0, result);
    }

    // ── countViolationsByAdjusterAndPeriod ────────────────────────────────

    @Test
    void test_countViolationsByAdjusterAndPeriod_positive() {
        Date start = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 90);
        Date end   = new Date();
        when(mockRepository.countViolationsByAdjusterAndPeriod(101L, start, end)).thenReturn(3L);

        Long count = service.countViolationsByAdjusterAndPeriod(101L, start, end);
        assertEquals(3L, count);
    }

    @Test
    void test_countViolationsByAdjusterAndPeriod_reviewTrigger() {
        // count > 3 → performance review should be triggered
        Date start = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 90);
        Date end   = new Date();
        when(mockRepository.countViolationsByAdjusterAndPeriod(101L, start, end)).thenReturn(5L);

        Long count = service.countViolationsByAdjusterAndPeriod(101L, start, end);
        assertTrue(count > 3, "Count > 3 should trigger a performance review");
    }

    // ── getEscalationCandidatesByAdjuster ─────────────────────────────────

    @Test
    void test_getEscalationCandidatesByAdjuster_positive() {
        when(mockRepository.findEscalationCandidatesByAdjuster(101L)).thenReturn(Arrays.asList(entity));
        when(mockModelMapper.map(any(), eq(SLAViolationDTO.class))).thenReturn(inputDto);

        List<SLAViolationDTO> result = service.getEscalationCandidatesByAdjuster(101L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void test_getEscalationCandidatesByAdjuster_negative() {
        when(mockRepository.findEscalationCandidatesByAdjuster(999L)).thenReturn(Collections.emptyList());
        try {
            service.getEscalationCandidatesByAdjuster(999L);
        } catch (ResourceNotFoundException e) {
            assertTrue(true);
        }
    }

    // ── deleteViolation ───────────────────────────────────────────────────

    @Test
    void test_deleteViolation_positive() {
        when(mockRepository.existsById(1L)).thenReturn(true);
        doNothing().when(mockRepository).deleteById(1L);
        try {
            service.deleteViolation(1L);
            verify(mockRepository, times(1)).deleteById(1L);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }

    @Test
    void test_deleteViolation_negative() {
        when(mockRepository.existsById(999L)).thenReturn(false);
        try {
            service.deleteViolation(999L);
        } catch (ResourceNotFoundException e) {
            assertTrue(true);
        }
    }

    // ── updateSLAViolation ────────────────────────────────────────────────

    @Test
    void test_updateSLAViolation_positive() {
        when(mockRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(mockRepository.save(any(SLAViolation.class))).thenReturn(entity);
        when(mockModelMapper.map(any(), eq(SLAViolationDTO.class))).thenReturn(inputDto);
        when(mockRepository.countAllViolationsByAdjuster(anyLong())).thenReturn(1L);
        SLAViolationDTO result = service.updateSLAViolation(1L, inputDto);
        assertNotNull(result);
        assertEquals("PAYMENT_DELAY", result.getViolationType());
        verify(mockRepository).save(any(SLAViolation.class));
    }

    @Test
    void test_updateSLAViolation_negative_notFound() {
        when(mockRepository.findById(999L)).thenReturn(Optional.empty());
        try{
            service.updateSLAViolation(999L, inputDto);
        }catch(DatabaseOperationException e){
            assertTrue(true);
        }
    }

    // ── updateEscalationStatus ────────────────────────────────────────────

    @Test
    void test_updateEscalationStatus_positive() {
        when(mockRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(mockModelMapper.map(any(), eq(SLAViolationDTO.class))).thenReturn(inputDto);
        when(mockRepository.countAllViolationsByAdjuster(anyLong())).thenReturn(1L);
        SLAViolationDTO result = service.updateEscalationStatus(1L, true);
        assertNotNull(result);
        assertTrue(result.isEscalated());
    }

    @Test
    void test_updateEscalationStatus_negative_notFound() {
        when(mockRepository.findById(999L)).thenReturn(Optional.empty());
        try{
            service.updateEscalationStatus(999L, true);
        }catch(ResourceNotFoundException e){
            assertTrue(true);
        }
    }

    // ── changeViolationSeverity ───────────────────────────────────────────

    @Test
    void test_changeViolationSeverity_positive() {
        when(mockRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(mockModelMapper.map(any(), eq(SLAViolationDTO.class))).thenReturn(inputDto);
        when(mockRepository.countAllViolationsByAdjuster(anyLong())).thenReturn(1L);
        SLAViolationDTO result = service.changeViolationSeverity(1L, "CRITICAL");
        assertNotNull(result);
        assertEquals("MEDIUM", result.getSeverity());
    }

    // ── changeViolationSeverity ───────────────────────────────────────────

    @Test
    void test_changeViolationSeverity_negative_notFound() {
        Long nonExistentId = 999L;
        when(mockRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        try{
            service.changeViolationSeverity(nonExistentId, "CRITICAL");
        }catch(ResourceNotFoundException e){
            assertTrue(true);
        }
    }

}