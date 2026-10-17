class TransferResult {
  final String recipientAccountNumber;
  final double amount;
  final double newBalance;
  final String message;

  TransferResult({
    required this.recipientAccountNumber,
    required this.amount,
    required this.newBalance,
    required this.message,
  });

  factory TransferResult.fromJson(Map<String, dynamic> json) {
    return TransferResult(
      recipientAccountNumber: json['recipientAccountNumber'],
      amount: (json['amount'] as num).toDouble(),
      newBalance: (json['newBalance'] as num).toDouble(),
      message: json['message'] ?? 'Sent!',
    );
  }
}