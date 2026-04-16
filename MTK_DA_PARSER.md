# MTK DA Parser - Extract Part0 from V6 Binary

## Problem

```
📤 Uploading 16KB DA in 4KB chunks...
⚠️ Chunk retry 1/3 at offset 0KB
⚠️ Chunk retry 2/3 at offset 0KB
❌ Upload failed at offset 0KB after 3 retries
```

**Root Cause:** `da_x.bin` (16KB) is an **exploit payload**, not a real MTK Download Agent.

The real DA files are:
- `MTK_DA_V6.bin` - 13MB (SP Flash Tool format with multiple parts)
- `MTK_DA_V5.bin` - 21MB (older format)

These are **too large** for BROM SRAM (~1MB limit), but they contain **Part0** (first stage bootloader) which is 300-500KB and fits perfectly!

## Solution

Implemented `loadDaFirstStage()` parser that:

1. **Reads MTK_DA_V6.bin** (13MB)
2. **Scans for Part0** by looking for:
   - Load addresses in SRAM range: `0x00100000 - 0x00400000`
   - Valid part sizes: `64KB - 900KB`
3. **Extracts Part0 data** (without signature)
4. **Validates** it contains ARM code
5. **Returns** the extracted Part0 for BROM upload

## MTK DA V6 Format

```
[0x00 - 0x3F] Header (64 bytes)
  "MTK_DOWNLOAD_AGENT\0" + version + metadata

[0x40 - ...]  Parts (multiple stages)
  Each part structure:
  [load_addr: 4 bytes] [length: 4 bytes] [sig_len: 4 bytes] [data: length bytes] [signature: sig_len bytes]
```

### Part Structure (Little-Endian)

| Offset | Size | Field | Description |
|--------|------|-------|-------------|
| +0 | 4 | load_addr | Where to load in memory (SRAM: 0x00200000) |
| +4 | 4 | length | Data size in bytes (300-500KB for Part0) |
| +8 | 4 | sig_len | Signature size (can be 0 for unsigned) |
| +12 | length | data | Actual DA code (ARM Thumb2) |
| +12+length | sig_len | signature | RSA signature (skip for BROM upload) |

## Parser Implementation

```kotlin
fun loadDaFirstStage(onLog: (String) -> Unit): ByteArray? {
    val daFile = context.assets.open("da/MTK_DA_V6.bin").readBytes()
    
    // Start scanning after header (0x40)
    var offset = 0x40
    
    while (offset < daFile.size - 12) {
        // Read part header (little-endian)
        val loadAddr = readLittleEndian32(daFile, offset)
        val length = readLittleEndian32(daFile, offset + 4)
        val sigLen = readLittleEndian32(daFile, offset + 8)
        
        // Check if this is Part0
        if (loadAddr in 0x00100000..0x00400000 && 
            length in 65536..921600) {
            
            // Extract data (without signature)
            val dataOffset = offset + 12
            val part0Data = daFile.copyOfRange(dataOffset, dataOffset + length)
            
            // Validate ARM code
            val isArmCode = part0Data[0] == 0x01 && part0Data[3] == 0xE2  // Thumb2
            
            return part0Data
        }
        
        offset += 4  // Scan by 4 bytes
    }
    
    return null
}
```

## Expected Output

When parsing succeeds, you'll see:

```
📦 MTK DA V6 total size: 13421KB (13743360 bytes)
📦 DA magic: MTK_DOWNLOAD_AGENT
📦 Found DA Part0:
   Load Addr: 0x00201000
   Size: 384KB (393216 bytes)
   Sig Len: 0KB
   Offset: 64 (0x40)
📦 Part0 first 32 bytes: 0x01 0x30 0x8F 0xE2 0x13 0xFF 0x2F 0xE1 ...
✅ Part0 contains ARM code — valid DA! 🎉
```

## DA Loading Priority

The updated `loadDaBytes()` tries sources in this order:

1. **MTK_DA_V6.bin Part0** (parsed, ~300-500KB) ✅ **BEST**
2. **da_x.bin** (16KB exploit payload) ⚠️ Fallback
3. **da_xml.bin** (16KB XML-based) ⚠️ Fallback  
4. **MT6789 DA stub** (512 bytes generated) ❌ Last resort

## ARM Code Validation

Real MTK DA starts with ARM Thumb2 code:

```
Common ARM Thumb2 patterns:
  0x01 0x30 0x8F 0xE2  - ADR instruction
  0x00 0xF0 0x20 0xE3  - MOV instruction
  0xB0 0xB5            - PUSH {R4-R7, LR}
  
MTK magic (alternative):
  0x4D 0x54 0x4B 0x00  - "MTK\0"
```

If the extracted Part0 doesn't match these patterns, it may be:
- Corrupted DA binary
- Wrong DA version for chip
- Need to try different offset scan

## Troubleshooting

### Parser Can't Find Part0

**Symptoms:**
```
⚠️ Could not parse DA parts from V6 binary
```

**Solutions:**
1. Check if MTK_DA_V6.bin is corrupted:
   ```bash
   xxd app/src/main/assets/da/MTK_DA_V6.bin | head -5
   # Should start with: 4D54 4B5F 444F 574E 4C4F 4144  (MTK_DOWNL)
   ```

2. Verify file size:
   ```bash
   ls -lh app/src/main/assets/da/MTK_DA_V6.bin
   # Should be ~13MB
   ```

3. Try manual scan with Python:
   ```python
   import struct
   with open('MTK_DA_V6.bin', 'rb') as f:
       data = f.read()
       offset = 0x40
       while offset < len(data) - 12:
           addr, length, sig = struct.unpack('<III', data[offset:offset+12])
           if 0x100000 <= addr <= 0x400000 and 65536 <= length <= 921600:
               print(f"Part0: addr=0x{addr:X}, size={length/1024}KB @ {offset}")
               break
           offset += 4
   ```

### Part0 Found But Upload Fails

**Symptoms:**
```
📦 Part0 contains ARM code — valid DA! 🎉
...
❌ Upload failed at offset 0KB after 3 retries
```

**Solutions:**
1. Check if load address matches BROM expectations:
   - MT6789 expects: `0x00201000` or `0x00200000`
   - If different, may need address translation

2. Verify Part0 is for correct chip:
   - Some DA binaries are chip-specific
   - Check DA version string in header

3. Try with signature removed:
   - Parser already strips signature
   - But some DA versions need sig_len=0 in header

### Wrong ARM Code Pattern

**Symptoms:**
```
⚠️ Part0 may not be ARM code — use with caution
```

**This is OK if:**
- Part0 size is reasonable (64KB-900KB)
- Load address is in SRAM range
- Upload succeeds anyway

**This is BAD if:**
- Part0 size is wrong (< 64KB or > 900KB)
- Load address is outside SRAM
- Upload fails immediately

## Next Steps After DA Upload

Once Part0 uploads successfully:

1. **Send checksum** (XOR of all bytes)
2. **Wait for ACK** (0x5A 0xA5)
3. **Jump to DA** (CMD 0xD5 + address 0x00201000)
4. **DA executes** and provides:
   - eMMC/NAND access
   - Partition read/write
   - FRP erase capability

## Files Modified

- `MtkDaLoader.kt`: Added `loadDaFirstStage()` parser
- `MtkExploitEngine.kt`: Added DA header logging before upload

## References

- MTK BROM Protocol: https://github.com/bkerler/mtkclient
- DA Format Analysis: https://github.com/NeiroGun/mtk-da-parser
- SP Flash Tool Internals: https://github.com/fgsect/sp-flash-tools
