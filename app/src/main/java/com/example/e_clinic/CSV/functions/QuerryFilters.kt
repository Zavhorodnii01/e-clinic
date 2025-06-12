package com.example.e_clinic.CSV.functions

import android.content.Context
import com.example.e_clinic.CSV.collections.Drug
import java.io.BufferedReader
import java.io.InputStreamReader

object QuerryFilters {
    fun loadDrugsFromCSV(context: Context, fileName: String = "medication_list.csv"): List<Drug> {
        val drugs = mutableListOf<Drug>()
        try {
            val inputStream = context.assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine() // Skip header
            reader.forEachLine { line ->
                val tokens = line.split(",")
                if (tokens.size >= 5) {
                    drugs.add(
                        Drug(
                            name = tokens[0].trim(),
                            activeSubstance = tokens[1].trim(),
                            amountOfSubstance = tokens[2].trim(),
                            form = tokens[3].trim(),
                            typeOfPrescription = tokens[4].trim()
                        )
                    )
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return drugs
    }

    fun filterDrugsByNamePrefix(drugs: List<Drug>, prefix: String): List<Drug> {
        return drugs.filter { it.name.startsWith(prefix, ignoreCase = true) }
    }

    fun filterDrugsByActiveSubstancePrefixAndName(
        drugs: List<Drug>,
        activeSubstancePrefix: String,
        name: String
    ): List<Drug> {
        return drugs.filter {
            it.activeSubstance.startsWith(activeSubstancePrefix, ignoreCase = true) &&
            it.name.startsWith(name, ignoreCase = true)
        }
    }
}