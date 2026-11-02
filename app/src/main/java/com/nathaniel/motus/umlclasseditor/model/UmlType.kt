package com.nathaniel.motus.umlclasseditor.model

import android.content.Context
import android.net.Uri
import android.util.Log
import com.nathaniel.motus.umlclasseditor.R
import com.nathaniel.motus.umlclasseditor.controller.IOUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File

open class UmlType(
    open var name: String? = null,
    typeLevel: TypeLevel = TypeLevel.PROJECT
) {
    enum class TypeLevel {
        PRIMITIVE, CUSTOM, PROJECT
    }

    var typeLevel: TypeLevel = typeLevel
        protected set

    init {
        // En el diseño original, solo se añadían a la lista estática si se creaban con nombre explícito
        if (name != null) {
            umlTypes.add(this)
        }
    }

    fun upgradeToProjectUmlType() {
        this.typeLevel = TypeLevel.PROJECT
        if (!umlTypes.contains(this)) {
            umlTypes.add(this)
        }
    }

    val isPrimitiveUmlType: Boolean
        get() = this.typeLevel == TypeLevel.PRIMITIVE

    val isCustomUmlType: Boolean
        get() = this.typeLevel == TypeLevel.CUSTOM

    val isProjectUmlType: Boolean
        get() = this.typeLevel == TypeLevel.PROJECT

    companion object {
        val umlTypes: ArrayList<UmlType> = ArrayList()

        private const val CUSTOM_TYPES_FILENAME = "custom_types"

        const val JSON_PACKAGE_VERSION_CODE: String = "PackageVersionCode"
        const val JSON_CUSTOM_TYPES: String = "CustomTypes"

        @JvmStatic
        fun createUmlType(name: String?, typeLevel: TypeLevel) {
            // Instanciarlo ya dispara el bloque 'init' que lo añade a la lista
            UmlType(name, typeLevel)
        }

        fun valueOf(name: String, inUmlTypes: ArrayList<UmlType>): UmlType? {
            return inUmlTypes.firstOrNull { it.name == name }
        }

        val customUmlTypesToJSONArray: JSONArray
            get() = JSONArray().apply {
                umlTypes.filter { it.isCustomUmlType }.forEach { put(it.name) }
            }

        fun createCustomUmlTypesFromJSONArray(jsonArray: JSONArray) {
            for (i in 0 until jsonArray.length()) {
                var typeName = jsonArray.optString(i)
                if (typeName.isNotEmpty()) {
                    while (containsUmlTypeNamed(typeName)) typeName = "$typeName(1)"
                    createUmlType(typeName, TypeLevel.CUSTOM)
                }
            }
        }

        // **********************************************************************************************
        // Modifiers
        // **********************************************************************************************
        @JvmStatic
        fun clearProjectUmlTypes() {
            umlTypes.removeAll { it.isProjectUmlType }
        }

        @JvmStatic
        fun clearUmlTypes() {
            umlTypes.clear()
        }

        @JvmStatic
        fun removeUmlType(umlType: UmlType) {
            umlTypes.remove(umlType)
        }

        @JvmStatic
        fun initializePrimitiveUmlTypes(context: Context) {
            val standardTypes = context.resources.getStringArray(R.array.standard_types)
            standardTypes.forEach { createUmlType(it, TypeLevel.PRIMITIVE) }
        }

        fun containsPrimitiveUmlTypeNamed(name: String?): Boolean =
            umlTypes.any { it.name == name && it.isPrimitiveUmlType }

        fun containsCustomUmlTypeNamed(name: String?): Boolean =
            umlTypes.any { it.name == name && it.isCustomUmlType }

        fun containsProjectUmlTypeNamed(name: String): Boolean =
            umlTypes.any { it.name == name && it.isProjectUmlType }

        @JvmStatic
        fun containsUmlTypeNamed(name: String?): Boolean =
            umlTypes.any { it.name == name }

        // **********************************************************************************************
        // Save and load methods
        // **********************************************************************************************
        @JvmStatic
        fun saveCustomUmlTypes(context: Context) {
            try {
                val jsonObject = JSONObject().apply {
                    put(JSON_CUSTOM_TYPES, customUmlTypesToJSONArray)
                    put(JSON_PACKAGE_VERSION_CODE, IOUtils.getAppVersionCode(context))
                }
                IOUtils.saveFileToInternalStorage(
                    jsonObject.toString(),
                    File(context.filesDir, CUSTOM_TYPES_FILENAME)
                )
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        @JvmStatic
        fun initializeCustomUmlTypes(context: Context) {
            try {
                val fileContent = IOUtils.getFileFromInternalStorage(File(context.filesDir, CUSTOM_TYPES_FILENAME))
                if (fileContent.isNotEmpty()) {
                    val jsonObject = JSONObject(fileContent)
                    Log.i("TEST", "Loaded custom types")
                    createCustomUmlTypesFromJSONArray(jsonObject.getJSONArray(JSON_CUSTOM_TYPES))
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        @JvmStatic
        fun exportCustomUmlTypes(context: Context, toDestination: Uri?) {
            if (toDestination == null) return
            try {
                val jsonObject = JSONObject().apply {
                    put(JSON_CUSTOM_TYPES, customUmlTypesToJSONArray)
                    put(JSON_PACKAGE_VERSION_CODE, IOUtils.getAppVersionCode(context))
                }
                IOUtils.saveFileToExternalStorage(context, jsonObject.toString(), toDestination)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        @JvmStatic
        fun importCustomUmlTypes(context: Context, fromDestination: Uri?) {
            if (fromDestination == null) return
            try {
                val fileContent = IOUtils.readFileFromExternalStorage(context, fromDestination)
                if (fileContent.isNotEmpty()) {
                    val jsonObject = JSONObject(fileContent)
                    createCustomUmlTypesFromJSONArray(jsonObject.getJSONArray(JSON_CUSTOM_TYPES))
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }
}