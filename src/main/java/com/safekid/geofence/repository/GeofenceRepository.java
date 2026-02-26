package com.safekid.geofence.repository;

import com.safekid.geofence.entity.GeofenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GeofenceRepository extends JpaRepository<GeofenceEntity, Long> {

    /** Belirtilen çocuğun aktif güvenli alanlarını döner. */
    List<GeofenceEntity> findByChild_CocukUniqueIdAndAktifTrue(String childId);
}
