package com.english.generate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

final class FakeGeminiServer implements AutoCloseable {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Queue<FakeResponse> responses = new ConcurrentLinkedQueue<>();
	private final List<RecordedRequest> requests = new ArrayList<>();
	private final HttpServer server;

	FakeGeminiServer() throws IOException {
		this.server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		this.server.createContext("/", this::handle);
		this.server.start();
	}

	URI baseUri() {
		return URI.create("http://localhost:" + server.getAddress().getPort());
	}

	void enqueueJsonText(String text) throws JsonProcessingException {
		enqueue(200, geminiResponse(text));
	}

	void enqueue(int status, String body) {
		responses.add(new FakeResponse(status, body));
	}

	List<RecordedRequest> requests() {
		return List.copyOf(requests);
	}

	RecordedRequest lastRequest() {
		return requests.getLast();
	}

	int requestCount() {
		return requests.size();
	}

	@Override
	public void close() {
		server.stop(0);
	}

	private void handle(HttpExchange exchange) throws IOException {
		String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		requests.add(new RecordedRequest(
				exchange.getRequestURI(),
				Map.copyOf(exchange.getRequestHeaders()),
				body));

		FakeResponse response = responses.poll();
		if (response == null) {
			response = new FakeResponse(500, "{\"error\":\"missing fake response\"}");
		}

		byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(response.status(), bytes.length);
		exchange.getResponseBody().write(bytes);
		exchange.close();
	}

	private String geminiResponse(String text) throws JsonProcessingException {
		return objectMapper.writeValueAsString(Map.of(
				"candidates", List.of(Map.of(
						"content", Map.of(
								"parts", List.of(Map.of("text", text)))))));
	}

	record RecordedRequest(URI uri, Map<String, List<String>> headers, String body) {
		String header(String name) {
			for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
				if (entry.getKey().equalsIgnoreCase(name) && !entry.getValue().isEmpty()) {
					return entry.getValue().getFirst();
				}
			}
			return null;
		}
	}

	private record FakeResponse(int status, String body) {
	}
}
