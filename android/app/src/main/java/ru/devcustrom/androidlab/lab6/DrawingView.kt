package ru.devcustrom.androidlab.lab6

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Простейший холст для рисования пальцем.
 * Храним список путей с цветом и толщиной линии.
 */
class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Stroke(
        val path: Path,
        val paint: Paint
    )

    private val strokes = mutableListOf<Stroke>()
    private val currentPath = Path()
    private val currentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private var currentColor = Color.BLACK
    private var currentStrokeWidth = 8f

    /** Сообщает наружу, что число штрихов изменилось (для счётчика на экране). */
    var onStrokesChanged: ((Int) -> Unit)? = null

    init {
        currentPaint.color = currentColor
        currentPaint.strokeWidth = currentStrokeWidth
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (stroke in strokes) {
            canvas.drawPath(stroke.path, stroke.paint)
        }
        canvas.drawPath(currentPath, currentPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath.reset()
                currentPath.moveTo(x, y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                currentPath.lineTo(x, y)
            }

            MotionEvent.ACTION_UP -> {
                // Сохраняем текущий штрих
                val paintCopy = Paint(currentPaint)
                strokes.add(Stroke(Path(currentPath), paintCopy))
                currentPath.reset()
                onStrokesChanged?.invoke(strokes.size)
            }

            else -> return false
        }
        invalidate()
        return true
    }

    fun setStrokeColor(color: Int) {
        currentColor = color
        currentPaint.color = currentColor
    }

    fun setStrokeWidth(width: Float) {
        currentStrokeWidth = width
        currentPaint.strokeWidth = currentStrokeWidth
    }

    fun clear() {
        strokes.clear()
        currentPath.reset()
        onStrokesChanged?.invoke(0)
        invalidate()
    }

    fun getStrokesCount(): Int = strokes.size

    /**
     * Снимок холста для экспорта в «Загрузки».
     *
     * Рисуем на отдельном [Bitmap] теми же штрихами, а не через `view.draw()`:
     * `draw()` захватывает только то, что сейчас на экране (с учётом скролла и
     * системных рамок), а нам нужен весь холст целиком и без фона разметки.
     * Фон заливаем [backgroundColor], иначе PNG получится прозрачным и рисунок
     * будет не виден в галерее на тёмном фоне.
     */
    fun render(backgroundColor: Int = Color.WHITE): Bitmap {
        val bitmap = Bitmap.createBitmap(
            width.coerceAtLeast(1),
            height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundColor)
        for (stroke in strokes) {
            canvas.drawPath(stroke.path, stroke.paint)
        }
        return bitmap
    }
}