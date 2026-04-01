package com.simats.endecryption.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @POST("register")
    fun registerUser(@Body request: RegisterRequest): Call<GenericResponse>

    @POST("verify-otp")
    fun verifyOtp(@Body request: VerifyOtpRequest): Call<GenericResponse>

    @POST("login")
    fun loginUser(@Body request: LoginRequest): Call<LoginResponse>

    @GET("profile/{email}")
    fun getProfile(@Path("email") email: String): Call<UserProfile>

    @POST("update-profile")
    fun updateProfile(@Body request: UpdateProfileRequest): Call<GenericResponse>

    @POST("change-password")
    fun changePassword(@Body request: ChangePasswordRequest): Call<GenericResponse>

    @Multipart
    @POST("encrypt-file")
    fun encryptFile(
        @Part("email") email: RequestBody,
        @Part file: MultipartBody.Part
    ): Call<EncryptionResponse>

    @GET("download-encrypted/{file_id}")
    fun downloadEncryptedFile(@Path("file_id") fileId: Int): Call<ResponseBody>

    @Multipart
    @POST("decrypt-file")
    fun decryptFile(
        @Part file: MultipartBody.Part,
        @Part("decryption_key") decryptionKey: RequestBody,
        @Part("email") email: RequestBody? = null
    ): Call<ResponseBody>

    @GET("encrypted-files/{email}")
    fun getUserFiles(@Path("email") email: String): Call<FileHistoryResponse>

    @GET("encrypted-files/{email}")
    fun getMyFiles(@Path("email") email: String): Call<FileHistoryResponse>

    @POST("forgot-password")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<GenericResponse>

    @POST("verify-otp")
    fun verifyResetOtp(@Body request: VerifyOtpRequest): Call<GenericResponse>

    @GET("history/{email}")
    fun getHistory(@Path("email") email: String): Call<FileHistoryResponse>

    @POST("resend-otp")
    fun resendOtp(@Body request: ForgotPasswordRequest): Call<GenericResponse>

    @POST("reset-password")
    fun resetPassword(@Body request: ResetPasswordRequest): Call<GenericResponse>

    @DELETE("wipe-data/{email}")
    fun wipeData(@Path("email") email: String): Call<GenericResponse>

    @GET("notifications/{email}")
    fun getNotifications(@Path("email") email: String): Call<NotificationRemoteResponse>
}
