package com.nathaniel.motus.umlclasseditor.model

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class UmlClassMethod(
    override var name: String? = null,
    var methodOrder: Int,
    var visibility: Visibility = Visibility.PRIVATE,
    var isStatic: Boolean = false,
    var umlType: UmlType? = null,
    var typeMultiplicity: TypeMultiplicity = TypeMultiplicity.SINGLE,
    var arrayDimension: Int = 1,
    val parameters: ArrayList<MethodParameter> = ArrayList(),
    var parameterCount: Int = 0
) : AdapterItem {

    val methodCompleteString: String
        get() {
            val vis = when (visibility) {
                Visibility.PUBLIC -> "+"
                Visibility.PROTECTED -> "~"
                else -> "-"
            }

            // Magia de Kotlin: Une todos los parámetros por comas en una sola línea
            val paramsString = parameters.joinToString(", ") { it.name ?: "" }

            val typeName = umlType?.name ?: "Desconocido"

            // Interpolación de variables limpia
            return when (typeMultiplicity) {
                TypeMultiplicity.COLLECTION -> "$vis$name($paramsString) : <$typeName>"
                TypeMultiplicity.ARRAY -> "$vis$name($paramsString) : [$typeName]^$arrayDimension"
                else -> "$vis$name($paramsString) : $typeName"
            }
        }

    // Funciones funcionales nativas de Kotlin (firstOrNull, any)
    fun findParameterByOrder(parameterOrder: Int): MethodParameter? {
        return parameters.firstOrNull { it.parameterOrder == parameterOrder }
    }

    fun getParameter(parameterName: String): MethodParameter? {
        return parameters.firstOrNull { it.name == parameterName }
    }

    fun containsParameterNamed(parameterName: String): Boolean {
        return parameters.any { it.name == parameterName }
    }

    fun addParameter(parameter: MethodParameter) {
        parameters.add(parameter)
        parameterCount++
    }

    fun removeParameter(parameter: MethodParameter) {
        parameters.remove(parameter)
    }

    fun incrementParameterCount() {
        parameterCount++
    }

    fun isEquivalentTo(method: UmlClassMethod): Boolean {
        if (this.methodOrder == method.methodOrder) return false
        if (this.name != method.name) return false
        if (this.umlType !== method.umlType) return false
        if (this.parameters.size != method.parameters.size) return false

        for (i in parameters.indices) {
            if (!parameters[i].isEquivalentTo(method.parameters[i])) return false
        }
        return true
    }

    fun toJSONObject(): JSONObject? {
        return try {
            JSONObject().apply {
                put(JSON_CLASS_METHOD_NAME, name)
                put(JSON_CLASS_METHOD_INDEX, methodOrder)
                put(JSON_CLASS_METHOD_VISIBILITY, visibility.name)
                put(JSON_CLASS_METHOD_STATIC, isStatic)
                put(JSON_CLASS_METHOD_TYPE, umlType?.name)
                put(JSON_CLASS_METHOD_TYPE_MULTIPLICITY, typeMultiplicity.name)
                put(JSON_CLASS_METHOD_ARRAY_DIMENSION, arrayDimension)
                put(JSON_CLASS_METHOD_PARAMETERS, parametersToJSONArray)
                put(JSON_CLASS_METHOD_PARAMETER_COUNT, parameterCount)
            }
        } catch (e: JSONException) {
            null
        }
    }

    private val parametersToJSONArray: JSONArray
        get() {
            return JSONArray().apply {
                parameters.forEach { p -> p.toJSONObject()?.let { put(it) } }
            }
        }

    companion object {
        const val JSON_CLASS_METHOD_NAME = "ClassMethodName"
        const val JSON_CLASS_METHOD_VISIBILITY = "ClassMethodVisibility"
        const val JSON_CLASS_METHOD_STATIC = "ClassMethodStatic"
        const val JSON_CLASS_METHOD_TYPE = "ClassMethodType"
        const val JSON_CLASS_METHOD_TYPE_MULTIPLICITY = "ClassMethodTypeMultiplicity"
        const val JSON_CLASS_METHOD_ARRAY_DIMENSION = "ClassMethodArrayDimension"
        const val JSON_CLASS_METHOD_PARAMETERS = "ClassMethodParameters"
        const val JSON_CLASS_METHOD_PARAMETER_COUNT = "ClassMethodParameterCount"
        const val JSON_CLASS_METHOD_INDEX = "ClassMethodIndex"

        fun indexOf(methodName: String, methods: ArrayList<UmlClassMethod>): Int {
            return methods.indexOfFirst { it.name == methodName }
        }

        fun fromJSONObject(jsonObject: JSONObject): UmlClassMethod? {
            return try {
                val typeName = jsonObject.getString(JSON_CLASS_METHOD_TYPE)

                if (UmlType.valueOf(typeName, UmlType.umlTypes) == null) {
                    UmlType.createUmlType(typeName, UmlType.TypeLevel.CUSTOM)
                }

                UmlClassMethod(
                    name = jsonObject.getString(JSON_CLASS_METHOD_NAME),
                    methodOrder = jsonObject.getInt(JSON_CLASS_METHOD_INDEX),
                    visibility = Visibility.valueOf(jsonObject.getString(JSON_CLASS_METHOD_VISIBILITY)),
                    isStatic = jsonObject.getBoolean(JSON_CLASS_METHOD_STATIC),
                    umlType = UmlType.valueOf(typeName, UmlType.umlTypes),
                    typeMultiplicity = TypeMultiplicity.valueOf(jsonObject.getString(JSON_CLASS_METHOD_TYPE_MULTIPLICITY)),
                    arrayDimension = jsonObject.getInt(JSON_CLASS_METHOD_ARRAY_DIMENSION),
                    parameters = getParametersFromJSONArray(jsonObject.getJSONArray(JSON_CLASS_METHOD_PARAMETERS)),
                    parameterCount = jsonObject.getInt(JSON_CLASS_METHOD_PARAMETER_COUNT)
                )
            } catch (e: JSONException) {
                null
            }
        }

        private fun getParametersFromJSONArray(jsonArray: JSONArray): ArrayList<MethodParameter> {
            val methodParameters = ArrayList<MethodParameter>()
            // Iteración segura sobre un JSONArray sin destruirlo
            for (i in 0 until jsonArray.length()) {
                val jsonParameter = jsonArray.optJSONObject(i)
                if (jsonParameter != null) {
                    MethodParameter.fromJSONObject(jsonParameter)?.let { methodParameters.add(it) }
                }
            }
            return methodParameters
        }
    }
}