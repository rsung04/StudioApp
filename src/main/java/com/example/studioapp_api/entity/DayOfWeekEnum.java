package com.example.studioapp_api.entity;

public enum DayOfWeekEnum {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;

    public java.time.DayOfWeek toJavaTimeDayOfWeek() {
        return java.time.DayOfWeek.valueOf(this.name());
    }
}