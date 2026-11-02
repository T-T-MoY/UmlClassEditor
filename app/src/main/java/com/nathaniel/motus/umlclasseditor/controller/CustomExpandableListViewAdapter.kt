package com.nathaniel.motus.umlclasseditor.controller

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import com.nathaniel.motus.umlclasseditor.R
import com.nathaniel.motus.umlclasseditor.model.AdapterItem

class CustomExpandableListViewAdapter(
    private val mContext: Context,
    private val mGroups: List<String>,
    private val mChildren: HashMap<String, List<AdapterItem>>
) : BaseExpandableListAdapter() {

    // En Kotlin, las funciones de una sola línea se pueden simplificar con el signo '='
    override fun getGroupCount(): Int = mGroups.size

    override fun getChildrenCount(groupPosition: Int): Int = mChildren[mGroups[groupPosition]]?.size ?: 0

    override fun getGroup(groupPosition: Int): Any = mGroups[groupPosition]

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return mChildren[mGroups[groupPosition]]!![childPosition]
    }

    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()

    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()

    override fun hasStableIds(): Boolean = false

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup): View {
        val title = mGroups[groupPosition]

        // El operador Elvis (?:) elimina la necesidad de hacer "if (view == null)"
        val view = convertView ?: LayoutInflater.from(mContext).inflate(R.layout.list_group, parent, false)

        val textViewGroup = view.findViewById<TextView>(R.id.group_text)
        textViewGroup.text = title

        return view
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val item = (getChild(groupPosition, childPosition) as AdapterItem).name

        val view = convertView ?: LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false)

        val childText = view.findViewById<TextView>(R.id.child_text)
        childText.text = item

        return view
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true
}