package com.cosain.trilo.trip.presentation.schedule;

import com.cosain.trilo.auth.application.token.UserPayload;
import com.cosain.trilo.auth.presentation.Login;
import com.cosain.trilo.auth.presentation.LoginUser;
import com.cosain.trilo.common.exception.CustomValidationException;
import com.cosain.trilo.common.exception.day.DayNotFoundException;
import com.cosain.trilo.common.exception.day.InvalidTripDayException;
import com.cosain.trilo.common.exception.schedule.InvalidScheduleMoveTargetOrderException;
import com.cosain.trilo.common.exception.schedule.NoScheduleMoveAuthorityException;
import com.cosain.trilo.common.exception.schedule.ScheduleNotFoundException;
import com.cosain.trilo.common.exception.schedule.TooManyDayScheduleException;
import com.cosain.trilo.trip.application.schedule.ScheduleCommandService;
import com.cosain.trilo.trip.application.schedule.ScheduleQueryService;
import com.cosain.trilo.trip.application.schedule.dto.ScheduleCreateCommand;
import com.cosain.trilo.trip.application.schedule.dto.ScheduleDetail;
import com.cosain.trilo.trip.application.schedule.dto.ScheduleMoveCommand;
import com.cosain.trilo.trip.application.schedule.dto.ScheduleUpdateCommand;
import com.cosain.trilo.trip.presentation.schedule.dto.request.ScheduleCreateRequest;
import com.cosain.trilo.trip.presentation.schedule.dto.request.ScheduleMoveRequest;
import com.cosain.trilo.trip.presentation.schedule.dto.request.ScheduleUpdateRequest;
import com.cosain.trilo.trip.presentation.schedule.dto.response.ScheduleCreateResponse;
import com.cosain.trilo.trip.presentation.schedule.dto.response.ScheduleMoveResponse;
import com.cosain.trilo.trip.presentation.schedule.dto.response.ScheduleUpdateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleCommandService scheduleCommandService;
    private final ScheduleQueryService scheduleQueryService;

    @GetMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.OK)
    public ScheduleDetail findSingleSchedule(@PathVariable Long scheduleId) {
        return scheduleQueryService.searchScheduleDetail(scheduleId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Login
    public ScheduleCreateResponse createSchedule(@LoginUser UserPayload userPayload, @RequestBody @Valid ScheduleCreateRequest request) {
        Long requestTripperId = userPayload.getId();
        var command = makeCommand(request, requestTripperId);

        Long scheduleId = scheduleCommandService.createSchedule(command);
        return ScheduleCreateResponse.from(scheduleId);
    }

    private static ScheduleCreateCommand makeCommand(ScheduleCreateRequest request, Long requestTripperId) {
        return ScheduleCreateCommand.of(
                requestTripperId,
                request.getTripId(),
                request.getDayId(),
                request.getTitle(),
                request.getPlaceId(),
                request.getPlaceName(),
                request.getCoordinate().getLatitude(),
                request.getCoordinate().getLongitude()
        );
    }
    @PutMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.OK)
    @Login
    public ScheduleUpdateResponse updateSchedule(@LoginUser UserPayload userPayload, @PathVariable Long scheduleId, @RequestBody ScheduleUpdateRequest request) {
        Long requestTripperId = userPayload.getId();

        var command = ScheduleUpdateCommand.of(
                scheduleId, requestTripperId, request.getTitle(),
                request.getContent(), request.getStartTime(), request.getEndTime()
        );

        scheduleCommandService.updateSchedule(command);
        return ScheduleUpdateResponse.from(scheduleId);
    }

    @PutMapping("/{scheduleId}/position")
    @ResponseStatus(HttpStatus.OK)
    @Login
    public ScheduleMoveResponse moveSchedule(@LoginUser UserPayload userPayload, @PathVariable Long scheduleId, @RequestBody ScheduleMoveRequest request)
            throws CustomValidationException, ScheduleNotFoundException, DayNotFoundException, NoScheduleMoveAuthorityException, TooManyDayScheduleException,
            InvalidTripDayException, InvalidScheduleMoveTargetOrderException {

        Long requestTripperId = userPayload.getId();

        var command = ScheduleMoveCommand.of(scheduleId, requestTripperId, request.getTargetDayId(), request.getTargetOrder());

        var scheduleMoveResult = scheduleCommandService.moveSchedule(command);
        return ScheduleMoveResponse.from(scheduleMoveResult);
    }

    @DeleteMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Login
    public void deleteSchedule(@LoginUser UserPayload userPayload, @PathVariable Long scheduleId) {
        Long deleteTripperId = userPayload.getId();
        scheduleCommandService.deleteSchedule(scheduleId, deleteTripperId);
    }

}
