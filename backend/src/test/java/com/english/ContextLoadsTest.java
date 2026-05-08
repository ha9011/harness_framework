package com.english;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class ContextLoadsTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoadsWithPostgreSqlTestcontainer() {
		String databaseName = jdbcTemplate.queryForObject("select current_database()", String.class);
		String version = jdbcTemplate.queryForObject("select version()", String.class);

		assertThat(databaseName).isEqualTo("test");
		assertThat(version).contains("PostgreSQL");
	}
}
