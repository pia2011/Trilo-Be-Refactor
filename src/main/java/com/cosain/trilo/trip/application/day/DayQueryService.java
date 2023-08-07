package com.cosain.trilo.trip.application.day;

import com.cosain.trilo.common.exception.day.DayNotFoundException;
import com.cosain.trilo.trip.application.dao.DayQueryDAO;
import com.cosain.trilo.trip.application.day.dto.DayScheduleDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DayQueryService {

    private final DayQueryDAO dayQueryDAO;

    public DayScheduleDetail searchDaySchedule(Long dayId) {
        return findDayWithScheduleByDayId(dayId);
    }

    private DayScheduleDetail findDayWithScheduleByDayId(Long dayId){
        return dayQueryDAO.findDayWithSchedulesByDayId(dayId)
                .orElseThrow(() -> new DayNotFoundException("일치하는 식별자의 여행을 조회할 수 없습니다."));
    }

    public List<DayScheduleDetail> searchDaySchedules(Long tripId){
        return dayQueryDAO.findDayScheduleListByTripId(tripId);
    }
}
