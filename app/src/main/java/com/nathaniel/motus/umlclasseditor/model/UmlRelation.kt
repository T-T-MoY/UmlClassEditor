package com.nathaniel.motus.umlclasseditor.model

import org.json.JSONException
import org.json.JSONObject

class UmlRelation(
    var relationOriginClass: UmlClass?, // La flecha empieza aquí
    var relationEndClass: UmlClass?,    // y termina aquí
    var umlRelationType: UmlRelationType
) {
    enum class UmlRelationType {
        INHERITANCE,
        REALIZATION,
        AGGREGATION,
        COMPOSITION,
        ASSOCIATION,
        DIRECTED_ASSOCIATION,
        BIDIRECTIONAL_ASSOCIATION,
        DEPENDENCY
    }

    var xOrigin: Float = 0f
    var yOrigin: Float = 0f
    var xEnd: Float = 0f
    var yEnd: Float = 0f

    fun toJSONObject(): JSONObject? {
        return try {
            JSONObject().apply {
                // Usamos .name en lugar de .toString() o getName()
                put(JSON_RELATION_TYPE, umlRelationType.name)
                // Usamos llamada segura (?.) por si la clase es nula
                put(JSON_RELATION_ORIGIN_CLASS, relationOriginClass?.name)
                put(JSON_RELATION_END_CLASS, relationEndClass?.name)
            }
        } catch (e: JSONException) {
            null
        }
    }

    companion object {
        const val JSON_RELATION_TYPE = "RelationType"
        const val JSON_RELATION_ORIGIN_CLASS = "RelationOriginClass"
        const val JSON_RELATION_END_CLASS = "RelationEndCLass" // Mantuve tu C mayúscula original por si el JSON ya está guardado así

        fun fromJSONObject(jsonObject: JSONObject, project: UmlProject): UmlRelation? {
            return try {
                UmlRelation(
                    relationOriginClass = project.getUmlClass(jsonObject.getString(JSON_RELATION_ORIGIN_CLASS)),
                    relationEndClass = project.getUmlClass(jsonObject.getString(JSON_RELATION_END_CLASS)),
                    umlRelationType = UmlRelationType.valueOf(jsonObject.getString(JSON_RELATION_TYPE))
                )
            } catch (e: JSONException) {
                null
            }
        }
    }
}