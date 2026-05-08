package com.english.config;

import com.english.auth.JwtAuthenticationFilter;
import com.english.auth.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final ObjectProvider<JsonAuthenticationEntryPoint> authenticationEntryPoint;
	private final ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilter;

	public SecurityConfig(
			ObjectProvider<JsonAuthenticationEntryPoint> authenticationEntryPoint,
			ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilter) {
		this.authenticationEntryPoint = authenticationEntryPoint;
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Bean
	@ConditionalOnBean(JwtProvider.class)
	JwtAuthenticationFilter jwtAuthenticationFilter(JwtProvider jwtProvider) {
		return new JwtAuthenticationFilter(jwtProvider);
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.sessionManagement(sessionManagement -> sessionManagement
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptionHandling -> exceptionHandling
						.authenticationEntryPoint(authenticationEntryPoint()))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/api/auth/signup", "/api/auth/login").permitAll()
						.requestMatchers("/api/health").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().permitAll());

		jwtAuthenticationFilter.ifAvailable(filter ->
				http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class));

		return http.build();
	}

	private JsonAuthenticationEntryPoint authenticationEntryPoint() {
		return authenticationEntryPoint.getIfAvailable(() -> new JsonAuthenticationEntryPoint(new ObjectMapper()));
	}
}
