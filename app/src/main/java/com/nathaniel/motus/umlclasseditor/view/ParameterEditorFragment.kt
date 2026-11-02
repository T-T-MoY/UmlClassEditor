package com.nathaniel.motus.umlclasseditor.view

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.nathaniel.motus.umlclasseditor.R
import com.nathaniel.motus.umlclasseditor.model.MethodParameter
import com.nathaniel.motus.umlclasseditor.model.TypeMultiplicity
import com.nathaniel.motus.umlclasseditor.model.TypeNameComparator
import com.nathaniel.motus.umlclasseditor.model.UmlClass
import com.nathaniel.motus.umlclasseditor.model.UmlClassMethod
import com.nathaniel.motus.umlclasseditor.model.UmlType

class ParameterEditorFragment : EditorFragment(), View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    private var mParameterOrder = -1
    private var mMethodOrder = -1
    private var mClassOrder = -1
    private var mMethodEditorFragmentTag: String? = null

    private lateinit var mMethodParameter: MethodParameter
    private lateinit var mUmlClassMethod: UmlClassMethod
    private lateinit var mUmlClass: UmlClass

    // UI Elements
    private lateinit var mEditParameterText: TextView
    private lateinit var mDeleteParameterButton: Button
    private lateinit var mParameterNameInput: TextInputEditText
    private lateinit var mParameterTypeSpinner: Spinner
    private lateinit var mParameterMultiplicityRadioGroup: RadioGroup
    private lateinit var mSingleRadio: RadioButton
    private lateinit var mCollectionRadio: RadioButton
    private lateinit var mArrayRadio: RadioButton

    // Contenedor de la dimensión
    private lateinit var mDimInputLayout: TextInputLayout
    private lateinit var mDimEdit: TextInputEditText

    private lateinit var mCancelButton: Button
    private lateinit var mOKButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_parameter_editor, container, false)
    }

    override fun onClick(v: View) {
        when (v.tag as? Int) {
            CANCEL_BUTTON_TAG -> onCancelButtonClicked()
            OK_BUTTON_TAG -> onOKButtonClicked()
            DELETE_PARAMETER_BUTTON_TAG -> startDeleteParameterDialog()
        }
    }

    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        if (checkedId == R.id.parameter_array_radio) setOnArrayDisplay() else setOnSingleDisplay()
    }

    private fun onCancelButtonClicked() {
        if (mParameterOrder == -1) mUmlClassMethod.removeParameter(mMethodParameter)
        mOnBackPressedCallback.remove()
        mCallback?.closeParameterEditorFragment(this)
    }

    override fun setOnCreateOrEditDisplay() {
        if (mParameterOrder == -1) setOnCreateDisplay() else setOnEditDisplay()
    }

    override fun readBundle() {
        requireArguments().let {
            mMethodEditorFragmentTag = it.getString(METHOD_EDITOR_FRAGMENT_TAG_KEY)
            mParameterOrder = it.getInt(PARAMETER_ORDER_KEY, -1)
            mMethodOrder = it.getInt(METHOD_ORDER_KEY, -1)
            mClassOrder = it.getInt(CLASS_ORDER_KEY, -1)
        }
    }

    override fun initializeMembers() {
        val project = mCallback?.project ?: return
        mUmlClass = project.findClassByOrder(mClassOrder) ?: return
        mUmlClassMethod = mUmlClass.findMethodByOrder(mMethodOrder) ?: return

        if (mParameterOrder != -1) {
            mMethodParameter = mUmlClassMethod.findParameterByOrder(mParameterOrder) ?: return
        } else {
            mMethodParameter = MethodParameter(parameterOrder = mUmlClassMethod.parameterCount)
            mUmlClassMethod.addParameter(mMethodParameter)
        }
    }

    override fun configureViews() {
        val activity = requireActivity()
        mEditParameterText = activity.findViewById(R.id.edit_parameter_text)

        mDeleteParameterButton = activity.findViewById(R.id.delete_parameter_button)
        mDeleteParameterButton.tag = DELETE_PARAMETER_BUTTON_TAG
        mDeleteParameterButton.setOnClickListener(this)

        mParameterNameInput = activity.findViewById(R.id.parameter_name_input)
        mParameterTypeSpinner = activity.findViewById(R.id.parameter_type_spinner)

        mParameterMultiplicityRadioGroup = activity.findViewById(R.id.parameter_multiplicity_radio_group)
        mParameterMultiplicityRadioGroup.setOnCheckedChangeListener(this)

        mSingleRadio = activity.findViewById(R.id.parameter_simple_radio)
        mCollectionRadio = activity.findViewById(R.id.parameter_collection_radio)
        mArrayRadio = activity.findViewById(R.id.parameter_array_radio)

        mDimEdit = activity.findViewById(R.id.parameter_dimension_input)
        mDimInputLayout = mDimEdit.parent.parent as TextInputLayout

        mCancelButton = activity.findViewById(R.id.parameter_cancel_button)
        mCancelButton.tag = CANCEL_BUTTON_TAG
        mCancelButton.setOnClickListener(this)

        mOKButton = activity.findViewById(R.id.parameter_ok_button)
        mOKButton.tag = OK_BUTTON_TAG
        mOKButton.setOnClickListener(this)
    }

    override fun initializeFields() {
        if (mParameterOrder != -1) {
            mParameterNameInput.setText(mMethodParameter.name)
            when (mMethodParameter.typeMultiplicity) {
                TypeMultiplicity.SINGLE -> mSingleRadio.isChecked = true
                TypeMultiplicity.COLLECTION -> mCollectionRadio.isChecked = true
                TypeMultiplicity.ARRAY -> mArrayRadio.isChecked = true
            }
            mDimEdit.setText(mMethodParameter.arrayDimension.toString())
            if (mMethodParameter.typeMultiplicity == TypeMultiplicity.ARRAY) setOnArrayDisplay() else setOnSingleDisplay()
        } else {
            mParameterNameInput.setText("")
            mSingleRadio.isChecked = true
            mDimEdit.setText("")
            setOnSingleDisplay()
        }
        populateTypeSpinner()
    }

    private fun populateTypeSpinner() {
        val arrayList = UmlType.umlTypes.filter { it.name != "void" }.mapNotNull { it.name }.sortedWith(TypeNameComparator()).toMutableList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, arrayList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mParameterTypeSpinner.adapter = adapter

        if (mParameterOrder != -1) {
            val index = arrayList.indexOf(mMethodParameter.umlType?.name)
            if (index >= 0) mParameterTypeSpinner.setSelection(index)
        }
    }

    private fun setOnEditDisplay() {
        mEditParameterText.text = getString(R.string.title_edit_parameter)
        mDeleteParameterButton.visibility = View.VISIBLE
    }

    private fun setOnCreateDisplay() {
        mEditParameterText.text = getString(R.string.title_create_parameter)
        mDeleteParameterButton.visibility = View.INVISIBLE
    }

    private fun setOnSingleDisplay() {
        mDimInputLayout.visibility = View.INVISIBLE
    }

    private fun setOnArrayDisplay() {
        mDimInputLayout.visibility = View.VISIBLE
    }

    fun updateParameterEditorFragment(parameterOrder: Int, methodOrder: Int, classOrder: Int) {
        mParameterOrder = parameterOrder
        mMethodOrder = methodOrder
        mClassOrder = classOrder
        initializeMembers()
        initializeFields()
        if (mParameterOrder == -1) setOnCreateDisplay() else setOnEditDisplay()

        if (mParameterOrder != -1 && mMethodParameter.typeMultiplicity == TypeMultiplicity.ARRAY) {
            setOnArrayDisplay()
        } else {
            setOnSingleDisplay()
        }
        setOnBackPressedCallback()
    }

    override fun closeFragment() {
        mCallback?.closeParameterEditorFragment(this)
    }

    override fun clearDraftObject() {
        if (mParameterOrder == -1) mUmlClassMethod.removeParameter(mMethodParameter)
    }

    override fun createOrUpdateObject(): Boolean = createOrUpdateParameter()

    private fun createOrUpdateParameter(): Boolean {
        val name = parameterName
        if (name.isBlank()) {
            Toast.makeText(context, R.string.error_parameter_name_blank, Toast.LENGTH_SHORT).show()
            return false
        }

        val existingParam = mUmlClassMethod.getParameter(name)
        if (mUmlClassMethod.containsParameterNamed(name) && existingParam?.parameterOrder != mParameterOrder) {
            Toast.makeText(context, R.string.error_name_already_used, Toast.LENGTH_SHORT).show()
            return false
        }

        mMethodParameter.apply {
            this.name = name
            this.umlType = parameterType
            this.typeMultiplicity = parameterMultiplicity
            this.arrayDimension = parameterArrayDimension
        }
        return true
    }

    private val parameterName: String get() = mParameterNameInput.text.toString()

    private val parameterType: UmlType? get() = UmlType.valueOf(mParameterTypeSpinner.selectedItem.toString(), UmlType.umlTypes)

    private val parameterMultiplicity: TypeMultiplicity
        get() = when {
            mSingleRadio.isChecked -> TypeMultiplicity.SINGLE
            mCollectionRadio.isChecked -> TypeMultiplicity.COLLECTION
            else -> TypeMultiplicity.ARRAY
        }

    private val parameterArrayDimension: Int
        get() {
            val text = mDimEdit.text.toString()
            return if (text.isBlank()) 0 else text.toInt()
        }

    private fun startDeleteParameterDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_parameter_title)
            .setMessage(R.string.dialog_delete_parameter_message)
            .setNegativeButton(R.string.button_no, null)
            .setPositiveButton(R.string.button_yes) { _, _ ->
                mUmlClassMethod.removeParameter(mMethodParameter)
                mCallback?.closeParameterEditorFragment(this)
            }
            .show()
    }

    companion object {
        private const val PARAMETER_ORDER_KEY = "parameterOrder"
        private const val METHOD_ORDER_KEY = "methodOrder"
        private const val CLASS_ORDER_KEY = "classOrder"
        private const val METHOD_EDITOR_FRAGMENT_TAG_KEY = "methodEditorFragmentTag"
        private const val DELETE_PARAMETER_BUTTON_TAG = 510
        private const val CANCEL_BUTTON_TAG = 520
        private const val OK_BUTTON_TAG = 530

        @JvmStatic
        fun newInstance(methodEditorFragmentTag: String?, parameterOrder: Int, methodOrder: Int, classOrder: Int): ParameterEditorFragment {
            return ParameterEditorFragment().apply {
                arguments = Bundle().apply {
                    putString(METHOD_EDITOR_FRAGMENT_TAG_KEY, methodEditorFragmentTag)
                    putInt(PARAMETER_ORDER_KEY, parameterOrder)
                    putInt(METHOD_ORDER_KEY, methodOrder)
                    putInt(CLASS_ORDER_KEY, classOrder)
                }
            }
        }
    }
}