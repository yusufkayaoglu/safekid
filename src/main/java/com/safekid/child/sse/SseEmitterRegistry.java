package com.safekid.child.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseEmitterRegistry {

    private final ConcurrentHashMap<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String parentId) {
        SseEmitter emitter = new SseEmitter(0L);

        emitters.computeIfAbsent(parentId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> {
            List<SseEmitter> list = emitters.get(parentId);
            if (list != null) {
                list.remove(emitter);
                if (list.isEmpty()) emitters.remove(parentId);
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        return emitter;
    }

    public void send(String parentId, Object data) {
        List<SseEmitter> list = emitters.get(parentId);
        if (list == null) return;

        list.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("location-update")
                        .data(data));
                return false;
            } catch (Exception e) {
                return true;
            }
        });

        if (list.isEmpty()) emitters.remove(parentId);
    }

    public void sendEvent(String parentId, String eventName, Object data) {

        List<SseEmitter> list = emitters.get(parentId);
        if (list == null) return;

        list.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                return false;
            } catch (Exception e) {
                return true;
            }
        });

        if (list.isEmpty()) emitters.remove(parentId);
    }
}
