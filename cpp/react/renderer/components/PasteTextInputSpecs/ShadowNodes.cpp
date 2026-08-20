#include <react/renderer/components/PasteTextInputSpecs/ShadowNodes.h>

#include <limits>

#include <react/renderer/attributedstring/AttributedStringBox.h>
#include <react/renderer/attributedstring/conversions.h>
#include <react/renderer/components/text/BaseTextShadowNode.h>
#include <react/renderer/core/LayoutConstraints.h>
#include <react/renderer/core/LayoutContext.h>
#include <react/renderer/textlayoutmanager/TextLayoutContext.h>

namespace facebook::react {
namespace {

FontWeight fontWeightFromString(const std::string& value) {
  if (value == "100") return FontWeight::Weight100;
  if (value == "200") return FontWeight::Weight200;
  if (value == "300" || value == "light") return FontWeight::Weight300;
  if (value == "500" || value == "medium") return FontWeight::Weight500;
  if (value == "600" || value == "semibold") return FontWeight::Weight600;
  if (value == "700" || value == "bold") return FontWeight::Weight700;
  if (value == "800") return FontWeight::Weight800;
  if (value == "900") return FontWeight::Weight900;
  return FontWeight::Regular;
}

FontStyle fontStyleFromString(const std::string& value) {
  if (value == "italic") return FontStyle::Italic;
  if (value == "oblique") return FontStyle::Oblique;
  return FontStyle::Normal;
}

TextAlignment textAlignmentFromString(const std::string& value) {
  if (value == "left") return TextAlignment::Left;
  if (value == "center") return TextAlignment::Center;
  if (value == "right") return TextAlignment::Right;
  if (value == "justify") return TextAlignment::Justified;
  return TextAlignment::Natural;
}

TextTransform textTransformFromString(const std::string& value) {
  if (value == "uppercase") return TextTransform::Uppercase;
  if (value == "lowercase") return TextTransform::Lowercase;
  if (value == "capitalize") return TextTransform::Capitalize;
  return TextTransform::None;
}

TextAlignmentVertical textAlignmentVerticalFromString(
    const std::string& value) {
  if (value == "top") return TextAlignmentVertical::Top;
  if (value == "bottom") return TextAlignmentVertical::Bottom;
  if (value == "center") return TextAlignmentVertical::Center;
  return TextAlignmentVertical::Auto;
}

} // namespace

void PasteTextInputShadowNode::setTextLayoutManager(
    std::shared_ptr<const TextLayoutManager> textLayoutManager) {
  ensureUnsealed();
  textLayoutManager_ = std::move(textLayoutManager);
}

Size PasteTextInputShadowNode::measureContent(
    const LayoutContext& layoutContext,
    const LayoutConstraints& layoutConstraints) const {
  auto textConstraints = getTextConstraints(layoutConstraints);
  auto paragraphAttributes = getParagraphAttributes();
  TextLayoutContext textLayoutContext{
      .pointScaleFactor = layoutContext.pointScaleFactor,
      .surfaceId = getSurfaceId(),
  };

  // ReactEditText stores its current Spannable in the native text cache on
  // every edit. Measuring that object preserves Android's exact wrapping and
  // font metrics without asking JS to count lines or characters.
  if (getStateData().cachedAttributedStringId != 0) {
    auto textSize = textLayoutManager_
                        ->measureCachedSpannableById(
                            getStateData().cachedAttributedStringId,
                            paragraphAttributes,
                            textLayoutContext,
                            textConstraints)
                        .size;
    return layoutConstraints.clamp(textSize);
  }

  auto attributedString = getMostRecentAttributedString(layoutContext);
  if (attributedString.isEmpty()) {
    attributedString = getAttributedString(layoutContext, true);
  }

  auto textSize = textLayoutManager_
                      ->measure(
                          AttributedStringBox{attributedString},
                          paragraphAttributes,
                          textLayoutContext,
                          textConstraints)
                      .size;
  return layoutConstraints.clamp(textSize);
}

void PasteTextInputShadowNode::layout(LayoutContext layoutContext) {
  updateStateIfNeeded(layoutContext);
  ConcreteViewShadowNode::layout(layoutContext);
}

Float PasteTextInputShadowNode::baseline(
    const LayoutContext& layoutContext,
    Size size) const {
  auto attributedString = getMostRecentAttributedString(layoutContext);
  if (attributedString.isEmpty()) {
    attributedString = getAttributedString(layoutContext, true);
  }

  auto top = YGNodeLayoutGetBorder(&yogaNode_, YGEdgeTop) +
      YGNodeLayoutGetPadding(&yogaNode_, YGEdgeTop);
  return LineMeasurement::baseline(textLayoutManager_->measureLines(
             AttributedStringBox{attributedString},
             getParagraphAttributes(),
             size)) +
      top;
}

LayoutConstraints PasteTextInputShadowNode::getTextConstraints(
    const LayoutConstraints& layoutConstraints) const {
  if (getConcreteProps().multiline) {
    return layoutConstraints;
  }

  return LayoutConstraints{
      .minimumSize = layoutConstraints.minimumSize,
      .maximumSize =
          Size{
              .width = std::numeric_limits<Float>::infinity(),
              .height = layoutConstraints.maximumSize.height,
          },
      .layoutDirection = layoutConstraints.layoutDirection,
  };
}

TextAttributes PasteTextInputShadowNode::getTextAttributes(
    const LayoutContext& layoutContext) const {
  const auto& props = getConcreteProps();
  auto attributes = TextAttributes::defaultTextAttributes();

  attributes.fontSizeMultiplier = layoutContext.fontSizeMultiplier;
  attributes.allowFontScaling = props.allowFontScaling;
  if (props.maxFontSizeMultiplier > 0) {
    attributes.maxFontSizeMultiplier = props.maxFontSizeMultiplier;
  }
  if (props.fontSize > 0) attributes.fontSize = props.fontSize;
  if (props.lineHeight > 0) attributes.lineHeight = props.lineHeight;
  if (props.letterSpacing != 0) attributes.letterSpacing = props.letterSpacing;
  if (!props.fontFamily.empty()) attributes.fontFamily = props.fontFamily;
  if (!props.fontWeight.empty()) {
    attributes.fontWeight = fontWeightFromString(props.fontWeight);
  }
  if (!props.fontStyle.empty()) {
    attributes.fontStyle = fontStyleFromString(props.fontStyle);
  }
  if (!props.textAlign.empty()) {
    attributes.alignment = textAlignmentFromString(props.textAlign);
  }
  if (!props.textTransform.empty()) {
    attributes.textTransform = textTransformFromString(props.textTransform);
  }

  return attributes;
}

ParagraphAttributes PasteTextInputShadowNode::getParagraphAttributes() const {
  const auto& props = getConcreteProps();
  auto attributes = ParagraphAttributes{};

  attributes.maximumNumberOfLines = props.numberOfLines;
  attributes.includeFontPadding = props.includeFontPadding;
  switch (props.textBreakStrategy) {
    case PasteTextInputTextBreakStrategy::Simple:
      attributes.textBreakStrategy = TextBreakStrategy::Simple;
      break;
    case PasteTextInputTextBreakStrategy::Balanced:
      attributes.textBreakStrategy = TextBreakStrategy::Balanced;
      break;
    case PasteTextInputTextBreakStrategy::HighQuality:
      attributes.textBreakStrategy = TextBreakStrategy::HighQuality;
      break;
  }
  if (!props.textAlignVertical.empty()) {
    attributes.textAlignVertical =
        textAlignmentVerticalFromString(props.textAlignVertical);
  }

  return attributes;
}

AttributedString PasteTextInputShadowNode::getAttributedString(
    const LayoutContext& layoutContext,
    bool usePlaceholder) const {
  const auto& props = getConcreteProps();
  const auto& string = usePlaceholder ? props.placeholder : props.text;
  auto textAttributes = getTextAttributes(layoutContext);
  auto attributedString = AttributedString{};
  attributedString.setBaseTextAttributes(textAttributes);

  if (!string.empty()) {
    auto fragment = AttributedString::Fragment{};
    fragment.string = string;
    fragment.textAttributes = textAttributes;
    fragment.textAttributes.backgroundColor = clearColor();
    fragment.parentShadowView = ShadowView(*this);
    attributedString.appendFragment(std::move(fragment));
  }

  return attributedString;
}

AttributedString PasteTextInputShadowNode::getMostRecentAttributedString(
    const LayoutContext& layoutContext) const {
  const auto& state = getStateData();
  auto reactTreeAttributedString = getAttributedString(layoutContext);
  auto treeChanged =
      !state.reactTreeAttributedString.compareTextAttributesWithoutFrame(
          reactTreeAttributedString);

  return treeChanged ? reactTreeAttributedString
                     : state.attributedStringBox.getValue();
}

void PasteTextInputShadowNode::updateStateIfNeeded(
    const LayoutContext& layoutContext) {
  ensureUnsealed();
  const auto& stateData = getStateData();
  auto reactTreeAttributedString = getAttributedString(layoutContext);

  if (stateData.reactTreeAttributedString == reactTreeAttributedString) {
    return;
  }

  const auto& props = BaseShadowNode::getConcreteProps();
  if (props.mostRecentEventCount < stateData.mostRecentEventCount) {
    return;
  }

  auto newEventCount = stateData.reactTreeAttributedString.isContentEqual(
                           reactTreeAttributedString)
      ? 0
      : props.mostRecentEventCount;
  auto newAttributedString = getMostRecentAttributedString(layoutContext);

  setStateData(AndroidTextInputState{
      AttributedStringBox(newAttributedString),
      reactTreeAttributedString,
      getParagraphAttributes(),
      newEventCount});
}

} // namespace facebook::react
