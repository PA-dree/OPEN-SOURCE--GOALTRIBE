package com.functions.goaltribe

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*

class GoalDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "goaltribe.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_GOALS = "goals"

        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "goalName"
        private const val COLUMN_PROGRESS = "progress"
        private const val COLUMN_MOTIVATION = "motivation"
        private const val COLUMN_RUN_HABIT = "runHabit"
        private const val COLUMN_STRETCH_HABIT = "stretchHabit"
        private const val COLUMN_TARGET_DATE = "targetDate"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_GOALS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_NAME TEXT,
                $COLUMN_PROGRESS INTEGER,
                $COLUMN_MOTIVATION TEXT,
                $COLUMN_RUN_HABIT INTEGER,
                $COLUMN_STRETCH_HABIT INTEGER,
                $COLUMN_TARGET_DATE TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GOALS")
        onCreate(db)
    }

    // CREATE
    fun createGoal(goal: Goal): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, if (goal.id.isNotEmpty()) goal.id else UUID.randomUUID().toString())
            put(COLUMN_NAME, goal.goalName)
            put(COLUMN_PROGRESS, goal.progress)
            put(COLUMN_MOTIVATION, goal.motivation)
            put(COLUMN_RUN_HABIT, if (goal.runHabit) 1 else 0)
            put(COLUMN_STRETCH_HABIT, if (goal.stretchHabit) 1 else 0)
            put(COLUMN_TARGET_DATE, goal.targetDate)
        }

        val result = db.insert(TABLE_GOALS, null, values)
        db.close()
        return result != -1L
    }

    // READ ALL
    fun getAllGoals(): List<Goal> {
        val goals = mutableListOf<Goal>()
        val db = readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_GOALS", null)

        if (cursor.moveToFirst()) {
            do {
                val goal = Goal(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    goalName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    progress = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROGRESS)),
                    motivation = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MOTIVATION)),
                    runHabit = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RUN_HABIT)) == 1,
                    stretchHabit = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STRETCH_HABIT)) == 1,
                    targetDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TARGET_DATE))
                )
                goals.add(goal)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return goals
    }

    // READ ONE
    fun getGoalById(id: String): Goal? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_GOALS,
            null,
            "$COLUMN_ID = ?",
            arrayOf(id),
            null,
            null,
            null
        )

        var goal: Goal? = null
        if (cursor.moveToFirst()) {
            goal = Goal(
                id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                goalName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                progress = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROGRESS)),
                motivation = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MOTIVATION)),
                runHabit = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RUN_HABIT)) == 1,
                stretchHabit = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STRETCH_HABIT)) == 1,
                targetDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TARGET_DATE))
            )
        }

        cursor.close()
        db.close()
        return goal
    }

    // UPDATE
    fun updateGoal(goal: Goal): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, goal.goalName)
            put(COLUMN_PROGRESS, goal.progress)
            put(COLUMN_MOTIVATION, goal.motivation)
            put(COLUMN_RUN_HABIT, if (goal.runHabit) 1 else 0)
            put(COLUMN_STRETCH_HABIT, if (goal.stretchHabit) 1 else 0)
            put(COLUMN_TARGET_DATE, goal.targetDate)
        }

        val result = db.update(TABLE_GOALS, values, "$COLUMN_ID = ?", arrayOf(goal.id))
        db.close()
        return result > 0
    }

    // DELETE
    fun deleteGoal(id: String): Boolean {
        val db = writableDatabase
        val result = db.delete(TABLE_GOALS, "$COLUMN_ID = ?", arrayOf(id))
        db.close()
        return result > 0
    }
}
