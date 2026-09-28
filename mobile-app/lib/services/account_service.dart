import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';
import '../models/account.dart';
import '../models/transaction.dart';
import 'auth_service.dart';

class AccountService {
  final _authService = AuthService();

  Future<Map<String, String>> _authHeaders() async {
    final token = await _authService.getAccessToken();
    return {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }

  Future<Account> getMyAccount() async {
    final response = await http.get(
      Uri.parse('${ApiConfig.baseUrl}/accounts/me'),
      headers: await _authHeaders(),
    );

    if (response.statusCode != 200) {
      throw ApiException(_extractMessage(response.body));
    }
    return Account.fromJson(jsonDecode(response.body));
  }

  /// Backend returns a Spring Page object — the actual list lives under
  /// the "content" key, not at the top level.
  Future<List<BankTransaction>> getMyTransactions({int page = 0, int size = 20}) async {
    final response = await http.get(
      Uri.parse('${ApiConfig.baseUrl}/accounts/me/transactions?page=$page&size=$size'),
      headers: await _authHeaders(),
    );

    if (response.statusCode != 200) {
      throw ApiException(_extractMessage(response.body));
    }

    final data = jsonDecode(response.body);
    final content = data['content'] as List;
    return content.map((json) => BankTransaction.fromJson(json)).toList();
  }

  String _extractMessage(String responseBody) {
    try {
      final decoded = jsonDecode(responseBody);
      return decoded['message'] ?? 'Something went wrong. Try again.';
    } catch (_) {
      return 'Something went wrong. Try again.';
    }
  }
}