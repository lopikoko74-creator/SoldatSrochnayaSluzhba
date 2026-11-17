package ru.soldat.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

private val Olive = Color(0xFF26372B)
private val Panel = Color(0xFF35483A)
private val Paper = Color(0xFFE9E3D2)
private val Gold = Color(0xFFD4B36A)

enum class Career(val title: String, val description: String) {
    CONSCRIPT("Срочная служба", "365 дней от прибытия до дембеля"),
    CONTRACT("Контракт", "Профессиональная служба и карьера"),
    OFFICER("Офицер", "Командование подразделением")
}

data class GameState(
    val career: String = Career.CONSCRIPT.name,
    val day: Int = 1,
    val health: Int = 100,
    val morale: Int = 80,
    val strength: Int = 35,
    val endurance: Int = 45,
    val intellect: Int = 50,
    val authority: Int = 10,
    val money: Int = 500,
    val reputation: Int = 0,
    val eventIndex: Int = 0,
    val log: List<String> = listOf("Ты прибыл в воинскую часть. Впереди первый день службы.")
)

data class Choice(
    val text: String,
    val result: String,
    val morale: Int = 0,
    val health: Int = 0,
    val authority: Int = 0,
    val reputation: Int = 0,
    val intellect: Int = 0
)

data class Event(val title: String, val speaker: String, val story: String, val choices: List<Choice>)

private val events = listOf(
    Event("Знакомство с сержантом", "Сержант Ковалёв",
        "«Новенький? Документы сюда. Здесь у нас всё просто: сначала думаешь, потом делаешь. Иногда наоборот, но это уже по расписанию».",
        listOf(
            Choice("Чётко представиться", "Сержант одобрительно кивнул. Первое впечатление удалось.", 5, authority = 3, reputation = 2),
            Choice("Пошутить про распорядок", "Ковалёв посмотрел на тебя долгим взглядом. Кажется, чувство юмора здесь проверяют отдельно.", -2, authority = -1),
            Choice("Молча ждать указаний", "Ты не стал торопиться с выводами. Сержант выдал первые распоряжения.", 1, authority = 1)
        )),
    Event("Свободная минута в казарме", "Ефрейтор Миша",
        "Сосед по койке предлагает познакомиться с отделением. У одного есть шахматы, у другого — целая коллекция историй о службе.",
        listOf(
            Choice("Познакомиться со всеми", "За ужином тебя уже узнают по имени.", 7, reputation = 4),
            Choice("Навести порядок в вещах", "Всё разложено по местам. Миша назвал это подозрительно взрослым поступком.", 2),
            Choice("Отдохнуть", "Ты немного восстановился после дороги.", 5, health = 2)
        )),
    Event("Первое утро", "Дежурный",
        "Подъём прозвучал так, будто динамик объявил личную войну с твоим сном. До построения несколько минут.",
        listOf(
            Choice("Быстро собраться и помочь соседу", "Вы успели вовремя. Сосед обещал не забыть помощь.", 4, authority = 1, reputation = 2),
            Choice("Сосредоточиться только на себе", "Ты готов к построению, но сосед едва успел.", 0, authority = 1),
            Choice("Попытаться урвать ещё минуту", "Минута оказалась самой короткой в истории человечества.", -5, authority = -2)
        )),
    Event("Наряд по столовой", "Старшина",
        "Тебя поставили помогать на раздаче. Старшина объясняет порядок, а очередь уже начинает проявлять нетерпение.",
        listOf(
            Choice("Работать спокойно и аккуратно", "Обед прошёл без происшествий. Почти историческое событие.", 3, reputation = 2),
            Choice("Подбодрить очередь шуткой", "Несколько человек улыбнулись. Старшина сделал вид, что не заметил.", 5, authority = 1),
            Choice("Попросить показать всё ещё раз", "Ты разобрался в процессе и избежал ошибок.", 2, intellect = 2)
        )),
    Event("Письмо из дома", "Почтальон части",
        "Пришло письмо. Несколько строк от близких неожиданно делают обычный день гораздо легче.",
        listOf(
            Choice("Прочитать письмо в тишине", "Ты почувствовал поддержку и немного приободрился.", morale = 8),
            Choice("Поделиться новостями с соседом", "Разговор помог отвлечься обоим.", 8, reputation = 2),
            Choice("Сразу ответить", "Ты написал домой, что держишься и привыкаешь к распорядку.", 10, intellect = 1)
        ))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SoldierGame() }
    }
}

@Composable
fun SoldierGame() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("soldat_save", 0) }
    var state by remember { mutableStateOf(loadState(prefs.getString("state", null))) }
    var started by remember { mutableStateOf(prefs.getBoolean("started", false)) }
    var selectedCareer by remember { mutableStateOf(Career.CONSCRIPT) }
    var showProfile by remember { mutableStateOf(false) }

    fun persist(s: GameState) {
        state = s
        prefs.edit().putString("state", JSONObject().apply {
            put("career", s.career); put("day", s.day); put("health", s.health)
            put("morale", s.morale); put("strength", s.strength); put("endurance", s.endurance)
            put("intellect", s.intellect); put("authority", s.authority)
            put("money", s.money); put("reputation", s.reputation); put("eventIndex", s.eventIndex)
            put("log", s.log.joinToString("\n"))
        }.toString()).apply()
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Gold, background = Olive, surface = Panel,
        onPrimary = Olive, onBackground = Paper, onSurface = Paper
    )) {
        Surface(Modifier.fillMaxSize(), color = Olive) {
            if (!started) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
                    verticalArrangement = Arrangement.Center) {
                    Text("СОЛДАТ", fontSize = 38.sp, fontWeight = FontWeight.Black, color = Gold)
                    Text("СРОЧНАЯ СЛУЖБА", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Paper)
                    Spacer(Modifier.height(10.dp))
                    Text("Твоя история начинается здесь.", color = Paper.copy(alpha = .8f))
                    Spacer(Modifier.height(26.dp))
                    Text("ВЫБЕРИ ПУТЬ", color = Gold, fontWeight = FontWeight.Bold)
                    Career.values().forEach { career ->
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { selectedCareer = career },
                            colors = CardDefaults.cardColors(containerColor = if (selectedCareer == career) Gold else Panel),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(career.title, color = if (selectedCareer == career) Olive else Paper,
                                    fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(career.description, color = if (selectedCareer == career) Olive else Paper.copy(alpha = .7f))
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = {
                        val fresh = GameState(career = selectedCareer.name)
                        persist(fresh)
                        prefs.edit().putBoolean("started", true).apply()
                        started = true
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                        Text("НАЧАТЬ СЛУЖБУ", color = Olive, fontWeight = FontWeight.Black)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("СОЛДАТ", color = Gold, fontWeight = FontWeight.Black, fontSize = 25.sp)
                            Text(Career.valueOf(state.career).title.uppercase(), color = Paper.copy(alpha = .7f), fontSize = 11.sp)
                        }
                        TextButton(onClick = { showProfile = !showProfile }) {
                            Text(if (showProfile) "К СОБЫТИЮ" else "ПРОФИЛЬ", color = Gold)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatChip("ДЕНЬ", "${state.day}")
                        StatChip("ЗДОРОВЬЕ", "${state.health}%")
                        StatChip("МОРАЛЬ", "${state.morale}%")
                    }
                    Spacer(Modifier.height(14.dp))
                    if (showProfile) {
                        PanelCard("ЛИЧНОЕ ДЕЛО") {
                            ProfileLine("Сила", state.strength)
                            ProfileLine("Выносливость", state.endurance)
                            ProfileLine("Интеллект", state.intellect)
                            ProfileLine("Авторитет", state.authority)
                            ProfileLine("Репутация", state.reputation)
                            Text("Деньги: ${state.money} ₽", color = Paper)
                            Text("Сохранение выполняется автоматически.", color = Paper.copy(alpha = .6f), fontSize = 12.sp)
                        }
                        PanelCard("ПОСЛЕДНИЕ СОБЫТИЯ") {
                            state.log.takeLast(8).reversed().forEach {
                                Text("• $it", color = Paper, modifier = Modifier.padding(vertical = 3.dp))
                            }
                        }
                    } else {
                        val event = events[state.eventIndex % events.size]
                        PanelCard("СЛУЖЕБНЫЙ ДЕНЬ") {
                            Text("ДЕНЬ ${state.day}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(event.title, color = Paper, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Text(event.speaker, color = Gold, fontSize = 13.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(event.story, color = Paper, fontSize = 16.sp, lineHeight = 23.sp)
                            Spacer(Modifier.height(18.dp))
                            Text("ТВОЁ РЕШЕНИЕ", color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            event.choices.forEach { choice ->
                                OutlinedButton(
                                    onClick = {
                                        val next = state.copy(
                                            day = state.day + 1,
                                            health = (state.health + choice.health).coerceIn(0, 100),
                                            morale = (state.morale + choice.morale).coerceIn(0, 100),
                                            authority = (state.authority + choice.authority).coerceIn(0, 100),
                                            reputation = (state.reputation + choice.reputation).coerceIn(0, 100),
                                            intellect = (state.intellect + choice.intellect).coerceIn(0, 100),
                                            eventIndex = state.eventIndex + 1,
                                            log = (state.log + "День ${state.day}: ${choice.result}").takeLast(30)
                                        )
                                        persist(next)
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Paper)
                                ) { Text(choice.text, modifier = Modifier.padding(4.dp)) }
                            }
                        }
                        PanelCard("РАСПОРЯДОК") {
                            Text("06:00  Подъём и утренние процедуры", color = Paper.copy(alpha = .8f))
                            Text("08:00  Завтрак", color = Paper.copy(alpha = .8f))
                            Text("09:00  Занятия и задачи дня", color = Paper.copy(alpha = .8f))
                            Text("18:00  Личное время", color = Paper.copy(alpha = .8f))
                            Text("22:00  Отбой", color = Paper.copy(alpha = .8f))
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Версия 0.1.0 • ранняя игровая сборка", color = Paper.copy(alpha = .45f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String) {
    Column(Modifier.fillMaxWidth(0.33f).background(Panel, RoundedCornerShape(10.dp)).padding(10.dp)) {
        Text(label, color = Paper.copy(alpha = .6f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Gold, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
fun PanelCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun ProfileLine(name: String, value: Int) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, color = Paper)
            Text("$value/100", color = Gold, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { value.coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = Gold,
            trackColor = Olive
        )
    }
}

private fun loadState(raw: String?): GameState {
    if (raw == null) return GameState()
    return try {
        val j = JSONObject(raw)
        val career = j.optString("career", Career.CONSCRIPT.name)
            .takeIf { it in Career.values().map(Career::name) } ?: Career.CONSCRIPT.name
        GameState(
            career = career,
            day = j.optInt("day", 1).coerceAtLeast(1),
            health = j.optInt("health", 100).coerceIn(0, 100),
            morale = j.optInt("morale", 80).coerceIn(0, 100),
            strength = j.optInt("strength", 35).coerceIn(0, 100),
            endurance = j.optInt("endurance", 45).coerceIn(0, 100),
            intellect = j.optInt("intellect", 50).coerceIn(0, 100),
            authority = j.optInt("authority", 10).coerceIn(0, 100),
            money = j.optInt("money", 500).coerceAtLeast(0),
            reputation = j.optInt("reputation", 0).coerceIn(0, 100),
            eventIndex = j.optInt("eventIndex", 0).coerceAtLeast(0),
            log = j.optString("log", "").split("\n").filter { it.isNotBlank() }.takeLast(30)
        )
    } catch (_: Exception) {
        GameState()
    }
}
