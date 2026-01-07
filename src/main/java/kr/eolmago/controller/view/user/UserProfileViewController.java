package kr.eolmago.controller.view.user;

import kr.eolmago.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
@Slf4j
public class UserProfileViewController {

    @GetMapping("/profile")
    public String mypage(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return "pages/user/mypage/profile";
    }

    @GetMapping("/favorites")
    public String favorites() {
        return "pages/user/mypage/favorites";
    }

    @GetMapping("/buyer-reviews")
    public String buyerReviews() {
        return "pages/user/mypage/buyer-reviews";
    }

    @GetMapping("/seller-reviews")
    public String sellerReviews() {
        return "pages/user/mypage/seller-reviews";
    }

    @GetMapping("/reports")
    public String reports() {
        return "pages/user/mypage/reports";
    }
}
