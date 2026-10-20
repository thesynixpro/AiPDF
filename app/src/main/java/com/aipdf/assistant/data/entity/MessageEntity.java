package com.aipdf.assistant.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "conversation_id")
    public long conversationId;

    @NonNull
    @ColumnInfo(name = "role")
    public String role = "user";

    @NonNull
    @ColumnInfo(name = "content")
    public String content = "";

    @ColumnInfo(name = "is_document_json")
    public boolean documentJson;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
