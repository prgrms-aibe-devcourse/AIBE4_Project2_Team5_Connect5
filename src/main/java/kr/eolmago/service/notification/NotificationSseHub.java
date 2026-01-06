package kr.eolmago.service.notification;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import kr.eolmago.dto.api.notification.response.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class NotificationSseHub {

	private static final long DEFAULT_TIMEOUT_MILLIS = 60L * 60 * 1000; // 1 hour
	private static final long DEFAULT_RECONNECT_TIME_MILLIS = 3000L;

	private static final String EVENT_INIT = "INIT";
	private static final String EVENT_NOTIFICATION = "NOTIFICATION";
	private static final String INIT_PAYLOAD = "connected";

	private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

	public SseEmitter connect(UUID userId) {
		SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MILLIS);
		add(userId, emitter);

		emitter.onCompletion(() -> remove(userId, emitter));
		emitter.onTimeout(() -> remove(userId, emitter));
		emitter.onError(e -> remove(userId, emitter));

		sendInit(userId, emitter);
		return emitter;
	}

	public void push(UUID userId, NotificationResponse data) {
		CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);
		if (list == null || list.isEmpty()) return;

		for (SseEmitter emitter : list) {
			if (!sendSafely(userId, emitter,
				SseEmitter.event()
					.name(EVENT_NOTIFICATION)
					.data(data, MediaType.APPLICATION_JSON))
			) {
				remove(userId, emitter);
			}
		}
	}

	private void sendInit(UUID userId, SseEmitter emitter) {
		boolean ok = sendSafely(userId, emitter,
			SseEmitter.event()
				.name(EVENT_INIT)
				.data(INIT_PAYLOAD)
				.reconnectTime(DEFAULT_RECONNECT_TIME_MILLIS));

		if (!ok) {
			remove(userId, emitter);
		}
	}

	private boolean sendSafely(UUID userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
		try {
			emitter.send(event);
			return true;
		} catch (IOException | IllegalStateException e) {
			log.debug("SSE send failed. userId={}, reason={}", userId, e.toString());
			return false;
		}
	}

	private void add(UUID userId, SseEmitter emitter) {
		emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
	}

	private void remove(UUID userId, SseEmitter emitter) {
		CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);
		if (list == null) return;

		list.remove(emitter);
		if (list.isEmpty()) {
			emitters.remove(userId);
		}
	}
}
