# SysMetrics Android TV UI/UX - Implementation Guide

## 📱 Overview

This guide documents the professional Android TV UI/UX redesign for SysMetrics, optimized for D-pad navigation and 2-meter viewing distance.

## ✅ Implemented Features

### 1. Design System
- **Colors**: Professional dark theme with dynamic metric colors (green/yellow/red)
- **Typography**: Large readable fonts (48sp for values, 16sp for labels)
- **Spacing**: TV-optimized dimensions for comfortable D-pad navigation
- **Focus States**: Visual feedback with borders, shadows, and subtle animations

### 2. Custom Views

#### MetricCardView
Location: `app/src/main/java/com/sysmetrics/app/ui/components/MetricCardView.kt`

Professional card component for displaying metrics with:
- Icon + metric name + large value + progress bar
- Dynamic color based on value (< 50% green, 50-80% yellow, > 80% red)
- Smooth animations for value changes
- Focus state with scale animation and elevation change

#### ProgressBarMetric
Location: `app/src/main/java/com/sysmetrics/app/ui/components/ProgressBarMetric.kt`

Custom progress bar with:
- Rounded corners for modern look
- Dynamic color based on progress value
- Smooth animation between values
- Optimized for TV viewing

### 3. D-pad Navigation

#### DpadNavigationHandler
Location: `app/src/main/java/com/sysmetrics/app/ui/navigation/DpadNavigationHandler.kt`

Handles all remote control inputs:
- **UP/DOWN**: Navigate between metric cards
- **CENTER/OK**: Open metric details
- **BACK**: Exit application
- **MENU**: Open settings (future)

### 4. Home Screen (TV Interface)

#### HomeTvFragment
Location: `app/src/main/java/com/sysmetrics/app/ui/home/HomeTvFragment.kt`

Features:
- 3 metric cards (CPU, RAM, Temperature)
- Circular navigation (UP from first card goes to last card)
- Real-time updates every 500ms
- System info footer (cores, uptime)
- Navigation hints at bottom

#### HomeTvViewModel
Location: `app/src/main/java/com/sysmetrics/app/ui/home/HomeTvViewModel.kt`

Manages:
- Real-time metrics collection via coroutines
- CPU usage calculation
- Memory monitoring
- Temperature reading
- System uptime tracking

### 5. Layouts

#### fragment_home_tv.xml
Main home screen layout with:
- ScrollView for potential future content
- 3 MetricCardView components
- Footer with cores and uptime info
- Navigation hint text

#### view_metric_card.xml
Reusable metric card component with:
- Horizontal layout: Icon + Name
- Large value text (48sp)
- Progress bar
- Percentage text below progress

## 🎨 Design Specifications

### Color Palette
```xml
Background Primary: #121212
Surface: #1E1E1E
Text Primary: #E0E0E0
Text Secondary: #AAAAAA
Focus Border: #2196F3
Success: #4CAF50 (< 50%)
Warning: #FFC107 (50-80%)
Error: #F44336 (> 80%)
```

### Typography Sizes
```xml
Footer: 12sp
Metric Name: 16sp
Header: 20sp
Metric Value: 48sp (Main values - optimized for 2m viewing)
```

### Spacing & Dimensions
```xml
Screen Padding: 24dp horizontal, 32dp top
Card Height: 120dp
Card Margin: 16dp between cards
Focus Border: 2dp
Progress Bar: 8dp height
```

## 🎮 Navigation Flow

```
┌─────────────────────────────────────┐
│  SysMetrics                         │
├─────────────────────────────────────┤
│  [CPU Usage] ← START HERE           │
│   48.5%                             │
│  ████████████░░░░░░░░░░ 48%         │
├─────────────────────────────────────┤
│  [RAM Usage]                        │
│   1250 MB / 1699 MB                 │
│  ██████████████░░░░░░░░ 73%         │
├─────────────────────────────────────┤
│  [Temperature]                      │
│   N/A                               │
│  ░░░░░░░░░░░░░░░░░░░░░░░ —          │
├─────────────────────────────────────┤
│  Cores: 4  |  Uptime: 02:35:18     │
└─────────────────────────────────────┘

⬆ ⬇ Navigate  |  OK: Details  |  Back: Exit
```

**D-pad Controls:**
- **⬆ UP**: Move to previous metric card
- **⬇ DOWN**: Move to next metric card
- **OK/CENTER**: Open metric details (future feature)
- **BACK**: Exit application

## 📂 Project Structure

```
app/src/main/
├── java/com/sysmetrics/app/
│   ├── ui/
│   │   ├── MainActivity.kt (Updated with TV interface support)
│   │   ├── MainActivityTv.kt (Dedicated TV activity)
│   │   ├── components/
│   │   │   ├── MetricCardView.kt
│   │   │   └── ProgressBarMetric.kt
│   │   ├── home/
│   │   │   ├── HomeTvFragment.kt
│   │   │   └── HomeTvViewModel.kt
│   │   └── navigation/
│   │       └── DpadNavigationHandler.kt
│   └── ...
└── res/
    ├── layout/
    │   ├── activity_main_tv.xml
    │   ├── fragment_home_tv.xml
    │   └── view_metric_card.xml
    ├── drawable/
    │   ├── bg_metric_card.xml (Focus state selector)
    │   ├── bg_progress_*.xml (Progress bar backgrounds)
    │   ├── ic_cpu.xml
    │   ├── ic_memory.xml
    │   └── ic_temperature.xml
    ├── anim/
    │   ├── focus_scale_in.xml
    │   └── focus_scale_out.xml
    └── values/
        ├── colors.xml (Updated with TV design system)
        ├── dimens.xml (Updated with TV dimensions)
        ├── themes.xml (Updated with TV styles)
        └── strings.xml (Added TV UI strings)
```

## 🚀 Usage

### Enable TV Interface

In `MainActivity.kt`, set:
```kotlin
private val useTvInterface = true
```

### Navigate with D-pad

1. Launch app on Android TV
2. Use **⬆/⬇** on remote to navigate between cards
3. Focused card will have **blue border** and **subtle scale effect**
4. Press **OK** to view details (coming soon)
5. Press **BACK** to exit

### Customize Update Interval

In `HomeTvViewModel.kt`:
```kotlin
private val updateIntervalMs = 500L // Change to desired interval (ms)
```

## 🎯 Performance Optimizations

1. **Efficient Updates**: Metrics update every 500ms (balanced accuracy/performance)
2. **View Recycling**: Reuses MetricCardView components
3. **Smooth Animations**: Hardware-accelerated animations (200-300ms)
4. **Memory Efficient**: Coroutines with proper lifecycle management
5. **No Memory Leaks**: ViewBinding with proper cleanup

## 📊 Metrics Displayed

### CPU Usage
- **Value**: Percentage (0-100%)
- **Color**: Green < 50%, Yellow 50-80%, Red > 80%
- **Update**: Every 500ms
- **Calculation**: Delta between /proc/stat readings

### RAM Usage
- **Value**: Used MB / Total MB
- **Progress**: Percentage of total memory
- **Color**: Same as CPU
- **Update**: Every 500ms

### Temperature
- **Value**: Celsius (°C) or N/A if unavailable
- **Color**: Based on temperature threshold
- **Update**: Every 500ms
- **Source**: /sys/class/thermal/thermal_zone*

### System Info
- **Cores**: Number of CPU cores
- **Uptime**: HH:MM:SS format
- **Update**: Uptime updates every second

## 🔧 Customization

### Change Metric Colors

Edit `res/values/colors.xml`:
```xml
<color name="metric_success">#4CAF50</color>  <!-- < 50% -->
<color name="metric_warning">#FFC107</color>  <!-- 50-80% -->
<color name="metric_error">#F44336</color>    <!-- > 80% -->
```

### Adjust Font Sizes

Edit `res/values/dimens.xml`:
```xml
<dimen name="text_size_value">48sp</dimen>      <!-- Main values -->
<dimen name="text_size_secondary">16sp</dimen>  <!-- Labels -->
```

### Modify Card Height

```xml
<dimen name="card_height">120dp</dimen>
```

### Change Focus Border

```xml
<dimen name="focus_border_width">2dp</dimen>
<color name="focus_border">#2196F3</color>
```

## 🧪 Testing

### Test on Emulator
1. Create Android TV emulator (API 34+)
2. Run app: `./gradlew assembleDebug`
3. Install: `adb install app/build/outputs/apk/debug/app-debug.apk`
4. Use emulator D-pad controls

### Test on Real Device
1. Enable ADB over network on TV
2. Connect: `adb connect <TV_IP>:5555`
3. Install APK
4. Test with physical remote

### Verify Features
- [ ] D-pad navigation works (UP/DOWN/OK/BACK)
- [ ] Focus state shows blue border
- [ ] Metrics update in real-time
- [ ] Colors change based on values
- [ ] Animations are smooth (60 FPS)
- [ ] Text is readable from 2m distance
- [ ] No lag or stuttering

## 📈 Performance Benchmarks

Target metrics:
- **Frame Rate**: 60 FPS constant
- **CPU Usage**: < 20% when monitoring
- **RAM Usage**: < 50MB
- **Response Time**: < 100ms for D-pad actions
- **Battery Impact**: Minimal (uses efficient coroutines)

## 🎨 Best Practices Applied

1. **Material Design 3**: Modern, consistent UI
2. **Android TV Guidelines**: Optimized for leanback experience
3. **MVVM Architecture**: Clean separation of concerns
4. **Kotlin Coroutines**: Efficient async operations
5. **Dependency Injection**: Hilt for testability
6. **ViewBinding**: Type-safe view access
7. **Reactive Programming**: StateFlow for UI updates
8. **Hardware Acceleration**: Smooth 60 FPS animations

## 🚧 Future Enhancements

### Phase 2 (Planned)
- [ ] Detail screens for each metric (graphs, statistics)
- [ ] Settings screen with TV navigation
- [ ] History tracking (last 5/10/30 minutes)
- [ ] Export logs functionality
- [ ] Sound notifications for critical values

### Phase 3 (Advanced)
- [ ] Multiple view modes (compact/detailed/graph)
- [ ] Customizable thresholds
- [ ] Widget for Android TV home screen
- [ ] Multi-language support
- [ ] Cloud sync (optional)

## 📝 Notes

- **Minimum SDK**: API 21 (Lollipop)
- **Target SDK**: API 34
- **Tested on**: Android TV 11+, Fire TV
- **Screen Support**: 720p, 1080p, 4K
- **Input Methods**: D-pad remote, game controller

## 🙏 Credits

Design inspired by:
- Material Design 3 Guidelines
- Android TV Design Guidelines
- System monitoring overlay best practices

---

**Last Updated**: 2025-12-10
**Version**: 1.0.0
**Author**: SysMetrics Development Team
