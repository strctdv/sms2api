package dev.starcat.sms2api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("./")
    fun postSmsMessage(@Body smsMessageContainer:  SmsMessageContainer): Call<SmsMessageContainer>
}