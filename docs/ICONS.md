# Icon System

Kaimera uses a consistent icon system based on **Material Design Icons** (Outlined style).

## Design Guidelines

- **Style:** Outlined (not filled)
- **Stroke Weight:** 1px (Thin/Delicate)
- **Size:** 24dp viewport
- **Color:** `?attr/colorOnPrimary` (White on dark backgrounds)
- **Background:** Round shape with `?attr/colorPrimary`

## Icon Source

All icons are sourced from Google Fonts Icons:
https://fonts.google.com/icons?icon.style=Outlined

## Current Icons

| App | Icon Name | Description | File |
|---|---|---|---|
| **Camera** | `ic_camera` | Outlined camera with lens | `app/src/main/res/drawable/ic_camera.xml` |
| **Browser** | `ic_search` | Globe/Network icon | `app/src/main/res/drawable/ic_search.xml` |
| **Files** | `ic_folder` | Outlined folder | `app/src/main/res/drawable/ic_folder.xml` |
| **Settings** | `ic_settings` | Outlined gear | `app/src/main/res/drawable/ic_settings.xml` |

## Adding New Icons

1. Go to [Google Fonts Icons](https://fonts.google.com/icons?icon.style=Outlined)
2. Select an icon
3. Download the **Android XML** version
4. Edit the XML to use `strokeColor` instead of `fillColor`
5. Set `strokeWidth="1"`
6. Save to `app/src/main/res/drawable/`
