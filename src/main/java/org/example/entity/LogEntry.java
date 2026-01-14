package org.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_logs")
public class LogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "\"level\"")
    private String level;

    @Column(name = "\"message\"")
    private String message;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;


    @Column(name = "\"timestamp\"")
    private LocalDateTime timestamp;
}
