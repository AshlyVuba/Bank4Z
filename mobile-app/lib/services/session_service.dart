import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';
import '../models/device_session.dart';
import 'auth_service.dart'; // ApiException + token access

class SessionService {
  final _authService = AuthService();

  Future<Map<String, String>> _authHeaders() async {
    final token = await _authService.getAccessToken();
    return {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }

  /// The backend returns a plain JSON array here (not a Spring Page).
  Future<List<DeviceSession>> getSessions() async {
    final response = await http.get(
      Uri.parse('${ApiConfig.baseUrl}/sessions'),
      headers: await _authHeaders(),
    );
    _ensureOk(response, const [200]);

    final list = jsonDecode(response.body) as List;
    return list.map((json) => DeviceSession.fromJson(json)).toList();
  }

  Future<void> revoke(String sessionId) async {
    final response = await http.delete(
      Uri.parse('${ApiConfig.baseUrl}/sessions/$sessionId'),
      headers: await _authHeaders(),
    );
    _ensureOk(response, const [200, 204]);
  }

  /// Returns how many other devices were logged out.
  Future<int> revokeOthers() async {
    final response = await http.post(
      Uri.parse('${ApiConfig.baseUrl}/sessions/revoke-others'),
      headers: await _authHeaders(),
    );
    _ensureOk(response, const [200]);

    return (jsonDecode(response.body)['count'] as num).toInt();
  }

  void _ensureOk(http.Response response, List<int> okCodes) {
    if (okCodes.contains(response.statusCode)) return;

    String message = 'Something went wrong. Try again.';
    try {
      message = jsonDecode(response.body)['message'] ?? message;
    } catch (_) {
      // body wasn't JSON, keep the generic message
    }
    throw ApiException(message);
  }
}