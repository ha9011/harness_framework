package com.english.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.exceptionHandling(exceptionHandling -> exceptionHandling
						.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/api/health").permitAll()
						.anyRequest().authenticated())
				.build();
	}
}
