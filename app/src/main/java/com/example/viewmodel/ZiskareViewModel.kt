package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesManager
import com.example.data.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

sealed class ApiState<out T> {
    object Idle : ApiState<Nothing>()
    object Loading : ApiState<Nothing>()
    data class Success<out T>(val data: T) : ApiState<T>()
    data class Error(val message: String) : ApiState<Nothing>()
}

class ZiskareViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = PreferencesManager(application)

    // Current Server Configurations
    val serverUrl = preferences.serverUrl
    val authToken = preferences.authToken
    val csrfToken = preferences.csrfToken
    val sessionId = preferences.sessionId
    val userName = preferences.userName
    val userRole = preferences.userRole

    // Authentication States
    private val _authState = MutableStateFlow<ApiState<LoginResponse>>(ApiState.Idle)
    val authState: StateFlow<ApiState<LoginResponse>> = _authState.asStateFlow()

    // Database Admin States
    private val _dbConnections = MutableStateFlow<ApiState<List<DbConnectionProfile>>>(ApiState.Idle)
    val dbConnections = _dbConnections.asStateFlow()

    private val _databases = MutableStateFlow<ApiState<List<String>>>(ApiState.Idle)
    val databases = _databases.asStateFlow()

    private val _collectionsObj = MutableStateFlow<ApiState<GetCollectionsResponse>>(ApiState.Idle)
    val collectionsObj = _collectionsObj.asStateFlow()

    private val _collectionData = MutableStateFlow<ApiState<GetCollectionDataResponse>>(ApiState.Idle)
    val collectionData = _collectionData.asStateFlow()

    // Network Config States
    private val _networkConfig = MutableStateFlow<ApiState<NetworkConfig>>(ApiState.Idle)
    val networkConfig = _networkConfig.asStateFlow()

    private val _networkStatus = MutableStateFlow<ApiState<NetworkStatusResponse>>(ApiState.Idle)
    val networkStatus = _networkStatus.asStateFlow()

    // Storage States
    private val _storageList = MutableStateFlow<ApiState<List<StorageItem>>>(ApiState.Idle)
    val storageList = _storageList.asStateFlow()

    private val _storageStatus = MutableStateFlow<ApiState<StorageStatus>>(ApiState.Idle)
    val storageStatus = _storageStatus.asStateFlow()

    private val _storageSettings = MutableStateFlow<ApiState<StorageSettings>>(ApiState.Idle)
    val storageSettings = _storageSettings.asStateFlow()

    // Email States
    private val _emailSettings = MutableStateFlow<ApiState<EmailSettings>>(ApiState.Idle)
    val emailSettings = _emailSettings.asStateFlow()

    private val _emailTemplates = MutableStateFlow<ApiState<List<EmailTemplate>>>(ApiState.Idle)
    val emailTemplates = _emailTemplates.asStateFlow()

    // User Manager States
    private val _apiUsers = MutableStateFlow<ApiState<List<ApiUser>>>(ApiState.Idle)
    val apiUsers = _apiUsers.asStateFlow()

    // Generic Action feedback state
    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    private suspend fun getApi(): ZiskareSpaceApi {
        val url = serverUrl.first()
        val token = authToken.first()
        val csrf = csrfToken.first()
        val sess = sessionId.first()
        return RetrofitClient.getApi(url, token, csrf, sess)
    }

    fun clearFeedback() {
        _actionFeedback.value = null
    }

    fun updateServerUrl(url: String) {
        viewModelScope.launch {
            preferences.saveServerUrl(url)
            _actionFeedback.value = "Target Server Configured: $url"
        }
    }

    // --- Authentication Actions ---

    fun login(authRequest: LoginRequest) {
        viewModelScope.launch {
            _authState.value = ApiState.Loading
            try {
                val api = getApi()
                val response = api.login(authRequest)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    preferences.saveAuthSession(
                        token = body.token,
                        csrf = body.csrfToken,
                        sessionId = body.sessionId,
                        name = body.user?.name ?: body.user?.username,
                        role = body.user?.role
                    )
                    _authState.value = ApiState.Success(body)
                    _actionFeedback.value = "Authenticated successfully!"
                } else {
                    _authState.value = ApiState.Error("Authentication failed: Status ${response.code()}")
                }
            } catch (e: Exception) {
                _authState.value = ApiState.Error("Network error: ${e.localizedMessage}")
            }
        }
    }

    fun register(req: RegisterSimpleRequest) {
        viewModelScope.launch {
            try {
                val response = getApi().register(req)
                if (response.isSuccessful) {
                    _actionFeedback.value = "Registered administrative node client successfully!"
                } else {
                    _actionFeedback.value = "Registration denied: Error ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = "Web-connection failure: ${e.localizedMessage}"
            }
        }
    }

    fun validateCurrentSession() {
        viewModelScope.launch {
            try {
                val api = getApi()
                val response = api.validateSession()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    preferences.saveAuthSession(
                        token = body.token,
                        csrf = body.csrfToken,
                        sessionId = body.sessionId,
                        name = body.user?.name ?: body.user?.username,
                        role = body.user?.role
                    )
                } else {
                    preferences.clearSession()
                }
            } catch (e: Exception) {
                // Keep session offline cache intact if network temporarily disrupted
            }
        }
    }

    fun performServerLogOut() {
        viewModelScope.launch {
            try {
                getApi().logout()
            } catch (e: Exception) {}
            preferences.clearSession()
            _authState.value = ApiState.Idle
            _actionFeedback.value = "Terminated admin session client."
        }
    }

    // --- Database Management Actions ---

    fun fetchDbConnections() {
        viewModelScope.launch {
            _dbConnections.value = ApiState.Loading
            try {
                val response = getApi().getDbConnections()
                if (response.isSuccessful && response.body() != null) {
                    _dbConnections.value = ApiState.Success(response.body()!!)
                } else {
                    _dbConnections.value = ApiState.Error("Failed parsing connections: ${response.code()}")
                }
            } catch (e: Exception) {
                _dbConnections.value = ApiState.Error(e.localizedMessage ?: "Network error")
            }
        }
    }

    fun activateDbConnection(id: String) {
        viewModelScope.launch {
            try {
                val response = getApi().activateDbConnection(ActivateDbProfileRequest(id))
                if (response.isSuccessful) {
                    _actionFeedback.value = "Activated MongoDB profile successfully!"
                    fetchDbConnections()
                } else {
                    _actionFeedback.value = "Error switching DB profile: ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = "Fail: ${e.localizedMessage}"
            }
        }
    }

    fun listDatabases() {
        viewModelScope.launch {
            _databases.value = ApiState.Loading
            try {
                val response = getApi().listDatabases()
                if (response.isSuccessful && response.body() != null) {
                    _databases.value = ApiState.Success(response.body()!!)
                } else {
                    _databases.value = ApiState.Error("Database fetch failure: ${response.code()}")
                }
            } catch (e: Exception) {
                _databases.value = ApiState.Error(e.localizedMessage ?: "Network exception")
            }
        }
    }

    fun createDatabase(dbName: String) {
        viewModelScope.launch {
            try {
                val response = getApi().createDatabase(CreateDatabaseRequest(dbName))
                if (response.isSuccessful) {
                    _actionFeedback.value = "Database '$dbName' provisioned successfully!"
                    listDatabases()
                } else {
                    _actionFeedback.value = "Error: ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = "Connection error: ${e.localizedMessage}"
            }
        }
    }

    fun getCollectionsInDatabase(dbName: String) {
        viewModelScope.launch {
            _collectionsObj.value = ApiState.Loading
            try {
                val response = getApi().getCollectionsInDb(GetCollectionsRequest(dbName))
                if (response.isSuccessful && response.body() != null) {
                    _collectionsObj.value = ApiState.Success(response.body()!!)
                } else {
                    _collectionsObj.value = ApiState.Error("Failed loading collections: Code ${response.code()}")
                }
            } catch (e: Exception) {
                _collectionsObj.value = ApiState.Error(e.localizedMessage ?: "Exception")
            }
        }
    }

    fun createCollection(dbName: String, collection: String) {
        viewModelScope.launch {
            try {
                val response = getApi().createCollection(CreateCollectionRequest(dbName, collection))
                if (response.isSuccessful) {
                    _actionFeedback.value = "Collection '$collection' added to $dbName"
                    getCollectionsInDatabase(dbName)
                } else {
                    _actionFeedback.value = "Error provisioning collection: ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = "Connect error: ${e.localizedMessage}"
            }
        }
    }

    fun getCollectionData(dbName: String, collection: String, page: Int = 1, limit: Int = 50) {
        viewModelScope.launch {
            _collectionData.value = ApiState.Loading
            try {
                val response = getApi().getCollectionData(dbName, collection, page, limit)
                if (response.isSuccessful && response.body() != null) {
                    _collectionData.value = ApiState.Success(response.body()!!)
                } else {
                    _collectionData.value = ApiState.Error("Error: Code ${response.code()}")
                }
            } catch (e: Exception) {
                _collectionData.value = ApiState.Error(e.localizedMessage ?: "Network failed")
            }
        }
    }

    fun addDocumentToCollection(database: String, collection: String, data: Map<String, Any>) {
        viewModelScope.launch {
            try {
                val response = getApi().addData(AddDataRequest(database, collection, data))
                if (response.isSuccessful) {
                    _actionFeedback.value = "Document successfully inserted! ID: ${response.body()?.insertedId}"
                    getCollectionData(database, collection)
                } else {
                    _actionFeedback.value = "Insert failed. Code ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = "Insert error: ${e.localizedMessage}"
            }
        }
    }

    fun updateCollectionData(dbName: String, collection: String, id: String, data: Map<String, Any>) {
        viewModelScope.launch {
            try {
                val response = getApi().updateData(UpdateDataRequest(dbName, collection, id, data))
                if (response.isSuccessful) {
                    _actionFeedback.value = "Document successfully synchronized!"
                    getCollectionData(dbName, collection)
                } else {
                    _actionFeedback.value = "Update mismatch: Code ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = "Access error: ${e.localizedMessage}"
            }
        }
    }

    fun deleteCollectionDataEntry(dbName: String, collection: String, id: String) {
        viewModelScope.launch {
            try {
                val response = getApi().deleteData(DeleteDataRequest(dbName, collection, id))
                if (response.isSuccessful) {
                    _actionFeedback.value = "Deleted document ID: $id"
                    getCollectionData(dbName, collection)
                } else {
                    _actionFeedback.value = "Revocation denied: Code ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = "Revocation failure: ${e.localizedMessage}"
            }
        }
    }

    fun deleteSystemCollection(dbName: String, collection: String) {
        viewModelScope.launch {
            try {
                val response = getApi().deleteCollection(DeleteCollectionRequest(dbName, collection))
                if (response.isSuccessful) {
                    _actionFeedback.value = "Dropped collection '$collection' successfully!"
                    getCollectionsInDatabase(dbName)
                } else {
                    _actionFeedback.value = "Drop denied: Code ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = "Exception: ${e.localizedMessage}"
            }
        }
    }

    fun deleteSystemDatabase(dbName: String) {
        viewModelScope.launch {
            try {
                val response = getApi().deleteDatabase(DeleteDatabaseRequest(dbName))
                if (response.isSuccessful) {
                    _actionFeedback.value = "Dropped system Database '$dbName'"
                    listDatabases()
                } else {
                    _actionFeedback.value = "Error: Code ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = e.localizedMessage
            }
        }
    }

    // --- Network Settings Actions ---

    fun loadNetworkConfig() {
        viewModelScope.launch {
            _networkConfig.value = ApiState.Loading
            try {
                val response = getApi().readNetworkConfig()
                if (response.isSuccessful && response.body() != null) {
                    _networkConfig.value = ApiState.Success(response.body()!!)
                } else {
                    _networkConfig.value = ApiState.Error("Config loading denied: Code ${response.code()}")
                }
            } catch (e: Exception) {
                _networkConfig.value = ApiState.Error(e.localizedMessage ?: "Disconnect")
            }
        }
    }

    fun checkNetworkStatus() {
        viewModelScope.launch {
            _networkStatus.value = ApiState.Loading
            try {
                val response = getApi().getNetworkStatus()
                if (response.isSuccessful && response.body() != null) {
                    _networkStatus.value = ApiState.Success(response.body()!!)
                } else {
                    _networkStatus.value = ApiState.Error("Probe refused. Server Offline.")
                }
            } catch (e: Exception) {
                _networkStatus.value = ApiState.Error(e.localizedMessage ?: "Endpoint unreachable")
            }
        }
    }

    fun updateNetworkSettings(config: NetworkConfig) {
        viewModelScope.launch {
            try {
                val response = getApi().updateNetworkConfig(config)
                if (response.isSuccessful) {
                    _actionFeedback.value = "Network settings updated successfully! Host rebooting."
                    loadNetworkConfig()
                } else {
                    _actionFeedback.value = "Change policy rejected: Code ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = "Fail: ${e.localizedMessage}"
            }
        }
    }

    fun resetNetworkSettings() {
        viewModelScope.launch {
            try {
                val response = getApi().resetNetworkConfig()
                if (response.isSuccessful) {
                    _actionFeedback.value = "Restored factory host parameters!"
                    loadNetworkConfig()
                } else {
                    _actionFeedback.value = "Failure: Code ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = e.localizedMessage
            }
        }
    }

    // --- Email Actions ---

    fun sendEmailPayload(req: SendEmailRequest) {
        viewModelScope.launch {
            try {
                val response = getApi().sendEmail(req)
                if (response.isSuccessful) {
                    _actionFeedback.value = "Email queued successfully! Job: ${response.body()?.jobId}"
                } else {
                    _actionFeedback.value = "Email refused: Status ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = "Network failure: ${e.localizedMessage}"
            }
        }
    }

    fun fetchEmailSettings() {
        viewModelScope.launch {
            _emailSettings.value = ApiState.Loading
            try {
                val response = getApi().getEmailSettings()
                if (response.isSuccessful && response.body() != null) {
                    _emailSettings.value = ApiState.Success(response.body()!!)
                } else {
                    _emailSettings.value = ApiState.Error("${response.code()}")
                }
            } catch (e: Exception) {
                _emailSettings.value = ApiState.Error(e.localizedMessage ?: "Disconnect")
            }
        }
    }

    fun updateEmailSettings(settings: EmailSettings) {
        viewModelScope.launch {
            try {
                val response = getApi().updateEmailSettings(settings)
                if (response.isSuccessful) {
                    _actionFeedback.value = "SMTP mail transport configured successfully!"
                    fetchEmailSettings()
                } else {
                    _actionFeedback.value = "Rejection: Code ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = e.localizedMessage
            }
        }
    }

    fun loadTemplates(db: String, coll: String) {
        viewModelScope.launch {
            _emailTemplates.value = ApiState.Loading
            try {
                val response = getApi().getTemplates(db, coll)
                if (response.isSuccessful && response.body() != null) {
                    _emailTemplates.value = ApiState.Success(response.body()!!)
                } else {
                    _emailTemplates.value = ApiState.Error("${response.code()}")
                }
            } catch (e: Exception) {
                _emailTemplates.value = ApiState.Error(e.localizedMessage ?: "Connect error")
            }
        }
    }

    // --- Storage Actions ---

    fun fetchStorageList(type: String, path: String) {
        viewModelScope.launch {
            _storageList.value = ApiState.Loading
            try {
                val response = getApi().getStorageList(type, path, null)
                if (response.isSuccessful && response.body() != null) {
                    _storageList.value = ApiState.Success(response.body()!!)
                } else {
                    _storageList.value = ApiState.Error("Storage list denied: ${response.code()}")
                }
            } catch (e: Exception) {
                _storageList.value = ApiState.Error(e.localizedMessage ?: "Connection error")
            }
        }
    }

    fun uploadStorageFile(localFile: File, type: String, path: String, userId: String) {
        viewModelScope.launch {
            try {
                val fileReq = localFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                val bodyPart = MultipartBody.Part.createFormData("file", localFile.name, fileReq)
                val typePart = type.toRequestBody("text/plain".toMediaTypeOrNull())
                val pathPart = path.toRequestBody("text/plain".toMediaTypeOrNull())
                val userPart = userId.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = getApi().uploadFile(bodyPart, typePart, pathPart, userPart)
                if (response.isSuccessful) {
                    _actionFeedback.value = "File '${localFile.name}' uploaded successfully!"
                    fetchStorageList(type, path)
                } else {
                    _actionFeedback.value = "Upload denied: Code ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = "Upload connection failure: ${e.localizedMessage}"
            }
        }
    }

    fun createStorageFolder(type: String, path: String, name: String) {
        viewModelScope.launch {
            try {
                val response = getApi().createFolder(CreateFolderRequest(type, path, name))
                if (response.isSuccessful) {
                    _actionFeedback.value = "Directory '$name' provisioned!"
                    fetchStorageList(type, path)
                } else {
                    _actionFeedback.value = "Denial: Code ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = e.localizedMessage
            }
        }
    }

    fun renameStorageItem(type: String, target: String, newName: String, path: String) {
        viewModelScope.launch {
            try {
                val response = getApi().renameStorageItem(RenameStorageRequest(type, target, newName))
                if (response.isSuccessful) {
                    _actionFeedback.value = "Successfully renamed item!"
                    fetchStorageList(type, path)
                } else {
                    _actionFeedback.value = "Rejection: Code ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = e.localizedMessage
            }
        }
    }

    fun deleteStorageItem(type: String, target: String, path: String) {
        viewModelScope.launch {
            try {
                val response = getApi().deleteStorageItem(DeleteStorageRequest(type, target))
                if (response.isSuccessful) {
                    _actionFeedback.value = "Removed storage item successfully!"
                    fetchStorageList(type, path)
                } else {
                    _actionFeedback.value = "Revocation error: Code ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = e.localizedMessage
            }
        }
    }

    fun fetchStorageStatus() {
        viewModelScope.launch {
            _storageStatus.value = ApiState.Loading
            try {
                val response = getApi().getStorageStatus()
                if (response.isSuccessful && response.body() != null) {
                    _storageStatus.value = ApiState.Success(response.body()!!)
                } else {
                    _storageStatus.value = ApiState.Error("${response.code()}")
                }
            } catch (e: Exception) {
                _storageStatus.value = ApiState.Error(e.localizedMessage ?: "Disconnect")
            }
        }
    }

    fun fetchStorageSettings() {
        viewModelScope.launch {
            _storageSettings.value = ApiState.Loading
            try {
                val response = getApi().getStorageSettings(1)
                if (response.isSuccessful && response.body() != null) {
                    _storageSettings.value = ApiState.Success(response.body()!!)
                } else {
                    _storageSettings.value = ApiState.Error("${response.code()}")
                }
            } catch (e: Exception) {
                _storageSettings.value = ApiState.Error(e.localizedMessage ?: "Disconnect")
            }
        }
    }

    fun updateStorageSettings(settings: StorageSettings) {
        viewModelScope.launch {
            try {
                val response = getApi().updateStorageSettings(settings)
                if (response.isSuccessful) {
                    _actionFeedback.value = "Global storage limits reposed!"
                    fetchStorageSettings()
                } else {
                    _actionFeedback.value = "Rejected: Code ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = e.localizedMessage
            }
        }
    }

    fun setUserStorageQuota(userId: String, bytes: Long) {
        viewModelScope.launch {
            try {
                val response = getApi().setUserStorageLimit(UserLimitRequest(userId, bytes))
                if (response.isSuccessful) {
                    _actionFeedback.value = "User limit allocated!"
                } else {
                    _actionFeedback.value = "Allocation denied: Code ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = e.localizedMessage
            }
        }
    }

    // --- Corporate API Users List ---

    fun loadApiUsers() {
        viewModelScope.launch {
            _apiUsers.value = ApiState.Loading
            try {
                val response = getApi().getUsersList(1, 100, null, null)
                if (response.isSuccessful && response.body() != null) {
                    _apiUsers.value = ApiState.Success(response.body()!!)
                } else {
                    _apiUsers.value = ApiState.Error("Forbidden: Code ${response.code()}")
                }
            } catch (e: Exception) {
                _apiUsers.value = ApiState.Error(e.localizedMessage ?: "Disconnect")
            }
        }
    }

    fun createApiUser(req: RegisterSimpleRequest) {
        viewModelScope.launch {
            try {
                val response = getApi().createUser(req)
                if (response.isSuccessful) {
                    _actionFeedback.value = "Created network corporate user: ${req.username}"
                    loadApiUsers()
                } else {
                    _actionFeedback.value = "Rejection: Code ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = e.localizedMessage
            }
        }
    }

    fun updateApiUserPermissions(id: String, role: String) {
        viewModelScope.launch {
            try {
                val response = getApi().updateUser(id, mapOf("role" to role))
                if (response.isSuccessful) {
                    _actionFeedback.value = "User role modified to: $role"
                    loadApiUsers()
                } else {
                    _actionFeedback.value = "Action Rejected: Code ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = e.localizedMessage
            }
        }
    }

    fun deleteApiUserAccount(id: String) {
        viewModelScope.launch {
            try {
                val response = getApi().deleteUser(id)
                if (response.isSuccessful) {
                    _actionFeedback.value = "Corporate user revoked."
                    loadApiUsers()
                } else {
                    _actionFeedback.value = "Revocation Denied: Code ${response.code()}"
                }
            } catch (e: Exception) {
                _actionFeedback.value = e.localizedMessage
            }
        }
    }
}
