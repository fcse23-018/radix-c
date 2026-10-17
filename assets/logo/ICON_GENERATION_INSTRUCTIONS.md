# App Icon Generation Instructions

The SVG logo has been created at `assets/logo/terrochroma_logo.svg`. To generate the required PNG icons for Android and iOS, follow these instructions:

## Android Icons

### Required Densities
- mdpi: 48x48
- hdpi: 72x72
- xhdpi: 96x96
- xxhdpi: 144x144
- xxxhdpi: 192x192

### Adaptive Icon Layers
- **Foreground**: The logo content (leaves, spectrum lines, "TC" text)
- **Background**: Solid color (#5D4037 - Soil Brown)
- **Monochrome**: Single-color version for Android 13+

### Using ImageMagick (CLI)

```bash
# Convert SVG to PNG at various densities
convert -density 300 -background none terrochroma_logo.svg -resize 48x48 ic_launcher_mdpi.png
convert -density 300 -background none terrochroma_logo.svg -resize 72x72 ic_launcher_hdpi.png
convert -density 300 -background none terrochroma_logo.svg -resize 96x96 ic_launcher_xhdpi.png
convert -density 300 -background none terrochroma_logo.svg -resize 144x144 ic_launcher_xxhdpi.png
convert -density 300 -background none terrochroma_logo.svg -resize 192x192 ic_launcher_xxxhdpi.png

# Create foreground layer (transparent background)
convert -density 300 -background none terrochroma_logo.svg -resize 108x108 ic_launcher_foreground.png

# Create monochrome version
convert -density 300 -background none -colorspace Gray terrochroma_logo.svg -resize 108x108 ic_launcher_monochrome.png
```

### File Placement
After generating PNGs, place them in:
- `composeApp/androidApp/src/main/res/mipmap-mdpi/ic_launcher.png`
- `composeApp/androidApp/src/main/res/mipmap-hdpi/ic_launcher.png`
- `composeApp/androidApp/src/main/res/mipmap-xhdpi/ic_launcher.png`
- `composeApp/androidApp/src/main/res/mipmap-xxhdpi/ic_launcher.png`
- `composeApp/androidApp/src/main/res/mipmap-xxxhdpi/ic_launcher.png`
- `composeApp/androidApp/src/main/res/mipmap-anydpi-v26/ic_launcher_foreground.png`
- `composeApp/androidApp/src/main/res/mipmap-anydpi-v26/ic_launcher_monochrome.png`

Repeat for `_round` versions.

## iOS Icons

### Required Sizes
- iPhone App: 60x60 @2x, 60x60 @3x
- iPad App: 76x76 @2x
- iPad Pro: 83.5x83.5 @2x
- App Store: 1024x1024

### File Placement
Place generated PNGs in:
- `composeApp/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/`

## Online Tools

If you don't have ImageMagick installed, use online tools:
1. https://cloudconvert.com/svg-to-png
2. https://convertio.co/svg-png/
3. https://www.aconvert.com/image/svg-to-png/

### Steps
1. Upload `terrochroma_logo.svg`
2. Set desired dimensions
3. Download PNG
4. Repeat for all required sizes

## Feature Graphic

Create a 1024x500px feature graphic showing:
- App name "TerroChroma"
- Tagline "Rapid Optical Soil Carbon Sensing at the Edge"
- Illustration of app in field use (phone with calibration card, soil sample)
- SOC visualization (color gradient from red to blue)

Save as: `assets/marketing/feature_graphic.png`

## Design Specifications

### Logo Colors
- Background: Soil Brown (#5D4037) to Deep Soil (#3E2723) gradient
- Leaves: Leaf Green (#2E7D32) to Olive (#689F38) gradient
- Spectrum lines: Leaf Green, Mid Green, Leaf Green, Olive (opacity 0.8)
- Text: Cream (#F5F5F0)
- Soil curve: Warm Beige (#EFEBE9) with 30% opacity

### Design Elements
- Circular background with gradient
- Two stylized leaves (left and right)
- Four vertical spectrum lines (chromatic motif)
- Bottom soil curve
- "TC" monogram centered

### Style Guidelines
- Professional and scientific
- Organic but not cartoonish
- High contrast for visibility
- Scalable at all sizes
- Recognizable as a soil/agritech app

## Verification

After generating icons:
1. Verify icons display correctly on Android devices
2. Test adaptive icon on Android 13+ (monochrome version)
3. Verify iOS icons in simulator
4. Check feature graphic in Google Play Console preview
5. Ensure no artifacts or blurring at any size

## Notes

- The SVG source file is included for future modifications
- Always regenerate from SVG to maintain quality
- Keep a backup of generated icons
- Update icons when logo design changes
