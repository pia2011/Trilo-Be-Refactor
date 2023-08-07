package com.cosain.trilo.trip.application.trip;

import com.cosain.trilo.user.application.event.UserDeleteEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TripEventListener {

    private final TripCommandService tripCommandService;

    @Async("threadPoolTaskExecutor")
    @TransactionalEventListener
    public void handle(UserDeleteEvent event){
        Long tripperId = event.getUserId();
        tripCommandService.deleteAllByTripperId(tripperId);
    }
}
