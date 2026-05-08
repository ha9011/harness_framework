package com.english.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/signup")
	public ResponseEntity<CurrentUserResponse> signup(@Valid @RequestBody SignupRequest request) {
		AuthResult result = authService.signup(request.email(), request.password(), request.nickname());
		return ResponseEntity.status(HttpStatus.CREATED)
				.header(HttpHeaders.SET_COOKIE, AuthCookie.issue(result.token()).toString())
				.body(result.user());
	}

	@PostMapping("/login")
	public ResponseEntity<CurrentUserResponse> login(@Valid @RequestBody LoginRequest request) {
		AuthResult result = authService.login(request.email(), request.password());
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, AuthCookie.issue(result.token()).toString())
				.body(result.user());
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, AuthCookie.delete().toString())
				.build();
	}

	@GetMapping("/me")
	public CurrentUserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
		return authService.currentUser(principal.userId());
	}
}
