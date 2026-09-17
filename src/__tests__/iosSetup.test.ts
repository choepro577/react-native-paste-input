import { expect, test } from '@jest/globals';
import { readFileSync } from 'fs';
import { join } from 'path';

test('resolves the React host after Expo starts its scene, not during setup', () => {
    const source = readFileSync(
        join(__dirname, '../../ios/PasteInputModule.mm'),
        'utf8'
    );
    const setup = source.slice(
        source.indexOf('+ (void)setup:'),
        source.indexOf('- (instancetype)init')
    );
    const presenter = source.slice(
        source.indexOf('- (nullable id)getSurfacePresenter'),
        source.indexOf('- (nullable UIView *)findBackingTextViewForTag')
    );

    // The host is nil until SceneDelegate creates the first React surface.
    expect(setup).toContain('_rootViewFactory = rootViewFactory;');
    expect(setup).not.toContain('.reactHost');
    expect(presenter).toContain('id reactHost = _rootViewFactory.reactHost;');
    expect(presenter).toContain('[reactHost performSelector:');
});
