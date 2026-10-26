package com.hotel.core.model.enums;

import java.time.LocalDate;

public enum BookingHistoryPeriod {
    PAST_7_DAYS("Past 7 Days"),
    TODAY("Today"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year");

    private final String label;

    BookingHistoryPeriod(String label) {
        this.label = label;
    }

    public LocalDate startDate(LocalDate today) {
        return switch (this) {
            case PAST_7_DAYS -> today.minusDays(6);
            case TODAY -> today;
            case THIS_MONTH -> today.withDayOfMonth(1);
            case THIS_YEAR -> today.withDayOfYear(1);
        };
    }

    public LocalDate endDateExclusive(LocalDate today) {
        return switch (this) {
            case PAST_7_DAYS, TODAY -> today.plusDays(1);
            case THIS_MONTH -> today.withDayOfMonth(1).plusMonths(1);
            case THIS_YEAR -> today.withDayOfYear(1).plusYears(1);
        };
    }

    @Override
    public String toString() {
        return label;
    }
}
