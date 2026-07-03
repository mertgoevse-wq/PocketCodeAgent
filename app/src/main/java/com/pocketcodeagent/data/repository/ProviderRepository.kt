package com.pocketcodeagent.data.repository

import com.pocketcodeagent.data.local.AppDatabase
import com.pocketcodeagent.data.local.entity.LogEntity
import com.pocketcodeagent.data.model.Provider
import com.pocketcodeagent.data.model.ProviderTestStatus
import com.pocketcodeagent.data.network.ApiClient
import com.pocketcodeagent.data.network.ProviderConnectionResult
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

    suspend fun testProviderConnection(provider: Provider): ProviderConnectionResult {
        log("NETWORK", "Testing provider connection: ${provider.name} / ${provider.modelName}")
        val result = ApiClient.testProvider(provider)
        if (result.success) {
            log("NETWORK", "Connection succeeded for ${provider.name} / ${provider.modelName}", "INFO")
        } else {
            log("NETWORK", result.sanitizedError ?: "Connection failed for ${provider.name}", "ERROR")
        }
        return result
    }

    suspend fun testAndPersistProviderConnection(provider: Provider): ProviderConnectionResult {
        val result = testProviderConnection(provider)
        if (provider.id > 0) {
            val updatedProvider = provider.copy(
                lastTestStatus = if (result.success) ProviderTestStatus.READY else ProviderTestStatus.ERROR,
                lastErrorSanitized = result.sanitizedError
            )
            providerDao.insertProvider(updatedProvider.toEntity())
        }
        return result
    }

    suspend fun listProviderModels(provider: Provider): Result<List<String>> {
        log("NETWORK", "Loading model list for ${provider.name}")
        return ApiClient.listModels(provider).onFailure { error ->
            log("NETWORK", error.message ?: "Model list failed for ${provider.name}", "ERROR")
        }
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
