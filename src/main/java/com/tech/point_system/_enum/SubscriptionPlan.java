package com.tech.point_system._enum;

public enum SubscriptionPlan {
    NONE(0),
    FREE_TRIAL(1),
    BASIC(2),
    PRO(3),
    ENTERPRISE(4);

    private final int tier;

    SubscriptionPlan(int tier) {
        this.tier = tier;
    }

    public int getTier() {
        return this.tier;
    }

    public static int getTierOf(SubscriptionPlan plan) {
        return plan != null ? plan.getTier() : 0;
    }
}
