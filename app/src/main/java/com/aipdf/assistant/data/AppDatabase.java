package com.aipdf.assistant.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.aipdf.assistant.data.dao.BookmarkDao;
import com.aipdf.assistant.data.dao.ConversationDao;
import com.aipdf.assistant.data.dao.DocumentDao;
import com.aipdf.assistant.data.dao.MessageDao;
import com.aipdf.assistant.data.entity.BookmarkEntity;
import com.aipdf.assistant.data.entity.ConversationEntity;
import com.aipdf.assistant.data.entity.DocumentEntity;
import com.aipdf.assistant.data.entity.MessageEntity;

@Database(
        entities = {DocumentEntity.class, ConversationEntity.class, MessageEntity.class, BookmarkEntity.class},
        version = 1,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract DocumentDao documentDao();

    public abstract ConversationDao conversationDao();

    public abstract MessageDao messageDao();

    public abstract BookmarkDao bookmarkDao();

    public static void init(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "aipdf.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
    }

    public static AppDatabase get() {
        if (instance == null) throw new IllegalStateException("AppDatabase.init not called");
        return instance;
    }
}
