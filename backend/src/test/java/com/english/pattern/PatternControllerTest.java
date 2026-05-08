package com.english.pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.english.auth.AuthResult;
import com.english.auth.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.List;
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
		"auth.jwt.secret=pattern-controller-test-secret-that-is-at-least-32-bytes",
		"auth.jwt.expiration=PT24H"
})
class PatternControllerTest {

	private static final String TOKEN_COOKIE = "token";

	private final MockMvc mockMvc;
	private final ObjectMapper objectMapper;
	private final AuthService authService;
	private final PatternService patternService;

	@Autowired
	PatternControllerTest(
			MockMvc mockMvc,
			ObjectMapper objectMapper,
			AuthService authService,
			PatternService patternService
	) {
		this.mockMvc = mockMvc;
		this.objectMapper = objectMapper;
		this.authService = authService;
		this.patternService = patternService;
	}

	@Test
	void patternApisRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/patterns"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
		mockMvc.perform(get("/api/patterns/1"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
		mockMvc.perform(post("/api/patterns")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
		mockMvc.perform(put("/api/patterns/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
		mockMvc.perform(delete("/api/patterns/1"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	void listPatternsReturnsPagedCurrentUsersActivePatternsWithExamples() throws Exception {
		AuthResult owner = signup("pattern-list-owner");
		AuthResult otherUser = signup("pattern-list-other");
		PatternResponse target = patternService.create(owner.user().id(), new PatternCreateRequest(
				"I used to...",
				"과거 습관",
				List.of(new PatternExampleRequest("I used to drink tea.", "나는 차를 마시곤 했다."))));
		PatternResponse deleted = patternService.create(owner.user().id(), new PatternCreateRequest("Could you...?", "부탁"));
		patternService.create(otherUser.user().id(), new PatternCreateRequest("I used to...", "과거 습관"));
		patternService.delete(owner.user().id(), deleted.id());

		mockMvc.perform(get("/api/patterns")
						.cookie(tokenCookie(owner))
						.param("page", "0")
						.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(10))
				.andExpect(jsonPath("$.content[0].id").value(target.id()))
				.andExpect(jsonPath("$.content[0].template").value("I used to..."))
				.andExpect(jsonPath("$.content[0].examples[0].sortOrder").value(1))
				.andExpect(jsonPath("$.content[0].examples[0].sentence").value("I used to drink tea."));
	}

	@Test
	void createGetUpdateAndDeletePattern() throws Exception {
		AuthResult user = signup("pattern-crud");

		MvcResult createResult = mockMvc.perform(post("/api/patterns")
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "template": "I'm afraid that...",
								  "description": "나쁜 소식을 부드럽게 말할 때 쓴다",
								  "examples": [
								    {
								      "sentence": "I'm afraid that we are late.",
								      "translation": "우리가 늦은 것 같아요."
								    }
								  ]
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.template").value("I'm afraid that..."))
				.andExpect(jsonPath("$.examples[0].sortOrder").value(1))
				.andReturn();
		Long patternId = idFrom(createResult, "/id");

		mockMvc.perform(get("/api/patterns/{id}", patternId)
						.cookie(tokenCookie(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(patternId))
				.andExpect(jsonPath("$.examples[0].translation").value("우리가 늦은 것 같아요."))
				.andExpect(jsonPath("$.generatedSentences").isArray());

		mockMvc.perform(put("/api/patterns/{id}", patternId)
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "template": "I usually...",
								  "description": "평소 습관",
								  "examples": [
								    {
								      "sentence": "I usually brew coffee.",
								      "translation": "나는 보통 커피를 내린다."
								    }
								  ]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.template").value("I usually..."))
				.andExpect(jsonPath("$.examples[0].sentence").value("I usually brew coffee."));

		mockMvc.perform(delete("/api/patterns/{id}", patternId)
						.cookie(tokenCookie(user)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/patterns/{id}", patternId)
						.cookie(tokenCookie(user)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("NOT_FOUND"));
	}

	@Test
	void invalidPatternRequestsReturnBadRequest() throws Exception {
		AuthResult user = signup("pattern-validation");

		mockMvc.perform(post("/api/patterns")
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "template": " ",
								  "description": "설명"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));

		mockMvc.perform(post("/api/patterns")
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "template": "I tend to...",
								  "description": "자주 하는 일을 말한다",
								  "examples": [
								    {
								      "sentence": " ",
								      "translation": "나는 커피를 마시는 편이다."
								    }
								  ]
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));
	}

	@Test
	void otherUsersPatternAccessReturnsForbidden() throws Exception {
		AuthResult owner = signup("pattern-forbidden-owner");
		AuthResult otherUser = signup("pattern-forbidden-other");
		PatternResponse pattern = patternService.create(owner.user().id(), new PatternCreateRequest("owner pattern", "소유자 패턴"));

		mockMvc.perform(get("/api/patterns/{id}", pattern.id())
						.cookie(tokenCookie(otherUser)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));
		mockMvc.perform(put("/api/patterns/{id}", pattern.id())
						.cookie(tokenCookie(otherUser))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "template": "changed",
								  "description": "변경됨"
								}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));
		mockMvc.perform(delete("/api/patterns/{id}", pattern.id())
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
