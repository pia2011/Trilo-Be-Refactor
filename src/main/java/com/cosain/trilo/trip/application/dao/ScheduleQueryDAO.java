package com.cosain.trilo.trip.application.dao;

import com.cosain.trilo.trip.application.schedule.dto.ScheduleDetail;
import com.cosain.trilo.trip.application.trip.dto.TempScheduleListQueryParam;
import com.cosain.trilo.trip.application.trip.dto.TempScheduleListSearchResult;

import java.util.Optional;

public interface ScheduleQueryDAO {

    Optional<ScheduleDetail> findScheduleDetailById(Long scheduleId);
    TempScheduleListSearchResult findTemporarySchedules(TempScheduleListQueryParam queryParam);
    boolean existById(Long scheduleId);
}
