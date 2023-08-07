package com.cosain.trilo.trip.presentation.response.trip;

import lombok.Getter;

@Getter
public class TripImageUpdateResponse {

    private Long tripId;
    private String imageURL;

    public TripImageUpdateResponse(Long tripId, String imageURL) {
        this.tripId = tripId;
        this.imageURL = imageURL;
    }
}
