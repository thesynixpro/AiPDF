package com.aipdf.assistant.ai.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class DocumentSpecParserTest {

    private static final String VALID = "{"
            + "\"title\":\"Quarterly Report\","
            + "\"subtitle\":\"Q3\","
            + "\"author\":\"A. Writer\","
            + "\"coverPage\":true,"
            + "\"tableOfContents\":false,"
            + "\"pageSize\":\"LETTER\","
            + "\"orientation\":\"landscape\","
            + "\"sections\":["
            + "  {\"heading\":\"Intro\",\"elements\":["
            + "     {\"type\":\"paragraph\",\"text\":\"Hello world\"},"
            + "     {\"type\":\"list\",\"ordered\":true,\"items\":[\"one\",\"two\"]}"
            + "  ]},"
            + "  {\"heading\":\"Data\",\"elements\":["
            + "     {\"type\":\"table\",\"headers\":[\"A\",\"B\"],\"rows\":[[\"1\",\"2\"]]}"
            + "  ]}"
            + "]}";

    @Test
    public void stripsCodeFencesAndProse() {
        String wrapped = "Here is your document:\n```json\n" + VALID + "\n```\nEnjoy!";
        DocumentSpec spec = DocumentSpecParser.parse(wrapped);
        assertNotNull(spec);
        assertEquals("Quarterly Report", spec.title);
    }

    @Test
    public void parsesStructure() {
        DocumentSpec spec = DocumentSpecParser.parse(VALID);
        assertNotNull(spec);
        assertTrue(spec.isValid());
        assertEquals("Quarterly Report", spec.title);
        assertEquals("A. Writer", spec.author);
        assertEquals("LETTER", spec.pageSize);
        assertEquals("landscape", spec.orientation);
        assertFalse(spec.tableOfContents);
        assertEquals(2, spec.sections.size());
        DocumentSpec.Section intro = spec.sections.get(0);
        assertEquals("Intro", intro.heading);
        assertEquals(2, intro.elements.size());
        assertEquals("paragraph", intro.elements.get(0).type);
        DocumentSpec.Element list = intro.elements.get(1);
        assertEquals("list", list.type);
        assertTrue(list.ordered);
        assertEquals(2, list.items.size());
        DocumentSpec.Element table = spec.sections.get(1).elements.get(0);
        assertEquals("table", table.type);
        assertEquals(2, table.headers.size());
        assertEquals(1, table.rows.size());
    }

    @Test
    public void returnsNullForNonJson() {
        assertNull(DocumentSpecParser.parse("Just prose with no JSON."));
    }

    @Test
    public void parsesClarificationsFromArray() {
        List<String> questions = DocumentSpecParser.parseClarifications(
                "{\"clarify\":[\"What tone?\",\"How many pages?\"]}");
        assertNotNull(questions);
        assertEquals(2, questions.size());
        assertEquals("What tone?", questions.get(0));
    }

    @Test
    public void parsesClarificationsFromObjects() {
        List<String> questions = DocumentSpecParser.parseClarifications(
                "{\"clarify\":[{\"question\":\"Audience?\"}]}");
        assertNotNull(questions);
        assertEquals(1, questions.size());
        assertEquals("Audience?", questions.get(0));
    }

    @Test
    public void clarificationReturnsNullWhenAbsent() {
        assertNull(DocumentSpecParser.parseClarifications(VALID));
    }

    @Test
    public void roundTripsThroughJson() {
        DocumentSpec spec = DocumentSpecParser.parse(VALID);
        String json = DocumentSpecParser.toJson(spec);
        DocumentSpec back = DocumentSpecParser.fromJsonString(json);
        assertNotNull(back);
        assertEquals(spec.title, back.title);
        assertEquals(spec.sections.size(), back.sections.size());
        assertEquals(spec.sections.get(0).elements.size(), back.sections.get(0).elements.size());
    }

    @Test
    public void handlesStringSectionContent() {
        String json = "{\"title\":\"T\",\"sections\":[{\"heading\":\"H\",\"text\":\"body\"}]}";
        DocumentSpec spec = DocumentSpecParser.parse(json);
        assertNotNull(spec);
        assertEquals(1, spec.sections.get(0).elements.size());
        assertEquals("body", spec.sections.get(0).elements.get(0).text);
    }
}
