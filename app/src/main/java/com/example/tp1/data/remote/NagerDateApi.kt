package com.example.tp1.data.remote

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class HolidayDto(
    val date: String,
    val localName: String
)

/**
 * Cliente mínimo de Nager.Date (https://date.nager.at), API REST pública
 * de feriados que no requiere clave de acceso.
 */
object NagerDateApi {

    private const val BASE_URL = "https://date.nager.at/api/v3/PublicHolidays"

    fun getPublicHolidays(year: Int, countryCode: String): List<HolidayDto> {
        val connection = URL("$BASE_URL/$year/$countryCode").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 8000
        connection.readTimeout = 8000

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return emptyList()
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val holidays = JSONArray(body)

            return buildList {
                for (i in 0 until holidays.length()) {
                    val holiday = holidays.getJSONObject(i)
                    add(
                        HolidayDto(
                            date = holiday.getString("date"),
                            localName = holiday.getString("localName")
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
