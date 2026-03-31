package ru.practicum.explore.main.event.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.explore.main.category.model.Category;
import ru.practicum.explore.main.user.model.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class   Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "annotation", nullable = false, length = 2000)
    private String annotation; // Краткое описание

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    private Category category; // Категория

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn; // Дата создания

    @Column(name = "description", length = 7000)
    private String description; // Полное описание

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate; // Дата события

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    @ToString.Exclude
    private User initiator; // Создатель события

    @Embedded
    private Location location; // Координаты (широта и долгота) события

    @Column(name = "paid", nullable = false)
    private Boolean paid; // Платное ли событие

    @Column(name = "participant_limit", nullable = false)
    private Integer participantLimit; // Лимит участников (0 - нет лимита)

    @Column(name = "published_on")
    private LocalDateTime publishedOn; // Дата публикации

    @Column(name = "request_moderation", nullable = false)
    private Boolean requestModeration; // Требуется ли пре модерация заявок

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private EventState state; // Статус

    @Column(name = "title", nullable = false, length = 120)
    private String title; // Заголовок

    @Column(name = "views")
    private Long views; // Количество просмотров

    @Column(name = "confirmed_requests")
    private Integer confirmedRequests; // Количество одобренных заявок

}
