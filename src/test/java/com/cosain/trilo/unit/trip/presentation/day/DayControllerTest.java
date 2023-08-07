package com.cosain.trilo.unit.trip.presentation.day;

import com.cosain.trilo.support.RestControllerTest;
import com.cosain.trilo.trip.application.day.DayCommandService;
import com.cosain.trilo.trip.application.day.DayQueryService;
import com.cosain.trilo.trip.application.day.dto.DayColorUpdateCommand;
import com.cosain.trilo.trip.application.day.dto.DayScheduleDetail;
import com.cosain.trilo.trip.application.day.dto.ScheduleSummary;
import com.cosain.trilo.trip.domain.vo.DayColor;
import com.cosain.trilo.trip.presentation.DayController;
import com.cosain.trilo.trip.presentation.request.day.DayColorUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DayController.class)
public class DayControllerTest extends RestControllerTest {

    @MockBean
    private DayCommandService dayCommandService;

    @MockBean
    private DayQueryService dayQueryService;

    private final static String ACCESS_TOKEN = "Bearer accessToken";

    @Nested
    class 색상_수정{
        @Test
        @DisplayName("인증된 사용자의 DayColor 수정 요청 -> 성공")
        public void successTest() throws Exception {
            long requestTripperId = 2L;
            mockingForLoginUserAnnotation(requestTripperId);

            // given
            Long dayId = 1L;
            String rawColorName = "RED";
            DayColorUpdateRequest request = new DayColorUpdateRequest(rawColorName);

            var command = DayColorUpdateCommand.of(dayId, requestTripperId, rawColorName);
            willDoNothing()
                    .given(dayCommandService)
                    .updateDayColor(eq(command));


            // when
            ResultActions resultActions = mockMvc.perform(put("/api/days/{dayId}/color", dayId)
                    .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                    .content(createJson(request))
                    .characterEncoding(StandardCharsets.UTF_8)
                    .contentType(MediaType.APPLICATION_JSON)
            );

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dayId").value(dayId));

            verify(dayCommandService, times(1)).updateDayColor(eq(command));
        }

        @Test
        @DisplayName("미인증 사용자 요청 -> 인증 실패 401")
        public void updateDayColor_with_unauthorizedUser() throws Exception {
            Long dayId = 1L;
            String rawColorName = "RED";
            DayColorUpdateRequest request = new DayColorUpdateRequest(rawColorName);

            mockMvc.perform(put("/api/days/{dayId}/color", dayId)
                            .content(createJson(request))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("auth-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(dayCommandService, times(0)).updateDayColor(any(DayColorUpdateCommand.class));
        }

        @Test
        @DisplayName("비어있는 바디 -> 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외")
        public void updateDayColor_with_emptyContent() throws Exception {
            mockingForLoginUserAnnotation();

            // given
            Long dayId = 1L;
            String content = "";

            mockMvc.perform(put("/api/days/{dayId}/color", dayId)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .content(content)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(dayCommandService, times(0)).updateDayColor(any(DayColorUpdateCommand.class));
        }

        @Test
        @DisplayName("형식이 올바르지 않은 바디 -> 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외")
        public void createTrip_with_invalidContent() throws Exception {
            mockingForLoginUserAnnotation();

            Long dayId = 1L;
            String content = """
                {
                    "colorName": 따옴표 안 감싼 색상명
                }
                """;

            mockMvc.perform(put("/api/days/{dayId}/color", dayId)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .content(content)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(dayCommandService, times(0)).updateDayColor(any(DayColorUpdateCommand.class));
        }


        @Test
        @DisplayName("DayId로 숫자가 아닌 문자열 주입 -> 올바르지 않은 경로 변수 타입 400 에러")
        public void updateDayColor_with_notNumberDayId() throws Exception {
            mockingForLoginUserAnnotation();

            String notNumberDayId = "가가가";
            DayColorUpdateRequest request = new DayColorUpdateRequest("RED");

            mockMvc.perform(put("/api/days/{dayId}/color", notNumberDayId)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .content(createJson(request))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0004"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(dayCommandService, times(0)).updateDayColor(any(DayColorUpdateCommand.class));
        }
    }

    @Nested
    class Day_조회{
        @Test
        @DisplayName("인증된 사용자 요청 -> Day 단건 조회")
        public void findSingleSchedule_with_authorizedUser() throws Exception {

            mockingForLoginUserAnnotation();
            ScheduleSummary scheduleSummary1 = new ScheduleSummary(1L, "제목", "장소 이름","장소 식별자1", 33.33, 33.33);
            ScheduleSummary scheduleSummary2 = new ScheduleSummary(2L, "제목2", "장소 이름2","장소 식별자2", 33.33, 33.33);

            DayScheduleDetail dayScheduleDetail = new DayScheduleDetail(1L, 1L, LocalDate.of(2023, 2, 3), DayColor.RED, List.of(scheduleSummary1, scheduleSummary2));
            given(dayQueryService.searchDaySchedule(eq(1L))).willReturn(dayScheduleDetail);

            mockMvc.perform(get("/api/days/1")
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dayId").value(dayScheduleDetail.getDayId()))
                    .andExpect(jsonPath("$.date").value(dayScheduleDetail.getDate().toString()))
                    .andExpect(jsonPath("$.tripId").value(dayScheduleDetail.getTripId()))
                    .andExpect(jsonPath("$.schedules").isArray())
                    .andExpect(jsonPath("$.schedules[0].coordinate.latitude").value(scheduleSummary1.getCoordinate().getLatitude()))
                    .andExpect(jsonPath("$.schedules[0].coordinate.longitude").value(scheduleSummary1.getCoordinate().getLongitude()))
                    .andExpect(jsonPath("$.schedules[0].scheduleId").value(scheduleSummary1.getScheduleId()))
                    .andExpect(jsonPath("$.schedules[0].title").value(scheduleSummary1.getTitle()))
                    .andExpect(jsonPath("$.schedules[0].placeName").value(scheduleSummary1.getPlaceName()))
                    .andExpect(jsonPath("$.schedules[0].placeId").value(scheduleSummary1.getPlaceId()))
                    .andExpect(jsonPath("$.schedules[1].coordinate.latitude").value(scheduleSummary2.getCoordinate().getLatitude()))
                    .andExpect(jsonPath("$.schedules[1].coordinate.longitude").value(scheduleSummary2.getCoordinate().getLongitude()))
                    .andExpect(jsonPath("$.schedules[1].scheduleId").value(scheduleSummary2.getScheduleId()))
                    .andExpect(jsonPath("$.schedules[1].title").value(scheduleSummary2.getTitle()))
                    .andExpect(jsonPath("$.schedules[1].placeName").value(scheduleSummary2.getPlaceName()))
                    .andExpect(jsonPath("$.schedules[1].placeId").value(scheduleSummary2.getPlaceId()));
        }

        @Test
        @DisplayName("미인증 사용자 요청 -> 200")
        public void findSingleSchedule_with_unauthorizedUser() throws Exception {
            mockMvc.perform(get("/api/days/1"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class Day_목록_조회{
        @Test
        @DisplayName("Day 목록 조회 -> 성공")
        public void findTripDayList_with_authorizedUser() throws Exception {

            Long tripId = 1L;
            mockingForLoginUserAnnotation();
            ScheduleSummary scheduleSummary = new ScheduleSummary(1L, "제목", "장소 이름", "장소 식별자", 33.33, 33.33);
            DayScheduleDetail dayScheduleDetail = new DayScheduleDetail(1L, 1L, LocalDate.of(2023, 5, 13), DayColor.BLACK, List.of(scheduleSummary));
            List<DayScheduleDetail> dayScheduleDetails = List.of(dayScheduleDetail);

            given(dayQueryService.searchDaySchedules(tripId)).willReturn(dayScheduleDetails);

            mockMvc.perform(get("/api/trips/{tripId}/days", tripId)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.days").isArray())
                    .andExpect(jsonPath("$.days[0].dayId").isNumber())
                    .andExpect(jsonPath("$.days[0].tripId").isNumber())
                    .andExpect(jsonPath("$.days[0].date").isString())
                    .andExpect(jsonPath("$.days[0].schedules").isArray())
                    .andExpect(jsonPath("$.days[0].schedules[0].scheduleId").isNumber())
                    .andExpect(jsonPath("$.days[0].schedules[0].title").isString())
                    .andExpect(jsonPath("$.days[0].schedules[0].placeName").isString())
                    .andExpect(jsonPath("$.days[0].schedules[0].placeId").isString())
                    .andExpect(jsonPath("$.days[0].schedules[0].coordinate.latitude").isNumber())
                    .andExpect(jsonPath("$.days[0].schedules[0].coordinate.longitude").isNumber());

        }

        @Test
        @DisplayName("미인증 사용자 요청 -> 200")
        public void findTripDayList_with_unauthorizedUser() throws Exception {
            mockMvc.perform(get("/api/trips/1/days"))
                    .andExpect(status().isOk());
        }
    }

}
