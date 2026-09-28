package com.backend.backend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.backend.backend.models.Subscription;
import com.backend.backend.models.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserId(Long userId);
    long countByStatus(SubscriptionStatus status);
    /**
     * Row lock used when debiting/refunding credits, so two concurrent scans
     * for the same user can't both spend the same credit. Combined with the
     * entity's @Version column this makes "spend a credit" safe.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Subscription s where s.id = :id")
    Optional<Subscription> findByIdForUpdate(@Param("id") Long id);

    /**
     * IDs (not entities, so nothing stale sits in the persistence context
     * before we lock) of subscriptions that can still pay for a scan,
     * soonest-expiring first so paid credits are used before the free plan.
     */
    @Query("""
            select s.id from Subscription s
            where s.user.id = :userId
              and s.status = :status
              and s.credits > 0
              and (s.endDate is null or s.endDate > :now)
            order by s.endDate asc
            """)
    List<Long> findSpendableIds(@Param("userId") Long userId,
                                @Param("status") SubscriptionStatus status,
                                @Param("now") LocalDateTime now);

    @Query("""
            select coalesce(sum(s.credits), 0) from Subscription s
            where s.user.id = :userId
              and s.status = :status
              and (s.endDate is null or s.endDate > :now)
            """)
    long sumSpendableCredits(@Param("userId") Long userId,
                             @Param("status") SubscriptionStatus status,
                             @Param("now") LocalDateTime now);
}
