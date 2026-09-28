class BankTransaction {
  final String id;
  final double amount;
  final String type;
  final String status;
  final String? reference;
  final DateTime createdAt;

  BankTransaction({
    required this.id,
    required this.amount,
    required this.type,
    required this.status,
    this.reference,
    required this.createdAt,
  });

  bool get isCredit => type == 'DEPOSIT' || type == 'TRANSFER_IN';

  factory BankTransaction.fromJson(Map<String, dynamic> json) {
    return BankTransaction(
      id: json['id'],
      amount: (json['amount'] as num).toDouble(),
      type: json['type'],
      status: json['status'],
      reference: json['reference'],
      createdAt: DateTime.parse(json['createdAt']),
    );
  }
}