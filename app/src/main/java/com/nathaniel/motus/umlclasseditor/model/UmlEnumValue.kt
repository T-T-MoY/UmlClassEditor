package com.nathaniel.motus.umlclasseditor.model

import org.json.JSONException
import org.json.JSONObject

class UmlEnumValue(
    override var name: String? = null,
    val valueOrder: Int
) : AdapterItem {

    fun toJSONObject(): JSONObject? {
        return try {
            JSONObject().apply {
                put(JSON_ENUM_VALUE_NAME, name)
                put(JSON_ENUM_VALUE_INDEX, valueOrder)
            }
        } catch (e: JSONException) {
            null
        }
    }

    companion object {
        private const val JSON_ENUM_VALUE_NAME = "EnumValueName"
        private const val JSON_ENUM_VALUE_INDEX = "EnumValueIndex"

        fun fromJSONObject(jsonObject: JSONObject): UmlEnumValue? {
            return try {
                UmlEnumValue(
                    name = jsonObject.getString(JSON_ENUM_VALUE_NAME),
                    valueOrder = jsonObject.getInt(JSON_ENUM_VALUE_INDEX)
                )
            } catch (e: JSONException) {
                null
            }
        }
    }
}