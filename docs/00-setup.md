# 00 — Подготовка окружения

Прежде чем писать код, нужно убедиться, что инструменты настроены правильно. Этот документ —
чеклист, который экономит несколько часов, если что-то пойдёт не так.

---

## 📋 Что нужно установить

| Что | Версия | Зачем |
|-----|--------|-------|
| **JDK** | 17 (или новее) | Gradle и AGP требуют Java 17+ |
| **Android Studio** | любая, умеющая открывать AGP 9 | IDE, эмулятор, SDK Manager |
| **Android SDK** | Platform **37** + Build-Tools **36.0.0** | Сборка |

> В проекте используется **AGP 9.4.1** и **Gradle 9.6.0**. Обе версии зафиксированы в репозитории
> (`gradle/libs.versions.toml` и `gradle/wrapper/gradle-wrapper.properties`) и **не обновляются**.
> Это осознанное решение — см. раздел «🐛 Что пошло не так» в [Лабе 1](01-lab-converter.md).

---

## 1️⃣ Установка Android Studio

1. Скачать **Android Studio** с [developer.android.com/studio](https://developer.android.com/studio).
2. Установить. В мастере установки оставить галочку **Android SDK** — компоненты скачаются сами.
3. Запустить первый раз и дождаться, пока мастер сам поставит SDK Platform и Build-Tools.

### Отдельно про JDK

В системе может быть несколько Java. `gradlew` возьмёт ту, что указана в `JAVA_HOME`.
Проверить:

```bash
java -version     # должно быть 17 или новее
echo %JAVA_HOME%  # Windows
```

Если `JAVA_HOME` указывает на Java 8 или 11 — сборка упадёт с
`Unsupported class file major version` или `Android Gradle plugin requires Java 17`.

В проекте есть файл `android/gradle/gradle-daemon-jvm.properties` с параметром
`toolchainVersion`. Если он указывает на версию, которой у вас нет, Gradle попробует
скачать её сама через [foojay](https://github.com/gradle/foojay-toolchains) — нужно
стабильное интернет-соединение. Если автоскачивание не работает, откройте этот файл и
поставьте `toolchainVersion` равным версии JDK, которая у вас установлена.

---

## 2️⃣ Импорт проекта

```bash
git clone https://github.com/devcustrom/Android-education.git
cd Android-education
```

Дальше **File → Open** и указать папку **`Android-education/android`** — именно `android/`,
а не корень репозитория.

> **Почему не корень?** Gradle ищет `settings.gradle.kts` и поднимает дерево проектов вверх.
> Если открыть корень, IDE не найдёт Gradle-проект и предложит создать новый — вы потеряете
> привязку к версиям из `libs.versions.toml`.

### Настройки Android Studio для проекта

| Параметр | Где | Значение |
|----------|-----|----------|
| Gradle JDK | `Settings → Build, Execution, Deployment → Build Tools → Gradle` | 17+ |
| Offline | `Settings → Build Tools → Gradle` | **выключить** |
| SDK Location | `local.properties`, поле `sdk.dir` | путь к вашему SDK |

`local.properties` в репозиторий **не коммитится** (он в `.gitignore`) — у каждого свой путь.
Если файл отсутствует, Android Studio создаст его сам при первом открытии проекта.

---

## 3️⃣ Сборка из командной строки

```bash
cd android
./gradlew assembleDebug          # Windows: .\gradlew.bat assembleDebug
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`

Полезные команды:

```bash
./gradlew tasks                  # список всех задач
./gradlew :app:dependencies      # дерево зависимостей модуля app
./gradlew clean                  # очистить build/
./gradlew :app:installDebug      # собрать и поставить на подключённый девайс/эмулятор
./gradlew --stop                 # остановить демон Gradle
```

---

## 4️⃣ Эмулятор

### Создание AVD

1. **Tools → Device Manager** (или **More Actions → Virtual Device Manager**).
2. **Create Virtual Device** → категория **Phone** → любой размер (например *Medium Phone*).
3. **System Image** — выберите **API 35** или новее, образ **Google APIs** или **Google Play**.
   Именно Google-образы: на чистом AOSP нет Google-сервисов, но для этих лаб достаточно
   и AOSP-образа.
4. Имя AVD, например `Medium_Phone_API_35`.

> Образы Android 15–16 весят 1.5–3 ГБ. Скачиваются один раз.

### Запуск из командной строки

```bash
# список AVD
emulator -list-avds

# запуск
emulator -avd Medium_Phone_API_35 -no-snapshot-load -no-boot-anim

# полезные флаги на слабой машине
emulator -avd Medium_Phone_API_35 -gpu swiftshader_indirect -no-snapshot -no-window
```

`-no-window` полезен, если вы работаете в основном через Gradle и `adb` — окно не нужно.
`-gpu swiftshader_indirect` — программный рендеринг, помогает на машинах без
поддержки GPU.

### Проверка, что эмулятор загрузился

```bash
adb devices
# должно быть: emulator-5554  device

adb shell getprop sys.boot_completed
# должно быть: 1
```

Если `sys.boot_completed` возвращает пустоту — система ещё загружается, подождите.

---

## 5️⃣ Первая сборка — что должно произойти

При первом запуске `./gradlew assembleDebug` происходит скачивание:

- дистрибутива Gradle 9.6.0 (≈ 150 МБ) — в `~/.gradle/wrapper/dists`;
- всех зависимостей из `google()` и `mavenCentral()`.

Это занимает 2–5 минут. Последующие сборки — секунды.

Убедитесь, что в выводе есть задача **`kspDebugKotlin`**: она генерирует код Room
(появится начиная с Лабы 5). Если её нет, а `@Entity` в коде есть — Room не подключён.

---

## 🐛 Что пошло не так (общие грабли)

### `SDK location not found`
```
SDK location not found. Define a valid SDK location with an ANDROID_HOME environment
variable or by setting the sdk.dir path in your project's local properties file
```
**Причина:** нет `android/local.properties`.
**Решение:** создайте файл в папке `android/`:
```properties
sdk.dir=C\:\\Users\\<ваше-имя>\\AppData\\Local\\Android\\Sdk
```
(Windows требует экранировать двоеточия и обратные слэши.)

---

### `Failed to find Build Tools revision 36.0.0`
**Причина:** в SDK не установлены нужные Build-Tools.
**Решение:** **Tools → SDK Manager → SDK Tools → SDK Build-Tools** → поставить 36.0.0.
Либо через командную строку:
```bash
sdkmanager "build-tools;36.0.0" "platforms;android-37"
```

---

### `Android Gradle plugin requires Java 17`
**Причина:** Gradle запустился на Java 11 или 8.
**Решение:** `Settings → Build Tools → Gradle → Gradle JDK` = 17, либо задайте `JAVA_HOME`.

---

### `The 'org.jetbrains.kotlin.android' plugin is no longer required ...`
```
Failed to apply plugin 'org.jetbrains.kotlin.android'.
> The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support since AGP 9.0.
```
**Причина:** в `plugins {}` остался `alias(libs.plugins.kotlin.android)`.
**Решение:** **удалите** его. В AGP 9 Kotlin встроен, отдельный плагин не нужен и даже мешает.
Это ровно тот случай, когда «дописал плагин, чтобы было как в статье» ломает проект.

---

### `kapt is incompatible with built-in Kotlin` / `cannot run kapt`
**Причина:** в модуле остался плагин `org.jetbrains.kotlin.kapt`.
**Решение:** используйте **KSP** вместо kapt:
```kotlin
// build.gradle.kts (корневой)
plugins {
    id("com.google.devtools.ksp") version "2.3.12" apply false
}
// app/build.gradle.kts
plugins { id("com.google.devtools.ksp") }
dependencies { ksp("androidx.room:room-compiler:2.7.2") }
```
Запасной вариант — плагин `com.android.legacy-kapt` на ту же версию, что и AGP.

---

### `Serializer has not been found for type 'X'`
**Причина:** не подключён плагин сериализации либо его версия не совпадает со встроенным Kotlin.
**Решение:** в `libs.versions.toml` плагин `org.jetbrains.kotlin.plugin.serialization` должен
стоять на версии **2.2.10** — ровно как встроенный Kotlin в AGP 9.4.1.
```toml
kotlinSerialization = "2.2.10"
```

---

### Синхронизация Gradle падает каждый раз заново
**Причина:** в `gradle.properties` включён `org.gradle.configuration-cache=true`, а окружение
его не поддерживает.
**Решение:** временно закомментируйте строку и перезапустите.

---

### Эмулятор не стартует: `PANIC: Avd's CPU Architecture 'arm64' is not supported`
**Причина:** на macOS с чипом Apple Silicon или на Windows без аппаратной виртуализации
эмулятор x86_64 не запускается.
**Решение:** проверьте, что в BIOS включена виртуализация (Intel VT-x / AMD-V),
либо используйте физическое устройство по USB с включённым отладчиком.

---

## 🔗 Полезные ссылки

- [Android Studio — загрузка](https://developer.android.com/studio)
- [Настройка SDK](https://developer.android.com/studio#install-tools)
- [Создание AVD](https://developer.android.com/studio/run/managing-avds)
- [Миграция на встроенный Kotlin (AGP 9)](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Переход с kapt на KSP](https://developer.android.com/build/migrate-to-ksp)
- [Версии AGP / Gradle / JDK](https://developer.android.com/build/releases/gradle-plugin)
- [Версии зависимостей AndroidX](https://developer.android.com/jetpack/androidx/releases)

---

**Дальше:** [Лаба 1 — Конвертер величин](01-lab-converter.md) →