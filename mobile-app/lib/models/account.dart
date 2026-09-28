class Account {
  final String id;
  final String accountNumber;
  final double balance;
  final String status;

  Account({
    required this.id,
    required this.accountNumber,
    required this.balance,
    required this.status,
  });

  factory Account.fromJson(Map<String, dynamic> json) {
    return Account(
      id: json['id'],
      accountNumber: json['accountNumber'],
      balance: (json['balance'] as num).toDouble(),
      status: json['status'],
    );
  }
}