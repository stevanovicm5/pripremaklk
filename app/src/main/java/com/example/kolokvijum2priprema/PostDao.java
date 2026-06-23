package com.example.kolokvijum2priprema;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;
@Dao
public interface PostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PostEntity> posts);

    @Query("SELECT * FROM posts LIMIT 1")
    PostEntity getFirstPost();

    @Query("DELETE FROM posts")
    void deleteAll();

    @Query("DELETE FROM posts WHERE id = (SELECT id FROM posts LIMIT 1)")
    void deleteFirst();
}
