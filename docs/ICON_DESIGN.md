# DeepEye Unlocker - App Icon Design

## Icon Overview

The DeepEyeUnlocker icon combines several symbolic elements to represent the tool's purpose:

### Visual Elements

1. **The Eye**: Represents the "DeepEye" brand and symbolizes vision, perception, and insight
2. **Circuit Board Pattern**: Integrated into the iris to represent technology and electronic device repair
3. **Unlock Symbol**: Keyhole in the pupil represents the core function - unlocking devices
4. **Neon Glow Effect**: Cyan and purple gradients create a futuristic, professional aesthetic
5. **Circuit Traces**: Radiating outward to symbolize connectivity and system-level access

### Color Palette

- **Primary**: Electric Cyan (#00E5FF) - Technology, precision
- **Secondary**: Purple/Violet (#9C27B0) - Premium, professional
- **Background**: Black (#0D0D0F) - Depth, sophistication
- **Accent**: Glowing effects for high-tech feel

### Design Philosophy

The icon follows modern app design principles:

- **Scalable**: Recognizable at all sizes (16px to 512px)
- **Distinctive**: Unique visual identity in app stores
- **Professional**: Suitable for developer tools and repair professionals
- **Thematic**: Clearly communicates the app's purpose

## File Structure

### Android

```
res/
├── mipmap-mdpi/           (48x48, 108x108 foreground)
├── mipmap-hdpi/           (72x72, 162x162 foreground)
├── mipmap-xhdpi/          (96x96, 216x216 foreground)
├── mipmap-xxhdpi/         (144x144, 324x324 foreground)
├── mipmap-xxxhdpi/        (192x192, 432x432 foreground)
└── mipmap-anydpi-v26/     (Adaptive icon XML)
```

### Desktop

- **Windows**: `deepeye_icon.ico` (multi-size ICO file)
- **macOS**: `deepeye_icon.png` (512x512 high-resolution)
- **Linux**: `deepeye_icon.png` (512x512)

## Generation

Run the icon generation script:

```bash
./scripts/generate-icons.sh
```

This will:

1. Check for ImageMagick (install if needed)
2. Generate all Android launcher icon densities
3. Create adaptive icon foreground layers
4. Generate desktop icons (PNG + ICO)

## Adaptive Icons (Android 8.0+)

The adaptive icon system allows:

- Different shapes on different devices (circle, rounded square, squircle)
- Foreground layer with 33% safe zone (18dp margin)
- Background layer (solid color: `#0D0D0F`)
- Smooth animations and visual effects

## Icon Guidelines Compliance

✅ **Android**:

- Follows Material Design 3 adaptive icon specs
- Proper safe zones and foreground sizing
- All required densities (mdpi through xxxhdpi)

✅ **Windows**:

- ICO with embedded sizes: 16, 32, 48, 64, 128, 256
- Transparency support

✅ **GitHub/Web**:

- High-resolution PNG for Open Graph previews
- Suitable for README badges and documentation

## Credits

Icon design generated with AI assistance and optimized for professional developer tool branding.
