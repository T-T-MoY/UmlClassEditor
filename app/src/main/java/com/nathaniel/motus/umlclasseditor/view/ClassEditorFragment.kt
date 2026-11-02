package com.nathaniel.motus.umlclasseditor.view

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ExpandableListView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.nathaniel.motus.umlclasseditor.R
import com.nathaniel.motus.umlclasseditor.controller.CustomExpandableListViewAdapter
import com.nathaniel.motus.umlclasseditor.model.AdapterItem
import com.nathaniel.motus.umlclasseditor.model.AdapterItemComparator
import com.nathaniel.motus.umlclasseditor.model.AddItemString
import com.nathaniel.motus.umlclasseditor.model.UmlClass
import com.nathaniel.motus.umlclasseditor.model.UmlClassAttribute
import com.nathaniel.motus.umlclasseditor.model.UmlClassMethod
import com.nathaniel.motus.umlclasseditor.model.UmlEnumValue
import com.nathaniel.motus.umlclasseditor.model.UmlType

class ClassEditorFragment : EditorFragment(), View.OnClickListener,
    AdapterView.OnItemLongClickListener, RadioGroup.OnCheckedChangeListener,
    ExpandableListView.OnChildClickListener {

    private lateinit var mEditClassText: TextView
    private lateinit var mClassNameInput: TextInputEditText
    private lateinit var mDeleteClassButton: Button
    private lateinit var mClassTypeRadioGroup: RadioGroup
    private lateinit var mJavaRadio: RadioButton
    private lateinit var mAbstractRadio: RadioButton
    private lateinit var mInterfaceRadio: RadioButton
    private lateinit var mEnumRadio: RadioButton
    private lateinit var mMemberListView: ExpandableListView
    private lateinit var mOKButton: Button
    private lateinit var mCancelButton: Button

    private var mXPos = 0f
    private var mYPos = 0f
    private var mClassOrder = -1
    private lateinit var mUmlClass: UmlClass

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_class_editor, container, false)
    }

    override fun readBundle() {
        requireArguments().let {
            mXPos = it.getFloat(XPOS_KEY)
            mYPos = it.getFloat(YPOS_KEY)
            mClassOrder = it.getInt(CLASS_ORDER_KEY, -1)
        }
    }

    override fun setOnCreateOrEditDisplay() {
        if (mClassOrder == -1) setOnCreateDisplay() else setOnEditDisplay()
    }

    override fun configureViews() {
        val activity = requireActivity()
        mEditClassText = activity.findViewById(R.id.edit_class_text)

        // Ahora usamos el nuevo TextInputEditText que definimos en el XML Material Design
        mClassNameInput = activity.findViewById(R.id.class_name_input)

        mDeleteClassButton = activity.findViewById(R.id.delete_class_button)
        mDeleteClassButton.tag = DELETE_CLASS_BUTTON_TAG
        mDeleteClassButton.setOnClickListener(this)

        mClassTypeRadioGroup = activity.findViewById(R.id.class_type_radio_group)
        mClassTypeRadioGroup.setOnCheckedChangeListener(this)

        mJavaRadio = activity.findViewById(R.id.class_java_radio)
        mAbstractRadio = activity.findViewById(R.id.class_abstract_radio)
        mInterfaceRadio = activity.findViewById(R.id.class_interface_radio)
        mEnumRadio = activity.findViewById(R.id.class_enum_radio)

        mMemberListView = activity.findViewById(R.id.class_members_list)
        mMemberListView.tag = MEMBER_LIST_TAG
        mMemberListView.setOnChildClickListener(this)
        mMemberListView.onItemLongClickListener = this

        mOKButton = activity.findViewById(R.id.class_ok_button)
        mOKButton.tag = OK_BUTTON_TAG
        mOKButton.setOnClickListener(this)

        mCancelButton = activity.findViewById(R.id.class_cancel_button)
        mCancelButton.tag = CANCEL_BUTTON_TAG
        mCancelButton.setOnClickListener(this)
    }

    override fun initializeMembers() {
        val project = mCallback?.project ?: return

        if (mClassOrder != -1) {
            mUmlClass = project.findClassByOrder(mClassOrder) ?: return
        } else {
            mUmlClass = UmlClass(classOrder = project.umlClassCount)
            project.addUmlClass(mUmlClass)
        }
        sIsJavaClass = mUmlClass.umlClassType != UmlClass.UmlClassType.ENUM
    }

    override fun initializeFields() {
        if (mClassOrder != -1) {
            mClassNameInput.setText(mUmlClass.name)
            when (mUmlClass.umlClassType) {
                UmlClass.UmlClassType.JAVA_CLASS -> mJavaRadio.isChecked = true
                UmlClass.UmlClassType.ABSTRACT_CLASS -> mAbstractRadio.isChecked = true
                UmlClass.UmlClassType.INTERFACE -> mInterfaceRadio.isChecked = true
                UmlClass.UmlClassType.ENUM -> mEnumRadio.isChecked = true
            }
        } else {
            mClassNameInput.setText("")
            mJavaRadio.isChecked = true
        }
        updateLists()
    }

    private fun populateMemberListViewForJavaClass() {
        var attributeGroupIsExpanded = false
        var methodGroupIsExpanded = false

        if (mMemberListView.expandableListAdapter != null) {
            attributeGroupIsExpanded = mMemberListView.isGroupExpanded(0)
            methodGroupIsExpanded = mMemberListView.isGroupExpanded(1)
        }

        val attributeList: MutableList<AdapterItem> = mUmlClass.attributes.sortedWith(AdapterItemComparator()).toMutableList()
        attributeList.add(0, AddItemString(getString(R.string.new_attribute_string)))

        val methodList: MutableList<AdapterItem> = mUmlClass.methods.sortedWith(AdapterItemComparator()).toMutableList()
        methodList.add(0, AddItemString(getString(R.string.new_method_string)))

        val titles = listOf(getString(R.string.attributes_string), getString(R.string.methods_string))
        val hashMap = hashMapOf(
            getString(R.string.attributes_string) to attributeList.toList(),
            getString(R.string.methods_string) to methodList.toList()
        )

        mMemberListView.setAdapter(CustomExpandableListViewAdapter(requireContext(), titles, hashMap))

        if (attributeGroupIsExpanded) mMemberListView.expandGroup(0)
        if (methodGroupIsExpanded) mMemberListView.expandGroup(1)
    }

    private fun populateMemberListViewForEnum() {
        val valueList: MutableList<AdapterItem> = mUmlClass.values.sortedWith(AdapterItemComparator()).toMutableList()
        valueList.add(0, AddItemString(getString(R.string.new_value_string)))

        val titles = listOf(getString(R.string.values_string))
        val hashMap = hashMapOf(getString(R.string.values_string) to valueList.toList())

        mMemberListView.setAdapter(CustomExpandableListViewAdapter(requireContext(), titles, hashMap))
    }

    fun updateLists() {
        if (sIsJavaClass) populateMemberListViewForJavaClass() else populateMemberListViewForEnum()
    }

    private fun setOnEditDisplay() {
        mEditClassText.text = getString(R.string.title_edit_class)
        mDeleteClassButton.visibility = View.VISIBLE
    }

    private fun setOnCreateDisplay() {
        mEditClassText.text = getString(R.string.title_create_class)
        mDeleteClassButton.visibility = View.INVISIBLE
    }

    fun updateClassEditorFragment(xPos: Float, yPos: Float, classOrder: Int) {
        mXPos = xPos
        mYPos = yPos
        mClassOrder = classOrder
        initializeMembers()
        initializeFields()
        if (mClassOrder == -1) setOnCreateDisplay() else setOnEditDisplay()
        sIsJavaClass = mClassOrder == -1 || mUmlClass.umlClassType != UmlClass.UmlClassType.ENUM
        setOnBackPressedCallback()
    }

    override fun closeFragment() {
        mCallback?.closeClassEditorFragment(this)
    }

    override fun onClick(v: View) {
        when (v.tag as? Int) {
            OK_BUTTON_TAG -> onOKButtonClicked()
            CANCEL_BUTTON_TAG -> onCancelButtonCLicked()
            DELETE_CLASS_BUTTON_TAG -> startDeleteClassDialog()
        }
    }

    override fun onItemLongClick(parent: AdapterView<*>?, view: View, position: Int, id: Long): Boolean {
        val expandableListView = view.parent as ExpandableListView
        val pos = expandableListView.getExpandableListPosition(position)

        if (ExpandableListView.getPackedPositionType(pos) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            val groupPos = ExpandableListView.getPackedPositionGroup(pos)
            val childPos = ExpandableListView.getPackedPositionChild(pos)

            if (childPos != 0) {
                val groupName = expandableListView.expandableListAdapter.getGroup(groupPos) as String
                val item = expandableListView.expandableListAdapter.getChild(groupPos, childPos) as AdapterItem

                when (groupName) {
                    getString(R.string.values_string) -> startDeleteValueDialog((item as UmlEnumValue).valueOrder)
                    getString(R.string.attributes_string) -> startDeleteAttributeDialog((item as UmlClassAttribute).attributeOrder)
                    getString(R.string.methods_string) -> startDeleteMethodDialog((item as UmlClassMethod).methodOrder)
                }
            }
        }
        return true
    }

    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        sIsJavaClass = checkedId != R.id.class_enum_radio
        updateLists()
    }

    override fun onChildClick(expandableListView: ExpandableListView, view: View, groupPosition: Int, childPosition: Int, id: Long): Boolean {
        val title = expandableListView.expandableListAdapter.getGroup(groupPosition) as String
        val item = expandableListView.expandableListAdapter.getChild(groupPosition, childPosition) as AdapterItem

        if (childPosition == 0) {
            when (item.name) {
                getString(R.string.new_attribute_string) -> mCallback?.openAttributeEditorFragment(-1, mUmlClass.classOrder)
                getString(R.string.new_method_string) -> mCallback?.openMethodEditorFragment(-1, mUmlClass.classOrder)
                getString(R.string.new_value_string) -> startNewValueDialog()
            }
        } else {
            when (title) {
                getString(R.string.attributes_string) -> mCallback?.openAttributeEditorFragment((item as UmlClassAttribute).attributeOrder, mUmlClass.classOrder)
                getString(R.string.methods_string) -> mCallback?.openMethodEditorFragment((item as UmlClassMethod).methodOrder, mUmlClass.classOrder)
                getString(R.string.values_string) -> startRenameValueDialog((item as UmlEnumValue).valueOrder)
            }
        }
        return true
    }

    override fun clearDraftObject() {
        if (mClassOrder == -1) mCallback?.project?.removeUmlClass(mUmlClass)
    }

    override fun createOrUpdateObject(): Boolean = createOrUpdateClass()

    private fun createOrUpdateClass(): Boolean {
        val name = className
        val project = mCallback?.project ?: return false

        if (name.isBlank()) {
            Toast.makeText(context, R.string.error_class_name_blank, Toast.LENGTH_SHORT).show()
            return false
        }

        if (project.containsClassNamed(name) && project.getUmlClass(name)?.classOrder != mClassOrder) {
            Toast.makeText(context, R.string.error_class_name_exists_project, Toast.LENGTH_SHORT).show()
            return false
        }

        if (UmlType.containsUmlTypeNamed(name) && UmlType.valueOf(name, UmlType.umlTypes)?.typeLevel != UmlType.TypeLevel.PROJECT) {
            Toast.makeText(context, R.string.error_class_name_exists_type, Toast.LENGTH_SHORT).show()
            return false
        }

        mUmlClass.apply {
            this.name = name
            this.umlClassType = classType
            if (mClassOrder == -1) {
                this.umlClassNormalXPos = mXPos
                this.umlClassNormalYPos = mYPos
                this.upgradeToProjectUmlType() // finish declaring type
            }
        }
        return true
    }

    private val className: String get() = mClassNameInput.text.toString()

    private val classType: UmlClass.UmlClassType
        get() = when {
            mJavaRadio.isChecked -> UmlClass.UmlClassType.JAVA_CLASS
            mAbstractRadio.isChecked -> UmlClass.UmlClassType.ABSTRACT_CLASS
            mInterfaceRadio.isChecked -> UmlClass.UmlClassType.INTERFACE
            else -> UmlClass.UmlClassType.ENUM
        }

    // **********************************************************************************************
    // Alert dialogs
    // **********************************************************************************************
    private fun startNewValueDialog() {
        val input = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_add_value_title)
            .setMessage(R.string.dialog_add_value_message)
            .setView(input)
            .setNegativeButton(R.string.button_cancel, null)
            .setPositiveButton(R.string.button_ok) { _, _ ->
                mUmlClass.addValue(UmlEnumValue(input.text.toString(), mUmlClass.valueCount))
                updateLists()
            }
            .show()
    }

    private fun startDeleteClassDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_class_title)
            .setMessage(R.string.dialog_delete_class_message)
            .setNegativeButton(R.string.button_no, null)
            .setPositiveButton(R.string.button_yes) { _, _ ->
                mCallback?.project?.removeUmlClass(mUmlClass)
                mCallback?.closeClassEditorFragment(this)
            }
            .show()
    }

    private fun startRenameValueDialog(valueOrder: Int) {
        val editText = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setView(editText)
            .setTitle(R.string.dialog_rename_value_title)
            .setMessage(R.string.dialog_rename_value_message)
            .setNegativeButton(R.string.button_cancel, null)
            .setPositiveButton(R.string.button_ok) { _, _ ->
                mUmlClass.findValueByOrder(valueOrder)?.name = editText.text.toString()
                updateLists()
            }
            .show()
    }

    private fun startDeleteValueDialog(valueOrder: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_value_title)
            .setMessage(R.string.dialog_delete_value_message)
            .setNegativeButton(R.string.button_no, null)
            .setPositiveButton(R.string.button_yes) { _, _ ->
                mUmlClass.findValueByOrder(valueOrder)?.let { mUmlClass.removeValue(it) }
                updateLists()
            }
            .show()
    }

    private fun startDeleteAttributeDialog(attributeOrder: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_attribute_title)
            .setMessage(R.string.dialog_delete_attribute_message)
            .setNegativeButton(R.string.button_no, null)
            .setPositiveButton(R.string.button_yes) { _, _ ->
                mUmlClass.findAttributeByOrder(attributeOrder)?.let { mUmlClass.removeAttribute(it) }
                updateLists()
            }
            .show()
    }

    private fun startDeleteMethodDialog(methodOrder: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_method_title)
            .setMessage(R.string.dialog_delete_method_message)
            .setNegativeButton(R.string.button_no, null)
            .setPositiveButton(R.string.button_yes) { _, _ ->
                mUmlClass.findMethodByOrder(methodOrder)?.let { mUmlClass.removeMethod(it) }
                updateLists()
            }
            .show()
    }

    companion object {
        private var sIsJavaClass = true
        private const val NEW_ATTRIBUTE_BUTTON_TAG = 210
        private const val MEMBER_LIST_TAG = 220
        private const val NEW_METHOD_BUTTON_TAG = 230
        private const val METHOD_LIST_TAG = 240
        private const val OK_BUTTON_TAG = 250
        private const val CANCEL_BUTTON_TAG = 260
        private const val DELETE_CLASS_BUTTON_TAG = 270
        private const val NEW_VALUE_BUTTON_TAG = 280
        private const val VALUE_LIST_TAG = 290
        private const val XPOS_KEY = "xPos"
        private const val YPOS_KEY = "yPos"
        private const val CLASS_ORDER_KEY = "classOrder"

        @JvmStatic
        fun newInstance(xPos: Float, yPos: Float, classOrder: Int): ClassEditorFragment {
            return ClassEditorFragment().apply {
                arguments = Bundle().apply {
                    putFloat(XPOS_KEY, xPos)
                    putFloat(YPOS_KEY, yPos)
                    putInt(CLASS_ORDER_KEY, classOrder)
                }
            }
        }
    }
}