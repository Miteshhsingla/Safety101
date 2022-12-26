package com.example.practicum.contacts


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ContactsDao {

    @Insert
    suspend fun insertContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query(value = "Select count(*) from contacts")
    suspend fun getCount():Int

    @Query(value = "Select * from contacts")
    suspend fun getAllContacts():List<Contact>
}