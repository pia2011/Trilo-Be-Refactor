package com.cosain.trilo.trip.application.dao;

import com.cosain.trilo.trip.application.schedule.dto.ScheduleDetail;
import com.cosain.trilo.trip.application.trip.service.temporary_search.TempScheduleListQueryParam;
import com.cosain.trilo.trip.application.trip.service.temporary_search.TempScheduleListSearchResult;

import java.util.Optional;

public interface ScheduleQueryDAO {

    Optional<ScheduleDetail> findScheduleDetailById(Long scheduleId);
    TempScheduleListSearchResult findTemporarySchedules(TempScheduleListQueryParam queryParam);
    boolean existById(Long scheduleId);
}
