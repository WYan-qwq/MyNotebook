package com.example.mynotebook.api

import com.example.mynotebook.BuildConfig
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // ---- 可运行时修改的 baseUrl（默认读 BuildConfig）----
    @Volatile
    private var currentBaseUrl: HttpUrl = BuildConfig.BASE_URL.ensureSlash().toHttpUrl()

    @Volatile
    private var retrofit: Retrofit? = null

    // ---- OkHttp ----
    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // ---- 对外暴露的 API（保持你原来的调用习惯）----
    val api: ApiService
        get() = getRetrofit().create(ApiService::class.java)

    // ---- 外部可在调试时动态切换 baseUrl ----
    @Synchronized
    fun setBaseUrl(newUrl: String) {
        val fixed = newUrl.ensureSlash()
        val newHttpUrl = fixed.toHttpUrl()
        if (newHttpUrl == currentBaseUrl) return
        currentBaseUrl = newHttpUrl
        retrofit = null          // 让下次 getRetrofit 重新创建
    }

    // ---- 内部：懒创建 Retrofit，baseUrl 改变时会重建 ----
    private fun getRetrofit(): Retrofit {
        val cached = retrofit
        if (cached != null) return cached
        return synchronized(this) {
            val again = retrofit
            again ?: Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
                .also { retrofit = it }
        }
    }

    // 工具：保证末尾带 /
    private fun String.ensureSlash(): String =
        if (endsWith("/")) this else "$this/"
}


