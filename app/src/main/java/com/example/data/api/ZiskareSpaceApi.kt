package com.example.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

// --- DATA TRANSFER OBJECTS (DTOs) ---

data class RegisterRequest(
    val name: String,
    val username: String,
    val email: String,
    val password: UserPassword, // can be string or nested depending on setup, we'll use string in helper constructors
    val role: String
)

data class UserPassword(val value: String) // Simple representation

data class RegisterSimpleRequest(
    val name: String,
    val username: String,
    val email: String,
    val password: String,
    val role: String
)

data class LoginRequest(
    val username: String? = null,
    val email: String? = null,
    val identifier: String? = null,
    val password: String
)

data class LoginResponse(
    val token: String?,
    val csrfToken: String?,
    val sessionId: String?,
    val user: ApiUser?
)

data class ApiUser(
    val id: String?,
    val name: String,
    val username: String,
    val email: String,
    val role: String
)

data class PwdResetRequest(val email: String)
data class PwdResetVerify(val email: String, val otp: String)
data class PwdResetVerifyResponse(val resetToken: String, val expiry: Long?)
data class PwdResetConfirm(val email: String, val resetToken: String, val password: String)

data class SuccessMessage(val message: String?)

// Database Management
data class DbConnectionProfile(
    val id: String?,
    val name: String,
    val uri: String,
    val isActive: Boolean? = false
)

data class ActivateDbProfileRequest(val id: String)

data class CreateDatabaseRequest(val dbName: String)

data class CreateCollectionRequest(
    val dbName: String,
    val collection: String
)

data class GetCollectionsRequest(val dbName: String)

data class GetCollectionsResponse(
    val collections: List<String>?,
    val count: Int?
)

data class GetCollectionDataResponse(
    val data: List<Map<String, Any>>?,
    val total: Int?,
    val page: Int?,
    val limit: Int?
)

data class GetSingleDataRequest(
    val dbName: String,
    val collection: String,
    val id: String
)

data class AddDataRequest(
    val database: String,
    val collection: String,
    val data: Map<String, Any>
)

data class AddDataResponse(val insertedId: String?)

data class UpdateDataRequest(
    val dbName: String,
    val collection: String,
    val id: String,
    val updatedData: Map<String, Any>
)

data class DeleteDataRequest(
    val dbName: String,
    val collection: String,
    val id: String
)

data class DeleteCollectionRequest(
    val dbName: String,
    val collection: String
)

data class DeleteDatabaseRequest(val dbName: String)

// Network Settings
data class NetworkConfig(
    val host: String?,
    val port: Int?,
    val sslEnabled: Boolean?,
    val timeoutMs: Int?,
    val rateLimit: Int?
)

data class NetworkStatusResponse(
    val online: Boolean?,
    val host: String?,
    val port: Int?
)

// Email Settings
data class SendEmailRequest(
    val to: String,
    val subject: String,
    val text: String?,
    val html: String?,
    val from: String?,
    val priority: String? = "normal"
)

data class SendEmailResponse(
    val jobId: String?,
    val status: String?
)

data class EmailSettings(
    val smtpHost: String?,
    val smtpPort: Int?,
    val secure: Boolean?,
    val authUser: String?,
    val apiEndpoint: String?
)

data class EmailTemplate(
    val id: String?,
    val name: String,
    val subject: String,
    val content: String
)

// Storage Management
data class StorageItem(
    val name: String,
    val path: String,
    val sizeBytes: Long?,
    val type: String, // "file" or "folder"
    val lastModified: String?
)

data class CreateFolderRequest(
    val type: String, // "local" or "drive"
    val path: String,
    val name: String
)

data class RenameStorageRequest(
    val type: String,
    val target: String,
    val name: String
)

data class DeleteStorageRequest(
    val type: String,
    val target: String
)

data class StorageSettings(
    val totalLimitBytes: Long?,
    val baseLocalPath: String?,
    val driveSyncEnabled: Boolean?
)

data class UserLimitRequest(
    val userId: String,
    val maxLimitBytes: Long
)

data class StorageStatus(
    val health: String?,
    val totalSpaceBytes: Long?,
    val freeSpaceBytes: Long?,
    val usedSpaceBytes: Long?
)

// --- RETROFIT INTERFACE ---

interface ZiskareSpaceApi {

    // --- Authentication ---

    @POST("auth/register")
    suspend fun register(@Body request: RegisterSimpleRequest): Response<ApiUser>

    @POST("auth/authenticate")
    suspend fun authenticate(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/password-reset/request")
    suspend fun requestPasswordReset(@Body request: PwdResetRequest): Response<SuccessMessage>

    @POST("auth/password-reset/verify")
    suspend fun verifyPasswordReset(@Body request: PwdResetVerify): Response<PwdResetVerifyResponse>

    @POST("auth/password-reset/confirm")
    suspend fun confirmPasswordReset(@Body request: PwdResetConfirm): Response<SuccessMessage>

    @GET("auth/me")
    suspend fun getMe(): Response<LoginResponse> // returns user or user details

    @GET("auth/validate-session")
    suspend fun validateSession(): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<SuccessMessage>


    // --- Database Management ---

    @GET("db/connections")
    suspend fun getDbConnections(): Response<List<DbConnectionProfile>>

    @POST("db/connections/activate")
    suspend fun activateDbConnection(@Body request: ActivateDbProfileRequest): Response<DbConnectionProfile>

    @GET("db/list-databases")
    suspend fun listDatabases(): Response<List<String>>

    @POST("db/create-database")
    suspend fun createDatabase(@Body request: CreateDatabaseRequest): Response<SuccessMessage>

    @POST("db/create-collection")
    suspend fun createCollection(@Body request: CreateCollectionRequest): Response<SuccessMessage>

    @POST("db/getCollection")
    suspend fun getCollectionsInDb(@Body request: GetCollectionsRequest): Response<GetCollectionsResponse>

    @GET("db/get-data/{dbName}/{collection}")
    suspend fun getCollectionData(
        @Path("dbName") dbName: String,
        @Path("collection") collection: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Response<GetCollectionDataResponse>

    @POST("db/getSingleData")
    suspend fun getSingleData(@Body request: GetSingleDataRequest): Response<Map<String, Any>>

    @POST("db/addData")
    suspend fun addData(@Body request: AddDataRequest): Response<AddDataResponse>

    @PUT("db/updateData")
    suspend fun updateData(@Body request: UpdateDataRequest): Response<SuccessMessage>

    @HTTP(method = "DELETE", path = "db/deleteData", hasBody = true)
    suspend fun deleteData(@Body request: DeleteDataRequest): Response<SuccessMessage>

    @HTTP(method = "DELETE", path = "db/delete/collection", hasBody = true)
    suspend fun deleteCollection(@Body request: DeleteCollectionRequest): Response<SuccessMessage>

    @HTTP(method = "DELETE", path = "db/delete/database", hasBody = true)
    suspend fun deleteDatabase(@Body request: DeleteDatabaseRequest): Response<SuccessMessage>


    // --- Network Settings ---

    @GET("network/read")
    suspend fun readNetworkConfig(): Response<NetworkConfig>

    @GET("network/status")
    suspend fun getNetworkStatus(): Response<NetworkStatusResponse>

    @PATCH("network/update")
    suspend fun updateNetworkConfig(@Body config: NetworkConfig): Response<NetworkConfig>

    @POST("network/reset")
    suspend fun resetNetworkConfig(): Response<SuccessMessage>


    // --- Email System ---

    @POST("email/send-email")
    suspend fun sendEmail(@Body request: SendEmailRequest): Response<SendEmailResponse>

    @GET("email/email-settings")
    suspend fun getEmailSettings(): Response<EmailSettings>

    @PUT("email/email-settings")
    suspend fun updateEmailSettings(@Body settings: EmailSettings): Response<SuccessMessage>

    @GET("email/get-templates/{dbName}/{collection}")
    suspend fun getTemplates(
        @Path("dbName") dbName: String,
        @Path("collection") collection: String
    ): Response<List<EmailTemplate>>

    @GET("email/get-single-template/{dbName}/{collection}/{id}")
    suspend fun getSingleTemplate(
        @Path("dbName") dbName: String,
        @Path("collection") collection: String,
        @Path("id") id: String
    ): Response<EmailTemplate>


    // --- User Management ---

    @GET("users/api")
    suspend fun getUsersList(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("search") search: String?,
        @Query("role") role: String?
    ): Response<List<ApiUser>>

    @POST("users/api")
    suspend fun createUser(@Body user: RegisterSimpleRequest): Response<ApiUser>

    @PUT("users/api/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body updates: Map<String, Any>
    ): Response<ApiUser>

    @DELETE("users/api/{id}")
    suspend fun deleteUser(@Path("id") id: String): Response<ApiUser>


    // --- Storage Management ---

    @GET("storage/list")
    suspend fun getStorageList(
        @Query("type") type: String, // "local" or "drive"
        @Query("path") path: String,
        @Query("page") page: Int?
    ): Response<List<StorageItem>>

    @Multipart
    @POST("storage/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody,
        @Part("path") path: RequestBody,
        @Part("userId") userId: RequestBody
    ): Response<SuccessMessage>

    @HTTP(method = "DELETE", path = "storage/delete", hasBody = true)
    suspend fun deleteStorageItem(@Body request: DeleteStorageRequest): Response<SuccessMessage>

    @POST("storage/create-folder")
    suspend fun createFolder(@Body request: CreateFolderRequest): Response<SuccessMessage>

    @POST("storage/rename")
    suspend fun renameStorageItem(@Body request: RenameStorageRequest): Response<SuccessMessage>

    @GET("storage/settings")
    suspend fun getStorageSettings(@Query("refresh") refresh: Int): Response<StorageSettings>

    @POST("storage/settings")
    suspend fun updateStorageSettings(@Body settings: StorageSettings): Response<SuccessMessage>

    @POST("storage/user-limit")
    suspend fun setUserStorageLimit(@Body request: UserLimitRequest): Response<SuccessMessage>

    @GET("storage/status")
    suspend fun getStorageStatus(): Response<StorageStatus>
}
