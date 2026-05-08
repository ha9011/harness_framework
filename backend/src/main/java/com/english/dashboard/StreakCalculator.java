package com.english.dashboard;

import java.time.LocalDate;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class StreakCalculator {

	public int calculate(Set<LocalDate> reviewedDates, LocalDate today) {
		if (reviewedDates == null || reviewedDates.isEmpty()) {
			return 0;
		}

		LocalDate cursor;
		if (reviewedDates.contains(today)) {
			cursor = today;
		} else if (reviewedDates.contains(today.minusDays(1))) {
			cursor = today.minusDays(1);
		} else {
			return 0;
		}

		int streak = 0;
		while (reviewedDates.contains(cursor)) {
			streak++;
			cursor = cursor.minusDays(1);
		}
		return streak;
	}
}
