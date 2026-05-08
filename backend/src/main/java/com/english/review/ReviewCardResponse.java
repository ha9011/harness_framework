package com.english.review;

public record ReviewCardResponse(
		Long reviewItemId,
		ReviewItemType itemType,
		ReviewDirection direction,
		ReviewCardFront front,
		ReviewCardBack back
) {
}
