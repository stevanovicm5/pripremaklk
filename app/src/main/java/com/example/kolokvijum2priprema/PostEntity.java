package com.example.kolokvijum2priprema;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "posts")
public class PostEntity {

    @PrimaryKey
    public int id;

    public String title;
}
