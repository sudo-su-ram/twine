package com.tether.app.data.remote

import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service interface
 */
interface TetherApiService {
    
    @POST("api/v1/auth")
    suspend fun authenticate(@Body request: AuthRequest): Response<AuthResponse>
    
    @POST("api/v1/pairing/invite")
    suspend fun createPairingInvite(@Body request: PairingInviteRequest): Response<PairingInviteResponse>
    
    @POST("api/v1/pairing/confirm")
    suspend fun confirmPairing(@Body request: ConfirmPairingRequest): Response<PairingConfirmResponse>
    
    @GET("api/v1/pairing/{inviteId}/status")
    suspend fun getPairingInviteStatus(@Path("inviteId") inviteId: String): Response<PairingInviteResponse>
    
    @HTTP(method = "DELETE", path = "api/v1/pairing/{pairingId}", hasBody = true)
    suspend fun unpair(@Path("pairingId") pairingId: String, @Body request: UnpairRequest): Response<Unit>
    
    @POST("api/v1/canvas/sync")
    suspend fun syncCanvas(@Body request: CanvasSyncRequest): Response<CanvasSyncResponse>
    
    @PUT("api/v1/canvas/background")
    suspend fun updateBackground(@Body request: BackgroundUpdateRequest): Response<Unit>
    
    @GET("api/v1/user/{userId}/public-key")
    suspend fun getUserPublicKey(@Path("userId") userId: String): Response<String>
}
