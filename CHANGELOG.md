# Changelog

Все значимые изменения этого репозитория документируются в этом файле.

Формат основан на [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/),
версионирование — по [Semantic Versioning](https://semver.org/lang/ru/).

## [Unreleased]

## [1.2.0] — Лабораторная 3

### Этап D — меню и несколько Activity
- Главный экран переписан: вместо двух кнопок — `RecyclerView` со списком всех шести лабораторных.
- `common/LabItem.kt` + `LabCatalog.all`: список лаб — данные, а не вёрстка. Недоступные лабы
  присутствуют с `activityClass = null` и помечены «В разработке».
- `common/LabAdapter.kt`: `ListAdapter` + `DiffUtil`, бейдж с номером лабы, обработка долгого нажатия.
- `common/LabIntents.kt`: все ключи `Intent` в одном месте.
- `common/StudentPrefs.kt`: имя студента в `SharedPreferences`.
- Options-меню: «О программе», «Настройки», «Выход» (`finishAffinity()`).
- Контекстное меню по долгому нажатию: «Открыть», «Поделиться» (`ACTION_SEND` + `createChooser`).
- `lab3/AboutActivity.kt`: получает имя студента через `getStringExtra`, показывает версию из `PackageInfo`.
- `lab3/SettingsActivity.kt`: ввод и сохранение имени студента.
- `Lab1Activity` отдаёт последний результат через `setResult`, `MainActivity` принимает его
  через `ActivityResultContracts.StartActivityForResult` и показывает в `Snackbar`.

### Принятые технические решения
- Контекстное меню сделано через `PopupMenu`, а не `registerForContextMenu(labsList)`: у
  `RecyclerView` нет `getContextMenuInfo()`, поэтому в `onCreateContextMenu` приходит `info == null`
  и позицию нажатой строки взять неоткуда. Её знает только адаптер.
- `setResult` вызывается **сразу после расчёта**, а не в `onPause` перед закрытием: на
  проверенной версии Android вызов в `onPause` при `isFinishing = true` не доходит —
  колбэк получает `RESULT_CANCELED` и `data == null`. Пустой или некорректный ввод сбрасывает
  результат через `setResult(RESULT_CANCELED)`.
- Все ключи `Intent` вынесены в `LabIntents`: строки в `putExtra`/`getStringExtra` — неявный
  контракт между двумя файлами, который ломается без ошибок компиляции.
- `setSupportActionBar(binding.toolbar)` обязателен: тема `NoActionBar`, без этого вызова
  `onCreateOptionsMenu` не вызывается и меню просто не существует — без единой ошибки в logcat.
- Версия приложения читается из `PackageInfo`, а не из `BuildConfig`: в AGP 9 генерация
  `BuildConfig` выключена по умолчанию.
- Добавлен экран «Настройки», которого нет в плане: пункт меню без действия — это баг, а не
  заглушка. Он же даёт имя студента для передачи через `Intent`.
- Документация: `docs/03-lab-menu-activity.md` с разбором 7 грабель.
- Скриншоты и GIF: `docs/assets/lab3-*` (список, options-меню, контекстное меню,
  «О программе», «Настройки», возвращённый результат).

### Исправления документации
- `docs/01-lab-converter.md` и `docs/02-lab-cats-gallery.md` приведены к шаблону плана:
  добавлены Mermaid-диаграммы архитектуры и разделы «Что получится», «Полезные ссылки»,
  «Коммиты лабы»; удалён дублирующий раздел «Цель».
- Зафиксировано противоречие самого `Plan.md`: раздел 6 требует `ListView` для Лаб 1–2,
  а пункты `B2`/`C2` — `Spinner` и `RecyclerView`. Следованы пункты этапов; расхождение
  отмечено в обеих документациях.

## [1.1.0] — Лабораторная 2

### Этап C — галерея котиков
- Добавлена Лаба 2: сетка котов с CATAAS, фильтр по тегам, pull-to-refresh, состояние ошибки.
- Модель `Cat` (`@Serializable`) и отдельный DTO `RandomCatDto` для второго формата ответа.
- `CataasApi`: `getCats(limit)` и `getRandomCat()` на общем `OkHttpClient`.
- `CatRepository`: сеть → кэш в `filesDir/cats_cache.json` → ошибка. Возвращает `LoadResult`
  с указанием источника, экран честно показывает, откуда взялись данные.
- `CatViewModel`: `StateFlow<CatsUiState>` (`Loading` / `Success` / `Error`) плюс локальная
  фильтрация по тегам без обращения к сети.
- `CatAdapter`: `ListAdapter` + `DiffUtil` (`areItemsTheSame` по `id`) + Coil с
  `placeholder` и `error`.
- Разметки `activity_lab2.xml`, `item_cat.xml`, ресурс-ключ `R.id.tag_name`.
- Русские окончания перенесены в `plurals` вместо ручной арифметики в коде.
- 8 unit-тестов разбора JSON (`CatParsingTest`), всего в проекте 19 тестов.
- Документация: `docs/02-lab-cats-gallery.md` с разбором 8 граблей.
- Скриншоты и GIF: `docs/assets/lab2-*` (загрузка, галерея, фильтр, кэш, ошибка).

### Принятые технические решения
- Свой класс результата назван `LoadResult`, а не `Result`: имя `Result` перебивает
  `kotlin.Result` внутри своего же файла (та же грабля, что в Лабе 1).
- `RecyclerView` **не** обёрнут в `NestedScrollView`: вложенный скролл измеряет список
  на всю высоту, отключая переиспользование view.
- Чипы фильтра пересоздаются только при реальном изменении набора тегов, иначе каждый
  `render()` создавал десятки `View`.
- `data/cats_fallback.json` оставлен неиспользованным намеренно: кэш создаётся при первом
  успешном ответе, иначе состояние ошибки стало бы недостижимым. Способ подключения
  описан в `docs/02-lab-cats-gallery.md`.
- На эмуляторе с локалью `en-US` подпись «3 котов» вместо «3 кота»: `getQuantityString`
  выбирает вариант по правилам локали **устройства**, а у английского языка нет категории
  `few`. Решение — `values-en/` в Лабе 6; ручные `% 10` в коде не пишутся.

## [1.0.0] — Лабораторная 1

### Этап B — конвертер величин
- Добавлена Лаба 1: конвертер длины, массы и объёма на трёх `Spinner`.
- Модель `Unit` (`@Serializable`) с полями `id`, `name`, `short`, `kind`, `toMeters`.
- `UnitsApi` загружает `data/units.json` с jsDelivr, при неудаче берёт копию из `assets`.
- `UnitsViewModel` отдаёт `StateFlow<UnitsUiState>` (`Loading` / `Ready` / `Error`).
- Арифметика вынесена в `UnitConverter` и покрыта 11 unit-тестами (`./gradlew test`).
- Состояние экрана (группа, обе единицы, введённое число) переживает поворот экрана.
- Документация: `docs/01-lab-converter.md` с разбором 8 граблей.
- Скриншоты и GIF: `docs/assets/lab1-*.png`, `docs/assets/lab1-demo.gif`.
- Удалены шаблонные тесты `ExampleUnitTest` / `ExampleInstrumentedTest` из Android Studio.

### Принятые технические решения
- Единицы хранятся как `kind: String`, а не как `kind: UnitKind`: значение приходит из JSON,
  а `UnitKind` нужен только приложению — для заголовков и группировки.
- Поле называется `toMeters`, хотя теперь в файле три группы. Смысл не изменился
  (сколько **базовых единиц своей группы** в одной единице), но имя оставлено прежним,
  чтобы документация и план лабы не расходились. Это осознанная техдолг.
- Активность и результат сохраняются по `id` единицы, а не по позиции в списке: порядок
  в JSON может измениться, и сохранённый индекс указал бы на другую единицу.
- Один общий `OkHttpClient` в `HttpClientProvider` на всё приложение вместо клиента
  на каждый запрос.
- Внизу экрана всегда показано, откуда взялись данные: CDN или `assets`.

### Этап A — каркас репозитория
- Инициализирована структура репозитория: `README.md`, `CHANGELOG.md`, `LICENSE` (MIT), `docs/`.
- Добавлен `docs/00-setup.md` — установка Android Studio, импорт, эмулятор, решение проблем.
- Добавлены данные `data/units.json` (19 единиц в 3 группах) и `data/cats_fallback.json` (20 котов).
- Добавлены заглушки изображений `images/cat_placeholder_1..3.png`.
- Проект переведён на пакет `ru.devcustrom.androidlab` (было `com.example.myapplication`).
- Тема переименована `Theme.MyApplication` → `Theme.AndroidLabs`, `app_name` → «Android Labs».
- Подготовлен тулчейн: View Binding, Lifecycle, RecyclerView, SwipeRefreshLayout, Coroutines,
  OkHttp, kotlinx.serialization, Coil, Room + KSP. Добавлено разрешение `INTERNET`.

### Принятые технические решения
- `compileSdk`/`targetSdk` = **37**, `minSdk` = 24. Значения из первоначального плана (34) не
  применены: AGP 9.4.1 поддерживает максимум API 37, а по правилу «заработало — не чини»
  понижение SDK недопустимо.
- Kotlin **не подключается отдельным плагином** — AGP 9 имеет встроенную поддержку Kotlin (KGP 2.2.10).
  Из этого следует, что `kapt` несовместим, и Room собирается через **KSP**.
- Плагин `org.jetbrains.kotlin.plugin.serialization` зафиксирован на версии **2.2.10** —
  ровно как встроенный Kotlin. Расхождение версий ломает генерацию `@Serializable`.
- `data/units.json` отправлен в `origin/main` **до** написания приложения: jsDelivr читает
  ветку с сервера GitHub и отдаёт 404, пока файл не запушен.