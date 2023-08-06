package com.cosain.trilo.unit.trip.presentation.schedule;

import com.cosain.trilo.support.RestControllerTest;
import com.cosain.trilo.trip.application.schedule.ScheduleCommandService;
import com.cosain.trilo.trip.application.schedule.ScheduleQueryService;
import com.cosain.trilo.trip.application.schedule.dto.*;
import com.cosain.trilo.trip.presentation.schedule.ScheduleController;
import com.cosain.trilo.trip.presentation.schedule.dto.request.ScheduleCreateRequest;
import com.cosain.trilo.trip.presentation.schedule.dto.request.ScheduleMoveRequest;
import com.cosain.trilo.trip.presentation.schedule.dto.request.ScheduleUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScheduleController.class)
public class ScheduleControllerTest extends RestControllerTest {

    @MockBean
    private ScheduleCommandService scheduleCommandService;

    @MockBean
    private ScheduleQueryService scheduleQueryService;

    private final String ACCESS_TOKEN = "Bearer accessToken";

    @Nested
    class 일정_생성_테스트{
        @Test
        @DisplayName("인증된 사용자의 올바른 요청 -> 일정 생성됨")
        public void createSchedule_with_authorizedUser() throws Exception {

            Long tripId = 1L;
            long requestTripperId = 2L;
            mockingForLoginUserAnnotation(requestTripperId);
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
                    .andExpect(jsonPath("$.scheduleId").value(createdScheduleId));

            verify(scheduleCommandService, times(1)).createSchedule(eq(command));
        }

        @Test
        @DisplayName("미인증 사용자 요청 -> 인증 실패 401")
        public void createSchedule_with_unauthorizedUser() throws Exception {
            Long tripId = 1L;
            Long dayId = 2L;
            String rawScheduleTitle = "일정 제목";
            String placeId = "place-id";
            String placeName = "place-Name";
            Double latitude = 37.5642;
            Double longitude = 127.0016;

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


            mockMvc.perform(post("/api/schedules")
                            .content(requestJson)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").exists())
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(scheduleCommandService, times(0)).createSchedule(any(ScheduleCreateCommand.class));
        }


        @Test
        @DisplayName("비어있는 바디 -> 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외")
        public void createSchedule_with_emptyContent() throws Exception {
            long requestTripperId = 2L;
            mockingForLoginUserAnnotation(requestTripperId);

            String emptyContent = "";

            mockMvc.perform(post("/api/schedules")
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .content(emptyContent)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(scheduleCommandService, times(0)).createSchedule(any(ScheduleCreateCommand.class));
        }

        @Test
        @DisplayName("형식이 올바르지 않은 바디 -> 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외")
        public void createSchedule_with_invalidContent() throws Exception {
            long requestTripperId = 2L;
            mockingForLoginUserAnnotation(requestTripperId);
            String invalidContent = """
                {
                    "dayId"+ 1,
                    "tripId": 2,
                    "title": 괄호로 감싸지지 않은 문자열,
                    "placeId": "place-5964",
                    "placeName": "장소명",
                    "coordinate": {
                        "latitude": 34.124,
                        "longitude": 123.124
                    }
                }
                """;

            mockMvc.perform(post("/api/schedules")
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .content(invalidContent)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(scheduleCommandService, times(0)).createSchedule(any(ScheduleCreateCommand.class));
        }

        @Test
        @DisplayName("타입이 올바르지 않은 요청 데이터 -> 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외")
        public void createSchedule_with_invalidType() throws Exception {
            long requestTripperId = 2L;
            mockingForLoginUserAnnotation(requestTripperId);
            String invalidTypeContent = """
                {
                    "dayId": 1,
                    "tripId": 7,
                    "title": "제목",
                    "placeId": "place-5964",
                    "placeName": "장소명",
                    "coordinate": {
                        "latitude": "숫자가 아닌 위도값",
                        "longitude": "숫자가 아닌 경도값"
                    }
                }
                """;
            mockMvc.perform(post("/api/schedules")
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .content(invalidTypeContent)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(scheduleCommandService, times(0)).createSchedule(any(ScheduleCreateCommand.class));
        }

        @Test
        @DisplayName("좌표 누락 데이터 -> 입력 검증 실패 400 예외")
        public void createSchedule_with_nullCoordinate() throws Exception {
            mockingForLoginUserAnnotation();

            Long dayId = 1L;
            Long tripId = 1L;
            String rawScheduleTitle = "일정 제목";
            String placeId = "place-id";
            String placeName = "place-Name";

            String requestJson = String.format("""
                {
                    "dayId": %d,
                    "tripId": %d,
                    "title": "%s",
                    "placeId": "%s",
                    "placeName": "%s"
                }
                """, dayId, tripId, rawScheduleTitle, placeId, placeName); // 좌표 누락됨

            mockMvc.perform(post("/api/schedules")
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .content(requestJson)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0003"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists())
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].errorCode").value("place-0002"))
                    .andExpect(jsonPath("$.errors[0].errorMessage").exists())
                    .andExpect(jsonPath("$.errors[0].errorDetail").exists());

            verify(scheduleCommandService, times(0)).createSchedule(any(ScheduleCreateCommand.class));
        }

        @Test
        @DisplayName("tripId 누락 데이터 -> 입력 검증 실패 400 예외")
        public void createSchedule_with_nullTripId() throws Exception {
            long tripperId = 1L;
            mockingForLoginUserAnnotation(tripperId);

            Long dayId = 2L;
            Long tripId = null;
            String rawScheduleTitle = "일정 제목";
            String placeId = "place-id";
            String placeName = "place-Name";
            Double latitude = 37.5642;
            Double longitude = 127.0016;

            ScheduleCreateRequest request = ScheduleCreateRequest.builder()
                    .tripId(tripId)
                    .dayId(dayId)
                    .title(rawScheduleTitle)
                    .placeId(placeId)
                    .placeName(placeName)
                    .coordinate(new ScheduleCreateRequest.CoordinateDto(latitude, longitude))
                    .build();

            mockMvc.perform(post("/api/schedules")
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .content(createJson(request))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0003"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists())
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].errorCode").value("schedule-0007"))
                    .andExpect(jsonPath("$.errors[0].errorMessage").exists())
                    .andExpect(jsonPath("$.errors[0].errorDetail").exists());

            verify(scheduleCommandService, times(0)).createSchedule(any(ScheduleCreateCommand.class));
        }

        @Test
        @DisplayName("tripId, 좌표 누락 데이터 -> 입력 검증 실패 400 예외")
        public void createSchedule_with_nullTripId_and_nullCoordinate() throws Exception {
            long tripperId = 1L;
            mockingForLoginUserAnnotation(tripperId);

            Long dayId = 2L;
            Long tripId = null;
            String rawScheduleTitle = "일정 제목";
            String placeId = "place-id";
            String placeName = "place-Name";

            ScheduleCreateRequest request = ScheduleCreateRequest.builder()
                    .tripId(tripId)
                    .dayId(dayId)
                    .title(rawScheduleTitle)
                    .placeId(placeId)
                    .placeName(placeName)
                    .coordinate(null)
                    .build();

            mockMvc.perform(post("/api/schedules")
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .content(createJson(request))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0003"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists())
                    .andExpect(jsonPath("$.errors", hasSize(2)))
                    .andExpect(jsonPath("$.errors[*].errorCode", hasItem("schedule-0007")))
                    .andExpect(jsonPath("$.errors[0].errorMessage").exists())
                    .andExpect(jsonPath("$.errors[0].errorDetail").exists())
                    .andExpect(jsonPath("$.errors[0].field").exists())
                    .andExpect(jsonPath("$.errors[*].errorCode", hasItem("place-0002")))
                    .andExpect(jsonPath("$.errors[1].errorMessage").exists())
                    .andExpect(jsonPath("$.errors[1].errorDetail").exists())
                    .andExpect(jsonPath("$.errors[1].field").exists());

            verify(scheduleCommandService, times(0)).createSchedule(any(ScheduleCreateCommand.class));
        }
    }

    @Nested
    class 일정_삭제_테스트{
        @Test
        @DisplayName("인증된 사용자 요청 -> 성공")
        public void deleteSchedule_with_authorizedUser() throws Exception {
            mockingForLoginUserAnnotation();
            willDoNothing().given(scheduleCommandService).deleteSchedule(eq(1L), any());

            mockMvc.perform(delete("/api/schedules/1")
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isNoContent())
                    .andExpect(jsonPath("$").doesNotExist());
            verify(scheduleCommandService).deleteSchedule(eq(1L), any());
        }

        @Test
        @DisplayName("미인증 사용자 요청 -> 인증 실패 401")
        public void deleteSchedule_with_unauthorizedUser() throws Exception {
            mockMvc.perform(delete("/api/schedules/1")
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").exists())
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());
        }
    }

    @Nested
    class 일정_이동_테스트{
        @Test
        @DisplayName("인증된 사용자의 올바른 요청 -> 일정 이동됨")
        public void moveSchedule_with_authorizedUser() throws Exception {
            long requestTripperId = 1L;
            mockingForLoginUserAnnotation(requestTripperId);
            Long scheduleId = 1L;
            Long targetDayId = 2L;
            int targetOrder = 3;

            var moveResult = ScheduleMoveResult.builder()
                    .scheduleId(scheduleId)
                    .beforeDayId(1L)
                    .afterDayId(targetDayId)
                    .positionChanged(true)
                    .build();

            var request = new ScheduleMoveRequest(targetDayId, targetOrder);
            var command = ScheduleMoveCommand.of(scheduleId, requestTripperId, targetDayId, targetOrder);

            given(scheduleCommandService.moveSchedule(eq(command)))
                    .willReturn(moveResult);

            // when
            ResultActions resultActions = runTest(scheduleId, createJson(request)); // 정상적으로 사용자가 일정 이동 요청했을 때

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scheduleId").value(scheduleId))
                    .andExpect(jsonPath("$.beforeDayId").value(moveResult.getBeforeDayId()))
                    .andExpect(jsonPath("$.afterDayId").value(moveResult.getAfterDayId()))
                    .andExpect(jsonPath("$.positionChanged").value(moveResult.isPositionChanged())); // 상태코드 및 응답 필드 검증

            verify(scheduleCommandService, times(1)).moveSchedule(eq(command)); // 내부 의존성 호출 검증
        }

        /**
         * <p>Authorization Header에 토큰을 담지 않은 사용자가 요청하면 인증 실패 오류가 발생함을 검증합니다.</p>
         * <ul>
         *     <li>에러 응답이 와야합니다. (401 UnAuthorized, 토큰 없음)</li>
         *     <li>내부 의존성이 호출되지 않아야합니다.</li>
         * </ul>
         */
        @Test
        @DisplayName("토큰 없는 사용자 요청 -> 인증 실패 401")
        public void updateSchedulePlace_withoutToken() throws Exception {
            Long scheduleId = 1L;
            Long targetDayId = 2L;
            int targetOrder = 3;

            var request = new ScheduleMoveRequest(targetDayId, targetOrder);

            // when
            ResultActions resultActions = runTestWithoutAuthority(scheduleId, createJson(request)); // 토큰 없는 사용자의 요청

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("auth-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists()); // 상태 코드 및 에러 응답 검증

            verify(scheduleCommandService, times(0)).moveSchedule(any(ScheduleMoveCommand.class)); // 서비스 호출 안 함 검증
        }

        /**
         * <p>경로변수로, 숫자가 아닌 일정 식별자 전달 시, 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외가 발생되는 지 검증합니다.</p>
         * <ul>
         *     <li>에러 응답이 와야합니다. (400 Bad Request, 경로 변수 관련 에러)</li>
         *     <li>내부 의존성이 호출되지 않아야합니다.</li>
         * </ul>
         */
        @Test
        @DisplayName("scheduleId가 숫자가 아닌 값 -> 경로변수 오류 400")
        public void updateSchedule_with_invalidScheduleId() throws Exception {
            long requestTripperId = 1L;
            mockingForLoginUserAnnotation(requestTripperId);

            String invalidScheduleId = "가가가";
            Long targetDayId = 2L;
            int targetOrder = 3;

            var request = new ScheduleMoveRequest(targetDayId, targetOrder);

            // when
            ResultActions resultActions = runTest(invalidScheduleId, createJson(request)); // 숫자가 아닌 일정 식별자로 일정 이동 요청

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0004"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists()); // 응답 메시지 검증

            // 내부 의존성 호출 안 됨
            verify(scheduleCommandService, times(0)).moveSchedule(any(ScheduleMoveCommand.class));
        }

        /**
         * <p>비어있는 본문으로 요청 시, 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외가 발생되는 지 검증합니다.</p>
         * <ul>
         *     <li>에러 응답이 와야합니다. (400 Bad Request, 형식이 올바르지 않은 바디 관련 에러)</li>
         *     <li>내부 의존성이 호출되지 않아야합니다.</li>
         * </ul>
         */
        @Test
        @DisplayName("비어있는 바디 -> 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외")
        public void moveSchedule_with_emptyContent() throws Exception {
            long requestTripperId = 1L;
            mockingForLoginUserAnnotation(requestTripperId);

            String emptyContent = "";
            Long scheduleId = 1L;

            // when
            ResultActions resultActions = runTest(scheduleId, emptyContent); // 비어있는 본문으로 요청할 때
            // then

            // 상태 코드 및 응답 에러 메시지 검증
            resultActions
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            // 서비스 호출 안 됨 검증
            verify(scheduleCommandService, times(0)).moveSchedule(any(ScheduleMoveCommand.class));
        }

        /**
         * 형식이 올바르지 않은 본문을 바디에 담아 요청할 때 에러가 발생함을 검증합니다.
         * <ul>
         *     <li>에러 응답이 와야합니다. (400 BadRequest, 형식이 올바르지 않은 바디 관련 에러)</li>
         *     <li>내부 의존성이 호출되지 않아야합니다.</li>
         * </ul>
         */
        @Test
        @DisplayName("형식이 올바르지 않은 바디 -> 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외")
        public void moveSchedule_with_invalidContent() throws Exception {
            // given
            long requestTripperId = 1L;
            mockingForLoginUserAnnotation(requestTripperId);

            Long scheduleId = 1L;
            String invalidContent = """
                {
                    "targetDayId": 따옴표로 감싸지 않은 값,
                    "targetOrder": 123
                }
                """;

            // when
            ResultActions resultActions = runTest(scheduleId, invalidContent);  // 형식이 올바르지 않은 body를 담아 요청할 때

            // then

            // 응답 메시지 검증
            resultActions
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            // 서비스가 호출되지 않음을 검증
            verify(scheduleCommandService, times(0)).moveSchedule(any(ScheduleMoveCommand.class));
        }

        /**
         * 타입이 올바르지 않은 필드가 포함된 본문을 보낼 때 에러가 발생함을 검증합니다.
         * <ul>
         *     <li>에러 응답이 와야합니다. (400 BadRequest, 형식이 올바르지 않은 데이터 형식, 데이터 타입 관련 에러)</li>
         *     <li>내부 의존성이 호출되지 않아야합니다.</li>
         * </ul>
         */
        @Test
        @DisplayName("타입이 올바르지 않은 요청 데이터 -> 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외")
        public void moveSchedule_with_invalidType() throws Exception {
            // given
            long requestTripperId = 1L;
            mockingForLoginUserAnnotation(requestTripperId);

            Long scheduleId = 1L;
            String invalidTypeContent = """
                {
                    "targetDayId": 1,
                    "targetOrder": "숫자가 아닌 값"
                }
                """;

            // when

            // 타입이 맞지 않는 필드가 포함된 body
            ResultActions resultActions = runTest(scheduleId, invalidTypeContent);

            // then

            // 응답 메시지 검증
            resultActions
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            // 서비스가 호출되지 않음을 검증
            verify(scheduleCommandService, times(0)).moveSchedule(any(ScheduleMoveCommand.class));
        }

        /**
         * 인증된 사용자의 요청을 mocking하여 수행하고, 그 결과를 객체로 얻어옵니다.
         * @param scheduleId : 일정 식별자(id)
         * @param content : 요청 본문(body)
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

        /**
         * 토큰이 없는(미인증) 사용자의 요청을 mocking하여 수행하고, 그 결과를 객체로 얻어옵니다.
         * @param scheduleId : 일정 식별자(id)
         * @param content : 요청 본문(body)
         * @return 실제 요청 실행 결과
         */
        private ResultActions runTestWithoutAuthority(Object scheduleId, String content) throws Exception {
            return mockMvc.perform(put("/api/schedules/{scheduleId}/position", scheduleId)
                    .content(content)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .contentType(MediaType.APPLICATION_JSON)
            );
        }
    }

    class 일정_수정_테스트{
        @Test
        @DisplayName("인증된 사용자 올바른 요청 -> 일정 수정됨")
        public void updateSchedule_with_authorizedUser() throws Exception {
            // given
            long requestTripperId = 1L;
            mockingForLoginUserAnnotation(requestTripperId);

            Long scheduleId = 1L;
            String rawScheduleTitle = "수정할 제목";
            String rawScheduleContent = "수정할 내용";
            LocalTime startTime = LocalTime.of(13,0);
            LocalTime endTime = LocalTime.of(13,5);

            var request = new ScheduleUpdateRequest(rawScheduleTitle, rawScheduleContent, startTime, endTime);
            var command = ScheduleUpdateCommand.of(scheduleId, requestTripperId, rawScheduleTitle, rawScheduleContent, startTime, endTime);

            // when
            ResultActions resultActions = runTest(scheduleId, createJson(request));

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scheduleId").value(scheduleId));

            verify(scheduleCommandService, times(1)).updateSchedule(eq(command));
        }

        @Test
        @DisplayName("미인증 사용자 요청 -> 인증 실패 401")
        public void updateSchedule_with_unauthorizedUser() throws Exception {
            // given
            Long scheduleId = 1L;
            String rawScheduleTitle = "수정할 제목";
            String rawScheduleContent = "수정할 내용";
            LocalTime startTime = LocalTime.of(13,0);
            LocalTime endTime = LocalTime.of(13,5);

            ScheduleUpdateRequest request = new ScheduleUpdateRequest(rawScheduleTitle, rawScheduleContent, startTime, endTime);

            // when
            ResultActions resultActions = runTestWithoutAuthority(scheduleId, createJson(request));

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").exists())
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(scheduleCommandService, times(0)).updateSchedule(any(ScheduleUpdateCommand.class));
        }

        @Test
        @DisplayName("scheduleId 가 숫자가 아님 -> 경로변수 오류 400 응답")
        public void updateSchedule_with_invalidScheduleId() throws Exception {
            long requestTripperId = 1L;
            mockingForLoginUserAnnotation(requestTripperId);

            String invalidScheduleId = "가가가";
            String rawScheduleTitle = "수정할 제목";
            String rawScheduleContent = "수정할 내용";
            LocalTime startTime = LocalTime.of(13,0);
            LocalTime endTime = LocalTime.of(13,5);

            ScheduleUpdateRequest request = new ScheduleUpdateRequest(rawScheduleTitle, rawScheduleContent, startTime, endTime);

            // given
            ResultActions resultActions = runTest(invalidScheduleId, createJson(request));

            // when
            resultActions
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0004"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(scheduleCommandService, times(0)).updateSchedule(any(ScheduleUpdateCommand.class));
        }


        @Test
        @DisplayName("비어있는 바디 -> 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외")
        public void updateSchedule_with_emptyContent() throws Exception {
            long requestTripperId = 1L;
            mockingForLoginUserAnnotation(requestTripperId);

            long scheduleId = 1L;
            String emptyContent = "";

            // when
            ResultActions resultActions = runTest(scheduleId, emptyContent);

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(scheduleCommandService, times(0)).updateSchedule(any(ScheduleUpdateCommand.class));
        }

        @Test
        @DisplayName("형식이 올바르지 않은 바디 -> 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외")
        public void updateSchedule_with_invalidContent() throws Exception {
            long requestTripperId = 1L;
            mockingForLoginUserAnnotation(requestTripperId);
            long scheduleId = 2L;
            String invalidContent = """
                {
                    "title": 따옴표로 감싸지 않은 제목,
                    "content": "본문",
                    "startTime": "13:05",
                    "endTime": "13:07"
                }
                """;

            // when
            ResultActions resultActions = runTest(scheduleId, invalidContent);

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(scheduleCommandService, times(0)).updateSchedule(any(ScheduleUpdateCommand.class));
        }

        private ResultActions runTest(Object scheduleId, String content) throws Exception {
            return mockMvc.perform(put("/api/schedules/{scheduleId}", scheduleId)
                    .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                    .content(content)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .contentType(MediaType.APPLICATION_JSON)
            );
        }

        private ResultActions runTestWithoutAuthority(Object scheduleId, String content) throws Exception {
            return mockMvc.perform(put("/api/schedules/{scheduleId}", scheduleId)
                    .content(content)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .contentType(MediaType.APPLICATION_JSON)
            );
        }
    }

    @Nested
    class 일정_조회_테스트{
        @Test
        @DisplayName("인증된 사용자 요청 -> 일정 단건 조회")
        public void findSingleSchedule_with_authorizedUser() throws Exception {

            Long scheduleId = 1L;
            mockingForLoginUserAnnotation();
            ScheduleDetail scheduleDetail = new ScheduleDetail(scheduleId, 1L, "제목", "장소 이름", 23.23, 23.23, 1L, "내용", LocalTime.of(15, 0), LocalTime.of(15, 30));
            given(scheduleQueryService.searchScheduleDetail(anyLong())).willReturn(scheduleDetail);

            mockMvc.perform(get("/api/schedules/{scheduleId}", scheduleId)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scheduleId").value(scheduleDetail.getScheduleId()))
                    .andExpect(jsonPath("$.content").value(scheduleDetail.getContent()))
                    .andExpect(jsonPath("$.title").value(scheduleDetail.getTitle()))
                    .andExpect(jsonPath("$.dayId").value(scheduleDetail.getDayId()))
                    .andExpect(jsonPath("$.coordinate.latitude").value(scheduleDetail.getCoordinate().getLatitude()))
                    .andExpect(jsonPath("$.coordinate.longitude").value(scheduleDetail.getCoordinate().getLongitude()))
                    .andExpect(jsonPath("$.scheduleTime.startTime").value(scheduleDetail.getScheduleTime().getStartTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))))
                    .andExpect(jsonPath("$.scheduleTime.endTime").value(scheduleDetail.getScheduleTime().getEndTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));

        }

        @Test
        @DisplayName("미인증 사용자 요청 -> 200")
        public void findSingleSchedule_with_unauthorizedUser() throws Exception {
            mockMvc.perform(get("/api/schedules/1"))
                    .andExpect(status().isOk());
        }
    }

}
