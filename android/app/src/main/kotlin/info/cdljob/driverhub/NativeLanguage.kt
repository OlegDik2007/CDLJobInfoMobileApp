package info.cdljob.driverhub

import android.content.Context
import java.util.Locale

object NativeLanguage {
    private const val PREFS = "cdl_native_language"
    private const val KEY = "language"
    private val supported = setOf("en", "uk", "ru", "es", "uz")

    fun set(context: Context, language: String) {
        val selected = language.lowercase(Locale.ROOT).take(2)
        if (selected in supported) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, selected).apply()
        }
    }

    fun current(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)?.takeIf { it in supported }
            ?: context.resources.configuration.locales[0].language
                .takeIf { it in supported } ?: "en"

    fun t(context: Context, key: String): String {
        val translations = when (key) {
            "tracking_channel" -> listOf("Road alert tracking", "Моніторинг дорожніх попереджень", "Мониторинг дорожных предупреждений", "Seguimiento de alertas viales", "Yo‘l ogohlantirishlarini kuzatish")
            "tracking_description" -> listOf("Keeps your road-alert location current while the app is closed.", "Оновлює розташування для попереджень, коли застосунок закритий.", "Обновляет местоположение для предупреждений, когда приложение закрыто.", "Actualiza tu ubicación para las alertas al cerrar la app.", "Ilova yopilganda ogohlantirishlar uchun joylashuvni yangilaydi.")
            "tracking_title" -> listOf("CDL Hub road alerts active", "Дорожні попередження CDL Hub активні", "Дорожные предупреждения CDL Hub активны", "Alertas CDL Hub activas", "CDL Hub ogohlantirishlari faol")
            "tracking_body" -> listOf("Checking your area for new road hazards", "Перевіряємо небезпеки на дорозі поблизу", "Проверяем опасности на дороге поблизости", "Buscando peligros viales cercanos", "Yaqindagi yo‘l xavflari tekshirilmoqda")
            "alert_channel" -> listOf("Road alerts", "Дорожні попередження", "Дорожные предупреждения", "Alertas viales", "Yo‘l ogohlantirishlari")
            "alert_description" -> listOf("Crashes, closures, truck restrictions and severe road conditions nearby", "Аварії, перекриття, обмеження вантажівок і небезпечні дорожні умови", "Аварии, перекрытия, ограничения грузовиков и опасные дорожные условия", "Choques, cierres, restricciones y condiciones peligrosas", "Avariya, yopilish, cheklov va xavfli yo‘l sharoiti")
            "ahead" -> listOf("ahead", "попереду", "впереди", "adelante", "oldinda")
            "away" -> listOf("away", "від вас", "от вас", "de distancia", "masofada")
            "nearby" -> listOf("nearby", "поблизу", "поблизости", "cerca", "yaqinda")
            "miles" -> listOf("mi", "миль", "миль", "mi", "mil")
            "road_alert" -> listOf("Road alert", "Дорожнє попередження", "Дорожное предупреждение", "Alerta vial", "Yo‘l ogohlantirishi")
            "new_road_alert" -> listOf("New road alert nearby. Open CDL Hub for details.", "Нове дорожнє попередження поблизу. Відкрийте CDL Hub.", "Новое дорожное предупреждение рядом. Откройте CDL Hub.", "Nueva alerta vial cercana. Abre CDL Hub.", "Yaqinda yangi yo‘l ogohlantirishi. CDL Hub’ni oching.")
            "truck_restriction" -> listOf("Truck restriction", "Обмеження для вантажівок", "Ограничение для грузовиков", "Restricción de camiones", "Yuk mashinasi cheklovi")
            "crash" -> listOf("Crash", "Аварія", "Авария", "Accidente", "Avariya")
            "notification_location_off" -> listOf("Location is off. Use the road search in the app.", "Геолокацію вимкнено. Скористайтеся пошуком доріг.", "Геолокация отключена. Используйте поиск дорог.", "GPS desactivado. Busca una carretera en la app.", "GPS o‘chirilgan. Ilovada yo‘lni qidiring.")
            else -> return key
        }
        val index = when (current(context)) {
            "uk" -> 1
            "ru" -> 2
            "es" -> 3
            "uz" -> 4
            else -> 0
        }
        return translations[index]
    }

    fun labelTitle(context: Context, original: String): String {
        var result = original
        val replacements = listOf(
            "Truck restriction" to "truck_restriction",
            "Road alert" to "road_alert",
            "Crash" to "crash",
            " ahead" to "ahead",
            " away" to "away"
        )
        for ((source, key) in replacements) {
            val translated = t(context, key)
            if (translated != source.trim()) {
                result = result.replace(source, if (source.startsWith(' ')) " " + translated else translated, ignoreCase = true)
            }
        }
        return result
    }
}
