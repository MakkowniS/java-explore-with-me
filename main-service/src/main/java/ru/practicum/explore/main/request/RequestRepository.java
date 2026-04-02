package ru.practicum.explore.main.request;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.explore.main.request.model.ParticipationRequest;

import java.util.List;

public interface RequestRepository  extends JpaRepository<ParticipationRequest,Long> {

    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    // Проверка на дубликат
    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    // Получение списка заявок на конкретное событие (для автора события)
    List<ParticipationRequest> findAllByEventId(Long eventId);

    // Массовое обновление статусов
    List<ParticipationRequest> findAllByIdIn(List<Long> ids);

}
