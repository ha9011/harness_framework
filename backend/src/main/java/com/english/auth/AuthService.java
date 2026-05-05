package com.english.auth;

import com.english.config.AuthenticationException;
import com.english.config.DuplicateException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateException("이미 사용 중인 이메일입니다: " + request.getEmail());
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getEmail(), hashedPassword, request.getNickname());
        User saved = userRepository.save(user);

        return AuthResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("이메일 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        String token = jwtProvider.generateToken(user.getEmail());
        return new LoginResult(token, AuthResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("사용자를 찾을 수 없습니다"));
        return AuthResponse.from(user);
    }
}
