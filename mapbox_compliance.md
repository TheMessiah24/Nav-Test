# Mapbox Guidelines Compliance Checklist

## Attribution Requirements
- [ ] Mapbox wordmark is visible on all maps
- [ ] Attribution notice is visible and legible
- [ ] Attribution has not been altered beyond permitted customizations

## Visual Design Compliance
- [ ] Map style matches Mapbox's design guidelines
- [ ] Navigation UI elements follow Mapbox's recommended patterns
- [ ] 3D buildings and terrain rendering matches Mapbox's visual standards
- [ ] Route line styling follows Mapbox's recommendations
- [ ] Traffic visualization uses Mapbox's standard color coding

## API Usage Compliance
- [ ] Maps SDK is used according to documentation
- [ ] Navigation SDK is used according to documentation
- [ ] Search SDK is used according to documentation
- [ ] No deprecated API methods are used
- [ ] API calls follow recommended patterns and best practices

## Token Management
- [ ] Public token is used for Maps SDK
- [ ] Secret token is properly secured and only used for downloads
- [ ] No tokens are hardcoded in version-controlled files (except public token in strings.xml)

## Technical Requirements
- [ ] App targets Android SDK 21+
- [ ] App uses NDK 23 as required
- [ ] App supports devices with OpenGL ES 3
- [ ] Kotlin version 1.6.0+ is used
- [ ] Java 8+ compatibility is maintained

## Telemetry Compliance
- [ ] Mapbox Telemetry is enabled by default
- [ ] User opt-out mechanism is provided

## Performance Optimization
- [ ] Map resources are properly managed
- [ ] Navigation resources are properly released when not in use
- [ ] Memory usage is optimized for extended navigation sessions
- [ ] Battery usage is optimized

## User Experience Guidelines
- [ ] Search experience follows Mapbox recommendations
- [ ] Navigation UI is clear and uncluttered
- [ ] Voice instructions follow Mapbox's timing patterns
- [ ] Map camera behavior follows Mapbox's recommendations
- [ ] Error handling follows Mapbox's guidelines

## Reference Image Comparison
- [ ] Navigation view matches reference image 1
- [ ] Map overview matches reference image 2
- [ ] 3D navigation view matches reference image 3
- [ ] Color scheme matches reference images
- [ ] UI layout and controls match reference images
