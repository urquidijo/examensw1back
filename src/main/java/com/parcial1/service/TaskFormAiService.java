package com.parcial1.service;

import static java.util.Map.entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parcial1.dto.FormAiFieldDefinition;
import com.parcial1.dto.FormAiFillRequest;
import com.parcial1.dto.FormAiFillResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class TaskFormAiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public TaskFormAiService(
            WebClient webClient,
            ObjectMapper objectMapper,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash-lite}") String model
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public FormAiFillResponse fillForm(String projectId, String taskId, FormAiFillRequest request) {
        validateRequest(request);

        try {
            String systemInstruction = buildSystemInstruction();
            String userPrompt = buildUserPrompt(projectId, taskId, request);
            Map<String, Object> payload = buildPayload(systemInstruction, userPrompt);

            String rawResponse = callGeminiWithRetry(payload, 3);
            String jsonText = extractJsonText(rawResponse);

            FormAiFillResponse parsed = parseResponse(jsonText, request.getFields());

            if (parsed.getSummary() == null || parsed.getSummary().isBlank()) {
                parsed.setSummary("Formulario completado con IA");
            }

            return parsed;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error completando formulario con IA: " + e.getMessage(), e);
        }
    }

    private void validateRequest(FormAiFillRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("El request no puede ser nulo");
        }

        if (request.getTranscript() == null || request.getTranscript().trim().isEmpty()) {
            throw new IllegalArgumentException("El texto dictado es obligatorio");
        }

        if (request.getFields() == null || request.getFields().isEmpty()) {
            throw new IllegalArgumentException("El formulario no tiene campos disponibles");
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("No se encontró la API key de Gemini. Revisa GEMINI_API_KEY.");
        }
    }

    private Map<String, Object> buildPayload(String systemInstruction, String userPrompt) {
        return Map.ofEntries(
                entry("systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemInstruction))
                )),
                entry("contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", userPrompt))
                        )
                )),
                entry("generationConfig", Map.ofEntries(
                        entry("temperature", 0.1),
                        entry("maxOutputTokens", 2048),
                        entry("responseMimeType", "application/json"),
                        entry("responseSchema", buildResponseSchema())
                ))
        );
    }

    private Map<String, Object> buildResponseSchema() {
        return Map.ofEntries(
                entry("type", "OBJECT"),
                entry("required", List.of("summary", "values")),
                entry("properties", Map.ofEntries(
                        entry("summary", Map.of("type", "STRING")),
                        entry("values", Map.ofEntries(
                                entry("type", "ARRAY"),
                                entry("items", Map.ofEntries(
                                        entry("type", "OBJECT"),
                                        entry("required", List.of("fieldId", "value")),
                                        entry("properties", Map.ofEntries(
                                                entry("fieldId", Map.of("type", "STRING")),
                                                entry("value", Map.of("type", "STRING"))
                                        ))
                                ))
                        ))
                ))
        );
    }

    private String buildSystemInstruction() {
        return """
                Eres una IA que completa formularios dinámicos de tareas a partir de texto dictado por voz.

                RESPUESTA:
                - Responde SOLO JSON válido.
                - No uses markdown.
                - No uses ```json.
                - No expliques nada fuera del JSON.

                FORMATO OBLIGATORIO:
                {
                  "summary": "frase corta",
                  "values": [
                    {
                      "fieldId": "id_del_campo",
                      "value": "valor"
                    }
                  ]
                }

                REGLAS:
                - Usa solamente fieldId existentes.
                - No inventes campos.
                - Si no puedes inferir un campo, no lo incluyas.
                - No devuelvas campos tipo FILE.
                - Para TEXT y TEXTAREA devuelve texto claro.
                - Para NUMBER devuelve solo el número como texto. Ejemplo: "1500".
                - Para DATE devuelve siempre formato yyyy-MM-dd.
                - Si el usuario dice "fecha actual", "hoy", "día de hoy" o algo similar, usa exactamente la FECHA ACTUAL enviada en el contexto.
                - Nunca inventes fechas.
                - Para CHECKBOX devuelve "true" o "false".
                - Para SELECT devuelve exactamente una opción disponible.
                - Si el usuario dicta información del cliente, úsala solo si corresponde al campo.
                - No cambies el significado del texto dictado.
                """;
    }

    private String buildUserPrompt(String projectId, String taskId, FormAiFillRequest request) {
        String fieldsText = request.getFields()
                .stream()
                .map(this::formatField)
                .reduce("", (a, b) -> a + b + "\n");

        return """
                projectId=%s
                taskId=%s
                CONTEXTO:
                Fecha actual: %s
                Tarea: %s
                Ticket: %s
                Cliente: %s
                Descripción del ticket: %s

                CAMPOS DISPONIBLES:
                %s

                TEXTO DICTADO POR EL USUARIO:
                %s
                """.formatted(
                projectId,
                taskId,
                safe(request.getCurrentDate()),
                safe(request.getTaskTitle()),
                safe(request.getTicketTitle()),
                safe(request.getClientName()),
                safe(request.getTicketDescription()),
                fieldsText,
                safe(request.getTranscript())
        );
    }

    private String formatField(FormAiFieldDefinition field) {
        return """
                - fieldId="%s", label="%s", type="%s", required=%s, placeholder="%s", options=%s
                """.formatted(
                safe(field.getId()),
                safe(field.getLabel()),
                safe(field.getType()),
                Boolean.TRUE.equals(field.getRequired()),
                safe(field.getPlaceholder()),
                field.getOptions() == null ? "[]" : field.getOptions().toString()
        ).trim();
    }

    private FormAiFillResponse parseResponse(String jsonText, List<FormAiFieldDefinition> fields) {
        try {
            String cleaned = sanitizeJson(jsonText);
            JsonNode root = objectMapper.readTree(cleaned);

            if (!root.isObject()) {
                throw new IllegalStateException("Gemini no devolvió un objeto JSON válido");
            }

            String summary = root.path("summary").asText("Formulario completado con IA");

            Map<String, FormAiFieldDefinition> fieldMap = new LinkedHashMap<>();
            for (FormAiFieldDefinition field : fields) {
                fieldMap.put(field.getId(), field);
            }

            Map<String, Object> values = new LinkedHashMap<>();

            JsonNode valuesNode = root.path("values");

            if (valuesNode.isArray()) {
                for (JsonNode item : valuesNode) {
                    String fieldId = item.path("fieldId").asText("");
                    String rawValue = item.path("value").asText("");

                    if (!fieldMap.containsKey(fieldId)) {
                        continue;
                    }

                    FormAiFieldDefinition field = fieldMap.get(fieldId);
                    Object normalizedValue = normalizeValue(field, rawValue);

                    if (normalizedValue != null) {
                        values.put(fieldId, normalizedValue);
                    }
                }
            }

            if (valuesNode.isObject()) {
                valuesNode.fields().forEachRemaining(entry -> {
                    String fieldId = entry.getKey();

                    if (!fieldMap.containsKey(fieldId)) {
                        return;
                    }

                    FormAiFieldDefinition field = fieldMap.get(fieldId);
                    Object normalizedValue = normalizeValue(field, entry.getValue().asText(""));

                    if (normalizedValue != null) {
                        values.put(fieldId, normalizedValue);
                    }
                });
            }

            return FormAiFillResponse.builder()
                    .summary(summary)
                    .values(values)
                    .build();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "La IA devolvió JSON inválido para el formulario. Respuesta: " + preview(jsonText),
                    e
            );
        }
    }

    private Object normalizeValue(FormAiFieldDefinition field, String rawValue) {
        if (field == null || field.getId() == null) {
            return null;
        }

        String type = safe(field.getType()).toUpperCase();
        String value = rawValue == null ? "" : rawValue.trim();

        if (value.isBlank()) {
            return null;
        }

        if ("FILE".equals(type)) {
            return null;
        }

        if ("NUMBER".equals(type)) {
            String cleanNumber = value
                    .replace(",", ".")
                    .replaceAll("[^0-9.\\-]", "");

            if (cleanNumber.isBlank()) {
                return null;
            }

            try {
                if (cleanNumber.contains(".")) {
                    return Double.parseDouble(cleanNumber);
                }

                return Long.parseLong(cleanNumber);
            } catch (Exception e) {
                return null;
            }
        }

        if ("CHECKBOX".equals(type)) {
            String normalized = normalizeText(value);

            if (
                    normalized.equals("true") ||
                    normalized.equals("si") ||
                    normalized.equals("sí") ||
                    normalized.equals("verdadero") ||
                    normalized.equals("acepta") ||
                    normalized.equals("aceptado") ||
                    normalized.equals("marcado")
            ) {
                return true;
            }

            if (
                    normalized.equals("false") ||
                    normalized.equals("no") ||
                    normalized.equals("falso") ||
                    normalized.equals("rechaza") ||
                    normalized.equals("rechazado") ||
                    normalized.equals("desmarcado")
            ) {
                return false;
            }

            return null;
        }

        if ("SELECT".equals(type)) {
            if (field.getOptions() == null || field.getOptions().isEmpty()) {
                return null;
            }

            String matched = findBestOption(value, field.getOptions());

            return matched;
        }

       if ("DATE".equals(type)) {
    String normalized = normalizeText(value);

    if (
            normalized.contains("fecha actual") ||
            normalized.equals("hoy") ||
            normalized.contains("dia de hoy") ||
            normalized.contains("día de hoy")
    ) {
        return java.time.LocalDate.now().toString();
    }

    if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
        return value;
    }

    return null;
}

        return value;
    }

    private String findBestOption(String value, List<String> options) {
        String normalizedValue = normalizeText(value);

        for (String option : options) {
            if (normalizeText(option).equals(normalizedValue)) {
                return option;
            }
        }

        for (String option : options) {
            String normalizedOption = normalizeText(option);

            if (
                    normalizedOption.contains(normalizedValue) ||
                    normalizedValue.contains(normalizedOption)
            ) {
                return option;
            }
        }

        return null;
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
                            e
                    );
                }

                if (e.statusCode == 503) {
                    throw new IllegalStateException(
                            "Gemini está temporalmente saturado (503). Intenta nuevamente. Detalle: " + preview(e.responseBody),
                            e
                    );
                }

                throw new IllegalStateException(
                        "Gemini error HTTP " + e.statusCode + ". Detalle: " + preview(e.responseBody),
                        e
                );
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
                .exchangeToMono(response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    if (response.statusCode().is2xxSuccessful()) {
                                        return Mono.just(body);
                                    }

                                    return Mono.error(new GeminiApiException(response.statusCode().value(), body));
                                })
                )
                .timeout(Duration.ofSeconds(40))
                .block();
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