package fr.geoking.arthur.audio.stem

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

object StemArrangementLoader {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun loadFromAssets(context: Context, path: String = "stems/arrangement.json"): StemArrangement {
        val jsonString = context.assets.open(path).bufferedReader().use { it.readText() }
        return json.decodeFromString<StemArrangement>(jsonString)
    }

    fun loadFromString(jsonString: String): StemArrangement {
        return json.decodeFromString<StemArrangement>(jsonString)
    }
}
