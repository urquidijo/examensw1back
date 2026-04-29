package com.parcial1.service;

import static java.util.Map.entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parcial1.dto.ProjectAssistantActionDto;
import com.parcial1.dto.ProjectAssistantChatRequest;
import com.parcial1.dto.ProjectAssistantChatResponse;
import com.parcial1.dto.ProjectAssistantMessageDto;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ProjectAssistantAiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    private static final Set<String> ALLOWED_ACTIONS = Set.of(
            "users",
            "tramites",
            "departments",
            "workflows",
            "tickets",
            "tasks",
            "kpis");

    public ProjectAssistantAiService(
            WebClient webClient,
            ObjectMapper objectMapper,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash-lite}") String model) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public ProjectAssistantChatResponse chat(String projectId, ProjectAssistantChatRequest request) {
        validateRequest(request);

        try {
            String systemInstruction = buildSystemInstruction();
            String userPrompt = buildUserPrompt(projectId, request);
            Map<String, Object> payload = buildPayload(systemInstruction, userPrompt);

            String rawResponse = callGeminiWithRetry(payload, 3);
            String jsonText = extractJsonText(rawResponse);

            ProjectAssistantChatResponse response = parseResponse(jsonText);

            if (response.getAnswer() == null || response.getAnswer().isBlank()) {
                response.setAnswer(
                        "Puedo ayudarte con usuarios, trámites, departamentos, workflows, tickets, tareas y KPI.");
            }

            response.setActions(filterActions(response.getActions()));
            response.setSuggestions(normalizeSuggestions(response.getSuggestions()));

            return response;

        } catch (Exception e) {
            return fallbackResponse(request.getMessage());
        }
    }

    private void validateRequest(ProjectAssistantChatRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("El request no puede ser nulo");
        }

        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("El mensaje es obligatorio");
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("No se encontró la API key de Gemini");
        }
    }

    private Map<String, Object> buildPayload(String systemInstruction, String userPrompt) {
        return Map.ofEntries(
                entry("systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemInstruction)))),
                entry("contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", userPrompt))))),
                entry("generationConfig", Map.ofEntries(
                        entry("temperature", 0.3),
                        entry("maxOutputTokens", 2048),
                        entry("responseMimeType", "application/json"),
                        entry("responseSchema", buildResponseSchema()))));
    }

    private Map<String, Object> buildResponseSchema() {
        return Map.ofEntries(
                entry("type", "OBJECT"),
                entry("required", List.of("answer", "actions", "suggestions")),
                entry("properties", Map.ofEntries(
                        entry("answer", Map.of("type", "STRING")),
                        entry("actions", Map.ofEntries(
                                entry("type", "ARRAY"),
                                entry("items", Map.ofEntries(
                                        entry("type", "OBJECT"),
                                        entry("required", List.of("label", "action")),
                                        entry("properties", Map.ofEntries(
                                                entry("label", Map.of("type", "STRING")),
                                                entry("action", Map.of("type", "STRING")))))))),
                        entry("suggestions", Map.ofEntries(
                                entry("type", "ARRAY"),
                                entry("items", Map.of("type", "STRING")))))));
    }

    private String buildSystemInstruction() {
        return """
                Eres un asistente inteligente de NexaFlow.
                Tu objetivo es guiar al usuario dentro de un proyecto de gestión de workflows.

                El sistema tiene estos módulos:
                - users: gestión de usuarios del proyecto.
                - tramites: gestión de trámites y formularios dinámicos.
                - departments: gestión de departamentos o áreas.
                - workflows: diseño de workflows con nodos, decisiones, fork, join e IA.
                - tickets: creación y monitoreo de tickets.
                - tasks: tareas asignadas a departamentos o funcionarios.
                - kpis: métricas, KPI y cuellos de botella.

                RESPUESTA:
                - Responde SOLO JSON válido.
                - No uses markdown.
                - No uses ```json.
                - No expliques fuera del JSON.

                FORMATO:
                {
                  "answer": "respuesta clara para el usuario",
                  "actions": [
                    { "label": "texto del botón", "action": "users|tramites|departments|workflows|tickets|tasks|kpis" }
                  ],
                  "suggestions": [
                    "pregunta sugerida 1",
                    "pregunta sugerida 2"
                  ]
                }

                REGLAS:
                - Responde en español.
                - Sé práctico y directo.
                - Da pasos concretos.
                - Si el usuario dice que no sabe qué hacer, sugiere un orden lógico.
                - Si pregunta por workflows, explica cómo crear, configurar, asignar departamentos y publicar.
                - Si pregunta por tickets, explica que un ticket inicia la operación real del workflow.
                - Si pregunta por tareas, explica que las tareas son el trabajo pendiente del funcionario o departamento.
                - Si pregunta por KPI, explica cuellos de botella por umbrales.
                - Si pregunta por trámites, explica formularios dinámicos y campos.
                - Si pregunta por departamentos, explica áreas responsables y asignación de usuarios.
                - No inventes funciones que el sistema no tiene.
                - No ejecutes acciones destructivas.
                - Solo puedes devolver actions con estos valores:
                  users, tramites, departments, workflows, tickets, tasks, kpis.
                """;
    }

    private String buildUserPrompt(String projectId, ProjectAssistantChatRequest request) {
        String historyText = buildHistoryText(request.getHistory());

        return """
                projectId=%s
                projectName=%s
                currentModule=%s

                HISTORIAL RECIENTE:
                %s

                MENSAJE ACTUAL DEL USUARIO:
                %s
                """.formatted(
                projectId,
                safe(request.getProjectName()),
                safe(request.getCurrentModule()),
                historyText,
                safe(request.getMessage()));
    }

    private String buildHistoryText(List<ProjectAssistantMessageDto> history) {
        if (history == null || history.isEmpty()) {
            return "Sin historial previo.";
        }

        StringBuilder builder = new StringBuilder();

        int start = Math.max(0, history.size() - 8);

        for (int i = start; i < history.size(); i++) {
            ProjectAssistantMessageDto message = history.get(i);

            builder.append(safe(message.getSender()))
                    .append(": ")
                    .append(safe(message.getText()))
                    .append("\n");
        }

        return builder.toString();
    }

    private ProjectAssistantChatResponse parseResponse(String jsonText) throws Exception {
        String clean = sanitizeJson(jsonText);
        JsonNode root = objectMapper.readTree(clean);

        String answer = root.path("answer").asText("");

        List<ProjectAssistantActionDto> actions = new ArrayList<>();
        JsonNode actionsNode = root.path("actions");

        if (actionsNode.isArray()) {
            for (JsonNode item : actionsNode) {
                actions.add(ProjectAssistantActionDto.builder()
                        .label(item.path("label").asText(""))
                        .action(item.path("action").asText(""))
                        .build());
            }
        }

        List<String> suggestions = new ArrayList<>();
        JsonNode suggestionsNode = root.path("suggestions");

        if (suggestionsNode.isArray()) {
            for (JsonNode item : suggestionsNode) {
                String suggestion = item.asText("");

                if (!suggestion.isBlank()) {
                    suggestions.add(suggestion);
                }
            }
        }

        return ProjectAssistantChatResponse.builder()
                .answer(answer)
                .actions(actions)
                .suggestions(suggestions)
                .build();
    }

    private List<ProjectAssistantActionDto> filterActions(List<ProjectAssistantActionDto> actions) {
        if (actions == null) {
            return List.of();
        }

        List<ProjectAssistantActionDto> filtered = new ArrayList<>();

        for (ProjectAssistantActionDto action : actions) {
            if (action == null)
                continue;

            String actionKey = safe(action.getAction()).trim();

            if (!ALLOWED_ACTIONS.contains(actionKey)) {
                continue;
            }

            String label = safe(action.getLabel()).trim();

            if (label.isBlank()) {
                label = defaultActionLabel(actionKey);
            }

            filtered.add(ProjectAssistantActionDto.builder()
                    .label(label)
                    .action(actionKey)
                    .build());
        }

        return filtered;
    }

    private List<String> normalizeSuggestions(List<String> suggestions) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();

        if (suggestions != null) {
            for (String suggestion : suggestions) {
                if (suggestion != null && !suggestion.isBlank()) {
                    unique.add(suggestion.trim());
                }
            }
        }

        if (unique.isEmpty()) {
            unique.add("¿Qué hago primero?");
            unique.add("¿Cómo creo un workflow?");
            unique.add("¿Cómo reviso cuellos de botella?");
        }

        return unique.stream().limit(4).toList();
    }

    private ProjectAssistantChatResponse fallbackResponse(String message) {
        String normalized = normalizeText(message);

        List<ProjectAssistantActionDto> actions = new ArrayList<>();

        if (normalized.contains("ticket")) {
            actions.add(new ProjectAssistantActionDto("Ir a tickets", "tickets"));
        } else if (normalized.contains("tarea")) {
            actions.add(new ProjectAssistantActionDto("Ir a tareas", "tasks"));
        } else if (normalized.contains("workflow") || normalized.contains("flujo")) {
            actions.add(new ProjectAssistantActionDto("Ir a workflows", "workflows"));
        } else if (normalized.contains("kpi") || normalized.contains("cuello") || normalized.contains("metrica")) {
            actions.add(new ProjectAssistantActionDto("Ir a KPI", "kpis"));
        } else {
            actions.add(new ProjectAssistantActionDto("Usuarios", "users"));
            actions.add(new ProjectAssistantActionDto("Trámites", "tramites"));
            actions.add(new ProjectAssistantActionDto("Workflows", "workflows"));
        }

        return ProjectAssistantChatResponse.builder()
                .answer("Puedo ayudarte a usar el proyecto. Te recomiendo revisar primero usuarios, trámites, departamentos y luego workflows. Después puedes crear tickets, atender tareas y revisar KPI.")
                .actions(actions)
                .suggestions(List.of(
                        "¿Qué hago primero?",
                        "¿Cómo creo un workflow?",
                        "¿Cómo creo un ticket?",
                        "¿Cómo reviso tareas?"))
                .build();
    }

    private String callGeminiWithRetry(Map<String, Object> payload, int maxAttempts) {
        long waitMs = 1500;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return callGemini(payload);
            } catch (GeminiApiException e) {
                if (e.statusCode == 503 && attempt < maxAttempts) {
                    sleep(waitMs);
                    waitMs *= 2;
                    continue;
                }

                throw e;
            }
        }

        throw new IllegalStateException("No se pudo obtener respuesta de Gemini.");
    }

    private String callGemini(Map<String, Object> payload) {
        return webClient.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent")
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> {
                            if (response.statusCode().is2xxSuccessful()) {
                                return Mono.just(body);
                            }

                            return Mono.error(new GeminiApiException(response.statusCode().value(), body));
                        }))
                .timeout(Duration.ofSeconds(40))
                .block();
    }

    private String extractJsonText(String rawResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse);

        JsonNode candidates = root.path("candidates");

        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IllegalStateException("Gemini no devolvió candidatos");
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");

        if (!parts.isArray() || parts.isEmpty()) {
            throw new IllegalStateException("Gemini no devolvió contenido");
        }

        StringBuilder textBuilder = new StringBuilder();

        for (JsonNode part : parts) {
            String text = part.path("text").asText("");

            if (!text.isBlank()) {
                textBuilder.append(text);
            }
        }

        return sanitizeJson(textBuilder.toString());
    }

    private String sanitizeJson(String text) {
        String clean = text == null ? "" : text.trim();

        clean = clean
                .replace("```json", "")
                .replace("```JSON", "")
                .replace("```", "")
                .trim();

        int firstBrace = clean.indexOf('{');
        int lastBrace = clean.lastIndexOf('}');

        if (firstBrace >= 0 && lastBrace > firstBrace) {
            clean = clean.substring(firstBrace, lastBrace + 1);
        }

        return clean.trim();
    }

    private String defaultActionLabel(String action) {
        return switch (action) {
            case "users" -> "Ir a usuarios";
            case "tramites" -> "Ir a trámites";
            case "departments" -> "Ir a departamentos";
            case "workflows" -> "Ir a workflows";
            case "tickets" -> "Ir a tickets";
            case "tasks" -> "Ir a tareas";
            case "kpis" -> "Ir a KPI";
            default -> "Abrir módulo";
        };
    }

    private String normalizeText(String value) {
        return java.text.Normalizer.normalize(safe(value), java.text.Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase()
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void sleep(long waitMs) {
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Proceso interrumpido durante retry de Gemini", e);
        }
    }

    private static class GeminiApiException extends RuntimeException {
        private final int statusCode;
        private final String responseBody;

        private GeminiApiException(int statusCode, String responseBody) {
            super("Gemini error HTTP " + statusCode);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }
    }
}