package com.parcial1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parcial1.dto.ai.WorkflowAiCommandRequest;
import com.parcial1.dto.ai.WorkflowAiOperation;
import com.parcial1.dto.ai.WorkflowAiResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GeminiWorkflowAiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash-lite}")
    private String model;

    public GeminiWorkflowAiService(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public WorkflowAiResponse processCommand(
        String projectId,
        String workflowId,
        WorkflowAiCommandRequest request
    ) {
        validateRequest(request);

        try {
            String systemInstruction = buildSystemInstruction();
            String userPrompt = buildUserPrompt(projectId, workflowId, request);

            Map<String, Object> payload = Map.of(
                "systemInstruction", Map.of(
                    "parts", List.of(
                        Map.of("text", systemInstruction)
                    )
                ),
                "contents", List.of(
                    Map.of(
                        "role", "user",
                        "parts", List.of(
                            Map.of("text", userPrompt)
                        )
                    )
                ),
                "generationConfig", Map.of(
                    "temperature", 0.1,
                    "maxOutputTokens", 2048,
                    "responseMimeType", "application/json"
                )
            );

            String rawResponse = webClient.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent")
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(
                    status -> status.isError(),
                    response -> response.bodyToMono(String.class)
                        .map(body -> new IllegalArgumentException("Gemini error: " + body))
                )
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(35))
                .block();

            String jsonText = extractJsonText(rawResponse);

            WorkflowAiResponse parsed = objectMapper.readValue(jsonText, WorkflowAiResponse.class);
            WorkflowAiResponse normalizedMode = normalizeModeFromRequest(request, parsed);

            return normalizeOperations(normalizedMode);

        } catch (Exception e) {
            e.printStackTrace();
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
    }

    private String buildSystemInstruction() {
        return """
            Convierte instrucciones en español a JSON para editar workflows.

            Debes responder SOLO con un objeto JSON válido:
            {
              "mode": "replace" | "patch",
              "summary": "texto corto",
              "operations": []
            }

            Operaciones permitidas:
            createNode, renameNode, updateNode, deleteNode, connectNodes, disconnectNodes

            Tipos de nodo:
            start, task, decision, fork, join, end

            REGLAS OBLIGATORIAS:
            - Cada createNode debe incluir nodeType, alias y label.
            - Si el usuario pide crear un workflow nuevo, nuevo flujo, desde cero o reemplazar el actual, usa mode="replace".
            - Si el usuario pide agregar, aumentar, modificar, renombrar, conectar, eliminar o quitar, usa mode="patch".
            - Si creas más de un nodo, SIEMPRE debes agregar connectNodes.
            - No dejes nodos aislados.
            - Si el flujo es lineal, conecta todos los nodos consecutivamente.
            - Si hay una decisión, conecta la decisión con cada rama.
            - Si un campo no aplica, usa null.
            - No agregues explicación fuera del JSON.
            """;
    }

    private String buildUserPrompt(
        String projectId,
        String workflowId,
        WorkflowAiCommandRequest request
    ) throws Exception {
        String workflowJson = objectMapper.writeValueAsString(request.workflow());
        String departmentsJson = objectMapper.writeValueAsString(
            request.departments() != null ? request.departments() : List.of()
        );

        String resolvedMode = resolveMode(request);

        return """
            workflowId=%s
            resolvedMode=%s
            workflow=%s
            departments=%s
            command=%s

            Interpretación obligatoria:
            - Si resolvedMode=replace, crea un workflow nuevo y reemplaza el actual.
            - Si resolvedMode=patch, modifica el workflow actual sin reemplazarlo completo.
            - Si creas varios nodos, incluye connectNodes.
            """.formatted(
            workflowId,
            resolvedMode,
            workflowJson,
            departmentsJson,
            request.prompt()
        );
    }

    private WorkflowAiResponse normalizeModeFromRequest(
        WorkflowAiCommandRequest request,
        WorkflowAiResponse response
    ) {
        String resolvedMode = resolveMode(request);

        if (response == null) {
            return null;
        }

        if (!resolvedMode.equals(response.mode())) {
            return new WorkflowAiResponse(
                resolvedMode,
                response.summary(),
                response.operations()
            );
        }

        return response;
    }

    private WorkflowAiResponse normalizeOperations(WorkflowAiResponse response) {
        if (response == null) {
            return null;
        }

        List<WorkflowAiOperation> originalOps =
            response.operations() != null ? response.operations() : List.of();

        List<WorkflowAiOperation> normalizedOps = new ArrayList<>();
        List<String> createdAliasesInOrder = new ArrayList<>();
        Set<String> existingConnections = new HashSet<>();

        int createCount = 1;
        boolean hasConnectOps = false;

        for (WorkflowAiOperation op : originalOps) {
            if (op == null || op.type() == null) {
                continue;
            }

            switch (op.type()) {
                case "createNode" -> {
                    String nodeType = nonBlank(op.nodeType()) ? op.nodeType() : "task";
                    String alias = nonBlank(op.alias()) ? op.alias() : "nodo" + createCount++;
                    String label = nonBlank(op.label()) ? op.label() : getDefaultLabel(nodeType);

                    List<Map<String, String>> decisionOptions = op.decisionOptions();
                    String decisionQuestion = op.decisionQuestion();

                    if ("decision".equals(nodeType)) {
                        if (!nonBlank(decisionQuestion)) {
                            decisionQuestion = "Seleccione una opción";
                        }
                        if (decisionOptions == null || decisionOptions.size() < 2) {
                            decisionOptions = List.of(
                                Map.of("value", "SI", "label", "Sí"),
                                Map.of("value", "NO", "label", "No")
                            );
                        }
                    } else {
                        decisionQuestion = null;
                        decisionOptions = null;
                    }

                    normalizedOps.add(new WorkflowAiOperation(
                        "createNode",
                        alias,
                        null,
                        null,
                        nodeType,
                        label,
                        null,
                        null,
                        op.departmentName(),
                        op.instructions(),
                        decisionQuestion,
                        decisionOptions,
                        null,
                        null,
                        op.x(),
                        op.y()
                    ));

                    createdAliasesInOrder.add(alias);
                }

                case "connectNodes" -> {
                    hasConnectOps = true;

                    String source = op.source();
                    String target = op.target();

                    if (nonBlank(source) && nonBlank(target)) {
                        String key = source.trim().toLowerCase() + "->" + target.trim().toLowerCase();
                        existingConnections.add(key);

                        normalizedOps.add(new WorkflowAiOperation(
                            "connectNodes",
                            null,
                            null,
                            source,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            op.conditionValue(),
                            op.conditionLabel(),
                            null,
                            null
                        ));
                    }
                }

                case "renameNode", "updateNode", "deleteNode", "disconnectNodes" -> {
                    normalizedOps.add(op);
                }

                default -> {
                    // Ignora tipos desconocidos
                }
            }
        }

        if ("replace".equals(response.mode()) && createdAliasesInOrder.size() > 1 && !hasConnectOps) {
            for (int i = 0; i < createdAliasesInOrder.size() - 1; i++) {
                String source = createdAliasesInOrder.get(i);
                String target = createdAliasesInOrder.get(i + 1);
                String key = source.trim().toLowerCase() + "->" + target.trim().toLowerCase();

                if (!existingConnections.contains(key)) {
                    normalizedOps.add(new WorkflowAiOperation(
                        "connectNodes",
                        null,
                        null,
                        source,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                    ));
                    existingConnections.add(key);
                }
            }
        }

        String summary = nonBlank(response.summary())
            ? response.summary()
            : ("replace".equals(response.mode()) ? "Workflow generado con IA" : "Cambios generados con IA");

        return new WorkflowAiResponse(
            response.mode(),
            summary,
            normalizedOps
        );
    }

    private String getDefaultLabel(String nodeType) {
        return switch (nodeType) {
            case "start" -> "Inicio";
            case "task" -> "Actividad";
            case "decision" -> "Decisión";
            case "fork" -> "Fork";
            case "join" -> "Join";
            case "end" -> "Fin";
            default -> "Actividad";
        };
    }

    private boolean nonBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String extractJsonText(String rawResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse);

        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            String promptFeedback = root.path("promptFeedback").toString();
            throw new RuntimeException("Gemini no devolvió candidatos. promptFeedback=" + promptFeedback);
        }

        JsonNode firstCandidate = candidates.get(0);

        JsonNode finishReason = firstCandidate.path("finishReason");
        if (finishReason.isTextual() && "SAFETY".equalsIgnoreCase(finishReason.asText())) {
            throw new RuntimeException("Gemini bloqueó la respuesta por safety");
        }

        JsonNode parts = firstCandidate.path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new RuntimeException("Gemini no devolvió texto utilizable");
        }

        String text = parts.get(0).path("text").asText();
        if (text == null || text.trim().isEmpty()) {
            throw new RuntimeException("Gemini devolvió una respuesta vacía");
        }

        return sanitizeJson(text);
    }

    private String sanitizeJson(String text) {
        String clean = text.trim();

        if (clean.startsWith("```json")) {
            clean = clean.substring(7).trim();
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3).trim();
        }

        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3).trim();
        }

        return clean;
    }

    private String resolveMode(WorkflowAiCommandRequest request) {
        if (request.forcedMode() != null && !request.forcedMode().isBlank()) {
            return request.forcedMode();
        }

        return inferIntentMode(request.prompt());
    }

    private String inferIntentMode(String prompt) {
        if (prompt == null) {
            return "patch";
        }

        String p = prompt.trim().toLowerCase();

        boolean replaceIntent =
            p.contains("crea un workflow") ||
            p.contains("crear un workflow") ||
            p.contains("crea un flujo") ||
            p.contains("crear un flujo") ||
            p.contains("haz un workflow") ||
            p.contains("hazme un workflow") ||
            p.contains("genera un workflow") ||
            p.contains("nuevo workflow") ||
            p.contains("nuevo flujo") ||
            p.contains("desde cero") ||
            p.contains("comienza de cero") ||
            p.contains("empieza de cero") ||
            p.contains("crea una politica de negocio") ||
            p.contains("genera una politica de negocio") ||
            p.contains("reemplaza todo");

        boolean patchIntent =
            p.contains("agrega") ||
            p.contains("añade") ||
            p.contains("aumenta") ||
            p.contains("inserta") ||
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
}