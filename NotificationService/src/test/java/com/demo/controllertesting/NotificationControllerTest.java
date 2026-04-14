package com.demo.controllertesting;


import com.demo.controller.NotificationControllerImpl;
import com.demo.dto.NotificationRequestDTO;
import com.demo.dto.NotificationResponseDTO;
import com.demo.dto.NotificationStatusUpdateDTO;
import com.demo.enums.NotificationCategory;
import com.demo.enums.NotificationStatus;
import com.demo.exception.GlobalExceptionHandler;
import com.demo.exception.NotificationNotFoundException;
import com.demo.services.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationControllerImpl.class)
@Import(GlobalExceptionHandler.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private NotificationResponseDTO sampleResponse;
    private NotificationRequestDTO sampleRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = NotificationResponseDTO.builder()
                .notificationId(1L)
                .userId(10L)
                .title("High Risk Claim Detected — CLM-2024-001")
                .message("Claim CLM-2024-001 has a fraud risk score of 75.")
                .category(NotificationCategory.RISK)
                .referenceId("CLM-2024-001")
                .status(NotificationStatus.UNREAD)
                .createdDate(LocalDateTime.now())
                .build();

        sampleRequest = NotificationRequestDTO.builder()
                .userId(10L)
                .title("High Risk Claim Detected — CLM-2024-001")
                .message("Claim CLM-2024-001 has a fraud risk score of 75.")
                .category(NotificationCategory.RISK)
                .referenceId("CLM-2024-001")
                .build();
    }

    @Test
    void createNotification_Positive_ShouldReturn201WithBody() throws Exception {
        when(notificationService.createNotification(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notificationId").value(1))
                .andExpect(jsonPath("$.data.category").value("RISK"));
    }

    @Test
    void createNotification_Negative_WhenUserIdMissing_ShouldReturn400() throws Exception {
        // userId is null — should fail @NotNull validation
        sampleRequest.setUserId(null);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createNotification_Negative_WhenTitleIsBlank_ShouldReturn400() throws Exception {
        sampleRequest.setTitle("");

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createNotification_Negative_WhenCategoryIsNull_ShouldReturn400() throws Exception {
        sampleRequest.setCategory(null);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getNotificationsForUser_Positive_ShouldReturn200WithList() throws Exception {
        when(notificationService.getNotificationsForUser(10L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/notifications/user/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].userId").value(10));
    }

    @Test
    void getNotificationsForUser_Negative_WhenUserHasNone_ShouldReturn200WithEmptyList() throws Exception {
        when(notificationService.getNotificationsForUser(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/notifications/user/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void getByStatus_Positive_ShouldReturnUnreadNotifications() throws Exception {
        when(notificationService.getByUserIdAndStatus(10L, NotificationStatus.UNREAD))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/notifications/user/10/status/UNREAD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("UNREAD"));
    }

    @Test
    void getByStatus_Negative_WhenInvalidStatus_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/notifications/user/10/status/INVALID_STATUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByCategory_Positive_ShouldReturnRiskNotifications() throws Exception {
        when(notificationService.getByUserIdAndCategory(10L, NotificationCategory.RISK))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/notifications/user/10/category/RISK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].category").value("RISK"));
    }

    @Test
    void getByCategory_Negative_WhenInvalidCategory_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/notifications/user/10/category/UNKNOWN_CATEGORY"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_Positive_ShouldReturn200WithNotification() throws Exception {
        when(notificationService.getById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationId").value(1));
    }

    @Test
    void getById_Negative_WhenNotFound_ShouldReturn404() throws Exception {
        when(notificationService.getById(999L))
                .thenThrow(new NotificationNotFoundException("Notification not found with id: 999"));

        mockMvc.perform(get("/api/notifications/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Notification not found with id: 999"));
    }

    @Test
    void updateStatus_Positive_ShouldReturn200WithUpdatedNotification() throws Exception {
        sampleResponse.setStatus(NotificationStatus.READ);
        when(notificationService.updateStatus(eq(1L), any())).thenReturn(sampleResponse);

        NotificationStatusUpdateDTO dto = new NotificationStatusUpdateDTO(NotificationStatus.READ);

        mockMvc.perform(patch("/api/notifications/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READ"));
    }

    @Test
    void updateStatus_Negative_WhenStatusIsNull_ShouldReturn400() throws Exception {
        NotificationStatusUpdateDTO dto = new NotificationStatusUpdateDTO(null);

        mockMvc.perform(patch("/api/notifications/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateStatus_Negative_WhenNotificationNotFound_ShouldReturn404() throws Exception {
        when(notificationService.updateStatus(eq(999L), any()))
                .thenThrow(new NotificationNotFoundException("Notification not found with id: 999"));

        NotificationStatusUpdateDTO dto = new NotificationStatusUpdateDTO(NotificationStatus.READ);

        mockMvc.perform(patch("/api/notifications/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void markAllAsRead_Positive_ShouldReturn200WithCount() throws Exception {
        when(notificationService.markAllAsRead(10L)).thenReturn(3);

        mockMvc.perform(patch("/api/notifications/user/10/mark-all-read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Marked 3 notifications as read"));
    }

    @Test
    void markAllAsRead_Negative_WhenUserHasNoUnread_ShouldReturn200WithZeroCount() throws Exception {
        when(notificationService.markAllAsRead(10L)).thenReturn(0);

        mockMvc.perform(patch("/api/notifications/user/10/mark-all-read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Marked 0 notifications as read"));
    }

    @Test
    void countUnread_Positive_ShouldReturn200WithCount() throws Exception {
        when(notificationService.countUnread(10L)).thenReturn(5L);

        mockMvc.perform(get("/api/notifications/unread-count/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    void countUnread_Negative_WhenUserHasNoUnread_ShouldReturnZero() throws Exception {
        when(notificationService.countUnread(10L)).thenReturn(0L);

        mockMvc.perform(get("/api/notifications/unread-count/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    void deleteNotification_Positive_ShouldReturn200() throws Exception {
        doNothing().when(notificationService).deleteNotification(1L);

        mockMvc.perform(delete("/api/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Notification deleted successfully"));
    }

    @Test
    void deleteNotification_Negative_WhenNotFound_ShouldReturn404() throws Exception {
        doThrow(new NotificationNotFoundException("Notification not found with id: 999"))
                .when(notificationService).deleteNotification(999L);

        mockMvc.perform(delete("/api/notifications/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Notification not found with id: 999"));
    }
}
