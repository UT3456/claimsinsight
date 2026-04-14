package com.demo.repository;

import com.demo.entities.Notification;
import com.demo.enums.NotificationCategory;
import com.demo.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedDateDesc(Long userId);

    List<Notification> findByUserIdAndStatusOrderByCreatedDateDesc(Long userId, NotificationStatus status);

    List<Notification> findByUserIdAndCategoryOrderByCreatedDateDesc(Long userId, NotificationCategory category);

    long countByUserIdAndStatus(Long userId, NotificationStatus status);

    @Modifying
    @Query("UPDATE Notification n " +
            "SET n.status = 'READ', n.readDate = :now " +
            "WHERE n.userId = :userId AND n.status = 'UNREAD'")
    int markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    boolean existsByReferenceIdAndCategoryAndStatus(String referenceId, NotificationCategory category, NotificationStatus status);
}
