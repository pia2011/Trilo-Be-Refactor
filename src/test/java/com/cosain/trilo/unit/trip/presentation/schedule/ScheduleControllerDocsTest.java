package com.cosain.trilo.unit.trip.presentation.schedule;

import com.cosain.trilo.support.RestDocsTestSupport;
import com.cosain.trilo.trip.application.schedule.ScheduleCommandService;
import com.cosain.trilo.trip.application.schedule.ScheduleQueryService;
import com.cosain.trilo.trip.application.schedule.dto.*;
import com.cosain.trilo.trip.presentation.schedule.ScheduleController;
import com.cosain.trilo.trip.presentation.schedule.dto.request.ScheduleMoveRequest;
import com.cosain.trilo.trip.presentation.schedule.dto.request.ScheduleUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScheduleController.class)
public class ScheduleControllerDocsTest extends RestDocsTestSupport {

    @MockBean
    private ScheduleCommandService scheduleCommandService;

    @MockBean
    private ScheduleQueryService scheduleQueryService;

    private final String ACCESS_TOKEN = "Bearer accessToken";

    @Nested
    class 일정_생성_테스트{
        @Test
        @DisplayName("인증된 사용자의 일정 생성 요청 -> 성공")
        void scheduleCreateDocTest() throws Exception {
            long requestTripperId = 1L;
            mockingForLoginUserAnnotation(requestTripperId);
            Long tripId = 2L;
            Long dayId = 3L;
            String rawScheduleTitle = "일정 제목";
            String placeId = "place-id";
            String placeName = "place-Name";
            Double latitude = 37.564213;
            Double longitude = 127.001698;

            String requestJson = String.format("""
                {
                    "dayId": %d,
                    "tripId": %d,
                    "title": "%s",
                    "placeId": "%s",
                    "placeName": "%s",
                    "coordinate": {
                        "latitude": %f,
                        "longitude": %f
                    }
                }
                """, dayId, tripId, rawScheduleTitle, placeId, placeName, latitude, longitude);

            var command = ScheduleCreateCommand.of(requestTripperId, tripId, dayId, rawScheduleTitle, placeId, placeName, latitude, longitude);

            Long createdScheduleId = 3L;
            given(scheduleCommandService.createSchedule(eq(command))).willReturn(createdScheduleId);


            mockMvc.perform(post("/api/schedules")
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .content(requestJson)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.scheduleId").value(createdScheduleId))
                    .andDo(restDocs.document(
                            requestHeaders(
                                    headerWithName(HttpHeaders.AUTHORIZATION)
                                            .description("Bearer 타입 AccessToken")
                            ),
                            requestFields(
                                    fieldWithPath("dayId")
                                            .type(NUMBER)
                                            .optional()
                                            .description("day의 식별자. null 일 경우 임시보관함으로 간주"),
                                    fieldWithPath("title")
                                            .type(STRING)
                                            .description("일정의 제목")
                                            .attributes(key("constraints").value("null일 수 없으며, 길이는 20자 이하까지 허용됩니다. (공백, 빈 문자열 허용)")),
                                    fieldWithPath("tripId")
                                            .type(NUMBER)
                                            .description("소속된 여행(Trip)의 식별자")
                                            .attributes(key("constraints").value("null 이 허용되지 않습니다.")),
                                    fieldWithPath("placeId")
                                            .type(STRING)
                                            .optional()
                                            .description("장소의 google map api 기준 식별자"),
                                    fieldWithPath("placeName")
                                            .type(STRING)
                                            .optional()
                                            .description("장소명"),
                                    fieldWithPath("coordinate")
                                            .type("Coordinate")
                                            .description("장소의 좌표. 하위 표를 참고하세요.")
                                            .attributes(key("constraints").value("null 이 허용되지 않습니다.")),
                                    fieldWithPath("coordinate.latitude").ignored(),
                                    fieldWithPath("coordinate.longitude").ignored()
                            ),
                            responseFields(
                                    fieldWithPath("scheduleId")
                                            .type(NUMBER)
                                            .description("생성된 일정 식별자(id)")
                            )
                    ))
                    .andDo(restDocs.document(
                            requestFields(beneathPath("coordinate"),
                                    fieldWithPath("latitude")
                                            .type(NUMBER)
                                            .description("위도")
                                            .attributes(key("constraints").value("null이여선 안 되고 -90 이상 90 이하까지만 허용")),
                                    fieldWithPath("longitude")
                                            .type(NUMBER)
                                            .description("경도")
                                            .attributes(key("constraints").value("null이여선 안 되고 -180 이상 180 이하까지만 허용"))
                            )
                    ));

            verify(scheduleCommandService, times(1)).createSchedule(eq(command));
        }
    }

    @Nested
    class 여행_삭제_테스트{
        @Test
        @DisplayName("인증된 사용자의 일정 삭제 요청 -> 성공")
        void scheduleDeleteDocTest() throws Exception {
            mockingForLoginUserAnnotation();

            // given
            Long scheduleId = 1L;
            willDoNothing().given(scheduleCommandService).deleteSchedule(eq(scheduleId), any());


            // when & then
            mockMvc.perform(delete("/api/schedules/{scheduleId}", scheduleId)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andExpect(status().isNoContent())
                    .andExpect(jsonPath("$").doesNotExist())
                    .andDo(print())
                    .andDo(restDocs.document(
                            requestHeaders(
                                    headerWithName(HttpHeaders.AUTHORIZATION)
                                            .description("Bearer 타입 AccessToken")
                            ),
                            pathParameters(
                                    parameterWithName("scheduleId")
                                            .description("삭제할 여행 식별자(id)")
                            )
                    ));

            verify(scheduleCommandService).deleteSchedule(eq(scheduleId), any());
        }
    }

    @Nested
    class 여행_이동_테스트{
        @Test
        @DisplayName("인증된 사용자의 일정 이동 요청 -> 성공")
        void scheduleMoveDocTest() throws Exception {
            // given
            long requestTripperId = 1L;
            mockingForLoginUserAnnotation(requestTripperId);

            Long scheduleId = 1L;
            Long targetDayId = 2L;
            int targetOrder = 3;

            var request = new ScheduleMoveRequest(targetDayId, targetOrder);
            var command = ScheduleMoveCommand.of(scheduleId, requestTripperId, targetDayId, targetOrder);
            ScheduleMoveResult moveResult = ScheduleMoveResult.builder()
                    .scheduleId(scheduleId)
                    .beforeDayId(1L)
                    .afterDayId(targetDayId)
                    .positionChanged(true)
                    .build();

            given(scheduleCommandService.moveSchedule(eq(command))).willReturn(moveResult);

            // when
            ResultActions resultActions = runTest(scheduleId, createJson(request));

            // then

            // 응답 메시지 검증
            resultActions
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scheduleId").value(scheduleId))
                    .andExpect(jsonPath("$.beforeDayId").value(moveResult.getBeforeDayId()))
                    .andExpect(jsonPath("$.afterDayId").value(moveResult.getAfterDayId()))
                    .andExpect(jsonPath("$.positionChanged").value(moveResult.isPositionChanged()));

            // 내부 의존성 호출 검증
            verify(scheduleCommandService, times(1)).moveSchedule(eq(command));

            // 문서화
            resultActions
                    .andDo(restDocs.document(
                            // 요청 헤더 문서화
                            requestHeaders(
                                    headerWithName(HttpHeaders.AUTHORIZATION)
                                            .description("Bearer 타입 AccessToken")
                            ),
                            // 요청 경로변수 문서화
                            pathParameters(
                                    parameterWithName("scheduleId")
                                            .description("이동할 여행 식별자(id)")
                            ),
                            // 요청 필드 문서화
                            requestFields(
                                    fieldWithPath("targetDayId")
                                            .type(NUMBER)
                                            .description("일정의 옮겨질 위치 Day 식별자(null일 경우 임시보관함)")
                                            .optional(),
                                    fieldWithPath("targetOrder")
                                            .type(NUMBER)
                                            .description("일정을 삽입할 위치(0,1,2,3, ...). 위의 보충 설명 참고")
                                            .attributes(key("constraints").value("null일 수 없고, 0 이상이여야 함. 해당 Day 상에서의 가능한 순서 범위를 벗어나선 안 됨"))
                            ),
                            // 응답 필드 문서화
                            responseFields(
                                    fieldWithPath("scheduleId")
                                            .type(NUMBER)
                                            .description("일정의 식별자(id)"),
                                    fieldWithPath("beforeDayId")
                                            .type(NUMBER)
                                            .description("일정이 이동하기 전의 Day 식별자(id)"),
                                    fieldWithPath("afterDayId")
                                            .type(NUMBER)
                                            .description("일정이 이동한 후의 Day 식별자(id)"),
                                    fieldWithPath("positionChanged")
                                            .type(BOOLEAN)
                                            .description("일정의 위치(Day, 상대적 순서) 변경 여부. 일정의 위치가 변경됐을 경우 true, 제자리 그대로일 경우 false")
                            )
                    ));
        }

        /**
         * 인증된 사용자의 요청을 mocking하여 수행하고, 그 결과를 객체로 얻어옵니다.
         * @param scheduleId 일정 식별자(id)
         * @param content 요청 본문(body)
         * @return 실제 요청 실행 결과
         */
        private ResultActions runTest(Object scheduleId, String content) throws Exception {
            return mockMvc.perform(put("/api/schedules/{scheduleId}/position", scheduleId)
                    .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                    .content(content)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .contentType(MediaType.APPLICATION_JSON)
            );
        }
    }

    @Nested
    class 여행_수정_테스트{
        @Test
        @DisplayName("인증된 사용자의 일정 수정 요청 -> 성공")
        void scheduleUpdateDocTest() throws Exception {
            // given
            long requestTripperId = 2L;
            mockingForLoginUserAnnotation(requestTripperId);

            Long scheduleId = 1L;
            String rawScheduleTitle = "수정 일정제목";
            String rawScheduleContent = "수정 일정내용";
            LocalTime startTime = LocalTime.of(13, 0);
            LocalTime endTime = LocalTime.of(13, 5);

            var request = new ScheduleUpdateRequest(rawScheduleTitle, rawScheduleContent, startTime, endTime);
            var command = ScheduleUpdateCommand.of(scheduleId, requestTripperId, rawScheduleTitle, rawScheduleContent, startTime, endTime);

            // when
            ResultActions resultActions = runTest(scheduleId, request);

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scheduleId").value(1L))
                    .andDo(print())
                    .andDo(restDocs.document(
                            requestHeaders(
                                    headerWithName(HttpHeaders.AUTHORIZATION)
                                            .description("Bearer 타입 AccessToken")
                            ),
                            pathParameters(
                                    parameterWithName("scheduleId")
                                            .description("수정 여행 식별자(id)")
                            ),
                            requestFields(
                                    fieldWithPath("title")
                                            .type(STRING)
                                            .description("일정의 제목")
                                            .attributes(key("constraints").value("null 일 수 없으며, 길이는 35자 이하까지 허용됩니다. (공백, 빈 문자열 허용)")),
                                    fieldWithPath("content")
                                            .type(STRING)
                                            .description("일정의 본문")
                                            .attributes(key("constraints").value("null을 허용하지 않으며 최대 65535 바이트까지 허용합니다. (공백, 빈문자열 허용)")),
                                    fieldWithPath("startTime")
                                            .type(STRING)
                                            .description("일정의 시작시간. 필수."),
                                    fieldWithPath("endTime")
                                            .type(STRING)
                                            .description("일정의 종료시간. 필수.")
                            ),
                            responseFields(
                                    fieldWithPath("scheduleId")
                                            .type(NUMBER)
                                            .description("일정의 식별자(id)")

                            )
                    ));

            verify(scheduleCommandService, times(1)).updateSchedule(eq(command));
        }

        private ResultActions runTest(Long scheduleId, ScheduleUpdateRequest request) throws Exception {
            return mockMvc.perform(put("/api/schedules/{scheduleId}", scheduleId)
                    .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                    .content(createJson(request))
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8));
        }
    }

    @Nested
    class 일정_조회_테스트{
        @Test
        void 일정_단건_조회() throws Exception{

            Long scheduleId = 1L;
            mockingForLoginUserAnnotation();
            ScheduleDetail scheduleDetail = new ScheduleDetail(scheduleId, 1L, "제목", "장소 이름", 23.23, 23.23, 1L, "내용", LocalTime.of(15, 0), LocalTime.of(15, 30));
            given(scheduleQueryService.searchScheduleDetail(anyLong())).willReturn(scheduleDetail);

            mockMvc.perform(RestDocumentationRequestBuilders.get( "/api/schedules/{scheduleId}", scheduleId)
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
                                    parameterWithName("scheduleId").description("조회할 일정 ID")
                            ),
                            responseFields(
                                    fieldWithPath("scheduleId").type(NUMBER).description("일정 ID"),
                                    fieldWithPath("dayId").type(NUMBER).description("Day ID"),
                                    fieldWithPath("title").type(STRING).description("일정 제목"),
                                    fieldWithPath("placeName").type(STRING).description("장소 이름"),
                                    fieldWithPath("order").type(NUMBER).description("일정 순서"),
                                    fieldWithPath("content").type(STRING).description("일정 내용"),
                                    fieldWithPath("coordinate").type("Coordinate").description("장소의 좌표 ( 하단 표 참고 )"),
                                    fieldWithPath("coordinate.latitude").ignored(),
                                    fieldWithPath("coordinate.longitude").ignored(),
                                    fieldWithPath("scheduleTime").type("ScheduleTime").description("일정 시간 계획 ( 하단 표 참고 )"),
                                    fieldWithPath("scheduleTime.startTime").ignored(),
                                    fieldWithPath("scheduleTime.endTime").ignored()
                            ),
                            responseFields(beneathPath("coordinate"),
                                    fieldWithPath("latitude").type(NUMBER).description("위도"),
                                    fieldWithPath("longitude").type(NUMBER).description("경도")
                            )
                            ,responseFields(beneathPath("scheduleTime"),
                                    fieldWithPath("startTime").type(STRING).description("일정 시작 시간"),
                                    fieldWithPath("endTime").type(STRING).description("일정 종료 시간")
                            )

                    ));

        }
    }
}
