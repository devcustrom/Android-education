package ru.devcustrom.androidlab.lab2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.devcustrom.androidlab.R
import ru.devcustrom.androidlab.data.model.Cat
import ru.devcustrom.androidlab.data.repository.CatRepository
import ru.devcustrom.androidlab.databinding.ActivityLab2Binding
import ru.devcustrom.androidlab.ui.CatViewModel
import ru.devcustrom.androidlab.ui.CatsUiState

/**
 * Лабораторная 2: галерея котиков.
 *
 * Экран умеет три состояния — загрузка, данные, ошибка — и честно переключается
 * между ними. Состояние приходит из [CatViewModel] через `StateFlow`, Activity
 * ничего не загружает и ничего не кэширует: это работа репозитория.
 */
class Lab2Activity : AppCompatActivity() {

    private lateinit var binding: ActivityLab2Binding

    private val viewModel: CatViewModel by viewModels {
        CatViewModel.Factory(CatRepository(applicationContext))
    }

    private val adapter = CatAdapter()

    /** Теги строятся один раз: пересоздавать `Chip` на каждый чих дорого. */
    private var builtTags: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLab2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.catsList.layoutManager = GridLayoutManager(this, SPAN_COUNT)
        binding.catsList.adapter = adapter
        // Размеры карточек не меняются при прокрутке — проще и быстрее,
        // чем подгонять item под каждую позицию.
        binding.catsList.setHasFixedSize(true)

        adapter.onCatClick = ::openCat

        binding.swipeRefresh.setOnRefreshListener { viewModel.load(force = true) }
        binding.retryButton.setOnClickListener { viewModel.load(force = true) }

        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: CatsUiState) = with(binding) {
        when (state) {
            is CatsUiState.Loading -> {
                loadingBox.visibility = View.VISIBLE
                errorBox.visibility = View.GONE
                // При обновлении список оставляем на экране — так делают все
                // нормальные приложения: не мигать пустотой перед RefreshIndicator.
                if (!swipeRefresh.isRefreshing) catsList.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }

            is CatsUiState.Success -> {
                loadingBox.visibility = View.GONE
                errorBox.visibility = View.GONE
                catsList.visibility = View.VISIBLE
                swipeRefresh.isRefreshing = false

                adapter.submitList(state.cats)

                sourceText.text = buildString {
                    append(catsCountText(state.cats.size))
                    append(" · ")
                    append(
                        getString(
                            if (state.origin == CatRepository.Origin.NETWORK) {
                                R.string.lab2_source_network
                            } else {
                                R.string.lab2_source_cache
                            }
                        )
                    )
                }

                buildTagChips()
            }

            is CatsUiState.Error -> {
                loadingBox.visibility = View.GONE
                catsList.visibility = View.GONE
                errorBox.visibility = View.VISIBLE
                errorText.text = state.message
                swipeRefresh.isRefreshing = false
            }
        }
    }

    /**
     * Чипы фильтра. Строятся один раз за экран: иначе на каждый чих разметки
     * создаются десятки `View`, и прокрутка начинает подтормаживать.
     */
    private fun buildTagChips() {
        val tags = viewModel.allTags()
        if (tags.isEmpty() || tags == builtTags) return

        builtTags = tags
        binding.tagsGroup.removeAllViews()
        tags.forEach { name ->
            val chip = Chip(this).apply {
                text = getString(R.string.lab2_tag_chip, name, viewModel.countByTag(name))
                isCheckable = true
                setTag(R.id.tag_name, name)
            }
            binding.tagsGroup.addView(chip)
        }
        binding.tagsGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val checked = checkedIds.firstOrNull()
            val name = checked?.let { group.findViewById<Chip>(it)?.getTag(R.id.tag_name) as? String }
            viewModel.selectTag(name)
        }
        binding.tagsScroll.visibility = View.VISIBLE
    }

    private fun openCat(cat: Cat) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cat.imageUrl)))
        }.onFailure {
            Snackbar.make(binding.root, R.string.lab2_no_browser, Snackbar.LENGTH_SHORT).show()
        }
    }

    /** Русские окончания берутся из `plurals`, а не пишутся руками в коде. */
    private fun catsCountText(count: Int): String =
        resources.getQuantityString(R.plurals.lab2_cats_count, count, count)

    private companion object {
        const val SPAN_COUNT = 2
    }
}