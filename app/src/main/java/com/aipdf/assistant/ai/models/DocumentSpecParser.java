package com.aipdf.assistant.ai.models;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses the JSON DocumentSpec emitted by the AI, including tolerant handling
 * of code-fence wrapping and slightly malformed structures.
 */
public final class DocumentSpecParser {

    private static final Gson GSON = new Gson();

    private DocumentSpecParser() {
    }

    /** Strips ```json fences and surrounding prose, returning candidate JSON text. */
    public static String extractJson(String raw) {
        if (raw == null) return null;
        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                text = text.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        return text;
    }

    public static DocumentSpec parse(String raw) {
        String json = extractJson(raw);
        if (json == null) return null;
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return fromJson(obj);
        } catch (Exception e) {
            return null;
        }
    }

    /** Returns the clarification question list if the AI asked for missing info. */
    public static List<String> parseClarifications(String raw) {
        String json = extractJson(raw);
        if (json == null || !json.trim().startsWith("{")) return null;
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (!obj.has("clarify")) return null;
            List<String> out = new ArrayList<>();
            JsonElement el = obj.get("clarify");
            if (el.isJsonArray()) {
                for (JsonElement e : el.getAsJsonArray()) {
                    String q = e.isJsonPrimitive() ? e.getAsString()
                            : (e.isJsonObject() && e.getAsJsonObject().has("question")
                            ? e.getAsJsonObject().get("question").getAsString() : null);
                    if (q != null && !q.trim().isEmpty()) out.add(q.trim());
                }
            } else if (el.isJsonPrimitive() && !el.getAsString().trim().isEmpty()) {
                out.add(el.getAsString().trim());
            }
            return out.isEmpty() ? null : out;
        } catch (Exception e) {
            return null;
        }
    }

    public static DocumentSpec fromJson(JsonObject obj) {
        DocumentSpec spec = new DocumentSpec();
        spec.title = getString(obj, "title", "");
        spec.subtitle = getString(obj, "subtitle", getString(obj, "description", ""));
        spec.author = getString(obj, "author", getString(obj, "authorName", ""));
        spec.coverPage = getBool(obj, "coverPage", getBool(obj, "cover", true));
        spec.tableOfContents = getBool(obj, "tableOfContents", getBool(obj, "toc", true));
        spec.pageSize = normalizePageSize(getString(obj, "pageSize", getString(obj, "page_size", "A4")));
        spec.orientation = normalizeOrientation(getString(obj, "orientation", "portrait"));
        spec.language = getString(obj, "language", "en");
        spec.sections = new ArrayList<>();

        JsonArray sections = arrayOrNull(obj, "sections");
        if (sections == null) sections = arrayOrNull(obj, "chapters");
        if (sections == null) return spec;

        for (JsonElement se : sections) {
            if (!se.isJsonObject()) continue;
            JsonObject so = se.getAsJsonObject();
            DocumentSpec.Section section = new DocumentSpec.Section();
            section.heading = getString(so, "heading", getString(so, "title", getString(so, "name", "Section")));
            section.level = getInt(so, "level", 1);
            section.elements = new ArrayList<>();
            JsonArray elems = arrayOrNull(so, "elements");
            if (elems == null) elems = arrayOrNull(so, "content");
            if (elems != null) {
                for (JsonElement ee : elems) {
                    DocumentSpec.Element el = parseElement(ee);
                    if (el != null) section.elements.add(el);
                }
            }
            // String-only section content becomes a paragraph.
            if (section.elements.isEmpty() && so.has("text") && so.get("text").isJsonPrimitive()) {
                section.elements.add(DocumentSpec.Element.paragraph(so.get("text").getAsString()));
            }
            spec.sections.add(section);
        }
        return spec;
    }

    private static DocumentSpec.Element parseElement(JsonElement ee) {
        if (ee.isJsonPrimitive()) {
            String t = ee.getAsString().trim();
            return t.isEmpty() ? null : DocumentSpec.Element.paragraph(t);
        }
        if (!ee.isJsonObject()) return null;
        JsonObject eo = ee.getAsJsonObject();
        String type = getString(eo, "type", "paragraph").toLowerCase(Locale.US);
        DocumentSpec.Element el = new DocumentSpec.Element();
        switch (type) {
            case "heading":
            case "h1":
            case "h2":
            case "h3":
                el.type = "heading";
                el.text = getString(eo, "text", getString(eo, "title", ""));
                el.level = type.equals("h1") ? 1 : type.equals("h2") ? 2 : type.equals("h3") ? 3 : getInt(eo, "level", 2);
                break;
            case "pagebreak":
            case "page_break":
            case "break":
                el.type = "pageBreak";
                break;
            case "list":
            case "bulletlist":
            case "numberedlist":
                el.type = "list";
                el.ordered = getBool(eo, "ordered", type.contains("number") || eo.has("ordered"));
                el.items = new ArrayList<>();
                JsonArray items = arrayOrNull(eo, "items");
                if (items == null) items = arrayOrNull(eo, "text");
                if (items != null) {
                    for (JsonElement ie : items) {
                        if (ie.isJsonPrimitive()) el.items.add(ie.getAsString());
                        else if (ie.isJsonObject() && ie.getAsJsonObject().has("text"))
                            el.items.add(ie.getAsJsonObject().get("text").getAsString());
                    }
                }
                if (el.items.isEmpty() && eo.has("text") && eo.get("text").isJsonPrimitive()) {
                    for (String line : eo.get("text").getAsString().split("\n")) {
                        String trimmed = line.replaceFirst("^\\s*([-•*]|\\d+[.)])\\s*", "").trim();
                        if (!trimmed.isEmpty()) el.items.add(trimmed);
                    }
                }
                break;
            case "table":
                el.type = "table";
                el.headers = new ArrayList<>();
                el.rows = new ArrayList<>();
                JsonArray headers = arrayOrNull(eo, "headers");
                if (headers == null) headers = arrayOrNull(eo, "columns");
                if (headers != null) {
                    for (JsonElement h : headers) if (h.isJsonPrimitive()) el.headers.add(h.getAsString());
                }
                JsonArray rows = arrayOrNull(eo, "rows");
                if (rows != null) {
                    for (JsonElement re : rows) {
                        List<String> row = new ArrayList<>();
                        if (re.isJsonArray()) {
                            for (JsonElement c : re.getAsJsonArray()) {
                                row.add(c.isJsonPrimitive() ? c.getAsString() : "");
                            }
                        } else if (re.isJsonObject() && re.getAsJsonObject().has("cells")) {
                            for (JsonElement c : re.getAsJsonObject().getAsJsonArray("cells")) {
                                row.add(c.isJsonPrimitive() ? c.getAsString() : "");
                            }
                        }
                        if (!row.isEmpty()) el.rows.add(row);
                    }
                }
                break;
            case "paragraph":
            default:
                el.type = "paragraph";
                el.text = getString(eo, "text", getString(eo, "content", getString(eo, "value", "")));
                break;
        }
        if (!"pageBreak".equals(el.type)) {
            boolean empty = el.text == null || el.text.trim().isEmpty();
            if (empty && "paragraph".equals(el.type)) return null;
            if (empty && "heading".equals(el.type)) return null;
            if ("list".equals(el.type) && (el.items == null || el.items.isEmpty())) return null;
            if ("table".equals(el.type) && (el.rows == null || el.rows.isEmpty())) return null;
        }
        return el;
    }

    private static String normalizePageSize(String s) {
        if (s == null) return "A4";
        String v = s.trim().toUpperCase(Locale.US);
        if (v.contains("LETTER")) return "LETTER";
        if (v.contains("LEGAL")) return "LEGAL";
        return "A4";
    }

    private static String normalizeOrientation(String s) {
        if (s == null) return "portrait";
        return s.trim().toLowerCase(Locale.US).contains("land") ? "landscape" : "portrait";
    }

    private static String getString(JsonObject o, String key, String def) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return def;
        JsonElement e = o.get(key);
        if (e.isJsonPrimitive()) {
            try {
                return e.getAsString();
            } catch (Exception ex) {
                return def;
            }
        }
        return def;
    }

    private static boolean getBool(JsonObject o, String key, boolean def) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return def;
        try {
            return o.get(key).getAsBoolean();
        } catch (Exception e) {
            return def;
        }
    }

    private static int getInt(JsonObject o, String key, int def) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return def;
        try {
            return o.get(key).getAsInt();
        } catch (Exception e) {
            return def;
        }
    }

    private static JsonArray arrayOrNull(JsonObject o, String key) {
        if (o == null || !o.has(key) || !o.get(key).isJsonArray()) return null;
        return o.get(key).getAsJsonArray();
    }

    public static String toJson(DocumentSpec spec) {
        return GSON.toJson(spec);
    }

    public static DocumentSpec fromJsonString(String json) {
        try {
            return GSON.fromJson(json, DocumentSpec.class);
        } catch (Exception e) {
            return null;
        }
    }
}
