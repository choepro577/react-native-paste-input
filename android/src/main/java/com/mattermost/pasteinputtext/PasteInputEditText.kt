package com.mattermost.pasteinputtext

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.events.EventDispatcher
import com.facebook.react.views.textinput.ReactEditText
import java.lang.Exception
import org.json.JSONArray
import org.json.JSONException


@SuppressLint("ViewConstructor")
class PasteInputEditText(context: ThemedReactContext) : ReactEditText(context) {
  private lateinit var mOnPasteListener: IPasteInputListener
  private lateinit var mPasteEventDispatcher: EventDispatcher
  private var mDisabledCopyPaste: Boolean = false
  // ReactEditText may invoke onTextChanged from its constructor, before this
  // subclass has initialized its fields.
  private var mMentionRangesJson: String? = "[]"
  private var mMentionTextColor: Int = DEFAULT_MENTION_TEXT_COLOR

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
    if (event != null) {
      mPasteEventDispatcher = event
    }
  }

  fun getOnPasteListener() : IPasteInputListener {
    return mOnPasteListener
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
        getOnPasteListener().onPaste(inputContentInfo.contentUri, mPasteEventDispatcher)
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
