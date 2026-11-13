import 'package:flutter/foundation.dart';

/// Bank4Z API configuration.
///
/// The right address depends on where the app is running, and this picks it
/// for you:
///  - Android emulator:  http://10.0.2.2:8080/api
///      (10.0.2.2 is the emulator's alias for your computer; 'localhost'
///       from inside the emulator points at the emulator itself)
///  - Windows desktop / iOS simulator:  http://localhost:8080/api
///      (they share your computer's network directly)
///  - Physical phone: pass your computer's LAN address when you run:
///      flutter run --dart-define=API_BASE_URL=http://192.168.1.50:8080/api
///      (phone and computer must be on the same Wi-Fi)
class ApiConfig {
  static const String _override = String.fromEnvironment('API_BASE_URL');

  static String get baseUrl {
    if (_override.isNotEmpty) return _override;
    if (!kIsWeb && defaultTargetPlatform == TargetPlatform.android) {
      return 'http://10.0.2.2:8080/api';
    }
    return 'http://localhost:8080/api';
  }
}