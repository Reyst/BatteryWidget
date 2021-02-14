package gsihome.reyst.battery

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.RemoteViews
import androidx.core.content.ContextCompat


class BatteryWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context?, manager: AppWidgetManager?, appWidgetIds: IntArray?) {
        super.onUpdate(context, manager, appWidgetIds)

        if (context == null || manager == null) return

        val info = getBatteryInfo(context)

        val widgetView = RemoteViews(context.packageName, R.layout.widget)

        val drawableId = if (info.isCharging) R.drawable.ic_battery_charging else 0
        widgetView.setTextViewCompoundDrawablesRelative(R.id.info, drawableId, 0, 0, 0)
        widgetView.setTextViewText(R.id.info, "${info.level}%")

        val color = ContextCompat.getColor(
            context,
            if (info.level < 20) android.R.color.holo_red_dark
            else android.R.color.white
        )

        widgetView.setTextColor(
            R.id.info,
            color
        )



        appWidgetIds?.forEach { updateWidgetById(context, manager, widgetView, it) }
    }

    private fun updateWidgetById(context: Context, manager: AppWidgetManager, widgetView: RemoteViews, widgetId: Int) {
        val updateIntent = Intent(context, BatteryWidget::class.java)
        updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
        val pIntent = PendingIntent.getBroadcast(context, widgetId, updateIntent, 0)
        widgetView.setOnClickPendingIntent(R.id.info, pIntent)

        // Обновляем виджет
        manager.updateAppWidget(widgetId, widgetView)
    }

/*
    private fun getBatteryPercentage(context: Context): Int {
        return if (Build.VERSION.SDK_INT >= 21) {
            val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        } else {
            val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

            context.registerReceiver(null, iFilter)
                ?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val batteryPct = level / scale.toDouble()
                    (batteryPct * 100).toInt()
                }
                ?: -1
        }
    }
*/

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if (intent?.action !in actions || context == null) return

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context.packageName, BatteryWidget::class.java.name))

        Intent(context, BatteryWidget::class.java)
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            .also { context.sendBroadcast(it) }

    }

    private fun getBatteryInfo(context: Context): Info {
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

        return context.registerReceiver(null, iFilter)
            ?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status: Int = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

                val batteryPct = (100 * level / scale.toDouble()).toInt()
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                Info(isCharging, batteryPct)
            }
            ?: Info(false, -1)
    }


    private data class Info(
        val isCharging: Boolean,
        val level: Int,
    )

    companion object {
        private val actions = listOf(
            "android.intent.action.ACTION_POWER_CONNECTED",
            "android.intent.action.ACTION_POWER_DISCONNECTED",
        )
    }
}