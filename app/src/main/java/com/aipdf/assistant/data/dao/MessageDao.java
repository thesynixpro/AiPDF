package com.aipdf.assistant.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.aipdf.assistant.data.entity.MessageEntity;

import java.util.List;

@Dao
public interface MessageDao {

    @Insert
    long insert(MessageEntity message);

    @Update
    void update(MessageEntity message);

    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId ORDER BY created_at ASC, id ASC")
    List<MessageEntity> forConversation(long conversationId);

    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId ORDER BY created_at DESC, id DESC LIMIT 1")
    MessageEntity latestForConversation(long conversationId);

    @Query("DELETE FROM messages WHERE conversation_id = :conversationId")
    void deleteForConversation(long conversationId);

    @Query("DELETE FROM messages")
    void clearAll();
}
