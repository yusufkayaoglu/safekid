package com.safekid.geofence.repository;

import com.safekid.geofence.entity.GeofenceAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GeofenceAlertRepository extends JpaRepository<GeofenceAlertEntity, Long> {

    /** Ebeveynin tüm çocuklarına ait alertleri, en yeniden eskiye sıralar. */
    @Query("""
            SELECT a FROM GeofenceAlertEntity a
            WHERE a.child.parent.ebeveynUniqueId = :parentId
            ORDER BY a.zaman DESC
            """)
    List<GeofenceAlertEntity> findAllByParentId(@Param("parentId") String parentId);

    /** Ebeveynin okunmamış alertleri, en yeniden eskiye. */
    @Query("""
            SELECT a FROM GeofenceAlertEntity a
            WHERE a.child.parent.ebeveynUniqueId = :parentId
              AND a.okundu = false
            ORDER BY a.zaman DESC
            """)
    List<GeofenceAlertEntity> findUnreadByParentId(@Param("parentId") String parentId);

    /** Ebeveynin okunmamış alert sayısı. */
    @Query("""
            SELECT COUNT(a) FROM GeofenceAlertEntity a
            WHERE a.child.parent.ebeveynUniqueId = :parentId
              AND a.okundu = false
            """)
    long countUnreadByParentId(@Param("parentId") String parentId);

    /**
     * Belirli bir çocuk + geofence çifti için en son alert zamanını döner.
     * Cooldown kontrolünde kullanılır.
     */
    @Query("""
            SELECT a FROM GeofenceAlertEntity a
            WHERE a.child.cocukUniqueId = :childId
              AND a.geofence.id = :geofenceId
            ORDER BY a.zaman DESC
            LIMIT 1
            """)
    Optional<GeofenceAlertEntity> findLastAlert(@Param("childId") String childId,
                                                @Param("geofenceId") Long geofenceId);

    /** Ebeveynin tüm okunmamışlarını okundu yap. */
    @Modifying
    @Query("""
            UPDATE GeofenceAlertEntity a
            SET a.okundu = true
            WHERE a.child.parent.ebeveynUniqueId = :parentId
              AND a.okundu = false
            """)
    void markAllReadByParentId(@Param("parentId") String parentId);
}
