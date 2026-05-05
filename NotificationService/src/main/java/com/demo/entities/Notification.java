package com.demo.entities;

import com.demo.enums.NotificationCategory;
import com.demo.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * TABLE: notifications
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_user_id",        columnList = "user_id"),
    @Index(name = "idx_notif_status",         columnList = "user_id,status"),
    @Index(name = "idx_notif_ref_cat_status", columnList = "reference_id,category,status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private NotificationCategory category;

    // claim_id / violation_id / score_id that caused this alert
    @Column(name = "reference_id", length = 50)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    // Null until the user actually opens the notification
    @Column(name = "read_date")
    private LocalDateTime readDate;

    @PrePersist
    public void prePersist() {
        if (createdDate == null) createdDate = LocalDateTime.now();
        if (status == null)      status      = NotificationStatus.UNREAD;
    }
}