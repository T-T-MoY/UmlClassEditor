package com.nathaniel.motus.umlclasseditor.model

import org.json.JSONException
import org.json.JSONObject

class UmlClassAttribute(
    override var name: String? = null,
    var attributeOrder: Int,
    var visibility: Visibility = Visibility.PRIVATE,
    var isStatic: Boolean = false,
    var isFinal: Boolean = false,
    var umlType: UmlType? = null,
    var typeMultiplicity: TypeMultiplicity = TypeMultiplicity.SINGLE,
    var arrayDimension: Int = 1
) : AdapterItem {

    // Kotlin permite usar propiedades calculadas con 'get()'
    // y usamos un 'when' para evitar múltiples if/else
    val attributeCompleteString: String
        get() {
            val vis = when (visibility) {
                Visibility.PUBLIC -> "+"
                Visibility.PROTECTED -> "~"
                else -> "-"
            }

            val typeName = umlType?.name ?: "Desconocido"

            // Interpolación de strings en Kotlin ($variable)
            return when (typeMultiplicity) {
                TypeMultiplicity.COLLECTION -> "$vis$name : <$typeName>"
                TypeMultiplicity.ARRAY -> "$vis$name : [$typeName]^$arrayDimension"
                else -> "$vis$name : $typeName"
            }
        }

    fun toJSONObject(): JSONObject? {
        return try {
            JSONObject().apply {
                put(JSON_CLASS_ATTRIBUTE_NAME, name)
                put(JSON_CLASS_ATTRIBUTE_INDEX, attributeOrder)
                put(JSON_CLASS_ATTRIBUTE_VISIBILITY, visibility.name)
                put(JSON_CLASS_ATTRIBUTE_STATIC, isStatic)
                put(JSON_CLASS_ATTRIBUTE_FINAL, isFinal)
                put(JSON_CLASS_ATTRIBUTE_TYPE, umlType?.name)
                put(JSON_CLASS_ATTRIBUTE_TYPE_MULTIPLICITY, typeMultiplicity.name)
                put(JSON_CLASS_ATTRIBUTE_ARRAY_DIMENSION, arrayDimension)
            }
        } catch (e: JSONException) {
            null
        }
    }

    companion object {
        const val JSON_CLASS_ATTRIBUTE_NAME = "ClassAttributeName"
        const val JSON_CLASS_ATTRIBUTE_INDEX = "ClassAttributeIndex"
        const val JSON_CLASS_ATTRIBUTE_VISIBILITY = "ClassAttributeVisibility"
        const val JSON_CLASS_ATTRIBUTE_STATIC = "ClassAttributeStatic"
        const val JSON_CLASS_ATTRIBUTE_FINAL = "ClassAttributeFinal"
        const val JSON_CLASS_ATTRIBUTE_TYPE = "ClassAttributeType"
        const val JSON_CLASS_ATTRIBUTE_TYPE_MULTIPLICITY = "ClassAttributeTypeMultiplicity"
        const val JSON_CLASS_ATTRIBUTE_ARRAY_DIMENSION = "ClassAttributeArrayDimension"

        // Eliminamos el 'for' y usamos la función nativa 'indexOfFirst'
        fun indexOf(attributeName: String, attributes: ArrayList<UmlClassAttribute>): Int {
            return attributes.indexOfFirst { it.name == attributeName }
        }

        fun fromJSONObject(jsonObject: JSONObject): UmlClassAttribute? {
            return try {
                val typeName = jsonObject.getString(JSON_CLASS_ATTRIBUTE_TYPE)

                if (UmlType.valueOf(typeName, UmlType.umlTypes) == null) {
                    UmlType.createUmlType(typeName, UmlType.TypeLevel.CUSTOM)
                }

                UmlClassAttribute(
                    name = jsonObject.getString(JSON_CLASS_ATTRIBUTE_NAME),
                    attributeOrder = jsonObject.getInt(JSON_CLASS_ATTRIBUTE_INDEX),
                    visibility = Visibility.valueOf(jsonObject.getString(JSON_CLASS_ATTRIBUTE_VISIBILITY)),
                    isStatic = jsonObject.getBoolean(JSON_CLASS_ATTRIBUTE_STATIC),
                    isFinal = jsonObject.getBoolean(JSON_CLASS_ATTRIBUTE_FINAL),
                    umlType = UmlType.valueOf(typeName, UmlType.umlTypes),
                    typeMultiplicity = TypeMultiplicity.valueOf(jsonObject.getString(JSON_CLASS_ATTRIBUTE_TYPE_MULTIPLICITY)),
                    arrayDimension = jsonObject.getInt(JSON_CLASS_ATTRIBUTE_ARRAY_DIMENSION)
                )
            } catch (e: JSONException) {
                null
            }
        }
    }
}