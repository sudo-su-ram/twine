package com.tether.app.data.repository

import com.tether.app.data.local.*
import com.tether.app.data.remote.*
import com.tether.app.domain.model.*
import com.tether.app.util.EncryptionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for user and pairing operations
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val pairingDao: PairingDao,
    private val apiService: TetherApiService,
    private val encryptionManager: EncryptionManager
) {
    
    val currentUser: Flow<User?> = userDao.getCurrentUser()
    
    suspend fun getCurrentUserSync(): User? = userDao.getCurrentUserSync()
    
    /**
     * Create or retrieve user account using twin code (no SMS OTP)
     */
    suspend fun createOrGetUser(twinCode: String): Result<User> {
        return try {
            // Hash the twin code to use as phone number hash equivalent
            val phoneNumberHash = hashTwinCode(twinCode)
            
            // Get or generate public key
            val publicKey = encryptionManager.getLocalPublicKey()
                ?: return Result.failure(IllegalStateException("Encryption not initialized"))
            
            val request = AuthRequest(
                phoneNumberHash = phoneNumberHash,
                publicKey = android.util.Base64.encodeToString(publicKey, android.util.Base64.DEFAULT)
            )
            
            val response = apiService.authenticate(request)
            
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                
                val user = User(
                    id = authResponse.userId,
                    phoneNumberHash = phoneNumberHash,
                    displayName = authResponse.displayName,
                    isPaired = authResponse.isPaired,
                    pairedWithUserId = authResponse.pairedWithUserId
                )
                
                userDao.insertUser(user)
                Result.success(user)
            } else {
                Result.failure(Exception("Authentication failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a pairing invite for partner
     */
    suspend fun createPairingInvite(partnerTwinCode: String): Result<PairingInviteResponse> {
        return try {
            val currentUser = getCurrentUserSync()
                ?: return Result.failure(IllegalStateException("No user logged in"))
            
            val partnerPhoneNumberHash = hashTwinCode(partnerTwinCode)
            
            val request = PairingInviteRequest(
                inviterUserId = currentUser.id,
                partnerPhoneNumberHash = partnerPhoneNumberHash
            )
            
            val response = apiService.createPairingInvite(request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create invite: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Confirm pairing with 4-word code
     */
    suspend fun confirmPairing(inviteId: String, inviteCode: String): Result<Pairing> {
        return try {
            val currentUser = getCurrentUserSync()
                ?: return Result.failure(IllegalStateException("No user logged in"))
            
            val publicKey = encryptionManager.getLocalPublicKey()
                ?: return Result.failure(IllegalStateException("Encryption not initialized"))
            
            val request = ConfirmPairingRequest(
                inviteId = inviteId,
                inviteCode = inviteCode,
                acceptorUserId = currentUser.id,
                acceptorPublicKey = android.util.Base64.encodeToString(publicKey, android.util.Base64.DEFAULT)
            )
            
            val response = apiService.confirmPairing(request)
            
            if (response.isSuccessful && response.body() != null) {
                val confirmResponse = response.body()!!
                
                // Save partner's public key for E2E encryption
                val partnerPublicKeyBytes = android.util.Base64.decode(
                    confirmResponse.partnerPublicKey,
                    android.util.Base64.DEFAULT
                )
                
                // Store pairing locally
                val pairing = Pairing(
                    id = confirmResponse.pairingId,
                    localUserId = currentUser.id,
                    partnerUserId = confirmResponse.partnerUserId,
                    partnerPhoneNumberHash = "", // Would get from server
                    status = PairingStatus.ACTIVE,
                    pairingCode = inviteCode,
                    pairedAt = System.currentTimeMillis()
                )
                
                pairingDao.insertPairing(pairing)
                
                // Update user as paired
                userDao.insertUser(currentUser.copy(
                    isPaired = true,
                    pairedWithUserId = confirmResponse.partnerUserId
                ))
                
                Result.success(pairing)
            } else {
                Result.failure(Exception("Failed to confirm pairing: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get active pairing
     */
    fun getActivePairing(): Flow<Pairing?> = pairingDao.getActivePairing()
    
    suspend fun getActivePairingSync(): Pairing? = pairingDao.getActivePairingSync()
    
    /**
     * Unpair from partner
     */
    suspend fun unpair(): Result<Unit> {
        return try {
            val pairing = getActivePairingSync()
                ?: return Result.failure(IllegalStateException("No active pairing"))
            
            val currentUser = getCurrentUserSync()
                ?: return Result.failure(IllegalStateException("No user logged in"))
            
            val request = UnpairRequest(
                pairingId = pairing.id,
                initiatorUserId = currentUser.id
            )
            
            val response = apiService.unpair(pairing.id, request)
            
            if (response.isSuccessful) {
                // Update local state
                pairingDao.updatePairing(pairing.copy(status = PairingStatus.DISSOLVED))
                userDao.insertUser(currentUser.copy(
                    isPaired = false,
                    pairedWithUserId = null
                ))
                
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to unpair: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete account and all data
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            // Clear encryption keys
            encryptionManager.clearKeys()
            
            // Clear local database
            userDao.deleteAllUsers()
            pairingDao.deleteAllPairings()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun hashTwinCode(twinCode: String): String {
        // Simple hash for twin code - in production use proper hashing
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(twinCode.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
