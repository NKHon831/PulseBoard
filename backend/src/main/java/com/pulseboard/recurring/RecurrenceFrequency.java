package com.pulseboard.recurring;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Which days a fixed expense falls on. Deliberately narrow — specific weekdays
 * or monthly schedules can be added as further constants without touching the
 * stored column, which holds the constant name.
 *
 * <p>Keep in sync with FREQUENCIES in frontend/src/lib/recurring.ts.
 */
public enum RecurrenceFrequency {

    /** Every day, weekends included. */
    DAILY,

    /** Monday to Friday only. */
    WEEKDAYS,

    /** Saturday and Sunday only. */
    WEEKENDS;

    /** Whether an expense should be generated for {@code date}. */
    public boolean matches(LocalDate date) {
        return switch (this) {
            case DAILY -> true;
            case WEEKDAYS -> !isWeekend(date);
            case WEEKENDS -> isWeekend(date);
        };
    }

    private static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
