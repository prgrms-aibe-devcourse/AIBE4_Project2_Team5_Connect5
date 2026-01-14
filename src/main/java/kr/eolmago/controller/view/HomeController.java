package kr.eolmago.controller.view;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        // userRole, userStatus는 NavModelAdvice가 자동으로 설정하므로 제거
        return "pages/home";
    }
}
