# Room entities are constructed via reflection by the generated DAO impl;
# keep field names so schema mapping doesn't silently break post-minify.
-keep class com.habitloop.app.data.Habit { *; }
-keep class com.habitloop.app.data.HabitCompletion { *; }

# Glance widget receiver is instantiated by the system via the manifest
# <receiver> entry, not a direct code reference — R8 can't see that usage
# and will strip it without this.
-keep class com.habitloop.app.widget.HabitStreakWidgetReceiver { *; }
-keep class com.habitloop.app.widget.HabitStreakWidget { *; }

# WorkManager instantiates Workers by reflection from the class name stored
# in the WorkRequest, same issue as above.
-keep class com.habitloop.app.worker.ReminderWorker { *; }
