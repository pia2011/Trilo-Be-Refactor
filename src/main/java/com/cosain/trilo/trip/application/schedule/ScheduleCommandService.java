package com.cosain.trilo.trip.application.schedule;

import com.cosain.trilo.common.exception.day.DayNotFoundException;
import com.cosain.trilo.common.exception.day.InvalidTripDayException;
import com.cosain.trilo.common.exception.schedule.*;
import com.cosain.trilo.common.exception.trip.TripNotFoundException;
import com.cosain.trilo.trip.application.exception.NoScheduleCreateAuthorityException;
import com.cosain.trilo.trip.application.exception.NoScheduleDeleteAuthorityException;
import com.cosain.trilo.trip.application.exception.NoScheduleUpdateAuthorityException;
import com.cosain.trilo.trip.application.exception.TooManyTripScheduleException;
import com.cosain.trilo.trip.application.schedule.dto.ScheduleCreateCommand;
import com.cosain.trilo.trip.application.schedule.dto.ScheduleMoveCommand;
import com.cosain.trilo.trip.application.schedule.dto.ScheduleMoveResult;
import com.cosain.trilo.trip.application.schedule.dto.ScheduleUpdateCommand;
import com.cosain.trilo.trip.domain.dto.ScheduleMoveDto;
import com.cosain.trilo.trip.domain.entity.Day;
import com.cosain.trilo.trip.domain.entity.Schedule;
import com.cosain.trilo.trip.domain.entity.Trip;
import com.cosain.trilo.trip.domain.repository.DayRepository;
import com.cosain.trilo.trip.domain.repository.ScheduleRepository;
import com.cosain.trilo.trip.domain.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ScheduleCommandService {

    private final TripRepository tripRepository;
    private final ScheduleRepository scheduleRepository;
    private final DayRepository dayRepository;

    @Transactional
    public Long createSchedule(ScheduleCreateCommand command) {
        Day targetDay = findTargetDay(command.getTargetDayId());
        Trip trip = findTrip(command.getTripId());
        validateCreateAuthority(trip, command.getRequestTripperId());
        validateTripScheduleCount(command.getTripId());
        validateDayScheduleCount(command.getTargetDayId());

        Schedule schedule;
        try {
            schedule = trip.createSchedule(targetDay, command.getScheduleTitle(), command.getPlace());
        } catch (ScheduleIndexRangeException e) {

            scheduleRepository.relocateDaySchedules(command.getTripId(), command.getTargetDayId());

            trip = findTrip(command.getTripId());
            targetDay = findTargetDay(command.getTargetDayId());
            schedule =  trip.createSchedule(targetDay, command.getScheduleTitle(), command.getPlace());
        }
        scheduleRepository.save(schedule);
        return schedule.getId();
    }
    private Day findTargetDay(Long dayId){
        return (dayId == null)
                ? null
                : dayRepository.findByIdWithTrip(dayId).orElseThrow(() -> new DayNotFoundException("Schedule을 Day에 넣으려고 했는데, 해당하는 Day가 존재하지 않음."));
    }

    private Trip findTrip(Long tripId){
        return tripRepository.findById(tripId).orElseThrow(() -> new TripNotFoundException("trip이 존재하지 않음"));
    }

    private void validateCreateAuthority(Trip trip, Long tripperId) {
        if(!trip.getTripperId().equals(tripperId)){
            throw new NoScheduleCreateAuthorityException("여행의 소유주가 아닌 사람이, 일정을 생성하려고 함");
        }
    }

    private void validateTripScheduleCount(Long tripId) {
        int tripScheduleCount = scheduleRepository.findTripScheduleCount(tripId);

        if (tripScheduleCount == Trip.MAX_TRIP_SCHEDULE_COUNT) {
            throw new TooManyTripScheduleException("여행 생성 시도 -> 여행 최대 일정 갯수 초과");
        }
    }

    private void validateDayScheduleCount(Long dayId) {
        if (dayId == null) {
            return;
        }
        int dayScheduleCount = scheduleRepository.findDayScheduleCount(dayId);

        if (dayScheduleCount == Day.MAX_DAY_SCHEDULE_COUNT) {
            throw new TooManyDayScheduleException("여행 생성 시도 -> Day의 최대 일정 갯수 초과");
        }
    }

    @Transactional
    public void deleteSchedule(Long scheduleId, Long deleteTripperId) {
        Schedule schedule = findSchedule(scheduleId);
        validateDeleteAuthority(schedule, deleteTripperId);
        scheduleRepository.delete(schedule);
    }

    private void validateDeleteAuthority(Schedule schedule, Long deleteTripperId) {
        Long tripperId = schedule.getTrip().getTripperId();

        if (!tripperId.equals(deleteTripperId)) {
            throw new NoScheduleDeleteAuthorityException("여행의 소유주가 아닌 사람이 일정을 삭제하려고 시도함");
        }
    }

    @Transactional
    public void updateSchedule(ScheduleUpdateCommand command) {
        Schedule schedule = findSchedule(command.getScheduleId());
        validateScheduleUpdateAuthority(schedule, command.getRequestTripperId());

        schedule.changeTitle(command.getScheduleTitle());
        schedule.changeContent(command.getScheduleContent());
        schedule.changeTime(command.getScheduleTime());
    }

    private Schedule findSchedule(Long scheduleId) {
        return scheduleRepository.findByIdWithTrip(scheduleId).orElseThrow(() -> new ScheduleNotFoundException("일정이 존재하지 않습니다."));
    }

    private void validateScheduleUpdateAuthority(Schedule schedule, Long tripperId) {
        if (!schedule.getTrip().getTripperId().equals(tripperId)) {
            throw new NoScheduleUpdateAuthorityException("일정을 수정할 권한이 없습니다");
        }
    }

    @Transactional
    public ScheduleMoveResult moveSchedule(ScheduleMoveCommand command)
            throws ScheduleNotFoundException, DayNotFoundException, NoScheduleMoveAuthorityException, TooManyDayScheduleException,
            InvalidTripDayException, InvalidScheduleMoveTargetOrderException {

        Schedule schedule = findScheduleWithTrip(command.getScheduleId());
        Day targetDay = findTargetDayWithTrip(command.getTargetDayId());
        Trip trip = schedule.getTrip();

        validateScheduleMoveAuthority(trip, command.getRequestTripperId());
        validateTargetDayScheduleCount(schedule, targetDay);

        return moveSchedule(schedule, trip, targetDay, command);
    }

    private Schedule findScheduleWithTrip(Long scheduleId) throws ScheduleNotFoundException {
        return scheduleRepository.findByIdWithTrip(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException("일치하는 식별자의 일정을 찾을 수 없음"));
    }

    private Day findTargetDayWithTrip(Long targetDayId) throws DayNotFoundException {
        if (targetDayId == null) {
            return null;
        }
        return dayRepository.findByIdWithTrip(targetDayId)
                .orElseThrow(() -> new DayNotFoundException("일치하는 식별자의 Day를 찾을 수 없음"));
    }

    private void validateScheduleMoveAuthority(Trip trip, Long requestTripperId) throws NoScheduleMoveAuthorityException {
        if (!trip.getTripperId().equals(requestTripperId)) {
            throw new NoScheduleMoveAuthorityException("권한 없는 사람이 일정을 이동하려 함");
        }
    }

    private void validateTargetDayScheduleCount(Schedule schedule, Day targetDay) throws TooManyDayScheduleException {
        Long beforeDayId = schedule.getDay() == null ? null : schedule.getDay().getId();
        Long afterDayId = targetDay == null ? null : targetDay.getId();

        if (Objects.equals(beforeDayId, afterDayId) || afterDayId == null) {
            return;
        }

        if (scheduleRepository.findDayScheduleCount(afterDayId) == Day.MAX_DAY_SCHEDULE_COUNT) {
            throw new TooManyDayScheduleException("옮기려는 Day 자리에 일정이 가득참");
        }
    }

    private ScheduleMoveResult moveSchedule(Schedule schedule, Trip trip, Day targetDay, ScheduleMoveCommand command)
            throws InvalidTripDayException, InvalidScheduleMoveTargetOrderException {

        ScheduleMoveDto moveDto;
        try {
            moveDto = trip.moveSchedule(schedule, targetDay, command.getTargetOrder());
        } catch (MidScheduleIndexConflictException | ScheduleIndexRangeException e) {

            scheduleRepository.relocateDaySchedules(trip.getId(), command.getTargetDayId());

            moveDto = retryMoveSchedule(command);
        }
        return ScheduleMoveResult.from(moveDto);
    }

    private ScheduleMoveDto retryMoveSchedule(ScheduleMoveCommand command) {

        Schedule schedule = findScheduleWithTrip(command.getScheduleId());
        Day targetDay = findTargetDayWithTrip(command.getTargetDayId());
        Trip trip = schedule.getTrip();

        return trip.moveSchedule(schedule, targetDay, command.getTargetOrder());
    }
}
