package com.nathaniel.motus.umlclasseditor.view

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.nathaniel.motus.umlclasseditor.R
import com.nathaniel.motus.umlclasseditor.model.UmlClass
import com.nathaniel.motus.umlclasseditor.model.UmlClass.UmlClassType
import com.nathaniel.motus.umlclasseditor.model.UmlProject
import com.nathaniel.motus.umlclasseditor.model.UmlRelation
import com.nathaniel.motus.umlclasseditor.model.UmlRelation.UmlRelationType
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.withSign

class GraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), View.OnTouchListener {

    internal enum class TouchMode {
        DRAG, ZOOM
    }

    private var mGraphFragment: GraphFragment? = null
    private var mZoom = 1f
    private var mXOffset = 0f
    private var mYOffset = 0f
    private var mUmlProject: UmlProject? = null

    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mOldDist = 0f
    private var mNewDist = 0f
    private var mTouchMode = TouchMode.DRAG
    private var mPrimaryPointerIndex = 0
    private var mXMidPoint = 0f
    private var mYMidpoint = 0f
    private var mOldXMidPoint = 0f
    private var mOldYMidPoint = 0f

    private var mMovingClass: UmlClass? = null
    private var mCallback: GraphViewObserver? = null
    private var mActionDownEventTime: Long = 0
    private var mFirstClickTime: Long = 0
    private var mFirstClickX = 0f
    private var mFirstClickY = 0f

    // ¡LA CLAVE DE LA OPTIMIZACIÓN! Un solo Path reutilizable para toda la clase.
    private val mSharedPath = Path()

    // Colores resueltos desde el tema actual (claro u oscuro)
    private val textColor = ContextCompat.getColor(context, R.color.graph_foreground)
    private val lineColor = ContextCompat.getColor(context, R.color.graph_line)
    private val solidFillColor = ContextCompat.getColor(context, R.color.graph_solid_fill)
    private val backgroundColor = ContextCompat.getColor(context, R.color.graph_background)

    private val plainTextPaint = Paint().apply { color = textColor }
    private val italicTextPaint = Paint().apply {
        color = textColor
        typeface = Typeface.defaultFromStyle(Typeface.ITALIC)
    }
    private val underlinedTextPaint = Paint().apply {
        color = textColor
        flags = Paint.UNDERLINE_TEXT_FLAG
    }
    private val linePaint = Paint().apply {
        color = lineColor
        style = Paint.Style.STROKE
    }
    private val dashPaint = Paint().apply {
        color = lineColor
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }
    private val solidBlackPaint = Paint().apply {
        color = lineColor
        style = Paint.Style.FILL_AND_STROKE
    }
    private val solidFillPaint = Paint().apply {
        color = solidFillColor
        style = Paint.Style.FILL
    }

    init {
        attrs?.let {
            val attr = context.obtainStyledAttributes(it, R.styleable.GraphView)
            mZoom = attr.getFloat(R.styleable.GraphView_zoom, 1f)
            mXOffset = attr.getInt(R.styleable.GraphView_xOffset, 0).toFloat()
            mYOffset = attr.getInt(R.styleable.GraphView_yOffset, 0).toFloat()
            attr.recycle()
        }
        setOnTouchListener(this)
        mCallback = context as? GraphViewObserver
    }

    interface GraphViewObserver {
        val isExpectingTouchLocation: Boolean
        fun createClass(xLocation: Float, yLocation: Float)
        fun editClass(umlClass: UmlClass?)
        fun createRelation(startClass: UmlClass?, endClass: UmlClass?, relationType: UmlRelationType?)
    }

    fun createBitmapFromView(): Bitmap? {
        val project = mUmlProject ?: return null
        if (project.umlClasses.isEmpty()) return null

        val originalZoom = mZoom
        val originalXOffset = mXOffset
        val originalYOffset = mYOffset

        // Calculamos las dimensiones "normales" (sin zoom) de todas las clases.
        // Hay que fijar también el tamaño de fuente base; de lo contrario se mediría
        // con el tamaño del zoom actual y el bitmap quedaría con espacio de sobra.
        mZoom = 1f
        plainTextPaint.textSize = FONT_SIZE
        italicTextPaint.textSize = FONT_SIZE
        underlinedTextPaint.textSize = FONT_SIZE
        project.umlClasses.forEach { updateUmlClassNormalDimensions(it) }

        val projectLeft = absoluteProjectLeft
        val projectTop = absoluteProjectTop
        val projectWidth = max(absoluteProjectWidth, 1f)
        val projectHeight = max(absoluteProjectHeight, 1f)

        val availableWidth = (EXPORT_MAX_DIMENSION - 2 * EXPORT_PADDING).toFloat()
        val availableHeight = (EXPORT_MAX_DIMENSION - 2 * EXPORT_PADDING).toFloat()
        val scale = min(EXPORT_MAX_SCALE, min(availableWidth / projectWidth, availableHeight / projectHeight))

        val bitmapWidth = ((projectWidth * scale) + 2 * EXPORT_PADDING).toInt().coerceAtLeast(1)
        val bitmapHeight = ((projectHeight * scale) + 2 * EXPORT_PADDING).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundColor)

        // Encuadramos todo el proyecto dentro del bitmap (no solo la zona visible).
        mZoom = scale
        mXOffset = EXPORT_PADDING - projectLeft * scale
        mYOffset = EXPORT_PADDING - projectTop * scale

        try {
            draw(canvas)
        } finally {
            mZoom = originalZoom
            mXOffset = originalXOffset
            mYOffset = originalYOffset
            invalidate()
        }

        return bitmap
    }

    fun setUmlProject(umlProject: UmlProject?) {
        mUmlProject = umlProject
        mUmlProject?.let {
            mZoom = it.zoom
            mXOffset = it.xOffset
            mYOffset = it.yOffset
        }
        invalidate()
    }

    fun setGraphFragment(graphFragment: GraphFragment?) { mGraphFragment = graphFragment }
    fun setZoom(zoom: Float) { mZoom = zoom }
    fun setXOffset(XOffset: Float) { mXOffset = XOffset }
    fun setYOffset(YOffset: Float) { mYOffset = YOffset }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mUmlProject?.let { project ->

            // Actualizamos el tamaño de fuente una sola vez por frame, no por cada clase.
            val scaledFontSize = FONT_SIZE * mZoom
            plainTextPaint.textSize = scaledFontSize
            italicTextPaint.textSize = scaledFontSize
            underlinedTextPaint.textSize = scaledFontSize

            updateProjectGeometricalParameters()
            project.umlClasses.forEach { drawUmlClass(canvas, it) }
            project.umlRelations.forEach { drawRelation(canvas, it) }
        }
    }

    private fun drawUmlClass(canvas: Canvas, umlClass: UmlClass) {
        updateUmlClassNormalDimensions(umlClass)
        drawUmlClassBorder(canvas, umlClass)
        drawUmlClassHeader(canvas, umlClass)

        if (umlClass.umlClassType == UmlClassType.ENUM) {
            drawValueBox(canvas, umlClass)
        } else {
            drawAttributeBox(canvas, umlClass)
            drawMethodBox(canvas, umlClass)
        }
    }

    /**
     * Dibuja el contorno del cuadro de clase con esquinas redondeadas y las
     * líneas separadoras de sus compartimentos.
     */
    private fun drawUmlClassBorder(canvas: Canvas, umlClass: UmlClass) {
        val left = visibleX(umlClass.umlClassNormalXPos)
        val top = visibleY(umlClass.umlClassNormalYPos)
        val right = visibleX(umlClass.umlClassNormalXPos + umlClass.umlClassNormalWidth)
        val bottom = visibleY(umlClass.umlClassNormalYPos + umlClass.umlClassNormalHeight)
        val radius = min(CORNER_RADIUS * mZoom, min(right - left, bottom - top) / 2f)

        canvas.drawRoundRect(left, top, right, bottom, radius, radius, linePaint)

        val headerBottom = umlClass.umlClassNormalYPos + getClassHeaderNormalHeight(umlClass)
        mSharedPath.reset()
        mSharedPath.moveTo(left, visibleY(headerBottom))
        mSharedPath.lineTo(right, visibleY(headerBottom))

        if (umlClass.umlClassType != UmlClassType.ENUM) {
            val attributeBottom = headerBottom + getAttributeBoxNormalHeight(umlClass)
            mSharedPath.moveTo(left, visibleY(attributeBottom))
            mSharedPath.lineTo(right, visibleY(attributeBottom))
        }
        canvas.drawPath(mSharedPath, linePaint)
    }

    private fun drawUmlClassHeader(canvas: Canvas, umlClass: UmlClass) {
        val nameStr = umlClass.name ?: "Desconocido"
        val interfaceStr = "<< Interfaz >>"

        when (umlClass.umlClassType) {
            UmlClassType.INTERFACE -> {
                canvas.drawText(
                    interfaceStr,
                    visibleX(umlClass.umlClassNormalXPos + (umlClass.umlClassNormalWidth - getTextNormalWidth(interfaceStr)) / 2),
                    visibleY(umlClass.umlClassNormalYPos + FONT_SIZE + INTERLINE),
                    plainTextPaint
                )
                canvas.drawText(
                    nameStr,
                    visibleX(umlClass.umlClassNormalXPos + (umlClass.umlClassNormalWidth - getTextNormalWidth(nameStr)) / 2),
                    visibleY(umlClass.umlClassNormalYPos + FONT_SIZE * 2 + INTERLINE * 2),
                    plainTextPaint
                )
            }
            UmlClassType.ABSTRACT_CLASS -> {
                canvas.drawText(
                    nameStr,
                    visibleX(umlClass.umlClassNormalXPos + (umlClass.umlClassNormalWidth - getTextNormalWidth(nameStr)) / 2),
                    visibleY(umlClass.umlClassNormalYPos + FONT_SIZE + INTERLINE),
                    italicTextPaint
                )
            }
            else -> {
                canvas.drawText(
                    nameStr,
                    visibleX(umlClass.umlClassNormalXPos + (umlClass.umlClassNormalWidth - getTextNormalWidth(nameStr)) / 2),
                    visibleY(umlClass.umlClassNormalYPos + FONT_SIZE + INTERLINE),
                    plainTextPaint
                )
            }
        }
    }

    private fun drawAttributeBox(canvas: Canvas, umlClass: UmlClass) {
        val top = umlClass.umlClassNormalYPos + getClassHeaderNormalHeight(umlClass)

        var currentY = top + INTERLINE + FONT_SIZE
        umlClass.attributes.forEach { a ->
            val paint = if (a.isStatic) underlinedTextPaint else plainTextPaint
            canvas.drawText(
                a.attributeCompleteString,
                visibleX(umlClass.umlClassNormalXPos + INTERLINE),
                visibleY(currentY),
                paint
            )
            currentY += FONT_SIZE + INTERLINE
        }
    }

    private fun drawMethodBox(canvas: Canvas, umlClass: UmlClass) {
        val top = umlClass.umlClassNormalYPos + getClassHeaderNormalHeight(umlClass) + getAttributeBoxNormalHeight(umlClass)

        var currentY = top + INTERLINE + FONT_SIZE
        umlClass.methods.forEach { m ->
            val paint = if (m.isStatic) underlinedTextPaint else plainTextPaint
            canvas.drawText(
                m.methodCompleteString,
                visibleX(umlClass.umlClassNormalXPos + INTERLINE),
                visibleY(currentY),
                paint
            )
            currentY += FONT_SIZE + INTERLINE
        }
    }

    private fun drawValueBox(canvas: Canvas, umlClass: UmlClass) {
        val top = umlClass.umlClassNormalYPos + getClassHeaderNormalHeight(umlClass)

        var currentY = top + INTERLINE + FONT_SIZE
        umlClass.values.forEach { v ->
            canvas.drawText(
                v.name ?: "",
                visibleX(umlClass.umlClassNormalXPos + INTERLINE),
                visibleY(currentY),
                plainTextPaint
            )
            currentY += FONT_SIZE + INTERLINE
        }
    }

    private fun drawRelation(canvas: Canvas, umlRelation: UmlRelation) {
        val originClass = umlRelation.relationOriginClass ?: return
        val endClass = umlRelation.relationEndClass ?: return

        val originAbsoluteLeft = originClass.umlClassNormalXPos
        val originAbsoluteRight = originClass.normalRightEnd
        val originAbsoluteTop = originClass.umlClassNormalYPos
        val originAbsoluteBottom = originClass.normalBottomEnd

        val endAbsoluteLeft = endClass.umlClassNormalXPos
        val endAbsoluteRight = endClass.normalRightEnd
        val endAbsoluteTop = endClass.umlClassNormalYPos
        val endAbsoluteBottom = endClass.normalBottomEnd

        var absoluteXOrigin = 0f
        var absoluteYOrigin = 0f
        var absoluteXEnd = 0f
        var absoluteYEnd = 0f

        if (endClass.isSouthOf(originClass)) {
            val lowerXLimit = originAbsoluteLeft - endAbsoluteTop + originAbsoluteBottom - endClass.umlClassNormalWidth
            val upperXLimit = originAbsoluteRight + endAbsoluteTop - originAbsoluteBottom
            absoluteXEnd = endAbsoluteRight - endClass.umlClassNormalWidth / (upperXLimit - lowerXLimit) * (endAbsoluteLeft - lowerXLimit)
            absoluteYEnd = endAbsoluteTop
            absoluteXOrigin = originAbsoluteLeft + originClass.umlClassNormalWidth / (upperXLimit - lowerXLimit) * (endAbsoluteLeft - lowerXLimit)
            absoluteYOrigin = originAbsoluteBottom
        }
        else if (endClass.isNorthOf(originClass)) {
            val lowerXLimit = originAbsoluteLeft - originAbsoluteTop + endAbsoluteBottom - endClass.umlClassNormalWidth
            val upperXLimit = originAbsoluteRight + originAbsoluteTop - endAbsoluteBottom
            absoluteXEnd = endAbsoluteRight - endClass.umlClassNormalWidth / (upperXLimit - lowerXLimit) * (endAbsoluteLeft - lowerXLimit)
            absoluteYEnd = endAbsoluteBottom
            absoluteXOrigin = originAbsoluteLeft + originClass.umlClassNormalWidth / (upperXLimit - lowerXLimit) * (endAbsoluteLeft - lowerXLimit)
            absoluteYOrigin = originAbsoluteTop
        }
        else if (endClass.isWestOf(originClass)) {
            val lowerYLimit = originAbsoluteTop - originAbsoluteLeft + endAbsoluteRight - endClass.umlClassNormalHeight
            val upperYLimit = originAbsoluteBottom + originAbsoluteLeft - endAbsoluteRight
            absoluteXEnd = endAbsoluteRight
            absoluteYEnd = endAbsoluteBottom - endClass.umlClassNormalHeight / (upperYLimit - lowerYLimit) * (endAbsoluteTop - lowerYLimit)
            absoluteXOrigin = originAbsoluteLeft
            absoluteYOrigin = originAbsoluteTop + originClass.umlClassNormalHeight / (upperYLimit - lowerYLimit) * (endAbsoluteTop - lowerYLimit)
        }
        else if (endClass.isEastOf(originClass)) {
            val lowerYLimit = originAbsoluteTop - endAbsoluteLeft + originAbsoluteRight - endClass.umlClassNormalHeight
            val upperYLimit = originAbsoluteBottom + endAbsoluteLeft - originAbsoluteRight
            absoluteXEnd = endAbsoluteLeft
            absoluteYEnd = endAbsoluteBottom - endClass.umlClassNormalHeight / (upperYLimit - lowerYLimit) * (endAbsoluteTop - lowerYLimit)
            absoluteXOrigin = originAbsoluteRight
            absoluteYOrigin = originAbsoluteTop + originClass.umlClassNormalHeight / (upperYLimit - lowerYLimit) * (endAbsoluteTop - lowerYLimit)
        }

        umlRelation.xOrigin = absoluteXOrigin
        umlRelation.yOrigin = absoluteYOrigin
        umlRelation.xEnd = absoluteXEnd
        umlRelation.yEnd = absoluteYEnd

        // Reutilizamos el Path maestro en lugar de instanciar uno nuevo
        mSharedPath.reset()
        mSharedPath.moveTo(visibleX(absoluteXOrigin), visibleY(absoluteYOrigin))
        mSharedPath.lineTo(visibleX(absoluteXEnd), visibleY(absoluteYEnd))

        when (umlRelation.umlRelationType) {
            UmlRelationType.INHERITANCE -> {
                canvas.drawPath(mSharedPath, linePaint)
                drawSolidWhiteArrow(canvas, visibleX(absoluteXOrigin), visibleY(absoluteYOrigin), visibleX(absoluteXEnd), visibleY(absoluteYEnd))
            }
            UmlRelationType.ASSOCIATION -> canvas.drawPath(mSharedPath, linePaint)
            UmlRelationType.DIRECTED_ASSOCIATION -> {
                canvas.drawPath(mSharedPath, linePaint)
                drawArrow(canvas, visibleX(absoluteXOrigin), visibleY(absoluteYOrigin), visibleX(absoluteXEnd), visibleY(absoluteYEnd))
            }
            UmlRelationType.BIDIRECTIONAL_ASSOCIATION -> {
                canvas.drawPath(mSharedPath, linePaint)
                drawArrow(canvas, visibleX(absoluteXOrigin), visibleY(absoluteYOrigin), visibleX(absoluteXEnd), visibleY(absoluteYEnd))
                drawArrow(canvas, visibleX(absoluteXEnd), visibleY(absoluteYEnd), visibleX(absoluteXOrigin), visibleY(absoluteYOrigin))
            }
            UmlRelationType.AGGREGATION -> {
                canvas.drawPath(mSharedPath, linePaint)
                drawSolidWhiteRhombus(canvas, visibleX(absoluteXOrigin), visibleY(absoluteYOrigin), visibleX(absoluteXEnd), visibleY(absoluteYEnd))
            }
            UmlRelationType.COMPOSITION -> {
                canvas.drawPath(mSharedPath, linePaint)
                drawSolidBlackRhombus(canvas, visibleX(absoluteXOrigin), visibleY(absoluteYOrigin), visibleX(absoluteXEnd), visibleY(absoluteYEnd))
            }
            UmlRelationType.DEPENDENCY -> {
                canvas.drawPath(mSharedPath, dashPaint)
                drawArrow(canvas, visibleX(absoluteXOrigin), visibleY(absoluteYOrigin), visibleX(absoluteXEnd), visibleY(absoluteYEnd))
            }
            UmlRelationType.REALIZATION -> {
                canvas.drawPath(mSharedPath, dashPaint)
                drawSolidWhiteArrow(canvas, visibleX(absoluteXOrigin), visibleY(absoluteYOrigin), visibleX(absoluteXEnd), visibleY(absoluteYEnd))
            }
        }
    }

    private fun drawArrow(canvas: Canvas, xOrigin: Float, yOrigin: Float, xEnd: Float, yEnd: Float) {
        canvas.save()
        canvas.rotate(getAngle(xEnd, yEnd, xOrigin, yOrigin), xEnd, yEnd)
        mSharedPath.reset()
        mSharedPath.moveTo(xEnd + ARROW_SIZE * mZoom, yEnd - ARROW_SIZE * 1.414f / 2f * mZoom)
        mSharedPath.lineTo(xEnd, yEnd)
        mSharedPath.lineTo(xEnd + ARROW_SIZE * mZoom, yEnd + ARROW_SIZE * 1.414f / 2f * mZoom)
        canvas.drawPath(mSharedPath, linePaint)
        canvas.restore()
    }

    private fun drawSolidWhiteArrow(canvas: Canvas, xOrigin: Float, yOrigin: Float, xEnd: Float, yEnd: Float) {
        canvas.save()
        canvas.rotate(getAngle(xEnd, yEnd, xOrigin, yOrigin), xEnd, yEnd)
        mSharedPath.reset()
        mSharedPath.moveTo(xEnd, yEnd)
        mSharedPath.lineTo(xEnd + ARROW_SIZE * mZoom, yEnd - ARROW_SIZE * 1.414f / 2f * mZoom)
        mSharedPath.lineTo(xEnd + ARROW_SIZE * mZoom, yEnd + ARROW_SIZE * 1.414f / 2f * mZoom)
        mSharedPath.close()
        canvas.drawPath(mSharedPath, solidFillPaint)
        canvas.drawPath(mSharedPath, linePaint)
        canvas.restore()
    }

    private fun drawSolidWhiteRhombus(canvas: Canvas, xOrigin: Float, yOrigin: Float, xEnd: Float, yEnd: Float) {
        canvas.save()
        canvas.rotate(getAngle(xEnd, yEnd, xOrigin, yOrigin), xEnd, yEnd)
        mSharedPath.reset()
        mSharedPath.moveTo(xEnd, yEnd)
        mSharedPath.lineTo(xEnd + ARROW_SIZE * mZoom, yEnd - ARROW_SIZE * 1.414f / 2f * mZoom)
        mSharedPath.lineTo(xEnd + ARROW_SIZE * 2f * mZoom, yEnd)
        mSharedPath.lineTo(xEnd + ARROW_SIZE * mZoom, yEnd + ARROW_SIZE * 1.414f / 2f * mZoom)
        mSharedPath.close()
        canvas.drawPath(mSharedPath, solidFillPaint)
        canvas.drawPath(mSharedPath, linePaint)
        canvas.restore()
    }

    private fun drawSolidBlackRhombus(canvas: Canvas, xOrigin: Float, yOrigin: Float, xEnd: Float, yEnd: Float) {
        canvas.save()
        canvas.rotate(getAngle(xEnd, yEnd, xOrigin, yOrigin), xEnd, yEnd)
        mSharedPath.reset()
        mSharedPath.moveTo(xEnd, yEnd)
        mSharedPath.lineTo(xEnd + ARROW_SIZE * mZoom, yEnd - ARROW_SIZE * 1.414f / 2f * mZoom)
        mSharedPath.lineTo(xEnd + ARROW_SIZE * 2f * mZoom, yEnd)
        mSharedPath.lineTo(xEnd + ARROW_SIZE * mZoom, yEnd + ARROW_SIZE * 1.414f / 2f * mZoom)
        mSharedPath.close()
        canvas.drawPath(mSharedPath, solidBlackPaint)
        canvas.restore()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val action = event.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mActionDownEventTime = event.eventTime
                mLastTouchX = event.x
                mLastTouchY = event.y
                mMovingClass = getTouchedClass(mLastTouchX, mLastTouchY)
                mTouchMode = TouchMode.DRAG
                mPrimaryPointerIndex = 0
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                mOldDist = spacing(event)
                calculateMidPoint(event)
                mOldXMidPoint = mXMidPoint
                mOldYMidPoint = mYMidpoint
                if (mOldDist > 10f) {
                    mTouchMode = TouchMode.ZOOM
                    mMovingClass = null
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mTouchMode == TouchMode.DRAG) {
                    if (mMovingClass == null) {
                        mXOffset += event.x - mLastTouchX
                        mYOffset += event.y - mLastTouchY
                    } else {
                        mMovingClass?.let {
                            it.umlClassNormalXPos += (event.x - mLastTouchX) / mZoom
                            it.umlClassNormalYPos += (event.y - mLastTouchY) / mZoom
                        }
                    }
                    mLastTouchX = event.x
                    mLastTouchY = event.y
                } else if (mTouchMode == TouchMode.ZOOM) {
                    mNewDist = spacing(event)
                    mZoom *= mNewDist / mOldDist
                    calculateMidPoint(event)
                    mXOffset = mXMidPoint + (mXOffset - mOldXMidPoint) * mNewDist / mOldDist
                    mYOffset = mYMidpoint + (mYOffset - mOldYMidPoint) * mNewDist / mOldDist
                    mOldDist = mNewDist
                    mOldXMidPoint = mXMidPoint
                    mOldYMidPoint = mYMidpoint
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                mTouchMode = TouchMode.DRAG
                if (event.actionIndex == mPrimaryPointerIndex) {
                    mPrimaryPointerIndex = (1 + mPrimaryPointerIndex) % 2
                }
                mLastTouchX = event.getX(mPrimaryPointerIndex)
                mLastTouchY = event.getY(mPrimaryPointerIndex)
            }
            MotionEvent.ACTION_UP -> {
                val isDoubleClick = event.eventTime - mActionDownEventTime <= CLICK_DELAY &&
                        event.eventTime - mFirstClickTime <= DOUBLE_CLICK_DELAY &&
                        distance(mLastTouchX, mLastTouchY, mFirstClickX, mFirstClickY) <= DOUBLE_CLICK_DISTANCE_MAX

                if (isDoubleClick) {
                    val touchedClass = getTouchedClass(mLastTouchX, mLastTouchY)
                    val touchedRelation = getTouchedRelation(mLastTouchX, mLastTouchY)
                    when {
                        touchedClass != null -> mCallback?.editClass(touchedClass)
                        touchedRelation != null -> promptDeleteRelation(touchedRelation, this)
                        else -> adjustViewToProject()
                    }
                }

                if (event.eventTime - mActionDownEventTime <= CLICK_DELAY) {
                    mFirstClickTime = event.eventTime
                    mFirstClickX = event.x
                    mFirstClickY = event.y
                    val touchedClass = getTouchedClass(mLastTouchX, mLastTouchY)

                    mGraphFragment?.let { fragment ->
                        if (fragment.isExpectingTouchLocation) {
                            fragment.isExpectingTouchLocation = false
                            fragment.clearPrompt()
                            mCallback?.createClass(absoluteX(mLastTouchX), absoluteY(mLastTouchY))
                        } else if (fragment.isExpectingEndClass && touchedClass != null && touchedClass != fragment.startClass) {
                            fragment.endClass = touchedClass
                            fragment.isExpectingEndClass = false
                            fragment.clearPrompt()
                            mCallback?.createRelation(fragment.startClass, fragment.endClass, fragment.umlRelationType)
                        } else if (fragment.isExpectingStartClass && touchedClass != null) {
                            fragment.startClass = touchedClass
                            fragment.isExpectingStartClass = false
                            fragment.isExpectingEndClass = true
                            fragment.setPrompt(context.getString(R.string.graph_prompt_choose_end_class))
                        }
                        else {
                            // Este 'else' vacío soluciona el error de Kotlin.
                            // Le dice que si no pasa nada de lo anterior, simplemente no haga nada.
                        }
                    }
                }
            }
        }
        invalidate()
        return true
    }

    private fun spacing(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private fun calculateMidPoint(event: MotionEvent) {
        mXMidPoint = (event.getX(0) + event.getX(1)) / 2
        mYMidpoint = (event.getY(0) + event.getY(1)) / 2
    }

    private fun updateUmlClassNormalDimensions(umlClass: UmlClass) {
        if (umlClass.umlClassType == UmlClassType.ENUM) {
            umlClass.umlClassNormalWidth = max(getClassHeaderNormalWidth(umlClass), getValueBoxNormalWidth(umlClass))
            umlClass.umlClassNormalHeight = getClassHeaderNormalHeight(umlClass) + getValueBoxNormalHeight(umlClass)
        } else {
            var currentWidth = getClassHeaderNormalWidth(umlClass)
            currentWidth = max(currentWidth, getAttributeBoxNormalWidth(umlClass))
            currentWidth = max(currentWidth, getMethodBoxNormalWidth(umlClass))

            umlClass.umlClassNormalWidth = currentWidth
            umlClass.umlClassNormalHeight = getClassHeaderNormalHeight(umlClass) + getAttributeBoxNormalHeight(umlClass) + getMethodBoxNormalHeight(umlClass)
        }
    }

    private fun getClassHeaderNormalWidth(umlClass: UmlClass): Float {
        return if (umlClass.umlClassType == UmlClassType.INTERFACE) {
            max(getTextNormalWidth("<< Interfaz >>"), getTextNormalWidth(umlClass.name)) + 2 * INTERLINE
        } else {
            getTextNormalWidth(umlClass.name) + 2 * INTERLINE
        }
    }

    private fun getClassHeaderNormalHeight(umlClass: UmlClass): Float {
        return if (umlClass.umlClassType == UmlClassType.INTERFACE) FONT_SIZE * 2 + 3 * INTERLINE
        else FONT_SIZE + 2 * INTERLINE
    }

    private fun getAttributeBoxNormalWidth(umlClass: UmlClass): Float {
        val maxWidth = umlClass.attributes.maxOfOrNull { getTextNormalWidth(it.attributeCompleteString) } ?: 0f
        return maxWidth + 2 * INTERLINE
    }

    private fun getAttributeBoxNormalHeight(umlClass: UmlClass): Float {
        return umlClass.attributes.size * FONT_SIZE + (umlClass.attributes.size + 1) * INTERLINE
    }

    private fun getMethodBoxNormalWidth(umlClass: UmlClass): Float {
        val maxWidth = umlClass.methods.maxOfOrNull { getTextNormalWidth(it.methodCompleteString) } ?: 0f
        return maxWidth + 2 * INTERLINE
    }

    private fun getMethodBoxNormalHeight(umlClass: UmlClass): Float {
        return umlClass.methods.size * FONT_SIZE + (umlClass.methods.size + 1) * INTERLINE
    }

    private fun getValueBoxNormalWidth(umlClass: UmlClass): Float {
        val maxWidth = umlClass.values.maxOfOrNull { getTextNormalWidth(it.name ?: "") } ?: 0f
        return maxWidth + 2 * INTERLINE
    }

    private fun getValueBoxNormalHeight(umlClass: UmlClass): Float {
        return umlClass.values.size * FONT_SIZE + (umlClass.values.size + 1) * INTERLINE
    }

    private fun getTextNormalWidth(text: String?): Float {
        return if (text != null) plainTextPaint.measureText(text) / mZoom else 0f
    }

    private fun getAngle(xOrigin: Float, yOrigin: Float, xEnd: Float, yEnd: Float): Float {
        val distance = sqrt(((xEnd - xOrigin) * (xEnd - xOrigin) + (yEnd - yOrigin) * (yEnd - yOrigin)).toDouble())
        return (abs(acos((xEnd - xOrigin) / distance)).withSign((yEnd - yOrigin).toDouble()) / Math.PI * 180f).toFloat()
    }

    private val absoluteProjectWidth: Float
        get() {
            var minX = 1000000f
            var maxX = -1000000f
            mUmlProject?.umlClasses?.forEach { c ->
                minX = min(c.umlClassNormalXPos, minX)
                maxX = max(c.normalRightEnd, maxX)
            }
            return maxX - minX
        }

    private val absoluteProjectHeight: Float
        get() {
            var minY = 1000000f
            var maxY = -1000000f
            mUmlProject?.umlClasses?.forEach { c ->
                minY = min(c.umlClassNormalYPos, minY)
                maxY = max(c.normalBottomEnd, maxY)
            }
            return maxY - minY
        }

    private val absoluteProjectLeft: Float
        get() = mUmlProject?.umlClasses?.minOfOrNull { it.umlClassNormalXPos } ?: 1000000f

    private val absoluteProjectRight: Float
        get() = mUmlProject?.umlClasses?.maxOfOrNull { it.normalRightEnd } ?: -1000000f

    private val absoluteProjectTop: Float
        get() = mUmlProject?.umlClasses?.minOfOrNull { it.umlClassNormalYPos } ?: 1000000f

    private val absoluteProjectBottom: Float
        get() = mUmlProject?.umlClasses?.maxOfOrNull { it.normalBottomEnd } ?: -1000000f

    private fun adjustViewToProject() {
        val xZoom = measuredWidth / absoluteProjectWidth
        val yZoom = measuredHeight / absoluteProjectHeight
        if (xZoom <= yZoom) {
            mZoom = xZoom
            mXOffset = -absoluteProjectLeft * mZoom
            mYOffset = -absoluteProjectTop * mZoom + (measuredHeight - absoluteProjectHeight * mZoom) / 2f
        } else {
            mZoom = yZoom
            mYOffset = -absoluteProjectTop * mZoom
            mXOffset = -absoluteProjectLeft * mZoom + (measuredWidth - absoluteProjectWidth * mZoom) / 2f
        }
        invalidate()
    }

    private fun visibleX(absoluteX: Float): Float = mXOffset + mZoom * absoluteX
    private fun visibleY(absoluteY: Float): Float = mYOffset + mZoom * absoluteY
    private fun absoluteX(visibleX: Float): Float = (visibleX - mXOffset) / mZoom
    private fun absoluteY(visibleY: Float): Float = (visibleY - mYOffset) / mZoom

    private fun getTouchedClass(visibleX: Float, visibleY: Float): UmlClass? {
        return mUmlProject?.umlClasses?.firstOrNull { it.containsPoint(absoluteX(visibleX), absoluteY(visibleY)) }
    }

    private fun getTouchedRelation(visibleX: Float, visibleY: Float): UmlRelation? {
        return mUmlProject?.umlRelations?.firstOrNull { r ->
            val absX = absoluteX(visibleX)
            val absY = absoluteY(visibleY)
            distance(absX, absY, r.xOrigin, r.yOrigin, r.xEnd, r.yEnd) <= 20 &&
                    absX >= min(r.xOrigin, r.xEnd) - 20 &&
                    absX <= max(r.xOrigin, r.xEnd) + 20 &&
                    absY >= min(r.yOrigin, r.yEnd) - 20 &&
                    absY <= max(r.yOrigin, r.yEnd) + 20
        }
    }

    private fun distance(dotX: Float, dotY: Float, originX: Float, originY: Float, endX: Float, endY: Float): Float {
        val uX: Float
        val uY: Float
        if (originX == endX) {
            uX = 0f
            uY = 1f
        } else if (originY == endY) {
            uX = 1f
            uY = 0f
        } else {
            val distEndOrigin = sqrt((1f + (endX - originX) * (endX - originX) / ((endY - originY) * (endY - originY))).toDouble()).toFloat()
            uX = 1f / distEndOrigin
            uY = ((originX - endX) / (endY - originY)) / distEndOrigin
        }
        return abs((dotX - originX) * uX + (dotY - originY) * uY)
    }

    private fun distance(X1: Float, Y1: Float, X2: Float, Y2: Float): Float {
        return sqrt(((X1 - X2) * (X1 - X2) + (Y1 - Y2) * (Y1 - Y2)).toDouble()).toFloat()
    }

    private fun updateProjectGeometricalParameters() {
        mUmlProject?.apply {
            zoom = mZoom
            xOffset = mXOffset
            yOffset = mYOffset
        }
    }

    private fun promptDeleteRelation(umlRelation: UmlRelation, view: View) {
        AlertDialog.Builder(context)
            .setTitle(R.string.dialog_delete_relation_title)
            .setMessage(R.string.dialog_delete_relation_message)
            .setNegativeButton(R.string.button_no, null)
            .setPositiveButton(R.string.button_yes) { _, _ ->
                mUmlProject?.removeUmlRelation(umlRelation)
                view.invalidate()
            }
            .show()
    }

    companion object {
        private const val CLICK_DELAY: Long = 200
        private const val DOUBLE_CLICK_DELAY: Long = 500
        private const val DOUBLE_CLICK_DISTANCE_MAX = 10f
        private const val FONT_SIZE = 20f
        private const val INTERLINE = 10f
        private const val ARROW_SIZE = 10f
        private const val CORNER_RADIUS = 16f
        private const val EXPORT_PADDING = 32f
        private const val EXPORT_MAX_DIMENSION = 4096
        private const val EXPORT_MAX_SCALE = 2f
    }
}