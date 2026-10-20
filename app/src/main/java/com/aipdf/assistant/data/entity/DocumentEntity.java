package com.aipdf.assistant.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "documents")
public class DocumentEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "name")
    public String name = "";

    @ColumnInfo(name = "path")
    public String path;

    @ColumnInfo(name = "size_bytes")
    public long sizeBytes;

    @ColumnInfo(name = "page_count")
    public int pageCount;

    @ColumnInfo(name = "category")
    public String category;

    @ColumnInfo(name = "is_generated")
    public boolean generated;

    @ColumnInfo(name = "added_at")
    public long addedAt;

    @ColumnInfo(name = "last_opened_at")
    public long lastOpenedAt;

    @ColumnInfo(name = "last_page")
    public int lastPage;
}
