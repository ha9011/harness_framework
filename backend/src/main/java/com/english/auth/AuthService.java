package com.english.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final int MIN_PASSWORD_LENGTH = 8;

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtProvider = jwtProvider;
	}

	@Transactional
	public AuthResult signup(String email, String password, String nickname) {
		validatePassword(password);
		if (userRepository.findByEmail(email).isPresent()) {
			throw AuthException.duplicateEmail();
		}

		User user = new User(email, passwordEncoder.encode(password), nickname);
		try {
			User saved = userRepository.saveAndFlush(user);
			return authenticate(saved);
		}
		catch (DataIntegrityViolationException exception) {
			throw AuthException.duplicateEmail();
		}
	}

	@Transactional(readOnly = true)
	public AuthResult login(String email, String password) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(AuthException::authenticationFailed);

		if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
			throw AuthException.authenticationFailed();
		}

		return authenticate(user);
	}

	@Transactional(readOnly = true)
	public CurrentUserResponse currentUser(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(AuthException::invalidToken);

		return CurrentUserResponse.from(user);
	}

	private AuthResult authenticate(User user) {
		JwtToken token = jwtProvider.issueToken(user.getId());
		return AuthResult.from(user, token);
	}

	private static void validatePassword(String password) {
		if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
			throw AuthException.shortPassword();
		}
	}
}
