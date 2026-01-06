package kr.eolmago.controller.view.notification;

import java.util.UUID;

import kr.eolmago.dto.api.common.PageResponse;
import kr.eolmago.dto.api.notification.response.NotificationResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.notification.NotificationService;
import kr.eolmago.service.notification.NotificationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationViewController {

	private final NotificationService notificationService;
	private final NotificationValidator notificationValidator;

	@GetMapping
	public String list(
		@AuthenticationPrincipal CustomUserDetails me,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		Model model
	) {
		UUID userId = notificationValidator.validateAndGetUserId(me);

		Page<NotificationResponse> resultPage = notificationService.list(userId, page, size);

		model.addAttribute("notificationsPage", PageResponse.of1Based(resultPage));
		model.addAttribute("pageParam", page);
		model.addAttribute("sizeParam", size);

		return "pages/notification/notification-list";
	}

	@PostMapping("/read-all")
	public String readAll(
		@AuthenticationPrincipal CustomUserDetails me,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		UUID userId = notificationValidator.validateAndGetUserId(me);
		notificationService.readAll(userId);
		return "redirect:/notifications?page=" + page + "&size=" + size;
	}

	@PostMapping("/{notificationId}/read")
	public String readOne(
		@AuthenticationPrincipal CustomUserDetails me,
		@PathVariable Long notificationId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		UUID userId = notificationValidator.validateAndGetUserId(me);
		notificationService.readOne(userId, notificationId);
		return "redirect:/notifications?page=" + page + "&size=" + size;
	}

	@PostMapping("/{notificationId}/delete")
	public String delete(
		@AuthenticationPrincipal CustomUserDetails me,
		@PathVariable Long notificationId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		UUID userId = notificationValidator.validateAndGetUserId(me);
		notificationService.delete(userId, notificationId);
		return "redirect:/notifications?page=" + page + "&size=" + size;
	}
}
