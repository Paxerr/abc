import '../auth/auth_session.dart';

class BookingHold {
  const BookingHold({
    required this.id,
    required this.status,
    required this.totalAmount,
    required this.expiresAt,
  });
  final String id, status;
  final num totalAmount;
  final DateTime? expiresAt;

  bool get confirmed => status == 'CONFIRMED';
  bool get expired => expiresAt != null && !DateTime.now().isBefore(expiresAt!);

  factory BookingHold.fromJson(Map<String, dynamic> json) {
    final expiresAt = DateTime.tryParse(
      json['holdExpiresAt']?.toString() ?? '',
    );
    if (json['bookingId'] is! String ||
        (json['bookingId'] as String).isEmpty ||
        json['status'] is! String ||
        json['totalAmount'] is! num ||
        (json['status'] == 'HOLD' && expiresAt == null)) {
      throw AuthFailure('Dữ liệu máy chủ không hợp lệ.');
    }
    return BookingHold(
      id: json['bookingId'] as String,
      status: json['status'] as String,
      totalAmount: json['totalAmount'] as num,
      expiresAt: expiresAt,
    );
  }
}

class BookingRepository {
  BookingRepository({AuthSession? session})
    : session = session ?? AuthSession.instance;
  final AuthSession session;

  Future<BookingHold> hold({
    required String slotId,
    required String optionId,
    required int quantity,
    int? participantsCount,
    bool allowSplit = false,
  }) async {
    if (slotId.isEmpty ||
        optionId.isEmpty ||
        quantity < 1 ||
        quantity > 100 ||
        (participantsCount != null && participantsCount < 1)) {
      throw AuthFailure('Vui lòng kiểm tra số lượng và khung giờ.');
    }
    return BookingHold.fromJson(
      await session.request(
        '/api/bookings/hold',
        method: 'POST',
        data: {
          'items': [
            {
              'slotId': slotId,
              'optionId': optionId,
              'quantity': quantity,
              if (participantsCount != null)
                'participantsCount': participantsCount,
              'allowSplit': allowSplit,
            },
          ],
        },
      ),
    );
  }

  Future<BookingHold> confirm(String holdId) async => BookingHold.fromJson(
    await session.request(
      '/api/bookings/${Uri.encodeComponent(holdId)}/confirm',
      method: 'POST',
    ),
  );

  Future<void> cancel(String holdId) async {
    // DELETE returns 204 with no JSON payload.
    await session.requestJson(
      '/api/bookings/hold/${Uri.encodeComponent(holdId)}',
      method: 'DELETE',
    );
  }
}
