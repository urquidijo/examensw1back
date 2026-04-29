package com.parcial1.service;

import static java.util.Map.entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parcial1.dto.ai.WorkflowAiCommandRequest;
import com.parcial1.dto.ai.WorkflowAiEdge;
import com.parcial1.dto.ai.WorkflowAiGraphResponse;
import com.parcial1.dto.ai.WorkflowAiNode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class GeminiWorkflowAiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiWorkflowAiService(
            WebClient webClient,
            ObjectMapper objectMapper,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash-lite}") String model) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public WorkflowAiGraphResponse processCommand(
            String projectId,
            String workflowId,
            WorkflowAiCommandRequest request) {
        validateRequest(request);

        try {
            String systemInstruction = buildSystemInstruction();
            String userPrompt = buildUserPrompt(projectId, workflowId, request);
            Map<String, Object> payload = buildPayload(systemInstruction, userPrompt);

            String rawResponse = callGeminiWithRetry(payload, 3);
            String jsonText = extractJsonText(rawResponse);
            WorkflowAiGraphResponse parsed = parseGraphResponse(jsonText);

            return normalizeGraphResponse(request, parsed);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error procesando comando con Gemini: " + e.getMessage(), e);
        }
    }

    private void validateRequest(WorkflowAiCommandRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("El request no puede ser nulo");
        }

        if (request.prompt() == null || request.prompt().trim().isEmpty()) {
            throw new IllegalArgumentException("El prompt es obligatorio");
        }

        if (request.workflow() == null) {
            throw new IllegalArgumentException("El workflow actual es obligatorio");
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("No se encontró la API key de Gemini. Revisa GEMINI_API_KEY.");
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
                        entry("temperature", 0.1),
                        entry("maxOutputTokens", 3072),
                        entry("responseMimeType", "application/json"),
                        entry("responseSchema", buildResponseSchema()))));
    }

    private Map<String, Object> buildResponseSchema() {
        return Map.ofEntries(
                entry("type", "OBJECT"),
                entry("required", List.of("mode", "summary", "nodes", "edges")),
                entry("properties", Map.ofEntries(
                        entry("mode", Map.of(
                                "type", "STRING",
                                "enum", List.of("replace", "patch"))),
                        entry("summary", Map.of(
                                "type", "STRING")),
                        entry("nodes", Map.ofEntries(
                                entry("type", "ARRAY"),
                                entry("items", buildNodeSchema()))),
                        entry("edges", Map.ofEntries(
                                entry("type", "ARRAY"),
                                entry("items", buildEdgeSchema()))))));
    }

    private Map<String, Object> buildNodeSchema() {
        return Map.ofEntries(
                entry("type", "OBJECT"),
                entry("required", List.of("id", "shape", "x", "y", "label", "data")),
                entry("properties", Map.ofEntries(
                        entry("id", Map.of("type", "STRING")),
                        entry("shape", Map.of(
                                "type", "STRING",
                                "enum", List.of(
                                        "workflow-start",
                                        "workflow-task",
                                        "workflow-decision",
                                        "workflow-fork",
                                        "workflow-join",
                                        "workflow-end"))),
                        entry("x", Map.of("type", "INTEGER")),
                        entry("y", Map.of("type", "INTEGER")),
                        entry("label", Map.of("type", "STRING")),
                        entry("data", buildNodeDataSchema()))));
    }

    private Map<String, Object> buildNodeDataSchema() {
        return Map.ofEntries(
                entry("type", "OBJECT"),
                entry("required", List.of("label", "nodeType")),
                entry("properties", Map.ofEntries(
                        entry("label", Map.of("type", "STRING")),
                        entry("nodeType", Map.of(
                                "type", "STRING",
                                "enum", List.of("start", "task", "decision", "fork", "join", "end"))),
                        entry("departmentId", Map.of("type", "STRING")),
                        entry("departmentName", Map.of("type", "STRING")),
                        entry("instructions", Map.of("type", "STRING")),
                        entry("aiAlias", Map.of("type", "STRING")),
                        entry("decisionMode", Map.of("type", "STRING")),
                        entry("decisionQuestion", Map.of("type", "STRING")),
                        entry("decisionOptions", Map.ofEntries(
                                entry("type", "ARRAY"),
                                entry("items", Map.ofEntries(
                                        entry("type", "OBJECT"),
                                        entry("required", List.of("value", "label")),
                                        entry("properties", Map.ofEntries(
                                                entry("value", Map.of("type", "STRING")),
                                                entry("label", Map.of("type", "STRING")))))))))));
    }

    private Map<String, Object> buildEdgeSchema() {
        return Map.ofEntries(
                entry("type", "OBJECT"),
                entry("required", List.of("id", "shape", "source", "target")),
                entry("properties", Map.ofEntries(
                        entry("id", Map.of("type", "STRING")),
                        entry("shape", Map.of(
                                "type", "STRING",
                                "enum", List.of("edge"))),
                        entry("source", buildEdgeEndpointSchema()),
                        entry("target", buildEdgeEndpointSchema()),
                        entry("attrs", Map.of("type", "OBJECT")))));
    }

    private Map<String, Object> buildEdgeEndpointSchema() {
        return Map.ofEntries(
                entry("type", "OBJECT"),
                entry("required", List.of("cell", "port")),
                entry("properties", Map.ofEntries(
                        entry("cell", Map.of("type", "STRING")),
                        entry("port", Map.of("type", "STRING")))));
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

                if (e.statusCode == 403) {
                    throw new IllegalStateException(
                            "Gemini rechazó la solicitud (403). Revisa tu API key. Detalle: " + preview(e.responseBody),
                            e);
                }

                if (e.statusCode == 503) {
                    throw new IllegalStateException(
                            "Gemini está temporalmente saturado (503). Intenta nuevamente. Detalle: "
                                    + preview(e.responseBody),
                            e);
                }

                throw new IllegalStateException(
                        "Gemini error HTTP " + e.statusCode + ". Detalle: " + preview(e.responseBody),
                        e);
            }
        }

        throw new IllegalStateException("No se pudo obtener una respuesta válida de Gemini.");
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

    private String buildSystemInstruction() {
        return """
                Eres un diseñador experto de workflows BPMN/UML para un sistema de trámites.
                Convierte instrucciones en español a un JSON de workflow visual.

                RESPUESTA:
                - Responde SOLO JSON válido.
                - No uses markdown.
                - No uses ```json.
                - No agregues explicaciones fuera del JSON.
                - El JSON debe tener exactamente:
                  {
                    "mode": "replace" | "patch",
                    "summary": "frase corta",
                    "nodes": [],
                    "edges": []
                  }

                MODOS:
                - Usa mode="replace" si el usuario pide crear un flujo desde cero, nuevo workflow, reemplazar todo o rehacer el flujo.
                - Usa mode="patch" si el usuario pide agregar, eliminar, renombrar, conectar, modificar o ajustar algo del flujo actual.

                TIPOS DE NODOS PERMITIDOS:
                - start: inicio del proceso.
                - task: actividad operativa asignable a departamento.
                - decision: decisión manual con exactamente dos salidas.
                - fork: apertura de paralelo.
                - join: cierre de paralelo.
                - end: fin del proceso.

                SHAPES PERMITIDOS:
                - start => workflow-start
                - task => workflow-task
                - decision => workflow-decision
                - fork => workflow-fork
                - join => workflow-join
                - end => workflow-end

                REGLAS GENERALES:
                - Todo workflow reemplazado debe tener exactamente 1 start y exactamente 1 end.
                - Nunca generes más de un nodo end.
                - Si hay varias rutas finales, todas deben converger al mismo único nodo end.
                - No dejes nodos sueltos.
                - Todo nodo, excepto start, debe tener entrada.
                - Todo nodo, excepto end, debe tener salida.
                - Usa ids cortos, estables y descriptivos.
                - Los ids no deben tener espacios ni tildes.
                - Usa nombres claros de actividades, no nombres genéricos si el usuario dio contexto.
                - Si hay departamentos disponibles, asigna departmentId y departmentName a los nodos task y decision cuando tenga sentido.
                - No asignes departamento a start, end, fork o join.

                REGLAS PARA DECISIONES:
                - Un nodo decision debe tener exactamente 2 salidas.
                - Las únicas condiciones permitidas son SI y NO.
                - Cada edge que sale de una decisión debe tener:
                  "conditionValue": "SI" o "NO"
                  "data": { "conditionValue": "SI" o "NO" }
                  "labels": con texto "Sí" o "No".
                - El data del nodo decision debe incluir:
                  "decisionMode": "MANUAL",
                  "decisionQuestion": "pregunta clara",
                  "decisionOptions": [
                    { "value": "SI", "label": "Sí" },
                    { "value": "NO", "label": "No" }
                  ]
                - No generes decisiones con 3 o más ramas.
                - No uses valores como APROBADO, RECHAZADO, OK, ERROR en conditionValue. Siempre usa SI o NO.

                REGLAS PARA PARALELO:
                - Si usas fork, debe tener al menos 2 salidas.
                - Todo fork debe tener un join correspondiente.
                - Todas las ramas que salen del fork deben llegar al mismo join.
                - El join debe continuar a otro nodo o al end.
                - No dejes ramas paralelas sin cerrar.

                REGLAS PARA PATCH:
                - Si agregas un nodo, también agrega sus edges necesarios.
                - Si eliminas un nodo, devuelve ese nodo con:
                  data: { "action": "delete" }
                - Si eliminas un edge, devuelve ese edge con:
                  data: { "action": "delete" }
                - Si renombras un nodo, devuelve el mismo id con el nuevo label y data.label.
                - Si conectas nodos existentes, devuelve solo el edge nuevo.
                - No inventes ids de nodos existentes: usa los ids del currentWorkflow.
                - En patch, conserva la coherencia del flujo actual.

                COHERENCIA:
                - Para flujo lineal: start -> task(s) -> end.
                - Para flujo condicional: start -> task opcional -> decision -> ruta SI / ruta NO -> end o siguiente actividad común.
                - Para flujo paralelo: start -> fork -> ramas paralelas -> join -> end o siguiente actividad.
                - Si el usuario pide algo ambiguo, genera la estructura más simple y válida.
                - summary debe ser una frase corta indicando qué hiciste.
                - En flujos condicionales, las ramas SI y NO pueden terminar en actividades diferentes, pero deben llegar al mismo único end.
                - En flujos paralelos, el join debe continuar hacia el mismo único end o hacia una actividad común que llegue al único end.
                """;
    }

    private String buildUserPrompt(
            String projectId,
            String workflowId,
            WorkflowAiCommandRequest request) throws Exception {
        String workflowJson = objectMapper.writeValueAsString(request.workflow());
        String departmentsJson = objectMapper.writeValueAsString(
                request.departments() != null ? request.departments() : List.of());

        String resolvedMode = resolveMode(request);

        return """
                projectId=%s
                workflowId=%s
                resolvedMode=%s
                currentWorkflow=%s
                departments=%s
                command=%s
                """.formatted(
                projectId,
                workflowId,
                resolvedMode,
                workflowJson,
                departmentsJson,
                request.prompt());
    }

    private String extractJsonText(String rawResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse);

        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            String promptFeedback = root.path("promptFeedback").toString();
            throw new IllegalStateException("Gemini no devolvió candidatos. promptFeedback=" + promptFeedback);
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new IllegalStateException("Gemini no devolvió partes de texto utilizables");
        }

        StringBuilder textBuilder = new StringBuilder();
        for (JsonNode part : parts) {
            String text = part.path("text").asText("");
            if (!text.isBlank()) {
                textBuilder.append(text);
            }
        }

        String text = textBuilder.toString().trim();
        if (text.isEmpty()) {
            throw new IllegalStateException("Gemini devolvió una respuesta vacía");
        }

        return sanitizeJson(text);
    }

    private WorkflowAiGraphResponse parseGraphResponse(String jsonText) {
        String cleaned = sanitizeJson(jsonText);

        try {
            JsonNode root = objectMapper.readTree(cleaned);

            if (!root.isObject()) {
                throw new IllegalStateException("Gemini no devolvió un objeto JSON válido");
            }

            JsonNode nodesNode = root.path("nodes");
            JsonNode edgesNode = root.path("edges");

            if (!nodesNode.isArray()) {
                throw new IllegalStateException("El campo nodes no es un array válido");
            }

            if (!edgesNode.isArray()) {
                throw new IllegalStateException("El campo edges no es un array válido");
            }

            return objectMapper.treeToValue(root, WorkflowAiGraphResponse.class);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Gemini devolvió JSON inválido o incompleto. Respuesta: " + preview(cleaned),
                    e);
        }
    }

    private String sanitizeJson(String text) {
        String clean = text == null ? "" : text.trim();

        if (clean.startsWith("```json")) {
            clean = clean.substring(7).trim();
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3).trim();
        }

        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3).trim();
        }

        int firstBrace = clean.indexOf('{');
        int lastBrace = clean.lastIndexOf('}');

        if (firstBrace >= 0 && lastBrace > firstBrace) {
            clean = clean.substring(firstBrace, lastBrace + 1);
        }

        return clean.trim();
    }

    private WorkflowAiGraphResponse normalizeGraphResponse(
            WorkflowAiCommandRequest request,
            WorkflowAiGraphResponse response) {
        String resolvedMode = resolveMode(request);

        if (response == null) {
            return new WorkflowAiGraphResponse(
                    resolvedMode,
                    "No se generó respuesta",
                    List.of(),
                    List.of());
        }

        String summary = nonBlank(response.summary())
                ? response.summary()
                : ("replace".equals(resolvedMode)
                        ? "Workflow generado con IA"
                        : "Cambios generados con IA");

        List<WorkflowAiNode> nodes = response.nodes() == null ? List.of() : response.nodes();
        List<WorkflowAiEdge> edges = response.edges() == null ? List.of() : response.edges();

        return new WorkflowAiGraphResponse(
                resolvedMode,
                summary,
                nodes,
                edges);
    }

    private String resolveMode(WorkflowAiCommandRequest request) {
        if (request.forcedMode() != null && !request.forcedMode().isBlank()) {
            return request.forcedMode().trim().toLowerCase();
        }
        return inferIntentMode(request.prompt());
    }

    private String inferIntentMode(String prompt) {
        if (prompt == null) {
            return "patch";
        }

        String p = prompt.trim().toLowerCase();

        boolean replaceIntent = p.contains("crea un workflow") ||
                p.contains("crear un workflow") ||
                p.contains("crea un flujo") ||
                p.contains("crear un flujo") ||
                p.contains("nuevo workflow") ||
                p.contains("nuevo flujo") ||
                p.contains("desde cero") ||
                p.contains("reemplaza todo");

        boolean patchIntent = p.contains("agrega") ||
                p.contains("añade") ||
                p.contains("aumenta") ||
                p.contains("conecta") ||
                p.contains("renombra") ||
                p.contains("cambia") ||
                p.contains("edita") ||
                p.contains("modifica") ||
                p.contains("elimina") ||
                p.contains("borra") ||
                p.contains("quita");

        if (replaceIntent && !patchIntent) {
            return "replace";
        }

        return "patch";
    }

    private boolean nonBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void sleep(long waitMs) {
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Proceso interrumpido durante el retry de Gemini", e);
        }
    }

    private String preview(String text) {
        if (text == null || text.isBlank()) {
            return "(sin detalle)";
        }

        String normalized = text.replace("\n", " ").replace("\r", " ").trim();
        return normalized.length() > 400
                ? normalized.substring(0, 400) + "..."
                : normalized;
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