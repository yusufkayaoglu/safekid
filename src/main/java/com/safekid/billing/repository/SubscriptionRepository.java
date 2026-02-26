package com.safekid.billing.repository;

import com.safekid.billing.entity.SubscriptionEntity;
import com.safekid.billing.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Abonelik veritabanı erişim katmanı.
 */
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

    /**
     * Belirtilen token'ın daha önce kaydedilip kaydedilmediğini kontrol eder.
     * Replay saldırı korumasında kullanılır.
     *
     * @param purchaseToken Google Play'den gelen token
     * @return token daha önce kullanılmışsa {@code true}
     */
    boolean existsByPurchaseToken(String purchaseToken);

    /**
     * Ebeveyni için en son aktif aboneliği döner.
     *
     * @param parentId ebeveynin unique ID'si
     * @param status   sorgulanacak durum (genellikle {@link SubscriptionStatus#ACTIVE})
     * @return aktif abonelik varsa dolu {@link Optional}
     */
    @Query("""
            SELECT s FROM SubscriptionEntity s
            WHERE s.parent.ebeveynUniqueId = :parentId
              AND s.status = :status
            ORDER BY s.verifiedAt DESC
            LIMIT 1
            """)
    Optional<SubscriptionEntity> findLatestByParentAndStatus(
            @Param("parentId") String parentId,
            @Param("status") SubscriptionStatus status);

    /**
     * Ebeveyne ait en son abonelik kaydını döner (durum fark etmeksizin).
     *
     * @param parentId ebeveynin unique ID'si
     * @return son abonelik kaydı
     */
    @Query("""
            SELECT s FROM SubscriptionEntity s
            WHERE s.parent.ebeveynUniqueId = :parentId
            ORDER BY s.verifiedAt DESC
            LIMIT 1
            """)
    Optional<SubscriptionEntity> findLatestByParent(@Param("parentId") String parentId);
}
