package com.claiminsight.ingestion.mapper;

import com.claiminsight.ingestion.dto.IngestionRequestDTO;
import com.claiminsight.ingestion.dto.IngestionResponseDTO;
import com.claiminsight.ingestion.model.ClaimRaw;
import com.claiminsight.ingestion.model.DataFeed;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

/** Maps between Ingestion DTOs and the ClaimRaw entity using ModelMapper. */
@Component
@RequiredArgsConstructor
public class ClaimRawMapper {

    private final ModelMapper modelMapper;

    /** Converts a request DTO to a ClaimRaw entity. */
    public ClaimRaw toEntity(IngestionRequestDTO dto, DataFeed dataFeed) {
        ClaimRaw raw = modelMapper.map(dto, ClaimRaw.class);
        raw.setDataFeed(dataFeed);
        raw.setRawId(null);
        raw.setIngestedDate(null);
        return raw;
    }

    /** Converts a ClaimRaw entity to a response DTO. */
    public IngestionResponseDTO toResponseDTO(ClaimRaw raw) {
        IngestionResponseDTO dto = modelMapper.map(raw, IngestionResponseDTO.class);
        if (raw.getDataFeed() != null) {
            dto.setFeedId(raw.getDataFeed().getFeedId());
            dto.setFeedType(raw.getDataFeed().getFeedType() != null ? raw.getDataFeed().getFeedType().name() : null);
        }
        return dto;
    }
}
