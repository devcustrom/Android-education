package ru.devcustrom.androidlab.lab6

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.devcustrom.androidlab.R

/**
 * Лаба 6, часть «графика»: рисование пальцем на `Canvas`.
 *
 * Экран показывает связку «пользовательский `View` + его же состояние»:
 * - [DrawingView] рисует штрихи и отдаёт снимок для экспорта;
 * - палитра и толщины живут в [BrushPalette], а не в коде экрана;
 * - цвет текста превью подбирает [ColorContrast] по контрасту (WCAG 2.1).
 */
class Lab6Activity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var colorPreview: View
    private lateinit var btnColor: Button
    private lateinit var btnStrokeWidth: Button
    private lateinit var btnClear: Button
    private lateinit var tvStrokes: TextView
    private lateinit var tvLocaleInfo: TextView
    private lateinit var toolbar: MaterialToolbar

    private var currentColor = BrushPalette.DEFAULT_COLOR
    private var currentWidth = BrushPalette.DEFAULT_WIDTH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lab6)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        drawingView = findViewById(R.id.drawingView)
        colorPreview = findViewById(R.id.colorPreview)
        btnColor = findViewById(R.id.btnColor)
        btnStrokeWidth = findViewById(R.id.btnStrokeWidth)
        btnClear = findViewById(R.id.btnClear)
        tvStrokes = findViewById(R.id.tvStrokes)
        tvLocaleInfo = findViewById(R.id.tvLocaleInfo)

        drawingView.setStrokeColor(currentColor)
        drawingView.setStrokeWidth(currentWidth)
        colorPreview.setBackgroundColor(currentColor)
        drawingView.onStrokesChanged = ::renderStrokeCount
        renderStrokeCount(drawingView.getStrokesCount())

        btnColor.setOnClickListener { showColorPicker() }
        btnStrokeWidth.setOnClickListener { showWidthPicker() }
        btnClear.setOnClickListener { drawingView.clear() }

        renderLocaleInfo()
    }

    override fun onResume() {
        super.onResume()
        // Язык мог измениться на экране настроек, пока этот экран стоял в стеке.
        renderLocaleInfo()
    }

    // --------------------------------------------------------------- options-меню

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_lab6, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_export_drawing -> {
            exportDrawing()
            true
        }

        android.R.id.home -> {
            finish()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    // ------------------------------------------------------------------- графика

    private fun renderStrokeCount(count: Int) {
        tvStrokes.text = resources.getQuantityString(R.plurals.lab6_strokes, count, count)
    }

    private fun renderLocaleInfo() {
        val locales = AppCompatDelegate.getApplicationLocales()
        val tag = if (locales.isEmpty) {
            java.util.Locale.getDefault().toLanguageTag()
        } else {
            locales.toLanguageTags()
        }
        tvLocaleInfo.text = getString(R.string.lab6_locale_info, tag)
    }

    private fun showColorPicker() {
        val names = BrushPalette.COLORS.map { hex(it) }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.lab6_select_color)
            .setItems(names) { _, which ->
                currentColor = BrushPalette.COLORS[which]
                drawingView.setStrokeColor(currentColor)
                colorPreview.setBackgroundColor(currentColor)
                btnColor.setTextColor(ColorContrast.forBackground(currentColor))
            }
            .show()
    }

    private fun showWidthPicker() {
        val labels = BrushPalette.WIDTHS.map { "${it.toInt()} px" }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.lab6_select_width)
            .setItems(labels) { _, which ->
                currentWidth = BrushPalette.WIDTHS[which]
                drawingView.setStrokeWidth(currentWidth)
            }
            .show()
    }

    private fun exportDrawing() {
        val bitmap = drawingView.render(
            ContextCompat.getColor(this, R.color.drawing_background),
        )

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                Lab6Exporter(this@Lab6Activity).export(bitmap)
            }
            val message = when (result) {
                is Lab6Exporter.Result.Success -> getString(R.string.lab6_export_ok, result.location)
                is Lab6Exporter.Result.Failure -> getString(R.string.lab6_export_failed, result.reason)
            }
            Snackbar.make(drawingView, message, Snackbar.LENGTH_LONG).show()
        }
    }

    /** `#RRGGBB` — компактная подпись для палитры, понятнее названий вроде «морская волна». */
    private fun hex(color: Int): String = String.format("#%06X", 0xFFFFFF and color)
}
