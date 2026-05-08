package com.english.word;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.english.auth.AuthResult;
import com.english.auth.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"auth.jwt.secret=word-controller-test-secret-that-is-at-least-32-bytes",
		"auth.jwt.expiration=PT24H"
})
class WordControllerTest {

	private static final String TOKEN_COOKIE = "token";

	private final MockMvc mockMvc;
	private final ObjectMapper objectMapper;
	private final AuthService authService;
	private final WordService wordService;

	@Autowired
	WordControllerTest(
			MockMvc mockMvc,
			ObjectMapper objectMapper,
			AuthService authService,
			WordService wordService
	) {
		this.mockMvc = mockMvc;
		this.objectMapper = objectMapper;
		this.authService = authService;
		this.wordService = wordService;
	}

	@Test
	void wordApisRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/words"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
		mockMvc.perform(get("/api/words/1"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
		mockMvc.perform(post("/api/words")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
		mockMvc.perform(post("/api/words/bulk")
						.contentType(MediaType.APPLICATION_JSON)
						.content("[]"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
		mockMvc.perform(put("/api/words/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
		mockMvc.perform(patch("/api/words/1/important"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
		mockMvc.perform(delete("/api/words/1"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	void listWordsSupportsPagingFiltersSortAndCurrentUserScope() throws Exception {
		AuthResult owner = signup("word-list-owner");
		AuthResult otherUser = signup("word-list-other");
		WordResponse target = wordService.create(owner.user().id(), new WordCreateRequest(
				"brew coffee",
				"커피를 내리다",
				"phrase",
				"/bru/",
				null,
				"brew는 천천히 우려내는 느낌"));
		wordService.toggleImportant(owner.user().id(), target.id());
		wordService.create(owner.user().id(), new WordCreateRequest("coffee bean", "커피콩", "noun", null, null, null));
		wordService.create(otherUser.user().id(), new WordCreateRequest("brew coffee", "커피를 내리다", "phrase", null, null, null));

		mockMvc.perform(get("/api/words")
						.cookie(tokenCookie(owner))
						.param("page", "0")
						.param("size", "10")
						.param("search", "coffee")
						.param("partOfSpeech", "phrase")
						.param("importantOnly", "true")
						.param("sort", "word"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(10))
				.andExpect(jsonPath("$.content[0].id").value(target.id()))
				.andExpect(jsonPath("$.content[0].word").value("brew coffee"))
				.andExpect(jsonPath("$.content[0].meaning").value("커피를 내리다"))
				.andExpect(jsonPath("$.content[0].partOfSpeech").value("phrase"))
				.andExpect(jsonPath("$.content[0].isImportant").value(true));
	}

	@Test
	void createGetUpdateToggleAndDeleteWord() throws Exception {
		AuthResult user = signup("word-crud");

		MvcResult createResult = mockMvc.perform(post("/api/words")
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "word": "make a bed",
								  "meaning": "침대를 정리하다",
								  "partOfSpeech": "phrase",
								  "pronunciation": "/meik/",
								  "synonyms": "tidy the bed",
								  "tip": "make the bed도 같은 의미"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.word").value("make a bed"))
				.andExpect(jsonPath("$.isImportant").value(false))
				.andReturn();
		Long wordId = idFrom(createResult, "/id");

		mockMvc.perform(get("/api/words/{id}", wordId)
						.cookie(tokenCookie(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(wordId))
				.andExpect(jsonPath("$.word").value("make a bed"))
				.andExpect(jsonPath("$.generatedSentences").isArray());

		mockMvc.perform(put("/api/words/{id}", wordId)
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "word": "make the bed",
								  "meaning": "침대를 정돈하다",
								  "partOfSpeech": "phrase",
								  "pronunciation": "/meik/",
								  "synonyms": "tidy the bed",
								  "tip": "the를 붙여도 자연스럽다"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.word").value("make the bed"))
				.andExpect(jsonPath("$.meaning").value("침대를 정돈하다"));

		mockMvc.perform(patch("/api/words/{id}/important", wordId)
						.cookie(tokenCookie(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isImportant").value(true));

		mockMvc.perform(delete("/api/words/{id}", wordId)
						.cookie(tokenCookie(user)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/words/{id}", wordId)
						.cookie(tokenCookie(user)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("NOT_FOUND"));
	}

	@Test
	void bulkCreateReturnsSavedSkippedAndEnrichmentFailedSections() throws Exception {
		AuthResult user = signup("word-bulk");
		wordService.create(user.user().id(), new WordCreateRequest("coffee", "커피"));

		mockMvc.perform(post("/api/words/bulk")
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								[
								  { "word": "coffee", "meaning": "커피" },
								  { "word": "study", "meaning": "공부하다", "partOfSpeech": "verb" },
								  { "word": " Study ", "meaning": "학습하다" }
								]
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.saved.length()").value(1))
				.andExpect(jsonPath("$.saved[0].word").value("study"))
				.andExpect(jsonPath("$.skipped.length()").value(2))
				.andExpect(jsonPath("$.skipped[0].word").value("coffee"))
				.andExpect(jsonPath("$.skipped[0].reason").value("이미 등록된 단어입니다"))
				.andExpect(jsonPath("$.enrichmentFailed").isArray())
				.andExpect(jsonPath("$.enrichmentFailed.length()").value(0));
	}

	@Test
	void invalidWordRequestsReturnBadRequest() throws Exception {
		AuthResult user = signup("word-validation");

		mockMvc.perform(post("/api/words")
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "word": " ",
								  "meaning": "뜻"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));

		mockMvc.perform(post("/api/words/bulk")
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("[]"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));
	}

	@Test
	void otherUsersWordAccessReturnsForbidden() throws Exception {
		AuthResult owner = signup("word-forbidden-owner");
		AuthResult otherUser = signup("word-forbidden-other");
		WordResponse word = wordService.create(owner.user().id(), new WordCreateRequest("owner word", "소유자 단어"));

		mockMvc.perform(get("/api/words/{id}", word.id())
						.cookie(tokenCookie(otherUser)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));
		mockMvc.perform(put("/api/words/{id}", word.id())
						.cookie(tokenCookie(otherUser))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "word": "changed",
								  "meaning": "변경됨"
								}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));
		mockMvc.perform(patch("/api/words/{id}/important", word.id())
						.cookie(tokenCookie(otherUser)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));
		mockMvc.perform(delete("/api/words/{id}", word.id())
						.cookie(tokenCookie(otherUser)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	private AuthResult signup(String prefix) {
		return authService.signup(prefix + "-" + UUID.randomUUID() + "@example.com", "password123", "tester");
	}

	private static Cookie tokenCookie(AuthResult result) {
		return new Cookie(TOKEN_COOKIE, result.token());
	}

	private Long idFrom(MvcResult result, String pointer) throws Exception {
		JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString()).at(pointer);
		return node.longValue();
	}
}
