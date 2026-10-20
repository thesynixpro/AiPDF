package com.aipdf.assistant.ai;

import android.content.Context;

import com.aipdf.assistant.R;
import com.aipdf.assistant.ai.models.DocumentSpec;

/**
 * Builds the prompts used for document generation, editing and PDF Q&A.
 */
public final class PromptLibrary {

    private PromptLibrary() {
    }

    public static final String DOC_JSON_SCHEMA = "{\n"
            + "  \"title\": \"Document title\",\n"
            + "  \"subtitle\": \"optional subtitle\",\n"
            + "  \"author\": \"optional author\",\n"
            + "  \"coverPage\": true,\n"
            + "  \"tableOfContents\": true,\n"
            + "  \"pageSize\": \"A4\",\n"
            + "  \"orientation\": \"portrait\",\n"
            + "  \"sections\": [\n"
            + "    {\n"
            + "      \"heading\": \"Section heading\",\n"
            + "      \"elements\": [\n"
            + "        {\"type\": \"paragraph\", \"text\": \"A paragraph of prose.\"},\n"
            + "        {\"type\": \"heading\", \"text\": \"Sub heading\", \"level\": 2},\n"
            + "        {\"type\": \"list\", \"ordered\": false, \"items\": [\"first\", \"second\"]},\n"
            + "        {\"type\": \"table\", \"headers\": [\"A\", \"B\"], \"rows\": [[\"1\", \"2\"], [\"3\", \"4\"]]},\n"
            + "        {\"type\": \"pageBreak\"}\n"
            + "      ]\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    public static String documentSystemInstruction(String language, int targetPages) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a professional document author and typesetter. ")
                .append("You convert a user request into a structured document specification. ")
                .append("Reply with ONE JSON object only, no prose, no markdown fences. ")
                .append("Use this exact schema:\n")
                .append(DOC_JSON_SCHEMA).append("\n")
                .append("Rules:\n")
                .append("- Use only the element types: paragraph, heading, list, table, pageBreak.\n")
                .append("- Write substantial, accurate, professional content. Do not use placeholders like [insert here].\n")
                .append("- For a CV, invoice or report include well-formed tables where useful.\n")
                .append("- headings inside a section use level 2 or 3.\n")
                .append("- Do not include a page number or footer text; the renderer adds those.\n");
        if (targetPages > 0) {
            sb.append("- Aim for roughly ").append(targetPages)
                    .append(" pages of content (approx. ").append(targetPages * 450)
                    .append(" words across all sections).\n");
        }
        if (language != null && !language.isEmpty() && !"en".equals(language)) {
            sb.append("- Write all content in the requested language: ").append(language).append(".\n");
        }
        sb.append("If critical information is missing you may instead reply with ")
                .append("{\"clarify\": [\"question 1\", \"question 2\"]} to ask up to three questions. ")
                .append("Only do this when generation would otherwise be impossible.");
        return sb.toString();
    }

    public static String documentUserInstruction(Context context, String userPrompt) {
        return "Create the following document:\n" + userPrompt;
    }

    public static String summarySystem(String language) {
        return "You are a precise document analyst. Summarize clearly and accurately. "
                + "Never invent facts that are not present in the document."
                + (language != null && !"en".equals(language) ? " Answer in language code " + language + "." : "");
    }

    public static String editSystem() {
        return "You are a document editing assistant. Improve and transform the provided content "
                + "while preserving meaning unless asked otherwise. Return only the resulting text.";
    }

    public static String documentChatSystem(Context context, String documentTitle) {
        return "You are answering questions about a PDF document titled \"" + documentTitle + "\". "
                + "Use ONLY the provided document excerpts. If the answer is not present, say so plainly. "
                + "Quote section names when helpful. Be concise.";
    }

    public static String documentExcerptBlock(String pageRange, String text) {
        return "\n--- Excerpt (" + pageRange + ") ---\n" + text + "\n--- End excerpt ---\n";
    }

    public static String documentTemplatePrompt(Context context, String templateName, String extra) {
        String base = "Create a " + templateName + ".";
        if (extra != null && !extra.trim().isEmpty()) base += " " + extra.trim();
        return base;
    }
}
