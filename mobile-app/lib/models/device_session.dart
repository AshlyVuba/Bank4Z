class DeviceSession {
  final String id;
  final String deviceName;
  final String? ipAddress;
  final DateTime createdAt;
  final DateTime lastUsedAt;
  final bool current; // true for the device making this request

  DeviceSession({
    required this.id,
    required this.deviceName,
    required this.ipAddress,
    required this.createdAt,
    required this.lastUsedAt,
    required this.current,
  });

  factory DeviceSession.fromJson(Map<String, dynamic> json) {
    return DeviceSession(
      id: json['id'],
      deviceName: json['deviceName'],
      ipAddress: json['ipAddress'],
      createdAt: DateTime.parse(json['createdAt']),
      lastUsedAt: DateTime.parse(json['lastUsedAt']),
      current: json['current'] ?? false,
    );
  }
}