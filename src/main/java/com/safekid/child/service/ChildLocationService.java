package com.safekid.child.service;

import com.safekid.child.dto.LocationCreateRequest;
import com.safekid.child.dto.LocationResponse;
import com.safekid.child.dto.MapChildLocation;
import com.safekid.child.entity.CocukKonumEntity;
import com.safekid.child.repository.CocukKonumRepository;
import com.safekid.child.sse.SseEmitterRegistry;
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

    public ChildLocationService(CocukKonumRepository konumRepo,
                                ChildRepository childRepo,
                                SseEmitterRegistry sseRegistry) {
        this.konumRepo = konumRepo;
        this.childRepo = childRepo;
        this.sseRegistry = sseRegistry;
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

        // Parent'a realtime push (SSE)
        String parentId = child.getParent().getEbeveynUniqueId();
        sseRegistry.send(parentId, new MapChildLocation(
                childId,
                child.getCocukAdi() + " " + child.getCocukSoyadi(),
                konum.getLat(),
                konum.getLng(),
                konum.getRecordedAt(),
                true
        ));

        return new LocationResponse(
                childId,
                konum.getLat(),
                konum.getLng(),
                konum.getRecordedAt()
        );
    }

    // ✅ PARENT -> LAST LOCATION (ULTRA PRO SECURITY)
    public LocationResponse getLastLocationForParent(String parentId, String childId) {

        ChildEntity child = childRepo.findByCocukUniqueId(childId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));

        // 🔥 ULTRA PRO SECURITY CHECK
        // ChildEntity içinde ebeveynUniqueId field olması lazım
        if (!child.getParent().getEbeveynUniqueId().equals(parentId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bu çocuk sana ait değil"
            );
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

    public List<MapChildLocation> getMapForParent(String parentId) {
        List<ChildEntity> children = childRepo.findAllByParent_EbeveynUniqueId(parentId);

        if (children.isEmpty()) return List.of();

        List<String> childIds = children.stream()
                .map(ChildEntity::getCocukUniqueId)
                .toList();

        Map<String, Object[]> locationMap = konumRepo.findLatestLocationsByChildIds(childIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        Function.identity()
                ));

        Instant fiveMinutesAgo = Instant.now().minusSeconds(300);

        return children.stream()
                .map(child -> {
                    Object[] row = locationMap.get(child.getCocukUniqueId());
                    if (row == null) {
                        return new MapChildLocation(
                                child.getCocukUniqueId(),
                                child.getCocukAdi() + " " + child.getCocukSoyadi(),
                                null, null, null, false
                        );
                    }

                    Instant recordedAt = (Instant) row[3];
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
