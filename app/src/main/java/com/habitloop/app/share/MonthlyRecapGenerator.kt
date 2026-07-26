package com.habitloop.app.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.habitloop.app.data.Habit
import com.habitloop.app.data.HabitCompletion
import com.habitloop.app.data.HabitInsights
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * A second shareable moment beyond the daily streak card — a monthly recap,
 * mirroring what Spotify Wrapped does for retention: give people a reason
 * to open and share the app once a month even on days they didn't complete
 * anything, not just streak days.
 */
object MonthlyRecapGenerator {

    fun shareRecap(context: Context, habits: List<Habit>, monthCompletions: List<HabitCompletion>) {
        val bitmap = renderCard(habits, monthCompletions)
        val file = saveToCache(context, bitmap)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share your month"))
    }

    private fun renderCard(habits: List<Habit>, monthCompletions: List<HabitCompletion>): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(0x1C, 0x1B, 0x1F))

        val monthName = LocalDate.now().month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 64f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        canvas.drawText(monthName, width / 2f, 140f, titlePaint)

        val subtitlePaint = Paint().apply {
            color = Color.LTGRAY
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("in review", width / 2f, 195f, subtitlePaint)

        val bestStreak = habits.maxOfOrNull { it.longestStreak } ?: 0
        val momentum = HabitInsights.momentumScore(habits)
        val activeHabits = habits.count { it.currentStreak > 0 }

        drawStat(canvas, width / 2f, 400f, monthCompletions.size.toString(), "habits completed this month")
        drawStat(canvas, width / 2f, 640f, activeHabits.toString(), "habits currently on a streak")
        drawStat(canvas, width / 2f, 880f, bestStreak.toString(), "longest streak")
        drawStat(canvas, width / 2f, 1120f, "$momentum%", "momentum score")

        val footerPaint = Paint().apply {
            color = Color.LTGRAY
            textSize = 40f
            textAlign = Paint.Align.CENTER
            alpha = 180
        }
        canvas.drawText("HabitLoop", width / 2f, 1280f, footerPaint)

        return bitmap
    }

    private fun drawStat(canvas: Canvas, centerX: Float, y: Float, value: String, label: String) {
        val valuePaint = Paint().apply {
            color = Color.rgb(0xFF, 0x7A, 0x45)
            textSize = 96f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        canvas.drawText(value, centerX, y, valuePaint)

        val labelPaint = Paint().apply {
            color = Color.WHITE
            textSize = 38f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label, centerX, y + 50f, labelPaint)
    }

    private fun saveToCache(context: Context, bitmap: Bitmap): File {
        val cacheDir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val file = File(cacheDir, "monthly_recap_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return file
    }
}
