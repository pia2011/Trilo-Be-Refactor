package com.cosain.trilo.unit.trip.presentation.trip;

import com.cosain.trilo.support.RestControllerTest;
import com.cosain.trilo.trip.application.day.service.day_search.ScheduleSummary;
import com.cosain.trilo.trip.application.trip.TripCommandService;
import com.cosain.trilo.trip.application.trip.TripQueryService;
import com.cosain.trilo.trip.application.trip.dto.*;
import com.cosain.trilo.trip.domain.vo.TripStatus;
import com.cosain.trilo.trip.presentation.TripController;
import com.cosain.trilo.trip.presentation.request.trip.TripCreateRequest;
import com.cosain.trilo.trip.presentation.request.trip.TripPeriodUpdateRequest;
import com.cosain.trilo.trip.presentation.request.trip.TripSearchRequest;
import com.cosain.trilo.trip.presentation.request.trip.TripTitleUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.web.servlet.ResultActions;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripController.class)
public class TripControllerTest extends RestControllerTest {

    @MockBean
    private TripCommandService tripCommandService;

    @MockBean
    private TripQueryService tripQueryService;
    private final String ACCESS_TOKEN = "Bearer accessToken";

    @Nested
    class 여행_생성{
        @Test
        @DisplayName("인증된 사용자의 여행 생성 요청 -> 성공")
        public void successTest() throws Exception {
            // given
            long requestTripperId = 1L;
            mockingForLoginUserAnnotation(requestTripperId); // 인증된 사용자 mocking 됨

            String rawTitle = "제목";
            var request = new TripCreateRequest(rawTitle);

            Long createdTripId = 1L;
            var command = TripCreateCommand.of(requestTripperId, rawTitle);
            given(tripCommandService.createTrip(eq(command))).willReturn(createdTripId); // 여행 생성 후 서비스에서 반환받을 여행 식별자 mocking

            // when
            ResultActions resultActions = runTest(createJson(request)); // 정상적으로 인증된 사용자가 요청했을 때

            // then
            resultActions
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tripId").value(createdTripId)); // 상태코드 및 응답 필드 검증

            verify(tripCommandService, times(1)).createTrip(eq(command)); // 내부 의존성 호출 검증
        }

        @Test
        @DisplayName("토큰이 없는 사용자 요청 -> 인증 실패 401")
        public void createTrip_without_token() throws Exception {
            // given
            String rawTitle = "제목";
            TripCreateRequest request = new TripCreateRequest(rawTitle);

            // when
            ResultActions resultActions = runTestWithoutAuthorization(createJson(request)); // 인증하지 않은 사용자가 요청

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("auth-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists()); // 상태 코드 및 에러 응답 검증

            verify(tripCommandService, times(0)).createTrip(any(TripCreateCommand.class)); // 서비스 호출 안 됨 검증
        }

        @Test
        @DisplayName("비어있는 바디 -> 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외")
        public void createTrip_with_emptyContent() throws Exception {
            // given
            Long tripperId = 1L;
            mockingForLoginUserAnnotation(tripperId);

            String emptyContent = "";

            // when
            ResultActions resultActions = runTest(emptyContent); // 비어있는 본문으로 요청할 때

            // given
            resultActions
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists()); // 상태 코드 및 응답 에러 메시지 검증

            verify(tripCommandService, times(0)).createTrip(any(TripCreateCommand.class)); // 서비스 호출 안 됨 검증
        }

        @Test
        @DisplayName("형식이 올바르지 않은 바디 -> 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외")
        public void createTrip_with_invalidContent() throws Exception {
            Long tripperId = 1L;
            mockingForLoginUserAnnotation(tripperId);
            String invalidContent = """
                {
                    "title": 따옴표 안 감싼 제목
                }
                """;

            // when
            ResultActions resultActions = runTest(invalidContent); // 형식이 올바르지 않은 body를 담아 요청할 때

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());
            verify(tripCommandService, times(0)).createTrip(any(TripCreateCommand.class)); // 서비스가 호출되지 않음을 검증
        }

        private ResultActions runTest(String content) throws Exception {
            return mockMvc.perform(post("/api/trips")
                    .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                    .content(content)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .contentType(MediaType.APPLICATION_JSON)
            );
        }

        private ResultActions runTestWithoutAuthorization(String content) throws Exception {
            return mockMvc.perform(post("/api/trips")
                    .content(content)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .contentType(MediaType.APPLICATION_JSON)
            );
        }
    }

    @Nested
    class 여행_기간_수정{
        @Test
        @DisplayName("인증된 사용자 요청 -> 성공")
        public void updateTripPeriod_with_authorizedUser() throws Exception {
            // given
            long tripperId = 2L;
            mockingForLoginUserAnnotation(tripperId); // 인증된 사용자 mocking

            Long tripId = 1L;

            LocalDate startDate = LocalDate.of(2023,5,10);
            LocalDate endDate = LocalDate.of(2023,5,15);
            var request = new TripPeriodUpdateRequest(startDate, endDate);

            var command = TripPeriodUpdateCommand.of(tripId, tripperId, startDate, endDate);

            // when
            ResultActions resultActions = runTest(tripId, createJson(request)); // 정상적으로 사용자가 여행 기간 수정 요청했을 때

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tripId").value(tripId)); // 상태코드 및 응답 필드 검증

            verify(tripCommandService, times(1)).updateTripPeriod(eq(command)); // 내부 의존성 호출 검증
        }

        @Test
        @DisplayName("미인증 사용자 요청 -> 인증 실패 401")
        public void updateTripPeriod_with_unauthorizedUser() throws Exception {
            // given
            Long tripId = 1L;

            LocalDate startDate = LocalDate.of(2023,5,10);
            LocalDate endDate = LocalDate.of(2023,5,15);

            var request = new TripPeriodUpdateRequest(startDate, endDate);

            // when
            ResultActions resultActions = runTestWithoutAuthorization(tripId, createJson(request)); // 토큰 없는 사용자의 요청

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("auth-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists()); // 상태 코드 및 에러 응답 검증


            verify(tripCommandService, times(0)).updateTripPeriod(any(TripPeriodUpdateCommand.class)); // 서비스 호출 안 됨 검증
        }

        @Test
        @DisplayName("tripId로 숫자가 아닌 문자열 주입 -> 올바르지 않은 경로 변수 타입 400 에러")
        public void updateTripPeriod_with_notNumberTripId() throws Exception {
            // given
            long tripperId = 1L;
            mockingForLoginUserAnnotation(tripperId);

            String notNumberTripId = "가가가";
            LocalDate startDate = LocalDate.of(2023,5,10);
            LocalDate endDate = LocalDate.of(2023,5,15);

            var request = new TripPeriodUpdateRequest(startDate, endDate);

            // when
            ResultActions resultActions = runTest(notNumberTripId, createJson(request)); // 숫자가 아닌 여행 식별자로 여행 기간 수정 요청

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0004"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists()); // 응답 메시지 검증

            verify(tripCommandService, times(0)).updateTripPeriod(any(TripPeriodUpdateCommand.class)); // 내부 의존성 호출 안 됨 검증
        }

        @Test
        @DisplayName("비어있는 바디 -> 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외")
        public void updateTripPeriod_with_emptyContent() throws Exception {
            // given
            long tripperId = 1L;
            mockingForLoginUserAnnotation(tripperId);

            Long tripId = 2L;
            String emptyContent = "";

            // when
            ResultActions resultActions = runTest(tripId, emptyContent); // 비어있는 본문으로 요청할 때

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists()); // 상태 코드 및 응답 에러 메시지 검증

            verify(tripCommandService, times(0)).updateTripPeriod(any(TripPeriodUpdateCommand.class)); // 서비스 호출 안 됨 검증
        }

        @Test
        @DisplayName("형식이 올바르지 않은 바디 -> 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외")
        public void updateTripPeriod_with_invalidContent() throws Exception {
            long tripperId = 2L;
            mockingForLoginUserAnnotation(tripperId);

            Long tripId = 1L;
            String invalidContent = """
                {
                    "startDate" + 2023-03-01,
                    "endDate": "2023-03-02"
                }
                """;

            // when
            ResultActions resultActions = runTest(tripId, invalidContent); // 형식이 올바르지 않은 body를 담아 요청할 때

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists()); // 응답 메시지 검증

            verify(tripCommandService, times(0)).updateTripPeriod(any(TripPeriodUpdateCommand.class)); // 서비스가 호출되지 않음을 검증
        }

        @Test
        @DisplayName("타입이 올바르지 않은 요청 데이터 -> 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외")
        public void updateTripPeriod_with_invalidType() throws Exception {
            long tripperId = 1L;
            mockingForLoginUserAnnotation(tripperId);

            Long tripId = 1L;
            String invalidTypeContent = """
                {
                    "title": "제목",
                    "startDate": "2023-03-01",
                    "endDate": "날짜형식이 아닌 문자열"
                }
                """;

            // when
            ResultActions resultActions = runTest(tripId, invalidTypeContent); // 타입이 맞지 않는 필드가 포함된 body

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists()); // 응답 메시지 검증

            verify(tripCommandService, times(0)).updateTripPeriod(any(TripPeriodUpdateCommand.class)); // 서비스가 호출되지 않음을 검증
        }

        private ResultActions runTest(Object tripId, String content) throws Exception {
            return mockMvc.perform(put("/api/trips/{tripId}/period", tripId)
                    .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                    .content(content)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .contentType(MediaType.APPLICATION_JSON)
            );
        }

        private ResultActions runTestWithoutAuthorization(Object tripId, String content) throws Exception {
            return mockMvc.perform(put("/api/trips/{tripId}/period", tripId)
                    .content(content)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .contentType(MediaType.APPLICATION_JSON)
            );
        }
    }

    @Nested
    class 여행_삭제{
        @Test
        @DisplayName("인증된 사용자의 올바른 여행 삭제 요청 -> 성공")
        public void deleteTrip_with_authorizedUser() throws Exception {
            // given
            long requestTripperId = 2L;
            mockingForLoginUserAnnotation(requestTripperId);

            Long tripId = 1L;

            // when
            ResultActions resultActions = runTest(tripId); // 인증 사용자의 여행 삭제 요청

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isNoContent())
                    .andExpect(jsonPath("$").doesNotExist()); // 응답 메시지 검증

            verify(tripCommandService, times(1)).deleteTrip(eq(tripId), eq(requestTripperId)); // 내부 의존성 호출 검증
        }

        @Test
        @DisplayName("미인증 사용자 요청 -> 인증 실패 401")
        public void deleteTrip_with_unauthorizedUser() throws Exception {
            // given
            Long tripId = 1L;

            // when
            ResultActions resultActions = runTestWithoutAuthorization(tripId); // 미인증 사용자의 여행 삭제 요청

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").exists())
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists()); // 응답 메시지 검증

            verify(tripCommandService, times(0)).deleteTrip(eq(tripId), any(Long.class)); // 내부 의존성 호출 안 됨 검증
        }

        @Test
        @DisplayName("tripId으로 숫자가 아닌 문자열 주입 -> 올바르지 않은 경로 변수 타입 400 에러")
        public void deleteTrip_with_stringTripId() throws Exception {
            // given
            long requestTripperId = 2L;
            mockingForLoginUserAnnotation(requestTripperId);

            String invalidTripId = "가가가";

            // when
            ResultActions resultActions = runTest(invalidTripId);  // 숫자가 아닌 여행 식별자로 삭제 요청

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0004"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists()); // 응답 메시지 검증

            verify(tripCommandService, times(0)).deleteTrip(anyLong(), anyLong()); // 내부 의존성 호출 안 됨 검증
        }

        private ResultActions runTest(Object tripId) throws Exception {
            return mockMvc.perform(delete("/api/trips/{tripId}", tripId)
                    .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .contentType(MediaType.APPLICATION_JSON)
            );
        }

        private ResultActions runTestWithoutAuthorization(Object tripId) throws Exception {
            return mockMvc.perform(delete("/api/trips/{tripId}", tripId)
                    // 인증 헤더 없음
                    .characterEncoding(StandardCharsets.UTF_8)
                    .contentType(MediaType.APPLICATION_JSON)
            );
        }
    }

    @Nested
    class 여행_제목_수정{
        @Test
        @DisplayName("인증된 사용자 요청 -> 성공")
        public void updateTripTitle_with_authorizedUser() throws Exception {
            // given
            long requestTripperId = 2L;
            mockingForLoginUserAnnotation(requestTripperId);

            Long tripId = 1L;
            String rawTitle = "변경할 제목";
            var request = new TripTitleUpdateRequest(rawTitle);
            var command = TripTitleUpdateCommand.of(tripId, requestTripperId, rawTitle);

            willDoNothing().given(tripCommandService).updateTripTitle(eq(command));


            mockMvc.perform(put("/api/trips/{tripId}/title", tripId)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .content(createJson(request))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tripId").value(tripId));

            verify(tripCommandService, times(1)).updateTripTitle(eq(command));
        }

        @Test
        @DisplayName("미인증 사용자 요청 -> 인증 실패 401")
        public void updateTripTitle_with_unauthorizedUser() throws Exception {
            // given
            Long tripId = 1L;
            String rawTitle = "변경할 제목";
            var request = new TripTitleUpdateRequest(rawTitle);
            mockMvc.perform(put("/api/trips/{tripId}/title", tripId)
                            .content(createJson(request))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("auth-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(tripCommandService, times(0)).updateTripTitle(any(TripTitleUpdateCommand.class));
        }

        @Test
        @DisplayName("tripId으로 숫자가 아닌 문자열 주입 -> 올바르지 않은 경로 변수 타입 400 에러")
        public void updateTripTitle_with_notNumberTripId() throws Exception {
            // given
            mockingForLoginUserAnnotation();

            String notNumberTripId = "가가가";
            String rawTitle = "변경할 제목";

            TripTitleUpdateRequest request = new TripTitleUpdateRequest(rawTitle);

            mockMvc.perform(put("/api/trips/{tripId}/title", notNumberTripId)
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

            verify(tripCommandService, times(0)).updateTripTitle(any(TripTitleUpdateCommand.class));
        }

        @Test
        @DisplayName("비어있는 바디 -> 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외")
        public void updateTripTitle_with_emptyContent() throws Exception {
            mockingForLoginUserAnnotation();
            Long tripId = 1L;

            String emptyContent = "";

            mockMvc.perform(put("/api/trips/{tripId}/title", tripId)
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

            verify(tripCommandService, times(0)).updateTripTitle(any(TripTitleUpdateCommand.class));
        }

        @Test
        @DisplayName("형식이 올바르지 않은 바디 -> 올바르지 않은 요청 데이터 형식으로 간주하고 400 예외")
        public void updateTrip_with_invalidContent() throws Exception {
            mockingForLoginUserAnnotation();

            Long tripId = 1L;
            String invalidContent = """
                {
                    "title": 괄호로 감싸지 않은 제목,
                }
                """;

            mockMvc.perform(put("/api/trips/1/title", tripId)
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

            verify(tripCommandService, times(0)).updateTripTitle(any(TripTitleUpdateCommand.class));
        }
    }

    @Nested
    class 여행_이미지_수정{
        @Test
        @DisplayName("실제 jpeg -> 성공")
        public void testRealJpegImage() throws Exception {
            long tripperId = 2L;
            mockingForLoginUserAnnotation(tripperId);
            Long tripId = 1L;

            String name = "image";
            String fileName = "test-jpeg-image.jpeg";
            String filePath = "src/test/resources/testFiles/" + fileName;
            FileInputStream fileInputStream = new FileInputStream(filePath);
            String contentType = "image/jpeg";
            MockMultipartFile multipartFile = new MockMultipartFile(name, fileName, contentType, fileInputStream);

            String imageURL = String.format("https://{이미지 파일 저장소 주소}/trips/%s/{이미지 파일명}.jpeg", tripId);
            given(tripCommandService.updateTripImage(any(TripImageUpdateCommand.class)))
                    .willReturn(imageURL);

            mockMvc.perform(multipart(POST, "/api/trips/{tripId}/image/update", tripId)
                            .file(multipartFile)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tripId").value(tripId))
                    .andExpect(jsonPath("$.imageURL").value(imageURL));

            verify(tripCommandService, times(1)).updateTripImage(any(TripImageUpdateCommand.class));
        }

        @Test
        @DisplayName("실제 gif -> 성공")
        public void testRealGifImage() throws Exception {
            long tripperId = 2L;
            mockingForLoginUserAnnotation(tripperId);
            Long tripId = 1L;

            String name = "image";
            String fileName = "test-gif-image.gif";
            String filePath = "src/test/resources/testFiles/" + fileName;
            FileInputStream fileInputStream = new FileInputStream(filePath);
            String contentType = "image/gif";


            MockMultipartFile multipartFile = new MockMultipartFile(name, fileName, contentType, fileInputStream);

            String imageURL = String.format("https://{이미지 파일 저장소 주소}/trips/%s/{이미지 파일명}.gif", tripId);
            given(tripCommandService.updateTripImage(any(TripImageUpdateCommand.class))).willReturn(imageURL);

            mockMvc.perform(multipart(POST, "/api/trips/{tripId}/image/update", tripId)
                            .file(multipartFile)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tripId").value(tripId))
                    .andExpect(jsonPath("$.imageURL").value(imageURL));

            verify(tripCommandService, times(1)).updateTripImage(any(TripImageUpdateCommand.class));
        }

        @Test
        @DisplayName("실제 png -> 성공")
        public void testRealPngImage() throws Exception {
            long tripperId = 2L;
            mockingForLoginUserAnnotation(tripperId);
            Long tripId = 1L;

            String name = "image";
            String fileName = "test-png-image.png";
            String filePath = "src/test/resources/testFiles/" + fileName;
            FileInputStream fileInputStream = new FileInputStream(filePath);
            String contentType = "image/png";
            MockMultipartFile multipartFile = new MockMultipartFile(name, fileName, contentType, fileInputStream);

            String imageURL = String.format("https://{이미지 파일 저장소 주소}/trips/%s/{이미지 파일명}.png", tripId);
            given(tripCommandService.updateTripImage(any(TripImageUpdateCommand.class)))
                    .willReturn(imageURL);

            mockMvc.perform(multipart(POST, "/api/trips/{tripId}/image/update", tripId)
                            .file(multipartFile)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tripId").value(tripId))
                    .andExpect(jsonPath("$.imageURL").value(imageURL));

            verify(tripCommandService, times(1)).updateTripImage(any(TripImageUpdateCommand.class));
        }

        @Test
        @DisplayName("실제 webp -> 성공")
        public void testRealWebpImage() throws Exception {
            Long tripperId = 2L;
            mockingForLoginUserAnnotation(tripperId);
            Long tripId = 1L;

            String name = "image";
            String fileName = "test-webp-image.webp";
            String filePath = "src/test/resources/testFiles/" + fileName;
            FileInputStream fileInputStream = new FileInputStream(filePath);
            String contentType = "image/webp";

            MockMultipartFile multipartFile = new MockMultipartFile(name, fileName, contentType, fileInputStream);
            String imageURL = String.format("https://{이미지 파일 저장소 주소}/trips/%s/{이미지 파일명}.webp", tripId);
            given(tripCommandService.updateTripImage(any(TripImageUpdateCommand.class)))
                    .willReturn(imageURL);

            mockMvc.perform(multipart(POST, "/api/trips/{tripId}/image/update", tripId)
                            .file(multipartFile)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tripId").value(tripId))
                    .andExpect(jsonPath("$.imageURL").value(imageURL));

            verify(tripCommandService, times(1)).updateTripImage(any(TripImageUpdateCommand.class));
        }

        @Test
        @DisplayName("미인증 사용자 -> 인증 실패 401")
        public void testUnAuthorizedUser() throws Exception {
            Long tripId = 1L;

            String name = "image";
            String fileName = "test-jpeg-image.jpeg";
            String filePath = "src/test/resources/testFiles/" + fileName;
            FileInputStream fileInputStream = new FileInputStream(filePath);
            String contentType = "image/jpeg";

            MockMultipartFile multipartFile = new MockMultipartFile(name, fileName, contentType, fileInputStream);
            mockMvc.perform(multipart(POST, "/api/trips/{tripId}/image/update", tripId)
                            .file(multipartFile)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andDo(print())
                    .andExpect(jsonPath("$.errorCode").value("auth-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(tripCommandService, times(0)).updateTripImage(any(TripImageUpdateCommand.class));
        }

        @Test
        @DisplayName("비어 있는 파일 -> 파일 비어 있음 관련 400 에러 발생")
        public void emptyFileTest() throws Exception {
            long tripperId = 2L;
            mockingForLoginUserAnnotation(tripperId);

            // given
            Long tripId = 1L;
            String name = "image";
            String fileName = "empty-file.jpg";
            String filePath = "src/test/resources/testFiles/" + fileName;

            FileInputStream fileInputStream = new FileInputStream(filePath);
            MockMultipartFile multipartFile = new MockMultipartFile(name, fileInputStream);

            // when & then
            mockMvc.perform(multipart(POST, "/api/trips/{tripId}/image/update", tripId)
                            .file(multipartFile)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    ).andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("file-0001"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(tripCommandService, times(0)).updateTripImage(any(TripImageUpdateCommand.class));
        }

        @Test
        @DisplayName("tripId으로 숫자가 아닌 문자열 주입 -> 올바르지 않은 경로 변수 타입 400 에러")
        public void testInvalidTripId() throws Exception {
            mockingForLoginUserAnnotation();
            String invalidTripId = "가가가";

            String name = "image";
            String fileName = "test-jpeg-image.jpeg";
            String filePath = "src/test/resources/testFiles/" + fileName;
            FileInputStream fileInputStream = new FileInputStream(filePath);
            String contentType = "image/jpeg";

            MockMultipartFile multipartFile = new MockMultipartFile(name, fileName, contentType, fileInputStream);
            mockMvc.perform(multipart(POST, "/api/trips/{tripId}/image/update", invalidTripId)
                            .file(multipartFile)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0004"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(tripCommandService, times(0)).updateTripImage(any(TripImageUpdateCommand.class));
        }

        @Test
        @DisplayName("파일을 보내지 않음 -> 검증 오류 400 에러")
        public void testNotMultipartRequest() throws Exception {
            long tripperId = 2L;
            mockingForLoginUserAnnotation(tripperId);
            Long tripId = 1L;

            mockMvc.perform(post("/api/trips/{tripId}/image/update", tripId)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("request-0003"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists())
                    .andExpect(jsonPath("$.errors").isNotEmpty())
                    .andExpect(jsonPath("$.errors[0].errorCode").value("file-0001"))
                    .andExpect(jsonPath("$.errors[0].errorMessage").exists())
                    .andExpect(jsonPath("$.errors[0].errorDetail").exists())
                    .andExpect(jsonPath("$.errors[0].field").value("image"));

            verify(tripCommandService, times(0)).updateTripImage(any(TripImageUpdateCommand.class));
        }

        @Test
        @DisplayName("MultipartFile에 파일 이름 없음 -> NoFileName 400 에러 발생")
        public void noFileName() throws Exception {
            long tripperId = 2L;
            mockingForLoginUserAnnotation(tripperId);
            Long tripId = 1L;

            String name = "image";
            String fileName = "test-jpeg-image.jpeg";
            String filePath = "src/test/resources/testFiles/" + fileName;
            FileInputStream fileInputStream = new FileInputStream(filePath);

            // MultipartFile에 파일 이름 정보가 전달되지 않은 특수상황 가정
            MockMultipartFile multipartFile = new MockMultipartFile(name, fileInputStream);

            mockMvc.perform(multipart(POST, "/api/trips/{tripId}/image/update", tripId)
                            .file(multipartFile)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("file-0002"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(tripCommandService, times(0)).updateTripImage(any(TripImageUpdateCommand.class));
        }

        @Test
        @DisplayName("확장자 없음 -> NoFileExtension 400 예외 발생")
        public void noFileExtension() throws Exception {
            long tripperId = 2L;
            mockingForLoginUserAnnotation(tripperId);
            Long tripId = 1L;

            String name = "image";
            String fileName = "no-extension"; // 확장자 없음
            String filePath = "src/test/resources/testFiles/" + fileName;
            FileInputStream fileInputStream = new FileInputStream(filePath);
            String contentType = "application/octet-stream";

            MockMultipartFile multipartFile = new MockMultipartFile(name, fileName, contentType, fileInputStream);

            mockMvc.perform(multipart(POST, "/api/trips/{tripId}/image/update", tripId)
                            .file(multipartFile)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("file-0003"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(tripCommandService, times(0)).updateTripImage(any(TripImageUpdateCommand.class));
        }

        @Test
        @DisplayName("이미지 파일 확장자 아님 -> NotImageFileExtension 400 에러 발생")
        public void notImageFileExtension() throws Exception {
            long tripperId = 2L;
            mockingForLoginUserAnnotation(tripperId);
            Long tripId = 1L;

            String name = "image";
            String fileName = "not-image-extension.txt"; // 확장자가 이미지가 아님
            String filePath = "src/test/resources/testFiles/" + fileName;
            FileInputStream fileInputStream = new FileInputStream(filePath);
            String contentType = "text/plain";

            MockMultipartFile multipartFile = new MockMultipartFile(name, fileName, contentType, fileInputStream);

            mockMvc.perform(multipart(POST, "/api/trips/{tripId}/image/update", tripId)
                            .file(multipartFile)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("file-0004"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(tripCommandService, times(0)).updateTripImage(any(TripImageUpdateCommand.class));
        }

        @Test
        @DisplayName("이미지 아님 -> NotImageFile 400 예외 발생")
        public void noImageFile() throws Exception {
            long tripperId = 2L;
            mockingForLoginUserAnnotation(tripperId);
            Long tripId = 1L;

            String name = "image";
            String fileName = "no-image.jpg"; // 확장자는 jpg인데 실제로 이미지가 아님
            String filePath = "src/test/resources/testFiles/" + fileName;
            FileInputStream fileInputStream = new FileInputStream(filePath);
            String contentType = "image/jpg";

            MockMultipartFile multipartFile = new MockMultipartFile(name, fileName, contentType, fileInputStream);

            mockMvc.perform(multipart(POST, "/api/trips/{tripId}/image/update", tripId)
                            .file(multipartFile)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("file-0005"))
                    .andExpect(jsonPath("$.errorMessage").exists())
                    .andExpect(jsonPath("$.errorDetail").exists());

            verify(tripCommandService, times(0)).updateTripImage(any(TripImageUpdateCommand.class));
        }
    }

    @Nested
    class 여행_조회{
        @Test
        @DisplayName("인증된 사용자의 요청 -> 여행 단건 정보 조회")
        public void findSingleTrip_with_authorizedUser() throws Exception {
            // given
            mockingForLoginUserAnnotation();
            TripDetail tripDetail = new TripDetail(1L, 2L, "여행 제목", TripStatus.DECIDED, LocalDate.of(2023, 4, 4), LocalDate.of(2023, 4, 5));
            given(tripQueryService.searchTripDetail(anyLong())).willReturn(tripDetail);

            // when & then
            mockMvc.perform(get("/api/trips/1")
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tripId").value(tripDetail.getTripId()))
                    .andExpect(jsonPath("$.title").value(tripDetail.getTitle()))
                    .andExpect(jsonPath("$.status").value(tripDetail.getStatus()))
                    .andExpect(jsonPath("$.startDate").value(tripDetail.getStartDate().toString()))
                    .andExpect(jsonPath("$.endDate").value(tripDetail.getEndDate().toString()));


            verify(tripQueryService).searchTripDetail(anyLong());
        }

        @Test
        @DisplayName("미인증 사용자 요청 -> 200")
        public void findSingleTrip_with_unauthorizedUser() throws Exception {
            mockMvc.perform(get("/api/trips/1"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class 여행_목록_조회{

        private static final String BASE_URL = "/api/trips";
        @Test
        void 여행_목록_정상_조회_200() throws Exception {

            // given
            int size = 5;
            String query = "제주";
            Long tripId = 1L;
            String imageURL = "https://.../image.jpg";
            TripSearchRequest.SortType sortType = TripSearchRequest.SortType.RECENT;
            TripSearchResponse.TripSummary tripSummary1 = new TripSearchResponse.TripSummary(2L, 1L, LocalDate.of(2023, 4, 4), LocalDate.of(2023, 4, 10), "제주도 여행", imageURL);
            TripSearchResponse.TripSummary tripSummary2 = new TripSearchResponse.TripSummary(1L, 1L, LocalDate.of(2023, 4, 4), LocalDate.of(2023, 4, 10), "제주 가보자", imageURL);
            TripSearchResponse tripSearchResponse = new TripSearchResponse(true, List.of(tripSummary1, tripSummary2));

            given(tripQueryService.findBySearchConditions(any(TripSearchRequest.class))).willReturn(tripSearchResponse);

            // when & then
            mockMvc.perform(RestDocumentationRequestBuilders.get(BASE_URL)
                            .param("sortType", String.valueOf(sortType))
                            .param("query", query)
                            .param("tripId", String.valueOf(tripId))
                            .param("size", String.valueOf(size))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        void 정렬_기준_값을_보내지_않을_경우_200() throws Exception {

            // given
            int size = 5;
            String query = "제주";
            Long tripId = 1L;
            String imageURL = "https://.../image.jpg";
            TripSearchResponse.TripSummary tripSummary1 = new TripSearchResponse.TripSummary(2L, 1L, LocalDate.of(2023, 4, 4), LocalDate.of(2023, 4, 10), "제주도 여행", imageURL);
            TripSearchResponse.TripSummary tripSummary2 = new TripSearchResponse.TripSummary(1L, 1L, LocalDate.of(2023, 4, 4), LocalDate.of(2023, 4, 10), "제주 가보자", imageURL);
            TripSearchResponse tripSearchResponse = new TripSearchResponse(true, List.of(tripSummary1, tripSummary2));

            given(tripQueryService.findBySearchConditions(any(TripSearchRequest.class))).willReturn(tripSearchResponse);

            // when & then
            mockMvc.perform(RestDocumentationRequestBuilders.get(BASE_URL)
                            .param("query", query)
                            .param("tripId", String.valueOf(tripId))
                            .param("size", String.valueOf(size))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        void 페이지_사이즈를_보내지_않을_경우_200() throws Exception {

            // given
            String query = "제주";
            Long tripId = 1L;
            String imageURL = "https://.../image.jpg";
            TripSearchRequest.SortType sortType = TripSearchRequest.SortType.RECENT;
            TripSearchResponse.TripSummary tripSummary1 = new TripSearchResponse.TripSummary(2L, 1L, LocalDate.of(2023, 4, 4), LocalDate.of(2023, 4, 10), "제주도 여행", imageURL);
            TripSearchResponse.TripSummary tripSummary2 = new TripSearchResponse.TripSummary(1L, 1L, LocalDate.of(2023, 4, 4), LocalDate.of(2023, 4, 10), "제주 가보자", imageURL);
            TripSearchResponse tripSearchResponse = new TripSearchResponse(true, List.of(tripSummary1, tripSummary2));

            given(tripQueryService.findBySearchConditions(any(TripSearchRequest.class))).willReturn(tripSearchResponse);

            // when & then
            mockMvc.perform(RestDocumentationRequestBuilders.get(BASE_URL)
                            .param("sortType", String.valueOf(sortType))
                            .param("query", query)
                            .param("tripId", String.valueOf(tripId))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

        }

        @ParameterizedTest
        @ValueSource(ints = {101, 2000, 30000})
        void 페이지_사이즈가_100을_초과하는_경우_400(int size) throws Exception {
            // given
            String query = "제주";
            Long tripId = 1L;
            String imageURL = "https://.../image.jpg";
            TripSearchRequest.SortType sortType = TripSearchRequest.SortType.RECENT;
            TripSearchResponse.TripSummary tripSummary1 = new TripSearchResponse.TripSummary(2L, 1L, LocalDate.of(2023, 4, 4), LocalDate.of(2023, 4, 10), "제주도 여행", imageURL);
            TripSearchResponse.TripSummary tripSummary2 = new TripSearchResponse.TripSummary(1L, 1L, LocalDate.of(2023, 4, 4), LocalDate.of(2023, 4, 10), "제주 가보자", imageURL);
            TripSearchResponse tripSearchResponse = new TripSearchResponse(true, List.of(tripSummary1, tripSummary2));

            given(tripQueryService.findBySearchConditions(any(TripSearchRequest.class))).willReturn(tripSearchResponse);

            // when & then
            mockMvc.perform(RestDocumentationRequestBuilders.get(BASE_URL)
                            .param("sortType", String.valueOf(sortType))
                            .param("query", query)
                            .param("tripId", String.valueOf(tripId))
                            .param("size", String.valueOf(size))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }


    @Nested
    class 사용자_여행_목록_조회{
        @Test
        @DisplayName("인증된 사용자 요청 -> 회원 여행 목록 조회")
        public void findTripperTripList_with_authorizedUser() throws Exception {

            // given
            long tripperId = 1L;
            mockingForLoginUserAnnotation(tripperId);

            Integer size = 3;

            TripListSearchResult.TripSummary tripSummary1 = new TripListSearchResult.TripSummary(1L, tripperId, "제목 1", TripStatus.DECIDED, LocalDate.of(2023, 3,4), LocalDate.of(2023, 3, 5), "image.jpg");
            TripListSearchResult.TripSummary tripSummary2 = new TripListSearchResult.TripSummary(2L, tripperId, "제목 2", TripStatus.UNDECIDED, null, null, "image.jpg");
            TripListSearchResult.TripSummary tripSummary3 = new TripListSearchResult.TripSummary(3L, tripperId, "제목 3", TripStatus.DECIDED, LocalDate.of(2023, 4,4), LocalDate.of(2023, 4, 5), "image.jpg");

            TripListQueryParam queryParam = TripListQueryParam.of(tripperId, null, size);
            TripListSearchResult searchResult = TripListSearchResult.of(true, List.of(tripSummary3, tripSummary2, tripSummary1));

            given(tripQueryService.searchTripList(eq(queryParam))).willReturn(searchResult);

            // when & then
            mockMvc.perform(get("/api/trippers/{tripperId}/trips", tripperId)
                            .param("size", String.valueOf(size))
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpectAll(status().isOk())
                    .andExpect(jsonPath("$.hasNext").value(searchResult.isHasNext()))
                    .andExpect(jsonPath("$.trips").isNotEmpty())
                    .andExpect(jsonPath("$.trips.[0].tripId").value(tripSummary3.getTripId()))
                    .andExpect(jsonPath("$.trips.[1].tripId").value(tripSummary2.getTripId()))
                    .andExpect(jsonPath("$.trips.[2].tripId").value(tripSummary1.getTripId()))
                    .andExpect(jsonPath("$.trips.[0].title").value(tripSummary3.getTitle()))
                    .andExpect(jsonPath("$.trips.[1].title").value(tripSummary2.getTitle()))
                    .andExpect(jsonPath("$.trips.[2].title").value(tripSummary1.getTitle()))
                    .andExpect(jsonPath("$.trips.[0].status").value(tripSummary3.getStatus()))
                    .andExpect(jsonPath("$.trips.[1].status").value(tripSummary2.getStatus()))
                    .andExpect(jsonPath("$.trips.[2].status").value(tripSummary1.getStatus()))
                    .andExpect(jsonPath("$.trips.[0].startDate").value(tripSummary3.getStartDate().toString()))
                    .andExpect(jsonPath("$.trips.[1].startDate").doesNotExist())
                    .andExpect(jsonPath("$.trips.[2].startDate").value(tripSummary1.getStartDate().toString()))
                    .andExpect(jsonPath("$.trips.[0].endDate").value(tripSummary3.getEndDate().toString()))
                    .andExpect(jsonPath("$.trips.[1].endDate").doesNotExist())
                    .andExpect(jsonPath("$.trips.[2].endDate").value(tripSummary1.getEndDate().toString()))
                    .andExpect(jsonPath("$.trips.[0].imageURL").value(tripSummary3.getImageURL()))
                    .andExpect(jsonPath("$.trips.[1].imageURL").value(tripSummary2.getImageURL()))
                    .andExpect(jsonPath("$.trips.[2].imageURL").value(tripSummary1.getImageURL()));
        }

        @Test
        @DisplayName("로그인 하지 않은 사용자 요청 -> 200 조회 성공")
        public void findTripperTripList_with_unauthorizedUser() throws Exception {
            // given
            long tripperId = 1L;
            Integer size = 3;

            TripListSearchResult.TripSummary tripSummary1 = new TripListSearchResult.TripSummary(1L, tripperId, "제목 1", TripStatus.DECIDED, LocalDate.of(2023, 3,4), LocalDate.of(2023, 3, 5), "image.jpg");
            TripListSearchResult.TripSummary tripSummary2 = new TripListSearchResult.TripSummary(2L, tripperId, "제목 2", TripStatus.UNDECIDED, null, null, "image.jpg");
            TripListSearchResult.TripSummary tripSummary3 = new TripListSearchResult.TripSummary(3L, tripperId, "제목 3", TripStatus.DECIDED, LocalDate.of(2023, 4,4), LocalDate.of(2023, 4, 5), "image.jpg");

            TripListQueryParam queryParam = TripListQueryParam.of(tripperId, null, size);
            TripListSearchResult searchResult = TripListSearchResult.of(true, List.of(tripSummary3, tripSummary2, tripSummary1));

            given(tripQueryService.searchTripList(eq(queryParam))).willReturn(searchResult);

            // when & then
            mockMvc.perform(get("/api/trippers/{tripperId}/trips", tripperId)
                            .param("size", String.valueOf(size))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpectAll(status().isOk())
                    .andExpect(jsonPath("$.hasNext").value(searchResult.isHasNext()))
                    .andExpect(jsonPath("$.trips").isNotEmpty())
                    .andExpect(jsonPath("$.trips.[0].tripId").value(tripSummary3.getTripId()))
                    .andExpect(jsonPath("$.trips.[1].tripId").value(tripSummary2.getTripId()))
                    .andExpect(jsonPath("$.trips.[2].tripId").value(tripSummary1.getTripId()))
                    .andExpect(jsonPath("$.trips.[0].title").value(tripSummary3.getTitle()))
                    .andExpect(jsonPath("$.trips.[1].title").value(tripSummary2.getTitle()))
                    .andExpect(jsonPath("$.trips.[2].title").value(tripSummary1.getTitle()))
                    .andExpect(jsonPath("$.trips.[0].status").value(tripSummary3.getStatus()))
                    .andExpect(jsonPath("$.trips.[1].status").value(tripSummary2.getStatus()))
                    .andExpect(jsonPath("$.trips.[2].status").value(tripSummary1.getStatus()))
                    .andExpect(jsonPath("$.trips.[0].startDate").value(tripSummary3.getStartDate().toString()))
                    .andExpect(jsonPath("$.trips.[1].startDate").doesNotExist())
                    .andExpect(jsonPath("$.trips.[2].startDate").value(tripSummary1.getStartDate().toString()))
                    .andExpect(jsonPath("$.trips.[0].endDate").value(tripSummary3.getEndDate().toString()))
                    .andExpect(jsonPath("$.trips.[1].endDate").doesNotExist())
                    .andExpect(jsonPath("$.trips.[2].endDate").value(tripSummary1.getEndDate().toString()))
                    .andExpect(jsonPath("$.trips.[0].imageURL").value(tripSummary3.getImageURL()))
                    .andExpect(jsonPath("$.trips.[1].imageURL").value(tripSummary2.getImageURL()))
                    .andExpect(jsonPath("$.trips.[2].imageURL").value(tripSummary1.getImageURL()));
        }
    }

    @Nested
    class 임시_보관함_조회{
        @Test
        @DisplayName("정상 동작 확인")
        public void findTripTemporaryStorage_with_authorizedUser() throws Exception {
            // given
            int size = 2;
            Long tripId = 1L;
            Long scheduleId = 1L;
            mockingForLoginUserAnnotation();
            ScheduleSummary scheduleSummary1 = new ScheduleSummary(2L, "일정 제목1", "제목","장소 식별자", 33.33, 33.33);
            ScheduleSummary scheduleSummary2 = new ScheduleSummary(3L, "일정 제목2", "제목","장소 식별자",33.33, 33.33);

            var queryParam = TempScheduleListQueryParam.of(tripId, scheduleId, size);
            var result = TempScheduleListSearchResult.of(true, List.of(scheduleSummary1, scheduleSummary2));
            given(tripQueryService.searchTemporary(eq(queryParam))).willReturn(result);

            mockMvc.perform(get("/api/trips/{tripId}/temporary-storage", tripId)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .param("size", String.valueOf(size))
                            .param("scheduleId", String.valueOf(scheduleId))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasNext").isBoolean())
                    .andExpect(jsonPath("$.tempSchedules").isNotEmpty())
                    .andExpect(jsonPath("$.tempSchedules.size()").value(result.getTempSchedules().size()))
                    .andExpect(jsonPath("$.tempSchedules[0].scheduleId").value(scheduleSummary1.getScheduleId()))
                    .andExpect(jsonPath("$.tempSchedules[1].scheduleId").value(scheduleSummary2.getScheduleId()))
                    .andExpect(jsonPath("$.tempSchedules[0].title").value(scheduleSummary1.getTitle()))
                    .andExpect(jsonPath("$.tempSchedules[1].title").value(scheduleSummary2.getTitle()))
                    .andExpect(jsonPath("$.tempSchedules[0].placeId").value(scheduleSummary1.getPlaceId()))
                    .andExpect(jsonPath("$.tempSchedules[1].placeId").value(scheduleSummary2.getPlaceId()))
                    .andExpect(jsonPath("$.tempSchedules[0].placeName").value(scheduleSummary1.getPlaceName()))
                    .andExpect(jsonPath("$.tempSchedules[1].placeName").value(scheduleSummary2.getPlaceName()))
                    .andExpect(jsonPath("$.tempSchedules[0].coordinate.latitude").value(scheduleSummary1.getCoordinate().getLatitude()))
                    .andExpect(jsonPath("$.tempSchedules[1].coordinate.latitude").value(scheduleSummary2.getCoordinate().getLatitude()))
                    .andExpect(jsonPath("$.tempSchedules[0].coordinate.longitude").value(scheduleSummary1.getCoordinate().getLongitude()))
                    .andExpect(jsonPath("$.tempSchedules[1].coordinate.longitude").value(scheduleSummary2.getCoordinate().getLongitude()));

            verify(tripQueryService, times(1)).searchTemporary(eq(queryParam));
        }

        @Test
        @DisplayName("미인증 사용자 요청 -> 200 성공")
        public void findTripTemporaryStorage_with_unauthorizedUser() throws Exception {
            // given
            int size = 2;
            Long tripId = 1L;
            Long scheduleId = 1L;
            mockingForLoginUserAnnotation();
            ScheduleSummary scheduleSummary1 = new ScheduleSummary(2L, "일정 제목1", "제목","장소 식별자", 33.33, 33.33);
            ScheduleSummary scheduleSummary2 = new ScheduleSummary(3L, "일정 제목2", "제목","장소 식별자",33.33, 33.33);

            var queryParam = TempScheduleListQueryParam.of(tripId, scheduleId, size);
            var result = TempScheduleListSearchResult.of(true, List.of(scheduleSummary1, scheduleSummary2));
            given(tripQueryService.searchTemporary(eq(queryParam))).willReturn(result);

            mockMvc.perform(get("/api/trips/{tripId}/temporary-storage", tripId)
                            .param("size", String.valueOf(size))
                            .param("scheduleId", String.valueOf(scheduleId))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasNext").isBoolean())
                    .andExpect(jsonPath("$.tempSchedules").isNotEmpty())
                    .andExpect(jsonPath("$.tempSchedules.size()").value(result.getTempSchedules().size()))
                    .andExpect(jsonPath("$.tempSchedules[0].scheduleId").value(scheduleSummary1.getScheduleId()))
                    .andExpect(jsonPath("$.tempSchedules[1].scheduleId").value(scheduleSummary2.getScheduleId()))
                    .andExpect(jsonPath("$.tempSchedules[0].title").value(scheduleSummary1.getTitle()))
                    .andExpect(jsonPath("$.tempSchedules[1].title").value(scheduleSummary2.getTitle()))
                    .andExpect(jsonPath("$.tempSchedules[0].placeId").value(scheduleSummary1.getPlaceId()))
                    .andExpect(jsonPath("$.tempSchedules[1].placeId").value(scheduleSummary2.getPlaceId()))
                    .andExpect(jsonPath("$.tempSchedules[0].placeName").value(scheduleSummary1.getPlaceName()))
                    .andExpect(jsonPath("$.tempSchedules[1].placeName").value(scheduleSummary2.getPlaceName()))
                    .andExpect(jsonPath("$.tempSchedules[0].coordinate.latitude").value(scheduleSummary1.getCoordinate().getLatitude()))
                    .andExpect(jsonPath("$.tempSchedules[1].coordinate.latitude").value(scheduleSummary2.getCoordinate().getLatitude()))
                    .andExpect(jsonPath("$.tempSchedules[0].coordinate.longitude").value(scheduleSummary1.getCoordinate().getLongitude()))
                    .andExpect(jsonPath("$.tempSchedules[1].coordinate.longitude").value(scheduleSummary2.getCoordinate().getLongitude()));

            verify(tripQueryService, times(1)).searchTemporary(eq(queryParam));
        }
    }
}
