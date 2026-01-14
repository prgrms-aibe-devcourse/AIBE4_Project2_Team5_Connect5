package kr.eolmago.controller.view.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class SellerViewModelSupport {

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.anon-key:}")
    private String supabaseAnonKey;

    public void applyCommonModel(Model model) {
        model.addAttribute("apiBase", "/api/auctions");
        model.addAttribute("redirectAfterPublish", "/seller/auctions");
        model.addAttribute("redirectAfterDelete", "/seller/auctions");

        model.addAttribute("supabaseUrl", supabaseUrl);
        model.addAttribute("supabaseAnonKey", supabaseAnonKey);
    }
}
