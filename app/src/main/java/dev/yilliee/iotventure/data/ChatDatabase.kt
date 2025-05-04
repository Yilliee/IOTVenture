package dev.yilliee.iotventure.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.Date

class ChatDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "chat_database"
        private const val DATABASE_VERSION = 1
        private const val TABLE_MESSAGES = "messages"
        private const val COLUMN_ID = "id"
        private const val COLUMN_HUNT_ID = "hunt_id"
        private const val COLUMN_SENDER = "sender"
        private const val COLUMN_MESSAGE = "message"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_IS_MINE = "is_mine"
        private const val COLUMN_STATUS = "status"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_MESSAGES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_HUNT_ID TEXT NOT NULL,
                $COLUMN_SENDER TEXT NOT NULL,
                $COLUMN_MESSAGE TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_IS_MINE INTEGER NOT NULL,
                $COLUMN_STATUS TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        onCreate(db)
    }

    suspend fun insertMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COLUMN_HUNT_ID, message.huntId)
            put(COLUMN_SENDER, message.sender)
            put(COLUMN_MESSAGE, message.message)
            put(COLUMN_TIMESTAMP, message.timestamp.time)
            put(COLUMN_IS_MINE, if (message.isMine) 1 else 0)
            put(COLUMN_STATUS, message.status.name)
        }
        writableDatabase.insert(TABLE_MESSAGES, null, values)
    }

    fun getMessagesForHunt(huntId: String): Flow<List<ChatMessage>> = flow {
        val messages = mutableListOf<ChatMessage>()
        val cursor = readableDatabase.query(
            TABLE_MESSAGES,
            null,
            "$COLUMN_HUNT_ID = ?",
            arrayOf(huntId),
            null,
            null,
            "$COLUMN_TIMESTAMP ASC"
        )

        with(cursor) {
            while (moveToNext()) {
                val message = ChatMessage(
                    id = getLong(getColumnIndexOrThrow(COLUMN_ID)),
                    huntId = getString(getColumnIndexOrThrow(COLUMN_HUNT_ID)),
                    sender = getString(getColumnIndexOrThrow(COLUMN_SENDER)),
                    message = getString(getColumnIndexOrThrow(COLUMN_MESSAGE)),
                    timestamp = Date(getLong(getColumnIndexOrThrow(COLUMN_TIMESTAMP))),
                    isMine = getInt(getColumnIndexOrThrow(COLUMN_IS_MINE)) == 1,
                    status = MessageStatus.valueOf(getString(getColumnIndexOrThrow(COLUMN_STATUS)))
                )
                messages.add(message)
            }
            close()
        }
        emit(messages)
    }

    suspend fun deleteMessagesForHunt(huntId: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete(
            TABLE_MESSAGES,
            "$COLUMN_HUNT_ID = ?",
            arrayOf(huntId)
        )
    }

    suspend fun deleteAllMessages() = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_MESSAGES, null, null)
    }
} 