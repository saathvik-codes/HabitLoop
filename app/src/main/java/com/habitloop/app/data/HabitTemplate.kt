package com.habitloop.app.data

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.habitloop.app.R

data class HabitTemplate(
    val id: String,
    val displayName: String,
    val emoji: String,
    @DrawableRes val iconRes: Int,
    val accentColor: Color,
    val reminderCopy: String
)

object HabitTemplates {
    val ALL = listOf(
        HabitTemplate("gym", "Gym", "🏋️", R.drawable.ic_template_gym, Color(0xFFFF7A45), "Time to move. A smaller session still counts."),
        HabitTemplate("study", "Study", "📚", R.drawable.ic_template_study, Color(0xFF4A90D9), "One focused session keeps the loop moving."),
        HabitTemplate("coding", "Coding", "💻", R.drawable.ic_template_coding, Color(0xFF6FCF97), "Build or learn something small today."),
        HabitTemplate("reading", "Reading", "📖", R.drawable.ic_template_reading, Color(0xFFBB6BD9), "A few pages count."),
        HabitTemplate("meditation", "Meditation", "🧘", R.drawable.ic_template_meditation, Color(0xFF56CCF2), "Take five quiet minutes."),
        HabitTemplate("sobriety", "Sobriety", "🌱", R.drawable.ic_template_sobriety, Color(0xFF27AE60), "Return to the choice that supports you."),
        HabitTemplate("prayer", "Prayer", "🙏", R.drawable.ic_template_prayer, Color(0xFFF2C94C), "Take a meaningful moment."),
        HabitTemplate("sleep", "Sleep routine", "🌙", R.drawable.ic_template_meditation, Color(0xFF6C79B8), "Begin your wind-down routine."),
        HabitTemplate("hydration", "Drink water", "💧", R.drawable.ic_template_meditation, Color(0xFF4DA6C8), "A glass of water is a useful reset."),
        HabitTemplate("walking", "Daily walk", "🚶", R.drawable.ic_template_gym, Color(0xFF68A66A), "Step outside and move at your pace."),
        HabitTemplate("medication", "Medication", "💊", R.drawable.ic_template_sobriety, Color(0xFFD46A7E), "Check your prescribed routine."),
        HabitTemplate("nutrition", "Balanced meal", "🥗", R.drawable.ic_template_gym, Color(0xFF76A85B), "Choose one meal that supports your day."),
        HabitTemplate("journaling", "Journal", "✍️", R.drawable.ic_template_reading, Color(0xFFA578B5), "Write one honest line."),
        HabitTemplate("chores", "Home reset", "🧹", R.drawable.ic_template_gym, Color(0xFFD09255), "A ten-minute reset makes tomorrow easier."),
        HabitTemplate("budget", "Money check-in", "💰", R.drawable.ic_template_study, Color(0xFF4C9A75), "Review today’s spending without judgment."),
        HabitTemplate("connection", "Connect with someone", "🤝", R.drawable.ic_template_prayer, Color(0xFFE48B75), "Send one thoughtful message."),
        HabitTemplate("creative", "Creative practice", "🎨", R.drawable.ic_template_coding, Color(0xFFC26D9A), "Make something small and unfinished."),
        HabitTemplate("screen_break", "Screen break", "📵", R.drawable.ic_template_meditation, Color(0xFF718096), "Put the screen down for a real pause."),
        HabitTemplate("language", "Language practice", "🗣️", R.drawable.ic_template_study, Color(0xFF5F86C7), "Practice a few useful words today."),
        HabitTemplate("stretch", "Stretching", "🤸", R.drawable.ic_template_gym, Color(0xFFE08C62), "Give your body five gentle minutes."),
        HabitTemplate("planning", "Plan tomorrow", "🗓️", R.drawable.ic_template_study, Color(0xFF7E8FB2), "Choose tomorrow’s three important actions.")
    )

    fun byId(id: String): HabitTemplate = ALL.firstOrNull { it.id == id } ?: ALL.first()
}
