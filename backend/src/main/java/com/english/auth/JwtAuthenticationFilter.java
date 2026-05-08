package com.english.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtProvider jwtProvider;

	public JwtAuthenticationFilter(JwtProvider jwtProvider) {
		this.jwtProvider = jwtProvider;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String token = tokenFromCookie(request);
		if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			authenticate(request, token);
		}

		filterChain.doFilter(request, response);
	}

	private void authenticate(HttpServletRequest request, String token) {
		try {
			JwtTokenClaims claims = jwtProvider.parse(token);
			AuthenticatedUser principal = new AuthenticatedUser(claims.userId());
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					principal,
					null,
					List.of(new SimpleGrantedAuthority("ROLE_USER")));
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		catch (AuthException exception) {
			SecurityContextHolder.clearContext();
		}
	}

	private static String tokenFromCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}

		return Arrays.stream(cookies)
				.filter(cookie -> AuthCookie.NAME.equals(cookie.getName()))
				.map(Cookie::getValue)
				.filter(value -> value != null && !value.isBlank())
				.findFirst()
				.orElse(null);
	}
}
