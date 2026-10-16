import 'dart:convert';
import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/auth/auth_session.dart';
import 'package:mobile/core/data/vendor_repository.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  late HttpServer server;
  late AuthSession session;
  late VendorRepository repository;
  late Uri requested;
  var status = 200;
  var malformed = false;
  setUp(() async {
    HttpOverrides.global = null;
    status = 200;
    malformed = false;
    server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    session = AuthSession(baseUrl: 'http://127.0.0.1:${server.port}');
    repository = VendorRepository(session: session);
    server.listen((request) async {
      requested = request.uri;
      request.response.statusCode = status;
      request.response.headers.contentType = ContentType.json;
      request.response.write(
        jsonEncode(
          malformed
              ? []
              : status != 200
              ? {'message': 'Vendor not found'}
              : request.uri.path.endsWith('/services')
              ? {
                  'content': [
                    {'id': 'service-1', 'name': 'SUP', 'price': 250000},
                  ],
                  'page': int.parse(request.uri.queryParameters['page']!),
                  'totalElements': 21,
                }
              : {
                  'id': 'vendor-1',
                  'businessName': 'Test Vendor',
                  'address': null,
                  'badgeTier': 'VERIFIED',
                  'ratingAvg': 4.9,
                  'ratingCount': 12,
                  'activeServicesCount': 21,
                },
        ),
      );
      await request.response.close();
    });
  });
  tearDown(() async {
    session.client.close(force: true);
    await server.close(force: true);
  });

  test('loads public vendor profile and handles nullable address', () async {
    final vendor = await repository.profile('vendor-1');
    expect(requested.path, '/api/vendors/vendor-1');
    expect(vendor.businessName, 'Test Vendor');
    expect(vendor.address, '');
    expect(vendor.ratingAvg, 4.9);
    expect(vendor.activeServicesCount, 21);
  });
  test('paginates vendor services and retains vendor identity', () async {
    final page = await repository.services('vendor-1', page: 1);
    expect(requested.path, '/api/vendors/vendor-1/services');
    expect(requested.queryParameters, {'page': '1', 'size': '20'});
    expect(page.page, 1);
    expect(page.total, 21);
    expect(page.items.single.vendorId, 'vendor-1');
    expect(page.items.single.price, 250000);
  });
  test('propagates missing vendor without fake fallback', () async {
    status = 404;
    await expectLater(
      repository.profile('missing'),
      throwsA(isA<AuthFailure>()),
    );
    await expectLater(
      repository.services('missing'),
      throwsA(isA<AuthFailure>()),
    );
  });
  test('rejects malformed responses', () async {
    malformed = true;
    await expectLater(
      repository.profile('vendor-1'),
      throwsA(isA<AuthFailure>()),
    );
    await expectLater(
      repository.services('vendor-1'),
      throwsA(isA<AuthFailure>()),
    );
  });
}
