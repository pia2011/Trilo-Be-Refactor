package com.cosain.trilo.trip.presentation;

import com.cosain.trilo.auth.application.token.UserPayload;
import com.cosain.trilo.auth.presentation.Login;
import com.cosain.trilo.auth.presentation.LoginUser;
import com.cosain.trilo.trip.application.day.DayCommandService;
import com.cosain.trilo.trip.application.day.DayQueryService;
import com.cosain.trilo.trip.application.day.dto.DayColorUpdateCommand;
import com.cosain.trilo.trip.application.day.dto.DayScheduleDetail;
import com.cosain.trilo.trip.presentation.request.day.DayColorUpdateRequest;
import com.cosain.trilo.trip.presentation.response.day.DayColorUpdateResponse;
import com.cosain.trilo.trip.presentation.response.day.DayListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DayController {

    private final DayCommandService dayCommandService;
    private final DayQueryService dayQueryService;

    @GetMapping("/api/days/{dayId}")
    @ResponseStatus(HttpStatus.OK)
    public DayScheduleDetail findSingleDay(@PathVariable Long dayId) {
        return dayQueryService.searchDaySchedule(dayId);
    }

    @GetMapping("/api/trips/{tripId}/days")
    @ResponseStatus(HttpStatus.OK)
    public DayListResponse findTripDayList(@PathVariable Long tripId) {
        List<DayScheduleDetail> dayScheduleDetails = dayQueryService.searchDaySchedules(tripId);
        return DayListResponse.of(dayScheduleDetails);
    }

    @PutMapping("/api/days/{dayId}/color")
    @ResponseStatus(HttpStatus.OK)
    @Login
    public DayColorUpdateResponse updateDayColor(
            @PathVariable("dayId") Long dayId,
            @LoginUser UserPayload userPayload,
            @RequestBody DayColorUpdateRequest request) {

        long requestTripperId = userPayload.getId();

        var command = DayColorUpdateCommand.of(dayId, requestTripperId, request.getColorName());
        dayCommandService.updateDayColor(command);
        return new DayColorUpdateResponse(dayId);
    }
}
