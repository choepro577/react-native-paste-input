import type { MentionRangeStyle } from './types';

export function serializeMentionRanges(
    mentionRanges: MentionRangeStyle[] | undefined
): string {
    if (!Array.isArray(mentionRanges) || mentionRanges.length === 0) {
        return '[]';
    }

    return JSON.stringify(
        mentionRanges
            .map((range) => ({
                start: Math.trunc(Number(range.start)),
                end: Math.trunc(Number(range.end)),
                kind: typeof range.kind === 'string' ? range.kind : undefined,
            }))
            .filter(
                (range) =>
                    Number.isFinite(range.start) &&
                    Number.isFinite(range.end) &&
                    range.start >= 0 &&
                    range.end > range.start
            )
    );
}
