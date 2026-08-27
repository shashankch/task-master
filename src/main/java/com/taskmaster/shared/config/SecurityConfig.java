package com.taskmaster.shared.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.taskmaster.shared.constant.SecurityConstants;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {})
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(SecurityConstants.PUBLIC_AUTH_ENDPOINTS).permitAll()
                .requestMatchers(SecurityConstants.PUBLIC_ACTUATOR_ENDPOINTS).permitAll()
                .requestMatchers(SecurityConstants.PUBLIC_SWAGGER_ENDPOINTS).permitAll()
                .requestMatchers(SecurityConstants.PUBLIC_WS_ENDPOINTS).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter defaultAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        Converter<Jwt, Collection<GrantedAuthority>> customAuthoritiesConverter = jwt -> {
            Collection<GrantedAuthority> authorities = defaultAuthoritiesConverter.convert(jwt);
            List<String> roles = jwt.getClaimAsStringList(SecurityConstants.CLAIM_ROLES);
            if (roles != null) {
                List<SimpleGrantedAuthority> roleAuthorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role.toUpperCase()))
                    .collect(Collectors.toList());
                if (authorities != null) {
                    roleAuthorities.addAll(authorities.stream()
                        .map(a -> new SimpleGrantedAuthority(a.getAuthority()))
                        .toList());
                }
                return Collections.unmodifiableCollection(roleAuthorities);
            }
            return authorities != null ? authorities : Collections.emptyList();
        };

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(customAuthoritiesConverter);
        return converter;
    }
}
