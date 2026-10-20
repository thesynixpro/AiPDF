package com.aipdf.assistant.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.aipdf.assistant.data.entity.ConversationEntity;

import java.util.List;

@Dao
public interface ConversationDao {

    @Insert
    long insert(ConversationEntity conversation);

    @Update
    void update(ConversationEntity conversation);

    @Delete
    void delete(ConversationEntity conversation);

    @Query("SELECT * FROM conversations ORDER BY updated_at DESC")
    LiveData<List<ConversationEntity>> observeAll();

    @Query("SELECT * FROM conversations WHERE id = :id")
    ConversationEntity findById(long id);

    @Query("DELETE FROM conversations")
    void clearAll();
}
