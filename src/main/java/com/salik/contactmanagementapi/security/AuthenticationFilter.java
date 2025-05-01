package com.salik.contactmanagementapi.security;

import com.salik.contactmanagementapi.model.User;
import com.salik.contactmanagementapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements WebFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String jwt = resolveToken(exchange.getRequest());

        if (StringUtils.hasText(jwt)) {
            String username = jwtUtil.extractUsername(jwt);

            if (StringUtils.hasText(username)) {
                return userRepository.findByUsername(username)
                        .filter(user -> jwtUtil.validateToken(jwt, createUserDetails(user)))
                        .map(user -> {
                            var authorities = user.getRoles().stream()
                                    .map(role -> new SimpleGrantedAuthority(role))
                                    .collect(Collectors.toList());

                            return new UserAuthentication(user.getId().toHexString(), user.getUsername(), authorities);
                        })
                        .flatMap(authentication ->
                                chain.filter(exchange)
                                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                        )
                        .switchIfEmpty(chain.filter(exchange));
            }
        }
        return chain.filter(exchange);
    }

    private String resolveToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private org.springframework.security.core.userdetails.UserDetails createUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role))
                        .collect(Collectors.toList()))
                .build();
    }
}