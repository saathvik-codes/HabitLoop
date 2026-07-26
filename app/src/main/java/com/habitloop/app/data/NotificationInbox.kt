package com.habitloop.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InboxNotification(
    val id: String,
    val title: String,
    val body: String,
    val category: String,
    val createdAt: Long,
    val read: Boolean
)

object NotificationInbox {
    private const val PREFS = "notification_inbox"
    private const val ITEMS = "items"
    private lateinit var context: Context
    private val mutableItems = MutableStateFlow<List<InboxNotification>>(emptyList())
    val items = mutableItems.asStateFlow()

    fun initialize(appContext: Context) {
        context = appContext.applicationContext
        mutableItems.value = readStored()
    }

    fun add(title: String, body: String, category: String, id: String = "${category}_${System.currentTimeMillis()}") {
        val updated = (listOf(InboxNotification(id, title.take(80), body.take(240), category, System.currentTimeMillis(), false)) +
            mutableItems.value.filterNot { it.id == id }).take(50)
        save(updated)
    }

    fun markAllRead() = save(mutableItems.value.map { it.copy(read = true) })

    private fun save(value: List<InboxNotification>) {
        mutableItems.value = value
        val array = JSONArray()
        value.forEach {
            array.put(JSONObject().apply {
                put("id", it.id); put("title", it.title); put("body", it.body)
                put("category", it.category); put("createdAt", it.createdAt); put("read", it.read)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(ITEMS, array.toString()).apply()
    }

    private fun readStored(): List<InboxNotification> = runCatching {
        val array = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ITEMS, "[]"))
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            InboxNotification(
                item.getString("id"), item.getString("title"), item.getString("body"),
                item.getString("category"), item.getLong("createdAt"), item.optBoolean("read")
            )
        }
    }.getOrDefault(emptyList())
}
