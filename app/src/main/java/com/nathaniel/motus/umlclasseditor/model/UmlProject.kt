package com.nathaniel.motus.umlclasseditor.model

import android.content.Context
import android.net.Uri
import com.nathaniel.motus.umlclasseditor.controller.IOUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File

class UmlProject(var name: String, context: Context?) {

    val umlClasses: ArrayList<UmlClass> = ArrayList()
    var umlClassCount: Int = 0
    var umlRelations: ArrayList<UmlRelation> = ArrayList()

    private var mAppVersionCode = 0
    var zoom: Float = 1f
    var xOffset: Float = 0f
    var yOffset: Float = 0f

    // Búsquedas simplificadas
    fun getUmlClass(className: String): UmlClass? = umlClasses.firstOrNull { it.name == className }

    fun findClassByOrder(classOrder: Int): UmlClass? = umlClasses.firstOrNull { it.classOrder == classOrder }

    fun setAppVersionCode(appVersionCode: Int) {
        mAppVersionCode = appVersionCode
    }

    // **********************************************************************************************
    // Modifiers
    // **********************************************************************************************
    fun addUmlClass(umlClass: UmlClass) {
        umlClasses.add(umlClass)
        umlClassCount++
    }

    fun removeUmlClass(umlClass: UmlClass) {
        umlClasses.remove(umlClass)
        UmlType.removeUmlType(umlClass)
        removeRelationsInvolving(umlClass)
        removeAttributesOfType(umlClass)
        removeMethodsOfType(umlClass)
        removeParametersOfType(umlClass)
    }

    fun addUmlRelation(umlRelation: UmlRelation) {
        umlRelations.add(umlRelation)
    }

    fun removeUmlRelation(umlRelation: UmlRelation) {
        umlRelations.remove(umlRelation)
    }

    // Limpieza masiva segura usando removeAll
    fun removeRelationsInvolving(umlClass: UmlClass) {
        umlRelations.removeAll { umlClass.isInvolvedInRelation(it) }
    }

    fun removeAttributesOfType(umlType: UmlType) {
        umlClasses.forEach { c -> c.attributes.removeAll { it.umlType === umlType } }
    }

    fun removeMethodsOfType(umlType: UmlType) {
        umlClasses.forEach { c -> c.methods.removeAll { it.umlType === umlType } }
    }

    fun removeParametersOfType(umlType: UmlType) {
        umlClasses.forEach { c ->
            c.methods.forEach { m ->
                m.parameters.removeAll { it.umlType === umlType }
            }
        }
    }

    fun incrementClassCount() {
        umlClassCount++
    }

    // **********************************************************************************************
    // Test methods
    // **********************************************************************************************
    fun relationAlreadyExistsBetween(firstClass: UmlClass, secondClass: UmlClass): Boolean {
        return umlRelations.any { r ->
            (r.relationOriginClass === firstClass && r.relationEndClass === secondClass) ||
                    (r.relationOriginClass === secondClass && r.relationEndClass === firstClass)
        }
    }

    fun hasConflictNameWith(umlClass: UmlClass): Boolean = umlClasses.any { it.name == umlClass.name }

    fun containsClassNamed(className: String): Boolean = umlClasses.any { it.name == className }

    // **********************************************************************************************
    // JSON methods
    // **********************************************************************************************
    fun toJSONObject(context: Context): JSONObject? {
        return try {
            JSONObject().apply {
                put(JSON_PROJECT_PACKAGE_VERSION_CODE, IOUtils.getAppVersionCode(context))
                put(JSON_PROJECT_ZOOM, zoom.toDouble())
                put(JSON_PROJECT_X_OFFSET, xOffset.toDouble())
                put(JSON_PROJECT_Y_OFFSET, yOffset.toDouble())
                put(JSON_PROJECT_NAME, name)
                put(JSON_PROJECT_CLASSES, classesToJSONArray)
                put(JSON_PROJECT_CLASS_COUNT, umlClassCount)
                put(JSON_PROJECT_RELATIONS, relationsToJSONArray)
            }
        } catch (e: JSONException) {
            null
        }
    }

    private val classesToJSONArray: JSONArray
        get() = JSONArray().apply { umlClasses.forEach { it.toJSONObject()?.let { json -> put(json) } } }

    private val relationsToJSONArray: JSONArray
        get() = JSONArray().apply { umlRelations.forEach { it.toJSONObject()?.let { json -> put(json) } } }

    private fun populateClassesFromJSONArray(jsonArray: JSONArray) {
        for (i in 0 until jsonArray.length()) {
            jsonArray.optJSONObject(i)?.let { jsonObject ->
                UmlClass.populateUmlClassFromJSONObject(jsonObject, this)
            }
        }
    }

    // **********************************************************************************************
    // Save and load project methods
    // **********************************************************************************************
    fun save(context: Context) {
        val destination = File(context.filesDir, PROJECT_DIRECTORY)
        if (!destination.exists()) destination.mkdir()
        IOUtils.saveFileToInternalStorage(toJSONObject(context).toString(), File(destination, name))
    }

    fun exportProject(context: Context, toDestination: Uri?) {
        toDestination?.let {
            IOUtils.saveFileToExternalStorage(context, toJSONObject(context).toString(), it)
        }
    }

    fun mergeWith(project: UmlProject) {
        for (c in project.umlClasses) {
            while (UmlType.containsPrimitiveUmlTypeNamed(c.name)) c.name = "${c.name}(1)"
            while (UmlType.containsCustomUmlTypeNamed(c.name)) c.name = "${c.name}(1)"
            while (c.alreadyExists(this)) c.name = "${c.name}(1)"

            this.addUmlClass(c)
        }
        project.umlRelations.forEach { this.addUmlRelation(it) }
    }

    companion object {
        const val JSON_PROJECT_NAME = "ProjectName"
        const val JSON_PROJECT_CLASS_COUNT = "ProjectClassCount"
        const val JSON_PROJECT_CLASSES = "ProjectClasses"
        const val JSON_PROJECT_RELATIONS = "ProjectRelations"
        const val JSON_PROJECT_PACKAGE_VERSION_CODE = "ProjectPackageVersionCode"
        const val JSON_PROJECT_ZOOM = "ProjectZoom"
        const val JSON_PROJECT_X_OFFSET = "ProjectXOffset"
        const val JSON_PROJECT_Y_OFFSET = "ProjectYOffset"

        const val PROJECT_DIRECTORY = "projects"

        fun fromJSONObject(jsonObject: JSONObject, context: Context?): UmlProject? {
            return try {
                val project = UmlProject(jsonObject.getString(JSON_PROJECT_NAME), context).apply {
                    setAppVersionCode(jsonObject.getInt(JSON_PROJECT_PACKAGE_VERSION_CODE))
                    zoom = jsonObject.getDouble(JSON_PROJECT_ZOOM).toFloat()
                    xOffset = jsonObject.getDouble(JSON_PROJECT_X_OFFSET).toFloat()
                    yOffset = jsonObject.getDouble(JSON_PROJECT_Y_OFFSET).toFloat()
                    umlClassCount = jsonObject.getInt(JSON_PROJECT_CLASS_COUNT)
                }

                // Usamos la cadena original para no alterar el jsonObject en caso de que se vuelva a leer
                val classesArray = jsonObject.getJSONArray(JSON_PROJECT_CLASSES)
                getClassesFromJSONArray(classesArray).forEach { project.addUmlClass(it) }

                project.populateClassesFromJSONArray(classesArray)
                project.umlRelations = getRelationsFromJSONArray(jsonObject.getJSONArray(JSON_PROJECT_RELATIONS), project)

                project
            } catch (e: JSONException) {
                null
            }
        }

        private fun getClassesFromJSONArray(jsonArray: JSONArray): ArrayList<UmlClass> {
            val classes = ArrayList<UmlClass>()
            for (i in 0 until jsonArray.length()) {
                jsonArray.optJSONObject(i)?.let { jsonObject ->
                    UmlClass.fromJSONObject(jsonObject)?.let { classes.add(it) }
                }
            }
            return classes
        }

        private fun getRelationsFromJSONArray(jsonArray: JSONArray, project: UmlProject): ArrayList<UmlRelation> {
            val relations = ArrayList<UmlRelation>()
            for (i in 0 until jsonArray.length()) {
                jsonArray.optJSONObject(i)?.let { jsonObject ->
                    UmlRelation.fromJSONObject(jsonObject, project)?.let { relations.add(it) }
                }
            }
            return relations
        }

        @JvmStatic
        fun load(context: Context, projectName: String): UmlProject? {
            val destination = File(context.filesDir, PROJECT_DIRECTORY)
            val source = File(destination, projectName)
            return try {
                fromJSONObject(JSONObject(IOUtils.getFileFromInternalStorage(source)), context)
            } catch (e: JSONException) {
                e.printStackTrace()
                null
            }
        }

        @JvmStatic
        fun importProject(context: Context, fromFileUri: Uri?): UmlProject? {
            if (fromFileUri == null) return null
            return try {
                val jsonString = IOUtils.readFileFromExternalStorage(context, fromFileUri)
                val umlProject = fromJSONObject(JSONObject(jsonString), context)

                umlProject?.umlClasses?.forEach { c ->
                    while (UmlType.containsPrimitiveUmlTypeNamed(c.name)) c.name = "${c.name}(1)"
                    while (UmlType.containsCustomUmlTypeNamed(c.name)) c.name = "${c.name}(1)"
                }
                umlProject
            } catch (e: JSONException) {
                e.printStackTrace()
                null
            }
        }
    }
}