package com.cosain.trilo.trip.presentation.request.trip;

import lombok.Getter;

@Getter
public class TripTitleUpdateResponse {

    private Long tripId;

    public TripTitleUpdateResponse(Long tripId) {
        this.tripId = tripId;
    }
}
