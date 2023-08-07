package com.cosain.trilo.unit.trip.application.day;

import com.cosain.trilo.common.exception.day.DayNotFoundException;
import com.cosain.trilo.trip.application.dao.DayQueryDAO;
import com.cosain.trilo.trip.application.day.DayQueryService;
import com.cosain.trilo.trip.application.day.dto.DayScheduleDetail;
import com.cosain.trilo.trip.application.day.dto.ScheduleSummary;
import com.cosain.trilo.trip.domain.vo.DayColor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DayQueryServiceTest {

    @InjectMocks
    private DayQueryService dayQueryService;

    @Mock
    private DayQueryDAO dayQueryDAO;

    @Nested
    class Day_조회{
        @Test
        void 메서드_호출_테스트(){

            // given
            Long dayId = 1L;
            ScheduleSummary scheduleSummary = new ScheduleSummary(1L, "제목", "장소","장소 식별자", 33.33, 33.33);
            DayScheduleDetail dayScheduleDetail = new DayScheduleDetail(dayId, 1L, LocalDate.of(2023, 5, 5), DayColor.RED, List.of(scheduleSummary));
            given(dayQueryDAO.findDayWithSchedulesByDayId(1L)).willReturn(Optional.of(dayScheduleDetail));

            // when
            DayScheduleDetail findDayScheduleDetail = dayQueryService.searchDaySchedule(1L);
            // then
            assertThat(findDayScheduleDetail.getDayId()).isEqualTo(dayId);
            verify(dayQueryDAO, times(1)).findDayWithSchedulesByDayId(dayId);
        }

        @Test
        void Day_단건_조회시_찾으려는_Day_가_존재하지_않는다면_DayNotFoundException예외가_발생한다(){

            // given
            Long dayId = 1L;
            given(dayQueryDAO.findDayWithSchedulesByDayId(dayId)).willReturn(Optional.empty());

            // when & then
            Assertions.assertThatThrownBy(() -> dayQueryService.searchDaySchedule(dayId)).isInstanceOf(DayNotFoundException.class);
        }

        @Test
        void Day_목록_조회(){
            // given
            Long tripId = 1L;
            List<DayScheduleDetail> dayScheduleDetails = new ArrayList<>();
            given(dayQueryDAO.findDayScheduleListByTripId(eq(tripId))).willReturn(dayScheduleDetails);
            // when
            List<DayScheduleDetail> daysWithSchedulesByTripId = dayQueryService.searchDaySchedules(tripId);
            // then
            verify(dayQueryDAO).findDayScheduleListByTripId(eq(tripId));
            assertThat(daysWithSchedulesByTripId).isEqualTo(dayScheduleDetails);
        }
    }

    @Nested
    class Day_목록_조회{
        @Test
        void 메서드_호출_테스트(){
            // given
            Long tripId = 1L;
            List<DayScheduleDetail> dayScheduleDetails = new ArrayList<>();
            given(dayQueryDAO.findDayScheduleListByTripId(eq(tripId))).willReturn(dayScheduleDetails);
            // when
            List<DayScheduleDetail> daysWithSchedulesByTripId = dayQueryService.searchDaySchedules(tripId);
            // then
            verify(dayQueryDAO).findDayScheduleListByTripId(eq(tripId));
            assertThat(daysWithSchedulesByTripId).isEqualTo(dayScheduleDetails);
        }
    }
}
