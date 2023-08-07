package com.cosain.trilo.trip.application.dao;

import com.cosain.trilo.trip.application.day.dto.DayScheduleDetail;

import java.util.List;
import java.util.Optional;

public interface DayQueryDAO {

    Optional<DayScheduleDetail> findDayWithSchedulesByDayId(Long dayId);

    List<DayScheduleDetail> findDayScheduleListByTripId(Long tripId);
}
