package kr.eolmago.controller.api.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;

import kr.eolmago.dto.api.notification.response.NotificationResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.repository.notification.NotificationRepository;
import kr.eolmago.service.notification.NotificationCommandService;
import kr.eolmago.service.notification.NotificationSseRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Notification", description = "알림 조회/읽음/삭제 및 SSE 실시간 푸시 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationApiController {

	private final NotificationRepository notificationRepository;
	private final NotificationCommandService notificationCommandService;
	private final NotificationSseRegistry sseRegistry;

	@Operation(
		summary = "내 알림 목록 조회(페이징)",
		description = """
                로그인한 사용자의 알림을 최신순으로 조회함.
                삭제(soft delete)된 알림은 제외함.
                """
	)
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping
	public Page<NotificationResponse> list(
		@AuthenticationPrincipal CustomUserDetails me,
		@Parameter(description = "페이지(0부터 시작)", example = "0")
		@RequestParam(defaultValue = "0") int page,
		@Parameter(description = "페이지 크기", example = "20")
		@RequestParam(defaultValue = "20") int size
	) {
		UUID userId = currentUserId(me);

		Pageable pageable = PageRequest.of(page, size);
		Page<NotificationResponse> result = notificationRepository
			.findByUser_UserIdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable)
			.map(NotificationResponse::from);

		return result;
	}

	@Operation(
		summary = "안 읽은 알림 개수 조회",
		description = "로그인한 사용자의 읽지 않은 알림 개수를 조회함. (삭제된 알림 제외)"
	)
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/unread-count")
	public long unreadCount(@AuthenticationPrincipal CustomUserDetails me) {
		UUID userId = currentUserId(me);
		return notificationRepository.countByUser_UserIdAndReadFalseAndDeletedFalse(userId);
	}

	@Operation(
		summary = "알림 개별 읽음 처리",
		description = "notificationId에 해당하는 알림을 읽음 처리함. (본인 알림만 가능)"
	)
	@SecurityRequirement(name = "bearerAuth")
	@PatchMapping("/{notificationId}/read")
	public void readOne(
		@AuthenticationPrincipal CustomUserDetails me,
		@Parameter(description = "알림 ID", example = "1", required = true)
		@PathVariable Long notificationId
	) {
		UUID userId = currentUserId(me);
		notificationCommandService.readOne(userId, notificationId);
	}

	@Operation(
		summary = "알림 전체 읽음 처리",
		description = "로그인한 사용자의 읽지 않은 알림을 전체 읽음 처리함. (삭제된 알림 제외)"
	)
	@SecurityRequirement(name = "bearerAuth")
	@PatchMapping("/read-all")
	public int readAll(@AuthenticationPrincipal CustomUserDetails me) {
		UUID userId = currentUserId(me);
		return notificationCommandService.readAll(userId);
	}

	@Operation(
		summary = "알림 삭제(soft delete)",
		description = "notificationId에 해당하는 알림을 soft delete 처리함. (본인 알림만 가능)"
	)
	@SecurityRequirement(name = "bearerAuth")
	@DeleteMapping("/{notificationId}")
	public void delete(
		@AuthenticationPrincipal CustomUserDetails me,
		@Parameter(description = "알림 ID", example = "1", required = true)
		@PathVariable Long notificationId
	) {
		UUID userId = currentUserId(me);
		notificationCommandService.delete(userId, notificationId);
	}

	@Operation(
		summary = "알림 SSE 스트림 연결",
		description = """
                로그인한 사용자에게 SSE 연결을 열어 실시간 알림을 푸시함.
                이벤트 이름:
                - INIT: 최초 연결 확인
                - NOTIFICATION: 알림 payload 전송
                """
	)
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(@AuthenticationPrincipal CustomUserDetails me) {
		UUID userId = currentUserId(me);
		return sseRegistry.connect(userId);
	}

	private UUID currentUserId(CustomUserDetails me) {
		if (me == null || me.getId() == null || me.getId().isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
		}

		try {
			return UUID.fromString(me.getId());
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 정보가 올바르지 않습니다.");
		}
	}
}
