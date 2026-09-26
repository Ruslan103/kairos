package com.example.kaizenkanban.data.transfer

import android.net.Uri
import android.util.Base64
import com.example.kaizenkanban.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object KairosTransferHelper {

    fun toJson(data: KairosTransferData): String {
        val root = JSONObject()
        root.put("version", data.version)
        root.put("exportedAt", data.exportedAt)
        root.put("app", data.app)

        val projectsArr = JSONArray()
        data.projects.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("position", p.position)
            projectsArr.put(obj)
        }
        root.put("projects", projectsArr)

        val boardsArr = JSONArray()
        data.boards.forEach { b ->
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("projectId", b.projectId)
            obj.put("name", b.name)
            obj.put("isDefault", b.isDefault)
            obj.put("isArchived", b.isArchived)
            boardsArr.put(obj)
        }
        root.put("boards", boardsArr)

        val categoriesArr = JSONArray()
        data.categories.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("color", c.color)
            categoriesArr.put(obj)
        }
        root.put("categories", categoriesArr)

        val columnsArr = JSONArray()
        data.columns.forEach { col ->
            val obj = JSONObject()
            obj.put("id", col.id)
            obj.put("boardId", col.boardId)
            obj.put("title", col.title)
            obj.put("position", col.position)
            columnsArr.put(obj)
        }
        root.put("columns", columnsArr)

        val tasksArr = JSONArray()
        data.tasks.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("columnId", t.columnId)
            obj.put("title", t.title)
            obj.put("description", t.description)
            obj.put("position", t.position)
            if (t.categoryId != null) obj.put("categoryId", t.categoryId)
            if (t.dueDate != null) obj.put("dueDate", t.dueDate)
            obj.put("isCompleted", t.isCompleted)
            if (t.completedAt != null) obj.put("completedAt", t.completedAt)
            obj.put("createdAt", t.createdAt)
            if (t.eisenhowerQuadrant != null) obj.put("eisenhowerQuadrant", t.eisenhowerQuadrant)
            obj.put("showEisenhowerButtons", t.showEisenhowerButtons)
            obj.put("isHidden", t.isHidden)
            if (t.repeatRule != null) obj.put("repeatRule", t.repeatRule)
            if (t.reminderMinutesOfDay != null) obj.put("reminderMinutesOfDay", t.reminderMinutesOfDay)
            if (t.recurringTemplateId != null) obj.put("recurringTemplateId", t.recurringTemplateId)
            if (t.complexity != null) obj.put("complexity", t.complexity)
            if (t.estimatedMinutes != null) obj.put("estimatedMinutes", t.estimatedMinutes)
            if (t.completionQuality != null) obj.put("completionQuality", t.completionQuality)
            obj.put("workflowStatus", t.workflowStatus)
            obj.put("isBoardArchived", t.isBoardArchived)
            obj.put("statsExcluded", t.statsExcluded)
            obj.put("isGoal", t.isGoal)
            if (t.goalStatsEpochMillis != null) obj.put("goalStatsEpochMillis", t.goalStatsEpochMillis)
            if (t.hubGroupId != null) obj.put("hubGroupId", t.hubGroupId)
            if (t.linkedColumnIds.isNotEmpty()) {
                val linkedArr = JSONArray()
                t.linkedColumnIds.forEach { linkedArr.put(it) }
                obj.put("linkedColumnIds", linkedArr)
            }
            tasksArr.put(obj)
        }
        root.put("tasks", tasksArr)

        val commentsArr = JSONArray()
        data.comments.forEach { cm ->
            val obj = JSONObject()
            obj.put("id", cm.id)
            obj.put("taskId", cm.taskId)
            obj.put("text", cm.text)
            obj.put("createdAt", cm.createdAt)
            commentsArr.put(obj)
        }
        root.put("comments", commentsArr)

        val colCommentsArr = JSONArray()
        data.columnComments.forEach { cm ->
            val obj = JSONObject()
            obj.put("id", cm.id)
            obj.put("columnId", cm.columnId)
            obj.put("text", cm.text)
            obj.put("createdAt", cm.createdAt)
            colCommentsArr.put(obj)
        }
        root.put("columnComments", colCommentsArr)

        val contactsArr = JSONArray()
        data.contacts.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("projectId", c.projectId)
            obj.put("name", c.name)
            obj.put("phone", c.phone)
            obj.put("email", c.email)
            obj.put("role", c.role)
            obj.put("position", c.position)
            contactsArr.put(obj)
        }
        root.put("contacts", contactsArr)

        val linksArr = JSONArray()
        data.taskLinks.forEach { link ->
            val obj = JSONObject()
            obj.put("parentId", link.parentId)
            obj.put("childId", link.childId)
            obj.put("createdAt", link.createdAt)
            linksArr.put(obj)
        }
        root.put("taskLinks", linksArr)

        return root.toString(2)
    }

    fun fromJson(jsonStr: String): KairosTransferData {
        val root = JSONObject(jsonStr)
        val version = root.optInt("version", 1)
        val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
        val app = root.optString("app", "Kairos")

        val projects = mutableListOf<Project>()
        val projectsArr = root.optJSONArray("projects") ?: JSONArray()
        for (i in 0 until projectsArr.length()) {
            val obj = projectsArr.getJSONObject(i)
            projects.add(
                Project(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    position = obj.optInt("position", i)
                )
            )
        }

        val boards = mutableListOf<Board>()
        val boardsArr = root.optJSONArray("boards") ?: JSONArray()
        for (i in 0 until boardsArr.length()) {
            val obj = boardsArr.getJSONObject(i)
            boards.add(
                Board(
                    id = obj.getString("id"),
                    projectId = obj.getString("projectId"),
                    name = obj.getString("name"),
                    isDefault = obj.optBoolean("isDefault", false),
                    isArchived = obj.optBoolean("isArchived", false)
                )
            )
        }

        val categories = mutableListOf<Category>()
        val categoriesArr = root.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until categoriesArr.length()) {
            val obj = categoriesArr.getJSONObject(i)
            categories.add(
                Category(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    color = obj.getLong("color")
                )
            )
        }

        val columns = mutableListOf<Column>()
        val columnsArr = root.optJSONArray("columns") ?: JSONArray()
        for (i in 0 until columnsArr.length()) {
            val obj = columnsArr.getJSONObject(i)
            columns.add(
                Column(
                    id = obj.getString("id"),
                    boardId = obj.getString("boardId"),
                    title = obj.getString("title"),
                    position = obj.getInt("position")
                )
            )
        }

        val tasks = mutableListOf<Task>()
        val tasksArr = root.optJSONArray("tasks") ?: JSONArray()
        for (i in 0 until tasksArr.length()) {
            val obj = tasksArr.getJSONObject(i)
            tasks.add(
                Task(
                    id = obj.getString("id"),
                    columnId = obj.getString("columnId"),
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    position = obj.optInt("position", i),
                    categoryId = if (obj.has("categoryId") && !obj.isNull("categoryId")) obj.getString("categoryId") else null,
                    dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate")) obj.getLong("dueDate") else null,
                    isCompleted = obj.optBoolean("isCompleted", false),
                    completedAt = if (obj.has("completedAt") && !obj.isNull("completedAt")) obj.getLong("completedAt") else null,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    eisenhowerQuadrant = if (obj.has("eisenhowerQuadrant") && !obj.isNull("eisenhowerQuadrant")) obj.getString("eisenhowerQuadrant") else null,
                    showEisenhowerButtons = obj.optBoolean("showEisenhowerButtons", false),
                    isHidden = obj.optBoolean("isHidden", false),
                    repeatRule = if (obj.has("repeatRule") && !obj.isNull("repeatRule")) obj.getString("repeatRule") else null,
                    reminderMinutesOfDay = if (obj.has("reminderMinutesOfDay") && !obj.isNull("reminderMinutesOfDay")) {
                        obj.getInt("reminderMinutesOfDay")
                    } else null,
                    recurringTemplateId = if (obj.has("recurringTemplateId") && !obj.isNull("recurringTemplateId")) {
                        obj.getString("recurringTemplateId")
                    } else null,
                    complexity = if (obj.has("complexity") && !obj.isNull("complexity")) obj.getInt("complexity") else null,
                    estimatedMinutes = if (obj.has("estimatedMinutes") && !obj.isNull("estimatedMinutes")) {
                        obj.getInt("estimatedMinutes")
                    } else null,
                    completionQuality = if (obj.has("completionQuality") && !obj.isNull("completionQuality")) {
                        obj.getInt("completionQuality")
                    } else null,
                    workflowStatus = obj.optString("workflowStatus", "").ifBlank {
                        if (obj.optBoolean("isCompleted", false)) "done" else "open"
                    },
                    isBoardArchived = obj.optBoolean("isBoardArchived", false),
                    statsExcluded = obj.optBoolean("statsExcluded", false),
                    isGoal = obj.optBoolean("isGoal", false),
                    goalStatsEpochMillis = if (obj.has("goalStatsEpochMillis") && !obj.isNull("goalStatsEpochMillis")) {
                        obj.getLong("goalStatsEpochMillis")
                    } else null,
                    hubGroupId = if (obj.has("hubGroupId") && !obj.isNull("hubGroupId")) {
                        obj.getString("hubGroupId")
                    } else null,
                    linkedColumnIds = buildList {
                        val linkedArr = obj.optJSONArray("linkedColumnIds")
                        if (linkedArr != null) {
                            for (j in 0 until linkedArr.length()) {
                                add(linkedArr.getString(j))
                            }
                        } else {
                            obj.optString("linkedColumnIds", "")
                                .split(',')
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .forEach { add(it) }
                        }
                    }
                )
            )
        }

        val comments = mutableListOf<Comment>()
        val commentsArr = root.optJSONArray("comments") ?: JSONArray()
        for (i in 0 until commentsArr.length()) {
            val obj = commentsArr.getJSONObject(i)
            comments.add(
                Comment(
                    id = obj.getString("id"),
                    taskId = obj.getString("taskId"),
                    text = obj.getString("text"),
                    createdAt = obj.getLong("createdAt")
                )
            )
        }

        val columnComments = mutableListOf<ColumnComment>()
        val colCommentsArr = root.optJSONArray("columnComments") ?: JSONArray()
        for (i in 0 until colCommentsArr.length()) {
            val obj = colCommentsArr.getJSONObject(i)
            columnComments.add(
                ColumnComment(
                    id = obj.getString("id"),
                    columnId = obj.getString("columnId"),
                    text = obj.getString("text"),
                    createdAt = obj.getLong("createdAt")
                )
            )
        }

        val contacts = mutableListOf<Contact>()
        val contactsArr = root.optJSONArray("contacts") ?: JSONArray()
        for (i in 0 until contactsArr.length()) {
            val obj = contactsArr.getJSONObject(i)
            contacts.add(
                Contact(
                    id = obj.getString("id"),
                    projectId = obj.getString("projectId"),
                    name = obj.getString("name"),
                    phone = obj.optString("phone", ""),
                    email = obj.optString("email", ""),
                    role = obj.optString("role", ""),
                    position = obj.optInt("position", i)
                )
            )
        }

        val taskLinks = mutableListOf<TaskLink>()
        val linksArr = root.optJSONArray("taskLinks") ?: JSONArray()
        for (i in 0 until linksArr.length()) {
            val obj = linksArr.getJSONObject(i)
            taskLinks.add(
                TaskLink(
                    parentId = obj.getString("parentId"),
                    childId = obj.getString("childId"),
                    createdAt = obj.optLong("createdAt", 0L)
                )
            )
        }

        return KairosTransferData(
            version = version,
            exportedAt = exportedAt,
            app = app,
            projects = projects,
            boards = boards,
            categories = categories,
            columns = columns,
            tasks = tasks,
            comments = comments,
            columnComments = columnComments,
            contacts = contacts,
            taskLinks = taskLinks
        )
    }

    fun compressToBase64(jsonStr: String): String {
        val bytes = jsonStr.toByteArray(StandardCharsets.UTF_8)
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { gz ->
            gz.write(bytes)
        }
        return Base64.encodeToString(baos.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
    }

    fun decompressFromBase64(base64Str: String): String {
        val decodedBytes = Base64.decode(base64Str, Base64.URL_SAFE or Base64.NO_WRAP)
        val bais = ByteArrayInputStream(decodedBytes)
        GZIPInputStream(bais).use { gz ->
            return gz.bufferedReader(StandardCharsets.UTF_8).readText()
        }
    }

    fun createDeepLinkUri(data: KairosTransferData): String {
        val minifiedJson = toJson(data)
        val compressed = compressToBase64(minifiedJson)
        val encodedParam = URLEncoder.encode(compressed, "UTF-8")
        return "kairos://import?data=$encodedParam"
    }

    fun parseFromDeepLink(uri: Uri): KairosTransferData? {
        val rawParam = uri.getQueryParameter("data") ?: return null
        return try {
            val decodedParam = URLDecoder.decode(rawParam, "UTF-8")
            val json = decompressFromBase64(decodedParam)
            fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createSummaryText(data: KairosTransferData): String {
        val sb = StringBuilder()
        sb.append("⏳ Задачи из Kairos:\n")
        val activeTasks = data.tasks.filter { !it.isCompleted }
        sb.append("Всего активных задач: ${activeTasks.size}\n\n")

        val boardMap = data.boards.associateBy { it.id }
        val columnMap = data.columns.associateBy { it.id }

        data.tasks.take(12).forEachIndexed { idx, t ->
            val colTitle = columnMap[t.columnId]?.title ?: ""
            val status = if (t.isCompleted) "✓ " else "• "
            val quadrant = if (t.eisenhowerQuadrant != null) "[${t.eisenhowerQuadrant}] " else ""
            sb.append("$status$quadrant${t.title}")
            if (colTitle.isNotBlank()) sb.append(" ($colTitle)")
            sb.append("\n")
        }

        if (data.tasks.size > 12) {
            sb.append("... и еще ${data.tasks.size - 12} задач\n")
        }

        sb.append("\n🔗 Нажмите ссылку, чтобы импортировать в Kairos:\n")
        sb.append(createDeepLinkUri(data))
        return sb.toString()
    }
}
