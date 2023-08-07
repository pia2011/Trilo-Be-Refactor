package com.cosain.trilo.trip.application.trip;

import com.cosain.trilo.common.exception.trip.EmptyPeriodUpdateException;
import com.cosain.trilo.common.exception.trip.NoTripDeleteAuthorityException;
import com.cosain.trilo.common.exception.trip.NoTripUpdateAuthorityException;
import com.cosain.trilo.common.exception.trip.TripNotFoundException;
import com.cosain.trilo.common.file.ImageFile;
import com.cosain.trilo.trip.application.exception.TripImageUploadFailedException;
import com.cosain.trilo.trip.application.trip.dto.TripCreateCommand;
import com.cosain.trilo.trip.application.trip.dto.TripImageUpdateCommand;
import com.cosain.trilo.trip.application.trip.dto.TripPeriodUpdateCommand;
import com.cosain.trilo.trip.application.trip.dto.TripTitleUpdateCommand;
import com.cosain.trilo.trip.domain.entity.Day;
import com.cosain.trilo.trip.domain.entity.Trip;
import com.cosain.trilo.trip.domain.repository.DayRepository;
import com.cosain.trilo.trip.domain.repository.ScheduleRepository;
import com.cosain.trilo.trip.domain.repository.TripRepository;
import com.cosain.trilo.trip.domain.vo.TripImage;
import com.cosain.trilo.trip.domain.vo.TripPeriod;
import com.cosain.trilo.trip.infra.adapter.TripImageOutputAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TripCommandService {

    private final TripRepository tripRepository;
    private final ScheduleRepository scheduleRepository;
    private final DayRepository dayRepository;
    private final TripImageOutputAdapter tripImageOutputAdapter;

    public Long createTrip(TripCreateCommand command) {
        Trip trip = Trip.create(command.getTripTitle(), command.getTripperId());
        Trip savedTrip = tripRepository.save(trip);
        return savedTrip.getId();
    }

    public void deleteAllByTripperId(Long tripperId){
        List<Trip> trips = tripRepository.findAllByTripperId(tripperId);
        List<Long> tripIdsForDelete = trips.stream().map(Trip::getId).collect(Collectors.toList());
        scheduleRepository.deleteAllByTripIds(tripIdsForDelete);
        dayRepository.deleteAllByTripIds(tripIdsForDelete);
        tripRepository.deleteAllByTripperId(tripperId);
    }

    public void deleteTrip(Long tripId, Long requestTripperId) throws TripNotFoundException, NoTripDeleteAuthorityException {
        Trip trip = findTrip(tripId);
        validateTripDeleteAuthority(trip, requestTripperId);

        scheduleRepository.deleteAllByTripId(tripId);
        dayRepository.deleteAllByTripId(tripId);
        tripRepository.delete(trip);
    }

    private Trip findTrip(Long tripId) throws TripNotFoundException{
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException("여행 삭제 시도 -> 일치하는 식별자의 여행을 찾지 못 함"));
    }
    private void validateTripDeleteAuthority(Trip trip, Long requestTripperId) throws NoTripDeleteAuthorityException {
        if (!trip.getTripperId().equals(requestTripperId)) {
            throw new NoTripDeleteAuthorityException("여행 삭제 시도 -> 삭제할 권한 없음");
        }
    }

    public String updateTripImage(TripImageUpdateCommand command)
            throws TripNotFoundException, NoTripUpdateAuthorityException, TripImageUploadFailedException {
        Long tripId = command.getTripId();
        Long requestTripperId = command.getRequestTripperId();
        ImageFile imageFile = command.getImageFile();

        Trip trip = findTrip(tripId);

        validateTripUpdateAuthority(trip, requestTripperId); // 이미지를 수정할 권한이 있는 지 검증

        String uploadName = makeUploadFileName(tripId, imageFile); // 이미지 저장소에 올릴 이름 구성
        tripImageOutputAdapter.uploadImage(imageFile, uploadName); // 이미지 저장소에 업로드 후, 전체 이미지 경로(fullPath)를 구성

        trip.changeImage(TripImage.of(uploadName)); // 여행이미지 도메인의 실제 이미지 변경
        return tripImageOutputAdapter.getFullTripImageURL(uploadName); // 이미지 전체 경로를 반환
    }

    private void validateTripUpdateAuthority(Trip trip, Long tripperId) throws NoTripUpdateAuthorityException {
        if (!trip.getTripperId().equals(tripperId)) {
            throw new NoTripUpdateAuthorityException();
        }
    }
    private static String makeUploadFileName(Long tripId, ImageFile file) {
        return String.format("trips/%d/%s.%s",
                tripId, UUID.randomUUID(), file.getExt());
    }

    public void updateTripPeriod(TripPeriodUpdateCommand command)
            throws TripNotFoundException, NoTripUpdateAuthorityException, EmptyPeriodUpdateException {

        Trip trip = findTripWithDays(command.getTargetTripId());

        validateTripUpdateAuthority(trip, command.getRequestTripperId());

        changePeriod(trip, command.getTripPeriod());
    }
    private Trip findTripWithDays(Long targetTripId) throws TripNotFoundException {
        return tripRepository.findByIdWithDays(targetTripId)
                .orElseThrow(() -> new TripNotFoundException("일치하는 식별자의 Trip을 찾을 수 없음"));
    }
    private void changePeriod(Trip trip, TripPeriod newPeriod) throws EmptyPeriodUpdateException {

        var changePeriodResult = trip.changePeriod(newPeriod);

        List<Day> createdDays = changePeriodResult.getCreatedDays();
        List<Long> deletedDayIds = changePeriodResult.getDeletedDayIds();

        if (!createdDays.isEmpty()) {
            dayRepository.saveAll(createdDays);
        }
        if (!deletedDayIds.isEmpty()) {
            scheduleRepository.relocateDaySchedules(trip.getId(), null);
            scheduleRepository.moveSchedulesToTemporaryStorage(trip.getId(), deletedDayIds);
            dayRepository.deleteAllByIds(deletedDayIds);
        }
    }

    public void updateTripTitle(TripTitleUpdateCommand command) {
        Trip trip = findTrip(command.getTripId());
        validateTripUpdateAuthority(trip, command.getRequestTripperId());

        trip.changeTitle(command.getTripTitle());
    }
}
