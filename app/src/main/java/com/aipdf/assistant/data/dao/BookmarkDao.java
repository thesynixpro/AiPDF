package com.aipdf.assistant.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.aipdf.assistant.data.entity.BookmarkEntity;

import java.util.List;

@Dao
public interface BookmarkDao {

    @Insert
    long insert(BookmarkEntity bookmark);

    @Query("SELECT * FROM bookmarks WHERE document_id = :documentId ORDER BY page ASC")
    LiveData<List<BookmarkEntity>> observeForDocument(long documentId);

    @Query("SELECT * FROM bookmarks WHERE document_id = :documentId AND page = :page LIMIT 1")
    BookmarkEntity find(long documentId, int page);

    @Query("DELETE FROM bookmarks WHERE document_id = :documentId AND page = :page")
    void deletePage(long documentId, int page);

    @Query("DELETE FROM bookmarks")
    void clearAll();
}
