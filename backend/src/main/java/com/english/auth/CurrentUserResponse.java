package com.english.auth;

public record CurrentUserResponse(
		Long id,
		String email,
		String nickname
) {

	public static CurrentUserResponse from(User user) {
		return new CurrentUserResponse(user.getId(), user.getEmail(), user.getNickname());
	}
}
