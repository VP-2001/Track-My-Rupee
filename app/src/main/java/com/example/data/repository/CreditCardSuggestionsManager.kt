package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.CreditCardSuggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class CreditCardSuggestionsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("credit_card_suggestions_prefs", Context.MODE_PRIVATE)

    private val _suggestions = MutableStateFlow(loadSuggestions())
    val suggestions: StateFlow<List<CreditCardSuggestion>> = _suggestions.asStateFlow()

    private fun getDefaultSuggestions(): List<CreditCardSuggestion> = listOf(
        CreditCardSuggestion(name = "HDFC Regalia", issuerBank = "HDFC Bank", isRuPay = false),
        CreditCardSuggestion(name = "HDFC Tata Neu RuPay", issuerBank = "HDFC Bank", isRuPay = true),
        CreditCardSuggestion(name = "ICICI Amazon Pay", issuerBank = "ICICI Bank", isRuPay = false),
        CreditCardSuggestion(name = "ICICI Coral RuPay", issuerBank = "ICICI Bank", isRuPay = true),
        CreditCardSuggestion(name = "SBI SimplyClick", issuerBank = "State Bank of India", isRuPay = false),
        CreditCardSuggestion(name = "SBI BPCL RuPay", issuerBank = "State Bank of India", isRuPay = true),
        CreditCardSuggestion(name = "Axis Ace", issuerBank = "Axis Bank", isRuPay = false),
        CreditCardSuggestion(name = "Axis Indian Oil RuPay", issuerBank = "Axis Bank", isRuPay = true),
        CreditCardSuggestion(name = "OneCard", issuerBank = "Federal Bank", isRuPay = false),
        CreditCardSuggestion(name = "PNB RuPay Platinum", issuerBank = "Punjab National Bank", isRuPay = true)
    )

    private fun loadSuggestions(): List<CreditCardSuggestion> {
        val jsonStr = prefs.getString("saved_card_suggestions", null) ?: return getDefaultSuggestions()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<CreditCardSuggestion>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CreditCardSuggestion(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.getString("name"),
                        issuerBank = obj.optString("issuerBank", ""),
                        isRuPay = obj.optBoolean("isRuPay", false)
                    )
                )
            }
            if (list.isEmpty()) getDefaultSuggestions() else list
        } catch (e: Exception) {
            getDefaultSuggestions()
        }
    }

    private fun persist(list: List<CreditCardSuggestion>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("issuerBank", item.issuerBank)
            obj.put("isRuPay", item.isRuPay)
            array.put(obj)
        }
        prefs.edit().putString("saved_card_suggestions", array.toString()).apply()
        _suggestions.value = list
    }

    fun addSuggestion(name: String, issuerBank: String, isRuPay: Boolean = false) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        val current = _suggestions.value.toMutableList()
        current.removeAll { it.name.equals(trimmedName, ignoreCase = true) }
        current.add(0, CreditCardSuggestion(name = trimmedName, issuerBank = issuerBank.trim(), isRuPay = isRuPay))
        persist(current)
    }

    fun updateSuggestion(id: String, newName: String, newBank: String, isRuPay: Boolean) {
        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) return
        val current = _suggestions.value.map {
            if (it.id == id) it.copy(name = trimmedName, issuerBank = newBank.trim(), isRuPay = isRuPay)
            else it
        }
        persist(current)
    }

    fun deleteSuggestion(id: String) {
        val current = _suggestions.value.filterNot { it.id == id }
        persist(current)
    }

    fun resetToDefaults() {
        persist(getDefaultSuggestions())
    }
}
