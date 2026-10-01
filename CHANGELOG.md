# Changelog

Все значимые изменения этого репозитория документируются в этом файле.

Формат основан на [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/),
версионирование — по [Semantic Versioning](https://semver.org/lang/ru/).

## [Unreleased]

### Этап A — каркас репозитория
- Инициализирована структура репозитория: `README.md`, `CHANGELOG.md`, `LICENSE` (MIT), `docs/`.
- Добавлен `docs/00-setup.md` — установка Android Studio, импорт, эмулятор, решение проблем.
- Добавлены данные `data/units.json` (18 единиц в 3 группах) и `data/cats_fallback.json` (20 котов).
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