package com.english.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("User 저장 후 findByEmail로 조회 성공")
    void findByEmail_success() {
        // given
        User user = new User("test@example.com", "password123", "테스트유저");
        userRepository.save(user);

        // when
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getNickname()).isEqualTo("테스트유저");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("existsByEmail - 존재하는 이메일 → true")
    void existsByEmail_exists() {
        // given
        User user = new User("exists@example.com", "password123", "유저");
        userRepository.save(user);

        // when & then
        assertThat(userRepository.existsByEmail("exists@example.com")).isTrue();
    }

    @Test
    @DisplayName("existsByEmail - 존재하지 않는 이메일 → false")
    void existsByEmail_notExists() {
        // when & then
        assertThat(userRepository.existsByEmail("noone@example.com")).isFalse();
    }

    @Test
    @DisplayName("중복 이메일 저장 시 예외 발생")
    void duplicateEmail_throwsException() {
        // given
        User user1 = new User("dup@example.com", "password1", "유저1");
        userRepository.saveAndFlush(user1);

        // when & then
        User user2 = new User("dup@example.com", "password2", "유저2");
        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
