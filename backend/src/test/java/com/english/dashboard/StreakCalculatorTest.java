package com.english.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StreakCalculatorTest {

	private final StreakCalculator streakCalculator = new StreakCalculator();

	@Test
	void countsBackwardFromTodayWhenTodayHasReview() {
		LocalDate today = LocalDate.of(2026, 5, 8);

		int streak = streakCalculator.calculate(
				Set.of(
						today,
						today.minusDays(1),
						today.minusDays(2),
						today.minusDays(4)),
				today);

		assertThat(streak).isEqualTo(3);
	}

	@Test
	void countsBackwardFromYesterdayWhenTodayHasNoReview() {
		LocalDate today = LocalDate.of(2026, 5, 8);

		int streak = streakCalculator.calculate(
				Set.of(
						today.minusDays(1),
						today.minusDays(2),
						today.minusDays(4)),
				today);

		assertThat(streak).isEqualTo(2);
	}

	@Test
	void returnsZeroWhenNeitherTodayNorYesterdayHasReview() {
		LocalDate today = LocalDate.of(2026, 5, 8);

		int streak = streakCalculator.calculate(
				Set.of(
						today.minusDays(2),
						today.minusDays(3)),
				today);

		assertThat(streak).isZero();
	}
}
