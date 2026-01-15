package kr.eolmago.controller.view.user;

import kr.eolmago.dto.api.user.response.UserProfileResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.user.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
@Slf4j
public class UserProfileViewController {

    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    public String mypage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            UUID userId = UUID.fromString(userDetails.getId());
            UserProfileResponse profile = userProfileService.getUserProfile(userId);
            model.addAttribute("profile", profile);
        }
        return "pages/user/mypage/profile";
    }

    @GetMapping("/favorites")
    public String favorites() {
        return "pages/user/mypage/favorites";
    }

    @GetMapping("/reports")
    public String reports() {
        return "pages/user/mypage/reports";
    }
}
