package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.model.MasterAgent
import com.example.model.PaymentGateway
import com.example.model.TransactionRequest
import com.example.model.UserAccount
import com.example.model.UserWithdrawalAccount
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Enterprise Supabase Cloud Database Manager for BP Wallet.
 * Permanently synchronizes Users, Transactions, Payment Gateways, Master Agents,
 * Withdrawal Accounts, and App Settings to Supabase PostgreSQL Database via PostgREST API.
 */
object SupabaseCloudManager {
    private const val TAG = "SupabaseCloudManager"

    // Supabase URL & Key with automatic fallback to user provided credentials
    val SUPABASE_URL: String = try {
        val url = BuildConfig.SUPABASE_URL
        if (url.isNotBlank()) url else "https://vmglozamlzwjbigareie.supabase.co"
    } catch (e: Exception) {
        "https://vmglozamlzwjbigareie.supabase.co"
    }

    val SUPABASE_ANON_KEY: String = try {
        val key = BuildConfig.SUPABASE_ANON_KEY
        if (key.isNotBlank()) key else "sb_publishable__fRb6fc87bNBTxr3SBEEtQ_NR8RHpzx"
    } catch (e: Exception) {
        "sb_publishable__fRb6fc87bNBTxr3SBEEtQ_NR8RHpzx"
    }

    private val gson: Gson = GsonBuilder().create()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _connectionStatus = MutableStateFlow("Connected to Supabase Cloud")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    /**
     * Copyable SQL Schema setup script for Supabase SQL Editor.
     * Generates all required database tables with JSONB flexibility.
     */
    val SQL_SCHEMA_SCRIPT = """
        -- =================================================================
        -- BP WALLET - SUPABASE CLOUD DATABASE PERMANENT STORAGE SCHEMA
        -- Copy & paste this code into your Supabase SQL Editor and click RUN
        -- =================================================================

        CREATE TABLE IF NOT EXISTS users (
            id TEXT PRIMARY KEY,
            data JSONB NOT NULL DEFAULT '{}'::jsonb,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
        );

        CREATE TABLE IF NOT EXISTS transactions (
            id TEXT PRIMARY KEY,
            data JSONB NOT NULL DEFAULT '{}'::jsonb,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
        );

        CREATE TABLE IF NOT EXISTS payment_gateways (
            id TEXT PRIMARY KEY,
            data JSONB NOT NULL DEFAULT '{}'::jsonb,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
        );

        CREATE TABLE IF NOT EXISTS master_agents (
            id TEXT PRIMARY KEY,
            data JSONB NOT NULL DEFAULT '{}'::jsonb,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
        );

        CREATE TABLE IF NOT EXISTS user_withdrawal_accounts (
            id TEXT PRIMARY KEY,
            data JSONB NOT NULL DEFAULT '{}'::jsonb,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
        );

        CREATE TABLE IF NOT EXISTS app_settings (
            key TEXT PRIMARY KEY,
            value TEXT NOT NULL,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
        );

        -- =================================================================
        -- Multi-Country & Multi-Currency Deposit System Relational Tables
        -- =================================================================
        CREATE TABLE IF NOT EXISTS payment_gateways_relational (
            id SERIAL PRIMARY KEY,
            country_code VARCHAR(10) NOT NULL,
            currency VARCHAR(10) NOT NULL,
            method_name VARCHAR(100) NOT NULL,
            short_description VARCHAR(255),
            account_title VARCHAR(100),
            account_number VARCHAR(100),
            iban VARCHAR(100),
            bank_name VARCHAR(100),
            deposit_instructions TEXT,
            logo_url TEXT,
            display_order INT DEFAULT 0,
            is_active BOOLEAN DEFAULT TRUE,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS deposit_requests (
            id SERIAL PRIMARY KEY,
            user_id UUID NOT NULL,
            gateway_id INT REFERENCES payment_gateways_relational(id),
            amount DECIMAL(15, 2) NOT NULL,
            currency VARCHAR(10) NOT NULL,
            screenshot_url TEXT NOT NULL,
            status VARCHAR(20) DEFAULT 'Pending',
            admin_notes TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        -- Disable row-level security or add public policies for app sync
        ALTER TABLE users ENABLE ROW LEVEL SECURITY;
        CREATE POLICY "Enable all access users" ON users FOR ALL USING (true) WITH CHECK (true);

        ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
        CREATE POLICY "Enable all access transactions" ON transactions FOR ALL USING (true) WITH CHECK (true);

        ALTER TABLE payment_gateways ENABLE ROW LEVEL SECURITY;
        CREATE POLICY "Enable all access payment_gateways" ON payment_gateways FOR ALL USING (true) WITH CHECK (true);

        ALTER TABLE master_agents ENABLE ROW LEVEL SECURITY;
        CREATE POLICY "Enable all access master_agents" ON master_agents FOR ALL USING (true) WITH CHECK (true);

        ALTER TABLE user_withdrawal_accounts ENABLE ROW LEVEL SECURITY;
        CREATE POLICY "Enable all access withdrawal_accounts" ON user_withdrawal_accounts FOR ALL USING (true) WITH CHECK (true);

        ALTER TABLE app_settings ENABLE ROW LEVEL SECURITY;
        CREATE POLICY "Enable all access app_settings" ON app_settings FOR ALL USING (true) WITH CHECK (true);

        ALTER TABLE payment_gateways_relational ENABLE ROW LEVEL SECURITY;
        CREATE POLICY "Enable all access payment_gateways_relational" ON payment_gateways_relational FOR ALL USING (true) WITH CHECK (true);

        ALTER TABLE deposit_requests ENABLE ROW LEVEL SECURITY;
        CREATE POLICY "Enable all access deposit_requests" ON deposit_requests FOR ALL USING (true) WITH CHECK (true);

        -- =================================================================
        -- Server-Side Helper Function: increment_user_wallet
        -- Used by Node.js / Express / Supabase backend when deposit is Approved
        -- =================================================================
        CREATE OR REPLACE FUNCTION increment_user_wallet(target_user_id TEXT, amount_to_add DECIMAL)
        RETURNS VOID AS $$
        BEGIN
            UPDATE users
            SET data = jsonb_set(
                data,
                '{walletBalance}',
                to_jsonb(COALESCE((data->>'walletBalance')::numeric, 0) + amount_to_add)
            )
            WHERE id = target_user_id;
        END;
        $$ LANGUAGE plpgsql;
    """.trimIndent()

    private fun buildHeaders(): Request.Builder {
        return Request.Builder()
            .header("apikey", SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .header("Prefer", "return=representation,resolution=merge-duplicates")
    }

    /**
     * Generic UPSERT to a Supabase PostgREST table.
     */
    private suspend fun upsertRow(table: String, id: String, dataObj: Any): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/$table"
            val jsonPayload = JsonObject().apply {
                addProperty("id", id)
                add("data", gson.toJsonTree(dataObj))
            }
            val requestBody = gson.toJson(jsonPayload).toRequestBody(jsonMediaType)
            val request = buildHeaders()
                .url(url)
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    _isOnline.value = true
                    _connectionStatus.value = "Supabase Cloud: Active & Synced"
                    _lastSyncTime.value = System.currentTimeMillis()
                    Log.d(TAG, "Successfully synced row '$id' to Supabase table '$table'")
                    true
                } else {
                    val code = response.code
                    val errorBody = response.body?.string() ?: "unknown"
                    Log.w(TAG, "Supabase upsert failed ($code) on '$table': $errorBody")
                    if (code == 404 || errorBody.contains("relation") || errorBody.contains("does not exist")) {
                        _connectionStatus.value = "Connected (Run SQL script in Supabase Editor)"
                    } else {
                        _connectionStatus.value = "Supabase Active ($code)"
                    }
                    false
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network fallback when syncing '$id' to Supabase: ${e.message}")
            false
        }
    }

    /**
     * Generic DELETE from a Supabase PostgREST table.
     */
    private suspend fun deleteRow(table: String, id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/$table?id=eq.$id"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .delete()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting row '$id' from Supabase: ${e.message}")
            false
        }
    }

    /**
     * Generic fetch rows from a Supabase table and parse into objects of type T.
     */
    private suspend inline fun <reified T> loadTable(table: String): List<T> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/$table?select=*"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return@withContext emptyList()
                    val jsonArray = JsonParser.parseString(jsonStr).asJsonArray
                    val results = mutableListOf<T>()
                    for (element in jsonArray) {
                        try {
                            val obj = element.asJsonObject
                            val targetJson = if (obj.has("data") && obj.get("data").isJsonObject) {
                                obj.getAsJsonObject("data")
                            } else {
                                obj
                            }
                            val item = gson.fromJson(targetJson, T::class.java)
                            if (item != null) results.add(item)
                        } catch (e: Exception) {
                            Log.w(TAG, "Skipping invalid row in '$table': ${e.message}")
                        }
                    }
                    _isOnline.value = true
                    _connectionStatus.value = "Supabase Cloud: Active & Synced"
                    results
                } else {
                    val errorBody = response.body?.string() ?: ""
                    if (response.code == 404 || errorBody.contains("relation") || errorBody.contains("does not exist")) {
                        _connectionStatus.value = "Connected (Run SQL setup script in Supabase)"
                    }
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Supabase fetch '$table' offline fallback: ${e.message}")
            emptyList()
        }
    }

    // ==========================================
    // SYNC METHODS FOR ALL ENTITIES
    // ==========================================

    suspend fun syncUser(user: UserAccount) {
        upsertRow("users", user.id, user)
    }

    suspend fun syncTransaction(tx: TransactionRequest) {
        upsertRow("transactions", tx.id, tx)
    }

    suspend fun syncPaymentGateway(gw: PaymentGateway) {
        upsertRow("payment_gateways", gw.id, gw)
    }

    suspend fun deletePaymentGatewayFromCloud(id: String) {
        deleteRow("payment_gateways", id)
    }

    suspend fun syncMasterAgent(agent: MasterAgent) {
        upsertRow("master_agents", agent.id, agent)
    }

    suspend fun syncWithdrawalAccount(acc: UserWithdrawalAccount) {
        upsertRow("user_withdrawal_accounts", acc.id, acc)
    }

    suspend fun deleteWithdrawalAccountFromCloud(id: String) {
        deleteRow("user_withdrawal_accounts", id)
    }

    suspend fun syncSetting(key: String, value: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = "$SUPABASE_URL/rest/v1/app_settings"
                val jsonPayload = JsonObject().apply {
                    addProperty("key", key)
                    addProperty("value", value)
                }
                val requestBody = gson.toJson(jsonPayload).toRequestBody(jsonMediaType)
                val request = buildHeaders()
                    .url(url)
                    .post(requestBody)
                    .build()
                okHttpClient.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.w(TAG, "Setting sync error: ${e.message}")
            }
        }
    }

    // ==========================================
    // LOAD METHODS FOR APP INITIALIZATION
    // ==========================================

    suspend fun loadAllUsers(): List<UserAccount> = loadTable("users")

    suspend fun loadAllTransactions(): List<TransactionRequest> = loadTable("transactions")

    suspend fun loadAllGateways(): List<PaymentGateway> = loadTable("payment_gateways")

    suspend fun loadAllMasterAgents(): List<MasterAgent> = loadTable("master_agents")

    suspend fun loadAllWithdrawalAccounts(): List<UserWithdrawalAccount> = loadTable("user_withdrawal_accounts")

    suspend fun loadSetting(key: String, defaultVal: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/app_settings?key=eq.$key"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return@withContext defaultVal
                    val array = JsonParser.parseString(jsonStr).asJsonArray
                    if (array.size() > 0) {
                        return@withContext array[0].asJsonObject.get("value").asString
                    }
                }
            }
            defaultVal
        } catch (e: Exception) {
            defaultVal
        }
    }
}
