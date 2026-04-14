package com.claiminsight.ingestion.mapper;

import com.claiminsight.ingestion.dto.DataFeedRequestDTO;
import com.claiminsight.ingestion.dto.DataFeedResponseDTO;
import com.claiminsight.ingestion.model.DataFeed;
import com.claiminsight.ingestion.model.FeedStatus;
import com.claiminsight.ingestion.model.FeedType;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

/** Maps between DataFeed DTOs and the DataFeed entity using ModelMapper. */
@Component
@RequiredArgsConstructor
public class DataFeedMapper {

    private final ModelMapper modelMapper;

    /** Converts a request DTO to a DataFeed entity. */
    public DataFeed toEntity(DataFeedRequestDTO dto, FeedType feedType, FeedStatus feedStatus) {
        DataFeed feed = modelMapper.map(dto, DataFeed.class);
        feed.setFeedType(feedType);
        feed.setStatus(feedStatus);
        feed.setFeedId(null);
        feed.setCreatedDate(null);
        feed.setLastSyncDate(null);
        return feed;
    }

    /** Converts a DataFeed entity to a response DTO. */
    public DataFeedResponseDTO toResponseDTO(DataFeed feed) {
        DataFeedResponseDTO dto = modelMapper.map(feed, DataFeedResponseDTO.class);
        dto.setFeedType(feed.getFeedType() != null ? feed.getFeedType().name() : null);
        dto.setStatus(feed.getStatus() != null ? feed.getStatus().name() : null);
        return dto;
    }
}
