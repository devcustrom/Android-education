package ru.devcustrom.androidlab.lab2

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.devcustrom.androidlab.data.model.Cat
import ru.devcustrom.androidlab.data.model.RandomCatDto
import ru.devcustrom.androidlab.data.model.toCat

/**
 * Разбор настоящих ответов CATAAS.
 *
 * Строки записаны как есть, с сервера — чтобы тест ловил то, что прилетает в
 * проде, а не то, что мы себе представляем.
 */
class CatParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun `разбирает массив котов`() {
        val body = """[{"id":"04eEQhDfAL8l5nt3","tags":["two","double","black"],
            "mimetype":"image/jpeg","createdAt":"2022-07-18T11:28:29.596Z"}]"""

        val cats = json.decodeFromString<List<Cat>>(body)

        assertEquals(1, cats.size)
        assertEquals("04eEQhDfAL8l5nt3", cats[0].id)
        assertEquals(listOf("two", "double", "black"), cats[0].tags)
        assertEquals("image/jpeg", cats[0].mimetype)
    }

    @Test
    fun `лишние поля сервера не ломают разбор`() {
        val body = """[{"id":"abc","tags":["cute"],"mimetype":"image/png",
            "createdAt":"2024-01-01T00:00:00.000Z","_id":"64f0","type":"jpg",
            "resizedUrl":"https://example.com/x.png","size":12345}]"""

        val cats = json.decodeFromString<List<Cat>>(body)

        assertEquals(1, cats.size)
        assertEquals("abc", cats[0].id)
    }

    @Test
    fun `котик без тегов не роняет разбор`() {
        val body = """[{"id":"only-id","mimetype":"image/jpeg"}]"""

        val cats = json.decodeFromString<List<Cat>>(body)

        assertTrue(cats[0].tags.isEmpty())
        assertEquals("Кот без тегов", cats[0].title)
    }

    @Test
    fun `случайный кот приходит объектом с created_at`() {
        val body = """{"id":"VPTdYP5NVR9GC7sZ","tags":["calvin"],
            "created_at":"2021-01-19T21:20:35.611Z",
            "url":"https://cataas.com/cat/VPTdYP5NVR9GC7sZ?position=center",
            "mimetype":"image/jpeg"}"""

        val cat = json.decodeFromString<RandomCatDto>(body).toCat()

        assertEquals("VPTdYP5NVR9GC7sZ", cat.id)
        assertEquals("2021-01-19T21:20:35.611Z", cat.createdAt)
    }

    @Test
    fun `адрес картинки собирается из id когда url нет`() {
        val cat = Cat(id="abc", tags = listOf("cute"), createdAt = "2024-01-01T00:00:00.000Z")

        assertEquals("https://cataas.com/cat/abc", cat.imageUrl)
    }

    @Test
    fun `готовый url сервера важнее собранного из id`() {
        val body = """{"id":"abc","tags":[],"created_at":"2024-01-01T00:00:00.000Z",
            "url":"https://cataas.com/cat/abc?position=center"}"""

        val cat = json.decodeFromString<RandomCatDto>(body).toCat()

        assertEquals("https://cataas.com/cat/abc?position=center", cat.imageUrl)
    }

    @Test
    fun `подпись собирается из тегов`() {
        val cat = Cat(id="abc", tags = listOf("cool_cat", "funny"), createdAt = "")

        assertEquals("Cool cat · Funny", cat.title)
    }

    @Test
    fun ` модель переживает круговую сериализацию в кэш`() {
        val original = Cat(
            id = "abc",
            tags = listOf("cute", "black"),
            mimetype = "image/jpeg",
            createdAt = "2024-01-01T00:00:00.000Z",
        )

        val restored = json.decodeFromString<List<Cat>>(
            json.encodeToString(listOf(original))
        )

        assertEquals(listOf(original), restored)
    }
}