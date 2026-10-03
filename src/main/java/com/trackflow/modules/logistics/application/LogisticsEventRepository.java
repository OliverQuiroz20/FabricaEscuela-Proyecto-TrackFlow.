package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.LogisticsEvent;
import java.util.List;
import java.util.Optional;

public interface LogisticsEventRepository {

    LogisticsEvent save(LogisticsEvent event);

    List<LogisticsEvent> findHistorial(String trackingNumber);

    List<LogisticsEvent> findTodosCronologicamente();

    boolean existePorEventId(String eventId);

    Optional<LogisticsEvent> porEventId(String eventId);
}
