package com.safekid.geofence.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safekid.child.sse.SseEmitterRegistry;
import com.safekid.geofence.dto.*;
import com.safekid.notification.FcmService;
import com.safekid.geofence.entity.*;
import com.safekid.geofence.repository.*;
import com.safekid.geofence.util.PointInPolygon;
import com.safekid.parent.entity.ChildEntity;
import com.safekid.parent.repository.ChildRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class GeofenceService {

    private final GeofenceRepository geofenceRepo;
    private final GeofenceAlertRepository alertRepo;
    private final ChildRepository childRepo;
    private final SseEmitterRegistry sseRegistry;
    private final ObjectMapper objectMapper;
    private final FcmService fcmService;

    @Value("${safekid.geofence.alert-cooldown-minutes:30}")
    private int alertCooldownMinutes;

    public GeofenceService(GeofenceRepository geofenceRepo,
                           GeofenceAlertRepository alertRepo,
                           ChildRepository childRepo,
                           SseEmitterRegistry sseRegistry,
                           ObjectMapper objectMapper,
                           FcmService fcmService) {
        this.geofenceRepo = geofenceRepo;
        this.alertRepo    = alertRepo;
        this.childRepo    = childRepo;
        this.sseRegistry  = sseRegistry;
        this.objectMapper = objectMapper;
        this.fcmService   = fcmService;
    }

    // ───────────── CRUD (🔥 SİLİNENLERİ GERİ EKLEDİM) ─────────────

    @Transactional
    public GeofenceResponse create(String parentId, GeofenceCreateRequest req) {

        ChildEntity child = validateOwnership(parentId, req.cocukId());

        if (req.koordinatlar() == null || req.koordinatlar().size() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Polygon en az 3 koordinat noktası gerektirir");
        }

        GeofenceEntity entity = new GeofenceEntity();
        entity.setChild(child);
        entity.setAlanAdi(req.alanAdi().trim());
        entity.setGeoJson(buildGeoJson(req.koordinatlar()));
        entity.setAktif(true);

        geofenceRepo.save(entity);
        return toResponse(entity);
    }

    public List<GeofenceResponse> listByChild(String parentId, String childId) {
        validateOwnership(parentId, childId);
        return geofenceRepo.findByChild_CocukUniqueIdAndAktifTrue(childId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(String parentId, Long geofenceId) {
        GeofenceEntity entity = geofenceRepo.findById(geofenceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        validateOwnership(parentId, entity.getChild().getCocukUniqueId());

        entity.setAktif(false);
        geofenceRepo.save(entity);
    }

    @Transactional
    public GeofenceResponse update(String parentId, Long geofenceId, GeofenceCreateRequest req) {

        GeofenceEntity entity = geofenceRepo.findById(geofenceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        validateOwnership(parentId, entity.getChild().getCocukUniqueId());

        entity.setAlanAdi(req.alanAdi().trim());
        entity.setGeoJson(buildGeoJson(req.koordinatlar()));

        geofenceRepo.save(entity);
        return toResponse(entity);
    }

    // ───────────── 🔥 ANY SAFE ZONE LOGIC ─────────────

    @Async
    @Transactional
    public void checkAndAlert(String childId, String parentId, String childName,
                              String fcmToken, double lat, double lng) {

        List<GeofenceEntity> zones =
                geofenceRepo.findByChild_CocukUniqueIdAndAktifTrue(childId);

        if (zones.isEmpty()) return;

        boolean insideAnyZone = false;

        for (GeofenceEntity zone : zones) {
            List<List<Double>> outerRing = parseOuterRing(zone.getGeoJson());

            if (PointInPolygon.isInside(lat, lng, outerRing)) {
                insideAnyZone = true;
                break;
            }
        }

        if (insideAnyZone) return;

        for (GeofenceEntity zone : zones) {

            if (isCooldownExpired(zone)) {

                Instant now = Instant.now();

                zone.setSonBildirimZamani(now);
                geofenceRepo.save(zone);

                sseRegistry.sendEvent(parentId, "geofence-breach",
                        new GeofenceAlertEvent(
                                "GEOFENCE_BREACH",
                                childId, childName,
                                lat, lng,
                                zone.getId(), zone.getAlanAdi(),
                                now
                        )
                );

                fcmService.sendPush(
                        fcmToken,
                        childName + " güvenli alanı terk etti!",
                        "Tüm güvenli alanların dışına çıktı."
                );

                ChildEntity childRef = childRepo.getReferenceById(childId);

                GeofenceAlertEntity record = new GeofenceAlertEntity();
                record.setChild(childRef);
                record.setGeofence(zone);
                record.setAlanAdi(zone.getAlanAdi());
                record.setLat(lat);
                record.setLng(lng);
                record.setZaman(now);

                alertRepo.save(record);

                break;
            }
        }
    }

    // ───────────── HELPERS ─────────────

    private String buildGeoJson(List<List<Double>> coords) {
        try {
            List<List<Double>> ring = new ArrayList<>(coords);

            List<Double> first = ring.get(0);
            List<Double> last  = ring.get(ring.size()-1);

            if (!first.get(0).equals(last.get(0)) ||
                    !first.get(1).equals(last.get(1))) {
                ring.add(List.copyOf(first));
            }

            Map<String,Object> geoJson = Map.of(
                    "type","Polygon",
                    "coordinates", List.of(ring)
            );

            return objectMapper.writeValueAsString(geoJson);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<List<Double>> parseOuterRing(String geoJson){
        try{
            Map<String,Object> map =
                    objectMapper.readValue(geoJson,new TypeReference<>(){});
            List<List<List<Double>>> coordinates =
                    (List<List<List<Double>>>) map.get("coordinates");
            return coordinates.get(0);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    private ChildEntity validateOwnership(String parentId,String childId){
        ChildEntity child = childRepo.findByCocukUniqueId(childId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));

        if(!child.getParent().getEbeveynUniqueId().equals(parentId)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return child;
    }

    private boolean isCooldownExpired(GeofenceEntity zone) {
        Instant last = zone.getSonBildirimZamani();
        if (last == null) return true;
        return Duration.between(last, Instant.now()).toMinutes() >= alertCooldownMinutes;
    }

    private GeofenceResponse toResponse(GeofenceEntity e){
        return new GeofenceResponse(
                e.getId(),
                e.getChild().getCocukUniqueId(),
                e.getAlanAdi(),
                parseOuterRing(e.getGeoJson()),
                e.isAktif(),
                e.getOlusturmaTarihi()
        );
    }

    // ───────────── ALERT LİSTELEME ─────────────

    public List<GeofenceAlertResponse> listAlerts(String parentId, boolean sadeceokunmamis) {

        List<GeofenceAlertEntity> alerts = sadeceokunmamis
                ? alertRepo.findUnreadByParentId(parentId)
                : alertRepo.findAllByParentId(parentId);

        return alerts.stream()
                .map(this::toAlertResponse)
                .toList();
    }

    public long unreadCount(String parentId) {
        return alertRepo.countUnreadByParentId(parentId);
    }

    @Transactional
    public void markRead(String parentId, Long alertId) {

        GeofenceAlertEntity alert = alertRepo.findById(alertId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Bildirim bulunamadı: " + alertId));

        if (!alert.getChild().getParent()
                .getEbeveynUniqueId().equals(parentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Bu bildirim size ait değil");
        }

        alert.setOkundu(true);
        alertRepo.save(alert);
    }

    @Transactional
    public void markAllRead(String parentId) {
        alertRepo.markAllReadByParentId(parentId);
    }


// ───────────── ALERT ENTITY → DTO ─────────────

    private GeofenceAlertResponse toAlertResponse(GeofenceAlertEntity a) {

        ChildEntity c = a.getChild();

        return new GeofenceAlertResponse(
                a.getId(),
                c.getCocukUniqueId(),
                c.getCocukAdi() + " " + c.getCocukSoyadi(),
                a.getGeofence().getId(),
                a.getAlanAdi(),
                a.getLat(),
                a.getLng(),
                a.getZaman(),
                a.isOkundu()
        );
    }
}