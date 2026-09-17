import { describe, expect, it } from '@jest/globals';

import { serializeMentionRanges } from '../mentions';

describe('serializeMentionRanges', () => {
    it('keeps valid ranges and drops invalid ranges', () => {
        expect(
            JSON.parse(
                serializeMentionRanges([
                    { start: 0, end: 5, kind: 'user' },
                    { start: -1, end: 3 },
                    { start: 8.9, end: 12.7 },
                    { start: 20, end: 20 },
                ])
            )
        ).toEqual([
            { start: 0, end: 5, kind: 'user' },
            { start: 8, end: 12 },
        ]);
    });

    it('serializes an empty input as an empty array', () => {
        expect(serializeMentionRanges(undefined)).toBe('[]');
    });
});
