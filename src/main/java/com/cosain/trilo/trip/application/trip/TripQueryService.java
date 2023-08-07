package com.cosain.trilo.trip.application.trip;

import com.cosain.trilo.common.exception.schedule.ScheduleNotFoundException;
import com.cosain.trilo.common.exception.trip.TripNotFoundException;
import com.cosain.trilo.trip.application.dao.ScheduleQueryDAO;
import com.cosain.trilo.trip.application.dao.TripQueryDAO;
import com.cosain.trilo.trip.application.exception.TripperNotFoundException;
import com.cosain.trilo.trip.application.trip.dto.TempScheduleListQueryParam;
import com.cosain.trilo.trip.application.trip.dto.TempScheduleListSearchResult;
import com.cosain.trilo.trip.application.trip.dto.TripSearchResponse;
import com.cosain.trilo.trip.application.trip.dto.TripDetail;
import com.cosain.trilo.trip.application.trip.dto.TripListQueryParam;
import com.cosain.trilo.trip.application.trip.dto.TripListSearchResult;
import com.cosain.trilo.trip.infra.adapter.TripImageOutputAdapter;
import com.cosain.trilo.trip.presentation.request.trip.TripSearchRequest;
import com.cosain.trilo.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripQueryService {


    private final TripQueryDAO tripQueryDAO;
    private final ScheduleQueryDAO scheduleQueryDAO;
    private final TripImageOutputAdapter tripImageOutputAdapter;
    private final UserRepository userRepository;

    public TempScheduleListSearchResult searchTemporary(TempScheduleListQueryParam queryParam) {
        verifyTripExists(queryParam.getTripId());
        verifyScheduleExists(queryParam.getScheduleId());
        return scheduleQueryDAO.findTemporarySchedules(queryParam);
    }

    private void verifyScheduleExists(Long scheduleId) {
        if(scheduleId != null && !scheduleQueryDAO.existById(scheduleId))
            throw new ScheduleNotFoundException();
    }

    private void verifyTripExists(Long tripId) {
        if(!tripQueryDAO.existById(tripId)){
            throw new TripNotFoundException();
        }
    }

    public TripSearchResponse findBySearchConditions(TripSearchRequest request){
        TripSearchResponse searchResponse = tripQueryDAO.findWithSearchConditions(request);
        searchResponse.getTrips().forEach(this::updateImageURL);
        return searchResponse;
    }

    private void updateImageURL(TripSearchResponse.TripSummary tripSummary) {
        String imageName = tripSummary.getImageURL();
        String fullImageURL = tripImageOutputAdapter.getFullTripImageURL(imageName);
        tripSummary.updateImageURL(fullImageURL);
    }

    public TripDetail searchTripDetail(Long tripId) {
        return tripQueryDAO.findTripDetailById(tripId).orElseThrow(TripNotFoundException::new);
    }

    public TripListSearchResult searchTripList(TripListQueryParam queryParam) {
        verifyTripperExists(queryParam.getTripperId());

        var tripListSearchResult = tripQueryDAO.findTripSummariesByTripperId(queryParam);
        tripListSearchResult.getTrips().forEach(this::updateImageURL);
        return tripListSearchResult;
    }

    private void verifyTripperExists(Long tripperId) {

        userRepository.findById(tripperId)
                .orElseThrow(() -> new TripperNotFoundException("해당 식별자의 사용자(여행자)를 찾지 못 함"));
    }

    private void updateImageURL(TripListSearchResult.TripSummary tripSummary) {
        String imageName = tripSummary.getImageURL(); // 실제 DB에 저장된 이미지 이름
        String fullImageURL = tripImageOutputAdapter.getFullTripImageURL(imageName);
        tripSummary.updateImageURL(fullImageURL);
    }
}
