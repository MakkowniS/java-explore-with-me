package ru.practicum.explore.main.comment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.explore.main.comment.model.Comment;
import ru.practicum.explore.main.comment.model.CommentState;
import ru.practicum.explore.main.comment.model.EventCommentCount;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Public. Опубликованные комментарии к событию
    List<Comment> findAllByEventIdAndState(Long eventId, CommentState state, Pageable pageable);

    // Private. Все комментарии пользователя
    List<Comment> findAllByAuthorId(Long authorId, Pageable pageable);

    // Admin. Все комментарии по статусу
    List<Comment> findAllByState(CommentState state, Pageable pageable);

    // Методы для подсчёта комментариев
    @Query("SELECT c.event.id as eventId, count(c.id) as commentCount " +
            "FROM Comment c " +
            "WHERE c.event.id IN :eventIds AND c.state = :state " +
            "GROUP BY c.event.id ")
    List<EventCommentCount> countByEventIdAndState(@Param("eventIds") List<Long> eventIds,
                                                   @Param("state") CommentState state);

    Long countByEventIdAndState(Long eventId, CommentState state);
}
