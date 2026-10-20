package com.aipdf.assistant.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "bookmarks", indices = {@Index("document_id")})
public class BookmarkEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "document_id")
    public long documentId;

    @ColumnInfo(name = "page")
    public int page;

    @NonNull
    @ColumnInfo(name = "label")
    public String label = "";

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
