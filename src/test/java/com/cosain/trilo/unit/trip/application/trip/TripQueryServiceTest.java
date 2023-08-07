package com.cosain.trilo.unit.trip.application.trip;

import com.cosain.trilo.common.exception.schedule.ScheduleNotFoundException;
import com.cosain.trilo.common.exception.trip.TripNotFoundException;
import com.cosain.trilo.fixture.UserFixture;
import com.cosain.trilo.trip.application.dao.ScheduleQueryDAO;
import com.cosain.trilo.trip.application.dao.TripQueryDAO;
import com.cosain.trilo.trip.application.day.service.day_search.ScheduleSummary;
import com.cosain.trilo.trip.application.exception.TripperNotFoundException;
import com.cosain.trilo.trip.application.trip.TripQueryService;
import com.cosain.trilo.trip.application.trip.dto.*;
import com.cosain.trilo.trip.domain.vo.TripStatus;
import com.cosain.trilo.trip.infra.adapter.TripImageOutputAdapter;
import com.cosain.trilo.trip.presentation.request.trip.TripSearchRequest;
import com.cosain.trilo.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TripQueryServiceTest {

    @InjectMocks
    private TripQueryService tripQueryService;

    @Mock
    private TripQueryDAO tripQueryDAO;

    @Mock
    private ScheduleQueryDAO scheduleQueryDAO;

    @Mock
    private TripImageOutputAdapter tripImageOutputAdapter;

    @Mock
    private UserRepository userRepository;

    @Nested
    class 임시_보관함_조회{
        @Test
        void tripId가_유효하고_임시보관함이_비어있지_않으면_페이지_요청의_크기만큼의_임시보관함_일정목록을_반환한다(){
            // given
            int size = 3;
            Long tripId = 1L;
            long scheduleId = 1L;

            ScheduleSummary scheduleSummary1 = new ScheduleSummary(2L, "일정 제목1", "제목","장소 식별자", 33.33, 33.33);
            ScheduleSummary scheduleSummary2 = new ScheduleSummary(3L, "일정 제목2", "제목","장소 식별자",33.33, 33.33);
            ScheduleSummary scheduleSummary3 = new ScheduleSummary(4L, "일정 제목3", "제목","장소 식별자",33.33, 33.33);

            var queryParam = TempScheduleListQueryParam.of(tripId, scheduleId, size);
            var result = TempScheduleListSearchResult.of(true, List.of(scheduleSummary1, scheduleSummary2, scheduleSummary3));

            given(tripQueryDAO.existById(eq(tripId))).willReturn(true);
            given(scheduleQueryDAO.existById(eq(scheduleId))).willReturn(true);
            given(scheduleQueryDAO.findTemporarySchedules(eq(queryParam))).willReturn(result);

            // when
            var returnResult = tripQueryService.searchTemporary(queryParam);

            // then
            assertThat(returnResult.isHasNext()).isEqualTo(result.isHasNext());
            assertThat(returnResult.getTempSchedules().size()).isEqualTo(3);
            assertThat(returnResult.getTempSchedules().get(0).getScheduleId()).isEqualTo(scheduleSummary1.getScheduleId());
            assertThat(returnResult.getTempSchedules().get(1).getScheduleId()).isEqualTo(scheduleSummary2.getScheduleId());
            assertThat(returnResult.getTempSchedules().get(2).getScheduleId()).isEqualTo(scheduleSummary3.getScheduleId());
        }
        @Test
        void tripId가_유효하지_않다면_TripNotFoundException을_발생시킨다(){
            // given
            long tripId = 1L;
            long scheduleId = 1L;
            int size = 3;

            var queryParam = TempScheduleListQueryParam.of(tripId, scheduleId, size);
            given(tripQueryDAO.existById(eq(tripId))).willReturn(false);

            // when & then
            assertThatThrownBy(() -> tripQueryService.searchTemporary(queryParam))
                    .isInstanceOf(TripNotFoundException.class);
        }

        @Test
        void scheduleId에_해당하는_Schedule이_존재하지_않는다면_ScheduleNotFoundException_에러를_발생시킨다(){
            // given
            long tripId = 1L;
            long scheduleId = 1L;
            int size = 3;

            var queryParam = TempScheduleListQueryParam.of(tripId, scheduleId, size);
            given(tripQueryDAO.existById(eq(tripId))).willReturn(true);
            given(scheduleQueryDAO.existById(eq(scheduleId))).willReturn(false);


            // when & then
            assertThatThrownBy(() -> tripQueryService.searchTemporary(queryParam))
                    .isInstanceOf(ScheduleNotFoundException.class);

        }
    }

    @Test
    void 여행_목록_조회_기능_호출테스트(){
        // given
        String imageName = "image.jpg";
        String imageURL = "https://.../image.jpg";
        TripSearchRequest tripSearchRequest = new TripSearchRequest("제주", "RECENT", 3, 1L);
        TripSearchResponse.TripSummary tripSummary1 = new TripSearchResponse.TripSummary(2L, 1L, LocalDate.of(2023, 4, 4), LocalDate.of(2023, 4, 10), "제주도 여행", imageName);
        TripSearchResponse.TripSummary tripSummary2 = new TripSearchResponse.TripSummary(1L, 1L, LocalDate.of(2023, 4, 4), LocalDate.of(2023, 4, 10), "제주 가보자", imageName);
        TripSearchResponse tripSearchResponse = new TripSearchResponse(true, List.of(tripSummary1, tripSummary2));

        given(tripQueryDAO.findWithSearchConditions(eq(tripSearchRequest))).willReturn(tripSearchResponse);
        given(tripImageOutputAdapter.getFullTripImageURL(eq(imageName))).willReturn(imageURL);

        // when
        tripQueryService.findBySearchConditions(tripSearchRequest);

        // then
        verify(tripQueryDAO, times(1)).findWithSearchConditions(eq(tripSearchRequest));
        verify(tripImageOutputAdapter, times(2)).getFullTripImageURL(eq(imageName));
    }

    @Nested
    class 여행_상세_조회_테스트{
        @Test
        @DisplayName("정상 호출 시에 호출 및 반환 결과 테스트")
        void called_test(){
            // given
            TripDetail tripDetail = new TripDetail(1L, 1L, "제목", TripStatus.DECIDED, LocalDate.of(2023, 5, 10), LocalDate.of(2023, 5, 15));
            given(tripQueryDAO.findTripDetailById(anyLong())).willReturn(Optional.of(tripDetail));

            // when
            TripDetail dto = tripQueryService.searchTripDetail( 1L);

            // then
            verify(tripQueryDAO).findTripDetailById(anyLong());
            assertThat(dto.getTripId()).isEqualTo(tripDetail.getTripId());
            assertThat(dto.getStatus()).isEqualTo(tripDetail.getStatus());
            assertThat(dto.getStartDate()).isEqualTo(tripDetail.getStartDate());
            assertThat(dto.getEndDate()).isEqualTo(tripDetail.getEndDate());
        }

        @Test
        @DisplayName("조회한 Trip이 없을 경우 TripNotFoundException 이 발생한다.")
        void when_the_trip_is_not_exist_is_throws_TripNotFoundException(){
            // given
            given(tripQueryDAO.findTripDetailById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tripQueryService.searchTripDetail( 1L))
                    .isInstanceOf(TripNotFoundException.class);
        }
    }

    @Nested
    class 여행_목록_조회_테스트{
        @Test
        @DisplayName("여행자(사용자) 여행 목록 조회 성공 테스트 : 의존성 호출 여부")
        void searchTripSummariesTest(){
            // given
            Long tripperId = 1L;
            Long standardTripId = 3L;
            int pageSize = 10;
            TripListQueryParam queryParam = TripListQueryParam.of(tripperId, standardTripId, pageSize);

//        Pageable pageable = PageRequest.of(0, 10);
            String imageName = "image.jpg";
            String imageURL = "https://.../image.jpg";
            TripListSearchResult.TripSummary tripSummary1 = new TripListSearchResult.TripSummary(2L, tripperId, "여행 1", TripStatus.DECIDED, LocalDate.of(2023,5,1), LocalDate.of(2023,5,1), imageName);
            TripListSearchResult.TripSummary tripSummary2 = new TripListSearchResult.TripSummary(1L, tripperId, "여행 2", TripStatus.UNDECIDED, null, null, imageName);
            TripListSearchResult result = TripListSearchResult.of(false, List.of(tripSummary1, tripSummary2));

            given(userRepository.findById(eq(tripperId))).willReturn(Optional.of(UserFixture.kakaoUser_Id(tripperId)));
            given(tripQueryDAO.findTripSummariesByTripperId(eq(queryParam))).willReturn(result);
            given(tripImageOutputAdapter.getFullTripImageURL(eq(imageName))).willReturn(imageURL);

            // when
            TripListSearchResult searchResult = tripQueryService.searchTripList(queryParam);

            // then
            assertThat(searchResult).isNotNull();
            assertThat(searchResult.getTrips()).hasSize(2);
            assertThat(searchResult.getTrips().get(0).getTripId()).isEqualTo(tripSummary1.getTripId());
            assertThat(searchResult.getTrips().get(1).getTripId()).isEqualTo(tripSummary2.getTripId());
            assertThat(searchResult.getTrips()).map(TripListSearchResult.TripSummary::getImageURL).allMatch(url -> url.equals(imageURL));
            assertThat(searchResult.isHasNext()).isFalse();

            verify(userRepository, times(1)).findById(eq(tripperId));
            verify(tripQueryDAO, times(1)).findTripSummariesByTripperId(eq(queryParam));
            verify(tripImageOutputAdapter, times(2)).getFullTripImageURL(anyString());
        }

        @Test
        @DisplayName("tripperId에 해당하는 사용자가 존재하지 않으면 TripperNotFoundException 예외 발생")
        void when_the_user_is_not_exist_that_coincide_with_tripper_id_it_will_throws_TripperNotFoundException(){
            // given
            Long tripperId = 1L;
            Long tripId = 2L;
            int pageSize = 10;
            TripListQueryParam queryParam = TripListQueryParam.of(tripperId, tripId, pageSize);
            given(userRepository.findById(tripperId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tripQueryService.searchTripList(queryParam))
                    .isInstanceOf(TripperNotFoundException.class);
        }
    }

}
