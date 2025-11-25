#!/bin/bash
# Kaimera Icon Generator using ImageMagick
# Creates professional launcher icons for all Android densities

set -e

echo "🎨 Generating Kaimera launcher icons with ImageMagick..."
echo ""

# Check for ImageMagick
if ! command -v convert &> /dev/null; then
    echo "⚠️  ImageMagick not found. Installing..."
    brew install imagemagick
fi

# Function to create icon at specific size
create_icon() {
    local density=$1
    local size=$2
    
    local regular_path="app/src/main/res/mipmap-$density/ic_launcher.png"
    local round_path="app/src/main/res/mipmap-$density/ic_launcher_round.png"
    
    # Create icon with gradient background and camera design
    convert -size ${size}x${size} \
        gradient:'#6200EE-#3700B3' \
        \( -size ${size}x${size} xc:none \
           -fill white \
           -stroke white -strokewidth 2 \
           -draw "roundrectangle $((size/5)),$((size*2/5)) $((size*4/5)),$((size*3/5+size/10)) $((size/12)),$((size/12))" \
           -fill white \
           -draw "circle $((size/2)),$((size/2+size/20)) $((size/2)),$((size/2+size/5))" \
           -fill '#3700B3' \
           -draw "circle $((size/2)),$((size/2+size/20)) $((size/2)),$((size/2+size/8))" \
           -fill '#F0F0F0' \
           -draw "circle $((size/2)),$((size/2+size/20)) $((size/2)),$((size/2+size/14))" \
        \) \
        -composite \
        "$regular_path"
    
    # Copy to round icon
    cp "$regular_path" "$round_path"
    
    echo "✅ Created $density (${size}x${size}px)"
}

# Create icons for all densities
create_icon "mdpi" 48
create_icon "hdpi" 72
create_icon "xhdpi" 96
create_icon "xxhdpi" 144
create_icon "xxxhdpi" 192

echo ""
echo "🎉 All icons generated successfully!"
echo ""
echo "📱 Icons created for all densities"
