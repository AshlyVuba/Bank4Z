/// Bank4Z API configuration.
///
/// IMPORTANT — the right base URL depends on where you're running the app:
///  - Android emulator:  http://10.0.2.2:8080     (10.0.2.2 is the emulator's
///                        alias for your machine's localhost — 'localhost'
///                        from inside the emulator points at itself, not you)
///  - iOS simulator:      http://localhost:8080    (simulator shares the
///                        host's network directly, no alias needed)
///  - Physical device:    http://<your-machine-LAN-IP>:8080
///                        (device and machine must be on the same Wi-Fi)
class ApiConfig {
  static const String baseUrl = 'http://10.0.2.2:8080/api';
}