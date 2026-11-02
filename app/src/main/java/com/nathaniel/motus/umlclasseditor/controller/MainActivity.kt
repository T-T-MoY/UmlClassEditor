package com.nathaniel.motus.umlclasseditor.controller

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.MenuCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.nathaniel.motus.umlclasseditor.R
import com.nathaniel.motus.umlclasseditor.model.TypeNameComparator
import com.nathaniel.motus.umlclasseditor.model.UmlClass
import com.nathaniel.motus.umlclasseditor.model.UmlProject
import com.nathaniel.motus.umlclasseditor.model.UmlRelation
import com.nathaniel.motus.umlclasseditor.model.UmlType
import com.nathaniel.motus.umlclasseditor.view.AttributeEditorFragment
import com.nathaniel.motus.umlclasseditor.view.ClassEditorFragment
import com.nathaniel.motus.umlclasseditor.view.GraphFragment
import com.nathaniel.motus.umlclasseditor.view.GraphView
import com.nathaniel.motus.umlclasseditor.view.MethodEditorFragment
import com.nathaniel.motus.umlclasseditor.view.ParameterEditorFragment
import java.io.File
import java.util.Calendar
import java.util.Collections

class MainActivity : AppCompatActivity(), FragmentObserver, GraphView.GraphViewObserver,
    NavigationView.OnNavigationItemSelectedListener {

    override var project: UmlProject? = null
    override val isExpectingTouchLocation: Boolean = false
    private var mPurpose = FragmentObserver.Purpose.NONE

    // Usamos lateinit para evitar que sean nulas y quitar los molestos "!!" en el código
    private lateinit var mToolbar: Toolbar
    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var mNavigationView: NavigationView
    private lateinit var mMenuHeaderProjectNameText: TextView
    private lateinit var mMainActivityFrame: FrameLayout
    private lateinit var mGraphView: GraphView

    private var mFirstBackPressedTime: Long = 0
    private lateinit var mOnBackPressedCallback: OnBackPressedCallback

    // Fragments
    private var mGraphFragment: GraphFragment? = null
    private var mClassEditorFragment: ClassEditorFragment? = null
    private var mAttributeEditorFragment: AttributeEditorFragment? = null
    private var mMethodEditorFragment: MethodEditorFragment? = null
    private var mParameterEditorFragment: ParameterEditorFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mMainActivityFrame = findViewById(R.id.activity_main_frame)

        UmlType.clearUmlTypes()
        UmlType.initializePrimitiveUmlTypes(this)
        UmlType.initializeCustomUmlTypes(this)

        loadPreferences() // Función extraída y limpiada

        configureToolbar()
        configureDrawerLayout()
        configureNavigationView()
        configureAndDisplayGraphFragment(R.id.activity_main_frame)
        createOnBackPressedCallback()
        setOnBackPressedCallback()

        // PEDIR PERMISOS SOLO UNA VEZ AL INICIAR LA APP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main_toolbar_menu, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onStart() {
        super.onStart()
        mGraphView = findViewById(R.id.graphview)
        mGraphView.setUmlProject(project)
    }

    override fun onDestroy() {
        super.onDestroy()
        project?.save(applicationContext)
        savePreferences()
        UmlType.saveCustomUmlTypes(this)
    }

    // **********************************************************************************************
    // Configuration methods
    // **********************************************************************************************
    private fun configureToolbar() {
        mToolbar = findViewById(R.id.main_activity_toolbar)
        setSupportActionBar(mToolbar)
    }

    private fun configureDrawerLayout() {
        mDrawerLayout = findViewById(R.id.activity_main_drawer)
        val toggle = ActionBarDrawerToggle(
            this, mDrawerLayout, mToolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        mDrawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun configureNavigationView() {
        mNavigationView = findViewById(R.id.activity_main_navigation_view)
        mMenuHeaderProjectNameText = mNavigationView.getHeaderView(0)
            .findViewById(R.id.activity_main_navigation_view_header_project_name_text)
        updateNavigationView()
        mNavigationView.setNavigationItemSelectedListener(this)
        updateThemeMenuSelection()
    }

    private fun updateNavigationView() {
        mMenuHeaderProjectNameText.text = project?.name ?: getString(R.string.default_project_name)
    }

    private fun savePreferences() {
        val editor = getPreferences(MODE_PRIVATE).edit()
        editor.putString(SHARED_PREFERENCES_PROJECT_NAME, project?.name)
        editor.apply()
    }

    private fun loadPreferences() {
        val prefs = getPreferences(MODE_PRIVATE)
        val projectName = prefs.getString(SHARED_PREFERENCES_PROJECT_NAME, null)
        project = if (projectName != null) {
            UmlProject.load(applicationContext, projectName)
        } else {
            UmlProject(getString(R.string.default_project_name), applicationContext)
        }
    }

    private fun createOnBackPressedCallback() {
        mOnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackButtonPressed()
            }
        }
    }

    private fun setOnBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, mOnBackPressedCallback)
    }

    private fun onBackButtonPressed() {
        if (Calendar.getInstance().timeInMillis - mFirstBackPressedTime > DOUBLE_BACK_PRESSED_DELAY) {
            mFirstBackPressedTime = Calendar.getInstance().timeInMillis
            Toast.makeText(this, R.string.toast_press_back_again, Toast.LENGTH_SHORT).show()
        } else finish()
    }

    // **********************************************************************************************
    // Fragment management
    // **********************************************************************************************
    private fun configureAndDisplayGraphFragment(viewContainerId: Int) {
        mGraphFragment = GraphFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(viewContainerId, mGraphFragment!!, GRAPH_FRAGMENT_TAG)
            .commitNow()
    }

    private fun configureAndDisplayClassEditorFragment(viewContainerId: Int, xLocation: Float, yLocation: Float, classOrder: Int) {
        if (mClassEditorFragment == null) {
            mClassEditorFragment = ClassEditorFragment.newInstance(xLocation, yLocation, classOrder)
            supportFragmentManager.beginTransaction()
                .hide(mGraphFragment!!)
                .add(viewContainerId, mClassEditorFragment!!, CLASS_EDITOR_FRAGMENT_TAG)
                .commitNow()
        } else {
            mClassEditorFragment!!.updateClassEditorFragment(xLocation, yLocation, classOrder)
            supportFragmentManager.beginTransaction()
                .hide(mGraphFragment!!)
                .show(mClassEditorFragment!!)
                .commitNow()
        }
    }

    private fun configureAndDisplayAttributeEditorFragment(viewContainerId: Int, attributeOrder: Int, classOrder: Int) {
        if (mAttributeEditorFragment == null) {
            mAttributeEditorFragment = AttributeEditorFragment.newInstance(mClassEditorFragment!!.tag, attributeOrder, classOrder)
            supportFragmentManager.beginTransaction()
                .hide(mClassEditorFragment!!)
                .add(viewContainerId, mAttributeEditorFragment!!, ATTRIBUTE_EDITOR_FRAGMENT_TAG)
                .commitNow()
        } else {
            mAttributeEditorFragment!!.updateAttributeEditorFragment(attributeOrder, classOrder)
            supportFragmentManager.beginTransaction()
                .hide(mClassEditorFragment!!)
                .show(mAttributeEditorFragment!!)
                .commitNow()
        }
    }

    private fun configureAndDisplayMethodEditorFragment(viewContainerId: Int, methodOrder: Int, classOrder: Int) {
        if (mMethodEditorFragment == null) {
            mMethodEditorFragment = MethodEditorFragment.newInstance(mClassEditorFragment!!.tag, methodOrder, classOrder)
            supportFragmentManager.beginTransaction()
                .hide(mClassEditorFragment!!)
                .add(viewContainerId, mMethodEditorFragment!!, METHOD_EDITOR_FRAGMENT_TAG)
                .commitNow()
        } else {
            mMethodEditorFragment!!.updateMethodEditorFragment(methodOrder, classOrder)
            supportFragmentManager.beginTransaction()
                .hide(mClassEditorFragment!!)
                .show(mMethodEditorFragment!!)
                .commitNow()
        }
    }

    private fun configureAndDisplayParameterEditorFragment(viewContainerId: Int, parameterOrder: Int, methodOrder: Int, classOrder: Int) {
        if (mParameterEditorFragment == null) {
            mParameterEditorFragment = ParameterEditorFragment.newInstance(mMethodEditorFragment!!.tag, parameterOrder, methodOrder, classOrder)
            supportFragmentManager.beginTransaction()
                .hide(mMethodEditorFragment!!)
                .add(viewContainerId, mParameterEditorFragment!!, PARAMETER_EDITOR_FRAGMENT_TAG)
                .commitNow()
        } else {
            mParameterEditorFragment!!.updateParameterEditorFragment(parameterOrder, methodOrder, classOrder)
            supportFragmentManager.beginTransaction()
                .hide(mMethodEditorFragment!!)
                .show(mParameterEditorFragment!!)
                .commitNow()
        }
    }

    // **********************************************************************************************
    // Callback methods
    // **********************************************************************************************
    override fun setPurpose(purpose: FragmentObserver.Purpose) {
        mPurpose = purpose
    }

    override fun closeClassEditorFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().hide(fragment).show(mGraphFragment!!).commitNow()
        mGraphView.invalidate()
    }

    override fun closeAttributeEditorFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().hide(fragment).show(mClassEditorFragment!!).commit()
        mClassEditorFragment?.updateLists()
    }

    override fun closeMethodEditorFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().hide(fragment).show(mClassEditorFragment!!).commitNow()
        mClassEditorFragment?.updateLists()
    }

    override fun closeParameterEditorFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().hide(fragment).show(mMethodEditorFragment!!).commitNow()
        mMethodEditorFragment?.updateLists()
    }

    override fun openAttributeEditorFragment(attributeOrder: Int, classOrder: Int) {
        configureAndDisplayAttributeEditorFragment(R.id.activity_main_frame, attributeOrder, classOrder)
    }

    override fun openMethodEditorFragment(methodOrder: Int, classOrder: Int) {
        configureAndDisplayMethodEditorFragment(R.id.activity_main_frame, methodOrder, classOrder)
    }

    override fun openParameterEditorFragment(parameterOrder: Int, methodOrder: Int, classOrder: Int) {
        configureAndDisplayParameterEditorFragment(R.id.activity_main_frame, parameterOrder, methodOrder, classOrder)
    }

    override fun createClass(xLocation: Float, yLocation: Float) {
        configureAndDisplayClassEditorFragment(R.id.activity_main_frame, xLocation, yLocation, -1)
    }

    override fun editClass(umlClass: UmlClass?) {
        if (umlClass != null) {
            configureAndDisplayClassEditorFragment(R.id.activity_main_frame, 0f, 0f, umlClass.classOrder)
        }
    }

    override fun createRelation(startClass: UmlClass?, endClass: UmlClass?, relationType: UmlRelation.UmlRelationType?) {
        if (startClass != null && endClass != null && relationType != null) {
            if (project?.relationAlreadyExistsBetween(startClass, endClass) == false) {
                project?.addUmlRelation(UmlRelation(startClass, endClass, relationType))
            }
        }
    }

    // **********************************************************************************************
    // Navigation view events
    // **********************************************************************************************
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        mDrawerLayout.closeDrawer(GravityCompat.START)

        // Uso de 'when' en lugar de if-else encadenados
        when (item.itemId) {
            R.id.drawer_menu_new_project -> drawerMenuNewProject()
            R.id.drawer_menu_load_project -> drawerMenuLoadProject()
            R.id.drawer_menu_save_as -> drawerMenuSaveAs()
            R.id.drawer_menu_merge_project -> drawerMenuMerge()
            R.id.drawer_menu_delete_project -> drawerMenuDeleteProject()
            R.id.drawer_menu_theme_system -> ThemeManager.setMode(this, ThemeManager.MODE_SYSTEM)
            R.id.drawer_menu_theme_light -> ThemeManager.setMode(this, ThemeManager.MODE_LIGHT)
            R.id.drawer_menu_theme_dark -> ThemeManager.setMode(this, ThemeManager.MODE_DARK)
        }
        return true
    }

    /** Marca en el menú lateral el tema que está activo actualmente. */
    private fun updateThemeMenuSelection() {
        val checkedItemId = when (ThemeManager.getSavedMode(this)) {
            ThemeManager.MODE_LIGHT -> R.id.drawer_menu_theme_light
            ThemeManager.MODE_DARK -> R.id.drawer_menu_theme_dark
            else -> R.id.drawer_menu_theme_system
        }
        mNavigationView.setCheckedItem(checkedItemId)
    }

    private fun drawerMenuSaveAs() {
        val editText = EditText(this)
        editText.setText(project?.name)
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_save_as_title)
            .setMessage(R.string.dialog_save_as_message)
            .setView(editText)
            .setNegativeButton(R.string.button_cancel) { _, _ -> }
            .setPositiveButton(R.string.button_ok) { _, _ -> saveAs(editText.text.toString()) }
            .show()
    }

    private fun drawerMenuNewProject() {
        project?.save(this)
        UmlType.clearProjectUmlTypes()
        project = UmlProject(getString(R.string.default_project_name), this)
        mGraphView.setUmlProject(project)
        updateNavigationView()
    }

    private fun drawerMenuLoadProject() {
        project?.save(this)
        val spinner = Spinner(this).apply { adapter = projectDirectoryAdapter() }

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_load_project_title)
            .setMessage(R.string.dialog_load_project_message)
            .setView(spinner)
            .setNegativeButton(R.string.button_cancel) { _, _ -> }
            .setPositiveButton(R.string.button_ok) { _, _ ->
                spinner.selectedItem?.toString()?.let { fileName ->
                    UmlType.clearProjectUmlTypes()
                    project = UmlProject.load(applicationContext, fileName)
                    mGraphView.setUmlProject(project)
                    updateNavigationView()
                }
            }
            .show()
    }

    private fun drawerMenuDeleteProject() {
        val spinner = Spinner(this).apply { adapter = projectDirectoryAdapter() }

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_project_title)
            .setMessage(R.string.dialog_delete_project_message)
            .setView(spinner)
            .setNegativeButton(R.string.button_cancel) { _, _ -> }
            .setPositiveButton(R.string.button_ok) { _, _ ->
                spinner.selectedItem?.toString()?.let { fileName ->
                    val file = File(File(filesDir, UmlProject.PROJECT_DIRECTORY), fileName)
                    AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_confirm_delete_project_title)
                        .setMessage(getString(R.string.dialog_confirm_delete_project_message, fileName))
                        .setNegativeButton(R.string.button_no) { _, _ -> }
                        .setPositiveButton(R.string.button_yes) { _, _ -> file.delete() }
                        .show()
                }
            }
            .show()
    }

    private fun drawerMenuMerge() {
        val spinner = Spinner(this).apply { adapter = projectDirectoryAdapter() }

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_merge_project_title)
            .setMessage(R.string.dialog_merge_project_message)
            .setView(spinner)
            .setNegativeButton(R.string.button_cancel) { _, _ -> }
            .setPositiveButton(R.string.button_ok) { _, _ ->
                spinner.selectedItem?.toString()?.let { fileName ->
                    val projectToMerge = UmlProject.load(applicationContext, fileName)
                    // Usamos let para pasarle el proyecto ya validado (it)
                    projectToMerge?.let {
                        project?.mergeWith(it)
                        mGraphView.invalidate()
                    }
                }
            }
            .show()
    }

    private fun projectDirectoryAdapter(): ArrayAdapter<String> {
        val fileList = IOUtils.sortedFiles(File(filesDir, UmlProject.PROJECT_DIRECTORY))
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fileList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return adapter
    }

    // **********************************************************************************************
    // Option menu events
    // **********************************************************************************************
    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.toolbar_menu_export -> if (sWriteExternalStoragePermission) menuItemExport()
            R.id.toolbar_menu_import -> if (sReadExternalStoragePermission) menuItemImport()
            R.id.toolbar_menu_export_image -> menuItemExportImage()
            R.id.toolbar_menu_create_custom_type -> menuCreateCustomType()
            R.id.toolbar_menu_delete_custom_types -> menuDeleteCustomTypes()
            R.id.toolbar_menu_export_custom_types -> if (sWriteExternalStoragePermission) menuExportCustomTypes()
            R.id.toolbar_menu_import_custom_types -> if (sReadExternalStoragePermission) menuImportCustomTypes()
            R.id.toolbar_menu_help -> menuHelp()
        }
        return true
    }

    private fun menuItemExport() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { type = "text/*" }
        startActivityForResult(intent, INTENT_CREATE_DOCUMENT_EXPORT_PROJECT)
    }

    private fun menuItemImport() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "*/*" }
        startActivityForResult(intent, INTENT_OPEN_DOCUMENT_IMPORT_PROJECT)
    }

    private fun menuItemExportImage() {
        if (project?.umlClasses?.isEmpty() != false) {
            Toast.makeText(this, R.string.toast_empty_project, Toast.LENGTH_SHORT).show()
            return
        }
        val suggestedName = "${project?.name ?: getString(R.string.default_diagram_filename)}.jpg"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/jpeg"
            putExtra(Intent.EXTRA_TITLE, suggestedName)
        }
        startActivityForResult(intent, INTENT_CREATE_DOCUMENT_EXPORT_IMAGE)
    }

    private fun exportImageToUri(uri: Uri) {
        val bitmap = mGraphView.createBitmapFromView()
        if (bitmap == null) {
            Toast.makeText(this, R.string.toast_empty_project, Toast.LENGTH_SHORT).show()
            return
        }

        val exported = try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            } ?: false
        } catch (e: Exception) {
            Log.e("TEST", "Image export failed: ${e.message}", e)
            false
        } finally {
            bitmap.recycle()
        }

        Toast.makeText(
            this,
            if (exported) R.string.toast_image_exported else R.string.toast_image_export_failed,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun menuCreateCustomType() {
        val editText = EditText(this)
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_create_custom_type_title)
            .setMessage(R.string.dialog_create_custom_type_message)
            .setView(editText)
            .setNegativeButton(R.string.button_cancel) { _, _ -> }
            .setPositiveButton(R.string.button_ok) { _, _ ->
                val typeName = editText.text.toString()
                when {
                    typeName.isBlank() -> Toast.makeText(this, R.string.toast_custom_type_blank, Toast.LENGTH_SHORT).show()
                    UmlType.containsUmlTypeNamed(typeName) -> Toast.makeText(this, R.string.toast_custom_type_exists, Toast.LENGTH_SHORT).show()
                    else -> {
                        UmlType.createUmlType(typeName, UmlType.TypeLevel.CUSTOM)
                        Toast.makeText(this, R.string.toast_custom_type_created, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun menuDeleteCustomTypes() {
        // Cambiamos getUmlTypes() por umlTypes y lo convertimos a TypedArray
        val listArray = UmlType.umlTypes.filter { it.isCustomUmlType }.map { it.name }.toTypedArray()

        val listView = ListView(this).apply {
            choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_multiple_choice, listArray)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_custom_types_title)
            .setMessage(R.string.dialog_delete_custom_types_message)
            .setView(listView)
            .setNegativeButton(R.string.button_cancel) { _, _ -> }
            .setPositiveButton(R.string.button_ok) { _, _ ->
                val checkMapping = listView.checkedItemPositions
                for (j in 0 until checkMapping.size()) {
                    if (checkMapping.valueAt(j)) {
                        val typeName = listView.getItemAtPosition(checkMapping.keyAt(j)).toString()
                        // Aquí también usamos umlTypes
                        val t = UmlType.valueOf(typeName, UmlType.umlTypes)
                        if (t != null) {
                            UmlType.removeUmlType(t)
                            project?.removeParametersOfType(t)
                            project?.removeMethodsOfType(t)
                            project?.removeAttributesOfType(t)
                        }
                    }
                }
                mGraphView.invalidate()
            }
            .show()
    }

    private fun menuExportCustomTypes() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { type = "text/*" }
        startActivityForResult(intent, INTENT_CREATE_DOCUMENT_EXPORT_CUSTOM_TYPES)
    }

    private fun menuImportCustomTypes() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "*/*" }
        startActivityForResult(intent, INTENT_OPEN_DOCUMENT_IMPORT_CUSTOM_TYPES)
    }

    private fun menuHelp() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_help_title)
            .setMessage(Html.fromHtml(IOUtils.readRawHtmlFile(this, R.raw.help_html), Html.FROM_HTML_MODE_LEGACY))
            .setPositiveButton(R.string.button_ok) { _, _ -> }
            .show()
    }

    // **********************************************************************************************
    // Intents
    // **********************************************************************************************
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return

        val fileNameUri = data.data!!
        when (requestCode) {
            INTENT_CREATE_DOCUMENT_EXPORT_PROJECT -> project?.exportProject(this, fileNameUri)
            INTENT_OPEN_DOCUMENT_IMPORT_PROJECT -> {
                UmlType.clearProjectUmlTypes()
                project = UmlProject.importProject(this, fileNameUri)
                mGraphView.setUmlProject(project)
            }
            INTENT_CREATE_DOCUMENT_EXPORT_CUSTOM_TYPES -> UmlType.exportCustomUmlTypes(this, fileNameUri)
            INTENT_OPEN_DOCUMENT_IMPORT_CUSTOM_TYPES -> UmlType.importCustomUmlTypes(this, fileNameUri)
            INTENT_CREATE_DOCUMENT_EXPORT_IMAGE -> exportImageToUri(fileNameUri)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION && grantResults.isNotEmpty()) {
            sWriteExternalStoragePermission = grantResults[WRITE_EXTERNAL_STORAGE_INDEX] == PackageManager.PERMISSION_GRANTED
            sReadExternalStoragePermission = grantResults[READ_EXTERNAL_STORAGE_INDEX] == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun saveAs(projectName: String) {
        project?.name = projectName
        updateNavigationView()
        project?.save(applicationContext)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun checkPermissions() {
        // En Android 13+ (TIRAMISU) o superior, los permisos de lectura generales ya no existen así.
        // Si solo usas Storage Access Framework (Intent), no necesitas pedir nada.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val permissionsNeeded = mutableListOf<String>()

            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            if (permissionsNeeded.isNotEmpty()) {
                requestPermissions(permissionsNeeded.toTypedArray(), REQUEST_PERMISSION)
            }
        }
    }

    companion object {
        private var sWriteExternalStoragePermission = true
        private var sReadExternalStoragePermission = true
        private const val WRITE_EXTERNAL_STORAGE_INDEX = 0
        private const val READ_EXTERNAL_STORAGE_INDEX = 1

        private const val DOUBLE_BACK_PRESSED_DELAY: Long = 2000
        private const val GRAPH_FRAGMENT_TAG = "graphFragment"
        private const val CLASS_EDITOR_FRAGMENT_TAG = "classEditorFragment"
        private const val ATTRIBUTE_EDITOR_FRAGMENT_TAG = "attributeEditorFragment"
        private const val METHOD_EDITOR_FRAGMENT_TAG = "methodEditorFragment"
        private const val PARAMETER_EDITOR_FRAGMENT_TAG = "parameterEditorFragment"

        private const val SHARED_PREFERENCES_PROJECT_NAME = "sharedPreferencesProjectName"

        private const val INTENT_CREATE_DOCUMENT_EXPORT_PROJECT = 1000
        private const val INTENT_OPEN_DOCUMENT_IMPORT_PROJECT = 2000
        private const val INTENT_CREATE_DOCUMENT_EXPORT_CUSTOM_TYPES = 3000
        private const val INTENT_OPEN_DOCUMENT_IMPORT_CUSTOM_TYPES = 4000
        private const val INTENT_CREATE_DOCUMENT_EXPORT_IMAGE = 6000

        private const val REQUEST_PERMISSION = 5000
    }
}