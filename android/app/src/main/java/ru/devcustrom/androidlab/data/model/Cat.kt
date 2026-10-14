package ru.devcustrom.androidlab.data.model

import kotlinx.serialization.Serializable

/**
 * Кот в галерее.
 *
 * Данные приходят из **Wikimedia Commons** — свободного хранилища медиафайлов.
 * Раньше лаба использовала CATAAS (`cataas.com`), но его API и CDN картинок
 * нестабильны и недоступны из части регионов, поэтому источник заменён на
 * Commons: и API, и сами картинки отдаются с `wikimedia.org` / `upload.wikimedia.org`.
 *
 * Модель намеренно «плоская»: `id`, `tags`, `mimetype`, `createdAt`, `url`.
 * Никакой логики предметной области — только данные и пара вычисляемых полей.
 */
@Serializable
data class Cat(
    /** Стабильный идентификатор страницы файла в Commons. */
    val id: String,
    /** «Теги» для фильтра — значимые слова из имени файла. */
    val tags: List<String> = emptyList(),
    val mimetype: String = "",
    val createdAt: String = "",
    /** Прямая ссылка на картинку (превью шириной [THUMB_WIDTH]). */
    val url: String = "",
) {
    /** Адрес картинки для Coil. */
    val imageUrl: String get() = url

    /** `Cool Cat` вместо `cool_cat` — для подписи под картинкой. */
    val tagLabel: String get() = tags.joinToString(separator = " · ") { tag ->
        tag.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }

    val title: String get() = tagLabel.ifBlank { FALLBACK_TITLE }

    companion object {
        const val FALLBACK_TITLE = "Кот без тегов"
        /** Ширина превью в пикселях — её же просим у Commons (`iiurlwidth`). */
        const val THUMB_WIDTH = 600
        const val MAX_TAGS = 4
    }
}

// ---------------------------------------------------------------------------
// Разбор ответа Wikimedia Commons API (action=query, formatversion=2).
//
// Ответ большой и вложенный, поэтому для него отдельные DTO. Держим их рядом с
// доменной моделью: здесь живёт единственное место, где один формат превращается
// в другой (`CommonsPage.toCat()`).
// ---------------------------------------------------------------------------

@Serializable
data class CommonsResponse(val query: CommonsQuery? = null)

@Serializable
data class CommonsQuery(val pages: List<CommonsPage> = emptyList())

@Serializable
data class CommonsPage(
    val pageid: Long = 0,
    val title: String = "",
    val imageinfo: List<CommonsImageInfo> = emptyList(),
)

@Serializable
data class CommonsImageInfo(
    val thumburl: String? = null,
    val url: String? = null,
    val mime: String = "",
    val timestamp: String = "",
)

/** Превращает страницу Commons в доменную модель — единственное место маппинга. */
fun CommonsPage.toCat(): Cat {
    val info = imageinfo.firstOrNull()
    return Cat(
        id = pageid.toString(),
        tags = tagsFromTitle(title),
        mimetype = info?.mime.orEmpty(),
        createdAt = info?.timestamp.orEmpty(),
        url = info?.thumburl ?: info?.url.orEmpty(),
    )
}

/**
 * Теги берём из имени файла: у файлов Commons категории почти всегда служебные
 * (`CC-BY-SA-4.0`, `Self-published work`), а в названии есть осмысленные слова
 * («Aska, cat, on her tree» → `aska`, `tree`).
 */
internal fun tagsFromTitle(title: String): List<String> =
    title.removePrefix("File:")
        .substringBeforeLast('.')
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .map { it.lowercase() }
        .filter { it.length >= 3 && it !in TAG_STOPWORDS }
        .distinct()
        .take(Cat.MAX_TAGS)

private val TAG_STOPWORDS = setOf(
    "the", "and", "with", "from", "for", "her", "his", "its", "our", "their",
    "van", "von", "der", "die", "das", "und", "les", "des", "une",
    "cat", "cats", "kitten", "kittens", "kitty",
    "photo", "photos", "image", "picture", "file", "called",
    "jpg", "jpeg", "png", "gif", "webp",
)
