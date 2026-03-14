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

    @PUT("update-profile/{current_email}")
    fun updateProfile(
        @Path("current_email") currentEmail: String,
        @Body request: UpdateProfileRequest
    ): Call<GenericResponse>

    @PUT("change-password")
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
        @Part file: MultipartBody.Part
    ): Call<ResponseBody>

    @GET("my-files/{email}")
    fun getUserFiles(@Path("email") email: String): Call<FileHistoryResponse>

}
