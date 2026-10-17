import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../services/account_service.dart';
import '../services/auth_service.dart'; // for ApiException

class TransferScreen extends StatefulWidget {
  const TransferScreen({super.key});

  @override
  State<TransferScreen> createState() => _TransferScreenState();
}

class _TransferScreenState extends State<TransferScreen> {
  final _formKey = GlobalKey<FormState>();
  final _accountController = TextEditingController();
  final _amountController = TextEditingController();
  final _referenceController = TextEditingController();
  final _accountService = AccountService();

  bool _isLoading = false;

  @override
  void dispose() {
    _accountController.dispose();
    _amountController.dispose();
    _referenceController.dispose();
    super.dispose();
  }

  Future<void> _handleSend() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);

    try {
      final result = await _accountService.transfer(
        recipientAccountNumber: _accountController.text.trim().toUpperCase(),
        amount: double.parse(_amountController.text.trim()),
        reference: _referenceController.text.trim(),
      );
      if (!mounted) return;

      // The message comes straight from the backend ("Sent! Money moved, no cap 💸")
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(result.message)));
      Navigator.pop(context, true); // true = a transfer happened, home should refresh
    } on ApiException catch (e) {
      // e.g. "Jaden broke and so are you 😭" for insufficient funds
      _showError(e.message);
    } catch (_) {
      _showError('Couldn\'t reach the server. Check your connection.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Send money')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                TextFormField(
                  controller: _accountController,
                  textCapitalization: TextCapitalization.characters,
                  maxLength: 10,
                  decoration: const InputDecoration(
                    labelText: 'Recipient account number',
                    hintText: '4Z12345678',
                    counterText: '',
                  ),
                  validator: (value) {
                    final text = (value ?? '').trim().toUpperCase();
                    if (text.isEmpty) return 'Enter the recipient\'s account number';
                    if (!RegExp(r'^4Z\d{8}$').hasMatch(text)) {
                      return 'That doesn\'t look like a Bank4Z account number';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _amountController,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.]'))],
                  decoration: const InputDecoration(
                    labelText: 'Amount',
                    prefixText: 'R ',
                  ),
                  validator: (value) {
                    final text = (value ?? '').trim();
                    if (text.isEmpty) return 'Enter an amount';
                    if (!RegExp(r'^\d+(\.\d{1,2})?$').hasMatch(text)) {
                      return 'Use a valid amount with at most 2 decimals';
                    }
                    if (double.parse(text) < 0.01) return 'Minimum transfer is R0.01';
                    return null;
                  },
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _referenceController,
                  maxLength: 100,
                  decoration: const InputDecoration(
                    labelText: 'Reference (optional)',
                    hintText: 'Pizza night 🍕',
                  ),
                ),
                const SizedBox(height: 24),
                ElevatedButton(
                  onPressed: _isLoading ? null : _handleSend,
                  child: _isLoading
                      ? const SizedBox(
                    height: 20,
                    width: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                      : const Text('Send it'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}