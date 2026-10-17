package com.example.kaizenkanban.domain.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object TaskCardKind {
    const val PLAIN = "plain"
    const val WORKOUT = "workout"
    const val SHOP = "shop"
    const val MEET = "meet"

    fun normalize(raw: String?): String = when (raw) {
        WORKOUT, SHOP, MEET -> raw
        else -> PLAIN
    }
}

data class CardLine(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val extra: String = "",
    val done: Boolean = false,
    val sets: List<Boolean> = emptyList()
)

/**
 * Structured body of one task. Inner rows are not separate board tasks.
 * [kind] matches [Task.cardType].
 */
data class TaskCardBody(
    val kind: String = TaskCardKind.PLAIN,
    val lines: List<CardLine> = emptyList(),
    val contactIds: List<String> = emptyList(),
    val outcome: String = "",
    val readKind: String = "book",
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val timerSeconds: Int = 90
) {
    fun progressCounts(): Pair<Int, Int> {
        return when (TaskCardKind.normalize(kind)) {
            TaskCardKind.WORKOUT -> {
                var done = 0
                var total = 0
                lines.forEach { line ->
                    if (line.text.isBlank()) return@forEach
                    if (line.sets.isNotEmpty()) {
                        total += line.sets.size
                        done += line.sets.count { it }
                    } else {
                        total += 1
                        if (line.done) done += 1
                    }
                }
                done to total
            }
            TaskCardKind.SHOP, TaskCardKind.MEET -> {
                val real = lines.filter { it.text.isNotBlank() }
                real.count { it.done } to real.size
            }
            else -> 0 to 0
        }
    }

    fun fraction(): Float {
        val (done, total) = progressCounts()
        if (total <= 0) return 0f
        return (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    /** Every named row is checked, and there is at least one such row. */
    fun isFullyChecked(): Boolean {
        return when (TaskCardKind.normalize(kind)) {
            TaskCardKind.WORKOUT -> {
                val real = lines.filter { it.text.isNotBlank() && it.sets.isNotEmpty() }
                real.isNotEmpty() && real.all { line -> line.sets.all { it } }
            }
            TaskCardKind.SHOP, TaskCardKind.MEET -> {
                val real = lines.filter { it.text.isNotBlank() }
                real.isNotEmpty() && real.all { it.done }
            }
            else -> false
        }
    }

    /** Fresh recurring instance: the plan stays, checks and page progress start over. */
    fun cleared(): TaskCardBody = copy(
        lines = lines.map { line -> line.copy(done = false, sets = line.sets.map { false }) },
        outcome = "",
        currentPage = 0
    )

    /** Appends [id] when it is new. An existing contact stays in place. */
    fun plusContact(id: String): TaskCardBody {
        val clean = id.trim()
        if (clean.isEmpty() || clean in contactIds) return this
        return copy(contactIds = contactIds + clean)
    }

    fun encode(): String {
        if (TaskCardKind.normalize(kind) == TaskCardKind.PLAIN) return ""
        val arr = JSONArray()
        lines.forEach { line ->
            val sets = JSONArray()
            line.sets.forEach { sets.put(it) }
            arr.put(
                JSONObject()
                    .put("id", line.id)
                    .put("text", line.text)
                    .put("extra", line.extra)
                    .put("done", line.done)
                    .put("sets", sets)
            )
        }
        val ids = JSONArray()
        val cleanIds = contactIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        cleanIds.forEach { ids.put(it) }
        return JSONObject()
            .put("lines", arr)
            .put("contactIds", ids)
            .put("contactId", cleanIds.firstOrNull() ?: JSONObject.NULL)
            .put("outcome", outcome)
            .put("readKind", if (readKind == "course") "course" else "book")
            .put("totalPages", totalPages.coerceAtLeast(0))
            .put("currentPage", currentPage.coerceAtLeast(0))
            .put("timerSeconds", timerSeconds.coerceAtLeast(0))
            .toString()
    }

    companion object {
        fun decode(kind: String, payload: String): TaskCardBody {
            val normalized = TaskCardKind.normalize(kind)
            if (normalized == TaskCardKind.PLAIN || payload.isBlank()) {
                return TaskCardBody(kind = normalized)
            }
            return try {
                val obj = JSONObject(payload)
                val arr = obj.optJSONArray("lines") ?: JSONArray()
                val lines = buildList {
                    for (i in 0 until arr.length()) {
                        val line = arr.optJSONObject(i) ?: continue
                        val setsArr = line.optJSONArray("sets")
                        val sets = if (setsArr == null) {
                            emptyList()
                        } else {
                            buildList {
                                for (j in 0 until setsArr.length()) add(setsArr.optBoolean(j, false))
                            }
                        }
                        add(
                            CardLine(
                                id = line.optString("id").ifBlank { UUID.randomUUID().toString() },
                                text = line.optString("text"),
                                extra = line.optString("extra"),
                                done = line.optBoolean("done", false),
                                sets = sets
                            )
                        )
                    }
                }
                TaskCardBody(
                    kind = normalized,
                    lines = lines,
                    contactIds = readContactIds(obj),
                    outcome = obj.optString("outcome"),
                    readKind = if (obj.optString("readKind") == "course") "course" else "book",
                    totalPages = obj.optInt("totalPages", 0).coerceAtLeast(0),
                    currentPage = obj.optInt("currentPage", 0).coerceAtLeast(0),
                    timerSeconds = obj.optInt("timerSeconds", 90).coerceAtLeast(0)
                )
            } catch (_: Exception) {
                TaskCardBody(kind = normalized)
            }
        }

        /** New cards store [contactIds]. Older cards stored a single contactId. */
        private fun readContactIds(obj: JSONObject): List<String> {
            val arr = obj.optJSONArray("contactIds")
            val fromArray = if (arr == null) {
                emptyList()
            } else {
                buildList {
                    for (i in 0 until arr.length()) {
                        val id = arr.optString(i).trim()
                        if (id.isNotEmpty() && id !in this) add(id)
                    }
                }
            }
            if (obj.has("contactIds") && !obj.isNull("contactIds")) return fromArray
            if (!obj.has("contactId") || obj.isNull("contactId")) return emptyList()
            val single = obj.optString("contactId").trim()
            return if (single.isEmpty()) emptyList() else listOf(single)
        }
    }
}
