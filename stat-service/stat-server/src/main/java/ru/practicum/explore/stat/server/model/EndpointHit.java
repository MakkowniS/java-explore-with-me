package ru.practicum.explore.stat.server.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hits")
@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "id")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EndpointHit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "app", nullable = false, length = 255)
    private String app;

    @Column(name = "uri", nullable = false, length = 255)
    private String uri;

    @Column(name = "ip", nullable = false, length = 50)
    private String ip;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime timestamp;
}
