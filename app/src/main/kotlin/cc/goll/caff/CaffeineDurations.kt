package cc.goll.caff


import android.content.Context

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileNotFoundException

import java.util.TreeSet
import java.util.SortedSet

import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException


private const val FILENAME: String = "durations.json"


object CaffeineDurations {
    private lateinit var data: TreeSet<Int>

    private fun setDefault() {
        data = sortedSetOf(5 * 60, 10 * 60, 30 * 60, Int.MAX_VALUE)
    }

    fun get(context: Context): TreeSet<Int> {
        if (!::data.isInitialized) {
            try {
                context.openFileInput(FILENAME).use {
                    val content = it.reader().readText()
                    data = TreeSet(Json.decodeFromString<SortedSet<Int>>(content))
                }
            } catch (e: FileNotFoundException) {
                setDefault()
            } catch (e: SerializationException) {
                setDefault()
            } catch (e: IllegalArgumentException) {
                setDefault()
            }
        }

        return data
    }

    fun save(context: Context) {
        context.openFileOutput(FILENAME, Context.MODE_PRIVATE).use {
            val serialized = Json.encodeToString<SortedSet<Int>>(data)
            it.writer().write(serialized)
        }
    }
}
