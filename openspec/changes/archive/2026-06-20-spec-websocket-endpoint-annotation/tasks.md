## 1. Verify specs against existing code

- [x] 1.1 Verify annotation definition matches `com.freenote.annotations.WebSocketEndpoint`
- [x] 1.2 Verify `WebServerProcessor` validation behavior matches spec
- [x] 1.3 Verify `BeanProviderProcessor` generation output matches spec
- [x] 1.4 Verify route resolution path in `DefaultLegacySessionBasedConnectionHandler` matches spec

## 2. Sync and finalize

- [x] 2.1 Sync delta spec to main specs
- [x] 2.2 Verify `./gradlew compileJava` still passes (no code changes expected)