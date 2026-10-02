# Changelog

Все значимые изменения этого репозитория документируются в этом файле.

Формат основан на [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/),
версионирование — по [Semantic Versioning](https://semver.org/lang/ru/).

## [Unreleased]

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