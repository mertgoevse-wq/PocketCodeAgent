package com.pocketcodeagent.data.repository

import com.pocketcodeagent.data.local.AppDatabase
import com.pocketcodeagent.data.local.entity.LogEntity
import com.pocketcodeagent.data.model.Provider
import com.pocketcodeagent.data.network.ApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProviderRepository(private val database: AppDatabase) {
    private val providerDao = database.providerDao()
    private val logDao = database.logDao()

    val allProvidersFlow: Flow<List<Provider>> = providerDao.getAllProvidersFlow().map { entities ->
        entities.map { Provider.fromEntity(it) }
    }

    suspend fun getAllProviders(): List<Provider> {
        return providerDao.getAllProviders().map { Provider.fromEntity(it) }
    }

    suspend fun getProviderById(id: Int): Provider? {
        val entity = providerDao.getProviderById(id) ?: return null
        return Provider.fromEntity(entity)
    }

    suspend fun saveProvider(provider: Provider): Long {
        log("DATABASE", "Saving provider: ${provider.name} with model: ${provider.modelName}")
        return providerDao.insertProvider(provider.toEntity())
    }

    suspend fun deleteProvider(provider: Provider) {
        log("DATABASE", "Deleting provider: ${provider.name}")
        providerDao.deleteProvider(provider.toEntity())
    }

    suspend fun testProviderConnection(provider: Provider): Result<String> {
        log("NETWORK", "Testing connection to: ${provider.name} (${provider.baseUrl})")
        val result = ApiClient.testProvider(provider)
        if (result.isSuccess) {
            log("NETWORK", "Connection succeeded for ${provider.name}: ${result.getOrNull()}", "INFO")
        } else {
            log("NETWORK", "Connection failed for ${provider.name}: ${result.exceptionOrNull()?.message}", "ERROR")
        }
        return result
    }

    suspend fun log(tag: String, message: String, level: String = "INFO") {
        logDao.insertLog(LogEntity(tag = tag, message = message, level = level))
    }

    fun getRecentLogsFlow(): Flow<List<LogEntity>> {
        return logDao.getRecentLogsFlow()
    }

    suspend fun clearLogs() {
        logDao.clearLogs()
    }
}
