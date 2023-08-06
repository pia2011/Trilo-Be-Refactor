package com.cosain.trilo.trip.application.schedule;

import com.cosain.trilo.common.exception.schedule.ScheduleNotFoundException;
import com.cosain.trilo.trip.application.dao.ScheduleQueryDAO;
import com.cosain.trilo.trip.application.schedule.dto.ScheduleDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleQueryService {

    private final ScheduleQueryDAO scheduleQueryDAO;

    public ScheduleDetail searchScheduleDetail(Long scheduleId) {
        return findSchedule(scheduleId);
    }

    private ScheduleDetail findSchedule(Long scheduleId) {
        return scheduleQueryDAO.findScheduleDetailById(scheduleId).orElseThrow(ScheduleNotFoundException::new);
    }
}
