package ru.devcustrom.androidlab.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Кот с CATAAS.
 *
 * Поля повторяют ответ списка `/api/cats` один в один — это осознанно.
 * Модель не должна «улучшать» серверные данные: как только здесь появятся
 * переименованные поля, придётся держать в голове маппинг в обе стороны.
 */
@Serializable
data class Cat(
    val id: String,
    val tags: List<String> = emptyList(),
    val mimetype: String = "",
    val createdAt: String = "",
    /**
     * Приходит только от `/cat?json=true`, у списка `/api/cats` такого поля нет.
     * `ignoreUnknownKeys` позволяет одной модели описывать оба ответа.
     */
    val url: String? = null,
) {
    /**
     * Адрес картинки. Сервер отдаёт готовый `url` только для одного кота,
     * поэтому адрес галереи собирается из `id`: `https://cataas.com/cat/{id}`.
     */
    val imageUrl: String get() = url ?: "https://cataas.com/cat/$id"

    /** `Cool Cat` вместо `cool_cat` — для подписи под картинкой. */
    val tagLabel: String get() = tags.joinToString(separator = " · ") { tag ->
        tag.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }

    val title: String get() = tagLabel.ifBlank { FALLBACK_TITLE }

    companion object {
        const val IMAGE_BASE_URL = "https://cataas.com/cat/"
        const val FALLBACK_TITLE = "Кот без тегов"
    }
}

/**
 * Ответ `/cat?json=true` — **объект**, а не массив, и имя поля другое:
 * `created_at` вместо `createdAt`.
 *
 * Один `@Serializable`-класс не может принять оба варианта: у одного свойства
 * может быть только одно имя в JSON. Поэтому для «случайного кота» — свой DTO,
 * который потом превращается в [Cat].
 */
@Serializable
data class RandomCatDto(
    val id: String,
    val tags: List<String> = emptyList(),
    val mimetype: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val url: String? = null,
)

/** Превращает DTO в доменную модель — единственное место, где живёт маппинг. */
fun RandomCatDto.toCat(): Cat = Cat(
    id = id,
    tags = tags,
    mimetype = mimetype,
    createdAt = createdAt,
    url = url,
)