#pragma once

#include <react/renderer/components/PasteTextInputSpecs/EventEmitters.h>
#include <react/renderer/components/PasteTextInputSpecs/Props.h>
#include <react/renderer/components/textinput/platform/android/react/renderer/components/androidtextinput/AndroidTextInputState.h>
#include <react/renderer/components/view/ConcreteViewShadowNode.h>
#include <react/renderer/textlayoutmanager/TextLayoutManager.h>

namespace facebook::react {

extern const char PasteTextInputComponentName[];

/**
 * Android codegen otherwise models this component as a plain View, which has
 * no intrinsic text measurement. This ShadowNode mirrors the native Android
 * TextInput measurement contract while retaining the package's generated
 * props and events.
 */
class PasteTextInputShadowNode final
    : public ConcreteViewShadowNode<
          PasteTextInputComponentName,
          PasteTextInputProps,
          PasteTextInputEventEmitter,
          AndroidTextInputState> {
 public:
  using ConcreteViewShadowNode::ConcreteViewShadowNode;

  static ShadowNodeTraits BaseTraits() {
    auto traits = ConcreteViewShadowNode::BaseTraits();
    traits.set(ShadowNodeTraits::Trait::LeafYogaNode);
    traits.set(ShadowNodeTraits::Trait::MeasurableYogaNode);
    traits.set(ShadowNodeTraits::Trait::BaselineYogaNode);
    return traits;
  }

  void setTextLayoutManager(
      std::shared_ptr<const TextLayoutManager> textLayoutManager);

 protected:
  Size measureContent(
      const LayoutContext& layoutContext,
      const LayoutConstraints& layoutConstraints) const override;

  void layout(LayoutContext layoutContext) override;

  Float baseline(const LayoutContext& layoutContext, Size size) const override;

 private:
  LayoutConstraints getTextConstraints(
      const LayoutConstraints& layoutConstraints) const;
  TextAttributes getTextAttributes(const LayoutContext& layoutContext) const;
  ParagraphAttributes getParagraphAttributes() const;
  AttributedString getAttributedString(
      const LayoutContext& layoutContext,
      bool usePlaceholder = false) const;
  AttributedString getMostRecentAttributedString(
      const LayoutContext& layoutContext) const;
  void updateStateIfNeeded(const LayoutContext& layoutContext);

  std::shared_ptr<const TextLayoutManager> textLayoutManager_;
};

} // namespace facebook::react
