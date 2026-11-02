package com.nathaniel.motus.umlclasseditor.controller

import androidx.fragment.app.Fragment
import com.nathaniel.motus.umlclasseditor.model.UmlProject

// **********************************************************************************************
// Callback interface
// **********************************************************************************************
interface FragmentObserver {

    val project: UmlProject?

    fun setPurpose(purpose: Purpose)

    fun closeClassEditorFragment(fragment: Fragment)
    fun closeAttributeEditorFragment(fragment: Fragment)
    fun closeMethodEditorFragment(fragment: Fragment)
    fun closeParameterEditorFragment(fragment: Fragment)

    fun openAttributeEditorFragment(attributeIndex: Int, classIndex: Int)
    fun openMethodEditorFragment(methodIndex: Int, classIndex: Int)
    fun openParameterEditorFragment(parameterIndex: Int, methodIndex: Int, classIndex: Int)

    enum class Purpose {
        NONE,
        CREATE_CLASS,
        EDIT_CLASS,
        CREATE_ATTRIBUTE,
        EDIT_ATTRIBUTE,
        CREATE_METHOD,
        EDIT_METHOD,
        CREATE_PARAMETER,
        EDIT_PARAMETER
    }
}