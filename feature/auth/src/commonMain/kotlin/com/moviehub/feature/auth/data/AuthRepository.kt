package com.moviehub.feature.auth.data

import com.moviehub.core.database.DebridSettingsRepository
import com.moviehub.core.database.TraktSettingsRepository
import com.moviehub.core.network.debrid.RealDebridClient
import com.moviehub.core.network.trakt.TraktClient
import kotlinx.coroutines.delay

interface AuthRepository {
    suspend fun getTraktDeviceCode(): Result<String>
    suspend fun pollTraktAuth(deviceCode: String): Result<Boolean>

    suspend fun getDebridDeviceCode(): Result<String>
    suspend fun pollDebridAuth(deviceCode: String): Result<Boolean>
}

class AuthRepositoryImpl(
    private val realDebridClient: RealDebridClient,
    private val debridSettings: DebridSettingsRepository,
    private val traktClient: TraktClient,
    private val traktSettings: TraktSettingsRepository,
) : AuthRepository {

    private val rdCodes = mutableMapOf<String, String>()
    private val traktCodes = mutableMapOf<String, String>()

    override suspend fun getTraktDeviceCode(): Result<String> {
        return traktClient.getDeviceCode().map { resp ->
            traktCodes[resp.userCode] = resp.deviceCode
            resp.userCode
        }
    }

    override suspend fun pollTraktAuth(deviceCode: String): Result<Boolean> {
        // deviceCode argument is the userCode displayed to the user
        val realDeviceCode = traktCodes[deviceCode] ?: return Result.failure(Exception("Invalid session"))
        
        var attempts = 0
        val maxAttempts = 20
        while (attempts < maxAttempts) {
            attempts++
            val tokenResult = traktClient.pollForToken(realDeviceCode)
            if (tokenResult.isSuccess) {
                val tokenResp = tokenResult.getOrThrow()
                traktSettings.setAccessToken(tokenResp.accessToken)
                traktSettings.setRefreshToken(tokenResp.refreshToken)
                return Result.success(true)
            } else {
                val err = tokenResult.exceptionOrNull()
                if (err?.message?.contains("expired", ignoreCase = true) == true) {
                    return Result.failure(err)
                }
            }
            delay(5000)
        }
        return Result.failure(Exception("Polling timed out"))
    }

    override suspend fun getDebridDeviceCode(): Result<String> {
        return realDebridClient.getDeviceCode().map { resp ->
            rdCodes[resp.userCode] = resp.deviceCode
            resp.userCode
        }
    }

    override suspend fun pollDebridAuth(deviceCode: String): Result<Boolean> {
        // deviceCode argument is the userCode displayed to the user
        val realDeviceCode = rdCodes[deviceCode] ?: return Result.failure(Exception("Invalid session"))
        
        var attempts = 0
        val maxAttempts = 20
        while (attempts < maxAttempts) {
            attempts++
            val tokenResult = realDebridClient.pollForToken(realDeviceCode)
            if (tokenResult.isSuccess) {
                val token = tokenResult.getOrThrow()
                debridSettings.setApiKey("realdebrid", token)
                return Result.success(true)
            } else {
                val err = tokenResult.exceptionOrNull()
                if (err?.message?.contains("expired", ignoreCase = true) == true) {
                    return Result.failure(err)
                }
            }
            delay(5000)
        }
        return Result.failure(Exception("Polling timed out"))
    }
}
