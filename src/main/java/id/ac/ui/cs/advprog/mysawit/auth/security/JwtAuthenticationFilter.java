package id.ac.ui.cs.advprog.mysawit.auth.security;

import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        System.out.println("JWT FILTER RUNNING: " + request.getRequestURI());

        try {

            String token = extractToken(request);
            System.out.println("TOKEN: " + token);

            if (token != null) {

                boolean valid = jwtTokenProvider.validateToken(token);
                System.out.println("TOKEN VALID: " + valid);

                if (valid && SecurityContextHolder.getContext().getAuthentication() == null) {

                    String userId = jwtTokenProvider.getUserIdFromToken(token);
                    String email = jwtTokenProvider.getEmailFromToken(token);
                    String role = jwtTokenProvider.getRoleFromToken(token);

                    System.out.println("USER ID: " + userId);
                    System.out.println("ROLE: " + role);

                    AuthUser user = AuthUser.builder()
                            .id(UUID.fromString(userId))
                            .email(email)
                            .role(Role.valueOf(role))
                            .build();

                    List<SimpleGrantedAuthority> authorities =
                            List.of(new SimpleGrantedAuthority("ROLE_" + role));

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    authorities
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    System.out.println("AUTHENTICATION SET SUCCESSFULLY");
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();   // IMPORTANT
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}