package ru.practicum.explore.main.event;

import org.springframework.data.jpa.domain.Specification;
import ru.practicum.explore.main.event.model.Event;
import ru.practicum.explore.main.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public class EventSpecification {

    // Текстовый поиск
    public static Specification<Event> textSearch(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isEmpty()) return cb.conjunction();
            String search = "%" + text.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("annotation")), search),
                    cb.like(cb.lower(root.get("description")), search)
            );
        };
    }

    public static Specification<Event> hasUsers(List<Long> userIds) {
        return ((root, query, cb) -> userIds == null || userIds.isEmpty()
                ? cb.conjunction()
                : root.get("initiator").get("id").in(userIds));
    }

    public static Specification<Event> hasStates(List<EventState> states) {
        return (root, query, cb) -> states == null || states.isEmpty()
                ? cb.conjunction()
                : root.get("state").in(states);
    }

    public static Specification<Event> hasCategories(List<Long> categories) {
        return (root, query, cb) -> categories == null || categories.isEmpty()
                ? cb.conjunction()
                : root.get("category").get("id").in(categories);
    }

    public static Specification<Event> isPaid(Boolean paid) {
        return (root, query, cb) -> paid == null
                ? cb.conjunction()
                : cb.equal(root.get("paid"), paid);
    }

    // Опубликованные
    public static Specification<Event> isPublished() {
        return (root, query, cb) -> cb.equal(root.get("state"), EventState.PUBLISHED);
    }

    // Фильтр по доступности
    public static Specification<Event> isAvailable(Boolean onlyAvailable) {
        return (root, query, cb) -> {
            if (onlyAvailable == null || !onlyAvailable) return cb.conjunction();
            return cb.or(
                    cb.equal(root.get("participantLimit"), 0),
                    cb.lessThan(root.get("confirmedRequests"), root.get("participantLimit"))
            );
        };
    }

    public static Specification<Event> isAfterStart(LocalDateTime start) {
        return (root, query, cb) -> start == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("eventDate"), start);
    }

    public static Specification<Event> isBeforeEnd(LocalDateTime end) {
        return (root, query, cb) -> end == null
                ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("eventDate"), end);
    }
}