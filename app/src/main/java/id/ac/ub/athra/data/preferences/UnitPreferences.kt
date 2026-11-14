package id.ac.ub.athra.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import id.ac.ub.athra.domain.model.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.preferences by preferencesDataStore("athra_preferences")

class UnitPreferences(private val context: Context) {
    fun observe(userId: String): Flow<WeightUnit> = context.preferences.data.map { values ->
        values[stringPreferencesKey("unit_$userId")]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() }
            ?: WeightUnit.KG
    }

    suspend fun set(userId: String, unit: WeightUnit) {
        context.preferences.edit { it[stringPreferencesKey("unit_$userId")] = unit.name }
    }
}
