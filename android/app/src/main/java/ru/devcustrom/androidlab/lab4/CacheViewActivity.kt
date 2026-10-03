package ru.devcustrom.androidlab.lab4

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.devcustrom.androidlab.R
import ru.devcustrom.androidlab.data.repository.CatRepository
import ru.devcustrom.androidlab.databinding.ActivityCacheViewBinding

/**
 * Показывает `cats_cache.json` как есть — моноширинным шрифтом.
 *
 * Зачем отдельный экран, если можно `Snackbar`: пользователь должен **видеть**
 * JSON, а не верю нам на слово. Кэш-строка, записанная в 22:00, и кэш-строка,
 * записанная в 22:05, отличаются одной записью — и это лучше один раз увидеть,
 * чем сто раз поверить.
 */
class CacheViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCacheViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCacheViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        lifecycleScope.launch {
            val cache = CatRepository(applicationContext).cache
            val text = withContext(Dispatchers.IO) { cache.readText() }
            val size = withContext(Dispatchers.IO) { cache.sizeBytes() }
            val count = withContext(Dispatchers.IO) { cache.read()?.size ?: 0 }

            binding.cacheContent.text = text ?: getString(R.string.lab4_cache_missing)
            binding.cacheMeta.text = getString(R.string.lab4_cache_meta, size, count)
            Log.i(TAG, "Показан кэш: $size байт, $count котов")
        }
    }

    companion object {
        private const val TAG = "CacheViewActivity"

        fun intent(context: Context): Intent = Intent(context, CacheViewActivity::class.java)
    }
}