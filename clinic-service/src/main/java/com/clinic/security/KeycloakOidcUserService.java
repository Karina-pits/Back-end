package com.clinic.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class KeycloakOidcUserService extends OidcUserService {

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        System.out.println("=== ID TOKEN CLAIMS ===");
        System.out.println(oidcUser.getIdToken().getClaims());
        if (oidcUser.getUserInfo() != null) {
            System.out.println("=== USERINFO CLAIMS ===");
            System.out.println(oidcUser.getUserInfo().getClaims());
        }

        Set<GrantedAuthority> authorities = new HashSet<>(oidcUser.getAuthorities());

        Set<String> roles = new HashSet<>();
        extractRoles(oidcUser.getIdToken().getClaims(), roles);
        if (oidcUser.getUserInfo() != null) {
            extractRoles(oidcUser.getUserInfo().getClaims(), roles);
        }

        System.out.println("=== EXTRACTED ROLES: " + roles + " ===");

        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        }

        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), "preferred_username");
    }

    private void extractRoles(Map<String, Object> claims, Set<String> roles) {
        Object realmAccessObj = claims.get("realm_access");
        if (realmAccessObj instanceof Map<?, ?> realmAccess) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof Collection<?> roleList) {
                for (Object role : roleList) {
                    roles.add(role.toString());
                }
            }
        }
    }
}