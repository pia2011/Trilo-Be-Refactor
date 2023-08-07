package com.cosain.trilo.trip.infra.dao.querydsl;

import com.cosain.trilo.trip.application.trip.dto.*;
import com.cosain.trilo.trip.application.trip.dto.QTripDetail;
import com.cosain.trilo.trip.application.trip.dto.QTripListSearchResult_TripSummary;
import com.cosain.trilo.trip.application.trip.dto.QTripSearchResponse_TripSummary;
import com.cosain.trilo.trip.domain.vo.TripStatus;
import com.cosain.trilo.trip.infra.dto.QTripStatistics;
import com.cosain.trilo.trip.infra.dto.TripStatistics;
import com.cosain.trilo.trip.presentation.request.trip.TripSearchRequest;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.cosain.trilo.trip.domain.entity.QTrip.trip;


@Component
@RequiredArgsConstructor
public class QuerydslTripQueryRepository {

    private final JPAQueryFactory query;
    public Optional<TripDetail> findTripDetailById(Long tripId) {
        return Optional.ofNullable(query.select(new QTripDetail(trip.id, trip.tripperId, trip.tripTitle.value, trip.status, trip.tripPeriod.startDate, trip.tripPeriod.endDate))
                .from(trip)
                .where(trip.id.eq(tripId))
                .fetchOne());
    }

    public TripListSearchResult findTripSummariesByTripperId(TripListQueryParam queryParam) {
        List<TripListSearchResult.TripSummary> result = query.select(new QTripListSearchResult_TripSummary(trip.id, trip.tripperId, trip.tripTitle.value, trip.status, trip.tripPeriod.startDate, trip.tripPeriod.endDate, trip.tripImage.fileName))
                .from(trip)
                .where(
                        trip.tripperId.eq(queryParam.getTripperId()),
                        ltTripId(queryParam.getTripId())
                )
                .orderBy(trip.id.desc())
                .limit(queryParam.getPageSize() + 1)
                .fetch();

        boolean hasNext = isHasNext(result, queryParam.getPageSize());
        return TripListSearchResult.of(hasNext, result);
    }

    private boolean isHasNext(List<?> result, int pageSize) {
        boolean hasNext = false;
        if (result.size() > pageSize) {
            hasNext = true;
            result.remove(pageSize);
        }
        return hasNext;
    }

    private BooleanExpression ltTripId(Long tripId) {
        return tripId == null ? null : trip.id.lt(tripId);
    }

    public boolean existById(Long tripId) {
        Integer fetchOne = query.selectOne()
                .from(trip)
                .where(trip.id.eq(tripId))
                .fetchFirst();

        return fetchOne != null;
    }

    public TripStatistics findTripStaticsByTripperId(Long tripperId, LocalDate today) {
        JPQLQuery<Long> subQuery = JPAExpressions.select(trip.count())
                .from(trip)
                .where(trip.tripPeriod.endDate.before(today));

        TripStatistics tripStatistics = query.select(new QTripStatistics(trip.count(), subQuery))
                .from(trip)
                .where(trip.tripperId.eq(tripperId))
                .fetchOne();

        return tripStatistics;
    }

    public TripSearchResponse findTripWithSearchCondition(TripSearchRequest request){

        List<TripSearchResponse.TripSummary> result = query.select(new QTripSearchResponse_TripSummary(trip.id, trip.tripperId, trip.tripPeriod.startDate, trip.tripPeriod.endDate, trip.tripTitle.value, trip.tripImage.fileName))
                .from(trip)
                .where(
                        decideOrFinished(),
                        containsQuery(request.getQuery()),
                        cursor(request.getSortType(), request.getTripId())
                )
                .orderBy(makeOrderSpecifiers(request.getSortType()))
                .limit(request.getSize() + 1)
                .fetch();

        boolean hasNext = isHasNext(result, request.getSize());
        return TripSearchResponse.of(hasNext, result);
    }

    /**
     * 동적 정렬
     * 최신순 : default
     * TODO : 좋아요 많은 순
     */
    private OrderSpecifier<?> makeOrderSpecifiers(TripSearchRequest.SortType sortType){
        return switch (sortType) {
            case RECENT -> new OrderSpecifier<>(Order.DESC, trip.id);
            default -> new OrderSpecifier<>(Order.DESC, trip.id);
        };
    }

    private BooleanExpression cursor(TripSearchRequest.SortType sortType, Long tripId) {
        return switch (sortType) {
            case RECENT -> ltTripId(tripId);
            default -> ltTripId(tripId);
        };
    }


    private BooleanExpression decideOrFinished(){
        return trip.status.in(TripStatus.DECIDED, TripStatus.FINISHED);
    }

    private BooleanExpression containsQuery(String query){
        return query == null ? null : trip.tripTitle.value.contains(query);
    }
}
