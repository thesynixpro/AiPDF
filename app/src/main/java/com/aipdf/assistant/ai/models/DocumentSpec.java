package com.aipdf.assistant.ai.models;

/**
 * A structured document description produced by the AI (or templates) that the
 * PDF rendering engine turns into a real PDF file.
 */
public class DocumentSpec {

    public String title = "";
    public String subtitle = "";
    public String author = "";
    public boolean coverPage = true;
    public boolean tableOfContents = true;
    public String pageSize = "A4";
    public String orientation = "portrait";
    public String language = "en";
    public java.util.List<Section> sections = new java.util.ArrayList<>();

    public static class Section {
        public String heading = "";
        public int level = 1;
        public java.util.List<Element> elements = new java.util.ArrayList<>();
    }

    public static class Element {
        /** paragraph | list | table | pageBreak | heading */
        public String type = "paragraph";
        public String text = "";
        public boolean ordered = false;
        public java.util.List<String> items = new java.util.ArrayList<>();
        public java.util.List<String> headers = new java.util.ArrayList<>();
        public java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
        public int level = 2;

        public static Element paragraph(String text) {
            Element e = new Element();
            e.type = "paragraph";
            e.text = text;
            return e;
        }

        public static Element heading(String text, int level) {
            Element e = new Element();
            e.type = "heading";
            e.text = text;
            e.level = level;
            return e;
        }

        public static Element list(boolean ordered, java.util.List<String> items) {
            Element e = new Element();
            e.type = "list";
            e.ordered = ordered;
            e.items = items;
            return e;
        }

        public static Element table(java.util.List<String> headers, java.util.List<java.util.List<String>> rows) {
            Element e = new Element();
            e.type = "table";
            e.headers = headers;
            e.rows = rows;
            return e;
        }

        public static Element pageBreak() {
            Element e = new Element();
            e.type = "pageBreak";
            return e;
        }
    }

    public boolean isValid() {
        return title != null && !title.trim().isEmpty() && sections != null && !sections.isEmpty();
    }

    public int estimateElementCount() {
        int n = 0;
        if (sections != null) {
            for (Section s : sections) {
                n++;
                if (s.elements != null) n += s.elements.size();
            }
        }
        return n;
    }
}
