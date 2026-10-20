package com.aipdf.assistant.pdf;

import com.aipdf.assistant.ai.models.DocumentSpec;

import java.util.ArrayList;
import java.util.List;

/** Built-in document templates used by the template gallery. */
public final class DocumentTemplates {

    public static class Template {
        public final String id;
        public final String name;
        public final String description;
        public final String aiPrompt;

        public Template(String id, String name, String description, String aiPrompt) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.aiPrompt = aiPrompt;
        }

        /** A minimal, editable starter document with no AI needed. */
        public DocumentSpec starterSpec() {
            DocumentSpec spec = new DocumentSpec();
            spec.title = name;
            spec.coverPage = true;
            spec.tableOfContents = true;
            DocumentSpec.Section section = new DocumentSpec.Section();
            section.heading = "Introduction";
            section.elements = new ArrayList<>();
            section.elements.add(DocumentSpec.Element.paragraph("Start writing your " + name.toLowerCase() + " here."));
            DocumentSpec.Element list = DocumentSpec.Element.list(false,
                    java.util.Arrays.asList("First point", "Second point", "Third point"));
            section.elements.add(list);
            spec.sections.add(section);
            return spec;
        }
    }

    private DocumentTemplates() {
    }

    public static List<Template> all() {
        List<Template> list = new ArrayList<>();
        list.add(new Template("cv", "Professional CV",
                "A polished curriculum vitae with a summary, experience and skills.",
                "Create a professional CV for a software developer with a profile summary, work experience, education and skills sections."));
        list.add(new Template("cover_letter", "Cover Letter",
                "A focused, formal cover letter for a job application.",
                "Create a professional cover letter for a job application."));
        list.add(new Template("business_proposal", "Business Proposal",
                "A persuasive proposal with objectives, scope and pricing.",
                "Create a business proposal with an executive summary, objectives, scope, timeline, and pricing table."));
        list.add(new Template("business_plan", "Business Plan",
                "A multi-section plan with market analysis and financials.",
                "Create a 20-page business plan for a new company including executive summary, market analysis, marketing strategy, operations, and financial projections tables."));
        list.add(new Template("invoice", "Invoice",
                "An invoice with customer details and a payment summary table.",
                "Create an invoice with a customer information block, an itemized table, subtotal, tax and total due."));
        list.add(new Template("receipt", "Receipt",
                "A simple purchase receipt with itemized totals.",
                "Create a receipt with an itemized list of purchased items and a payment summary."));
        list.add(new Template("project_report", "Project Report",
                "A structured report with status, risks and conclusions.",
                "Create a project report with overview, milestones, risks and a conclusion."));
        list.add(new Template("assignment", "School Assignment",
                "A clear assignment paper with introduction and sources.",
                "Create a school assignment about the given topic with an introduction, body sections and a conclusion."));
        list.add(new Template("research", "Research Report",
                "An academic-style report with abstract and methodology.",
                "Create a research report with abstract, introduction, methodology, results, discussion and references."));
        list.add(new Template("minutes", "Meeting Minutes",
                "Meeting notes with attendees, agenda and action items.",
                "Create meeting minutes with attendees, agenda, decisions and an action item table."));
        list.add(new Template("study_notes", "Study Notes",
                "Organized notes with key concepts and review questions.",
                "Create study notes organized into chapters with key concepts and review questions."));
        list.add(new Template("ebook", "E-book",
                "A long-form document with chapters and a table of contents.",
                "Create an e-book with multiple chapters, an introduction and a conclusion."));
        list.add(new Template("budget", "Personal Budget Report",
                "A monthly budget with category tables and totals.",
                "Create a monthly personal budget report with income, expense categories as a table, and a summary."));
        list.add(new Template("blank", "Blank Document",
                "Start from an empty page and write freely.",
                "Create a blank document with a title and one empty section for me to edit."));
        return list;
    }

    public static Template byId(String id) {
        for (Template t : all()) if (t.id.equals(id)) return t;
        return null;
    }
}
