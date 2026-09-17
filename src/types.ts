import type { ForwardedRef } from 'react';
import type { ColorValue, HostComponent, TextInputProps } from 'react-native';
import type { Int32 } from 'react-native/Libraries/Types/CodegenTypes';

type HostInstance = InstanceType<HostComponent<PasteInputProps>>;

export type PasteTextInputInstance = HostInstance & {
    clear: () => void;
    isFocused: () => boolean;
    getNativeRef?: () => HostInstance;
    setSelection: (start: number, end: number) => void;
};

export interface PastedFile {
    fileName: string;
    fileSize: Int32;
    type: string;
    uri: string;
}

export interface PasteEvent {
    nativeEvent: {
        data: PastedFile[];
        error?: {
            message: string;
        };
    };
}

export interface MentionRangeStyle {
    start: number;
    end: number;
    kind?: string;
}

/**
 * Smart punctuation settings for iOS
 */
export type SmartPunctuation = 'default' | 'enable' | 'disable';

/**
 * Props for the PasteInput component
 * Extends all standard TextInput props
 */
export interface PasteInputProps extends TextInputProps {
    forwardedRef?: ForwardedRef<PasteTextInputInstance>;
    /**
     * Maps to numberOfLines on Android. Mirrors the HTML rows attribute.
     */
    rows?: number;
    /**
     * Disable copy, cut, and paste actions
     */
    disableCopyPaste?: boolean;

    /** Character ranges rendered with mentionTextColor. */
    mentionRanges?: MentionRangeStyle[];

    /** Text color used for mentionRanges. */
    mentionTextColor?: ColorValue;

    /**
     * Configure smart punctuation (iOS only)
     */
    smartPunctuation?: SmartPunctuation;

    /**
     * Callback when files are pasted
     * @param error - Error message if paste failed, null otherwise
     * @param files - Array of pasted files
     */
    onPaste?(error: string | null | undefined, files: Array<PastedFile>): void;
}

export interface Selection {
    start: number;
    end?: number | undefined;
}

export type SubmitBehavior = 'submit' | 'blurAndSubmit' | 'newline';
