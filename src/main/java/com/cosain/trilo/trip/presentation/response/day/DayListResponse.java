package com.cosain.trilo.trip.presentation.response.day;

import com.cosain.trilo.trip.application.day.dto.DayScheduleDetail;
import lombok.Getter;

import java.util.List;

@Getter
public class DayListResponse {

    private List<DayScheduleDetail> days;

    private DayListResponse(List<DayScheduleDetail> dayScheduleDetails) {
        this.days = dayScheduleDetails;
    }

    public static DayListResponse of(List<DayScheduleDetail> dayScheduleDetails){
        return new DayListResponse(dayScheduleDetails);
    }

}
