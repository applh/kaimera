#!/usr/bin/env python3
"""
Kamerai App Icon Generator
Creates professional launcher icons for all Android densities
"""

try:
    from PIL import Image, ImageDraw
    import math
except ImportError:
    print("Error: Pillow library not found")
    print("Install with: pip3 install Pillow")
    exit(1)

# Icon sizes for different densities
SIZES = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
}

# Color scheme
GRADIENT_START = (98, 0, 238)    # #6200EE
GRADIENT_END = (55, 0, 179)      # #3700B3
WHITE = (255, 255, 255)
LIGHT_GRAY = (240, 240, 240)

def create_gradient_background(size):
    """Create a purple gradient background"""
    img = Image.new('RGB', (size, size))
    draw = ImageDraw.Draw(img)
    
    for y in range(size):
        # Calculate gradient color
        ratio = y / size
        r = int(GRADIENT_START[0] + (GRADIENT_END[0] - GRADIENT_START[0]) * ratio)
        g = int(GRADIENT_START[1] + (GRADIENT_END[1] - GRADIENT_START[1]) * ratio)
        b = int(GRADIENT_START[2] + (GRADIENT_END[2] - GRADIENT_START[2]) * ratio)
        
        draw.line([(0, y), (size, y)], fill=(r, g, b))
    
    return img

def draw_camera_icon(draw, size):
    """Draw a minimalist camera icon"""
    center = size // 2
    
    # Camera body (rounded rectangle)
    body_width = int(size * 0.6)
    body_height = int(size * 0.45)
    body_left = center - body_width // 2
    body_top = center - body_height // 2 + int(size * 0.05)
    body_right = center + body_width // 2
    body_bottom = center + body_height // 2 + int(size * 0.05)
    corner_radius = int(size * 0.08)
    
    # Draw rounded rectangle for camera body
    draw.rounded_rectangle(
        [body_left, body_top, body_right, body_bottom],
        radius=corner_radius,
        fill=WHITE,
        outline=WHITE,
        width=2
    )
    
    # Camera viewfinder (small rectangle on top)
    vf_width = int(size * 0.15)
    vf_height = int(size * 0.08)
    vf_left = center - vf_width // 2 + int(size * 0.1)
    vf_top = body_top - vf_height - 2
    vf_right = vf_left + vf_width
    vf_bottom = body_top - 2
    
    draw.rounded_rectangle(
        [vf_left, vf_top, vf_right, vf_bottom],
        radius=int(size * 0.02),
        fill=WHITE,
        outline=WHITE
    )
    
    # Lens (circle with aperture effect)
    lens_radius = int(size * 0.18)
    lens_center_y = center + int(size * 0.05)
    
    # Outer lens circle
    draw.ellipse(
        [center - lens_radius, lens_center_y - lens_radius,
         center + lens_radius, lens_center_y + lens_radius],
        fill=GRADIENT_END,
        outline=GRADIENT_END
    )
    
    # Inner lens circle (aperture)
    inner_radius = int(lens_radius * 0.6)
    draw.ellipse(
        [center - inner_radius, lens_center_y - inner_radius,
         center + inner_radius, lens_center_y + inner_radius],
        fill=GRADIENT_START,
        outline=GRADIENT_START
    )
    
    # Aperture blades (hexagon effect)
    blade_radius = int(inner_radius * 0.5)
    num_blades = 6
    blade_points = []
    for i in range(num_blades):
        angle = (i * 2 * math.pi / num_blades) - math.pi / 2
        x = center + int(blade_radius * math.cos(angle))
        y = lens_center_y + int(blade_radius * math.sin(angle))
        blade_points.append((x, y))
    
    draw.polygon(blade_points, fill=LIGHT_GRAY, outline=LIGHT_GRAY)
    
    # Flash indicator (small circle)
    flash_radius = int(size * 0.04)
    flash_x = body_right - flash_radius * 3
    flash_y = body_top + flash_radius * 2
    
    draw.ellipse(
        [flash_x - flash_radius, flash_y - flash_radius,
         flash_x + flash_radius, flash_y + flash_radius],
        fill=LIGHT_GRAY,
        outline=LIGHT_GRAY
    )

def create_icon(size):
    """Create a complete icon at the specified size"""
    # Create gradient background
    img = create_gradient_background(size)
    draw = ImageDraw.Draw(img)
    
    # Draw camera icon
    draw_camera_icon(draw, size)
    
    return img

def main():
    """Generate icons for all densities"""
    print("🎨 Generating Kamerai launcher icons...")
    print()
    
    for density, size in SIZES.items():
        # Create icon
        icon = create_icon(size)
        
        # Save regular icon
        regular_path = f'app/src/main/res/mipmap-{density}/ic_launcher.png'
        icon.save(regular_path, 'PNG')
        print(f"✅ Created {density:8s} ({size:3d}x{size:3d}px): {regular_path}")
        
        # Save round icon (same design)
        round_path = f'app/src/main/res/mipmap-{density}/ic_launcher_round.png'
        icon.save(round_path, 'PNG')
        print(f"✅ Created {density:8s} ({size:3d}x{size:3d}px): {round_path}")
    
    print()
    print("🎉 All icons generated successfully!")
    print()
    print("📱 Icons are ready for:")
    print("   - Android launcher (all densities)")
    print("   - Google Play Store listing")
    print("   - App distribution")

if __name__ == '__main__':
    main()
