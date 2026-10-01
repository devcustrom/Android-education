package ru.devcustrom.androidlab.probe

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ProbeDto(val id: String, val value: Double)

internal object SerializationProbe {
    private val json = Json { ignoreUnknownKeys = true }

    fun roundTrip(): Double = json.decodeFromString<ProbeDto>("""{"id":"x","value":1.5}""").value
}