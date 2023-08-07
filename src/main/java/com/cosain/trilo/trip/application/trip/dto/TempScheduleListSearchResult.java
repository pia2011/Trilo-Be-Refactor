package com.cosain.trilo.trip.application.trip.dto;

import com.cosain.trilo.trip.application.day.dto.ScheduleSummary;
import lombok.Getter;

import java.util.List;

@Getter
public class TempScheduleListSearchResult {

    private final boolean hasNext;
    private final List<ScheduleSummary> tempSchedules;

    public static TempScheduleListSearchResult of(boolean hasNext, List<ScheduleSummary> tempSchedules) {
        return new TempScheduleListSearchResult(hasNext, tempSchedules);
    }

    private TempScheduleListSearchResult(boolean hasNext, List<ScheduleSummary> tempSchedules) {
        this.hasNext = hasNext;
        this.tempSchedules = tempSchedules;
    }
}
