package com.aipdf.assistant.data.repo;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.aipdf.assistant.data.AppDatabase;
import com.aipdf.assistant.data.dao.ConversationDao;
import com.aipdf.assistant.data.dao.MessageDao;
import com.aipdf.assistant.data.entity.ConversationEntity;
import com.aipdf.assistant.data.entity.MessageEntity;
import com.aipdf.assistant.util.AppExecutors;

import java.util.List;

public class ChatRepository {

    private final ConversationDao conversationDao;
    private final MessageDao messageDao;

    public ChatRepository(Context context) {
        AppDatabase db = AppDatabase.get();
        this.conversationDao = db.conversationDao();
        this.messageDao = db.messageDao();
    }

    public LiveData<List<ConversationEntity>> observeConversations() {
        return conversationDao.observeAll();
    }

    public interface IdCallback {
        void onId(long id);
    }

    public void createConversation(String title, String providerId, String modelId,
                                   long docId, String docName, IdCallback callback) {
        AppExecutors.disk(() -> {
            ConversationEntity c = new ConversationEntity();
            c.title = title == null || title.trim().isEmpty() ? "New conversation" : title.trim();
            c.providerId = providerId;
            c.modelId = modelId;
            c.attachedDocumentId = docId;
            c.attachedDocumentName = docName;
            c.createdAt = System.currentTimeMillis();
            c.updatedAt = System.currentTimeMillis();
            long id = conversationDao.insert(c);
            AppExecutors.main(() -> callback.onId(id));
        });
    }

    public void updateConversation(ConversationEntity c) {
        AppExecutors.disk(() -> {
            c.updatedAt = System.currentTimeMillis();
            conversationDao.update(c);
        });
    }

    public void deleteConversation(ConversationEntity c) {
        AppExecutors.disk(() -> {
            messageDao.deleteForConversation(c.id);
            conversationDao.delete(c);
        });
    }

    public ConversationEntity findConversation(long id) {
        return conversationDao.findById(id);
    }

    public void loadMessages(long conversationId, MessagesCallback callback) {
        AppExecutors.disk(() -> {
            List<MessageEntity> messages = messageDao.forConversation(conversationId);
            AppExecutors.main(() -> callback.onMessages(messages));
        });
    }

    public interface MessagesCallback {
        void onMessages(List<MessageEntity> messages);
    }

    public void addMessage(MessageEntity message) {
        AppExecutors.disk(() -> messageDao.insert(message));
    }

    public long addMessageSync(MessageEntity message) {
        return messageDao.insert(message);
    }

    public void updateMessage(MessageEntity message) {
        AppExecutors.disk(() -> messageDao.update(message));
    }

    public void clearAll() {
        AppExecutors.disk(() -> {
            messageDao.clearAll();
            conversationDao.clearAll();
        });
    }
}
