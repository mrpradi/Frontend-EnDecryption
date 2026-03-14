package com.simats.endecryption.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    /**
     * DEBUGGING TIPS:
     * 1. If using EMULATOR: Use "http://10.0.2.2:8000/"
     * 2. If using PHYSICAL DEVICE: Use your computer's IP (e.g., "http://192.168.1.5:8000/")
     *    - Find IP by running 'ipconfig' in terminal.
     *    - Ensure phone and PC are on the SAME Wi-Fi network.
     * 3. Ensure your backend server is running on port 8000 and allows external connections (--host 0.0.0.0).
     */
    
    // Change this to your current computer IP or 10.0.2.2 for emulator
    private const val BASE_URL = "http://172.20.10.3:8000/" 

    val instance: ApiService by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS) // Reduced timeout for faster error feedback
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()

        retrofit.create(ApiService::class.java)
    }
}
