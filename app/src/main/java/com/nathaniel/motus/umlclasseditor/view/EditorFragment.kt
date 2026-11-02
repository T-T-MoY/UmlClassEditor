package com.nathaniel.motus.umlclasseditor.view

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.nathaniel.motus.umlclasseditor.controller.FragmentObserver

abstract class EditorFragment : Fragment() {
    protected var mCallback: FragmentObserver? = null
    protected lateinit var mOnBackPressedCallback: OnBackPressedCallback

    //    **********************************************************************************************
    //    Abstract methods
    //    **********************************************************************************************
    protected abstract fun readBundle()
    protected abstract fun clearDraftObject()
    protected abstract fun createOrUpdateObject(): Boolean
    protected abstract fun closeFragment()
    protected abstract fun configureViews()
    protected abstract fun initializeMembers()
    protected abstract fun initializeFields()
    protected abstract fun setOnCreateOrEditDisplay()

    //    **********************************************************************************************
    //    Common methods
    //    **********************************************************************************************
    protected fun createOnBackPressedCallback() {
        mOnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onCancelButtonCLicked()
            }
        }
    }

    protected fun onCancelButtonCLicked() {
        clearDraftObject()
        mOnBackPressedCallback.remove()
        closeFragment()
    }

    protected fun onOKButtonClicked() {
        if (createOrUpdateObject()) {
            mOnBackPressedCallback.remove()
            closeFragment()
        }
    }

    protected fun setOnBackPressedCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(this, mOnBackPressedCallback)
    }

    // Usamos onAttach para asegurar que el contexto (la Activity) implemente la interfaz de forma segura
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mCallback = context as? FragmentObserver
    }

    override fun onDetach() {
        super.onDetach()
        mCallback = null
    }

    //    **********************************************************************************************
    //    Fragment events
    //    **********************************************************************************************
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) readBundle()
        createOnBackPressedCallback()
        setOnBackPressedCallback()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureViews()
        initializeMembers()
        initializeFields()
        setOnCreateOrEditDisplay()
    }
}