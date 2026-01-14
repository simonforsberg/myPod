package org.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    public Long getId() { return id; }

    public String getLevel() { return level; }

    public String getMessage() { return message; }

    public String getErrorDetails() { return errorDetails; }

    public LocalDateTime getTimestamp() { return timestamp; }
}
