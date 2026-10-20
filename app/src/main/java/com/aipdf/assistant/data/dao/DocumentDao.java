package com.aipdf.assistant.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.aipdf.assistant.data.entity.DocumentEntity;

import java.util.List;

@Dao
public interface DocumentDao {

    @Insert
    long insert(DocumentEntity doc);

    @Update
    void update(DocumentEntity doc);

    @Delete
    void delete(DocumentEntity doc);

    @Query("SELECT * FROM documents ORDER BY last_opened_at DESC, added_at DESC")
    LiveData<List<DocumentEntity>> observeAll();

    @Query("SELECT * FROM documents ORDER BY last_opened_at DESC, added_at DESC LIMIT :limit")
    LiveData<List<DocumentEntity>> observeRecent(int limit);

    @Query("SELECT * FROM documents WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC")
    LiveData<List<DocumentEntity>> searchByName(String query);

    @Query("SELECT * FROM documents ORDER BY name COLLATE NOCASE ASC")
    LiveData<List<DocumentEntity>> observeByName();

    @Query("SELECT * FROM documents ORDER BY added_at DESC")
    LiveData<List<DocumentEntity>> observeByDate();

    @Query("SELECT * FROM documents ORDER BY size_bytes DESC")
    LiveData<List<DocumentEntity>> observeBySize();

    @Query("SELECT * FROM documents WHERE id = :id")
    DocumentEntity findById(long id);

    @Query("SELECT * FROM documents WHERE path = :path LIMIT 1")
    DocumentEntity findByPath(String path);

    @Query("SELECT * FROM documents")
    List<DocumentEntity> all();
}
