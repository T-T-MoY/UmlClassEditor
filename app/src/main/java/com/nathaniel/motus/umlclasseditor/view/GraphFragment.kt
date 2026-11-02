package com.nathaniel.motus.umlclasseditor.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.nathaniel.motus.umlclasseditor.R
import com.nathaniel.motus.umlclasseditor.controller.FragmentObserver
import com.nathaniel.motus.umlclasseditor.model.UmlClass
import com.nathaniel.motus.umlclasseditor.model.UmlRelation.UmlRelationType

class GraphFragment : Fragment(), View.OnClickListener {

    // Vistas seguras con lateinit
    private lateinit var mGraphView: GraphView
    private lateinit var mGraphText: TextView
    private lateinit var mInheritanceButton: ImageButton
    private lateinit var mRealizationButton: ImageButton
    private lateinit var mAggregationButton: ImageButton
    private lateinit var mEscapeButton: Button
    private lateinit var mAssociationButton: ImageButton
    private lateinit var mDependancyButton: ImageButton
    private lateinit var mCompositionButton: ImageButton
    private lateinit var mDirectedAssociationButton: ImageButton
    private lateinit var mBidirectionalAssociationButton: ImageButton
    private lateinit var mNewClassButton: Button

    var isExpectingTouchLocation: Boolean = false
    var isExpectingStartClass: Boolean = false
    var isExpectingEndClass: Boolean = false
    var startClass: UmlClass? = null
    var endClass: UmlClass? = null
    var umlRelationType: UmlRelationType? = null

    var callBack: FragmentObserver? = null
        private set

    //    **********************************************************************************************
    //    Fragment events
    //    **********************************************************************************************
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_graph, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureViews(view)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callBack = context as? FragmentObserver
    }

    override fun onDetach() {
        super.onDetach()
        callBack = null
    }

    //    **********************************************************************************************
    //    Setup methods
    //    **********************************************************************************************
    private fun configureViews(view: View) {
        // En lugar de llamar a requireActivity(), buscamos directamente en la vista inflada (es más rápido y seguro)
        mGraphView = view.findViewById(R.id.graphview)
        mGraphView.tag = GRAPHVIEW_TAG
        mGraphView.setGraphFragment(this)

        mGraphText = view.findViewById(R.id.graph_text)

        mInheritanceButton = view.findViewById(R.id.inheritance_button)
        mInheritanceButton.tag = INHERITANCE_BUTTON_TAG
        mInheritanceButton.setOnClickListener(this)

        mRealizationButton = view.findViewById(R.id.realization_button)
        mRealizationButton.tag = REALIZATION_BUTTON_TAG
        mRealizationButton.setOnClickListener(this)

        mAggregationButton = view.findViewById(R.id.aggregation_button)
        mAggregationButton.tag = AGGREGATION_BUTTON_TAG
        mAggregationButton.setOnClickListener(this)

        mEscapeButton = view.findViewById(R.id.escape_button)
        mEscapeButton.tag = ESCAPE_BUTTON_TAG
        mEscapeButton.setOnClickListener(this)

        mAssociationButton = view.findViewById(R.id.association_button)
        mAssociationButton.tag = ASSOCIATION_BUTTON_TAG
        mAssociationButton.setOnClickListener(this)

        mDependancyButton = view.findViewById(R.id.dependency_button)
        mDependancyButton.tag = DEPENDENCY_BUTTON_TAG
        mDependancyButton.setOnClickListener(this)

        mCompositionButton = view.findViewById(R.id.composition_button)
        mCompositionButton.tag = COMPOSITION_BUTTON_TAG
        mCompositionButton.setOnClickListener(this)

        mDirectedAssociationButton = view.findViewById(R.id.directed_association_button)
        mDirectedAssociationButton.tag = DIRECTED_ASSOCIATION_BUTTON_TAG
        mDirectedAssociationButton.setOnClickListener(this)

        mBidirectionalAssociationButton = view.findViewById(R.id.bidirectional_association_button)
        mBidirectionalAssociationButton.tag = BIDIRECTIONAL_ASSOCIATION_BUTTON_TAG
        mBidirectionalAssociationButton.setOnClickListener(this)

        mNewClassButton = view.findViewById(R.id.new_class_button)
        mNewClassButton.tag = NEW_CLASS_BUTTON_TAG
        mNewClassButton.setOnClickListener(this)
    }

    //    **********************************************************************************************
    //    Modifiers
    //    **********************************************************************************************
    fun setPrompt(prompt: String?) {
        mGraphText.text = prompt
    }

    fun clearPrompt() {
        mGraphText.text = ""
    }

    //    **********************************************************************************************
    //    Listener methods
    //    **********************************************************************************************
    override fun onClick(v: View) {
        when (v.tag as? Int) {
            NEW_CLASS_BUTTON_TAG -> {
                isExpectingTouchLocation = true
                setPrompt(getString(R.string.graph_prompt_locate_class))
            }
            INHERITANCE_BUTTON_TAG -> startRelation(UmlRelationType.INHERITANCE)
            REALIZATION_BUTTON_TAG -> startRelation(UmlRelationType.REALIZATION)
            AGGREGATION_BUTTON_TAG -> startRelation(UmlRelationType.AGGREGATION)
            ASSOCIATION_BUTTON_TAG -> startRelation(UmlRelationType.ASSOCIATION)
            DIRECTED_ASSOCIATION_BUTTON_TAG -> startRelation(UmlRelationType.DIRECTED_ASSOCIATION)
            BIDIRECTIONAL_ASSOCIATION_BUTTON_TAG -> startRelation(UmlRelationType.BIDIRECTIONAL_ASSOCIATION)
            COMPOSITION_BUTTON_TAG -> startRelation(UmlRelationType.COMPOSITION)
            DEPENDENCY_BUTTON_TAG -> startRelation(UmlRelationType.DEPENDENCY)
            ESCAPE_BUTTON_TAG -> clearInput()
        }
    }

    private fun startRelation(relationType: UmlRelationType) {
        isExpectingStartClass = true
        isExpectingEndClass = false
        umlRelationType = relationType
        setPrompt(getString(R.string.graph_prompt_choose_start_class))
    }

    private fun clearInput() {
        isExpectingEndClass = false
        isExpectingStartClass = false
        isExpectingTouchLocation = false
        clearPrompt()
    }

    companion object {
        const val GRAPHVIEW_TAG = 110
        const val NEW_CLASS_BUTTON_TAG = 120
        const val INHERITANCE_BUTTON_TAG = 130
        const val REALIZATION_BUTTON_TAG = 140
        const val AGGREGATION_BUTTON_TAG = 150
        const val ESCAPE_BUTTON_TAG = 160
        const val ASSOCIATION_BUTTON_TAG = 170
        const val DEPENDENCY_BUTTON_TAG = 180
        const val COMPOSITION_BUTTON_TAG = 190
        const val DIRECTED_ASSOCIATION_BUTTON_TAG = 200
        const val BIDIRECTIONAL_ASSOCIATION_BUTTON_TAG = 210

        @JvmStatic
        fun newInstance(): GraphFragment = GraphFragment()
    }
}