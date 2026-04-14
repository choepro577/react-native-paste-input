package com.mattermost.pasteinputtext;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.Event;

public class PasteTextInputContentSizeChangeEvent extends Event<PasteTextInputContentSizeChangeEvent> {
  private static final String EVENT_NAME = "topContentSizeChange";
  private final float mContentWidth;
  private final float mContentHeight;

  public PasteTextInputContentSizeChangeEvent(int surfaceId, int viewId, float contentWidth, float contentHeight) {
    super(surfaceId, viewId);
    mContentWidth = contentWidth;
    mContentHeight = contentHeight;
  }

  @Override
  public boolean canCoalesce() {
    return false;
  }

  @NonNull
  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Nullable
  @Override
  protected WritableMap getEventData() {
    WritableMap contentSize = Arguments.createMap();
    contentSize.putDouble("width", mContentWidth);
    contentSize.putDouble("height", mContentHeight);

    WritableMap eventData = Arguments.createMap();
    eventData.putMap("contentSize", contentSize);
    eventData.putInt("target", getViewTag());
    return eventData;
  }
}
