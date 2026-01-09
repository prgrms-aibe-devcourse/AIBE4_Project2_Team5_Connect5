package kr.eolmago.global.security;

import kr.eolmago.domain.entity.user.SocialLogin;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.UserProfile;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public class CustomUserDetails implements UserDetails, OAuth2User {
    private final String id;
    private final String email;
    private final String password;
    private final String profileImageUrl;
    private final Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;

    private CustomUserDetails(String id, String email, String password, String profileImageUrl, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.profileImageUrl = profileImageUrl;
        this.authorities = authorities;
    }

    public static CustomUserDetails from(User user, SocialLogin socialLogin, UserProfile userProfile) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
        return new CustomUserDetails(
                user.getUserId().toString(),
                socialLogin.getEmail(),
                null,
                userProfile.getProfileImageUrl(),
                authorities
        );
    }

    public static CustomUserDetails from(User user, SocialLogin socialLogin, UserProfile userProfile, Map<String, Object> attributes) {
        CustomUserDetails userDetails = from(user, socialLogin, userProfile);
        userDetails.attributes = attributes;
        return userDetails;
    }

    public UUID getUserId() {
        return UUID.fromString(this.id);
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public String getName() {
        return this.id;
    }
}
