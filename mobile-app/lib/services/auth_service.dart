import 'dart:convert';
import 'dart:io' show Platform;
import 'dart:math';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../config/api_config.dart';

/// Thrown when the API returns a non-2xx response, carrying the
/// human-readable message so screens can show it directly (this is where
/// the Gen Z copy from the backend flows straight through to the UI
/// without the app needing its own copy).
class ApiException implements Exception {
  final String message;
  ApiException(this.message);
}

class AuthService {
  final _storage = const FlutterSecureStorage();

  static const _accessTokenKey = 'bank4z_access_token';
  static const _refreshTokenKey = 'bank4z_refresh_token';
  static const _deviceIdKey = 'bank4z_device_id';

  Future<void> register({
    required String fullName,
    required String email,
    required String idNumber,
    required String phoneNumber,
    required String password,
  }) async {
    final response = await http.post(
      Uri.parse('${ApiConfig.baseUrl}/auth/register'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'fullName': fullName,
        'email': email,
        'idNumber': idNumber,
        'phoneNumber': phoneNumber,
        'password': password,
      }),
    );

    if (response.statusCode != 201) {
      throw ApiException(_extractMessage(response.body));
    }
  }

  Future<void> login({
    required String email,
    required String password,
  }) async {
    final response = await http.post(
      Uri.parse('${ApiConfig.baseUrl}/auth/login'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'email': email,
        'password': password,
        // Lets the backend show this login in "Devices" and spot new phones.
        'deviceId': await _getOrCreateDeviceId(),
        'deviceName': _deviceName(),
      }),
    );

    if (response.statusCode != 200) {
      throw ApiException(_extractMessage(response.body));
    }

    final data = jsonDecode(response.body);
    await _storage.write(key: _accessTokenKey, value: data['accessToken']);
    await _storage.write(key: _refreshTokenKey, value: data['refreshToken']);
  }

  Future<String?> getAccessToken() => _storage.read(key: _accessTokenKey);

  Future<bool> isLoggedIn() async {
    final token = await getAccessToken();
    return token != null;
  }

  /// Ends the session on the server first (so the tokens stop working
  /// everywhere, not just here), then clears them from this phone.
  /// If the phone is offline we still log out locally.
  Future<void> logout() async {
    final token = await getAccessToken();
    if (token != null) {
      try {
        await http.delete(
          Uri.parse('${ApiConfig.baseUrl}/sessions/current'),
          headers: {'Authorization': 'Bearer $token'},
        );
      } catch (_) {
        // offline or server down: fall through and clear local tokens anyway
      }
    }

    await _storage.delete(key: _accessTokenKey);
    await _storage.delete(key: _refreshTokenKey);
    // The device id is deliberately kept, so this phone stays a "known device".
  }

  /// A random id created once per app install and kept in secure storage.
  Future<String> _getOrCreateDeviceId() async {
    var id = await _storage.read(key: _deviceIdKey);
    if (id == null) {
      final random = Random.secure();
      id = List.generate(16, (_) => random.nextInt(256).toRadixString(16).padLeft(2, '0')).join();
      await _storage.write(key: _deviceIdKey, value: id);
    }
    return id;
  }

  String _deviceName() {
    final os = Platform.operatingSystem; // android, ios, windows, ...
    return 'Bank4Z on ${os[0].toUpperCase()}${os.substring(1)}';
  }

  String _extractMessage(String responseBody) {
    try {
      final decoded = jsonDecode(responseBody);
      if (decoded['details'] != null) {
        final details = decoded['details'] as Map<String, dynamic>;
        return details.values.first.toString();
      }
      return decoded['message'] ?? 'Something went wrong. Try again.';
    } catch (_) {
      return 'Something went wrong. Try again.';
    }
  }
}