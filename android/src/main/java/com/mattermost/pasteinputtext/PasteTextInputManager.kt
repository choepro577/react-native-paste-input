package com.mattermost.pasteinputtext

import android.text.InputType
import android.view.ViewGroup
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import com.facebook.react.common.MapBuilder
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.events.EventDispatcher
import com.facebook.react.views.textinput.ReactEditText
import com.facebook.react.views.textinput.ReactTextInputManager

@ReactModule(name = "PasteTextInput")
class PasteTextInputManager(context: ReactApplicationContext) : ReactTextInputManager() {
  private var disableCopyPaste: Boolean = false
  private val mContext = context

  override fun getName(): String = NAME

  @com.facebook.react.uimanager.annotations.ReactProp(name = "disableCopyPaste", defaultBoolean = false)
  fun setDisableCopyPaste(editText: PasteInputEditText, disabled: Boolean) {
    disableCopyPaste = disabled
    val eventDispatcher = getEventDispatcher(mContext, editText)
    editText.customInsertionActionModeCallback = PasteInputActionCallback(editText, disabled, eventDispatcher)
    editText.customSelectionActionModeCallback = PasteInputActionCallback(editText, disabled, eventDispatcher)
    editText.setDisableCopyPaste(disabled)
  }

  private fun getEventDispatcher(reactContext: ReactContext, editText: ReactEditText): EventDispatcher? {
    return UIManagerHelper.getEventDispatcherForReactTag(reactContext, editText.id)
  }

  override fun createViewInstance(context: ThemedReactContext): PasteInputEditText {
    val editText = PasteInputEditText(context)
    val inputType = editText.inputType

    editText.inputType = inputType and (InputType.TYPE_TEXT_FLAG_MULTI_LINE.inv())
    editText.returnKeyType = "done"
    editText.layoutParams =
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
      )
    val eventDispatcher = getEventDispatcher(mContext, editText)
    editText.customInsertionActionModeCallback = PasteInputActionCallback(editText, disableCopyPaste, eventDispatcher)
    editText.customSelectionActionModeCallback = PasteInputActionCallback(editText, disableCopyPaste, eventDispatcher)

    return editText
  }

  override fun onAfterUpdateTransaction(view: ReactEditText) {
    super.onAfterUpdateTransaction(view)
    val multiline = (view.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
    view.setSingleLine(!multiline)
    view.setHorizontallyScrolling(!multiline)
  }

  override fun addEventEmitters(reactContext: ThemedReactContext, editText: ReactEditText) {
    super.addEventEmitters(reactContext, editText)

    val pasteInputEditText = editText as PasteInputEditText
    val eventDispatcher = getEventDispatcher(reactContext, editText)
    pasteInputEditText.setOnPasteListener(PasteInputListener(pasteInputEditText, reactContext.surfaceId), eventDispatcher)
  }

  override fun getExportedCustomBubblingEventTypeConstants(): MutableMap<String, Any> {
    val map = (super.getExportedCustomBubblingEventTypeConstants() ?: mutableMapOf()).toMutableMap()
    map["onPaste"] = MapBuilder.of<String, Any>(
      "phasedRegistrationNames",
      MapBuilder.of("bubbled", "onPaste") as Any,
    )

    return map
  }

  companion object {
    const val NAME = "PasteTextInput"
    const val CACHE_DIR_NAME = "mmPasteInput"
  }
}
