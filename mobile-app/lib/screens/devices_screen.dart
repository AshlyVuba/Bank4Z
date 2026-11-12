import 'package:flutter/material.dart';
import '../models/device_session.dart';
import '../services/auth_service.dart'; // for ApiException
import '../services/session_service.dart';

class DevicesScreen extends StatefulWidget {
  const DevicesScreen({super.key});

  @override
  State<DevicesScreen> createState() => _DevicesScreenState();
}

class _DevicesScreenState extends State<DevicesScreen> {
  final _service = SessionService();

  List<DeviceSession> _sessions = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final sessions = await _service.getSessions();
      setState(() => _sessions = sessions);
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    } catch (_) {
      setState(() => _error = 'Couldn\'t reach the server. Check your connection.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<bool> _confirm(String title, String body) async {
    final result = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(body),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Log out')),
        ],
      ),
    );
    return result == true;
  }

  Future<void> _logoutDevice(DeviceSession session) async {
    final confirmed = await _confirm(
      'Log out ${session.deviceName}?',
      'That device will have to log in again.',
    );
    if (!confirmed || !mounted) return;

    try {
      await _service.revoke(session.id);
      if (!mounted) return;
      setState(() => _sessions.removeWhere((s) => s.id == session.id));
    } on ApiException catch (e) {
      _showMessage(e.message);
    } catch (_) {
      _showMessage('Couldn\'t reach the server. Check your connection.');
    }
  }

  Future<void> _logoutOthers() async {
    final confirmed = await _confirm(
      'Log out other devices?',
      'Every device except this one will have to log in again.',
    );
    if (!confirmed || !mounted) return;

    try {
      final count = await _service.revokeOthers();
      if (!mounted) return;
      _showMessage(count == 0 ? 'No other devices were logged in' : 'Logged out $count other device(s) 🔒');
      await _load();
    } on ApiException catch (e) {
      _showMessage(e.message);
    } catch (_) {
      _showMessage('Couldn\'t reach the server. Check your connection.');
    }
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Devices')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
          ? _buildError()
          : RefreshIndicator(onRefresh: _load, child: _buildList()),
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
            ElevatedButton(onPressed: _load, child: const Text('Try again')),
          ],
        ),
      ),
    );
  }

  Widget _buildList() {
    final hasOthers = _sessions.any((s) => !s.current);

    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.all(16),
      children: [
        const Text(
          'Where you\'re logged in. Don\'t recognise one? Log it out.',
          style: TextStyle(color: Colors.grey),
        ),
        const SizedBox(height: 8),
        ..._sessions.map(_buildTile),
        const SizedBox(height: 16),
        OutlinedButton.icon(
          onPressed: hasOthers ? _logoutOthers : null,
          icon: const Icon(Icons.lock_outline),
          label: const Text('Log out other devices'),
        ),
      ],
    );
  }

  Widget _buildTile(DeviceSession session) {
    final details = StringBuffer('Last active ${session.lastUsedAt.toLocal()}');
    if (session.ipAddress != null) {
      details.write('  ·  ${session.ipAddress}');
    }

    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: Icon(session.current ? Icons.smartphone : Icons.devices_other),
      title: Text(session.current ? '${session.deviceName} (this device)' : session.deviceName),
      subtitle: Text(details.toString()),
      trailing: session.current
          ? null
          : IconButton(
        icon: const Icon(Icons.logout),
        tooltip: 'Log out this device',
        onPressed: () => _logoutDevice(session),
      ),
    );
  }
}