import 'package:flutter/material.dart';
import '../services/auth_service.dart';
import '../services/account_service.dart';
import '../models/account.dart';
import '../models/transaction.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  static const int _pageSize = 20;

  final _accountService = AccountService();
  Account? _account;
  final List<BankTransaction> _transactions = [];

  int _page = 0;
  bool _hasMore = true;
  bool _isLoading = true;
  bool _isLoadingMore = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadInitial();
  }

  Future<void> _loadInitial() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final account = await _accountService.getMyAccount();
      final transactions = await _accountService.getMyTransactions(page: 0, size: _pageSize);
      setState(() {
        _account = account;
        _transactions
          ..clear()
          ..addAll(transactions);
        _page = 0;
        _hasMore = transactions.length == _pageSize;
      });
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    } catch (_) {
      setState(() => _error = 'Couldn\'t reach the server. Check your connection.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _loadMore() async {
    if (_isLoadingMore || !_hasMore) return;
    setState(() => _isLoadingMore = true);

    try {
      final nextPage = _page + 1;
      final transactions = await _accountService.getMyTransactions(page: nextPage, size: _pageSize);
      setState(() {
        _transactions.addAll(transactions);
        _page = nextPage;
        _hasMore = transactions.length == _pageSize;
      });
    } catch (_) {
      // "Load more" fails silently — the user still has their existing data
      // on screen, so no need to interrupt them for a background fetch.
    } finally {
      if (mounted) setState(() => _isLoadingMore = false);
    }
  }

  Future<void> _handleLogout() async {
    await AuthService().logout();
    if (!mounted) return;
    Navigator.pushReplacementNamed(context, '/login');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Bank4Z'),
        actions: [
          IconButton(icon: const Icon(Icons.logout), onPressed: _handleLogout),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
          ? _buildError()
          : RefreshIndicator(
        onRefresh: _loadInitial,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            _buildBalanceCard(),
            const SizedBox(height: 24),
            const Text(
              'Recent activity',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            if (_transactions.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 32),
                child: Center(child: Text('Nothing here yet — go make some moves 💸')),
              )
            else
              ..._transactions.map(_buildTransactionTile),
            if (_hasMore)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 16),
                child: Center(
                  child: _isLoadingMore
                      ? const CircularProgressIndicator()
                      : TextButton(onPressed: _loadMore, child: const Text('Load more')),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(_error!, textAlign: TextAlign.center),
            const SizedBox(height: 16),
            ElevatedButton(onPressed: _loadInitial, child: const Text('Try again')),
          ],
        ),
      ),
    );
  }

  Widget _buildBalanceCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(_account?.accountNumber ?? '', style: const TextStyle(color: Colors.grey)),
            const SizedBox(height: 8),
            Text(
              'R ${_account?.balance.toStringAsFixed(2) ?? '0.00'}',
              style: const TextStyle(fontSize: 32, fontWeight: FontWeight.bold),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTransactionTile(BankTransaction tx) {
    return ListTile(
      leading: Icon(
        tx.isCredit ? Icons.arrow_downward : Icons.arrow_upward,
        color: tx.isCredit ? Colors.green : Colors.redAccent,
      ),
      title: Text(tx.reference ?? tx.type),
      subtitle: Text(tx.createdAt.toLocal().toString()),
      trailing: Text(
        '${tx.isCredit ? '+' : '-'}R ${tx.amount.toStringAsFixed(2)}',
        style: TextStyle(
          color: tx.isCredit ? Colors.green : Colors.redAccent,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}