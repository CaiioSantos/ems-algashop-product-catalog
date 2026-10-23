package com.algaworks.algashop.product.catalog.infrastructure.security;

import com.algaworks.algashop.product.catalog.application.security.SecurityCheckApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;
@Slf4j
@Service(value = "securityCheck")
public class OAuth2SecurityCheckApplicationServiceImpl  implements SecurityCheckApplicationService {

@Override
public UUID getAuthenticatedUserId() {
    if (isMachineAuthenticated()) {
        throw new AccessDeniedException("Machine-to-machine authentication does not have a user ID");
    }
    Jwt jwt = this.getJwt();
    try {
        return UUID.fromString(jwt.getSubject());
    } catch (IllegalAccessError e) {
        log.error("Invalid user ID format: {}", jwt.getSubject(), e);
        throw new AccessDeniedException("Invalid user ID format");
    }
}

@Override
public boolean isAuthenticated() {
    try {
        return this.getAuthentication().isAuthenticated();
    } catch (IllegalStateException e) {
        log.debug("No authentication found in the security context", e);
        return false;
    }
}

private Authentication getAuthentication() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
        throw new IllegalStateException("No authentication found in the security context");
    }
    return authentication;
}

@Override
public boolean isMachineAuthenticated() {
    Jwt jwt;
    try {
        jwt = this.getJwt();
    } catch (IllegalStateException e) {
        log.debug("No JWT found in the authentication principal", e);
        return false;
    }
    return jwt.getAudience().contains(jwt.getSubject());
}

private Jwt getJwt() {
    Authentication authentication = this.getAuthentication();
    if (authentication.getPrincipal() instanceof Jwt jwt) {
        jwt.getAudience().contains(jwt.getSubject());
        return jwt;
    }
    throw new IllegalStateException("Authentication principal is not a Jwt");
}

@Override
public boolean canAccessOwnProfile() {
    return this.isAuthenticated() && !this.isMachineAuthenticated();
}
}
