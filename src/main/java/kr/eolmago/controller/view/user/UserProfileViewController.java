package kr.eolmago.controller.view.user;

import kr.eolmago.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/me")
@RequiredArgsConstructor
@Slf4j
public class UserProfileViewController {

    @GetMapping
    public String mypage(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return "pages/user/mypage";  // View만 반환
    }
}
