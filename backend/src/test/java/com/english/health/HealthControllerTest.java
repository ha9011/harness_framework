package com.english.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.english.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@Import(SecurityConfig.class)
@WebMvcTest(HealthController.class)
class HealthControllerTest {

	private final MockMvc mockMvc;

	@Autowired
	HealthControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void healthReturnsOkWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ok"));
	}
}
