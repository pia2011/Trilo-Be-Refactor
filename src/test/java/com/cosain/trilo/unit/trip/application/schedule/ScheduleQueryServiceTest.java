package com.cosain.trilo.unit.trip.application.schedule;

import com.cosain.trilo.common.exception.schedule.ScheduleNotFoundException;
import com.cosain.trilo.trip.application.dao.ScheduleQueryDAO;
import com.cosain.trilo.trip.application.schedule.ScheduleQueryService;
import com.cosain.trilo.trip.application.schedule.dto.ScheduleDetail;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class ScheduleQueryServiceTest {

    @InjectMocks
    private ScheduleQueryService scheduleQueryService;

    @Mock
    private ScheduleQueryDAO scheduleQueryDAO;

    @Nested
    class 일정_조회_테스트{
        @Test
        @DisplayName("정상 호출 및 반환 테스트")
        void searchScheduleDetailTest(){
            // given
            ScheduleDetail scheduleDetail = new ScheduleDetail(1L, 1L, "제목", "장소 이름", 24.24, 24.24, 3L, "내용", LocalTime.of(15, 30), LocalTime.of(16, 0));
            given(scheduleQueryDAO.findScheduleDetailById(anyLong())).willReturn(Optional.of(scheduleDetail));

            // when
            ScheduleDetail dto = scheduleQueryService.searchScheduleDetail(anyLong());

            // then
            assertThat(dto.getScheduleId()).isEqualTo(scheduleDetail.getScheduleId());
            assertThat(dto.getDayId()).isEqualTo(scheduleDetail.getDayId());
            assertThat(dto.getContent()).isEqualTo(scheduleDetail.getContent());
            assertThat(dto.getTitle()).isEqualTo(scheduleDetail.getTitle());
            assertThat(dto.getCoordinate().getLatitude()).isEqualTo(scheduleDetail.getCoordinate().getLatitude());
            assertThat(dto.getCoordinate().getLongitude()).isEqualTo(scheduleDetail.getCoordinate().getLongitude());
            assertThat(dto.getOrder()).isEqualTo(scheduleDetail.getOrder());
            assertThat(dto.getPlaceName()).isEqualTo(scheduleDetail.getPlaceName());
        }

        @Test
        @DisplayName("조회한 일정이 없을 경우 ScheduleNotFoundException 이 발생한다 ")
        void searchScheduleDetail_Fail_ScheduleNotFound() {
            // given
            given(scheduleQueryDAO.findScheduleDetailById(anyLong())).willReturn(Optional.empty());

            // when & then
            Assertions.assertThatThrownBy(() -> scheduleQueryService.searchScheduleDetail(1L)).isInstanceOf(ScheduleNotFoundException.class);
        }
    }
}
