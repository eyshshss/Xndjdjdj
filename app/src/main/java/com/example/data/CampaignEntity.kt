package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "campaigns")
data class CampaignEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String,
    val senderEmail: String,
    val appPassword: String,
    val smtpHost: String = "smtp.gmail.com",
    val smtpPort: Int = 465,
    val useSsl: Boolean = true,
    val htmlContent: String,
    val delaySeconds: Int = 3,
    val isScheduled: Boolean = false,
    val scheduledTimeMillis: Long? = null,
    val status: String = "DRAFT", // DRAFT, SCHEDULED, RUNNING, COMPLETED, PAUSED
    val createdAt: Long = System.currentTimeMillis()
)
