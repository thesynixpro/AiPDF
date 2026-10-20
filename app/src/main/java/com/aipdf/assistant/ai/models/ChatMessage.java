package com.aipdf.assistant.ai.models;

public class ChatMessage {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_SYSTEM = "system";

    public final String role;
    public final String content;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content == null ? "" : content;
    }

    public static ChatMessage user(String text) {
        return new ChatMessage(ROLE_USER, text);
    }

    public static ChatMessage assistant(String text) {
        return new ChatMessage(ROLE_ASSISTANT, text);
    }

    public static ChatMessage system(String text) {
        return new ChatMessage(ROLE_SYSTEM, text);
    }
}
