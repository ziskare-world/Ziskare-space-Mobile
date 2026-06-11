package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val authType: String = "Password", // Password or SSH Key
    val status: String = "Online", // Online, Offline, Warning
    val cpuUsage: Float = 0f, // current usage percentage
    val ramUsage: Float = 0f,
    val diskUsage: Float = 0f,
    val activeAlertsCount: Int = 0,
    val colorHex: String = "#2196F3"
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val role: String, // Administrator, Operator, Read-Only Viewer
    val email: String,
    val mfaEnabled: Boolean = false,
    val mfaSecret: String = ""
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val serverName: String,
    val action: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Success" // Success, Error
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serverId: Int,
    val serverName: String,
    val metric: String, // CPU, RAM, Disk, SSH, Process
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val level: String = "Warning" // Info, Warning, Critical
)
