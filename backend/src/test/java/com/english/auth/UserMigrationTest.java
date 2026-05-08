package com.english.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class UserMigrationTest {

	private final JdbcTemplate jdbcTemplate;

	@Autowired
	UserMigrationTest(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Test
	void usersTableHasRequiredColumnsAndConstraints() {
		List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
				select column_name, data_type, character_maximum_length, is_nullable
				from information_schema.columns
				where table_schema = 'public'
				  and table_name = 'users'
				order by ordinal_position
				""");

		assertThat(columns).extracting(row -> row.get("column_name"))
				.containsExactly("id", "email", "password", "nickname", "created_at");
		assertThat(columns).filteredOn(row -> row.get("column_name").equals("email"))
				.singleElement()
				.satisfies(row -> {
					assertThat(row.get("data_type")).isEqualTo("character varying");
					assertThat(row.get("character_maximum_length")).isEqualTo(320);
					assertThat(row.get("is_nullable")).isEqualTo("NO");
				});
		assertThat(columns).filteredOn(row -> row.get("column_name").equals("password"))
				.singleElement()
				.satisfies(row -> {
					assertThat(row.get("data_type")).isEqualTo("character varying");
					assertThat((Integer) row.get("character_maximum_length")).isGreaterThanOrEqualTo(255);
					assertThat(row.get("is_nullable")).isEqualTo("NO");
				});
		assertThat(columns).filteredOn(row -> row.get("column_name").equals("created_at"))
				.singleElement()
				.satisfies(row -> {
					assertThat(row.get("data_type")).isEqualTo("timestamp with time zone");
					assertThat(row.get("is_nullable")).isEqualTo("NO");
				});

		Integer uniqueEmailConstraints = jdbcTemplate.queryForObject("""
				select count(*)
				from information_schema.table_constraints tc
				join information_schema.constraint_column_usage ccu
				  on tc.constraint_name = ccu.constraint_name
				 and tc.table_schema = ccu.table_schema
				where tc.table_schema = 'public'
				  and tc.table_name = 'users'
				  and tc.constraint_type = 'UNIQUE'
				  and ccu.column_name = 'email'
				""", Integer.class);

		assertThat(uniqueEmailConstraints).isEqualTo(1);
	}
}
