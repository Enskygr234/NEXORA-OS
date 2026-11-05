package com.winterdeveloper.nexora.data.settings

import com.winterdeveloper.nexora.data.local.ThemeProfileDao
import com.winterdeveloper.nexora.data.local.ThemeProfileEntity
import com.winterdeveloper.nexora.domain.model.Appearance
import com.winterdeveloper.nexora.domain.model.ThemeProfile
import com.winterdeveloper.nexora.domain.repository.ThemeProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONException
import org.json.JSONObject

class ThemeProfileRepositoryImpl(private val dao: ThemeProfileDao) : ThemeProfileRepository {

    override val profiles: Flow<List<ThemeProfile>> = dao.observe().map { rows ->
        rows.mapNotNull { row ->
            try {
                ThemeProfile(row.id, row.name, AppearanceJson.decode(JSONObject(row.appearanceJson)))
            } catch (e: JSONException) {
                null
            }
        }
    }

    override suspend fun save(name: String, appearance: Appearance): Long =
        dao.insert(
            ThemeProfileEntity(
                name = name.trim().take(32),
                appearanceJson = AppearanceJson.encode(appearance).toString(),
                createdAt = System.currentTimeMillis(),
            )
        )

    override suspend fun delete(id: Long) {
        dao.delete(id)
    }
}
