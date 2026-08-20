package com.mattermost.pasteinputtext

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.view.Gravity
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
import org.json.JSONArray
import org.json.JSONException


@SuppressLint("ViewConstructor")
class PasteInputEditText(context: ThemedReactContext) : ReactEditText(context) {
  private lateinit var mOnPasteListener: IPasteInputListener
  private var mEventDispatcher: EventDispatcher? = null
  private var mDisabledCopyPaste: Boolean = false
  private var mSurfaceId: Int = ViewUtil.NO_SURFACE_ID
  private var mPreviousContentWidth: Int = 0
  private var mPreviousContentHeight: Int = 0
  // ReactEditText can call onTextChanged from its constructor, before this
  // subclass has initialized its fields. Keep constructor-time reads nullable.
  private var mMentionRangesJson: String? = "[]"
  private var mMentionTextColor: Int = DEFAULT_MENTION_TEXT_COLOR
  private var mIsMultiline: Boolean = false
  private val dispatchContentSizeChangeRunnable: Runnable? = Runnable {
    dispatchContentSizeChangeNow()
  }

  fun setDisableCopyPaste(disabled: Boolean) {
    this.mDisabledCopyPaste = disabled
  }

  fun setMentionRangesJson(rangesJson: String?) {
    val nextRangesJson = rangesJson ?: "[]"
    if (mMentionRangesJson == nextRangesJson) {
      return
    }

    mMentionRangesJson = nextRangesJson
    applyMentionSpans()
  }

  fun setMentionTextColor(color: Int?) {
    val nextColor = color ?: DEFAULT_MENTION_TEXT_COLOR
    if (mMentionTextColor == nextColor) {
      return
    }

    mMentionTextColor = nextColor
    applyMentionSpans()
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

  private fun dispatchContentSizeChangeNow() {
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

  private fun scheduleContentSizeChangeDispatch() {
    val runnable = dispatchContentSizeChangeRunnable ?: return
    removeCallbacks(runnable)
    post(runnable)
  }

  private fun updateGravityForCurrentContent() {
    val textValue = text?.toString()?.replace("\r\n", "\n") ?: ""
    val explicitLineCount = if (textValue.isEmpty()) {
      1
    } else {
      textValue.split("\n").size
    }
    val layoutLineCount = layout?.lineCount ?: lineCount
    val shouldTopAlign = mIsMultiline && maxOf(explicitLineCount, layoutLineCount) > 1

    gravity = if (shouldTopAlign) {
      Gravity.TOP or Gravity.START
    } else {
      Gravity.CENTER_VERTICAL or Gravity.START
    }
  }

  fun syncMultilineMode(multiline: Boolean) {
    mIsMultiline = multiline
    setSingleLine(!multiline)
    setHorizontallyScrolling(!multiline)
    updateGravityForCurrentContent()
    scheduleContentSizeChangeDispatch()
  }

  private fun parseMentionRanges(textLength: Int): List<Pair<Int, Int>> {
    val rangesJson = mMentionRangesJson ?: "[]"
    if (rangesJson.isBlank() || rangesJson == "[]") {
      return emptyList()
    }

    return try {
      val ranges = mutableListOf<Pair<Int, Int>>()
      val jsonRanges = JSONArray(rangesJson)
      var lastEnd = 0

      for (index in 0 until jsonRanges.length()) {
        val range = jsonRanges.optJSONObject(index) ?: continue
        val start = range.optInt("start", -1)
        val end = range.optInt("end", -1)

        if (start < 0 || end <= start || end > textLength || start < lastEnd) {
          continue
        }

        ranges.add(start to end)
        lastEnd = end
      }

      ranges
    } catch (_: JSONException) {
      emptyList()
    }
  }

  private fun applyMentionSpans() {
    val editable = text ?: return
    val existingSpans = editable.getSpans(
      0,
      editable.length,
      MentionForegroundColorSpan::class.java,
    )

    for (span in existingSpans) {
      editable.removeSpan(span)
    }

    for ((start, end) in parseMentionRanges(editable.length)) {
      editable.setSpan(
        MentionForegroundColorSpan(mMentionTextColor),
        start,
        end,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
      )
    }
  }

  override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
    super.onTextChanged(text, start, lengthBefore, lengthAfter)
    applyMentionSpans()
    updateGravityForCurrentContent()
    scheduleContentSizeChangeDispatch()
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)
    updateGravityForCurrentContent()
    scheduleContentSizeChangeDispatch()
  }

  override fun onSelectionChanged(selStart: Int, selEnd: Int) {
    super.onSelectionChanged(selStart, selEnd)
    if (mIsMultiline) {
      updateGravityForCurrentContent()
      scheduleContentSizeChangeDispatch()
    }
  }

  override fun onDetachedFromWindow() {
    dispatchContentSizeChangeRunnable?.let(::removeCallbacks)
    super.onDetachedFromWindow()
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

  companion object {
    private val DEFAULT_MENTION_TEXT_COLOR = Color.rgb(24, 144, 255)
  }
}

private class MentionForegroundColorSpan(color: Int) : ForegroundColorSpan(color)
