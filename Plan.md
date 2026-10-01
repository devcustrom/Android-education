# План разработки репозитория `Android-education`

> Этот документ — задание для AI-агента, работающего внутри репозитория. Все шаги описаны в порядке выполнения. После каждой лабы — коммит, тег, обновление документации. Агент должен строго следовать правилам документирования из раздела «Правила».

---

## 0. Контекст проекта

**Репозиторий:** https://github.com/devcustrom/Android-education

**Цель:** учебный Android-проект на Kotlin, покрывающий 6 лабораторных работ. Репозиторий должен быть **наглядным пособием**: по нему читатель может пройти весь путь от нуля до рабочего приложения, понять каждое решение и избежать граблей.

**Стек:**
- Kotlin, View Binding (не Data Binding)
- Coroutines + Flow
- OkHttp + kotlinx.serialization
- Coil (загрузка картинок)
- ViewModel + StateFlow
- Room (Лаба 5)
- minSdk 24, targetSdk 34, compileSdk 34
- AGP: версия, которая уже работает в проекте — **не трогать**

**Внешние API:**
- CATAAS: `https://cataas.com/api/cats?limit=20` (список), `https://cataas.com/cat?json=true` (случайный кот), `https://cataas.com/cat/{id}` (картинка)
- Данные в репозитории раздаются через jsDelivr: `https://cdn.jsdelivr.net/gh/devcustrom/Android-education@main/...`

**Важно:** версии библиотек и AGP **не обновлять**. Если что-то не собирается — понижать версию библиотеки, а не поднимать AGP. Правило: «заработало — не чини».

---

## 1. Структура репозитория (целевая)

```
Android-education/
├── README.md
├── CHANGELOG.md
├── docs/
│   ├── 00-setup.md
│   ├── 01-lab-converter.md
│   ├── 02-lab-cats-gallery.md
│   ├── 03-lab-menu-activity.md
│   ├── 04-lab-files-state.md
│   ├── 05-lab-room.md
│   ├── 06-lab-canvas-widget.md
│   └── assets/
│       ├── lab1-demo.gif
│       ├── lab2-demo.gif
│       └── ...
├── android/
│   └── app/src/main/java/com/example/androidlabs/
│       ├── MainActivity.kt
│       ├── common/LabItem.kt
│       ├── data/
│       │   ├── model/{Cat,Unit,Note}.kt
│       │   ├── network/{CataasApi,UnitsApi}.kt
│       │   ├── repository/{CatRepository,NotesRepository}.kt
│       │   └── db/{NoteDao,AppDatabase}.kt
│       ├── ui/{CatViewModel,UnitsViewModel,NotesViewModel}.kt
│       ├── lab1/{Lab1Activity,UnitAdapter}.kt
│       ├── lab2/{Lab2Activity,CatAdapter}.kt
│       ├── lab3/AboutActivity.kt
│       ├── lab4/{Lab4Activity,CacheViewActivity}.kt
│       ├── lab5/{Lab5Activity,NoteAdapter,NoteEditActivity}.kt
│       └── lab6/{Lab6Activity,DrawingView,widget/CatWidgetProvider}.kt
├── data/
│   ├── cats_fallback.json
│   └── units.json
└── images/
    └── cat_placeholder_1.png
```

---

## 2. Правила документирования

Каждый `docs/NN-lab-*.md` пишется по **единому шаблону**:

```markdown
# Лаба N: Название

## 🎯 Цель работы
Дословная цитата из методички.

## 📚 Что изучим
Список API и концепций.

## 🖼️ Что получится
![Демо](assets/labN-demo.gif)

## 🧠 Теория (кратко)
3–5 абзацев — только то, что нужно для понимания лабы.

## 🛠️ Пошаговая реализация

### Шаг 1: ...
**Зачем:** ...
**Как:** (код)
**Результат:** (скриншот)

(и так далее)

## 🐛 Что пошло не так (грабли)
- Ошибка X → причина → решение
- Ошибка Y → причина → решение

## 🤔 Разбор решений
Почему выбран именно этот подход, какие были альтернативы.

## ✅ Чек-лист сдачи
- [ ] Работает то
- [ ] Работает сё
- [ ] Обработаны ошибки

## 🔗 Полезные ссылки
Документация, туториалы, SO-ответы.

## 📌 Коммиты лабы
- `abc1234` — описание
```

**Правила:**
1. Документация пишется **по ходу работы**, не в конце.
2. Каждый шаг — со скриншотом или GIF (в `docs/assets/`).
3. Ошибки **не удалять** — записывать в раздел «Грабли».
4. Архитектурная диаграмма в Mermaid — в каждой лабе.
5. Коммиты — по Conventional Commits: `feat(labN):`, `fix(labN):`, `docs(labN):`.
6. После завершения лабы — тег `labN-v1.0`.

---

## 3. Порядок работ

### Этап A. Каркас репозитория

**A1.** Создать `README.md` со структурой:
- Название, описание
- Скриншоты (плейсхолдеры пока)
- Таблица лабораторных (номер, тема, ссылка на docs, тег)
- Быстрый старт (`git clone`, `./gradlew assembleDebug`)
- Стек
- Структура репозитория
- Как читать репозиторий
- Лицензия MIT

**A2.** Создать `CHANGELOG.md` с секцией `[Unreleased]`.

**A3.** Создать `docs/00-setup.md`:
- Как установить Android Studio
- Как импортировать проект
- Как настроить эмулятор
- Возможные проблемы (AGP/Gradle несовместимость — что делать)
- Как запустить проект

**A4.** Создать папки `data/` и `images/`, положить туда `units.json` и заглушки для котов.

**A5.** Коммит: `chore: инициализация структуры репозитория`.

---

### Этап B. Лаба 1 — Конвертер величин

**B1.** Создать `docs/01-lab-converter.md` по шаблону.

**B2.** В Android-проекте:
- Добавить зависимость OkHttp + kotlinx.serialization + ViewModel (если ещё нет).
- `data/model/Unit.kt` — `@Serializable data class Unit(id, name, short, toMeters)`.
- `data/network/UnitsApi.kt` — загрузка `units.json` с jsDelivr.
- `ui/UnitsViewModel.kt` — StateFlow с `List<Unit>`.
- `lab1/UnitAdapter.kt` — `ArrayAdapter<Unit>` для Spinner.
- `lab1/Lab1Activity.kt`:
  - Два Spinner (откуда, куда).
  - EditText для ввода + TextWatcher.
  - TextView результата.
  - Логика: `value * from.toMeters / to.toMeters`.
- Разметка `activity_lab1.xml`, `item_unit.xml`.

**B3.** Запустить, убедиться что работает: ввод 100 см → 1 м.

**B4.** Скриншот + GIF → `docs/assets/lab1-*`.

**B5.** Дописать документацию: шаги, грабли, разбор решений (почему Spinner, почему загрузка из сети, а не хардкод).

**B6.** Коммиты по шагам, тег `lab1-v1.0`.

---

### Этап C. Лаба 2 — Галерея котиков

**C1.** `docs/02-lab-cats-gallery.md` по шаблону.

**C2.** В проекте:
- `data/model/Cat.kt` — `@Serializable` с полями `id, tags, createdAt, mimetype`, свойство `imageUrl`.
- `data/network/CataasApi.kt` — OkHttp + kotlinx.serialization, методы `getCats(limit)` и `getRandomCat()`.
- `data/repository/CatRepository.kt` — сначала сеть, при ошибке — кэш из `filesDir/cats_cache.json`.
- `ui/CatViewModel.kt` — `UiState`: Loading / Success / Error, `viewModelScope`.
- `lab2/CatAdapter.kt` — `ListAdapter<Cat, VH>` с `DiffUtil`, Coil для картинок.
- `lab2/Lab2Activity.kt` — RecyclerView + ProgressBar + Error state + Retry.
- Разметки: `activity_lab2.xml`, `item_cat.xml`.

**C3.** Проверка: приложение грузит 20 котов, показывает, кэширует.

**C4.** Скриншоты → `docs/assets/lab2-*`.

**C5.** Документация: шаги, грабли (типа «сеть в главном потоке — краш»), разбор (почему ListAdapter, почему Coil, почему StateFlow).

**C6.** Тег `lab2-v1.0`.

---

### Этап D. Лаба 3 — Меню и несколько Activity

**D1.** `docs/03-lab-menu-activity.md`.

**D2.** В проекте:
- `MainActivity.kt` — RecyclerView со списком 6 лаб, клик → `startActivity`.
- `common/LabItem.kt` — модель (number, title, activityClass).
- **Options Menu** (три точки): «О программе», «Настройки», «Выход».
- `lab3/AboutActivity.kt` — экран «О программе».
- **Context Menu** на долгое нажатие по элементу списка: «Открыть», «Поделиться».
- **Передача данных**: `Intent.putExtra("student_name", ...)`.
- **Возврат результата**: `registerForActivityResult(StartActivityForResult())` — `Lab1Activity` возвращает последнее использованное значение.

**D3.** Документация: разбор `Intent`, `putExtra`, `getExtra`, `ActivityResultContracts`.

**D4.** Тег `lab3-v1.0`.

---

### Этап E. Лаба 4 — Файлы и состояния

**E1.** `docs/04-lab-files-state.md`.

**E2.** В проекте:
- `lab4/Lab4Activity.kt`:
  - Кнопки: «Показать кэш», «Очистить кэш», «Экспорт в Downloads».
  - `onSaveInstanceState` — сохраняет позицию скролла и выбранные единицы.
  - `onRestoreInstanceState` — восстанавливает.
  - Context Menu на элементе списка котов: «Поделиться», «Удалить из кэша», «Скопировать ID».
- `lab4/CacheViewActivity.kt` — показывает содержимое `cats_cache.json`.
- Экспорт файла в `Downloads` через `MediaStore` (API 29+).

**E3.** Проверка: повернуть экран — состояние сохраняется; кэш виден; экспорт работает.

**E4.** Документация: разбор `onSaveInstanceState`, `Bundle`, `filesDir`, `MediaStore`.

**E5.** Тег `lab4-v1.0`.

---

### Этап F. Лаба 5 — Room

**F1.** `docs/05-lab-room.md`.

**F2.** В проекте:
- Зависимости Room + KSP.
- `data/db/Note.kt` — `@Entity`.
- `data/db/NoteDao.kt` — `@Query`, `@Insert`, `@Update`, `@Delete`, `Flow<List<Note>>`.
- `data/db/AppDatabase.kt` — `RoomDatabase`.
- `data/repository/NotesRepository.kt` — обёртка над DAO.
- `ui/NotesViewModel.kt` — `Flow<List<Note>>` через `stateIn`.
- `lab5/Lab5Activity.kt` — список заметок + FAB «Добавить».
- `lab5/NoteEditActivity.kt` — редактирование/создание.
- `lab5/NoteAdapter.kt` — `ListAdapter`.

**F3.** Документация: разбор Entity/Dao/Database, почему Room лучше голого SQLite, как работает Flow.

**F4.** Тег `lab5-v1.0`.

---

### Этап G. Лаба 6 — Графика, локализация, виджет

Разбить на три подэтапа.

**G1.** `docs/06-lab-canvas-widget.md` — сразу три подраздела.

**G2. Графика:**
- `lab6/DrawingView.kt` — `onDraw` + `onTouchEvent` (рисование пальцем).
- `lab6/Lab6Activity.kt` — выбор цвета, толщины, кнопка «Очистить».
- `Path` для хранения линии.

**G3. Локализация:**
- Все строки — в `strings.xml`.
- `values-en/strings.xml` — английский.
- `values-de/strings.xml` — немецкий.
- Переключатель языка в настройках через `AppCompatDelegate.setApplicationLocales`.

**G4. Виджет:**
- `lab6/widget/CatWidgetProvider.kt` — `AppWidgetProvider`.
- `res/layout/widget_cat.xml`.
- `res/xml/widget_info.xml`.
- Регистрация в `AndroidManifest.xml`.
- `RemoteViews` для обновления.
- `updatePeriodMillis = 1800000` (30 мин).
- Кнопка «Обновить кота» в виджете через `PendingIntent`.

**G5.** Документация: разбор `Canvas`, `Locale`, `AppWidgetProvider`, `RemoteViews`, `PendingIntent`.

**G6.** Тег `lab6-v1.0`.

---

## 4. Критерии готовности

Репозиторий считается готовым, когда:

1. **Все 6 лабораторных** работают в одном приложении, доступны из главного меню.
2. **Для каждой лабы** есть `docs/NN-lab-*.md` по шаблону.
3. **Каждая лаба** имеет тег `labN-v1.0` и запись в `CHANGELOG.md`.
4. **README.md** содержит таблицу со ссылками на все лабы и скриншоты.
5. **Приложение собирается** без ошибок из чистого клона репозитория.
6. **Нет зашитых данных** — всё, что можно, берётся из `data/*.json` или API.
7. **Раздел «Грабли»** в каждой лабе содержит минимум 2-3 реальных ошибки с решениями.
8. **Mermaid-диаграмма архитектуры** есть в каждой лабе.

---

## 5. Правила для агента

1. **Никогда не обновлять AGP, Gradle, Android Studio.** Если что-то не собирается — понижать версию библиотеки.
2. **Не удалять ошибки** из документации — они ценность репозитория.
3. **Не хардкодить данные.** Всё — через JSON в репозитории или внешние API.
4. **Один коммит — одно осмысленное изменение.** Сообщения по Conventional Commits.
5. **Перед каждым коммитом** — проверять, что проект собирается: `./gradlew assembleDebug`.
6. **После каждой лабы** — `git tag -a labN-v1.0 -m "Лаба N завершена"`.
7. **Язык документации — русский.** Код и имена переменных — английский.
8. **Скриншоты и GIF** кладутся в `docs/assets/` с именами `labN-{demo,error,step1,...}.{gif,png}`.
9. **Если пользователь не дал скриншот** — оставить плейсхолдер `![TODO](assets/labN-demo.gif)` и напомнить ему.
10. **Не сокращать документацию** ради скорости. Полнота важнее.

---

## 6. Что НЕ делать

- ❌ Не использовать Data Binding (только View Binding).
- ❌ Не использовать Java (только Kotlin).
- ❌ Не использовать RecyclerView там, где методичка требует ListView — **ListView для Лаб 1–2**, RecyclerView для остальных.
- ❌ Не добавлять лишние библиотеки (Retrofit, Hilt, Koin) — только то, что в стеке.
- ❌ Не переписывать уже работающие лабы при добавлении новых.
- ❌ Не пушить в `main` без предварительной проверки сборки.
- ❌ Не удалять коммиты и не делать force push без явного указания.

---

## 7. Команды для проверки

Перед каждым коммитом:

```bash
cd android
./gradlew assembleDebug
```

После успешной сборки:

```bash
git add .
git commit -m "feat(labN): описание"
git push origin main
```

Тег:

```bash
git tag -a labN-v1.0 -m "Лаба N завершена"
git push origin labN-v1.0
```

---

## 8. Итоговый чек-лист

- [ ] README.md
- [ ] CHANGELOG.md
- [ ] docs/00-setup.md
- [ ] Лаба 1 + docs + tag
- [ ] Лаба 2 + docs + tag
- [ ] Лаба 3 + docs + tag
- [ ] Лаба 4 + docs + tag
- [ ] Лаба 5 + docs + tag
- [ ] Лаба 6 + docs + tag
- [ ] Все скриншоты в docs/assets/
- [ ] Все Mermaid-диаграммы на месте
- [ ] Проект собирается из чистого клона
- [ ] Лицензия MIT

---

**Начинай с Этапа A (каркас репозитория). После каждого этапа останавливайся и показывай результат пользователю для подтверждения.**