import 'package:flutter/material.dart';
import '../models/bank_notification.dart';
import '../services/auth_service.dart'; // for ApiException
import '../services/notification_service.dart';

class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  static const int _pageSize = 20;

  final _service = NotificationService();
  final List<BankNotification> _items = [];

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
      final items = await _service.getNotifications(page: 0, size: _pageSize);
      setState(() {
        _items
          ..clear()
          ..addAll(items);
        _page = 0;
        _hasMore = items.length == _pageSize;
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
      final items = await _service.getNotifications(page: nextPage, size: _pageSize);
      setState(() {
        _items.addAll(items);
        _page = nextPage;
        _hasMore = items.length == _pageSize;
      });
    } catch (_) {
      // Silent: the user already has data on screen.
    } finally {
      if (mounted) setState(() => _isLoadingMore = false);
    }
  }

  Future<void> _markRead(BankNotification n) async {
    if (n.read) return;
    setState(() => n.read = true); // optimistic: feels instant
    try {
      await _service.markAsRead(n.id);
    } catch (_) {
      if (mounted) setState(() => n.read = false); // roll back if it failed
    }
  }

  Future<void> _markAllRead() async {
    try {
      await _service.markAllAsRead();
      if (!mounted) return;
      setState(() {
        for (final n in _items) {
          n.read = true;
        }
      });
    } on ApiException catch (e) {
      _showError(e.message);
    } catch (_) {
      _showError('Couldn\'t reach the server. Check your connection.');
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final hasUnread = _items.any((n) => !n.read);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Notifications'),
        actions: [
          if (hasUnread)
            TextButton(onPressed: _markAllRead, child: const Text('Mark all read')),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
          ? _buildError()
          : RefreshIndicator(
        onRefresh: _loadInitial,
        child: _items.isEmpty ? _buildEmpty() : _buildList(),
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

  Widget _buildEmpty() {
    // A scrollable, so pull-to-refresh still works on an empty list
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      children: const [
        SizedBox(height: 120),
        Center(child: Text('All quiet... for now 🤫')),
      ],
    );
  }

  Widget _buildList() {
    return ListView.builder(
      physics: const AlwaysScrollableScrollPhysics(),
      itemCount: _items.length + (_hasMore ? 1 : 0),
      itemBuilder: (context, index) {
        if (index == _items.length) {
          return Padding(
            padding: const EdgeInsets.symmetric(vertical: 16),
            child: Center(
              child: _isLoadingMore
                  ? const CircularProgressIndicator()
                  : TextButton(onPressed: _loadMore, child: const Text('Load more')),
            ),
          );
        }
        return _buildTile(_items[index]);
      },
    );
  }

  Widget _buildTile(BankNotification n) {
    final received = n.type == 'TRANSFER_RECEIVED';
    return ListTile(
      onTap: () => _markRead(n),
      leading: Icon(
        received ? Icons.arrow_downward : Icons.arrow_upward,
        color: received ? Colors.green : Colors.redAccent,
      ),
      title: Text(
        n.message,
        style: TextStyle(fontWeight: n.read ? FontWeight.normal : FontWeight.bold),
      ),
      subtitle: Text(n.createdAt.toLocal().toString()),
      trailing: n.read
          ? null
          : const Icon(Icons.circle, size: 10, color: Colors.purpleAccent),
    );
  }
}