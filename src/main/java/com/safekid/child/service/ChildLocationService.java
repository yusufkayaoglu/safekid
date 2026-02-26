package com.safekid.child.service;

import com.safekid.child.dto.LocationCreateRequest;
import com.safekid.child.dto.LocationResponse;
import com.safekid.child.dto.MapChildLocation;
import com.safekid.child.entity.CocukKonumEntity;
import com.safekid.child.repository.CocukKonumRepository;
import com.safekid.child.sse.SseEmitterRegistry;
import com.safekid.geofence.service.GeofenceService;
import com.safekid.parent.entity.ChildEntity;
import com.safekid.parent.repository.ChildRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChildLocationService {

    private final CocukKonumRepository konumRepo;
    private final ChildRepository childRepo;
    private final SseEmitterRegistry sseRegistry;
    private final GeofenceService geofenceService;

    public ChildLocationService(CocukKonumRepository konumRepo,
                                ChildRepository childRepo,
                                SseEmitterRegistry sseRegistry,
                                GeofenceService geofenceService) {
        this.konumRepo       = konumRepo;
        this.childRepo       = childRepo;
        this.sseRegistry     = sseRegistry;
        this.geofenceService = geofenceService;
    }

    @Transactional
    public LocationResponse saveLocation(String childId, LocationCreateRequest req) {

        ChildEntity child = childRepo.findByCocukUniqueId(childId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));

        CocukKonumEntity konum = new CocukKonumEntity();
        konum.setChild(child);

        konum.setLat(req.lat());
        konum.setLng(req.lng());
        konum.setRecordedAt(req.recordedAt() != null ? req.recordedAt() : Instant.now());

        konumRepo.save(konum);

        String parentId = child.getParent().getEbeveynUniqueId();

        // 🔥 realtime map update
        sseRegistry.send(parentId, new MapChildLocation(
                childId,
                child.getCocukAdi() + " " + child.getCocukSoyadi(),
                konum.getLat(),
                konum.getLng(),
                konum.getRecordedAt(),
                true
        ));

        geofenceService.checkAndAlert(
                childId, parentId,
                child.getCocukAdi() + " " + child.getCocukSoyadi(),
                child.getParent().getFcmToken(),
                konum.getLat(), konum.getLng());

        return new LocationResponse(
                childId,
                konum.getLat(),
                konum.getLng(),
                konum.getRecordedAt()
        );
    }

    // ✅ LAST LOCATION
    public LocationResponse getLastLocationForParent(String parentId, String childId) {

        ChildEntity child = childRepo.findByCocukUniqueId(childId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));

        if (!child.getParent().getEbeveynUniqueId().equals(parentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Bu çocuk sana ait değil");
        }

        CocukKonumEntity last =
                konumRepo.findTopByChild_CocukUniqueIdOrderByRecordedAtDesc(childId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Konum bulunamadı"));

        return new LocationResponse(
                childId,
                last.getLat(),
                last.getLng(),
                last.getRecordedAt()
        );
    }

    // ⭐⭐⭐ SAFE KID MAP (ULTRA STABLE VERSION)
    public List<MapChildLocation> getMapForParent(String parentId) {

        List<ChildEntity> children =
                childRepo.findAllByParent_EbeveynUniqueId(parentId);

        if (children.isEmpty()) return List.of();

        List<String> childIds = children.stream()
                .map(ChildEntity::getCocukUniqueId)
                .toList();

        // 🔥 DUPLICATE KEY FIX (EN ÖNEMLİ SATIR)
        Map<String, Object[]> locationMap =
                konumRepo.findLatestLocationsByChildIds(childIds)
                        .stream()
                        .collect(Collectors.toMap(
                                row -> (String) row[0],
                                Function.identity(),
                                (existing, replacement) -> replacement // ⭐ SON KAYIT KAZANSIN
                        ));

        Instant fiveMinutesAgo = Instant.now().minusSeconds(300);

        return children.stream()
                .map(child -> {

                    Object[] row = locationMap.get(child.getCocukUniqueId());

                    if (row == null) {
                        return new MapChildLocation(
                                child.getCocukUniqueId(),
                                child.getCocukAdi() + " " + child.getCocukSoyadi(),
                                null,null,null,false
                        );
                    }

                    Object rawTs = row[3];

                    Instant recordedAt =
                            rawTs instanceof Instant i ? i :
                                    rawTs instanceof java.sql.Timestamp ts ? ts.toInstant() :
                                            rawTs instanceof java.time.OffsetDateTime odt ? odt.toInstant() :
                                                    Instant.now();

                    return new MapChildLocation(
                            child.getCocukUniqueId(),
                            child.getCocukAdi() + " " + child.getCocukSoyadi(),
                            (Double) row[1],
                            (Double) row[2],
                            recordedAt,
                            recordedAt.isAfter(fiveMinutesAgo)
                    );
                })
                .toList();
    }
}