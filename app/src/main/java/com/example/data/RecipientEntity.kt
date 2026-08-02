package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipients")
data class RecipientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val name: String = "",
    val extraData: String = "", // JSON or formatted key-values
    val status: String = "PENDING", // PENDING, SENT, FAILED, SKIPPED
    val sentAt: Long? = null,
    val errorMessage: String? = null
)
