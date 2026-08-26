package com.tech.point_system._enum;

public enum BillingPeriod {
    MONTHLY(30, 1),
    QUARTERLY(90, 3),
    SEMIANNUAL(180, 6),
    YEARLY(365, 12);

    private final int days;
    private final int months;

    BillingPeriod(int days, int months) {
        this.days = days;
        this.months = months;
    }

    public int getDays() {
        return days;
    }

    public int getMonths() {
        return months;
    }
}

