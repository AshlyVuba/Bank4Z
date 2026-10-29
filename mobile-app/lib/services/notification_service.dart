import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';
import '../models/bank_notification.dart';
import 'auth_service.dart'; // ApiException + token access

class NotificationService {
  final _authService = AuthService();

  Future<Map<String, String>> _authHeaders() async {
    final token = await _authService.getAccessToken();
    return {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }

  /// Backend returns a Spring Page; the list lives under "content".
  Future<List<BankNotification>> getNotifications({int page = 0, int size = 20}) async {
    final response = await http.get(
      Uri.parse('${ApiConfig.baseUrl}/notifications?page=$page&size=$size'),
      headers: await _authHeaders(),
    );
    _ensureOk(response, const [200]);

    final content = jsonDecode(response.body)['content'] as List;
    return content.map((json) => BankNotification.fromJson(json)).toList();
  }

  Future<int> getUnreadCount() async {
    final response = await http.get(
      Uri.parse('${ApiConfig.baseUrl}/notifications/unread-count'),
      headers: await _authHeaders(),
    );
    _ensureOk(response, const [200]);

    return (jsonDecode(response.body)['count'] as num).toInt();
  }

  Future<void> markAsRead(String id) async {
    final response = await http.post(
      Uri.parse('${ApiConfig.baseUrl}/notifications/$id/read'),
      headers: await _authHeaders(),
    );
    _ensureOk(response, const [200, 204]);
  }

  Future<void> markAllAsRead() async {
    final response = await http.post(
      Uri.parse('${ApiConfig.baseUrl}/notifications/read-all'),
      headers: await _authHeaders(),
    );
    _ensureOk(response, const [200, 204]);
  }

  void _ensureOk(http.Response response, List<int> okCodes) {
    if (okCodes.contains(response.statusCode)) return;

    String message = 'Something went wrong. Try again.';
    try {
      message = jsonDecode(response.body)['message'] ?? message;
    } catch (_) {
      // body wasn't JSON (e.g. an empty 403) — keep the generic message
    }
    throw ApiException(message);
  }
}