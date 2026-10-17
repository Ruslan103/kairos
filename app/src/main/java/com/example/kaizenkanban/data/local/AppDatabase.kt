package com.example.kaizenkanban.data.local

import android.content.Context
import androidx.room.Database
import com.example.kaizenkanban.domain.model.linesFromNoteBody
import java.util.UUID
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProjectEntity::class,
        BoardEntity::class,
        CategoryEntity::class,
        ColumnEntity::class,
        TaskEntity::class,
        CommentEntity::class,
        ColumnCommentEntity::class,
        ContactEntity::class,
        RecurringTemplateEntity::class,
        TaskLinkEntity::class,
        TaskAttachmentEntity::class,
        StatsJournalEntity::class,
        KairosReminderEntity::class,
        ProjectNoteEntity::class,
        NoteLineEntity::class,
        NotePhotoEntity::class
    ],
    version = 45,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun kanbanDao(): KanbanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN completedAt INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS comments (
                        id TEXT NOT NULL PRIMARY KEY,
                        taskId TEXT NOT NULL,
                        text TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN eisenhowerQuadrant TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN showEisenhowerButtons INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS column_comments (
                        id TEXT NOT NULL PRIMARY KEY,
                        columnId TEXT NOT NULL,
                        text TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN isHidden INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN linkedColumnIds TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS contacts (
                        id TEXT NOT NULL PRIMARY KEY,
                        projectId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        email TEXT NOT NULL,
                        role TEXT NOT NULL,
                        position INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE boards ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN repeatRule TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderMinutesOfDay INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN recurringTemplateId TEXT DEFAULT NULL")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recurring_templates (
                        id TEXT NOT NULL PRIMARY KEY,
                        projectId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        rhythm TEXT NOT NULL,
                        timesPerWeek INTEGER,
                        weekdays TEXT NOT NULL,
                        targetBoardId TEXT NOT NULL,
                        targetColumnId TEXT NOT NULL,
                        enabled INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE recurring_templates ADD COLUMN reminderMinutesOfDay INTEGER DEFAULT NULL"
                )
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE recurring_templates ADD COLUMN reminderTimesOfDay TEXT NOT NULL DEFAULT '540'"
                )
                // Copy legacy single time into the multi-time column when present.
                db.execSQL(
                    """
                    UPDATE recurring_templates
                    SET reminderTimesOfDay = CAST(reminderMinutesOfDay AS TEXT)
                    WHERE reminderMinutesOfDay IS NOT NULL
                    """.trimIndent()
                )
            }
        }

        /**
         * Drop legacy reminderMinutesOfDay so the table matches [RecurringTemplateEntity].
         * Without this, Room schema validation crashes on open after 18→19.
         */
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val hasMinutes = tableHasColumn(db, "recurring_templates", "reminderMinutesOfDay")
                val hasTimes = tableHasColumn(db, "recurring_templates", "reminderTimesOfDay")
                if (!hasMinutes && hasTimes) {
                    // Already matches entity (fresh v19 install) — nothing to rebuild.
                    return
                }
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recurring_templates_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        projectId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        rhythm TEXT NOT NULL,
                        timesPerWeek INTEGER,
                        weekdays TEXT NOT NULL,
                        targetBoardId TEXT NOT NULL,
                        targetColumnId TEXT NOT NULL,
                        enabled INTEGER NOT NULL,
                        reminderTimesOfDay TEXT NOT NULL DEFAULT '540',
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                val timesExpr = when {
                    hasTimes && hasMinutes -> """
                        CASE
                            WHEN reminderTimesOfDay IS NOT NULL AND trim(reminderTimesOfDay) != ''
                                THEN reminderTimesOfDay
                            WHEN reminderMinutesOfDay IS NOT NULL
                                THEN CAST(reminderMinutesOfDay AS TEXT)
                            ELSE '540'
                        END
                    """.trimIndent()
                    hasTimes -> """
                        CASE
                            WHEN reminderTimesOfDay IS NOT NULL AND trim(reminderTimesOfDay) != ''
                                THEN reminderTimesOfDay
                            ELSE '540'
                        END
                    """.trimIndent()
                    hasMinutes -> """
                        CASE
                            WHEN reminderMinutesOfDay IS NOT NULL
                                THEN CAST(reminderMinutesOfDay AS TEXT)
                            ELSE '540'
                        END
                    """.trimIndent()
                    else -> "'540'"
                }
                db.execSQL(
                    """
                    INSERT INTO recurring_templates_new (
                        id, projectId, title, rhythm, timesPerWeek, weekdays,
                        targetBoardId, targetColumnId, enabled, reminderTimesOfDay, createdAt
                    )
                    SELECT
                        id, projectId, title, rhythm, timesPerWeek, weekdays,
                        targetBoardId, targetColumnId, enabled,
                        $timesExpr,
                        createdAt
                    FROM recurring_templates
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE recurring_templates")
                db.execSQL("ALTER TABLE recurring_templates_new RENAME TO recurring_templates")
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE recurring_templates ADD COLUMN eisenhowerQuadrant TEXT DEFAULT NULL"
                )
                db.execSQL(
                    "ALTER TABLE recurring_templates ADD COLUMN showEisenhowerButtons INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN complexity INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE tasks ADD COLUMN estimatedMinutes INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE tasks ADD COLUMN completionQuality INTEGER DEFAULT NULL")
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN workflowStatus TEXT NOT NULL DEFAULT 'open'"
                )
                db.execSQL(
                    """
                    UPDATE tasks
                    SET workflowStatus = CASE
                        WHEN isCompleted = 1 THEN 'done'
                        ELSE 'open'
                    END
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_links (
                        parentId TEXT NOT NULL,
                        childId TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(parentId, childId)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE recurring_templates ADD COLUMN linkParentIds TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE recurring_templates ADD COLUMN linkChildIds TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableHasColumn(db, "tasks", "isBoardArchived")) {
                    db.execSQL(
                        "ALTER TABLE tasks ADD COLUMN isBoardArchived INTEGER NOT NULL DEFAULT 0"
                    )
                }
                if (!tableHasColumn(db, "tasks", "statsExcluded")) {
                    db.execSQL(
                        "ALTER TABLE tasks ADD COLUMN statsExcluded INTEGER NOT NULL DEFAULT 0"
                    )
                }
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS stats_journal (
                        id TEXT NOT NULL PRIMARY KEY,
                        projectId TEXT NOT NULL,
                        sourceTaskId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        kind TEXT NOT NULL,
                        eventAt INTEGER NOT NULL,
                        complexity INTEGER,
                        completionQuality INTEGER,
                        eisenhowerQuadrant TEXT,
                        weight INTEGER NOT NULL,
                        leafScore REAL NOT NULL,
                        towardGoal INTEGER NOT NULL,
                        recurringTemplateId TEXT,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableHasColumn(db, "tasks", "isGoal")) {
                    db.execSQL(
                        "ALTER TABLE tasks ADD COLUMN isGoal INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }
        }

        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableHasColumn(db, "recurring_templates", "complexity")) {
                    db.execSQL(
                        "ALTER TABLE recurring_templates ADD COLUMN complexity INTEGER DEFAULT NULL"
                    )
                }
            }
        }

        private val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableHasColumn(db, "tasks", "goalStatsEpochMillis")) {
                    db.execSQL(
                        "ALTER TABLE tasks ADD COLUMN goalStatsEpochMillis INTEGER DEFAULT NULL"
                    )
                }
            }
        }

        private val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableHasColumn(db, "stats_journal", "relatedGoalIds")) {
                    db.execSQL(
                        "ALTER TABLE stats_journal ADD COLUMN relatedGoalIds TEXT NOT NULL DEFAULT ''"
                    )
                }
            }
        }

        private val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_attachments (
                        id TEXT NOT NULL PRIMARY KEY,
                        taskId TEXT NOT NULL,
                        relativePath TEXT NOT NULL,
                        mimeType TEXT NOT NULL DEFAULT 'image/jpeg',
                        createdAt INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableHasColumn(db, "recurring_templates", "skippedOccurrenceKeys")) {
                    db.execSQL(
                        "ALTER TABLE recurring_templates ADD COLUMN skippedOccurrenceKeys TEXT NOT NULL DEFAULT ''"
                    )
                }
            }
        }

        private val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableHasColumn(db, "tasks", "hubGroupId")) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN hubGroupId TEXT DEFAULT NULL")
                }
            }
        }

        private val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableHasColumn(db, "recurring_templates", "startsOnDayMillis")) {
                    db.execSQL(
                        "ALTER TABLE recurring_templates ADD COLUMN startsOnDayMillis INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }
        }

        private val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableHasColumn(db, "recurring_templates", "remindLeadAmount")) {
                    db.execSQL(
                        "ALTER TABLE recurring_templates ADD COLUMN remindLeadAmount INTEGER DEFAULT NULL"
                    )
                }
                if (!tableHasColumn(db, "recurring_templates", "remindLeadUnit")) {
                    db.execSQL(
                        "ALTER TABLE recurring_templates ADD COLUMN remindLeadUnit TEXT DEFAULT NULL"
                    )
                }
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS kairos_reminders (
                        id TEXT NOT NULL PRIMARY KEY,
                        projectId TEXT NOT NULL,
                        text TEXT NOT NULL,
                        triggerAtMillis INTEGER NOT NULL,
                        taskId TEXT,
                        recurringTemplateId TEXT,
                        leadAmount INTEGER,
                        leadUnit TEXT,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableHasColumn(db, "tasks", "startMinutesOfDay")) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN startMinutesOfDay INTEGER DEFAULT NULL")
                }
                if (!tableHasColumn(db, "tasks", "deadlineAt")) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN deadlineAt INTEGER DEFAULT NULL")
                }
            }
        }

        private val MIGRATION_35_36 = object : Migration(35, 36) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS project_notes (
                        id TEXT NOT NULL PRIMARY KEY,
                        projectId TEXT NOT NULL,
                        body TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        pinned INTEGER NOT NULL,
                        linkedTaskId TEXT,
                        reminderAtMillis INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS note_photos (
                        id TEXT NOT NULL PRIMARY KEY,
                        noteId TEXT NOT NULL,
                        mediaUri TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_36_37 = object : Migration(36, 37) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableHasColumn(db, "recurring_templates", "deadlineAtMillis")) {
                    db.execSQL(
                        "ALTER TABLE recurring_templates ADD COLUMN deadlineAtMillis INTEGER DEFAULT NULL"
                    )
                }
            }
        }

        /**
         * Replace absolute deadlineAtMillis with offset columns and drop the old column.
         * Room rejects the database when a column is left behind, and that used to wipe the file.
         */
        private val MIGRATION_37_38 = object : Migration(37, 38) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_templates_new` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `rhythm` TEXT NOT NULL,
                        `timesPerWeek` INTEGER,
                        `weekdays` TEXT NOT NULL,
                        `targetBoardId` TEXT NOT NULL,
                        `targetColumnId` TEXT NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `reminderTimesOfDay` TEXT NOT NULL,
                        `eisenhowerQuadrant` TEXT,
                        `showEisenhowerButtons` INTEGER NOT NULL,
                        `linkParentIds` TEXT NOT NULL,
                        `linkChildIds` TEXT NOT NULL,
                        `complexity` INTEGER,
                        `skippedOccurrenceKeys` TEXT NOT NULL,
                        `startsOnDayMillis` INTEGER NOT NULL,
                        `deadlineOffsetDays` INTEGER,
                        `deadlineOffsetHours` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `remindLeadAmount` INTEGER,
                        `remindLeadUnit` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `recurring_templates_new` (
                        id, projectId, title, rhythm, timesPerWeek, weekdays,
                        targetBoardId, targetColumnId, enabled, reminderTimesOfDay,
                        eisenhowerQuadrant, showEisenhowerButtons, linkParentIds, linkChildIds,
                        complexity, skippedOccurrenceKeys, startsOnDayMillis,
                        deadlineOffsetDays, deadlineOffsetHours, createdAt,
                        remindLeadAmount, remindLeadUnit
                    )
                    SELECT
                        id, projectId, title, rhythm, timesPerWeek, weekdays,
                        targetBoardId, targetColumnId, enabled, reminderTimesOfDay,
                        eisenhowerQuadrant, showEisenhowerButtons, linkParentIds, linkChildIds,
                        complexity, skippedOccurrenceKeys, startsOnDayMillis,
                        NULL, NULL, createdAt,
                        remindLeadAmount, remindLeadUnit
                    FROM recurring_templates
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE recurring_templates")
                db.execSQL("ALTER TABLE recurring_templates_new RENAME TO recurring_templates")
            }
        }

        private val MIGRATION_38_39 = object : Migration(38, 39) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableHasColumn(db, "tasks", "timerMinutes")) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN timerMinutes INTEGER DEFAULT NULL")
                }
                if (!tableHasColumn(db, "recurring_templates", "timerMinutes")) {
                    db.execSQL("ALTER TABLE recurring_templates ADD COLUMN timerMinutes INTEGER DEFAULT NULL")
                }
            }
        }

        private val MIGRATION_39_40 = object : Migration(39, 40) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableHasColumn(db, "recurring_templates", "hubGroupId")) {
                    db.execSQL("ALTER TABLE recurring_templates ADD COLUMN hubGroupId TEXT DEFAULT NULL")
                }
            }
        }

        private val MIGRATION_40_41 = object : Migration(40, 41) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableHasColumn(db, "project_notes", "title")) {
                    db.execSQL("ALTER TABLE project_notes ADD COLUMN title TEXT NOT NULL DEFAULT ''")
                }
            }
        }

        private val MIGRATION_41_42 = object : Migration(41, 42) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `note_lines` (
                        `id` TEXT NOT NULL,
                        `noteId` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                if (!tableHasColumn(db, "note_photos", "lineId")) {
                    db.execSQL("ALTER TABLE note_photos ADD COLUMN lineId TEXT NOT NULL DEFAULT ''")
                }
                val pendingLines = mutableListOf<com.example.kaizenkanban.domain.model.NoteLine>()
                db.query("SELECT id, body, createdAt FROM project_notes").use { notes ->
                    val idIdx = notes.getColumnIndex("id")
                    val bodyIdx = notes.getColumnIndex("body")
                    val createdIdx = notes.getColumnIndex("createdAt")
                    while (notes.moveToNext()) {
                        pendingLines += linesFromNoteBody(
                            notes.getString(idIdx),
                            notes.getString(bodyIdx) ?: "",
                            notes.getLong(createdIdx)
                        )
                    }
                }
                pendingLines.forEach { line ->
                    db.execSQL(
                        "INSERT INTO note_lines (id, noteId, text, createdAt, position) VALUES (?, ?, ?, ?, ?)",
                        arrayOf<Any>(line.id, line.noteId, line.text, line.createdAt, line.position)
                    )
                }
                val unboundPhotos = mutableListOf<Pair<String, String>>()
                db.query("SELECT id, noteId FROM note_photos WHERE lineId = ''").use { photos ->
                    val idIdx = photos.getColumnIndex("id")
                    val noteIdx = photos.getColumnIndex("noteId")
                    while (photos.moveToNext()) {
                        unboundPhotos += photos.getString(idIdx) to photos.getString(noteIdx)
                    }
                }
                unboundPhotos.forEach { (photoId, noteId) ->
                    var lineId: String? = null
                    db.query(
                        "SELECT id FROM note_lines WHERE noteId = ? ORDER BY position ASC LIMIT 1",
                        arrayOf(noteId)
                    ).use { lines ->
                        if (lines.moveToFirst()) lineId = lines.getString(0)
                    }
                    if (lineId == null) {
                        lineId = UUID.randomUUID().toString()
                        db.execSQL(
                            "INSERT INTO note_lines (id, noteId, text, createdAt, position) VALUES (?, ?, ?, ?, ?)",
                            arrayOf<Any>(lineId, noteId, "", System.currentTimeMillis(), 0)
                        )
                    }
                    db.execSQL(
                        "UPDATE note_photos SET lineId = ? WHERE id = ?",
                        arrayOf(lineId, photoId)
                    )
                }
            }
        }

        private val MIGRATION_42_43 = object : Migration(42, 43) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableHasColumn(db, "tasks", "cardType")) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN cardType TEXT NOT NULL DEFAULT 'plain'")
                }
                if (!tableHasColumn(db, "tasks", "cardPayload")) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN cardPayload TEXT NOT NULL DEFAULT ''")
                }
                if (!tableHasColumn(db, "recurring_templates", "cardType")) {
                    db.execSQL(
                        "ALTER TABLE recurring_templates ADD COLUMN cardType TEXT NOT NULL DEFAULT 'plain'"
                    )
                }
                if (!tableHasColumn(db, "recurring_templates", "cardPayload")) {
                    db.execSQL(
                        "ALTER TABLE recurring_templates ADD COLUMN cardPayload TEXT NOT NULL DEFAULT ''"
                    )
                }
            }
        }

        private val MIGRATION_43_44 = object : Migration(43, 44) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableHasColumn(db, "contacts", "social")) {
                    db.execSQL("ALTER TABLE contacts ADD COLUMN social TEXT NOT NULL DEFAULT ''")
                }
            }
        }

        private val MIGRATION_44_45 = object : Migration(44, 45) {
            override fun migrate(db: SupportSQLiteDatabase) {
                fun add(column: String, ddl: String) {
                    if (!tableHasColumn(db, "stats_journal", column)) {
                        db.execSQL("ALTER TABLE stats_journal ADD COLUMN $ddl")
                    }
                }
                add("parentIds", "parentIds TEXT NOT NULL DEFAULT ''")
                add("childIds", "childIds TEXT NOT NULL DEFAULT ''")
                add("isGoal", "isGoal INTEGER NOT NULL DEFAULT 0")
                add("goalStatsEpochMillis", "goalStatsEpochMillis INTEGER")
                add("dueDate", "dueDate INTEGER")
                add("deadlineAt", "deadlineAt INTEGER")
                add("taskCreatedAt", "taskCreatedAt INTEGER NOT NULL DEFAULT 0")
                add("replacesTask", "replacesTask INTEGER NOT NULL DEFAULT 0")
            }
        }

        private fun tableHasColumn(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
            db.query("PRAGMA table_info(`$table`)").use { cursor ->
                val nameIdx = cursor.getColumnIndex("name")
                if (nameIdx < 0) return false
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIdx) == column) return true
                }
            }
            return false
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = openDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        private fun openDatabase(context: Context): AppDatabase {
            val db = buildDatabase(context)
            // Force open now so migration / schema errors surface here, not mid-UI.
            db.openHelper.writableDatabase
            return db
        }

        private fun buildDatabase(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "kaizen_kanban_db"
            )
                .addMigrations(
                    MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                    MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
                    MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
                    MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21,
                    MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25,
                    MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29,
                    MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33,
                    MIGRATION_33_34, MIGRATION_34_35, MIGRATION_35_36, MIGRATION_36_37, MIGRATION_37_38,
                    MIGRATION_38_39, MIGRATION_39_40, MIGRATION_40_41, MIGRATION_41_42,
                    MIGRATION_42_43, MIGRATION_43_44, MIGRATION_44_45
                )
                .build()
    }
}
