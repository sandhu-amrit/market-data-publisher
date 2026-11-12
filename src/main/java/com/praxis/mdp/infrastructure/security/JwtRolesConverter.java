package com.praxis.mdp.infrastructure.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Maps {@code roles}, Keycloak {@code realm_access.roles}, and {@code scope} onto {@code ROLE_*}
 * authorities. Expected role names are {@code MDP_OPERATOR} and {@code MDP_VIEWER}.
 */
public class JwtRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Set<String> roles = new LinkedHashSet<>();
    addClaim(roles, jwt.getClaim("roles"));
    Object realm = jwt.getClaim("realm_access");
    if (realm instanceof Map<?, ?> map) {
      addClaim(roles, map.get("roles"));
    }
    String scope = jwt.getClaimAsString("scope");
    if (scope != null) {
      for (String part : scope.split(" ")) {
        roles.add(part);
      }
    }
    List<GrantedAuthority> authorities = new ArrayList<>();
    for (String role : roles) {
      if (role == null || role.isBlank()) {
        continue;
      }
      String normalized = role.startsWith("ROLE_") ? role : "ROLE_" + role;
      authorities.add(new SimpleGrantedAuthority(normalized));
    }
    return authorities;
  }

  private static void addClaim(Set<String> roles, Object claim) {
    if (claim instanceof Collection<?> values) {
      for (Object value : values) {
        if (value != null) {
          roles.add(value.toString());
        }
      }
    }
  }
}
