package com.codecrafthub.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Modelo de dados de um Curso.
 *
 * <p>Exemplo de JSON aceito/retornado pela API (snake_case):</p>
 * <pre>
 * {
 *   "id": 1,
 *   "name": "Python Basics",
 *   "description": "Learn Python fundamentals",
 *   "target_date": "2025-12-31",
 *   "status": "Not Started",
 *   "created_at": "2025-01-15T10:30:00"
 * }
 * </pre>
 *
 * <p>No Java usamos camelCase (targetDate, createdAt) e mapeamos para
 * snake_case com @JsonProperty, conforme exigido pelo exercício.</p>
 */
public class Course {

    /** ID auto-gerado, começa em 1. */
    private Long id;

    /** Nome do curso (obrigatório). */
    private String name;

    /** Descrição do curso (obrigatório). */
    private String description;

    /** Data alvo de conclusão (obrigatório, formato YYYY-MM-DD). */
    @JsonProperty("target_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate targetDate;

    /**
     * Status atual. Valores permitidos exatamente:
     * "Not Started", "In Progress", "Completed".
     */
    private String status;

    /** Timestamp de criação, gerado automaticamente. */
    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    // Construtor padrão (exigido pelo Jackson)
    public Course() {
    }

    public Course(Long id, String name, String description,
                  LocalDate targetDate, String status, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.targetDate = targetDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    // --- Getters e Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
