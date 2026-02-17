package com.safekid.child.repository;

import com.safekid.child.entity.CocukKonumEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CocukKonumRepository extends JpaRepository<CocukKonumEntity, Long> {

    Optional<CocukKonumEntity> findTopByChild_CocukUniqueIdOrderByRecordedAtDesc(String childId);

    List<CocukKonumEntity>
    findByChild_CocukUniqueIdAndRecordedAtAfterOrderByRecordedAtAsc(
            String cocukUniqueId,
            Instant recordedAt
    );

    @Query(value = """
            SELECT ck.cocuk_unique_id, ck.lat, ck.lng, ck.recorded_at
            FROM cocuk_konum ck
            INNER JOIN (
                SELECT cocuk_unique_id, MAX(recorded_at) AS max_recorded
                FROM cocuk_konum
                WHERE cocuk_unique_id IN (:childIds)
                GROUP BY cocuk_unique_id
            ) latest ON ck.cocuk_unique_id = latest.cocuk_unique_id
                   AND ck.recorded_at = latest.max_recorded
            """, nativeQuery = true)
    List<Object[]> findLatestLocationsByChildIds(@Param("childIds") List<String> childIds);

    List<CocukKonumEntity>
    findByChild_CocukUniqueIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            String cocukUniqueId,
            Instant start,
            Instant end
    );

    List<CocukKonumEntity>
    findTop100ByChild_CocukUniqueIdOrderByRecordedAtDesc(String cocukUniqueId);


}
