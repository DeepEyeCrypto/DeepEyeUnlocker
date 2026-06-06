import { isJailbreakCompatible } from '../jailbreak-compat';

describe('isJailbreakCompatible', () => {
  it('Test 23: isJailbreakCompatible_palera1n_blocks_ios17', () => {
    const { compatible, reason } = isJailbreakCompatible('JailbreakPalera1n', '17.2');
    expect(compatible).toBe(false);
    expect(reason).toContain('17.2');
  });

  it('Test 24: isJailbreakCompatible_checkra1n_passes_ios14', () => {
    const { compatible, reason } = isJailbreakCompatible('JailbreakCheckra1n', '14.3');
    expect(compatible).toBe(true);
    expect(reason).toBeUndefined();
  });

  it('palera1n allows 15.0 to 16.7', () => {
    expect(isJailbreakCompatible('JailbreakPalera1n', '15.0').compatible).toBe(true);
    expect(isJailbreakCompatible('JailbreakPalera1n', '16.7.2').compatible).toBe(true);
    expect(isJailbreakCompatible('JailbreakPalera1n', '16.8').compatible).toBe(false);
  });

  it('checkra1n blocks 15.0', () => {
    expect(isJailbreakCompatible('JailbreakCheckra1n', '15.0').compatible).toBe(false);
  });

  it('fails if undefined osVersion', () => {
    const { compatible, reason } = isJailbreakCompatible('JailbreakPalera1n', undefined);
    expect(compatible).toBe(false);
    expect(reason).toBe('iOS version unknown');
  });
});
