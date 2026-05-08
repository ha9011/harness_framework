package com.english;

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
class SchemaMigrationTest {

	private final JdbcTemplate jdbcTemplate;

	@Autowired
	SchemaMigrationTest(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Test
	void learningTablesExistWithRequiredColumns() {
		List<String> tableNames = jdbcTemplate.queryForList("""
				select table_name
				from information_schema.tables
				where table_schema = 'public'
				  and table_type = 'BASE TABLE'
				order by table_name
				""", String.class);

		assertThat(tableNames).contains(
				"words",
				"patterns",
				"pattern_examples",
				"generation_history",
				"generated_sentences",
				"generated_sentence_words",
				"sentence_situations",
				"study_records",
				"study_record_items",
				"review_items",
				"review_logs",
				"user_settings");

		assertColumn("words", "user_id", "bigint", null, "NO");
		assertColumn("words", "word", "character varying", 200, "NO");
		assertColumn("words", "meaning", "character varying", 500, "NO");
		assertColumn("words", "deleted", "boolean", null, "NO");
		assertDefaultContains("words", "deleted", "false");

		assertColumn("patterns", "user_id", "bigint", null, "NO");
		assertColumn("patterns", "template", "character varying", 255, "NO");
		assertColumn("patterns", "description", "character varying", 500, "NO");
		assertColumn("pattern_examples", "sort_order", "integer", null, "NO");

		assertColumn("generation_history", "requested_count", "integer", null, "NO");
		assertColumn("generation_history", "actual_count", "integer", null, "NO");
		assertColumn("generated_sentences", "sentence", "text", null, "NO");
		assertColumn("generated_sentences", "deleted", "boolean", null, "NO");

		assertColumn("study_record_items", "item_type", "character varying", 10, "NO");
		assertColumn("study_record_items", "item_id", "bigint", null, "NO");

		assertColumn("review_items", "item_type", "character varying", 20, "NO");
		assertColumn("review_items", "direction", "character varying", 15, "NO");
		assertColumn("review_items", "next_review_date", "date", null, "NO");
		assertColumn("review_items", "last_result", "character varying", 10, "YES");
		assertColumn("review_logs", "reviewed_at", "timestamp with time zone", null, "NO");

		assertColumn("user_settings", "daily_review_count", "integer", null, "NO");
		assertDefaultContains("user_settings", "daily_review_count", "10");
	}

	@Test
	void userOwnedTablesHaveUserForeignKeys() {
		assertUserForeignKey("words");
		assertUserForeignKey("patterns");
		assertUserForeignKey("generation_history");
		assertUserForeignKey("generated_sentences");
		assertUserForeignKey("study_records");
		assertUserForeignKey("review_items");
		assertUserForeignKey("user_settings");
	}

	@Test
	void polymorphicAssociationsDoNotUseDatabaseForeignKeys() {
		assertNoForeignKeyOnColumn("study_record_items", "item_id");
		assertNoForeignKeyOnColumn("review_items", "item_id");
	}

	@Test
	void requiredIndexesAndConstraintsExist() {
		List<String> indexNames = jdbcTemplate.queryForList("""
				select indexname
				from pg_indexes
				where schemaname = 'public'
				order by indexname
				""", String.class);

		assertThat(indexNames).contains(
				"idx_words_deleted",
				"idx_patterns_deleted",
				"idx_generated_sentences_deleted",
				"idx_review_items_deleted",
				"idx_review_items_next_review_date",
				"idx_review_items_item_type_last_result",
				"idx_generated_sentence_words_word_id",
				"idx_review_logs_reviewed_at");

		assertThat(uniqueConstraintsUsingColumn("words", "word")).isZero();
		assertThat(uniqueConstraintsUsingColumn("patterns", "template")).isZero();
		assertThat(uniqueConstraintsUsingColumn("study_record_items", "item_id")).isEqualTo(1);
	}

	private void assertColumn(
			String tableName,
			String columnName,
			String dataType,
			Integer characterMaximumLength,
			String isNullable
	) {
		Map<String, Object> column = jdbcTemplate.queryForMap("""
				select data_type, character_maximum_length, is_nullable
				from information_schema.columns
				where table_schema = 'public'
				  and table_name = ?
				  and column_name = ?
				""", tableName, columnName);

		assertThat(column.get("data_type")).isEqualTo(dataType);
		if (characterMaximumLength != null) {
			assertThat(column.get("character_maximum_length")).isEqualTo(characterMaximumLength);
		}
		assertThat(column.get("is_nullable")).isEqualTo(isNullable);
	}

	private void assertDefaultContains(String tableName, String columnName, String expected) {
		String defaultValue = jdbcTemplate.queryForObject("""
				select column_default
				from information_schema.columns
				where table_schema = 'public'
				  and table_name = ?
				  and column_name = ?
				""", String.class, tableName, columnName);

		assertThat(defaultValue).contains(expected);
	}

	private void assertUserForeignKey(String tableName) {
		Integer count = jdbcTemplate.queryForObject("""
				select count(*)
				from information_schema.table_constraints tc
				join information_schema.key_column_usage kcu
				  on tc.constraint_name = kcu.constraint_name
				 and tc.table_schema = kcu.table_schema
				join information_schema.constraint_column_usage ccu
				  on tc.constraint_name = ccu.constraint_name
				 and tc.table_schema = ccu.table_schema
				where tc.table_schema = 'public'
				  and tc.constraint_type = 'FOREIGN KEY'
				  and tc.table_name = ?
				  and kcu.column_name = 'user_id'
				  and ccu.table_name = 'users'
				  and ccu.column_name = 'id'
				""", Integer.class, tableName);

		assertThat(count).isEqualTo(1);
	}

	private void assertNoForeignKeyOnColumn(String tableName, String columnName) {
		Integer count = jdbcTemplate.queryForObject("""
				select count(*)
				from information_schema.table_constraints tc
				join information_schema.key_column_usage kcu
				  on tc.constraint_name = kcu.constraint_name
				 and tc.table_schema = kcu.table_schema
				where tc.table_schema = 'public'
				  and tc.constraint_type = 'FOREIGN KEY'
				  and tc.table_name = ?
				  and kcu.column_name = ?
				""", Integer.class, tableName, columnName);

		assertThat(count).isZero();
	}

	private int uniqueConstraintsUsingColumn(String tableName, String columnName) {
		Integer count = jdbcTemplate.queryForObject("""
				select count(*)
				from information_schema.table_constraints tc
				join information_schema.key_column_usage kcu
				  on tc.constraint_name = kcu.constraint_name
				 and tc.table_schema = kcu.table_schema
				where tc.table_schema = 'public'
				  and tc.constraint_type = 'UNIQUE'
				  and tc.table_name = ?
				  and kcu.column_name = ?
				""", Integer.class, tableName, columnName);

		return count == null ? 0 : count;
	}
}
