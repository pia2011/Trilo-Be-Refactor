package com.cosain.trilo.trip.presentation;

import com.cosain.trilo.auth.application.token.UserPayload;
import com.cosain.trilo.auth.presentation.Login;
import com.cosain.trilo.auth.presentation.LoginUser;
import com.cosain.trilo.common.exception.CustomValidationException;
import com.cosain.trilo.common.exception.trip.EmptyPeriodUpdateException;
import com.cosain.trilo.common.exception.trip.NoTripDeleteAuthorityException;
import com.cosain.trilo.common.exception.trip.NoTripUpdateAuthorityException;
import com.cosain.trilo.common.exception.trip.TripNotFoundException;
import com.cosain.trilo.common.file.ImageFile;
import com.cosain.trilo.trip.application.trip.TripCommandService;
import com.cosain.trilo.trip.application.trip.TripQueryService;
import com.cosain.trilo.trip.application.trip.dto.*;
import com.cosain.trilo.trip.presentation.request.trip.*;
import com.cosain.trilo.trip.presentation.response.trip.TripCreateResponse;
import com.cosain.trilo.trip.presentation.response.trip.TripImageUpdateResponse;
import com.cosain.trilo.trip.presentation.response.trip.TripPeriodUpdateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TripController {

    private final TripQueryService tripQueryService;
    private final TripCommandService tripCommandService;

    @Login
    @PostMapping("/api/trips")
    @ResponseStatus(HttpStatus.CREATED)
    public TripCreateResponse createTrip(@LoginUser UserPayload userPayload, @RequestBody TripCreateRequest request) throws CustomValidationException {
        Long tripperId = userPayload.getId();
        var command = TripCreateCommand.of(tripperId, request.getTitle());

        Long tripId = tripCommandService.createTrip(command);
        return TripCreateResponse.from(tripId);
    }

    @Login
    @DeleteMapping("/api/trips/{tripId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrip(@LoginUser UserPayload userPayload, @PathVariable Long tripId) throws TripNotFoundException, NoTripDeleteAuthorityException {
        Long tripperId = userPayload.getId();
        tripCommandService.deleteTrip(tripId, tripperId);
    }

    @Login
    @PutMapping("/api/trips/{tripId}/title")
    @ResponseStatus(HttpStatus.OK)
    public TripTitleUpdateResponse updateTrip(@LoginUser UserPayload userPayload, @PathVariable Long tripId, @RequestBody TripTitleUpdateRequest request) {
        Long requestTripperId = userPayload.getId();

        var command = TripTitleUpdateCommand.of(tripId, requestTripperId, request.getTitle());
        tripCommandService.updateTripTitle(command);

        return new TripTitleUpdateResponse(tripId);
    }

    @Login
    @PostMapping("/api/trips/{tripId}/image/update")
    @ResponseStatus(HttpStatus.OK)
    public TripImageUpdateResponse updateTripImage(
            @LoginUser UserPayload userPayload,
            @PathVariable Long tripId,
            @ModelAttribute @Valid TripImageUpdateRequest request) {

        Long tripperId = userPayload.getId();
        ImageFile imageFile = ImageFile.from(request.getImage());
        TripImageUpdateCommand command = new TripImageUpdateCommand(tripId, tripperId, imageFile);

        String imageURL = tripCommandService.updateTripImage(command);
        return new TripImageUpdateResponse(tripId, imageURL);
    }

    @Login
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/api/trips/{tripId}/period")
    public TripPeriodUpdateResponse updateTrip(
            @LoginUser UserPayload userPayload, @PathVariable Long tripId, @RequestBody TripPeriodUpdateRequest request)
            throws CustomValidationException, TripNotFoundException, NoTripUpdateAuthorityException, EmptyPeriodUpdateException {

        Long requestTripperId = userPayload.getId();

        var command = TripPeriodUpdateCommand.of(tripId, requestTripperId, request.getStartDate(), request.getEndDate());

        tripCommandService.updateTripPeriod(command);
        return new TripPeriodUpdateResponse(tripId);
    }

    @GetMapping("/api/trippers/{tripperId}/trips")
    @ResponseStatus(HttpStatus.OK)
    public TripListSearchResult findTripperTripList(@ModelAttribute TripListSearchRequest request, @PathVariable Long tripperId) {
        var queryParam = TripListQueryParam.of(tripperId, request.getTripId(), request.getSize());
        return tripQueryService.searchTripList(queryParam);
    }

    @GetMapping("/api/trips/{tripId}/temporary-storage")
    @ResponseStatus(HttpStatus.OK)
    public TempScheduleListSearchResult findTripTemporaryStorage(@PathVariable Long tripId, @ModelAttribute TempScheduleListRequest request) {
        var queryParam = TempScheduleListQueryParam.of(tripId, request.getScheduleId(), request.getSize());
        return tripQueryService.searchTemporary(queryParam);
    }

    @GetMapping("/api/trips")
    @ResponseStatus(HttpStatus.OK)
    public TripSearchResponse findTripList(@ModelAttribute @Valid TripSearchRequest tripSearchRequest){
        return tripQueryService.findBySearchConditions(tripSearchRequest);
    }

    @GetMapping("/api/trips/{tripId}")
    @ResponseStatus(HttpStatus.OK)
    public TripDetail findSingleTrip(@PathVariable Long tripId) {
        return tripQueryService.searchTripDetail(tripId);
    }
}
