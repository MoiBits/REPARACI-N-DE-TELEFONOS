package com.example;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {RepairOrder.class, PhoneSale.class}, version = 8, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract RepairOrderDao repairOrderDao();
    public abstract PhoneSaleDao phoneSaleDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "moibitrepair_db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        if (instance != null) {
            if (instance.isOpen()) {
                instance.close();
            }
            instance = null;
        }
    }
}
