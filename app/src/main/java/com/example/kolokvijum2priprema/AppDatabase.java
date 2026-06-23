package com.example.kolokvijum2priprema;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {PostEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract PostDao postDao();
}