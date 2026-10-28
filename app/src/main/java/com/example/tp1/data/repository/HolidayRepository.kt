package com.example.tp1.data.repository

import com.example.tp1.data.remote.NagerDateApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Expone los feriados nacionales (AR) de un año como mapa fecha ISO -> nombre.
 * Si la API no está disponible (sin conexión, error, etc.) devuelve un mapa
 * vacío en lugar de propagar la excepción, para que la app siga funcionando
 * y solo se omita el aviso de feriado.
 */
class HolidayRepository {

    private val cache = mutableMapOf<Int, Map<String, String>>()

    suspend fun getHolidaysMap(year: Int, countryCode: String = "AR"): Map<String, String> {
        cache[year]?.let { return it }

        val map = try {
            withContext(Dispatchers.IO) {
                NagerDateApi.getPublicHolidays(year, countryCode)
            }.associate { it.date to it.localName }
        } catch (e: Exception) {
            emptyMap()
        }

        if (map.isNotEmpty()) {
            cache[year] = map
        }

        return map
    }
}
