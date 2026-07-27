package com.habitloop.app.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.habitloop.app.MainActivity
import com.habitloop.app.data.HabitDatabase
import com.habitloop.app.data.HabitTemplates
import com.habitloop.app.data.isDueOn
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/**
 * Home-screen widget: shows each habit's current streak and lets the user
 * tap straight through to the app to log today's completion — the whole
 * point being "log a habit in under 2 seconds, no app-open required" from
 * the design brief. This is the single biggest differentiator versus the
 * saturated habit-tracker space, most of which ship no widget at all.
 */
class HabitStreakWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: android.content.Context, id: GlanceId) {
        val dao = HabitDatabase.getInstance(context).habitDao()
        val habits = dao.observeHabits().first().filter { it.isDueOn(LocalDate.now()) }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF1C1B1F)))
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Text(
                    text = "Today",
                    style = TextStyle(fontWeight = FontWeight.Bold, color = ColorProvider(androidx.compose.ui.graphics.Color.White))
                )
                habits.take(4).forEach { habit ->
                    val template = HabitTemplates.byId(habit.templateId)
                    Row(modifier = GlanceModifier.padding(top = 6.dp)) {
                        Image(
                            provider = ImageProvider(template.iconRes),
                            contentDescription = null,
                            modifier = GlanceModifier.size(20.dp)
                        )
                        Text(
                            text = " ${habit.name}",
                            style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.White))
                        )
                        Image(
                            provider = ImageProvider(com.habitloop.app.R.drawable.ic_streak_flame),
                            contentDescription = null,
                            modifier = GlanceModifier.padding(start = 4.dp).size(16.dp)
                        )
                        Text(
                            text = "${habit.currentStreak}",
                            style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.White))
                        )
                    }
                }
                if (habits.isEmpty()) {
                    Text(
                        text = "Add your first habit",
                        style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.LightGray))
                    )
                }
            }
        }
    }
}

class HabitStreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitStreakWidget()
}
