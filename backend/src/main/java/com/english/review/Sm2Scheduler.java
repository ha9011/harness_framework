package com.english.review;

import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class Sm2Scheduler {

	private static final double EASY_BONUS = 1.3;
	private static final double EASY_EASE_INCREMENT = 0.15;
	private static final double HARD_EASE_DECREMENT = 0.2;
	private static final double MIN_EASE_FACTOR = 1.3;

	public Sm2Schedule schedule(
			int currentIntervalDays,
			double currentEaseFactor,
			ReviewResult result,
			LocalDate today
	) {
		if (result == null) {
			throw ReviewException.badRequest("복습 결과는 필수입니다");
		}

		return switch (result) {
			case EASY -> schedule(today, currentIntervalDays * currentEaseFactor * EASY_BONUS,
					currentEaseFactor + EASY_EASE_INCREMENT);
			case MEDIUM -> schedule(today, currentIntervalDays * currentEaseFactor, currentEaseFactor);
			case HARD -> schedule(today, 1, Math.max(MIN_EASE_FACTOR, currentEaseFactor - HARD_EASE_DECREMENT));
		};
	}

	private Sm2Schedule schedule(LocalDate today, double intervalDays, double easeFactor) {
		int roundedInterval = Math.max(1, (int) Math.round(intervalDays));
		return new Sm2Schedule(today.plusDays(roundedInterval), roundedInterval, easeFactor);
	}
}
