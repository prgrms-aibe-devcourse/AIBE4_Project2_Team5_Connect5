package kr.eolmago.service.notification;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class NotificationSseEmitterStore {

	private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters;

	public NotificationSseEmitterStore() {
		this.emitters = new ConcurrentHashMap<>();
	}

	public void add(UUID userId, SseEmitter emitter) {
		CopyOnWriteArrayList<SseEmitter> list =
			emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
		list.add(emitter);
	}

	public CopyOnWriteArrayList<SseEmitter> get(UUID userId) {
		return emitters.get(userId);
	}

	public void remove(UUID userId, SseEmitter emitter) {
		CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);
		if (list == null) {
			return;
		}

		list.remove(emitter);

		if (list.isEmpty()) {
			emitters.remove(userId);
		}
	}
}
