package com.cosain.trilo.unit.trip.presentation.day;

import com.cosain.trilo.support.RestDocsTestSupport;
import com.cosain.trilo.trip.application.day.DayCommandService;
import com.cosain.trilo.trip.application.day.DayQueryService;
import com.cosain.trilo.trip.application.day.dto.DayColorUpdateCommand;
import com.cosain.trilo.trip.application.day.dto.DayScheduleDetail;
import com.cosain.trilo.trip.application.day.dto.ScheduleSummary;
import com.cosain.trilo.trip.domain.vo.DayColor;
import com.cosain.trilo.trip.presentation.DayController;
import com.cosain.trilo.trip.presentation.request.day.DayColorUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DayController.class)
public class DayControllerDocsTest extends RestDocsTestSupport {

    @MockBean
    private DayQueryService dayQueryService;

    @MockBean
    private DayCommandService dayCommandService;
    private final static String ACCESS_TOKEN = "Bearer accessToken";

    @Test
    @DisplayName("인증된 사용자의 DayColor 수정 요청 -> 성공")
    public void dayColorUpdateDocsTest() throws Exception {
        // given
        Long dayId = 1L;
        long requestTripperId = 2L;
        mockingForLoginUserAnnotation(requestTripperId);

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
                .andExpect(jsonPath("$.dayId").value(dayId))
                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION)
                                        .description("Bearer 타입 AccessToken")
                        ),
                        pathParameters(
                                parameterWithName("dayId")
                                        .description("Day의 식별자(id)")
                        ),
                        requestFields(
                                fieldWithPath("colorName")
                                        .type(STRING)
                                        .description("변경할 색상 이름")
                                        .attributes(key("constraints").value("null일 수 없으며, 아래의 설명을 참고하여 가능한 색상 이름을 전달해주세요. (대소문자 구분 없음)"))
                        ),
                        responseFields(
                                fieldWithPath("dayId")
                                        .type(NUMBER)
                                        .description("Day의 식별자(id)")
                        )
                ));

        verify(dayCommandService, times(1)).updateDayColor(eq(command));
    }

    @Test
    void Day_단건_조회() throws Exception{

        mockingForLoginUserAnnotation();
        ScheduleSummary scheduleSummary1 = new ScheduleSummary(1L, "제목", "장소 이름","장소 식별자1", 33.33, 33.33);
        ScheduleSummary scheduleSummary2 = new ScheduleSummary(2L, "제목2", "장소 이름2","장소 식별자2", 33.33, 33.33);

        Long dayId = 1L;
        DayScheduleDetail dayScheduleDetail = new DayScheduleDetail(dayId, 1L, LocalDate.of(2023, 2, 3), DayColor.BLACK, List.of(scheduleSummary1, scheduleSummary2));
        given(dayQueryService.searchDaySchedule(eq(dayId))).willReturn(dayScheduleDetail);

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/days"+"/{dayId}",dayId)
                        .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION)
                                        .description("Bearer 타입 AccessToken")
                        ),
                        pathParameters(
                                parameterWithName("dayId").description("조회할 Day ID")
                        ),
                        responseFields(
                                fieldWithPath("dayId").type(NUMBER).description("Day ID"),
                                fieldWithPath("tripId").type(NUMBER).description("여행 ID"),
                                fieldWithPath("date").type(STRING).description("여행 날짜"),
                                fieldWithPath("dayColor").type("DayColor").description("색상 정보 (하단 표 참고)"),
                                fieldWithPath("dayColor.name").ignored(),
                                fieldWithPath("dayColor.code").ignored(),
                                subsectionWithPath("schedules").type(ARRAY).description("일정 목록 (하단 표 참고)")
                        ),
                        responseFields(beneathPath("dayColor"),
                                fieldWithPath("name").type(STRING).description("색상 이름"),
                                fieldWithPath("code").type(STRING).description("색상 코드")
                        ),
                        responseFields(beneathPath("schedules").withSubsectionId("schedules"),
                                fieldWithPath("scheduleId").type(NUMBER).description("일정 ID"),
                                fieldWithPath("title").type(STRING).description("일정 제목"),
                                fieldWithPath("placeName").type(STRING).description("장소 이름"),
                                fieldWithPath("placeId").type(STRING).description("장소 ID"),
                                subsectionWithPath("coordinate").type(OBJECT).description("장소의 좌표 (하단 표 참고)")
                        ),
                        responseFields(beneathPath("schedules[].coordinate").withSubsectionId("coordinate"),
                                fieldWithPath("latitude").type(NUMBER).description("위도"),
                                fieldWithPath("longitude").type(NUMBER).description("경도")
                        )
                ));
    }

    @Test
    void Day_목록_조회() throws Exception {
        Long tripId = 1L;
        mockingForLoginUserAnnotation();
        ScheduleSummary scheduleSummary = new ScheduleSummary(1L, "제목", "장소 이름", "장소 식별자", 33.33, 33.33);
        DayScheduleDetail dayScheduleDetail = new DayScheduleDetail(1L, 1L, LocalDate.of(2023, 5, 13), DayColor.BLACK, List.of(scheduleSummary));
        List<DayScheduleDetail> dayScheduleDetails = List.of(dayScheduleDetail);

        given(dayQueryService.searchDaySchedules(tripId)).willReturn(dayScheduleDetails);

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/trips/{tripId}/days", tripId)
                        .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION)
                                        .description("Bearer 타입 AccessToken")
                        ),
                        pathParameters(
                                parameterWithName("tripId").description("조회할 Trip ID")
                        ),
                        responseFields(
                                subsectionWithPath("days").type(ARRAY).description("Day 목록")
                        ),
                        responseFields(beneathPath("days").withSubsectionId("days"),
                                fieldWithPath("dayId").type(NUMBER).description("Day ID"),
                                fieldWithPath("tripId").type(NUMBER).description("여행 ID"),
                                fieldWithPath("date").description(STRING).description("여행 날짜"),
                                subsectionWithPath("dayColor").type("DayColor").description("색상 정보 (하단 표 참고)"),
                                subsectionWithPath("schedules").type("Schedule[]").description("일정 목록 (하단 표 참고)")
                        ),
                        responseFields(beneathPath("days[].dayColor").withSubsectionId("dayColor"),
                                fieldWithPath("name").type(STRING).description("색상 이름"),
                                fieldWithPath("code").type(STRING).description("색상 코드")
                        ),
                        responseFields(beneathPath("days[].schedules").withSubsectionId("schedules"),
                                fieldWithPath("[].scheduleId").type(NUMBER).description("일정 ID"),
                                fieldWithPath("[].title").type(STRING).description("일정 제목"),
                                fieldWithPath("[].placeName").type(STRING).description("장소 이름"),
                                fieldWithPath("[].placeId").type(STRING).description("장소 ID"),
                                subsectionWithPath("[].coordinate").type("Coordinate").description("장소의 좌표 (하단 표 참고)")
                        ),
                        responseFields(beneathPath("days[].schedules[].coordinate").withSubsectionId("coordinate"),
                                fieldWithPath("latitude").type(NUMBER).description("위도"),
                                fieldWithPath("longitude").type(NUMBER).description("경도")
                        )
                ));

    }
}
