/// Named BankNotification, not Notification: Flutter's widgets library
/// already exports a class called Notification, and the clash causes
/// confusing "ambiguous import" errors.
class BankNotification {
  final String id;
  final String type;
  final String message;
  bool read; // mutable so the UI can flip it instantly when tapped
  final DateTime createdAt;

  BankNotification({
    required this.id,
    required this.type,
    required this.message,
    required this.read,
    required this.createdAt,
  });

  factory BankNotification.fromJson(Map<String, dynamic> json) {
    return BankNotification(
      id: json['id'],
      type: json['type'],
      message: json['message'],
      read: json['read'] ?? false,
      createdAt: DateTime.parse(json['createdAt']),
    );
  }
}