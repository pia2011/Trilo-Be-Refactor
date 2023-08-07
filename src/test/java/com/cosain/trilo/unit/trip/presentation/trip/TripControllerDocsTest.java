package com.cosain.trilo.unit.trip.presentation.trip;

import com.cosain.trilo.common.file.ImageFile;
import com.cosain.trilo.support.RestDocsTestSupport;
import com.cosain.trilo.trip.application.day.dto.ScheduleSummary;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripController.class)
public class TripControllerDocsTest extends RestDocsTestSupport {

    @MockBean
    private TripCommandService tripCommandService;

    @MockBean
    private TripQueryService tripQueryService;

    private final String ACCESS_TOKEN = "Bearer accessToken";

    @Nested
    class 여행_생성{
        @Test
        @DisplayName("인증된 사용자의 여행 생성 요청 -> 성공")
        void successTest() throws Exception {
            // given
            Long tripperId= 1L;
            mockingForLoginUserAnnotation(tripperId); // 인증된 사용자 mocking 됨

            String rawTitle = "제목";
            var request = new TripCreateRequest(rawTitle);

            Long createdTripId = 1L;
            var command = TripCreateCommand.of(tripperId, rawTitle);
            given(tripCommandService.createTrip(eq(command))).willReturn(createdTripId); // 여행 생성 후 서비스에서 반환받을 여행 식별자 mocking

            // when
            ResultActions resultActions = runTest(createJson(request)); // 정상적으로 인증된 사용자가 요청했을 때

            // then
            resultActions
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tripId").value(createdTripId)); // 상태코드 및 응답 필드 검증

            verify(tripCommandService, times(1)).createTrip(eq(command)); // 내부 의존성 호출 검증


            // 문서화
            resultActions
                    .andDo(restDocs.document(
                            // 헤더 문서화
                            requestHeaders(
                                    headerWithName(HttpHeaders.AUTHORIZATION)
                                            .description("Bearer 타입 AccessToken")
                            ),
                            // 요청 필드 문서화
                            requestFields(
                                    fieldWithPath("title")
                                            .type(STRING)
                                            .description("여행의 제목")
                                            .attributes(key("constraints").value("null 또는 공백일 수 없으며, 길이는 1-20자까지만 허용됩니다."))
                            ),
                            // 응답 필드 문서화
                            responseFields(
                                    fieldWithPath("tripId")
                                            .type(NUMBER)
                                            .description("생성된 여행의 식별자(id)")
                            )
                    ));
        }

        private ResultActions runTest(String content) throws Exception {
            return mockMvc.perform(post("/api/trips")
                    .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                    .content(content)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .contentType(MediaType.APPLICATION_JSON));
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
                    .andExpect(jsonPath("$.tripId").value(tripId))
                    .andDo(restDocs.document(
                            requestHeaders(
                                    headerWithName(HttpHeaders.AUTHORIZATION)
                                            .description("Bearer 타입 AccessToken")
                            ),
                            pathParameters(
                                    parameterWithName("tripId")
                                            .description("제목 수정할 여행 ID")
                            ),
                            requestFields(
                                    fieldWithPath("title")
                                            .type(STRING)
                                            .description("여행 제목")
                                            .attributes(key("constraints").value("null 또는 공백일 수 없으며, 길이는 1-20자까지만 허용됩니다."))
                            ),
                            responseFields(
                                    fieldWithPath("tripId")
                                            .type(NUMBER)
                                            .description("수정된 여행 식별자(id)")
                            )
                    ));

            verify(tripCommandService, times(1)).updateTripTitle(eq(command));
        }
    }

    @Nested
    class 여행_이미지_수정{
        @Test
        @DisplayName("jpeg -> 성공")
        public void tripImageUpdateApiDocsTest() throws Exception {
            long tripperId= 2L;
            mockingForLoginUserAnnotation(tripperId);
            Long tripId = 1L;

            String name = "image";
            String fileName = "test-jpeg-image.jpeg";
            String filePath = "src/test/resources/testFiles/" + fileName;
            FileInputStream fileInputStream = new FileInputStream(filePath);
            String contentType = "image/jpeg";

            MockMultipartFile multipartFile = new MockMultipartFile(name, fileName, contentType, fileInputStream);
            TripImageUpdateCommand command = new TripImageUpdateCommand(tripId, tripperId, ImageFile.from(multipartFile));

            String imageURL = String.format("https://{이미지 파일 저장소 주소}/trips/%s/{이미지 파일명}.jpeg", tripId);
            given(tripCommandService.updateTripImage(any(TripImageUpdateCommand.class))).willReturn(imageURL);

            mockMvc.perform(multipart("/api/trips/{tripId}/image/update", tripId)
                            .file(multipartFile)
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tripId").value(tripId))
                    .andExpect(jsonPath("$.imageURL").value(imageURL))
                    .andDo(restDocs.document(
                            requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION)
                                    .description("Bearer 타입 AccessToken")
                            ),
                            pathParameters(
                                    parameterWithName("tripId")
                                            .description("이미지 수정할 여행(Trip)의 식별자(id)")
                            ),
                            requestParts(partWithName("image")
                                    .description("올릴 이미지 파일")
                                    .attributes(key("constraints").value("이미지를 필수로 전달해야합니다. 허용되는 이미지 타입(jpg, jpeg, png, gif, webp)"))
                            ),
                            responseFields(
                                    fieldWithPath("tripId")
                                            .type(NUMBER)
                                            .description("수정된 여행(trip) 식별자(id)"),
                                    fieldWithPath("imageURL")
                                            .type(STRING)
                                            .description("이미지가 저장된 URL(경로)")
                            )
                    ));

            verify(tripCommandService, times(1)).updateTripImage(any(TripImageUpdateCommand.class));
        }
    }

    @Nested
    class 여행_기간_수정{
        @Test
        @DisplayName("인증된 사용자 요청 -> 성공")
        public void updateTripPeriod_with_authorizedUser() throws Exception {
            // given
            long tripperId = 2L;
            mockingForLoginUserAnnotation(tripperId);

            Long tripId = 1L;
            LocalDate startDate = LocalDate.of(2023, 4, 1);
            LocalDate endDate = LocalDate.of(2023, 4, 5);

            var request = new TripPeriodUpdateRequest(startDate, endDate);
            var command = TripPeriodUpdateCommand.of(tripId, tripperId, startDate, endDate);

            // when
            ResultActions resultActions = runTest(tripId, createJson(request)); // 정상적으로 인증된 사용자가 요청했을 때

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tripId").value(tripId)); // 응답 메시지 검증

            verify(tripCommandService, times(1)).updateTripPeriod(eq(command)); // 내부 의존성 호출 횟수 검증

            // 문서화
            resultActions
                    .andDo(restDocs.document(
                            // 헤더 문서화
                            requestHeaders(
                                    headerWithName(HttpHeaders.AUTHORIZATION)
                                            .description("Bearer 타입 AccessToken")
                            ),
                            // 요청 경로변수 문서화
                            pathParameters(
                                    parameterWithName("tripId")
                                            .description("기간 수정할 여행 ID")
                            ),
                            // 요청 필드 문서화
                            requestFields(
                                    fieldWithPath("startDate")
                                            .type(STRING)
                                            .optional()
                                            .description("여행 시작 일자 (형식 : yyyy-MM-dd)")
                                            .attributes(key("constraints").value("startDate,endDate는 한쪽만 null이여선 안 되며(둘다 null은 가능), endDate가 startDate보다 앞서선 안 됩니다. 여행 일수는 최대 10일까지 허용됩니다.")),
                                    fieldWithPath("endDate")
                                            .type(STRING)
                                            .optional()
                                            .description("여행 종료 일자 (형식 : yyyy-MM-dd)")
                                            .attributes(key("constraints").value("startDate 참고"))
                            ),
                            // 응답 필드 문서화
                            responseFields(
                                    fieldWithPath("tripId")
                                            .type(NUMBER)
                                            .description("기간 수정된 여행 식별자(id)")
                            )
                    ));
        }
        private ResultActions runTest(Object tripId, String content) throws Exception {
            return mockMvc.perform(put("/api/trips/{tripId}/period", tripId)
                    .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                    .content(content)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .contentType(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    class 여행_삭제{
        @Test
        @DisplayName("인증된 사용자의 여행 삭제 요청 -> 성공")
        void tripDeleteDocTest() throws Exception {
            // given
            long requestTripperId = 2L;
            mockingForLoginUserAnnotation(requestTripperId);
            long tripId = 1L;

            // when
            ResultActions resultActions = runTest(tripId); // 인증된 사용자의 여행 삭제 요청

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isNoContent())
                    .andExpect(jsonPath("$").doesNotExist()); // 응답 메시지 검증

            verify(tripCommandService, times(1)).deleteTrip(eq(tripId), eq(requestTripperId)); // 내부 의존성 호출 검증

            resultActions
                    .andDo(restDocs.document(

                            requestHeaders(
                                    headerWithName(HttpHeaders.AUTHORIZATION)
                                            .description("Bearer 타입 AccessToken")
                            ),

                            pathParameters(
                                    parameterWithName("tripId")
                                            .description("삭제할 여행 식별자(id)")
                            )
                    ));
        }

        private ResultActions runTest(Object tripId) throws Exception {
            return mockMvc.perform(delete("/api/trips/{tripId}", tripId)
                    .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .contentType(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    class 여행_조회{

        private final String BASE_URL = "/api/trips";
        @Test
        void 여행_단건_조회() throws Exception{
            // given
            Long tripId = 1L;
            mockingForLoginUserAnnotation();
            TripDetail tripDetail = new TripDetail(tripId, 2L, "여행 제목", TripStatus.DECIDED, LocalDate.of(2023, 4, 4), LocalDate.of(2023, 4, 5));
            given(tripQueryService.searchTripDetail(anyLong())).willReturn(tripDetail);

            mockMvc.perform(RestDocumentationRequestBuilders.get(BASE_URL + "/{tripId}", tripId)
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
                                    parameterWithName("tripId").description("조회할 여행 ID")
                            ),
                            responseFields(
                                    fieldWithPath("tripId").type(NUMBER).description("여행 ID"),
                                    fieldWithPath("tripperId").type(NUMBER).description("여행자 ID"),
                                    fieldWithPath("title").type(STRING).description("여행 제목"),
                                    fieldWithPath("status").type(STRING).description("여행 상태"),
                                    fieldWithPath("startDate").type(STRING).description("여행 시작 날짜"),
                                    fieldWithPath("endDate").type(STRING).description("여행 끝 날짜")
                            )
                    ));

        }
    }

    @Nested
    class 여행_목록_조회{

        private final String BASE_URL = "/api/trips";
        @Test
        void 여행_목록_조회() throws Exception{

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
                    .andExpect(status().isOk())
                    .andDo(restDocs.document(
                            queryParameters(
                                    parameterWithName("tripId").optional().description("최신순 조회 시 기준이 되는 여행 ID"),
                                    parameterWithName("size").optional().description("가져올 데이터의 개수, 기본값 : 8"),
                                    parameterWithName("sortType").optional().description("정렬 기준 RECENT(기본값), LIKE"),
                                    parameterWithName("query").optional().description("검색어")
                            ),
                            responseFields(
                                    fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
                                    subsectionWithPath("trips").type(ARRAY).description("여행 목록")
                            )
                    )).andDo(restDocs.document(
                            responseFields(beneathPath("trips").withSubsectionId("trips"),
                                    fieldWithPath("tripId").type(NUMBER).description("여행 ID"),
                                    fieldWithPath("tripperId").type(NUMBER).description("여행자 ID"),
                                    fieldWithPath("title").type(STRING).description("여행 제목"),
                                    fieldWithPath("period").type(NUMBER).description("여행 기간"),
                                    fieldWithPath("imageURL").type(STRING).description("이미지가 저장된 URL(경로)")
                            )
                    ));
        }
    }

    @Nested
    class 사용자_여행_목록_조회{
        @Test
        void 사용자_여행_목록_조회() throws Exception{
            mockingForLoginUserAnnotation();

            Long tripperId = 1L;
            Long tripId = 5L;
            int size = 3;
            TripListSearchResult.TripSummary tripSummary1 = new TripListSearchResult.TripSummary(4L, tripperId, "제목 1", TripStatus.DECIDED, LocalDate.of(2023, 3,4), LocalDate.of(2023, 4, 1), "image.jpg");
            TripListSearchResult.TripSummary tripSummary2 = new TripListSearchResult.TripSummary(3L, tripperId, "제목 2", TripStatus.UNDECIDED, null, null, "image.jpg");
            TripListSearchResult.TripSummary tripSummary3 = new TripListSearchResult.TripSummary(2L, tripperId, "제목 3", TripStatus.DECIDED, LocalDate.of(2023, 4,4), LocalDate.of(2023, 4, 5), "image.jpg");
            TripListQueryParam queryParam = TripListQueryParam.of(tripperId, tripId, size);
            TripListSearchResult searchResult = TripListSearchResult.of(true, List.of(tripSummary1, tripSummary2, tripSummary3));

            given(tripQueryService.searchTripList(eq(queryParam))).willReturn(searchResult);

            // when & then
            mockMvc.perform(RestDocumentationRequestBuilders.get("/api/trippers/{tripperId}/trips", tripperId)
                            .param("tripId", String.valueOf(tripId))
                            .param("size", String.valueOf(size))
                            .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasNext").value(searchResult.isHasNext()))
                    .andExpect(jsonPath("$.trips").isNotEmpty())
                    .andExpect(jsonPath("$.trips.[0].tripId").value(tripSummary1.getTripId()))
                    .andExpect(jsonPath("$.trips.[1].tripId").value(tripSummary2.getTripId()))
                    .andExpect(jsonPath("$.trips.[2].tripId").value(tripSummary3.getTripId()))
                    .andExpect(jsonPath("$.trips.[0].title").value(tripSummary1.getTitle()))
                    .andExpect(jsonPath("$.trips.[1].title").value(tripSummary2.getTitle()))
                    .andExpect(jsonPath("$.trips.[2].title").value(tripSummary3.getTitle()))
                    .andExpect(jsonPath("$.trips.[0].status").value(tripSummary1.getStatus()))
                    .andExpect(jsonPath("$.trips.[1].status").value(tripSummary2.getStatus()))
                    .andExpect(jsonPath("$.trips.[2].status").value(tripSummary3.getStatus()))
                    .andExpect(jsonPath("$.trips.[0].startDate").value(tripSummary1.getStartDate().toString()))
                    .andExpect(jsonPath("$.trips.[1].startDate").doesNotExist())
                    .andExpect(jsonPath("$.trips.[2].startDate").value(tripSummary3.getStartDate().toString()))
                    .andExpect(jsonPath("$.trips.[0].endDate").value(tripSummary1.getEndDate().toString()))
                    .andExpect(jsonPath("$.trips.[1].endDate").doesNotExist())
                    .andExpect(jsonPath("$.trips.[2].endDate").value(tripSummary3.getEndDate().toString()))
                    .andExpect(jsonPath("$.trips.[0].imageURL").value(tripSummary1.getImageURL()))
                    .andExpect(jsonPath("$.trips.[1].imageURL").value(tripSummary2.getImageURL()))
                    .andExpect(jsonPath("$.trips.[2].imageURL").value(tripSummary3.getImageURL()))
                    .andDo(restDocs.document(
                            requestHeaders(
                                    headerWithName(HttpHeaders.AUTHORIZATION)
                                            .description("Bearer 타입 AccessToken")
                            ),
                            pathParameters(
                                    parameterWithName("tripperId").description("여행자 ID")
                            ),
                            queryParameters(
                                    parameterWithName("tripId").optional().description("기준이 되는 여행 ID (하단 설명 참고)"),
                                    parameterWithName("size").description("가져올 데이터의 개수")
                            ),
                            responseFields(
                                    fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
                                    subsectionWithPath("trips").type(ARRAY).description("여행 목록")
                            )
                    )).andDo(restDocs.document(
                            responseFields(beneathPath("trips").withSubsectionId("trips"),
                                    fieldWithPath("tripId").type(NUMBER).description("여행 ID"),
                                    fieldWithPath("tripperId").type(NUMBER).description("여행자 ID"),
                                    fieldWithPath("title").type(STRING).description("여행 제목"),
                                    fieldWithPath("status").type(STRING).description("여행 상태"),
                                    fieldWithPath("startDate").type(STRING).description("여행 시작 날짜"),
                                    fieldWithPath("endDate").type(STRING).description("여행 끝 날짜"),
                                    fieldWithPath("imageURL").type(STRING).description("이미지가 저장된 URL(경로)")
                            )
                    ));
        }
    }

    @Nested
    class 임시_보관함_조회{
        @Test
        void 임시보관함_조회() throws Exception{

            // given
            Long tripId = 1L;
            Long scheduleId = 1L;
            int size = 2;
            mockingForLoginUserAnnotation();
            ScheduleSummary scheduleSummary1 = new ScheduleSummary(2L, "제목", "장소 이름","장소 식별자", 33.33, 33.33);
            ScheduleSummary scheduleSummary2 = new ScheduleSummary(3L, "제목", "장소 이름","장소 식별자",33.33, 33.33);

            var queryParam = TempScheduleListQueryParam.of(tripId, scheduleId, size);
            var result = TempScheduleListSearchResult.of(true, List.of(scheduleSummary1, scheduleSummary2));
            given(tripQueryService.searchTemporary(eq(queryParam))).willReturn(result);

            mockMvc.perform(RestDocumentationRequestBuilders.get("/api/trips/{tripId}/temporary-storage", tripId)
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
                    .andExpect(jsonPath("$.tempSchedules[1].coordinate.longitude").value(scheduleSummary2.getCoordinate().getLongitude()))
                    .andDo(restDocs.document(
                            requestHeaders(
                                    headerWithName(HttpHeaders.AUTHORIZATION)
                                            .description("Bearer 타입 AccessToken")
                            ),
                            queryParameters(
                                    parameterWithName("scheduleId").optional().description("기준이 되는 일정 ID (하단 설명 참고)"),
                                    parameterWithName("size").description("가져올 데이터 개수")
                            ),
                            pathParameters(
                                    parameterWithName("tripId").description("조회할 여행 ID")
                            ),
                            responseFields(
                                    fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
                                    subsectionWithPath("tempSchedules").type(ARRAY).description("임시 일정 목록")
                            )
                    )).andDo(restDocs.document(
                            responseFields(beneathPath("tempSchedules").withSubsectionId("tempSchedules"),
                                    fieldWithPath("scheduleId").type(NUMBER).description("일정 ID"),
                                    fieldWithPath("title").type(STRING).description("제목"),
                                    fieldWithPath("placeName").type(STRING).description("장소 이름"),
                                    fieldWithPath("placeId").type(STRING).description("장소 식별자"),
                                    subsectionWithPath("coordinate").type(OBJECT).description("장소의 좌표")
                            )
                    )).andDo(restDocs.document(
                            responseFields(beneathPath("tempSchedules[].coordinate").withSubsectionId("coordinate"),
                                    fieldWithPath("latitude").type(NUMBER).description("위도"),
                                    fieldWithPath("longitude").type(NUMBER).description("경도")
                            )
                    ));

            verify(tripQueryService, times(1)).searchTemporary(eq(queryParam));
        }
    }
}
