package com.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = authHeader.substring(7);
            String username = jwtService.extractUsername(jwt);

            if (username == null || username.isBlank()) {
                sendError(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Token không hợp lệ."
                );
                return;
            }

            // Đọc lại tài khoản từ database cho mỗi request có token.
            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(username);

            if (!jwtService.isTokenValid(jwt, userDetails)) {
                sendError(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Token không hợp lệ hoặc đã hết hạn."
                );
                return;
            }

            // Chặn cả token được cấp trước khi tài khoản bị vô hiệu hóa.
            if (!userDetails.isEnabled()) {
                throw new DisabledException("Account disabled");
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authToken.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authToken);
            SecurityContextHolder.setContext(context);

        } catch (DisabledException exception) {
            sendError(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "Tài khoản đã bị vô hiệu hóa."
            );
            return;

        } catch (JwtException | IllegalArgumentException
                 | AuthenticationException exception) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Token không hợp lệ hoặc phiên đăng nhập không còn được chấp nhận."
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {
        SecurityContextHolder.clearContext();

        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"message\":\"" + message + "\"}"
        );
    }
}