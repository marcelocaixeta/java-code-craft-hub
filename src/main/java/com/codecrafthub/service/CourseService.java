package com.codecrafthub.service;

import com.codecrafthub.model.Course;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Camada de negócio + persistência em arquivo JSON.
 *
 * <p>Todo o CRUD lê/escreve o arquivo {@code courses.json}.
 * O arquivo é criado automaticamente se não existir.</p>
 */
@Service
public class CourseService {

    /** Status válidos (devem ser exatamente estes valores). */
    public static final Set<String> ALLOWED_STATUS = new HashSet<>(
            Arrays.asList("Not Started", "In Progress", "Completed"));

    private final File storageFile;
    private final ObjectMapper mapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * @param storagePath caminho do JSON, configurável via
     *                    {@code app.storage.file} (padrão: courses.json).
     *                    No Docker o working dir é /app, então será /app/courses.json.
     */
    public CourseService(@Value("${app.storage.file:courses.json}") String storagePath) {
        this.storageFile = new File(storagePath);
        this.mapper = new ObjectMapper();
        // Suporte a LocalDate / LocalDateTime
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Cria o arquivo automaticamente se não existir
        ensureStorageExists();
        System.out.println("Data will be stored in: " + this.storageFile.getAbsolutePath());
    }

    /** Garante que courses.json exista com conteúdo inicial "[]". */
    private void ensureStorageExists() {
        try {
            File parent = storageFile.getAbsoluteFile().getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!storageFile.exists()) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(storageFile, new ArrayList<Course>());
                System.out.println("Created new storage file: " + storageFile.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage file: " + storageFile, e);
        }
    }

    /** Lê todos os cursos do arquivo JSON. */
    private List<Course> readFromFile() {
        lock.readLock().lock();
        try {
            if (!storageFile.exists() || storageFile.length() == 0) {
                return new ArrayList<>();
            }
            List<Course> courses = mapper.readValue(storageFile,
                    new TypeReference<List<Course>>() {
                    });
            return courses != null ? courses : new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read storage file: " + storageFile, e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Escreve a lista completa de cursos no arquivo JSON. */
    private void writeToFile(List<Course> courses) {
        lock.writeLock().lock();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(storageFile, courses);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write storage file: " + storageFile, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Valida campos obrigatórios na criação. */
    private void validateForCreate(Course course) {
        if (course.getName() == null || course.getName().isBlank()) {
            throw new IllegalArgumentException("Field 'name' is required and must not be empty.");
        }
        if (course.getDescription() == null || course.getDescription().isBlank()) {
            throw new IllegalArgumentException("Field 'description' is required and must not be empty.");
        }
        if (course.getTargetDate() == null) {
            throw new IllegalArgumentException("Field 'target_date' is required (format YYYY-MM-DD).");
        }
        validateStatus(course.getStatus());
    }

    /** Valida o status (obrigatório na criação; opcional no PUT parcial). */
    private void validateStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException(
                    "Field 'status' is required. Allowed values: Not Started, In Progress, Completed.");
        }
        if (!ALLOWED_STATUS.contains(status)) {
            throw new IllegalArgumentException(
                    "Invalid status '" + status + "'. Allowed values: Not Started, In Progress, Completed.");
        }
    }

    /** Gera o próximo ID (max + 1, começando em 1). */
    private long nextId(List<Course> courses) {
        return courses.stream()
                .mapToLong(c -> c.getId() != null ? c.getId() : 0L)
                .max()
                .orElse(0L) + 1;
    }

    // ---------------- Operações públicas (CRUD) ----------------

    /** GET /api/courses — lista todos. */
    public List<Course> findAll() {
        return readFromFile();
    }

    /** GET /api/courses/{id} — busca por ID ou lança 404. */
    public Course findById(Long id) {
        return readFromFile().stream()
                .filter(c -> c.getId() != null && c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Course not found with id: " + id));
    }

    /** POST /api/courses — cria um novo curso. */
    public Course create(Course input) {
        validateForCreate(input);
        List<Course> courses = readFromFile();
        Course course = new Course();
        course.setId(nextId(courses));
        course.setName(input.getName().trim());
        course.setDescription(input.getDescription().trim());
        course.setTargetDate(input.getTargetDate());
        course.setStatus(input.getStatus());
        course.setCreatedAt(LocalDateTime.now());
        courses.add(course);
        writeToFile(courses);
        return course;
    }

    /**
     * PUT /api/courses/{id} — atualização parcial (merge).
     * Campos nulos/vazios no JSON são ignorados, mantendo o valor atual.
     * Isso permite, por exemplo, enviar só {"status": "In Progress"}.
     */
    public Course update(Long id, Course input) {
        List<Course> courses = readFromFile();
        Course existing = courses.stream()
                .filter(c -> c.getId() != null && c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Course not found with id: " + id));

        if (input.getName() != null) {
            if (input.getName().isBlank()) {
                throw new IllegalArgumentException("Field 'name' must not be empty.");
            }
            existing.setName(input.getName().trim());
        }
        if (input.getDescription() != null) {
            if (input.getDescription().isBlank()) {
                throw new IllegalArgumentException("Field 'description' must not be empty.");
            }
            existing.setDescription(input.getDescription().trim());
        }
        if (input.getTargetDate() != null) {
            existing.setTargetDate(input.getTargetDate());
        }
        if (input.getStatus() != null) {
            validateStatus(input.getStatus());
            existing.setStatus(input.getStatus());
        }

        writeToFile(courses);
        return existing;
    }

    /** DELETE /api/courses/{id} — remove e retorna o removido. */
    public Course delete(Long id) {
        List<Course> courses = readFromFile();
        Course existing = courses.stream()
                .filter(c -> c.getId() != null && c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Course not found with id: " + id));
        courses.removeIf(c -> c.getId() != null && c.getId().equals(id));
        writeToFile(courses);
        return existing;
    }

    /** GET /api/courses/stats — totais por status (bônus). */
    public Map<String, Object> stats() {
        List<Course> courses = readFromFile();
        Map<String, Long> byStatus = courses.stream()
                .collect(Collectors.groupingBy(Course::getStatus, Collectors.counting()));
        // Garante que os 3 status sempre apareçam, mesmo com 0
        for (String s : ALLOWED_STATUS) {
            byStatus.putIfAbsent(s, 0L);
        }
        return Map.of(
                "total", courses.size(),
                "by_status", byStatus);
    }

    /** GET /api/courses/search?q=term — busca em nome/descrição (bônus). */
    public List<Course> search(String term) {
        if (term == null || term.isBlank()) {
            return findAll();
        }
        String lower = term.toLowerCase();
        return readFromFile().stream()
                .filter(c -> (c.getName() != null && c.getName().toLowerCase().contains(lower))
                        || (c.getDescription() != null && c.getDescription().toLowerCase().contains(lower)))
                .collect(Collectors.toList());
    }
}
