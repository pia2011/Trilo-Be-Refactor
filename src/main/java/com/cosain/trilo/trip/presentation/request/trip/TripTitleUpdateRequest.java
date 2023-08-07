package com.cosain.trilo.trip.presentation.request.trip;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripTitleUpdateRequest {

    private String title;

    public TripTitleUpdateRequest(String title) {
        this.title = title;
    }
}
