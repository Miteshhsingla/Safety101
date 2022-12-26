package com.example.practicum.contacts



import androidx.room.Entity
import androidx.room.PrimaryKey
import java.lang.StringBuilder

@Entity(tableName = "contacts")
data class Contact(

    val name: String,
    var phoneNo: String,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
)



