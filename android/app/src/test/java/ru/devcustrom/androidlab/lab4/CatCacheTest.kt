package ru.devcustrom.androidlab.lab4

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.devcustrom.androidlab.data.model.Cat
import ru.devcustrom.androidlab.data.repository.CatCache
import java.io.File

/**
 * Тесты файлового кэша — Лаба 4.
 *
 * Класс [CatCache] работает с обычным [File] и ничего не знает про Android,
 * поэтому проверяется без Robolectric, без эмулятора и без `Context`.
 * Это и есть главный практический вывод лабы: хочешь тестировать файлы —
 * выноси их в класс, который принимает `File`, а не `Context`.
 */
class CatCacheTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var file: File
    private lateinit var cache: CatCache

    @Before
    fun setUp() {
        file = File(temp.newFolder("files"), CatCache.FILE_NAME)
        cache = CatCache(file)
    }

    private fun cat(id: String, vararg tags: String) = Cat(id = id, tags = tags.toList())

    // region чтение

    @Test
    fun `пустой кэш не существует`() {
        assertFalse(cache.exists())
        assertEquals(0L, cache.sizeBytes())
    }

    @Test
    fun `чтение отсутствующего файла возвращает null а не исключение`() {
        assertNull(cache.read())
        assertNull(cache.readText())
    }

    @Test
    fun `пустой файл это не список а null`() {
        file.writeText("")

        assertNull(cache.read())
    }

    @Test
    fun `повреждённый json не роняет экран`() {
        file.writeText("{это не json")

        assertNull(cache.read())
    }

    // endregion

    // region запись

    @Test
    fun `запись и чтение возвращают тех же котов`() {
        val cats = listOf(cat("a", "black"), cat("b", "cute", "small"))

        cache.write(cats)

        assertTrue(cache.exists())
        assertEquals(cats, cache.read())
    }

    @Test
    fun `повторная запись перезаписывает а не дописывает`() {
        cache.write(listOf(cat("a"), cat("b")))
        cache.write(listOf(cat("c")))

        assertEquals(listOf(cat("c")), cache.read())
    }

    @Test
    fun `создаются промежуточные каталоги`() {
        val deep = File(temp.root, "a/b/c/${CatCache.FILE_NAME}")

        CatCache(deep).write(listOf(cat("a")))

        assertTrue(deep.exists())
    }

    @Test
    fun `пустой список все равно пишется в файл`() {
        cache.write(emptyList())

        assertTrue(cache.exists())
        assertEquals("[]", file.readText())
    }

    // endregion

    // region очистка

    @Test
    fun `очистка удаляет файл`() {
        cache.write(listOf(cat("a")))

        assertTrue(cache.clear())
        assertFalse(cache.exists())
        assertNull(cache.read())
    }

    @Test
    fun `очистка уже пустого кэша не падает`() {
        assertFalse(cache.clear())
    }

    // endregion

    // region удаление одного кота

    @Test
    fun `удаление убирает только нужного кота`() {
        cache.write(listOf(cat("keep"), cat("drop"), cat("keep2")))

        assertTrue(cache.remove("drop"))

        assertEquals(listOf(cat("keep"), cat("keep2")), cache.read())
    }

    @Test
    fun `удаление последнего кота убирает сам файл`() {
        cache.write(listOf(cat("only")))

        assertTrue(cache.remove("only"))

        // Пустой [] и отсутствие файла для приложения равнозначны,
        // но «файла нет» честнее показывается пользователю в интерфейсе.
        assertFalse(cache.exists())
    }

    @Test
    fun `удаление чужого id ничего не делает`() {
        cache.write(listOf(cat("a")))

        assertFalse(cache.remove("b"))

        assertEquals(listOf(cat("a")), cache.read())
    }

    @Test
    fun `удаление из пустого кэша возвращает false`() {
        assertFalse(cache.remove("a"))
    }

    // endregion

    // region прочее

    @Test
    fun `путь указывает на реальный файл`() {
        assertEquals(file.absolutePath, cache.path)
        assertEquals(file, cache.sourceFile)
    }

    @Test
    fun `requireNonEmpty отличает пустой кэш от отсутствующего`() {
        val ok = runCatching { CatCache.requireNonEmpty(listOf(cat("a"))) }
        assertTrue(ok.isSuccess)

        val empty = runCatching { CatCache.requireNonEmpty(emptyList()) }
        assertTrue(empty.isFailure)

        val missing = runCatching { CatCache.requireNonEmpty(null) }
        assertTrue(missing.isFailure)
    }

    @Test
    fun `sizeBytes совпадает с длиной файла`() {
        cache.write(listOf(cat("a"), cat("b")))

        assertEquals(file.length(), cache.sizeBytes())
    }

    // endregion
}