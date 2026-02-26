package com.safekid.child.sse;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseEmitterRegistry {

    // parentId -> emitter listesi
    private final ConcurrentHashMap<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String parentId) {
        SseEmitter emitter = new SseEmitter(0L); // 0L = sonsuz, timeout yok

        emitters.computeIfAbsent(parentId, k -> new CopyOnWriteArrayList<>())
                .add(emitter);

        Runnable cleanup = () -> removeEmitter(parentId, emitter);

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        return emitter;
    }

    private void removeEmitter(String parentId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(parentId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(parentId);
            }
        }
    }

    /**
     * Her 25 saniyede bir keepalive gönderir.
     * Bağlantısı kopmuş emitter'lar tespit edilerek listeden çıkarılır.
     */
    @Scheduled(fixedRate = 25_000)
    public void sendHeartbeat() {
        emitters.forEach((parentId, list) -> {
            if (list == null || list.isEmpty()) return;
            list.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event().comment("keepalive"));
                    return false;
                } catch (Throwable t) {
                    // Client bağlantısı kopmuş — sadece listeden çıkar,
                    // complete() ÇAĞIRMA: yeni async dispatch tetikler ve hata üretir.
                    return true;
                }
            });
            if (list.isEmpty()) emitters.remove(parentId);
        });
    }

    /**
     * Generic SSE event gönderir.
     * IOException / broken-pipe durumunda emitter sessizce temizlenir,
     * exception dışarıya sızmaz.
     */
    public void sendEvent(String parentId, String eventName, Object data) {
        List<SseEmitter> list = emitters.get(parentId);
        if (list == null) return;

        try {
            list.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                    return false;
                } catch (Throwable t) {
                    // Client bağlantısı kopmuş — sadece listeden çıkar.
                    // complete() ÇAĞIRMA: Tomcat'in async error dispatch döngüsünü tetikler.
                    return true;
                }
            });
        } catch (Throwable ignored) {
            // removeIf kendisi bir şeyler fırlatırsa da dışarıya sızmasın
        }

        if (list.isEmpty()) emitters.remove(parentId);
    }

    /** Konum güncellemesi için kısayol */
    public void send(String parentId, Object data) {
        sendEvent(parentId, "location-update", data);
    }
}
