package ru.practicum.explore.main.event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import ru.practicum.explore.main.event.model.Event;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    @EntityGraph(attributePaths = {"category", "initiator"})
    Optional<Event> findById(Long id);

    Optional<Event> findByIdAndInitiatorId(long eventId, long userId);

    List<Event> findAllByInitiatorId(long userId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Event e SET e.views = :views WHERE e.id = :eventId")
    void incrementViews(Long eventId, Long views);

    boolean existsByCategoryId(Long categoryId);
}
