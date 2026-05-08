package com.english.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class Sm2SchedulerTest {

	private final Sm2Scheduler scheduler = new Sm2Scheduler();

	@Test
	void easyMultipliesIntervalByEaseFactorAndBonusThenIncreasesEaseFactor() {
		LocalDate today = LocalDate.of(2026, 5, 8);

		Sm2Schedule schedule = scheduler.schedule(4, 2.0, ReviewResult.EASY, today);

		assertThat(schedule.intervalDays()).isEqualTo(10);
		assertThat(schedule.easeFactor()).isEqualTo(2.15);
		assertThat(schedule.nextReviewDate()).isEqualTo(LocalDate.of(2026, 5, 18));
	}

	@Test
	void mediumMultipliesIntervalByEaseFactorAndKeepsEaseFactor() {
		LocalDate today = LocalDate.of(2026, 5, 8);

		Sm2Schedule schedule = scheduler.schedule(1, 2.5, ReviewResult.MEDIUM, today);

		assertThat(schedule.intervalDays()).isEqualTo(3);
		assertThat(schedule.easeFactor()).isEqualTo(2.5);
		assertThat(schedule.nextReviewDate()).isEqualTo(LocalDate.of(2026, 5, 11));
	}

	@Test
	void hardResetsIntervalAndDoesNotReduceEaseFactorBelowMinimum() {
		LocalDate today = LocalDate.of(2026, 5, 8);

		Sm2Schedule schedule = scheduler.schedule(9, 1.35, ReviewResult.HARD, today);

		assertThat(schedule.intervalDays()).isEqualTo(1);
		assertThat(schedule.easeFactor()).isEqualTo(1.3);
		assertThat(schedule.nextReviewDate()).isEqualTo(LocalDate.of(2026, 5, 9));
	}
}
