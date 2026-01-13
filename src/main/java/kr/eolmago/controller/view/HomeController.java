package kr.eolmago.controller.view;

import kr.eolmago.domain.entity.user.enums.UserRole;
import kr.eolmago.global.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .map(auth -> auth.replace("ROLE_", ""))
                    .orElse("GUEST");
            model.addAttribute("userRole", UserRole.valueOf(role));
        } else {
            model.addAttribute("userRole", UserRole.GUEST);
        }
        return "pages/home";
    }
}
