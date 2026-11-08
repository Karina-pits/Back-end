package com.clinic.web;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        if (principal instanceof org.springframework.security.authentication.AbstractAuthenticationToken token
                && token.getPrincipal() instanceof OidcUser oidcUser) {
            model.addAttribute("username", oidcUser.getPreferredUsername());
            model.addAttribute("authorities", oidcUser.getAuthorities());
            model.addAttribute("authenticated", true);
        } else {
            model.addAttribute("authenticated", false);
        }
        return "index";
    }
}
