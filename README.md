# CodeCraftHub — Plataforma de Aprendizado para Desenvolvedores

API REST simples em **Java 17 + Spring Boot 3** para acompanhar cursos que você quer aprender.
Sem banco de dados: os dados ficam em um arquivo JSON (`courses.json`).
Sem autenticação: foco em aprender o básico de REST.

> Rodando 100% via **Docker** — você NÃO precisa instalar Java nem Maven no Kubuntu.

---

## 1. Visão geral e recursos

- CRUD completo de cursos
- Validação: campos obrigatórios, `status` restrito, data `YYYY-MM-DD`
- Persistência em `courses.json` (criado automaticamente)
- Respostas JSON em `snake_case`: `target_date`, `created_at`
- Bônus: `GET /api/courses/stats` e `GET /api/courses/search?q=term`
- Tratamento de erros: `400` (validação), `404` (não encontrado), `500` (erro de arquivo)

Modelo de curso:

```json
{
  "id": 1,
  "name": "Python Basics",
  "description": "Learn Python fundamentals",
  "target_date": "2025-12-31",
  "status": "Not Started",
  "created_at": "2025-01-15T10:30:00"
}
```

- `status` permitido: `Not Started`, `In Progress`, `Completed` (exato)
- `id` e `created_at` gerados automaticamente

Estrutura:

```
codecrafthub/
├── Dockerfile                  # build multi-stage (Maven) + run (JRE) — sem instalar nada no host
├── docker-compose.yml          # sobe a API na porta 8080 com volume courses.json
├── courses.json                # armazenamento (auto-criado, persistido via volume)
├── pom.xml
└── src/main/java/com/codecrafthub/
    ├── CodeCraftHubApplication.java
    ├── controller/CourseController.java
    ├── model/Course.java
    └── service/CourseService.java
```

---

## 2. Pré-requisitos (só Docker!)

- Docker Engine + plugin Compose (`docker compose version`)
- Porta `8080` livre
- Nada de JDK/Maven instalado — o build acontece dentro do container `maven:3.9-eclipse-temurin-17`

Verifique:

```bash
docker --version
docker compose version
```

---

## 3. Como rodar (Docker — recomendado)

```bash
cd codecrafthub

# garante que o arquivo de dados exista (o compose monta como volume)
touch courses.json
echo '[]' > courses.json

# build da imagem (só na primeira vez ou quando mudar código)
docker compose build

# sobe em background
docker compose up -d

# acompanha os logs
docker logs -f codecrafthub-api
```

Você deve ver:

```
CodeCraftHub API is starting...
Data will be stored in: /app/courses.json
API will be available at: http://localhost:8080
```

Parar / reiniciar:

```bash
docker compose stop
docker compose start
docker compose down        # para e remove o container (courses.json continua no host)
docker compose up -d --build  # rebuild + restart após mudar código
```

### Alternativa sem Docker (se você instalar JDK 17 + Maven)

```bash
mvn clean install
mvn spring-boot:run
```

---

## 4. Documentação da API + exemplos curl

Base: `http://localhost:8080`

### POST /api/courses — criar curso (201)

```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Python Basics",
    "description": "Learn Python fundamentals",
    "target_date": "2025-12-31",
    "status": "Not Started"
  }'
```

Resposta `201`:

```json
{"id":1,"name":"Python Basics","description":"Learn Python fundamentals","status":"Not Started","target_date":"2025-12-31","created_at":"2026-09-10T21:48:25"}
```

### GET /api/courses — listar todos (200)

```bash
curl http://localhost:8080/api/courses
```

### GET /api/courses/{id} — buscar um (200 ou 404)

```bash
curl http://localhost:8080/api/courses/1
```

Erro `404`:

```json
{"error":"Course not found with id: 1"}
```

### PUT /api/courses/{id} — atualizar (aceita parcial)

```bash
curl -X PUT http://localhost:8080/api/courses/1 \
  -H "Content-Type: application/json" \
  -d '{"status": "In Progress"}'
```

Exemplo completo:

```bash
curl -X PUT http://localhost:8080/api/courses/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Python Advanced",
    "description": "Deep dive",
    "target_date": "2026-06-30",
    "status": "Completed"
  }'
```

### DELETE /api/courses/{id} — remover

```bash
curl -X DELETE http://localhost:8080/api/courses/1
```

Resposta:

```json
{"id":1,"message":"Course deleted successfully"}
```

### GET /api/courses/stats — estatísticas (bônus)

```bash
curl http://localhost:8080/api/courses/stats
```

```json
{"total":1,"by_status":{"Not Started":0,"Completed":0,"In Progress":1}}
```

### GET /api/courses/search?q=term — busca (bônus)

```bash
curl "http://localhost:8080/api/courses/search?q=python"
```

### Erros de validação (400)

Sem `name`:

```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -d '{"description":"x","target_date":"2025-12-31","status":"Not Started"}'
# {"error":"Field 'name' is required and must not be empty."}
```

Status inválido:

```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -d '{"name":"X","description":"Y","target_date":"2025-12-31","status":"Done"}'
# {"error":"Invalid status 'Done'. Allowed values: Not Started, In Progress, Completed."}
```

---

## 5. Troubleshooting

| Sintoma | Causa provável / solução |
|---|---|
| `port is already allocated` no `up` | Porta 8080 em uso: `docker ps`, `sudo ss -tlnp \| grep 8080`, ou troque para `"8081:8080"` no compose |
| `courses.json` virou diretório | Você subiu sem criar o arquivo antes. Fix: `docker compose down && rm -rf courses.json && echo '[]' > courses.json && docker compose up -d` |
| Mudou código e nada mudou | Rebuild: `docker compose up -d --build` |
| `curl: Failed to connect` | Container ainda subindo: `docker logs codecrafthub-api --tail 30` e aguarde `Tomcat started on port 8080` |
| Data rejeitada | Use `YYYY-MM-DD`, ex: `2025-12-31`. Outro formato dá `400` |
| Status rejeitado | Use exatamente `Not Started`, `In Progress` ou `Completed` (case-sensitive) |
| Quer limpar tudo | `echo '[]' > courses.json && docker compose restart` |
| Ver dados brutos | `cat courses.json` no host (é o mesmo arquivo dentro do container via volume) |
| Rebuild limpo total | `docker compose down && docker compose build --no-cache && docker compose up -d` |

Comandos úteis:

```bash
docker ps | grep codecrafthub
docker logs codecrafthub-api --tail 50
docker exec -it codecrafthub-api ls -lh /app
curl -s http://localhost:8080/api/courses | python3 -m json.tool
```

---

## Licença

Projeto educacional (Coursera / CodeCraftHub). Uso livre para estudos.
