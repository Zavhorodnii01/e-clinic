package com.example.e_clinic.Services

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import androidx.core.content.edit

class PinManager(context: Context) {
    private val sharedPreferences by lazy{
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        EncryptedSharedPreferences.create(
            "secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun savePin(pin: String) {
        sharedPreferences.edit { putString("pin", pin) }
    }

    fun getPin(): String? {
        return sharedPreferences.getString("pin", null)
    }

    fun validatePin(inputPin: String): Boolean {
        val savedPin = getPin()
        return savedPin != null && savedPin == inputPin
    }

    fun clearPin() {
        sharedPreferences.edit { remove("pin") }
    }


}