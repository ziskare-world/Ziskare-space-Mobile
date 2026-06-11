package com.example.data

import kotlinx.coroutines.flow.Flow

class ServerRepository(private val db: AppDatabase) {
    val allServers: Flow<List<ServerEntity>> = db.serverDao().getAllServers()
    val allUsers: Flow<List<UserEntity>> = db.userDao().getAllUsers()
    val recentLogs: Flow<List<ActivityLogEntity>> = db.activityLogDao().getRecentLogs()
    val allAlerts: Flow<List<AlertEntity>> = db.alertDao().getAllAlerts()
    val unreadAlerts: Flow<List<AlertEntity>> = db.alertDao().getUnreadAlerts()

    suspend fun getServerById(id: Int): ServerEntity? = db.serverDao().getServerById(id)
    suspend fun insertServer(server: ServerEntity): Long = db.serverDao().insertServer(server)
    suspend fun updateServer(server: ServerEntity) = db.serverDao().updateServer(server)
    suspend fun deleteServer(server: ServerEntity) = db.serverDao().deleteServer(server)

    suspend fun getUserByUsername(username: String): UserEntity? = db.userDao().getUserByUsername(username)
    suspend fun insertUser(user: UserEntity): Long = db.userDao().insertUser(user)
    suspend fun updateUser(user: UserEntity) = db.userDao().updateUser(user)
    suspend fun deleteUser(user: UserEntity) = db.userDao().deleteUser(user)

    suspend fun insertLog(log: ActivityLogEntity) = db.activityLogDao().insertLog(log)

    suspend fun insertAlert(alert: AlertEntity) = db.alertDao().insertAlert(alert)
    suspend fun markAlertAsRead(alertId: Int) = db.alertDao().markAlertAsRead(alertId)
    suspend fun markAllAlertsAsRead() = db.alertDao().markAllAlertsAsRead()
}
