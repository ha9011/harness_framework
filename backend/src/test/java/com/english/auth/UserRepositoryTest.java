package com.english.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

	private final EntityManager entityManager;
	private final UserRepository userRepository;

	@Autowired
	UserRepositoryTest(EntityManager entityManager, UserRepository userRepository) {
		this.entityManager = entityManager;
		this.userRepository = userRepository;
	}

	@Test
	void findByEmailReturnsMatchingUser() {
		User saved = userRepository.save(new User(
				"user@example.com",
				"$2a$10$123456789012345678901u123456789012345678901234567890123456",
				"혜진"));

		entityManager.flush();
		entityManager.clear();

		assertThat(userRepository.findByEmail("user@example.com"))
				.hasValueSatisfying(found -> {
					assertThat(found.getId()).isEqualTo(saved.getId());
					assertThat(found.getEmail()).isEqualTo("user@example.com");
					assertThat(found.getPassword()).startsWith("$2a$10$");
					assertThat(found.getNickname()).isEqualTo("혜진");
					assertThat(found.getCreatedAt()).isNotNull();
				});
	}

	@Test
	void duplicateEmailFails() {
		userRepository.saveAndFlush(new User(
				"duplicate@example.com",
				"$2a$10$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu",
				"사용자1"));

		assertThatThrownBy(() -> userRepository.saveAndFlush(new User(
				"duplicate@example.com",
				"$2a$10$abcdefghijklmnopqrstvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv",
				"사용자2")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
