package com.nathaniel.motus.umlclasseditor.view

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ExpandableListView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.nathaniel.motus.umlclasseditor.R
import com.nathaniel.motus.umlclasseditor.controller.CustomExpandableListViewAdapter
import com.nathaniel.motus.umlclasseditor.model.AdapterItem
import com.nathaniel.motus.umlclasseditor.model.AdapterItemComparator
import com.nathaniel.motus.umlclasseditor.model.AddItemString
import com.nathaniel.motus.umlclasseditor.model.MethodParameter
import com.nathaniel.motus.umlclasseditor.model.TypeMultiplicity
import com.nathaniel.motus.umlclasseditor.model.TypeNameComparator
import com.nathaniel.motus.umlclasseditor.model.UmlClass
import com.nathaniel.motus.umlclasseditor.model.UmlClassMethod
import com.nathaniel.motus.umlclasseditor.model.UmlType
import com.nathaniel.motus.umlclasseditor.model.Visibility

class MethodEditorFragment : EditorFragment(), View.OnClickListener, RadioGroup.OnCheckedChangeListener,
    AdapterView.OnItemLongClickListener, ExpandableListView.OnChildClickListener {

    private var mMethodOrder = -1
    private var mClassOrder = -1
    private var mClassEditorFragmentTag: String? = null

    private lateinit var mUmlClassMethod: UmlClassMethod
    private lateinit var mUmlClass: UmlClass

    // UI Elements
    private lateinit var mEditMethodText: TextView
    private lateinit var mDeleteMethodButton: Button
    private lateinit var mMethodNameInput: TextInputEditText
    private lateinit var mPublicRadio: RadioButton
    private lateinit var mProtectedRadio: RadioButton
    private lateinit var mPrivateRadio: RadioButton
    private lateinit var mStaticCheck: CheckBox
    private lateinit var mTypeSpinner: Spinner
    private lateinit var mMethodMultiplicityRadioGroup: RadioGroup
    private lateinit var mSingleRadio: RadioButton
    private lateinit var mCollectionRadio: RadioButton
    private lateinit var mArrayRadio: RadioButton

    // Referencia al contenedor de dimensión (TextInputLayout)
    private lateinit var mDimInputLayout: TextInputLayout
    private lateinit var mDimEdit: TextInputEditText

    private lateinit var mParameterList: ExpandableListView
    private lateinit var mCancelButton: Button
    private lateinit var mOKButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_method_editor, container, false)
    }

    override fun readBundle() {
        requireArguments().let {
            mClassEditorFragmentTag = it.getString(CLASS_EDITOR_FRAGMENT_TAG_KEY)
            mMethodOrder = it.getInt(METHOD_ORDER_KEY, -1)
            mClassOrder = it.getInt(CLASS_ORDER_KEY, -1)
        }
    }

    override fun setOnCreateOrEditDisplay() {
        if (mMethodOrder != -1) setOnEditDisplay() else setOnCreateDisplay()
    }

    override fun configureViews() {
        val activity = requireActivity()
        mEditMethodText = activity.findViewById(R.id.edit_method_text)

        mDeleteMethodButton = activity.findViewById(R.id.delete_method_button)
        mDeleteMethodButton.tag = DELETE_METHOD_BUTTON_TAG
        mDeleteMethodButton.setOnClickListener(this)

        mMethodNameInput = activity.findViewById(R.id.method_name_input)
        mPublicRadio = activity.findViewById(R.id.method_public_radio)
        mProtectedRadio = activity.findViewById(R.id.method_protected_radio)
        mPrivateRadio = activity.findViewById(R.id.method_private_radio)
        mStaticCheck = activity.findViewById(R.id.method_static_check)
        mTypeSpinner = activity.findViewById(R.id.method_type_spinner)

        mMethodMultiplicityRadioGroup = activity.findViewById(R.id.method_multiplicity_radio_group)
        mMethodMultiplicityRadioGroup.setOnCheckedChangeListener(this)

        mSingleRadio = activity.findViewById(R.id.method_simple_radio)
        mCollectionRadio = activity.findViewById(R.id.method_collection_radio)
        mArrayRadio = activity.findViewById(R.id.method_array_radio)

        mDimEdit = activity.findViewById(R.id.method_dimension_input)
        mDimInputLayout = mDimEdit.parent.parent as TextInputLayout

        mParameterList = activity.findViewById(R.id.method_parameters_list)
        mParameterList.setOnChildClickListener(this)
        mParameterList.onItemLongClickListener = this

        mCancelButton = activity.findViewById(R.id.method_cancel_button)
        mCancelButton.tag = CANCEL_BUTTON_TAG
        mCancelButton.setOnClickListener(this)

        mOKButton = activity.findViewById(R.id.method_ok_button)
        mOKButton.tag = OK_BUTTON_TAG
        mOKButton.setOnClickListener(this)
    }

    override fun initializeMembers() {
        val project = mCallback?.project ?: return
        mUmlClass = project.findClassByOrder(mClassOrder) ?: return

        if (mMethodOrder != -1) {
            mUmlClassMethod = mUmlClass.findMethodByOrder(mMethodOrder) ?: return
        } else {
            mUmlClassMethod = UmlClassMethod(methodOrder = mUmlClass.umlClassMethodCount)
            mUmlClass.addMethod(mUmlClassMethod)
        }
    }

    override fun initializeFields() {
        if (mMethodOrder != -1) {
            mMethodNameInput.setText(mUmlClassMethod.name)
            when (mUmlClassMethod.visibility) {
                Visibility.PUBLIC -> mPublicRadio.isChecked = true
                Visibility.PROTECTED -> mProtectedRadio.isChecked = true
                else -> mPrivateRadio.isChecked = true
            }
            mStaticCheck.isChecked = mUmlClassMethod.isStatic
            when (mUmlClassMethod.typeMultiplicity) {
                TypeMultiplicity.SINGLE -> mSingleRadio.isChecked = true
                TypeMultiplicity.COLLECTION -> mCollectionRadio.isChecked = true
                else -> mArrayRadio.isChecked = true
            }
            mDimEdit.setText(mUmlClassMethod.arrayDimension.toString())
            if (mUmlClassMethod.typeMultiplicity == TypeMultiplicity.ARRAY) setOnArrayDisplay() else setOnSingleDisplay()
        } else {
            mMethodNameInput.setText("")
            mPublicRadio.isChecked = true
            mStaticCheck.isChecked = false
            mSingleRadio.isChecked = true
            mDimEdit.setText("")
            setOnSingleDisplay()
        }
        populateTypeSpinner()
        populateParameterListView()
    }

    private fun populateTypeSpinner() {
        val spinnerArray = UmlType.umlTypes.mapNotNull { it.name }.sortedWith(TypeNameComparator()).toMutableList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, spinnerArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mTypeSpinner.adapter = adapter

        val selectionName = if (mMethodOrder != -1) mUmlClassMethod.umlType?.name else "void"
        val index = spinnerArray.indexOf(selectionName)
        if (index >= 0) mTypeSpinner.setSelection(index)
    }

    private fun populateParameterListView() {
        val parameterGroupIsExpanded = mParameterList.expandableListAdapter != null && mParameterList.isGroupExpanded(0)

        val parameterList: MutableList<AdapterItem> = mUmlClassMethod.parameters.sortedWith(AdapterItemComparator()).toMutableList()
        parameterList.add(0, AddItemString(getString(R.string.new_parameter_string)))

        val title = listOf(getString(R.string.parameters_string))
        val hashMap = hashMapOf(getString(R.string.parameters_string) to parameterList.toList())

        mParameterList.setAdapter(CustomExpandableListViewAdapter(requireContext(), title, hashMap))
        if (parameterGroupIsExpanded) mParameterList.expandGroup(0)
    }

    private fun setOnEditDisplay() {
        mEditMethodText.text = getString(R.string.title_edit_method)
        mDeleteMethodButton.visibility = View.VISIBLE
    }

    private fun setOnCreateDisplay() {
        mEditMethodText.text = getString(R.string.title_create_method)
        mDeleteMethodButton.visibility = View.INVISIBLE
    }

    private fun setOnArrayDisplay() {
        mDimInputLayout.visibility = View.VISIBLE
    }

    private fun setOnSingleDisplay() {
        mDimInputLayout.visibility = View.INVISIBLE
    }

    fun updateMethodEditorFragment(methodOrder: Int, classOrder: Int) {
        mMethodOrder = methodOrder
        mClassOrder = classOrder
        initializeMembers()
        initializeFields()
        if (mMethodOrder != -1) setOnEditDisplay() else setOnCreateDisplay()

        if (mMethodOrder != -1 && mUmlClassMethod.typeMultiplicity == TypeMultiplicity.ARRAY) {
            setOnArrayDisplay()
        } else {
            setOnSingleDisplay()
        }
        setOnBackPressedCallback()
    }

    override fun closeFragment() {
        mCallback?.closeMethodEditorFragment(this)
    }

    override fun onClick(v: View) {
        when (v.tag as? Int) {
            CANCEL_BUTTON_TAG -> onCancelButtonCLicked()
            OK_BUTTON_TAG -> onOKButtonClicked()
            DELETE_METHOD_BUTTON_TAG -> startDeleteMethodDialog()
        }
    }

    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        if (checkedId == R.id.method_array_radio) setOnArrayDisplay() else setOnSingleDisplay()
    }

    override fun onItemLongClick(adapterView: AdapterView<*>?, view: View, position: Int, id: Long): Boolean {
        val expandableListView = view.parent as ExpandableListView
        val pos = expandableListView.getExpandableListPosition(position)

        if (ExpandableListView.getPackedPositionType(pos) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            val groupPos = ExpandableListView.getPackedPositionGroup(pos)
            val childPos = ExpandableListView.getPackedPositionChild(pos)

            if (childPos != 0) {
                val item = expandableListView.expandableListAdapter.getChild(groupPos, childPos) as AdapterItem
                startDeleteParameterDialog((item as MethodParameter).parameterOrder)
            }
        }
        return true
    }

    override fun onChildClick(expandableListView: ExpandableListView, view: View, groupPosition: Int, childPosition: Int, id: Long): Boolean {
        val item = expandableListView.expandableListAdapter.getChild(groupPosition, childPosition) as AdapterItem

        if (item.name == getString(R.string.new_parameter_string) && childPosition == 0) {
            mCallback?.openParameterEditorFragment(-1, mUmlClassMethod.methodOrder, mUmlClass.classOrder)
        } else {
            mCallback?.openParameterEditorFragment((item as MethodParameter).parameterOrder, mUmlClassMethod.methodOrder, mUmlClass.classOrder)
        }
        return true
    }

    override fun clearDraftObject() {
        if (mMethodOrder == -1) mUmlClass.removeMethod(mUmlClassMethod)
    }

    override fun createOrUpdateObject(): Boolean = createOrUpdateMethod()

    private fun createOrUpdateMethod(): Boolean {
        val name = methodName
        if (name.isBlank()) {
            Toast.makeText(context, R.string.error_method_name_blank, Toast.LENGTH_SHORT).show()
            return false
        }

        mUmlClassMethod.apply {
            this.name = name
            this.visibility = methodVisibility
            this.isStatic = isMethodStatic
            this.umlType = methodType
            this.typeMultiplicity = methodMultiplicity
            this.arrayDimension = methodArrayDimension
        }

        val existingMethod = mUmlClass.methods.firstOrNull { it.isEquivalentTo(mUmlClassMethod) && it !== mUmlClassMethod }
        if (existingMethod != null) {
            Toast.makeText(context, R.string.error_method_already_defined, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private val methodName: String get() = mMethodNameInput.text.toString()

    private val methodVisibility: Visibility
        get() = when {
            mPublicRadio.isChecked -> Visibility.PUBLIC
            mPrivateRadio.isChecked -> Visibility.PROTECTED
            else -> Visibility.PRIVATE
        }

    private val isMethodStatic: Boolean get() = mStaticCheck.isChecked
    private val methodType: UmlType? get() = UmlType.valueOf(mTypeSpinner.selectedItem.toString(), UmlType.umlTypes)
    private val methodMultiplicity: TypeMultiplicity
        get() = when {
            mSingleRadio.isChecked -> TypeMultiplicity.SINGLE
            mCollectionRadio.isChecked -> TypeMultiplicity.COLLECTION
            else -> TypeMultiplicity.ARRAY
        }

    private val methodArrayDimension: Int
        get() {
            val text = mDimEdit.text.toString()
            return if (text.isBlank()) 0 else text.toInt()
        }

    fun updateLists() {
        populateParameterListView()
    }

    private fun startDeleteMethodDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_method_title)
            .setMessage(R.string.dialog_delete_method_message)
            .setNegativeButton(R.string.button_no, null)
            .setPositiveButton(R.string.button_yes) { _, _ ->
                mUmlClass.removeMethod(mUmlClassMethod)
                mCallback?.closeMethodEditorFragment(this)
            }
            .show()
    }

    private fun startDeleteParameterDialog(parameterIndex: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_parameter_title)
            .setMessage(R.string.dialog_delete_parameter_message)
            .setNegativeButton(R.string.button_no, null)
            .setPositiveButton(R.string.button_yes) { _, _ ->
                mUmlClassMethod.findParameterByOrder(parameterIndex)?.let { mUmlClassMethod.removeParameter(it) }
                updateLists()
            }
            .show()
    }

    companion object {
        private const val METHOD_ORDER_KEY = "methodOrder"
        private const val CLASS_ORDER_KEY = "classOrder"
        private const val CLASS_EDITOR_FRAGMENT_TAG_KEY = "classEditorFragmentTag"
        private const val DELETE_METHOD_BUTTON_TAG = 410
        private const val CANCEL_BUTTON_TAG = 420
        private const val OK_BUTTON_TAG = 430

        @JvmStatic
        fun newInstance(classEditorFragmentTag: String?, methodOrder: Int, classOrder: Int): MethodEditorFragment {
            return MethodEditorFragment().apply {
                arguments = Bundle().apply {
                    putString(CLASS_EDITOR_FRAGMENT_TAG_KEY, classEditorFragmentTag)
                    putInt(METHOD_ORDER_KEY, methodOrder)
                    putInt(CLASS_ORDER_KEY, classOrder)
                }
            }
        }
    }
}