//
//  PasteInputTextView.m
//  PasteInput
//
//  Created by Elias Nahum on 04-11-20.
//  Copyright © 2020 Facebook. All rights reserved.
//

#import "PasteInputTextView.h"
#import "UIPasteboard+GetImageInfo.h"

static NSString *const PasteInputMentionAttributeName = @"PasteInputMentionAttribute";

@interface PasteInputTextView ()
@property (nonatomic, assign) BOOL applyingMentionAttributes;
@end

@implementation PasteInputTextView

- (instancetype)initWithFrame:(CGRect)frame
{
    if (self = [super initWithFrame:frame]) {
        _mentionRangesJson = @"[]";
        _mentionTextColor = [UIColor colorWithRed:24.0 / 255.0 green:144.0 / 255.0 blue:255.0 / 255.0 alpha:1.0];
        _applyingMentionAttributes = NO;
    }

    return self;
}

#pragma mark - Overrides

- (void)setAttributedText:(NSAttributedString *)attributedText
{
    if (self.applyingMentionAttributes || attributedText == nil) {
        [super setAttributedText:attributedText];
        return;
    }

    self.applyingMentionAttributes = YES;
    [super setAttributedText:[self attributedStringByApplyingMentionAttributes:attributedText]];
    self.applyingMentionAttributes = NO;
}

- (BOOL)canPerformAction:(SEL)action withSender:(id)sender
{
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wundeclared-selector"
    BOOL prevent = action == @selector(paste:) ||
    action == @selector(copy:) ||
    action == @selector(cut:) ||
    action == @selector(_share:);
#pragma clang diagnostic pop
    
    if (_disableCopyPaste && prevent) {
        return NO;
    }
    
    if (action == @selector(paste:) && [UIPasteboard generalPasteboard].numberOfItems > 0) {
        return true;
    }
    
    return [super canPerformAction:action withSender:sender];
}

- (void)setMentionRangesJson:(NSString *)mentionRangesJson
{
    NSString *nextRangesJson = mentionRangesJson ?: @"[]";
    if ([_mentionRangesJson isEqualToString:nextRangesJson]) {
        return;
    }

    _mentionRangesJson = [nextRangesJson copy];
    [self reapplyMentionAttributesToCurrentText];
}

- (void)setMentionTextColor:(UIColor *)mentionTextColor
{
    UIColor *nextColor = mentionTextColor ?: [UIColor colorWithRed:24.0 / 255.0 green:144.0 / 255.0 blue:255.0 / 255.0 alpha:1.0];
    if ([_mentionTextColor isEqual:nextColor]) {
        return;
    }

    _mentionTextColor = nextColor;
    [self reapplyMentionAttributesToCurrentText];
}

-(void)paste:(id)sender {
    [super paste:sender];
    
    UIPasteboard *pasteboard = [UIPasteboard generalPasteboard];

    BOOL hasStrings = pasteboard.hasStrings;
    if (hasStrings) {
        NSArray<NSString *> *strs = pasteboard.strings;
        for (NSString *s in strs) {
            hasStrings = [s length] != 0 && ![s containsString:@"<img src="];
        }
    }
    if (pasteboard.hasURLs || hasStrings || pasteboard.hasColors) {
        return;
    }
    
    if (_onPaste) {
        NSArray<NSDictionary *> *files = [pasteboard getCopiedFiles];
        if (files != nil && files.count > 0) {
            _onPaste(@{
                @"data": files,
            });
        } else {
            return;
        }
    }
}

#pragma mark - Mention text attributes

- (NSArray<NSDictionary *> *)validMentionRangesForTextLength:(NSUInteger)textLength
{
    if (self.mentionRangesJson.length == 0 || [self.mentionRangesJson isEqualToString:@"[]"]) {
        return @[];
    }

    NSData *jsonData = [self.mentionRangesJson dataUsingEncoding:NSUTF8StringEncoding];
    if (!jsonData) {
        return @[];
    }

    id parsedJson = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
    if (![parsedJson isKindOfClass:[NSArray class]]) {
        return @[];
    }

    NSMutableArray<NSDictionary *> *validRanges = [NSMutableArray array];
    NSUInteger lastEnd = 0;

    for (id item in (NSArray *)parsedJson) {
        if (![item isKindOfClass:[NSDictionary class]]) {
            continue;
        }

        NSNumber *startValue = ((NSDictionary *)item)[@"start"];
        NSNumber *endValue = ((NSDictionary *)item)[@"end"];
        if (![startValue isKindOfClass:[NSNumber class]] || ![endValue isKindOfClass:[NSNumber class]]) {
            continue;
        }

        NSInteger start = [startValue integerValue];
        NSInteger end = [endValue integerValue];
        if (start < 0 || end <= start || (NSUInteger)end > textLength || (NSUInteger)start < lastEnd) {
            continue;
        }

        [validRanges addObject:@{
            @"start": @(start),
            @"end": @(end),
        }];
        lastEnd = (NSUInteger)end;
    }

    return validRanges;
}

- (UIColor *)defaultMentionBaseTextColor
{
    UIColor *attributeColor = self.defaultTextAttributes[NSForegroundColorAttributeName];
    if ([attributeColor isKindOfClass:[UIColor class]]) {
        return attributeColor;
    }

    return self.textColor;
}

- (NSAttributedString *)attributedStringByApplyingMentionAttributes:(NSAttributedString *)attributedText
{
    if (attributedText.length == 0) {
        return attributedText;
    }

    NSMutableAttributedString *mutableText = [attributedText mutableCopy];
    NSRange fullRange = NSMakeRange(0, mutableText.length);
    UIColor *baseTextColor = [self defaultMentionBaseTextColor];
    NSMutableArray<NSValue *> *existingMentionRanges = [NSMutableArray array];

    [mutableText enumerateAttribute:PasteInputMentionAttributeName
                            inRange:fullRange
                            options:0
                         usingBlock:^(id value, NSRange range, BOOL *stop) {
        if (value) {
            [existingMentionRanges addObject:[NSValue valueWithRange:range]];
        }
    }];

    for (NSValue *rangeValue in existingMentionRanges) {
        NSRange range = [rangeValue rangeValue];
        [mutableText removeAttribute:PasteInputMentionAttributeName range:range];
        if (baseTextColor) {
            [mutableText addAttribute:NSForegroundColorAttributeName value:baseTextColor range:range];
        } else {
            [mutableText removeAttribute:NSForegroundColorAttributeName range:range];
        }
    }

    for (NSDictionary *rangeItem in [self validMentionRangesForTextLength:mutableText.length]) {
        NSUInteger start = [rangeItem[@"start"] unsignedIntegerValue];
        NSUInteger end = [rangeItem[@"end"] unsignedIntegerValue];
        NSRange mentionRange = NSMakeRange(start, end - start);

        [mutableText addAttribute:NSForegroundColorAttributeName value:self.mentionTextColor range:mentionRange];
        [mutableText addAttribute:PasteInputMentionAttributeName value:@YES range:mentionRange];
    }

    return mutableText;
}

- (void)reapplyMentionAttributesToCurrentText
{
    if (self.attributedText == nil) {
        return;
    }

    UITextRange *selectedRange = self.selectedTextRange;
    NSInteger selectionStart = NSNotFound;
    NSInteger selectionEnd = NSNotFound;
    if (selectedRange) {
        selectionStart = [self offsetFromPosition:self.beginningOfDocument toPosition:selectedRange.start];
        selectionEnd = [self offsetFromPosition:self.beginningOfDocument toPosition:selectedRange.end];
    }

    self.applyingMentionAttributes = YES;
    [super setAttributedText:[self attributedStringByApplyingMentionAttributes:self.attributedText]];
    self.applyingMentionAttributes = NO;

    if (selectionStart == NSNotFound || selectionEnd == NSNotFound) {
        return;
    }

    NSInteger textLength = (NSInteger)self.attributedText.length;
    NSInteger clampedStart = MAX(0, MIN(selectionStart, textLength));
    NSInteger clampedEnd = MAX(0, MIN(selectionEnd, textLength));
    UITextPosition *startPosition = [self positionFromPosition:self.beginningOfDocument offset:clampedStart];
    UITextPosition *endPosition = [self positionFromPosition:self.beginningOfDocument offset:clampedEnd];
    if (!startPosition || !endPosition) {
        return;
    }

    UITextRange *restoredRange = [self textRangeFromPosition:startPosition toPosition:endPosition];
    if (restoredRange) {
        [self setSelectedTextRange:restoredRange notifyDelegate:NO];
    }
}

@end
