plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ru.devcustrom.androidlab"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "ru.devcustrom.androidlab"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests {
            /**
             * Методы `android.util.Log` в обычных unit-тестах — заглушки, и
             * без этого флага вызов бросает `RuntimeException: not mocked`.
             *
             * Лаба 4 добавила 11 тестов на файловый кэш, и все они падали на
             * `Log.i(TAG, "Кэш записан: …")`. Три варианта:
             *
             * 1. Убрать логирование из `CatCache` — теряем диагностику в проде.
             * 2. Замокать `Log` — нужен Mockito ради одного класса.
             * 3. `returnDefaultValues = true` — методы возвращают 0/false.
             *
             * Выбран третий: логи остаются, лишних зависимостей нет. Побочный
             * эффект тот же, что и у Robolectric: тест не заметит, если код
             * начнёт *читать* результат вызова `Log`. Читать его нельзя.
             */
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}