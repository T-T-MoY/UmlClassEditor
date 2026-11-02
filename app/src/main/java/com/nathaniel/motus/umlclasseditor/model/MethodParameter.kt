package com.nathaniel.motus.umlclasseditor.model

import org.json.JSONException
import org.json.JSONObject

// 1. Convertimos los constructores antiguos de Java en un Constructor Primario
// usando valores por defecto. Esto elimina decenas de líneas de código.
class MethodParameter(
    override var name: String? = null,
    var parameterOrder: Int,
    var umlType: UmlType? = null,
    var typeMultiplicity: TypeMultiplicity = TypeMultiplicity.SINGLE,
    var arrayDimension: Int = 1
) : AdapterItem {

    // 2. Limpieza de lógica visual: Más fácil de leer sin tanto "this."
    fun isEquivalentTo(other: MethodParameter): Boolean {
        if (this.umlType !== other.umlType || this.typeMultiplicity != other.typeMultiplicity) {
            return false
        }
        return if (this.typeMultiplicity == TypeMultiplicity.ARRAY) {
            this.arrayDimension == other.arrayDimension
        } else {
            true
        }
    }

    // 3. Uso de ".apply { }" para armar el JSON de forma fluida
    fun toJSONObject(): JSONObject? {
        return try {
            JSONObject().apply {
                put(JSON_METHOD_PARAMETER_NAME, name)
                put(JSON_METHOD_PARAMETER_INDEX, parameterOrder)
                // Usamos safe call (?.) por si umlType es nulo
                put(JSON_METHOD_PARAMETER_TYPE, umlType?.name)
                put(JSON_METHOD_PARAMETER_TYPE_MULTIPLICITY, typeMultiplicity.name)
                put(JSON_METHOD_PARAMETER_ARRAY_DIMENSION, arrayDimension)
            }
        } catch (e: JSONException) {
            null
        }
    }

    companion object {
        const val JSON_METHOD_PARAMETER_NAME = "MethodParameterName"
        const val JSON_METHOD_PARAMETER_TYPE = "MethodParameterType"
        const val JSON_METHOD_PARAMETER_TYPE_MULTIPLICITY = "MethodParameterTypeMultiplicity"
        const val JSON_METHOD_PARAMETER_ARRAY_DIMENSION = "MethodParameterArrayDimension"
        const val JSON_METHOD_PARAMETER_INDEX = "MethodParameterIndex"

        fun fromJSONObject(jsonObject: JSONObject): MethodParameter? {
            return try {
                val typeName = jsonObject.getString(JSON_METHOD_PARAMETER_TYPE)

                // Limpieza de ".Companion.getUmlTypes()".
                // Kotlin accede directo a las propiedades estáticas con ".umlTypes"
                if (UmlType.valueOf(typeName, UmlType.umlTypes) == null) {
                    UmlType.createUmlType(typeName, UmlType.TypeLevel.CUSTOM)
                }

                // Usamos los parámetros nombrados en el constructor
                MethodParameter(
                    name = jsonObject.getString(JSON_METHOD_PARAMETER_NAME),
                    parameterOrder = jsonObject.getInt(JSON_METHOD_PARAMETER_INDEX),
                    umlType = UmlType.valueOf(typeName, UmlType.umlTypes),
                    typeMultiplicity = TypeMultiplicity.valueOf(jsonObject.getString(JSON_METHOD_PARAMETER_TYPE_MULTIPLICITY)),
                    arrayDimension = jsonObject.getInt(JSON_METHOD_PARAMETER_ARRAY_DIMENSION)
                )
            } catch (e: JSONException) {
                null
            }
        }
    }
}