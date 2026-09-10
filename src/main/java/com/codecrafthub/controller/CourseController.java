package com.codecrafthub.controller;

import com.codecrafthub.model.Course;
import com.codecrafthub.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * REST Controller — define os endpoints da API.
 *
 * <p>Rotas:</p>
 * <ul>
 *   <li>POST   /api/courses        — criar curso</li>
 *   <li>GET    /api/courses        — listar todos</li>
 *   <li>GET    /api/courses/stats  — estatísticas (bônus, antes de /{id})</li>
 *   <li>GET    /api/courses/search — busca ?q=term (bônus, antes de /{id})</li>
 *   <li>GET    /api/courses/{id}   — buscar por ID</li>
 *   <li>PUT    /api/courses/{id}   — atualizar (parcial ou total)</li>
 *   <li>DELETE /api/courses/{id}   — remover</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*") // libera o painel HTML/JS (porta diferente) para chamar a API
public class CourseController {

    private final CourseService service;

    // Injeção via construtor (recomendado no Spring)
    public CourseController(CourseService service) {
        this.service = service;
    }

    /** POST /api/courses — adiciona um novo curso. Retorna 201 Created. */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Course course) {
        try {
            Course created = service.create(course);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (RuntimeException e) {
            return serverError(e);
        }
    }

    /** GET /api/courses — retorna todos os cursos. */
    @GetMapping
    public ResponseEntity<?> list() {
        try {
            List<Course> courses = service.findAll();
            return ResponseEntity.ok(courses);
        } catch (RuntimeException e) {
            return serverError(e);
        }
    }

    /**
     * GET /api/courses/stats — estatísticas (bônus).
     * IMPORTANTE: deve vir antes de /{id} para o Spring não confundir "stats" com ID.
     */
    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        try {
            return ResponseEntity.ok(service.stats());
        } catch (RuntimeException e) {
            return serverError(e);
        }
    }

    /**
     * GET /api/courses/search?q=term — busca por nome/descrição (bônus).
     * IMPORTANTE: deve vir antes de /{id}.
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(name = "q", required = false, defaultValue = "") String q) {
        try {
            return ResponseEntity.ok(service.search(q));
        } catch (RuntimeException e) {
            return serverError(e);
        }
    }

    /** GET /api/courses/{id} — retorna um curso ou 404. */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.findById(id));
        } catch (NoSuchElementException e) {
            return notFound(e.getMessage());
        } catch (RuntimeException e) {
            return serverError(e);
        }
    }

    /**
     * PUT /api/courses/{id} — atualiza um curso.
     * Aceita objeto parcial, ex: {"status": "In Progress"}.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Course course) {
        try {
            return ResponseEntity.ok(service.update(id, course));
        } catch (NoSuchElementException e) {
            return notFound(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (RuntimeException e) {
            return serverError(e);
        }
    }

    /** DELETE /api/courses/{id} — remove um curso. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            // Resposta simples de confirmação
            return ResponseEntity.ok(Map.of("message", "Course deleted successfully", "id", id));
        } catch (NoSuchElementException e) {
            return notFound(e.getMessage());
        } catch (RuntimeException e) {
            return serverError(e);
        }
    }

    // ---------- Helpers de erro (JSON padronizado) ----------

    private ResponseEntity<Map<String, String>> badRequest(String msg) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
    }

    private ResponseEntity<Map<String, String>> notFound(String msg) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", msg));
    }

    private ResponseEntity<Map<String, String>> serverError(Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "File read/write error: " + e.getMessage()));
    }
}
