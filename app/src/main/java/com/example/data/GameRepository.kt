package com.example.data

import android.content.Context
import com.example.model.GameRequirement
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameRepository(private val context: Context) {

  private val moshi: Moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  suspend fun loadGames(): List<GameRequirement> = withContext(Dispatchers.IO) {
    val json = context.assets.open("games.json").bufferedReader().use { it.readText() }
    parseGamesJson(json)
  }

  fun parseGamesJson(json: String): List<GameRequirement> {
    val listType = Types.newParameterizedType(List::class.java, GameRequirement::class.java)
    val adapter = moshi.adapter<List<GameRequirement>>(listType)
    return adapter.fromJson(json).orEmpty()
  }
}
