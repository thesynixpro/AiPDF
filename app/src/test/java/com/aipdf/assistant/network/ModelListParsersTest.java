package com.aipdf.assistant.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.aipdf.assistant.ai.models.AiModel;

import org.junit.Test;

import java.util.List;

public class ModelListParsersTest {

    @Test
    public void parsesGeminiModels() {
        String json = "{"
                + "\"models\":["
                + "  {\"name\":\"models/gemini-1.5-pro\",\"displayName\":\"Gemini 1.5 Pro\","
                + "   \"description\":\"Pro model\",\"inputTokenLimit\":2097152,"
                + "   \"supportedGenerationMethods\":[\"generateContent\",\"countTokens\"]},"
                + "  {\"name\":\"models/embedding-001\",\"displayName\":\"Embedding\","
                + "   \"supportedGenerationMethods\":[\"embedContent\"]},"
                + "  {\"name\":\"models/gemini-1.5-flash\",\"displayName\":\"Gemini 1.5 Flash\","
                + "   \"supportedGenerationMethods\":[\"generateContent\"]}"
                + "]}";
        List<AiModel> models = ModelListParsers.parseGeminiModels(json);
        assertEquals(2, models.size());
        // Sorted by id.
        assertEquals("gemini-1.5-flash", models.get(0).id);
        assertEquals("gemini-1.5-pro", models.get(1).id);
        assertEquals("Gemini 1.5 Pro", models.get(1).displayName);
        assertEquals(Long.valueOf(2097152L), models.get(1).contextLength);
        assertTrue(models.get(1).supportsGeneration);
    }

    @Test
    public void skipsNonGenerativeGeminiModels() {
        String json = "{\"models\":[{\"name\":\"models/embedding-001\","
                + "\"supportedGenerationMethods\":[\"embedContent\"]}]}";
        List<AiModel> models = ModelListParsers.parseGeminiModels(json);
        assertEquals(0, models.size());
    }

    @Test
    public void handlesMissingGeminiArray() {
        assertTrue(ModelListParsers.parseGeminiModels("{}").isEmpty());
        assertTrue(ModelListParsers.parseGeminiModels("{\"models\":null}").isEmpty());
    }

    @Test
    public void parsesOpenRouterModels() {
        String json = "{"
                + "\"data\":["
                + "  {\"id\":\"openai/gpt-4o\",\"name\":\"GPT-4o\",\"context_length\":128000},"
                + "  {\"id\":\"anthropic/claude-3.5-sonnet\",\"name\":\"Claude 3.5 Sonnet\","
                + "   \"context_length\":200000},"
                + "  {\"id\":\"\",\"name\":\"invalid\"}"
                + "]}";
        List<AiModel> models = ModelListParsers.parseOpenRouterModels(json);
        assertEquals(2, models.size());
        assertEquals("anthropic/claude-3.5-sonnet", models.get(0).id);
        assertEquals("anthropic", models.get(0).providerLabel);
        assertEquals(Long.valueOf(200000L), models.get(0).contextLength);
        assertEquals("openai/gpt-4o", models.get(1).id);
        assertEquals("openai", models.get(1).providerLabel);
    }

    @Test
    public void handlesMissingOpenRouterArray() {
        assertTrue(ModelListParsers.parseOpenRouterModels("{}").isEmpty());
        assertTrue(ModelListParsers.parseOpenRouterModels("{\"data\":[]}").isEmpty());
    }

    @Test
    public void modelContextLabelFormats() {
        AiModel model = new AiModel("m", "M", null, 1000000L, "p", true);
        assertNotNull(model.contextLabel());
        assertEquals("1.0M tokens", model.contextLabel());
        AiModel small = new AiModel("m", "M", null, null, "p", true);
        assertNull(small.contextLabel());
    }

    @Test
    public void jsonRoundTripPreservesModels() {
        String json = "{\"data\":[{\"id\":\"x/y\",\"name\":\"XY\",\"context_length\":4096}]}";
        List<AiModel> models = ModelListParsers.parseOpenRouterModels(json);
        String encoded = ModelListParsers.toJson(models);
        List<AiModel> decoded = ModelListParsers.parseOpenRouterModels(
                "{\"data\":" + encoded + "}");
        assertEquals(1, decoded.size());
        assertEquals("x/y", decoded.get(0).id);
    }
}
