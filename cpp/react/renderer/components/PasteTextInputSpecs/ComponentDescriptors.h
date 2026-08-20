#pragma once

#include <react/renderer/components/PasteTextInputSpecs/ShadowNodes.h>
#include <react/renderer/core/ConcreteComponentDescriptor.h>
#include <react/renderer/componentregistry/ComponentDescriptorProviderRegistry.h>

namespace facebook::react {

class PasteTextInputComponentDescriptor final
    : public ConcreteComponentDescriptor<PasteTextInputShadowNode> {
 public:
  PasteTextInputComponentDescriptor(
      const ComponentDescriptorParameters& parameters)
      : ConcreteComponentDescriptor<PasteTextInputShadowNode>(parameters),
        textLayoutManager_(
            std::make_shared<TextLayoutManager>(contextContainer_)) {}

  State::Shared createInitialState(
      const Props::Shared& props,
      const ShadowNodeFamily::Shared& family) const override {
    return std::make_shared<PasteTextInputShadowNode::ConcreteState>(
        std::make_shared<const AndroidTextInputState>(
            AndroidTextInputState({}, {}, {}, 0)),
        family);
  }

 protected:
  void adopt(ShadowNode& shadowNode) const override {
    auto& textInputShadowNode =
        static_cast<PasteTextInputShadowNode&>(shadowNode);
    textInputShadowNode.setTextLayoutManager(textLayoutManager_);
    textInputShadowNode.dirtyLayout();
    ConcreteComponentDescriptor::adopt(shadowNode);
  }

 private:
  const std::shared_ptr<TextLayoutManager> textLayoutManager_;
};

void PasteTextInputSpecs_registerComponentDescriptorsFromCodegen(
    std::shared_ptr<const ComponentDescriptorProviderRegistry> registry);

} // namespace facebook::react
