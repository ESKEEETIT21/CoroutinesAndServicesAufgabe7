package com.example.jetpackcompose.api

import android.util.Log
import com.example.jetpackcompose.data.ForecastData
import com.example.jetpackcompose.data.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

object WeatherApiService {
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

    private val client = OkHttpClient.Builder().build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(WeatherApi::class.java)

    /**
     * Defines the Weather API endpoints for fetching weather data.
     */
    interface WeatherApi {
        /**
         * Fetches the current weather data for a given city.
         *
         * @param city The name of the city.
         * @param apiKey The API key for authentication.
         * @param units The unit system to use (default is "metric").
         * @return A Retrofit Response containing the weather data.
         */
        @GET("weather")
        suspend fun fetchWeather(
            @Query("q") city: String,
            @Query("appid") apiKey: String,
            @Query("units") units: String = "metric"
        ): retrofit2.Response<WeatherData>

        /**
         * Fetches the weather forecast data for a given city.
         *
         * @param city The name of the city.
         * @param apiKey The API key for authentication.
         * @param units The unit system to use (default is "metric").
         * @return A Retrofit Response containing the forecast data.
         */
        @GET("forecast")
        suspend fun fetchForecast(
            @Query("q") city: String,
            @Query("appid") apiKey: String,
            @Query("units") units: String = "metric"
        ): retrofit2.Response<ForecastData>
    }

    /**
     * Fetches the current weather data for a specified city using the Weather API.
     *
     * @param city The name of the city for which to fetch weather data.
     * @param apiKey The API key for authenticating the request.
     * @return The WeatherData object if the request is successful, or `null` otherwise.
     */
    suspend fun fetchWeather(city: String, apiKey: String): WeatherData? {
        return try {
            withContext(Dispatchers.Default) {
                val response = api.fetchWeather(city, apiKey)
                if (response.isSuccessful) {
                    response.body()
                } else {
                    Log.e("WeatherApiService", "Failed to fetch data: ${response.code()}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherApiService", "Error fetching data: ${e.message}")
            null
        }
    }

    // TODO: Methode fetchForecast implementieren, um die Wettervorhersage abzurufen.
    /**
     * Fetches the weather forecast data for a specified city using the Weather API.
     *
     * @param city The name of the city for which to fetch forecast data.
     * @param apiKey The API key for authenticating the request.
     * @return The ForecastData object if the request is successful, or `null` otherwise.
     */
    suspend fun fetchForecast(city: String, apiKey: String): ForecastData? {
        return try {
            withContext(Dispatchers.IO) {
                val response = api.fetchForecast(city, apiKey)
                if (response.isSuccessful) {
                    response.body()
                } else {
                    Log.e("WeatherApiService", "Failed to fetch data: ${response.code()}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherApiService", "Error fetching data: ${e.message}")
            null
        }
    }
}