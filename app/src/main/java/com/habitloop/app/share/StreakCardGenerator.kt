package com.habitloop.app.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.content.res.ResourcesCompat
import com.habitloop.app.data.Habit
import com.habitloop.app.data.HabitTemplates
import java.io.File
import java.io.FileOutputStream

/**
 * Zero-cost viral growth loop: renders a shareable "Day N streak" card
 * on-device (plain Canvas, no image library, no server) and hands it to the
 * system share sheet for Instagram/WhatsApp Stories. No ad spend needed to
 * acquire users this way — every share is a free impression for the app.
 */
object StreakCardGenerator {

    fun shareStreak(context: Context, habit: Habit) {
        val bitmap = renderCard(context, habit)
        val file = saveToCache(context, bitmap)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share your streak"))
    }

    private fun renderCard(context: Context, habit: Habit): Bitmap {
        val template = HabitTemplates.byId(habit.templateId)
        val width = 1080
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = Color.rgb(
            (template.accentColor.red * 255).toInt(),
            (template.accentColor.green * 255).toInt(),
            (template.accentColor.blue * 255).toInt()
        )
        canvas.drawColor(bgColor)

        val iconDrawable = ResourcesCompat.getDrawable(context.resources, template.iconRes, null)
        if (iconDrawable != null) {
            val iconSize = 260
            val iconBitmap = iconDrawable.toBitmap(width = iconSize, height = iconSize)
            val left = (width - iconSize) / 2f
            canvas.drawBitmap(iconBitmap, left, 220f, null)
        }

        val streakPaint = Paint().apply {
            color = Color.WHITE
            textSize = 160f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        canvas.drawText("Day ${habit.currentStreak}", width / 2f, 650f, streakPaint)

        val namePaint = Paint().apply {
            color = Color.WHITE
            textSize = 70f
            textAlign = Paint.Align.CENTER
            alpha = 220
        }
        canvas.drawText(habit.name, width / 2f, 760f, namePaint)

        val footerPaint = Paint().apply {
            color = Color.WHITE
            textSize = 42f
            textAlign = Paint.Align.CENTER
            alpha = 160
        }
        canvas.drawText("HabitLoop", width / 2f, 980f, footerPaint)

        return bitmap
    }

    private fun saveToCache(context: Context, bitmap: Bitmap): File {
        val cacheDir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val file = File(cacheDir, "streak_card_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}
