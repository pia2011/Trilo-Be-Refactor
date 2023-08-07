package com.cosain.trilo.unit.trip.application.trip;

import com.cosain.trilo.common.exception.trip.NoTripDeleteAuthorityException;
import com.cosain.trilo.common.exception.trip.NoTripUpdateAuthorityException;
import com.cosain.trilo.common.exception.trip.TripNotFoundException;
import com.cosain.trilo.common.file.ImageFile;
import com.cosain.trilo.fixture.TripFixture;
import com.cosain.trilo.trip.application.trip.TripCommandService;
import com.cosain.trilo.trip.application.trip.dto.TripCreateCommand;
import com.cosain.trilo.trip.application.trip.dto.TripImageUpdateCommand;
import com.cosain.trilo.trip.application.trip.dto.TripPeriodUpdateCommand;
import com.cosain.trilo.trip.application.trip.dto.TripTitleUpdateCommand;
import com.cosain.trilo.trip.domain.entity.Trip;
import com.cosain.trilo.trip.domain.repository.DayRepository;
import com.cosain.trilo.trip.domain.repository.ScheduleRepository;
import com.cosain.trilo.trip.domain.repository.TripRepository;
import com.cosain.trilo.trip.domain.vo.TripImage;
import com.cosain.trilo.trip.infra.adapter.TripImageOutputAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TripCommandServiceTest {

    @InjectMocks
    private TripCommandService tripCommandService;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private DayRepository dayRepository;

    @Mock
    private TripImageOutputAdapter tripImageOutputAdapter;

    @Nested
    class 여행_삭제_테스트{
        @Test
        @DisplayName("정상 삭제 요청이 들어왔을 때, 리포지토리가 정상적으로 호출되는 지 여부 테스트")
        public void deleteSuccess_and_Repository_Called_Test() {
            // given
            Long tripId = 1L;
            Long requestTripperId = 2L;

            Trip trip = TripFixture.undecided_Id(tripId, requestTripperId);
            given(tripRepository.findById(eq(tripId))).willReturn(Optional.of(trip)); // 리포지토리에서 여행 조회시 사용자의 여행이 조회됨

            // when
            tripCommandService.deleteTrip(tripId, requestTripperId); // 여행 소유자가 여행 삭제 요청

            // then
            verify(tripRepository, times(1)).findById(eq(tripId));
            verify(scheduleRepository, times(1)).deleteAllByTripId(eq(tripId));
            verify(dayRepository, times(1)).deleteAllByTripId(eq(tripId));
            verify(tripRepository, times(1)).delete(any(Trip.class)); // 의존성 호출 검증
        }

        /**
         * <p>존재하지 않는 여행삭제 요청을 했을 때 예외가 발생되는 지 검증합니다.</p>
         * <ul>
         *     <li>{@link TripNotFoundException} 예외가 발생해야합니다.</li>
         *     <li>여행을 조회하는 부분까지만 의존성이 호출되어야 합니다.</li>
         * </ul>
         * @see TripNotFoundException
         */
        @Test
        @DisplayName("존재하지 않는 여행을 삭제하려 하면, TripNotFoundException 발생")
        public void if_delete_not_exist_trip_then_it_throws_TripNotFoundException() {
            // given
            Long tripId = 1L;
            Long tripperId = 1L;
            given(tripRepository.findById(eq(tripId))).willReturn(Optional.empty()); // 리포지토리에서 여행 빈 Optional이 반환됨

            // when & then
            assertThatThrownBy(() -> tripCommandService.deleteTrip(tripId, tripperId))
                    .isInstanceOf(TripNotFoundException.class); // 존재하지 않는 여행 삭제 요청 -> 예외 발생 검증

            verify(tripRepository, times(1)).findById(eq(tripId));
            verify(scheduleRepository, times(0)).deleteAllByTripId(eq(tripId));
            verify(dayRepository, times(0)).deleteAllByTripId(eq(tripId));
            verify(tripRepository, times(0)).delete(any(Trip.class)); // 의존성 호출 검증
        }

        /**
         * <p>권한이 없는 사용자가 여행삭제 요청을 했을 때 예외가 발생되는 지 검증합니다.</p>
         * <ul>
         *     <li>{@link NoTripDeleteAuthorityException} 예외가 발생해야합니다.</li>
         *     <li>여행을 조회하는 부분까지만 의존성이 호출되어야 합니다.</li>
         * </ul>
         * @see NoTripDeleteAuthorityException
         */
        @Test
        @DisplayName("다른 사람이 여행 삭제 요청 -> 예외 발생")
        public void noTripDeleteAuthorityTest() {
            // given
            Long tripId = 1L;
            Long tripOwnerId = 1L;
            Long invalidTripperId = 2L;

            Trip trip = TripFixture.undecided_Id(tripId, tripOwnerId);
            given(tripRepository.findById(eq(tripId))).willReturn(Optional.of(trip));

            // when & then
            assertThatThrownBy(() -> tripCommandService.deleteTrip(tripId, invalidTripperId)) // 여행 소유자가 아님 -> 예외 발생
                    .isInstanceOf(NoTripDeleteAuthorityException.class);

            verify(tripRepository, times(1)).findById(eq(tripId));
            verify(scheduleRepository, times(0)).deleteAllByTripId(eq(tripId));
            verify(dayRepository, times(0)).deleteAllByTripId(eq(tripId));
            verify(tripRepository, times(0)).delete(any(Trip.class)); // 의존성 호출 검증
        }

    }

    @Nested
    class 여행_생성_테스트{
        @Test
        @DisplayName("create 하면, 내부적으로 repository가 호출된다.")
        public void create_and_repository_called() {
            // given
            Long tripId = 1L;
            Long requestTripperId = 2L;
            var command = TripCreateCommand.of(requestTripperId, "제목");

            Trip savedTrip = TripFixture.undecided_Id(tripId, requestTripperId);
            given(tripRepository.save(any(Trip.class))).willReturn(savedTrip); // 리포지토리에서 가져올 저장된 여행 mocking

            // when
            Long returnTripId = tripCommandService.createTrip(command);

            // then
            verify(tripRepository, times(1)).save(any(Trip.class));
            assertThat(returnTripId).isEqualTo(tripId);
        }
    }

    @Nested
    class 여행_제목_수정_테스트{
        @Test
        @DisplayName("여행 제목 변경 성공 테스트")
        public void successTest() {
            // given
            Long tripId = 1L;
            Long tripperId = 2L;

            String beforeTitle = "여행 제목";
            String requestTitle = "수정 여행 제목";

            var command = TripTitleUpdateCommand.of(tripId, tripperId, requestTitle);

            Trip trip = TripFixture.undecided_Id_Title(tripId, tripperId, beforeTitle);

            given(tripRepository.findById(eq(tripId))).willReturn(Optional.of(trip));

            // when
            tripCommandService.updateTripTitle(command);

            // then
            verify(tripRepository, times(1)).findById(eq(tripId));
            assertThat(trip.getTripTitle().getValue()).isEqualTo(requestTitle);
        }

        @Test
        @DisplayName("여행 존재 안 함 -> TripNotFoundException 발생")
        public void if_update_not_exist_trip_tripTitle_then_it_throws_TripNotFoundException() {
            // given
            Long tripId = 1L;
            Long tripperId = 2L;

            String requestTitle = "수정 여행 제목";
            var command = TripTitleUpdateCommand.of(tripId, tripperId, requestTitle);

            // mock
            given(tripRepository.findById(eq(tripId))).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tripCommandService.updateTripTitle(command))
                    .isInstanceOf(TripNotFoundException.class);
            verify(tripRepository, times(1)).findById(eq(tripId));
        }

        @DisplayName("여행의 소유자가 아닌 사람 -> NoTripUpdateAuthorityException 발생")
        @Test
        void noTripUpdateAuthorityTest() {
            // given
            Long tripId = 1L;
            Long realTripOwnerId = 2L;
            Long noAuthorityTripperId = 4L;

            String beforeTitle = "여행 제목";
            String requestTitle = "수정 여행 제목";

            var command = TripTitleUpdateCommand.of(tripId, noAuthorityTripperId, requestTitle);
            Trip trip = TripFixture.undecided_Id_Title(tripId, realTripOwnerId, beforeTitle);

            given(tripRepository.findById(eq(tripId))).willReturn(Optional.of(trip));

            // when & then
            assertThatThrownBy(() -> tripCommandService.updateTripTitle(command))
                    .isInstanceOf(NoTripUpdateAuthorityException.class);

            verify(tripRepository, times(1)).findById(eq(tripId));
        }
    }

    @Nested
    class 여행_기간_변경_테스트{
        @Test
        @DisplayName("기간이 정해진 여행을 다른 날짜의 기간으로 수정 -> 성공")
        public void testDecidedTripPeriodToOtherTripPeriod() throws Exception {
            // given
            long tripId = 1L;
            Long tripperId = 2L;

            LocalDate beforeStartDate = LocalDate.of(2023,3,1);
            LocalDate beforeEndDate = LocalDate.of(2023,3,4);
            LocalDate newStartDate = LocalDate.of(2023,3,2);
            LocalDate newEndDate = LocalDate.of(2023,3,5);

            var command = TripPeriodUpdateCommand.of(tripId, tripperId, newStartDate, newEndDate);

            Trip trip = TripFixture.decided_Id(tripId, tripperId, beforeStartDate, beforeEndDate, 1L);

            given(tripRepository.findByIdWithDays(eq(tripId))).willReturn(Optional.of(trip)); // trip 조회 일어남.
            given(scheduleRepository.relocateDaySchedules(eq(tripId), isNull())).willReturn(0);
            given(scheduleRepository.moveSchedulesToTemporaryStorage(eq(tripId), anyList())).willReturn(0);
            given(dayRepository.deleteAllByIds(anyList())).willReturn(2);

            // when
            tripCommandService.updateTripPeriod(command);

            // then
            // 여행 기간 수정을 위해 여행이 조회됨
            verify(tripRepository, times(1)).findByIdWithDays(eq(tripId));

            // 생성된 Day가 있으므로 재배치 기능 호출됨
            verify(dayRepository, times(1)).saveAll(anyList());

            // Day 삭제되고 일정들의 임시보관함 이동을 위해 임시보관함 재배치가 일어남
            verify(scheduleRepository, times(1)).relocateDaySchedules(eq(tripId), isNull());

            // 일정들이 일괄적으로 임시보관함으로 이동됨
            verify(scheduleRepository, times(1)).moveSchedulesToTemporaryStorage(eq(tripId), anyList());

            // 새로운 기간에 속하지 않는 Day들이 일괄 삭제됨
            verify(dayRepository, times(1)).deleteAllByIds(anyList());
        }

        /**
         * 초기화되지 않은 여행을 새로운 기간으로 수정(초기화)할 때에 대한 테스트입니다.
         * <ul>
         *     <li>내부 의존성이 잘 호출됐는 지 검증해야합니다.</li>
         * </ul>
         */
        @Test
        @DisplayName("기간이 정해지지 않은 여행의 기간을 새로운 기간으로 수정 -> 기간 수정됨")
        public void unDecidedTripPeriod_initTest() throws Exception {
            // given
            Long tripId = 1L;
            Long tripperId = 2L;

            LocalDate startDate = LocalDate.of(2023, 3, 1);
            LocalDate endDate = LocalDate.of(2023, 3, 3);

            TripPeriodUpdateCommand command = TripPeriodUpdateCommand.of(tripId, tripperId, startDate, endDate);

            Trip trip = TripFixture.undecided_Id(tripId, tripperId);
            given(tripRepository.findByIdWithDays(eq(tripId))).willReturn(Optional.of(trip));

            // when
            tripCommandService.updateTripPeriod(command);

            // then

            // 수정할 여행에 대한 조회가 일어남.
            verify(tripRepository, times(1)).findByIdWithDays(eq(tripId));

            // Day 생성을 위해 리포지토리가 호출됨
            verify(dayRepository, times(1)).saveAll(anyList());

            // 삭제되는 Day가 없으므로 임시보관함 재배치는 일어나지 않음
            verify(scheduleRepository, times(0)).relocateDaySchedules(eq(tripId), isNull());

            // 삭제되는 Day 자체가 없으므로 임시보관함으로의 일정 이동도 일어나지 않음
            verify(scheduleRepository, times(0)).moveSchedulesToTemporaryStorage(eq(tripId), anyList());

            // 삭제되는 Day들이 없음
            verify(dayRepository, times(0)).deleteAllByIds(anyList());
        }

        /**
         * <p>존재하지 않는 여행기간 수정 요청을 했을 때 예외가 발생되는 지 검증합니다.</p>
         * <ul>
         *     <li>{@link TripNotFoundException} 예외가 발생해야합니다.</li>
         *     <li>여행을 조회하는 부분까지만 의존성이 호출되어야 합니다.</li>
         * </ul>
         * @see TripNotFoundException
         */
        @Test
        @DisplayName("존재하지 않는 여행의 기간을 수정하려 하면, TripNotFoundException 발생")
        public void if_update_not_exist_trip_then_it_throws_TripNotFoundException() {
            // given
            Long tripId = 1L;
            Long tripperId = 2L;

            LocalDate startDate = LocalDate.of(2023, 3, 1);
            LocalDate endDate = LocalDate.of(2023, 3, 3);

            var command = TripPeriodUpdateCommand.of(tripId, tripperId, startDate, endDate);
            given(tripRepository.findByIdWithDays(eq(tripId))).willReturn(Optional.empty()); // 여행 존재 x

            // when & then
            assertThatThrownBy(() -> tripCommandService.updateTripPeriod(command))
                    .isInstanceOf(TripNotFoundException.class);
            verify(tripRepository, times(1)).findByIdWithDays(eq(tripId));
        }

        /**
         * <p>권한이 없는 사용자가 여행기간 수정 요청을 했을 때 예외가 발생되는 지 검증합니다.</p>
         * <ul>
         *     <li>{@link NoTripUpdateAuthorityException} 예외가 발생해야합니다.</li>
         *     <li>여행을 조회하는 부분까지만 의존성이 호출되어야 합니다.</li>
         * </ul>
         * @see NoTripUpdateAuthorityException
         */
        @Test
        @DisplayName("다른 사람이 여행 기간 수정 요청 -> 예외 발생")
        void noTripUpdateAuthorityTest() {
            // given
            Long tripId = 1L;
            Long tripperId = 2L;
            Long noAuthorityTripperId = 3L;

            // 다른 사람의 여행 기간 수정 요청
            var command = TripPeriodUpdateCommand.of(tripId, noAuthorityTripperId, null, null);

            Trip trip = TripFixture.undecided_Id(tripId, tripperId);
            given(tripRepository.findByIdWithDays(eq(tripId))).willReturn(Optional.of(trip));

            // when & then
            assertThatThrownBy(() -> tripCommandService.updateTripPeriod(command))
                    .isInstanceOf(NoTripUpdateAuthorityException.class);

            // 여행 조회를 위해 리포지토리 1번 조회됨
            verify(tripRepository, times(1)).findByIdWithDays(eq(tripId));
        }

    }

    @Nested
    class 여행_이미지_변경_테스트{
        private static final String TEST_RESOURCE_PATH = "src/test/resources/testFiles/";
        @DisplayName("여행 이미지 변경 성공 테스트")
        @Test
        public void successTest() throws Exception {
            // given
            Long tripId = 1L;
            Long tripperId = 2L;
            ImageFile imageFile = imageFileFixture("test-jpeg-image.jpeg");
            TripImageUpdateCommand command = new TripImageUpdateCommand(tripId, tripperId, imageFile);

            Trip trip = TripFixture.undecided_Id(tripId, tripperId);
            given(tripRepository.findById(eq(tripId))).willReturn(Optional.of(trip));

            willDoNothing().given(tripImageOutputAdapter).uploadImage(any(ImageFile.class), anyString());

            String fullPath = String.format("https://{여행 이미지 저장소}/trips/%d/{uuid 파일명}.jpeg", tripId);
            given(tripImageOutputAdapter.getFullTripImageURL(anyString())).willReturn(fullPath);

            // when
            String returnFullPath = tripCommandService.updateTripImage(command);

            // then
            assertThat(trip.getTripImage()).isNotEqualTo(TripImage.defaultImage());
            assertThat(returnFullPath).isEqualTo(fullPath);
            verify(tripRepository, times(1)).findById(eq(tripId));
            verify(tripImageOutputAdapter, times(1)).uploadImage(any(ImageFile.class), anyString());
            verify(tripImageOutputAdapter, times(1)).getFullTripImageURL(anyString());
        }

        @DisplayName("일치하는 식별자의 여행이 없으면 -> TripNotFoundException")
        @Test
        public void tripNotFoundTest() throws Exception {
            // given
            Long tripId = 1L;
            Long tripperId = 2L;
            ImageFile imageFile = imageFileFixture("test-jpeg-image.jpeg");
            TripImageUpdateCommand command = new TripImageUpdateCommand(tripId, tripperId, imageFile);

            given(tripRepository.findById(eq(tripId))).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->tripCommandService.updateTripImage(command))
                    .isInstanceOf(TripNotFoundException.class);
            verify(tripRepository, times(1)).findById(eq(tripId));
            verify(tripImageOutputAdapter, times(0)).uploadImage(any(ImageFile.class), anyString());
        }

        @DisplayName("수정할 권한 없는 사용자 -> NoTripUpdateAuthorityException")
        @Test
        public void testNoTripUpdateAuthorityTripper() throws Exception {
            // given
            Long tripId = 1L;
            Long tripperId = 2L;
            Long invalidTripperId = 3L;
            ImageFile imageFile = imageFileFixture("test-jpeg-image.jpeg");

            TripImageUpdateCommand command = new TripImageUpdateCommand(tripId, invalidTripperId, imageFile);

            Trip trip = TripFixture.undecided_Id(tripId, tripperId);
            given(tripRepository.findById(eq(tripId))).willReturn(Optional.of(trip));

            // when & then
            assertThatThrownBy(() ->tripCommandService.updateTripImage(command))
                    .isInstanceOf(NoTripUpdateAuthorityException.class);
            verify(tripRepository, times(1)).findById(eq(tripId));
            verify(tripImageOutputAdapter, times(0)).uploadImage(any(ImageFile.class), anyString());
        }


        private ImageFile imageFileFixture(String testImageResourceFileName) throws IOException {
            String name = "image";
            String filePath = TEST_RESOURCE_PATH + testImageResourceFileName;
            FileInputStream fileInputStream = new FileInputStream(filePath);
            String contentType = "image/jpeg";

            MockMultipartFile multipartFile = new MockMultipartFile(name, testImageResourceFileName, contentType, fileInputStream);
            return ImageFile.from(multipartFile);
        }
    }
}
