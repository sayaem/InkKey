package com.example.keyboard

import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.data.FontRepository
import com.example.data.KeyboardHeightPreset
import com.example.data.ThemeMode
import com.example.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class InkKeyInputMethodService : InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    KeyboardActionListener {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var fontRepository: FontRepository
    private lateinit var preferencesRepository: UserPreferencesRepository

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        fontRepository = FontRepository(this)
        preferencesRepository = UserPreferencesRepository(this)
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@InkKeyInputMethodService)
            setViewTreeViewModelStoreOwner(this@InkKeyInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@InkKeyInputMethodService)

            setContent {
                val activeProfile by fontRepository.activeProfileFlow.collectAsState(initial = null)
                val prefs by preferencesRepository.userPreferencesFlow.collectAsState(
                    initial = com.example.data.UserPreferences()
                )

                val isDark = when (prefs.themeMode) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> false
                }

                InkKeyKeyboardView(
                    profileName = activeProfile?.name ?: "My Handwriting",
                    imeOptions = currentInputEditorInfo?.imeOptions ?: EditorInfo.IME_ACTION_NONE,
                    hapticsEnabled = prefs.hapticsEnabled,
                    keyHeightDp = prefs.keyboardHeight.rowHeightDp,
                    isDarkTheme = isDark,
                    listener = this@InkKeyInputMethodService
                )
            }
        }
        return composeView
    }

    override fun onCommitText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun onDelete() {
        val ic = currentInputConnection ?: return
        val selectedText = ic.getSelectedText(0)
        if (selectedText.isNullOrEmpty()) {
            ic.deleteSurroundingText(1, 0)
        } else {
            ic.commitText("", 1)
        }
    }

    override fun onPerformAction(actionId: Int) {
        val ic = currentInputConnection ?: return
        if (actionId != 0 && actionId != EditorInfo.IME_ACTION_NONE) {
            ic.performEditorAction(actionId)
        } else {
            ic.commitText("\n", 1)
        }
    }

    override fun onSwitchInputMethod() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showInputMethodPicker()
    }

    override fun onHideKeyboard() {
        requestHideSelf(0)
    }

    override fun onOpenHandwritingStudio() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_ROUTE", "EXPORT_STUDIO")
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
        super.onDestroy()
    }
}
