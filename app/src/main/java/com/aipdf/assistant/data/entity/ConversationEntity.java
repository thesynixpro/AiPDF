package com.aipdf.assistant.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "conversations")
public class ConversationEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "title")
    public String title = "";

    @ColumnInfo(name = "provider_id")
    public String providerId;

    @ColumnInfo(name = "model_id")
    public String modelId;

    @ColumnInfo(name = "attached_document_id")
    public long attachedDocumentId;

    @ColumnInfo(name = "attached_document_name")
    public String attachedDocumentName;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
