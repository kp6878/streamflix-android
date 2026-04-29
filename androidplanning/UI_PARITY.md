# UI Parity — Design Tokens & Visual Specifications

Every value below is extracted directly from the iOS/macOS source code. The Android implementation
must reproduce these values exactly using Jetpack Compose equivalents.

---

## Color Palette

### Core Colors

| Token | iOS Value | Compose Equivalent |
|-------|-----------|-------------------|
| Background | `Color.black` | `Color(0xFF000000)` |
| Primary Accent | `Color.red` / `#E50914` | `Color(0xFFE50914)` |
| Sidebar Active | `Color(red: 0.9, green: 0.25, blue: 0.2)` | `Color(0xFFE64033)` — warm coral |
| Sidebar Text Active | `Color.white` | `Color.White` |
| Sidebar Text Inactive | `Color.white.opacity(0.7)` | `Color.White.copy(alpha = 0.7f)` |
| Primary Text | `Color.white` | `Color.White` |
| Secondary Text | `Color.gray` / `white.opacity(0.7)` | `Color.Gray` / `Color.White.copy(alpha = 0.7f)` |
| Progress Fill | `Color.red` | `Color(0xFFE50914)` |
| Progress Track | `gray.opacity(0.3)` | `Color.Gray.copy(alpha = 0.3f)` |
| Star Rating | `Color.yellow` | `Color(0xFFFFD700)` |
| Metadata Text | `white.opacity(0.8)` | `Color.White.copy(alpha = 0.8f)` |
| Description Text | `white.opacity(0.85)` | `Color.White.copy(alpha = 0.85f)` |
| Hover Overlay | `black.opacity(0.3)` | `Color.Black.copy(alpha = 0.3f)` |
| Subtle Background | `gray.opacity(0.1)` to `gray.opacity(0.2)` | `Color.Gray.copy(alpha = 0.1f..0.2f)` |

### Quality Badge Colors

| Quality | iOS Color | Compose |
|---------|-----------|---------|
| 4K / UHD / 2160p | `Color.purple` | `Color(0xFF9C27B0)` |
| FHD / 1080p | `Color.blue` | `Color(0xFF2196F3)` |
| HD / 720p | `Color.green` | `Color(0xFF4CAF50)` |
| SD / 480p | `Color.orange` | `Color(0xFFFF9800)` |
| CAM / TS / SCR | `Color.red` | `Color(0xFFF44336)` |

Badge styling: text color = `Color.White`, `4.dp` corner radius, `8.dp` horizontal / `4.dp` vertical padding, font = `caption.bold()`.

### Audio Codec Badge Colors

| Codec | Display Text | Color |
|-------|-------------|-------|
| TrueHD + Atmos | "TrueHD Atmos" | Purple accent |
| TrueHD | "TrueHD" | Purple |
| DTS-HD MA | "DTS-HD MA" | Blue accent |
| Atmos | "Atmos" | Purple accent |
| DTS | "DTS" | Blue |
| DD+ / DDP / E-AC3 | "DD+" | Blue |
| AC3 / DD5.1 | "DD 5.1" | Green |
| AAC | "AAC" | Gray |

### Cached Stream Indicator

- **Icon:** Lightning bolt (Material Icons `bolt`)
- **Color:** `Color.Green`
- **Label:** "Cached" / "Instant"

### Status Colors

| State | Color |
|-------|-------|
| Success / Configured | `Color.Green` |
| Warning / Download paused | `Color(0xFFFF9800)` (orange) |
| Error / Danger | `Color.Red` |
| Info / Downloading | `Color.Blue` |
| Primary Action | `Color.Blue` |

---

## Typography

| Token | iOS Font | Compose Equivalent |
|-------|----------|-------------------|
| App title | `.title2.bold()` | `MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)` |
| Screen title | `.largeTitle.bold()` | `headlineLarge` + `FontWeight.Bold` |
| Section title | `.title2.bold()` | `titleMedium` + `FontWeight.Bold` |
| Hero title fallback | `.system(size: 42, weight: .bold)` | `fontSize = 42.sp, fontWeight = FontWeight.Bold` |
| Body text | `.body` | `bodyLarge` |
| Hero metadata | `.subheadline` | `bodyMedium` |
| Hero description | `.callout` | `bodyMedium` (14sp) |
| Hero buttons | `.callout.weight(.semibold/.medium)` | `bodyMedium` + `FontWeight.SemiBold` / `.Medium` |
| Card title | `.subheadline.bold()` | `bodyMedium` + `FontWeight.Bold` |
| Card subtitle | `.caption` | `labelSmall` |
| Badge text | `.caption2.bold()` | `labelSmall` + `FontWeight.Bold` (10sp) |
| Player time | `.title2.bold().monospacedDigit()` | `titleMedium` + `Bold` + `FontFamily.Monospace` |
| Player title | `.title.bold()` | `titleLarge` + `FontWeight.Bold` |
| Player metadata | `.body.weight(.medium)` | `bodyLarge` + `FontWeight.Medium` |
| Profile title | `.system(size: 36, weight: .bold)` | `fontSize = 36.sp, fontWeight = FontWeight.Bold` |

### Font Family

Use **Google Fonts Inter** (or system default `sans-serif`) for all text. Do NOT use serif fonts.

---

## Spacing System

| Token | Value | Usage |
|-------|-------|-------|
| Section gap | `24.dp` | Between major UI sections |
| Home row horizontal | `48.dp` | Row title padding on Home |
| Content horizontal padding | `24.dp` | Content detail screen |
| Player horizontal padding | `32.dp` | Player overlay |
| Standard element gap | `12–16.dp` | Between related elements |
| Pill button padding | `18.dp` h / `12.dp` v | Player bottom toolbar |
| Filter pill padding | `16.dp` h / `8–10.dp` v | Browse/Library filters |
| Card gap | `16.dp` | Between content cards in rows |
| Settings section internal | `20.dp` | Inside settings section cards |
| Settings section external | `24.dp` | Between settings cards |

---

## Corner Radii

| Token | Value | Usage |
|-------|-------|-------|
| Cards | `8.dp` | Content posters, thumbnails |
| Badges | `4.dp` | Quality/codec badges |
| Hero buttons | `6.dp` | Play, My List buttons on hero |
| Bottom nav categories | `10.dp` | Category picker pills |
| Filter pills | `16.dp` | Status filter pills |
| Genre pills | `20.dp` | Browse genre chips |
| Player pills | `22.dp` | All player toolbar buttons |
| Sections | `12.dp` | Settings cards, info panels |
| Input fields | `8.dp` | Text fields, search bars |
| Avatar circles | `50.dp` (full circle) | Profile avatars |
| Info panel | `14.dp` | Paused info overlay |
| Track picker sheet | `16.dp` | Audio/subtitle picker |

---

## Animation Specifications

| Token | Value | Compose Equivalent |
|-------|-------|--------------------|
| Hover transition | `0.15–0.2s easeInOut` | `animateFloatAsState(animationSpec = tween(150–200))` |
| Hero crossfade | `0.6s easeInOut` | `Crossfade(animationSpec = tween(600))` |
| Player controls auto-hide | `4 seconds` | `LaunchedEffect` with `delay(4000)` |
| Hero auto-rotate | `8 seconds` | `LaunchedEffect` with `delay(8000)` |
| Controls fade | `.opacity` transition | `AnimatedVisibility(enter = fadeIn(), exit = fadeOut())` |
| Card hover scale | `1.05x` | `Modifier.scale(1.05f)` on press |
| Watch card hover scale | `1.02x` | `Modifier.scale(1.02f)` on press |
| Profile hover scale | `1.05x` | `Modifier.scale(1.05f)` on press |
| Auto-play countdown | `10 seconds` | 10s countdown timer |
| Scrub settle delay | `500ms` | After scrub ends, wait 500ms before releasing thumb |

---

## Hero Banner Specifications

| Property | Phone | Tablet | TV |
|----------|-------|--------|-----|
| Height | `500.dp` | `620.dp` | `620.dp` |
| Max featured items | 6 | 6 | 6 |
| Auto-rotate interval | `8s` | `8s` | `8s` |
| Transition | Crossfade `0.6s` | Crossfade `0.6s` | Crossfade `0.6s` |
| Bottom gradient | 55% of height | 55% of height | 55% of height |
| Top gradient height | `100.dp` | `100.dp` | `100.dp` |
| Logo max size | `280×80.dp` | `350×100.dp` | `350×100.dp` |
| Content max width | `450.dp` | `550.dp` | `550.dp` |
| Pagination dots | `8×8.dp`, `8.dp` spacing | Same | Same |
| Dot active color | `Color.White` | Same | Same |
| Dot inactive color | `White.copy(0.4f)` | Same | Same |

### Hero Action Buttons

| Button | Text Color | Background | Padding | Radius |
|--------|-----------|------------|---------|--------|
| Play | Black | White | `24.dp` h, `10.dp` v | `6.dp` |
| My List | `White.copy(0.9f)` | `White.copy(0.15f)` | `18.dp` h, `10.dp` v | `6.dp` |
| More Info | `White.copy(0.9f)` | `White.copy(0.15f)` | `18.dp` h, `10.dp` v | `6.dp` |

---

## Content Card Specifications

### Poster Card (ContentCard)

| Property | Value |
|----------|-------|
| Width | `150.dp` |
| Height | `225.dp` (2:3 ratio) |
| Corner radius | `8.dp` |
| Press scale | `1.05x` |
| Press brightness | `+0.1` |
| Shadow (normal) | `8.dp` elevation |
| Shadow (pressed) | `12.dp` elevation |
| Rating badge | Bottom-left corner, star icon + rating text |
| Rating badge bg | `Black.copy(0.7f)`, `4.dp` radius |
| Title area height | `50.dp` |
| Title font | `.caption.weight(.medium)`, 2-line limit |
| Play overlay | `44×44.dp` white circle, black play icon |

### Watch History Card

| Property | Value |
|----------|-------|
| Width | `280.dp` |
| Thumbnail height | `160.dp` |
| Corner radius | `8.dp` |
| Press scale | `1.02x` |
| Shadow | `Black.copy(0.3f)`, `5.dp` radius |
| Episode badge | Top-left, `caption2.bold`, white on `Black.copy(0.7f)`, `4.dp` radius |
| Progress bar height | `4.dp` |
| Progress track | `Gray.copy(0.3f)` |
| Progress fill | `Color.Red` |
| Title font | `bodyMedium.bold`, white, 1-line limit |
| Time remaining | `labelSmall`, gray |
| Play button | `30.dp` circle, `White.copy(0.2f)` bg |
| Info bar bg | `Gray.copy(0.1f)`, `12.dp` padding |

---

## Profile Picker Specifications

| Property | Value |
|----------|-------|
| Background | `Color.Black`, full screen |
| Title | `36.sp`, `FontWeight.Bold`, white |
| Grid | Adaptive `120–160.dp` columns, `28.dp` spacing |
| Max width | `700.dp` centered, `60.dp` horizontal padding |
| Avatar circle | `100×100.dp` |
| Avatar icon size | `44.sp`, `FontWeight.Medium` |
| Name font | `bodyMedium`, gray → white on selection |
| Kids badge | `labelSmall.bold`, blue bg, `8.dp` padding |
| Add button | Dashed circle outline, `2.dp` stroke, plus icon `36.sp` |
| Manage link | `bodyMedium`, gray, bottom-centered |

### Avatar Gradients (Map SF Symbols → Material Icons)

| iOS Symbol | Android Equivalent | Gradient |
|------------|-------------------|----------|
| `person.circle.fill` | `Icons.Default.Person` | Blue → Cyan |
| `star.circle.fill` | `Icons.Default.Star` | Yellow → Orange |
| `heart.circle.fill` | `Icons.Default.Favorite` | Red → Pink |
| `film.circle.fill` | `Icons.Default.Movie` | Purple → Blue |
| `sparkles` | `Icons.Default.AutoAwesome` | Yellow → Pink |
| `gamecontroller.fill` | `Icons.Default.SportsEsports` | Green → Mint |
| `music.note` | `Icons.Default.MusicNote` | Pink → Purple |
| `book.circle.fill` | `Icons.Default.MenuBook` | Orange → Yellow |
| `globe` | `Icons.Default.Language` | Cyan → Blue |
| `person.crop.circle.fill` | `Icons.Default.AccountCircle` | Indigo → Purple |

---

## Profile Editor Specifications

| Property | Value |
|----------|-------|
| Max width | `400.dp` centered |
| Selected avatar | `120×120.dp` circle, gradient fill, `20.dp` shadow |
| Avatar icon | `50.sp`, `FontWeight.SemiBold` |
| Avatar grid | 5 columns, `60.dp` fixed, `16.dp` spacing |
| Selected border | `3.dp` white stroke, `1.1x` scale |
| Grid background | `Gray.copy(0.15f)`, `16.dp` radius |
| Name field | Plain, `Gray.copy(0.2f)` bg, `8.dp` radius, `12.dp` padding |
| Kids toggle | Switch style, blue tint, `Gray.copy(0.15f)` bg, `12.dp` radius |
| Save button | `FontWeight.Bold`, white text, full width, `14.dp` padding, blue bg |
| Delete button | `FontWeight.Bold`, red text, `Red.copy(0.15f)` bg, `12.dp` radius |

---

## Bottom Navigation Bar

| Property | Value |
|----------|-------|
| Tabs | Home, Browse, Search, Library, Downloads, Settings |
| Icons | `home`, `grid_view`, `search`, `favorite`, `download`, `settings` |
| Selected tint | `Color(0xFFE50914)` (Netflix red) |
| Unselected tint | `Color.Gray` |
| Background | `Color.Black` or `Surface` dark |
| Label visibility | Always show labels |

---

## Stream Selection Sheet Specifications

### Download Status Banners

| State | Border Color | Background | Icon Color | Button |
|-------|-------------|------------|------------|--------|
| Already Downloaded | Orange top `3.dp` | `Orange.copy(0.15f)` | Orange | "Play Offline" (orange bg, black text) |
| Currently Downloading | Blue top `3.dp` | `Blue.copy(0.15f)` | Blue | ProgressView with % |
| Download Paused | Yellow top `3.dp` | `Yellow.copy(0.15f)` | Yellow | "Resume" button |

### Addon Picker

| Property | Value |
|----------|-------|
| Layout | Horizontal scroll, `8.dp` spacing |
| Pill padding | `12.dp` h / `8.dp` v |
| Pill radius | `8.dp` |
| Selected | Blue bg, white text |
| Unselected | `Gray.copy(0.2f)`, gray text |

### Stream Row

Each stream shows:
- Quality badge (color-coded as above)
- Audio codec badge
- File size badge
- Cached indicator (green bolt icon)
- Language flags (emoji)
- Download button (hidden if already downloaded)
- Play button

---

## Downloads Screen Specifications

### Category Picker

| Property | Value |
|----------|-------|
| Layout | `HStack`, `12.dp` spacing |
| Padding | `16.dp` h / `10.dp` v |
| Radius | `10.dp` |
| Selected | Blue bg, white text |
| Unselected | `Gray.copy(0.2f)`, gray text |
| Count badge | `labelSmall.bold` |

### Status Filter Pills

| Property | Value |
|----------|-------|
| Font | `labelSmall.medium` |
| Radius | `16.dp` |
| Selected | `Gray.copy(0.4f)` bg, white text |
| Unselected | Clear bg, `1.dp` `Gray.copy(0.3f)` border |

---

## Settings Screen Specifications

### Section Card

| Property | Value |
|----------|-------|
| Background | `Gray.copy(0.1f)` |
| Corner radius | `12.dp` |
| Internal padding | `20.dp` |
| Section spacing | `24.dp` |

### Input Field

| Property | Value |
|----------|-------|
| Style | No outline, filled |
| Background | `Gray.copy(0.2f)` |
| Padding | `12.dp` |
| Corner radius | `8.dp` |
| Password toggle | Show/hide icon button |

### Service Icon Colors

| Service | Icon | Color |
|---------|------|-------|
| StreamFlix Account | `person.crop.circle.fill` | Red |
| Real-Debrid | `cloud.fill` | Green |
| OpenSubtitles | `captions.bubble.fill` | Yellow |

---

## Shared Components

### Loading Overlay

| Property | Value |
|----------|-------|
| Background | `Black.copy(0.8f)`, full screen |
| Spinner | `CircularProgressIndicator`, `1.5x` scale, red tint |
| Text | "Loading content...", white |

### Error Banner

| Property | Value |
|----------|-------|
| Background | `Red.copy(0.9f)`, `8.dp` radius |
| Icon | Warning triangle, yellow |
| Text | White |
| Animation | Slide from top + fade |
| Auto-dismiss | 5 seconds |

### Empty States

All empty states follow: centered icon (`60.dp`) + headline text + optional CTA button.

---

## Platform Adaptations (Android-Specific)

| Feature | iOS | Android |
|---------|-----|---------|
| Navigation | Sidebar (macOS) / TabView (iOS) | BottomNavigation |
| Hover effects | Scale, brightness, borders | Ripple + pressed state scale |
| Player overlay | ZStack over entire window | Full-screen Activity or Dialog |
| Detail view | Sheet/overlay with close button | Full-screen with back nav |
| Profile name in nav | Shown with chevron | Avatar icon only |
| Content cards | Title on hover (macOS) | Title always visible |
| Swipe gestures | N/A | Brightness (left) / Volume (right) in player |
| Back navigation | Close button | System back gesture + close button |
