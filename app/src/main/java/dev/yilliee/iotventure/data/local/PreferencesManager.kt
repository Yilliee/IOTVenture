package dev.yilliee.iotventure.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dev.yilliee.iotventure.data.model.TeamMessage
import dev.yilliee.iotventure.data.model.Challenge
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PreferencesManager(context: Context) {

    companion object {
        private const val TAG = "PreferencesManager"
        private const val PREFS_NAME = "iotventure_prefs"
        private const val KEY_DEVICE_TOKEN = "device_token"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_TEAM_NAME = "team_name"
        private const val KEY_TEAM_PASSWORD = "team_password"
        private const val KEY_MESSAGES = "team_messages"
        private const val KEY_LAST_MESSAGE_FETCH = "last_message_fetch"
        private const val KEY_SERVER_IP = "server_ip"
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_CHALLENGES = "challenges"
        private const val KEY_SOLVED_CHALLENGES = "solved_challenges"
        private const val KEY_SOLVE_QUEUE = "solve_queue"
        private const val KEY_GAME_START_TIME = "game_start_time"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // Login data
    fun saveDeviceToken(token: String) {
        prefs.edit().putString(KEY_DEVICE_TOKEN, token).apply()
    }

    fun getDeviceToken(): String? {
        return prefs.getString(KEY_DEVICE_TOKEN, null)
    }

    fun saveUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }

    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    fun savePassword(password: String) {
        prefs.edit().putString(KEY_PASSWORD, password).apply()
    }

    fun getPassword(): String? {
        return prefs.getString(KEY_PASSWORD, null)
    }

    fun saveTeamName(teamName: String) {
        prefs.edit().putString(KEY_TEAM_NAME, teamName).apply()
    }

    fun getTeamName(): String? {
        return prefs.getString(KEY_TEAM_NAME, null)
    }

    fun saveTeamPassword(teamPassword: String) {
        prefs.edit().putString(KEY_TEAM_PASSWORD, teamPassword).apply()
    }

    fun getTeamPassword(): String? {
        return prefs.getString(KEY_TEAM_PASSWORD, null)
    }

    // Team messages
    fun saveTeamMessages(messages: List<TeamMessage>) {
        val messagesJson = json.encodeToString(messages)
        prefs.edit().putString(KEY_MESSAGES, messagesJson).apply()
    }

    fun getTeamMessages(): List<TeamMessage> {
        val messagesJson = prefs.getString(KEY_MESSAGES, null) ?: return emptyList()
        return try {
            json.decodeFromString(messagesJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveLastMessageFetchTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_MESSAGE_FETCH, time).apply()
    }

    fun getLastMessageFetchTime(): Long {
        return prefs.getLong(KEY_LAST_MESSAGE_FETCH, 0)
    }

    // Challenges
    fun saveChallenges(challenges: List<Challenge>) {
        try {
            val jsonString = json.encodeToString(challenges)
            prefs.edit().putString(KEY_CHALLENGES, jsonString).apply()
            Log.d(TAG, "Saved ${challenges.size} challenges to preferences")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving challenges", e)
        }
    }

    fun getChallenges(): List<Challenge> {
        return try {
            val jsonString = prefs.getString(KEY_CHALLENGES, "[]") ?: "[]"
            json.decodeFromString<List<Challenge>>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting challenges", e)
            emptyList()
        }
    }

    // Solve queue
    fun setSolveQueue(challenges: List<Challenge>) {
        try {
            val jsonString = json.encodeToString(challenges)
            prefs.edit().putString(KEY_SOLVE_QUEUE, jsonString).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving solve queue", e)
        }
    }

    fun getSolveQueue(): List<Challenge> {
        return try {
            val jsonString = prefs.getString(KEY_SOLVE_QUEUE, "[]") ?: "[]"
            json.decodeFromString<List<Challenge>>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting solve queue", e)
            emptyList()
        }
    }

    // Solved challenges
    fun getSolvedChallenges(): Set<Int> {
        return prefs.getStringSet(KEY_SOLVED_CHALLENGES, emptySet())?.map { it.toInt() }?.toSet() ?: emptySet()
    }

    fun setSolvedChallenges(challengeIds: List<Int>) {
        prefs.edit().putStringSet(KEY_SOLVED_CHALLENGES, challengeIds.map { it.toString() }.toSet()).apply()
    }

    // Game start time
    fun setGameStartTime(time: Long) {
        prefs.edit().putLong(KEY_GAME_START_TIME, time).apply()
    }

    fun getGameStartTime(): Long {
        return prefs.getLong(KEY_GAME_START_TIME, 0)
    }

    // Server settings
    fun saveServerSettings(ip: String, port: String) {
        prefs.edit()
            .putString(KEY_SERVER_IP, ip)
            .putString(KEY_SERVER_PORT, port)
            .apply()
    }

    fun getServerIp(): String {
        return prefs.getString(KEY_SERVER_IP, "192.168.100.18") ?: "192.168.100.18"
    }

    fun getServerPort(): String {
        return prefs.getString(KEY_SERVER_PORT, "3000") ?: "3000"
    }

    // Clear all data on logout
    fun clearAllData() {
        // Save current server settings
        val currentIp = getServerIp()
        val currentPort = getServerPort()
        
        // Clear all preferences
        prefs.edit().clear().apply()
        
        // Restore server settings
        saveServerSettings(currentIp, currentPort)
    }
}
