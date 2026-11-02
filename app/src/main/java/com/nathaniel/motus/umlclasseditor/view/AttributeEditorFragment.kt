package com.nathaniel.motus.umlclasseditor.view

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.nathaniel.motus.umlclasseditor.R
import com.nathaniel.motus.umlclasseditor.model.TypeMultiplicity
import com.nathaniel.motus.umlclasseditor.model.TypeNameComparator
import com.nathaniel.motus.umlclasseditor.model.UmlClass
import com.nathaniel.motus.umlclasseditor.model.UmlClassAttribute
import com.nathaniel.motus.umlclasseditor.model.UmlType
import com.nathaniel.motus.umlclasseditor.model.Visibility

class AttributeEditorFragment : EditorFragment(), View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    private var mAttributeOrder = 0
    private var mClassOrder = 0
    private var mClassEditorFragmentTag: String? = null

    private lateinit var mUmlClassAttribute: UmlClassAttribute
    private lateinit var mUmlClass: UmlClass
    private lateinit var mEditAttributeText: TextView
    private lateinit var mDeleteAttributeButton: Button

    // Cambiamos a los nuevos componentes Material
    private lateinit var mAttributeNameInput: TextInputEditText
    private lateinit var mPublicRadio: RadioButton
    private lateinit var mProtectedRadio: RadioButton
    private lateinit var mPrivateRadio: RadioButton
    private lateinit var mStaticCheck: CheckBox
    private lateinit var mFinalCheck: CheckBox
    private lateinit var mTypeSpinner: Spinner
    private lateinit var mMultiplicityRadioGroup: RadioGroup
    private lateinit var mSimpleRadio: RadioButton
    private lateinit var mCollectionRadio: RadioButton
    private lateinit var mArrayRadio: RadioButton

    // El texto mDimText ya no existe, solo manejaremos el InputBox (TextInputLayout) y su EditText
    private lateinit var mDimInputLayout: TextInputLayout
    private lateinit var mDimEdit: TextInputEditText

    private lateinit var mOKButton: Button
    private lateinit var mCancelButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_attribute_editor, container, false)
    }

    override fun readBundle() {
        mAttributeOrder = requireArguments().getInt(ATTRIBUTE_ORDER_KEY, -1)
        mClassOrder = requireArguments().getInt(CLASS_ORDER_KEY, -1)
        mClassEditorFragmentTag = requireArguments().getString(CLASS_EDITOR_FRAGMENT_TAG_KEY)
    }

    override fun setOnCreateOrEditDisplay() {
        if (mAttributeOrder == -1) setOnCreateDisplay() else setOnEditDisplay()
    }

    override fun configureViews() {
        val activity = requireActivity()
        mEditAttributeText = activity.findViewById(R.id.edit_attribute_text)
        mDeleteAttributeButton = activity.findViewById(R.id.delete_attribute_button)
        mDeleteAttributeButton.tag = DELETE_ATTRIBUTE_BUTTON_TAG
        mDeleteAttributeButton.setOnClickListener(this)

        mAttributeNameInput = activity.findViewById(R.id.attribute_name_input)
        mMultiplicityRadioGroup = activity.findViewById(R.id.attribute_multiplicity_radio_group)
        mMultiplicityRadioGroup.setOnCheckedChangeListener(this)
        mPublicRadio = activity.findViewById(R.id.attribute_public_radio)
        mProtectedRadio = activity.findViewById(R.id.attribute_protected_radio)
        mPrivateRadio = activity.findViewById(R.id.attribute_private_radio)
        mStaticCheck = activity.findViewById(R.id.attribute_static_check)
        mFinalCheck = activity.findViewById(R.id.attribute_final_check)
        mTypeSpinner = activity.findViewById(R.id.attribute_type_spinner)
        mTypeSpinner.tag = TYPE_SPINNER_TAG
        mSimpleRadio = activity.findViewById(R.id.attribute_simple_radio)
        mCollectionRadio = activity.findViewById(R.id.attribute_collection_radio)
        mArrayRadio = activity.findViewById(R.id.attribute_array_radio)

        // Enlazamos solo el TextInputEditText de dimensión y su contenedor (TextInputLayout) para ocultarlo/mostrarlo
        mDimEdit = activity.findViewById(R.id.attribute_dimension_input)
        mDimInputLayout = mDimEdit.parent.parent as TextInputLayout

        mOKButton = activity.findViewById(R.id.attribute_ok_button)
        mOKButton.tag = OK_BUTTON_TAG
        mOKButton.setOnClickListener(this)
        mCancelButton = activity.findViewById(R.id.attribute_cancel_button)
        mCancelButton.tag = CANCEL_BUTTON_TAG
        mCancelButton.setOnClickListener(this)
    }

    override fun initializeMembers() {
        mUmlClass = mCallback?.project?.findClassByOrder(mClassOrder) ?: return

        if (mAttributeOrder != -1) {
            mUmlClassAttribute = mUmlClass.findAttributeByOrder(mAttributeOrder) ?: return
        } else {
            mUmlClassAttribute = UmlClassAttribute(attributeOrder = mUmlClass.umlClassAttributeCount)
            mUmlClass.addAttribute(mUmlClassAttribute)
        }
    }

    override fun initializeFields() {
        if (mAttributeOrder != -1) {
            mAttributeNameInput.setText(mUmlClassAttribute.name)
            when (mUmlClassAttribute.visibility) {
                Visibility.PUBLIC -> mPublicRadio.isChecked = true
                Visibility.PROTECTED -> mProtectedRadio.isChecked = true
                else -> mPrivateRadio.isChecked = true
            }
            mStaticCheck.isChecked = mUmlClassAttribute.isStatic
            mFinalCheck.isChecked = mUmlClassAttribute.isFinal

            when (mUmlClassAttribute.typeMultiplicity) {
                TypeMultiplicity.SINGLE -> mSimpleRadio.isChecked = true
                TypeMultiplicity.COLLECTION -> mCollectionRadio.isChecked = true
                else -> mArrayRadio.isChecked = true
            }
            mDimEdit.setText(mUmlClassAttribute.arrayDimension.toString())

            if (mUmlClassAttribute.typeMultiplicity == TypeMultiplicity.ARRAY) setOnArrayDisplay() else setOnSingleDisplay()
        } else {
            mAttributeNameInput.setText("")
            mPublicRadio.isChecked = true
            mStaticCheck.isChecked = false
            mFinalCheck.isChecked = false
            mSimpleRadio.isChecked = true
            mDimEdit.setText("")
            setOnSingleDisplay()
        }
        populateTypeSpinner()
    }

    private fun populateTypeSpinner() {
        val spinnerArray = UmlType.umlTypes
            .filter { it.name != "void" }
            .mapNotNull { it.name }
            .sortedWith(TypeNameComparator())
            .toMutableList()

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, spinnerArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mTypeSpinner.adapter = adapter

        if (mAttributeOrder != -1) {
            mTypeSpinner.setSelection(spinnerArray.indexOf(mUmlClassAttribute.umlType?.name))
        }
    }

    private fun setOnEditDisplay() {
        mDeleteAttributeButton.visibility = View.VISIBLE
        mEditAttributeText.text = getString(R.string.title_edit_attribute)
    }

    private fun setOnCreateDisplay() {
        mDeleteAttributeButton.visibility = View.INVISIBLE
        mEditAttributeText.text = getString(R.string.title_create_attribute)
    }

    private fun setOnArrayDisplay() {
        // En lugar del viejo TextView, ocultamos/mostramos la caja moderna completa
        mDimInputLayout.visibility = View.VISIBLE
    }

    private fun setOnSingleDisplay() {
        mDimInputLayout.visibility = View.INVISIBLE
    }

    fun updateAttributeEditorFragment(attributeOrder: Int, classOrder: Int) {
        mAttributeOrder = attributeOrder
        mClassOrder = classOrder
        initializeMembers()
        initializeFields()
        if (mAttributeOrder == -1) setOnCreateDisplay() else setOnEditDisplay()

        if (mAttributeOrder != -1 && mUmlClassAttribute.typeMultiplicity == TypeMultiplicity.ARRAY) {
            setOnArrayDisplay()
        } else {
            setOnSingleDisplay()
        }
        setOnBackPressedCallback()
    }

    override fun closeFragment() {
        mCallback?.closeAttributeEditorFragment(this)
    }

    override fun onClick(v: View) {
        when (v.tag as? Int) {
            OK_BUTTON_TAG -> onOKButtonClicked()
            CANCEL_BUTTON_TAG -> onCancelButtonCLicked()
            DELETE_ATTRIBUTE_BUTTON_TAG -> startDeleteAttributeDialog()
        }
    }

    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        if (checkedId == R.id.attribute_array_radio) setOnArrayDisplay() else setOnSingleDisplay()
    }

    override fun createOrUpdateObject(): Boolean = createOrUpdateAttribute()

    override fun clearDraftObject() {
        if (mAttributeOrder == -1) mUmlClass.removeAttribute(mUmlClassAttribute)
    }

    private fun createOrUpdateAttribute(): Boolean {
        val name = attributeName
        if (name.isBlank()) {
            Toast.makeText(context, R.string.error_attribute_name_blank, Toast.LENGTH_SHORT).show()
            return false
        }

        val existingAttribute = mUmlClass.getAttribute(name)
        if (mUmlClass.containsAttributeNamed(name) && existingAttribute?.attributeOrder != mAttributeOrder) {
            Toast.makeText(context, R.string.error_name_already_used, Toast.LENGTH_SHORT).show()
            return false
        }

        mUmlClassAttribute.apply {
            this.name = name
            this.visibility = getVisibility()
            this.isStatic = getIsStatic()
            this.isFinal = getIsFinal()
            this.umlType = getType()
            this.typeMultiplicity = getMultiplicity()
            this.arrayDimension = getArrayDimension()
        }
        return true
    }

    private val attributeName: String get() = mAttributeNameInput.text.toString()

    private fun getVisibility(): Visibility = when {
        mPublicRadio.isChecked -> Visibility.PUBLIC
        mProtectedRadio.isChecked -> Visibility.PROTECTED
        else -> Visibility.PRIVATE
    }

    private fun getIsStatic(): Boolean = mStaticCheck.isChecked
    private fun getIsFinal(): Boolean = mFinalCheck.isChecked
    private fun getType(): UmlType? = UmlType.valueOf(mTypeSpinner.selectedItem.toString(), UmlType.umlTypes)

    private fun getMultiplicity(): TypeMultiplicity = when {
        mSimpleRadio.isChecked -> TypeMultiplicity.SINGLE
        mCollectionRadio.isChecked -> TypeMultiplicity.COLLECTION
        else -> TypeMultiplicity.ARRAY
    }

    private fun getArrayDimension(): Int {
        val text = mDimEdit.text.toString()
        return if (text.isBlank()) 0 else text.toInt()
    }

    private fun startDeleteAttributeDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_attribute_title)
            .setMessage(R.string.dialog_delete_attribute_message)
            .setNegativeButton(R.string.button_no, null)
            .setPositiveButton(R.string.button_yes) { _, _ ->
                mUmlClass.attributes.remove(mUmlClassAttribute)
                mCallback?.closeAttributeEditorFragment(this)
            }
            .show()
    }

    companion object {
        private const val ATTRIBUTE_ORDER_KEY = "attributeOrder"
        private const val CLASS_ORDER_KEY = "classOrder"
        private const val CLASS_EDITOR_FRAGMENT_TAG_KEY = "classEditorFragmentTag"
        private const val TYPE_SPINNER_TAG = 310
        private const val OK_BUTTON_TAG = 320
        private const val CANCEL_BUTTON_TAG = 330
        private const val DELETE_ATTRIBUTE_BUTTON_TAG = 340

        @JvmStatic
        fun newInstance(classEditorFragmentTag: String?, attributeOrder: Int, classOrder: Int): AttributeEditorFragment {
            return AttributeEditorFragment().apply {
                arguments = Bundle().apply {
                    putInt(ATTRIBUTE_ORDER_KEY, attributeOrder)
                    putInt(CLASS_ORDER_KEY, classOrder)
                    putString(CLASS_EDITOR_FRAGMENT_TAG_KEY, classEditorFragmentTag)
                }
            }
        }
    }
}