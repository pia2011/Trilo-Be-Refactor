package com.cosain.trilo.trip.presentation.response.day;

import lombok.Getter;

@Getter
public class DayColorUpdateResponse {

    private Long dayId;

    public DayColorUpdateResponse (Long dayId) {
        this.dayId = dayId;
    }
}
