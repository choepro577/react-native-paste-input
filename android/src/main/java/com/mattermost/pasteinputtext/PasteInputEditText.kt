package com.mattermost.pasteinputtext

import android.annotation.SuppressLint
import android.os.Build
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import com.facebook.react.uimanager.PixelUtil.toDIPFromPixel
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.common.ViewUtil
import com.facebook.react.uimanager.events.EventDispatcher
import com.facebook.react.views.textinput.ReactEditText
import java.lang.Exception


@SuppressLint("ViewConstructor")
class PasteInputEditText(context: ThemedReactContext) : ReactEditText(context) {
  private lateinit var mOnPasteListener: IPasteInputListener
  private var mEventDispatcher: EventDispatcher? = null
  private var mDisabledCopyPaste: Boolean = false
  private var mSurfaceId: Int = ViewUtil.NO_SURFACE_ID
  private var mPreviousContentWidth: Int = 0
  private var mPreviousContentHeight: Int = 0

  fun setDisableCopyPaste(disabled: Boolean) {
    this.mDisabledCopyPaste = disabled
  }

  fun setOnPasteListener(listener: IPasteInputListener, event: EventDispatcher?) {
    mOnPasteListener = listener
    mEventDispatcher = event
  }

  fun setEventDispatcher(surfaceId: Int, event: EventDispatcher?) {
    mSurfaceId = surfaceId
    mEventDispatcher = event
  }

  fun getOnPasteListener() : IPasteInputListener {
    return mOnPasteListener
  }

  private fun dispatchContentSizeChange() {
    val eventDispatcher = mEventDispatcher ?: return

    var contentWidth = width
    var contentHeight = height

    layout?.let { textLayout ->
      contentWidth = compoundPaddingLeft + textLayout.width + compoundPaddingRight
      contentHeight = compoundPaddingTop + textLayout.height + compoundPaddingBottom
    }

    if (contentWidth <= 0 || contentHeight <= 0) {
      return
    }

    if (contentWidth == mPreviousContentWidth && contentHeight == mPreviousContentHeight) {
      return
    }

    mPreviousContentWidth = contentWidth
    mPreviousContentHeight = contentHeight

    eventDispatcher.dispatchEvent(
      PasteTextInputContentSizeChangeEvent(
        mSurfaceId,
        id,
        toDIPFromPixel(contentWidth.toFloat()),
        toDIPFromPixel(contentHeight.toFloat()),
      ),
    )
  }

  override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
    super.onTextChanged(text, start, lengthBefore, lengthAfter)
    dispatchContentSizeChange()
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)
    dispatchContentSizeChange()
  }

  override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
    val ic = super.onCreateInputConnection(outAttrs)

    EditorInfoCompat.setContentMimeTypes(outAttrs, arrayOf("*/*"))

    val callback = InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, _ ->
      val lacksPermission = (flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && lacksPermission) {
        try {
            inputContentInfo.requestPermission()
        } catch (e: Exception) {
          return@OnCommitContentListener false
        }
      }

      if (!mDisabledCopyPaste) {
        getOnPasteListener().onPaste(inputContentInfo.contentUri, mEventDispatcher)
      }

      true
    }

    return InputConnectionCompat.createWrapper(ic!!, outAttrs, callback)
  }
}
