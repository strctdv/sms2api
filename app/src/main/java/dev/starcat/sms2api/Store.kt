package dev.starcat.sms2api

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.properties.Delegates

class ServiceBuilder(baseUrl: String) {

    private val gson = GsonBuilder().setLenient().create()

    private val client = OkHttpClient.Builder().build()
    private val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    fun <T> buildService(service: Class<T>): T{
        return retrofit.create(service)
    }
}

object Store {

    private val tag = this.javaClass.simpleName
    private val storeListeners: CopyOnWriteArraySet<StoreListener> = CopyOnWriteArraySet()

    @get:Synchronized @set:Synchronized
    var receivedCount: Int by Delegates.observable(0) { property, old, new ->
        onCounterChange()
        Log.d(tag, "Received message count: $new")
    }

    @get:Synchronized @set:Synchronized
    var forwardedCount: Int by Delegates.observable(0) { property, old, new ->
        onCounterChange()
        Log.d(tag, "Forwarded message count: $new")
    }

    @get:Synchronized @set:Synchronized
    var failedCount: Int by Delegates.observable(0) { property, old, new ->
        onCounterChange()
        Log.d(tag, "Failed message count: $new")
    }

    @get:Synchronized @set:Synchronized
    var url: String by Delegates.observable("") { property, old, new ->
        Log.d(tag, "URL: $new")
    }

    @get:Synchronized @set:Synchronized
    var apiKey: String by Delegates.observable("") { property, old, new ->
        Log.d(tag, "API key: $new")
    }

    @get:Synchronized @set:Synchronized
    var userDefinedId: String by Delegates.observable("") { property, old, new ->
        Log.d(tag, "User defined app ID: $new")
    }

    init {
        Log.d(tag, "Singleton initialized with hash '${this.hashCode()}'")
    }

    fun processMessage(messageContainer: SmsMessageContainer) {

        receivedCount++

        //Add extra user defined fields
        messageContainer.userDefinedId = userDefinedId
        messageContainer.apiKey = apiKey

        Log.d(tag, "Going to process a message: ${messageContainer}")

        forwardMessageToAPI(messageContainer)

    }

    fun addListener(listener: StoreListener) {
        if (!storeListeners.add(listener)) Log.d(tag, "Listener cannot be added! $listener")
        else Log.d(tag, "Listener $listener has been added.")
    }

    fun removeListener(listener: StoreListener) {
        if (!storeListeners.remove(listener)) Log.d(tag, "Listener cannot be removed! $listener")
        else Log.d(tag, "Listener $listener has been removed.")
    }

    private fun onCounterChange() {
        for (listener in storeListeners) listener.onCounterChange()
    }

    private fun forwardMessageToAPI(messageContainer: SmsMessageContainer) {

        try {

            // Retrofit needs an URL string ends width '/', otherwise throws a fatal exception
            // to avoid this behavior, we have to check the string here
            var baseUrl = Store.url.filter { !it.isWhitespace() }

            if (baseUrl.length < Const.MIN_BASE_URL_LENGTH) {
                failedCount++
                Log.w(tag, "Base URL '$baseUrl' seems to short.")
                return
            }

            if (!baseUrl.endsWith("/", true)) {
                baseUrl = "${baseUrl}/"
            }

            Log.d(tag, "Base URL: $baseUrl")

            val retrofit = ServiceBuilder(baseUrl).buildService(ApiService::class.java)

            retrofit.postSmsMessage(messageContainer).enqueue(

                    object : Callback<SmsMessageContainer> {

                        override fun onFailure(call: Call<SmsMessageContainer>, t: Throwable) {
                            failedCount++
                            Log.e(tag, "Call failed with reason '$t'")
                        }

                        override fun onResponse(call: Call<SmsMessageContainer>, response: Response<SmsMessageContainer>) {

                            val responseBody = response.body()

                            if (response.isSuccessful) {

                                forwardedCount++
                                Log.d(tag, "Call successed with response body '${responseBody}'")

                            } else {

                                failedCount++
                                Log.w(tag, "Call failed with code '${response.code()}' and body '${responseBody}'")

                            }
                        }
                    }
            )
        } catch (t: Throwable) {

            failedCount++
            Log.w(tag, "Something went wrong during the HTTP request.")

            t.printStackTrace()
        }
    }
}