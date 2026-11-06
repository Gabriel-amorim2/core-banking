package com.bankingPlatform.core.security;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.bankingPlatform.core.common.InvalidTokenSubjectException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${api.security.token}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // remove "Bearer "

        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            var verifier = JWT.require(algorithm)
                    .withIssuer("auth-service")
                    .acceptLeeway(1)
                    .build();
            var decodedToken = verifier.verify(token);
            String subject = decodedToken.getSubject();
            if(subject == null || subject.isBlank()) throw new InvalidTokenSubjectException("Token JWT não possui o campo 'sub' (subject) obrigatório.");

            UUID userId = UUID.fromString(subject);

            Claim rolesClaim = decodedToken.getClaim("roles");
            List<SimpleGrantedAuthority> authorities = Collections.emptyList();

            if (!rolesClaim.isNull() && !rolesClaim.isMissing()) {
                List<String> roleStrings = rolesClaim.asList(String.class);
                if (roleStrings != null) {
                    authorities = roleStrings.stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                }
            }
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                authorities
                        );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }


        } catch (JWTVerificationException e) {
            System.out.println("Token inválido: " + e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
