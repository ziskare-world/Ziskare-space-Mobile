package com.example.models

data class RemoteProcess(
    val pid: Int,
    val name: String,
    val cpuPercentage: Float,
    val memoryPercentage: Float,
    val owner: String,
    val status: String // Running, Sleeping, Stopped, Zombie
)

data class ResourceHistoryPoint(
    val timestamp: String,
    val cpu: Float,
    val ram: Float,
    val networkIn: Float, // MB/s
    val networkOut: Float // MB/s
)

data class SshOutputLine(
    val message: String,
    val type: LogType = LogType.OUTPUT
)

enum class LogType {
    INPUT, OUTPUT, ERROR, SYSTEM
}
