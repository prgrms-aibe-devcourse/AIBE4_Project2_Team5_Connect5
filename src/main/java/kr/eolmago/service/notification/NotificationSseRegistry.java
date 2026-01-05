package kr.eolmago.service.notification;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import kr.eolmago.dto.api.notification.response.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class NotificationSseRegistry {

	private static final long DEFAULT_TIMEOUT_MILLIS = 60L * 60 * 1000; // 1 hour
	private static final long DEFAULT_RECONNECT_TIME_MILLIS = 3000L;

	private final NotificationSseEmitterStore emitterStore;

	public NotificationSseRegistry(NotificationSseEmitterStore emitterStore) {
		this.emitterStore = emitterStore;
	}

	public SseEmitter connect(UUID userId) {
		SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MILLIS);
		emitterStore.add(userId, emitter);

		emitter.onCompletion(() -> emitterStore.remove(userId, emitter));
		emitter.onTimeout(() -> emitterStore.remove(userId, emitter));
		emitter.onError(e -> emitterStore.remove(userId, emitter));

		sendInit(userId, emitter);
		return emitter;
	}

	public void push(UUID userId, NotificationResponse data) {
		CopyOnWriteArrayList<SseEmitter> list = emitterStore.get(userId);
		if (list == null || list.isEmpty()) {
			return;
		}

		for (SseEmitter emitter : list) {
			try {
				emitter.send(SseEmitter.event()
					.name("NOTIFICATION")
					.data(data, MediaType.APPLICATION_JSON));
			} catch (IOException e) {
				emitterStore.remove(userId, emitter);
			}
		}
	}

	private void sendInit(UUID userId, SseEmitter emitter) {
		try {
			emitter.send(SseEmitter.event()
				.name("INIT")
				.data("connected")
				.reconnectTime(DEFAULT_RECONNECT_TIME_MILLIS));
		} catch (IOException e) {
			emitterStore.remove(userId, emitter);
		}
	}
}
