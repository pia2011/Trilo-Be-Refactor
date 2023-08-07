package com.cosain.trilo.trip.application.dao;

import com.cosain.trilo.trip.application.trip.dto.TripSearchResponse;
import com.cosain.trilo.trip.application.trip.dto.TripDetail;
import com.cosain.trilo.trip.application.trip.dto.TripListQueryParam;
import com.cosain.trilo.trip.application.trip.dto.TripListSearchResult;
import com.cosain.trilo.trip.infra.dto.TripStatistics;
import com.cosain.trilo.trip.presentation.request.trip.TripSearchRequest;

import java.time.LocalDate;
import java.util.Optional;

public interface TripQueryDAO {

    Optional<TripDetail> findTripDetailById(Long tripId);
    TripListSearchResult findTripSummariesByTripperId(TripListQueryParam queryParam);
    boolean existById(Long tripId);
    TripStatistics findTripStaticsByTripperId(Long tripperId, LocalDate today);
    TripSearchResponse findWithSearchConditions(TripSearchRequest request);
}
