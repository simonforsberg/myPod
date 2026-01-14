package org.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_logs")
public class LogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String level;
    private String message;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    private LocalDateTime timestamp;
}
