package ru.practicum.explore.main.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explore.main.comment.dto.CommentDto;
import ru.practicum.explore.main.comment.dto.NewCommentDto;
import ru.practicum.explore.main.comment.dto.UpdateCommentDto;
import ru.practicum.explore.main.comment.model.Comment;
import ru.practicum.explore.main.comment.model.CommentState;
import ru.practicum.explore.main.error.model.exception.ConflictException;
import ru.practicum.explore.main.error.model.exception.NotFoundException;
import ru.practicum.explore.main.event.EventRepository;
import ru.practicum.explore.main.event.model.Event;
import ru.practicum.explore.main.event.model.EventState;
import ru.practicum.explore.main.user.UserRepository;
import ru.practicum.explore.main.user.model.User;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    // Private

    @Override
    @Transactional
    public CommentDto addComment(NewCommentDto dto) {
        User author = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Event event = eventRepository.findById(dto.getEventId())
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Невозможно прокомментировать неопубликованное событие");
        }

        Comment comment = CommentMapper.mapToComment(dto, author, event);
        log.info("Добавлен комментарий от пользователя {} для события {}", author.getId(), event.getId());
        return CommentMapper.mapToCommentDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public CommentDto updateComment(UpdateCommentDto dto) {
        Comment comment = existComment(dto.getCommentId());

        if (!comment.getAuthor().getId().equals(dto.getUserId())) {
            throw new ConflictException("Невозможно редактировать чужие комментарии");
        }

        comment.setText(dto.getText());
        comment.setState(CommentState.PUBLISHED);
        log.info("Обновлён комментарий {} пользователем {}", comment.getId(), dto.getUserId());
        return CommentMapper.mapToCommentDto(commentRepository.save(comment));
    }

    @Override
    public List<CommentDto> getUserComments(Long userId, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdOn").descending());
        log.info("Получены комментарии пользователя {}", userId);
        return commentRepository.findAllByAuthorId(userId, pageable).stream()
                .map(CommentMapper::mapToCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUserComment(Long userId, Long commentId) {
        Comment comment = existComment(commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Невозможно удалить чужой комментарий");
        }
        log.info("Пользователь {} удалил комментарий {}", userId, commentId);
        commentRepository.delete(comment);
    }

    // Public

    @Override
    public List<CommentDto> getEventComments(Long eventId, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdOn").descending());
        log.info("Получены комментарии события {}", eventId);
        return commentRepository.findAllByEventIdAndState(eventId, CommentState.PUBLISHED, pageable).stream()
                .map(CommentMapper::mapToCommentDto)
                .collect(Collectors.toList());

    }

    // Admin

    @Override
    @Transactional
    public CommentDto moderateComment(Long commentId, boolean approved) {
        Comment comment = existComment(commentId);
        comment.setState(approved ? CommentState.PUBLISHED : CommentState.REJECTED);
        log.info("Комментарию {} подтверждён: {}", commentId, approved);
        return CommentMapper.mapToCommentDto(commentRepository.save(comment));
    }

    @Override
    public List<CommentDto> getCommentsByState(CommentState state, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("state").descending());
        log.info("Получены события со статусом {}", state);
        return commentRepository.findAllByState(state, pageable).stream()
                .map(CommentMapper::mapToCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteCommentAdmin(Long commentId) {
        Comment comment = existComment(commentId);

        log.info("Администратор удалил комментарий {}", commentId);
        commentRepository.delete(comment);
    }

    // Дополнительный метод

    private Comment existComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));
    }
}
