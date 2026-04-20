package ru.practicum.explore.main.comment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.explore.main.comment.model.Comment;
import ru.practicum.explore.main.comment.model.CommentState;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Public. Опубликованные комментарии к событию
    List<Comment> findAllByEventIdAndState(Long eventId, CommentState state, Pageable pageable);

    // Private. Все комментарии пользователя
    List<Comment> findAllByAuthorId(Long authorId, Pageable pageable);

    // Admin. Все комментарии по статусу
    List<Comment> findAllByState(CommentState state, Pageable pageable);

}
