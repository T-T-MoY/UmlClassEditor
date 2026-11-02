package com.nathaniel.motus.umlclasseditor.model

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class UmlClass(
    name: String? = null,
    var classOrder: Int = 0,
    var umlClassType: UmlClassType = UmlClassType.JAVA_CLASS,
    var attributes: ArrayList<UmlClassAttribute> = ArrayList(),
    var umlClassAttributeCount: Int = 0,
    var methods: ArrayList<UmlClassMethod> = ArrayList(),
    var umlClassMethodCount: Int = 0,
    var values: ArrayList<UmlEnumValue> = ArrayList(),
    var valueCount: Int = 0,
    var umlClassNormalXPos: Float = 0f,
    var umlClassNormalYPos: Float = 0f,
    var umlClassNormalWidth: Float = 0f,
    var umlClassNormalHeight: Float = 0f
) : UmlType(name, TypeLevel.PROJECT) {

    enum class UmlClassType {
        JAVA_CLASS, ABSTRACT_CLASS, INTERFACE, ENUM
    }

    val normalRightEnd: Float
        get() = umlClassNormalXPos + umlClassNormalWidth

    val normalBottomEnd: Float
        get() = umlClassNormalYPos + umlClassNormalHeight

    // Sobrescribimos la propiedad name de la clase padre UmlType
    override var name: String?
        get() = super.name
        set(value) {
            super.name = value
        }

    // Funciones de búsqueda optimizadas con Kotlin
    fun findAttributeByOrder(attributeOrder: Int): UmlClassAttribute? =
        attributes.firstOrNull { it.attributeOrder == attributeOrder }

    fun findMethodByOrder(methodOrder: Int): UmlClassMethod? =
        methods.firstOrNull { it.methodOrder == methodOrder }

    fun findValueByOrder(valueOrder: Int): UmlEnumValue? =
        values.firstOrNull { it.valueOrder == valueOrder }

    fun getAttribute(attributeName: String): UmlClassAttribute? =
        attributes.firstOrNull { it.name == attributeName }

    // **********************************************************************************************
    // Modifiers
    // **********************************************************************************************
    fun addMethod(method: UmlClassMethod) {
        methods.add(method)
        umlClassMethodCount++
    }

    fun removeMethod(method: UmlClassMethod) {
        methods.remove(method)
    }

    fun addAttribute(attribute: UmlClassAttribute) {
        attributes.add(attribute)
        umlClassAttributeCount++
    }

    fun removeAttribute(attribute: UmlClassAttribute) {
        attributes.remove(attribute)
    }

    fun addValue(value: UmlEnumValue) {
        values.add(value)
        valueCount++
    }

    fun removeValue(value: UmlEnumValue) {
        values.remove(value)
    }

    fun incrementUmlClassAttributeCount() {
        umlClassAttributeCount++
    }

    fun incrementUmlClassMethodCount() {
        umlClassMethodCount++
    }

    fun incrementValueCount() {
        valueCount++
    }

    // **********************************************************************************************
    // Test methods
    // **********************************************************************************************
    fun containsPoint(absoluteX: Float, absoluteY: Float): Boolean {
        return absoluteX in umlClassNormalXPos..normalRightEnd &&
                absoluteY in umlClassNormalYPos..normalBottomEnd
    }

    fun isSouthOf(umlClass: UmlClass): Boolean {
        return this.umlClassNormalYPos >= umlClass.normalBottomEnd &&
                this.normalRightEnd >= umlClass.umlClassNormalXPos - this.umlClassNormalYPos + umlClass.normalBottomEnd &&
                this.umlClassNormalXPos <= umlClass.normalRightEnd + this.umlClassNormalYPos - umlClass.normalBottomEnd
    }

    fun isNorthOf(umlClass: UmlClass): Boolean {
        return this.normalBottomEnd <= umlClass.umlClassNormalYPos &&
                this.normalRightEnd >= umlClass.umlClassNormalXPos - umlClass.umlClassNormalYPos + this.normalBottomEnd &&
                this.umlClassNormalXPos <= umlClass.normalRightEnd + umlClass.umlClassNormalYPos - this.normalBottomEnd
    }

    fun isWestOf(umlClass: UmlClass): Boolean {
        return this.normalRightEnd <= umlClass.umlClassNormalXPos &&
                this.normalBottomEnd >= umlClass.umlClassNormalYPos - umlClass.umlClassNormalXPos + this.normalRightEnd &&
                this.umlClassNormalYPos <= umlClass.normalBottomEnd + umlClass.umlClassNormalXPos - this.normalRightEnd
    }

    fun isEastOf(umlClass: UmlClass): Boolean {
        return this.umlClassNormalXPos >= umlClass.normalRightEnd &&
                this.normalBottomEnd >= umlClass.umlClassNormalYPos - this.umlClassNormalXPos + umlClass.normalRightEnd &&
                this.umlClassNormalYPos <= umlClass.normalBottomEnd + this.umlClassNormalXPos - umlClass.normalRightEnd
    }

    fun isInvolvedInRelation(umlRelation: UmlRelation): Boolean {
        return this === umlRelation.relationOriginClass || this === umlRelation.relationEndClass
    }

    fun alreadyExists(inProject: UmlProject): Boolean {
        return inProject.umlClasses.any { this.name == it.name }
    }

    fun containsAttributeNamed(attributeName: String): Boolean {
        return attributes.any { it.name == attributeName }
    }

    fun containsEquivalentMethodTo(method: UmlClassMethod): Boolean {
        return methods.any { it.isEquivalentTo(method) }
    }

    // **********************************************************************************************
    // JSON methods
    // **********************************************************************************************
    fun toJSONObject(): JSONObject? {
        return try {
            JSONObject().apply {
                put(JSON_CLASS_NAME, name.toString())
                put(JSON_CLASS_INDEX, classOrder)
                put(JSON_CLASS_CLASS_TYPE, umlClassType.name)
                put(JSON_CLASS_ATTRIBUTES, attributesToJSONArray)
                put(JSON_CLASS_ATTRIBUTE_COUNT, umlClassAttributeCount)
                put(JSON_CLASS_METHODS, methodsToJSONArray)
                put(JSON_CLASS_METHOD_COUNT, umlClassMethodCount)
                put(JSON_CLASS_VALUES, valuesToJSONArray)
                put(JSON_CLASS_VALUE_COUNT, valueCount)
                put(JSON_CLASS_NORMAL_XPOS, umlClassNormalXPos.toDouble())
                put(JSON_CLASS_NORMAL_YPOS, umlClassNormalYPos.toDouble())
            }
        } catch (e: JSONException) {
            null
        }
    }

    private val attributesToJSONArray: JSONArray
        get() = JSONArray().apply { attributes.forEach { put(it.toJSONObject()) } }

    private val methodsToJSONArray: JSONArray
        get() = JSONArray().apply { methods.forEach { put(it.toJSONObject()) } }

    private val valuesToJSONArray: JSONArray
        get() = JSONArray().apply { values.forEach { put(it.toJSONObject()) } }

    companion object {
        private const val JSON_CLASS_NAME = "ClassName"
        private const val JSON_CLASS_INDEX = "ClassIndex"
        private const val JSON_CLASS_CLASS_TYPE = "ClassClassType"
        private const val JSON_CLASS_ATTRIBUTES = "ClassAttributes"
        private const val JSON_CLASS_METHODS = "ClassMethods"
        private const val JSON_CLASS_VALUES = "ClassValues"
        private const val JSON_CLASS_NORMAL_XPOS = "ClassNormalXPos"
        private const val JSON_CLASS_NORMAL_YPOS = "ClassNormalYPos"
        private const val JSON_CLASS_ATTRIBUTE_COUNT = "ClassAttributeCount"
        private const val JSON_CLASS_METHOD_COUNT = "ClassMethodCount"
        private const val JSON_CLASS_VALUE_COUNT = "ClassValueCount"

        fun fromJSONObject(jsonObject: JSONObject): UmlClass? {
            return try {
                UmlClass(name = jsonObject.getString(JSON_CLASS_NAME))
            } catch (e: JSONException) {
                null
            }
        }

        fun populateUmlClassFromJSONObject(jsonObject: JSONObject, project: UmlProject) {
            try {
                val umlClass = project.getUmlClass(jsonObject.getString(JSON_CLASS_NAME)) ?: return

                umlClass.apply {
                    classOrder = jsonObject.getInt(JSON_CLASS_INDEX)
                    umlClassType = UmlClassType.valueOf(jsonObject.getString(JSON_CLASS_CLASS_TYPE))
                    attributes = getAttributesFromJSONArray(jsonObject.getJSONArray(JSON_CLASS_ATTRIBUTES))
                    methods = getMethodsFromJSONArray(jsonObject.getJSONArray(JSON_CLASS_METHODS))
                    values = getValuesFromJSONArray(jsonObject.getJSONArray(JSON_CLASS_VALUES))
                    umlClassNormalXPos = jsonObject.getInt(JSON_CLASS_NORMAL_XPOS).toFloat()
                    umlClassNormalYPos = jsonObject.getInt(JSON_CLASS_NORMAL_YPOS).toFloat()
                    umlClassAttributeCount = jsonObject.getInt(JSON_CLASS_ATTRIBUTE_COUNT)
                    umlClassMethodCount = jsonObject.getInt(JSON_CLASS_METHOD_COUNT)
                    valueCount = jsonObject.getInt(JSON_CLASS_VALUE_COUNT)
                }
            } catch (ignored: JSONException) {
            }
        }

        // Parseo seguro de JSONArrays sin destruirlos
        private fun getAttributesFromJSONArray(jsonArray: JSONArray): ArrayList<UmlClassAttribute> {
            val list = ArrayList<UmlClassAttribute>()
            for (i in 0 until jsonArray.length()) {
                jsonArray.optJSONObject(i)?.let { json ->
                    UmlClassAttribute.fromJSONObject(json)?.let { list.add(it) }
                }
            }
            return list
        }

        private fun getMethodsFromJSONArray(jsonArray: JSONArray): ArrayList<UmlClassMethod> {
            val list = ArrayList<UmlClassMethod>()
            for (i in 0 until jsonArray.length()) {
                jsonArray.optJSONObject(i)?.let { json ->
                    UmlClassMethod.fromJSONObject(json)?.let { list.add(it) }
                }
            }
            return list
        }

        private fun getValuesFromJSONArray(jsonArray: JSONArray): ArrayList<UmlEnumValue> {
            val list = ArrayList<UmlEnumValue>()
            for (i in 0 until jsonArray.length()) {
                jsonArray.optJSONObject(i)?.let { json ->
                    UmlEnumValue.fromJSONObject(json)?.let { list.add(it) }
                }
            }
            return list
        }
    }
}