package ru.devcustrom.androidlab.lab2

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.devcustrom.androidlab.data.model.Cat
import ru.devcustrom.androidlab.data.model.CommonsResponse
import ru.devcustrom.androidlab.data.model.tagsFromTitle
import ru.devcustrom.androidlab.data.model.toCat

/**
 * Разбор ответа Wikimedia Commons.
 *
 * Строки записаны как есть, с сервера (`formatversion=2`, поэтому `pages` —
 * массив), чтобы тест ловил то, что прилетает в проде.
 */
class CatParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private fun catsFrom(body: String): List<Cat> =
        json.decodeFromString<CommonsResponse>(body).query?.pages.orEmpty().map { it.toCat() }

    @Test
    fun `разбирает ответ Commons в список котов`() {
        val body = """
            {"batchcomplete":"","query":{"pages":[
              {"pageid":105443106,"ns":6,"title":"File:Aska, cat, on her tree.png",
               "imageinfo":[{"thumburl":"https://upload.wikimedia.org/a/600px-Aska.png",
                 "thumbwidth":600,"thumbheight":400,
                 "url":"https://upload.wikimedia.org/a/Aska.png",
                 "descriptionurl":"https://commons.wikimedia.org/wiki/File:Aska.png",
                 "mime":"image/png","timestamp":"2024-01-02T03:04:05Z"}]}
            ]}}
        """.trimIndent()

        val cats = catsFrom(body)

        assertEquals(1, cats.size)
        assertEquals("105443106", cats[0].id)
        assertEquals("https://upload.wikimedia.org/a/600px-Aska.png", cats[0].imageUrl)
        assertEquals("image/png", cats[0].mimetype)
        assertEquals("2024-01-02T03:04:05Z", cats[0].createdAt)
        assertEquals(listOf("aska", "tree"), cats[0].tags)
    }

    @Test
    fun `лишние поля сервера не ломают разбор`() {
        val body = """
            {"batchcomplete":"","continue":{"gcmcontinue":"abc"},
             "query":{"pages":[
               {"pageid":7,"ns":6,"title":"File:Cute black kitten.jpg",
                "imagerepository":"local","_extra":true,
                "imageinfo":[{"thumburl":"https://upload.wikimedia.org/x.jpg",
                  "mime":"image/jpeg","timestamp":"2024-01-01T00:00:00Z",
                  "thumbattribs":{"width":600}}]}
             ]}}
        """.trimIndent()

        val cats = catsFrom(body)

        assertEquals(1, cats.size)
        assertEquals("7", cats[0].id)
        assertEquals(listOf("cute", "black"), cats[0].tags)
    }

    @Test
    fun `теги берутся из имени файла без служебных слов`() {
        assertEquals(listOf("aska", "tree"), tagsFromTitle("File:Aska, cat, on her tree.png"))
        assertEquals(listOf("anak", "kucing", "taman"), tagsFromTitle("File:Anak Kucing di Taman.jpg"))
        assertTrue(tagsFromTitle("File:The cat photo.jpg").isEmpty())
    }

    @Test
    fun `не больше четырёх тегов`() {
        val tags = tagsFromTitle("File:Fluffy white sleeping tabby kitten portrait.jpg")
        assertEquals(Cat.MAX_TAGS, tags.size)
    }

    @Test
    fun `страница без imageinfo не роняет разбор`() {
        val body = """{"query":{"pages":[{"pageid":1,"title":"File:Cat photo.jpg"}]}}"""

        val cats = catsFrom(body)

        assertEquals("", cats[0].url)
        assertTrue(cats[0].tags.isEmpty())
        assertEquals("Кот без тегов", cats[0].title)
    }

    @Test
    fun `подпись собирается из тегов`() {
        val cat = Cat(id = "abc", tags = listOf("cool_cat", "funny"), createdAt = "")

        assertEquals("Cool cat · Funny", cat.title)
    }

    @Test
    fun `модель переживает круговую сериализацию в кэш`() {
        val original = Cat(
            id = "105443106",
            tags = listOf("aska", "tree"),
            mimetype = "image/png",
            createdAt = "2024-01-02T03:04:05Z",
            url = "https://upload.wikimedia.org/a/600px-Aska.png",
        )

        val restored = json.decodeFromString<List<Cat>>(
            json.encodeToString(listOf(original)),
        )

        assertEquals(listOf(original), restored)
    }
}
