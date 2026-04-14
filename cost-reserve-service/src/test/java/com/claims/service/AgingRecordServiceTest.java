package com.claims.service;

import com.claims.dto.request.AgingRecordRequest;
import com.claims.dto.response.AgingRecordResponse;
import com.claims.enums.AgingBucket;
import com.claims.entity.AgingRecord;
import com.claims.exception.ResourceNotFoundException;
import com.claims.repository.AgingRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgingRecordServiceTest {

    @Mock
    private AgingRecordRepository agingRecordRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AgingRecordServiceImpl agingRecordService;

    private AgingRecord agingRecord;
    private AgingRecordRequest request;
    private AgingRecordResponse response;

    @BeforeEach
    void setUp() {
        agingRecord = new AgingRecord(1L, "CLM001", 45, AgingBucket.BUCKET_31_60);

        request = new AgingRecordRequest("CLM001", 45, null);

        response = new AgingRecordResponse(1L, "CLM001", 45, AgingBucket.BUCKET_31_60);
    }

    @Test
    void testCreateAgingRecord_success() {
        when(modelMapper.map(request, AgingRecord.class)).thenReturn(agingRecord);
        when(agingRecordRepository.save(agingRecord)).thenReturn(agingRecord);
        when(modelMapper.map(agingRecord, AgingRecordResponse.class)).thenReturn(response);

        AgingRecordResponse result = agingRecordService.createAgingRecord(request);

        assertThat(result.getAgingId()).isEqualTo(1L);
        assertThat(result.getAgingBucket()).isEqualTo(AgingBucket.BUCKET_31_60);
        verify(agingRecordRepository).save(agingRecord);
    }

    @Test
    void testCreateAgingRecord_bucketDerived_0_30() {
        AgingRecord rec = new AgingRecord(2L, "CLM002", 20, AgingBucket.BUCKET_0_30);
        AgingRecordRequest req = new AgingRecordRequest("CLM002", 20, null);
        AgingRecordResponse res = new AgingRecordResponse(2L, "CLM002", 20, AgingBucket.BUCKET_0_30);

        when(modelMapper.map(req, AgingRecord.class)).thenReturn(rec);
        when(agingRecordRepository.save(rec)).thenReturn(rec);
        when(modelMapper.map(rec, AgingRecordResponse.class)).thenReturn(res);

        AgingRecordResponse result = agingRecordService.createAgingRecord(req);

        assertThat(result.getAgingBucket()).isEqualTo(AgingBucket.BUCKET_0_30);
    }

    @Test
    void testCreateAgingRecord_bucketDerived_90_plus() {
        AgingRecord rec = new AgingRecord(3L, "CLM003", 100, AgingBucket.BUCKET_90_PLUS);
        AgingRecordRequest req = new AgingRecordRequest("CLM003", 100, null);
        AgingRecordResponse res = new AgingRecordResponse(3L, "CLM003", 100, AgingBucket.BUCKET_90_PLUS);

        when(modelMapper.map(req, AgingRecord.class)).thenReturn(rec);
        when(agingRecordRepository.save(rec)).thenReturn(rec);
        when(modelMapper.map(rec, AgingRecordResponse.class)).thenReturn(res);

        AgingRecordResponse result = agingRecordService.createAgingRecord(req);

        assertThat(result.getAgingBucket()).isEqualTo(AgingBucket.BUCKET_90_PLUS);
    }

    @Test
    void testGetAgingRecordById_success() {
        when(agingRecordRepository.findById(1L)).thenReturn(Optional.of(agingRecord));
        when(modelMapper.map(agingRecord, AgingRecordResponse.class)).thenReturn(response);

        AgingRecordResponse result = agingRecordService.getAgingRecordById(1L);

        assertThat(result.getAgingId()).isEqualTo(1L);
    }

    @Test
    void testGetAgingRecordById_notFound_throwsException() {
        when(agingRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agingRecordService.getAgingRecordById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetAllAgingRecords_success() {
        when(agingRecordRepository.findAll()).thenReturn(List.of(agingRecord));
        when(modelMapper.map(agingRecord, AgingRecordResponse.class)).thenReturn(response);

        List<AgingRecordResponse> result = agingRecordService.getAllAgingRecords();

        assertThat(result).hasSize(1);
    }



    @Test
    void testUpdateAgingRecord_notFound_throwsException() {
        when(agingRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agingRecordService.updateAgingRecord(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testDeleteAgingRecord_success() {
        when(agingRecordRepository.findById(1L)).thenReturn(Optional.of(agingRecord));

        agingRecordService.deleteAgingRecord(1L);

        verify(agingRecordRepository).delete(agingRecord);
    }

    @Test
    void testDeleteAgingRecord_notFound_throwsException() {
        when(agingRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agingRecordService.deleteAgingRecord(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetAgingRecordsByClaimId_success() {
        when(agingRecordRepository.findByClaimId("CLM001")).thenReturn(List.of(agingRecord));
        when(modelMapper.map(agingRecord, AgingRecordResponse.class)).thenReturn(response);

        List<AgingRecordResponse> result = agingRecordService.getAgingRecordsByClaimId("CLM001");

        assertThat(result).hasSize(1);
    }

    @Test
    void testGetAgingRecordsByClaimId_notFound_throwsException() {
        when(agingRecordRepository.findByClaimId("CLM999")).thenReturn(List.of());

        assertThatThrownBy(() -> agingRecordService.getAgingRecordsByClaimId("CLM999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetAgingRecordsByBucket_success() {
        when(agingRecordRepository.findByAgingBucket(AgingBucket.BUCKET_31_60))
                .thenReturn(List.of(agingRecord));
        when(modelMapper.map(agingRecord, AgingRecordResponse.class)).thenReturn(response);

        List<AgingRecordResponse> result = agingRecordService.getAgingRecordsByBucket(AgingBucket.BUCKET_31_60);

        assertThat(result).hasSize(1);
    }

    @Test
    void testGetClaimsAgedBeyond_negativeDays_throwsException() {
        assertThatThrownBy(() -> agingRecordService.getClaimsAgedBeyond(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Days threshold cannot be negative");
    }

    @Test
    void testGetAgingSummaryForClaim_success() {
        when(agingRecordRepository.findByClaimId("CLM001")).thenReturn(List.of(agingRecord));

        Map<String, Object> result = agingRecordService.getAgingSummaryForClaim("CLM001");

        assertThat(result.get("claimId")).isEqualTo("CLM001");
        assertThat(result.get("agingDays")).isEqualTo(45);
        assertThat(result.get("escalationRequired")).isEqualTo(false);
    }

    @Test
    void testGetPortfolioAgingHealth_success() {
        AgingRecord critical = new AgingRecord(2L, "CLM002", 95, AgingBucket.BUCKET_90_PLUS);
        when(agingRecordRepository.findAll()).thenReturn(List.of(agingRecord, critical));

        Map<String, Object> result = agingRecordService.getPortfolioAgingHealth();

        assertThat(result.get("totalClaims")).isEqualTo(2L);
        assertThat(result.get("criticalClaims_90Plus")).isEqualTo(1L);
    }

    @Test
    void testGetPortfolioAgingHealth_noRecords_throwsException() {
        when(agingRecordRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> agingRecordService.getPortfolioAgingHealth())
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
