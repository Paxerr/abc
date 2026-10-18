import 'dart:convert';
import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:vendor_mobile/core/auth/auth_session.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  late HttpServer server;
  late AuthSession auth;
  final calls = <String>[];
  setUp(() async {
    HttpOverrides.global = null;
    FlutterSecureStorage.setMockInitialValues({});
    calls.clear();
    server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    auth = AuthSession(baseUrl: 'http://127.0.0.1:${server.port}');
    server.listen((req) async {
      calls.add('${req.method} ${req.uri.path}');
      req.response.headers.contentType = ContentType.json;
      final raw = await utf8.decoder.bind(req).join();
      if (req.uri.path == '/api/auth/oauth2/google') {
        expect(req.method, 'POST');
        expect(req.headers.value('Authorization'), isNull);
        final body = jsonDecode(raw) as Map;
        expect(body.keys, ['idToken']);
        if (body['idToken'] == 'rejected') {
          req.response.statusCode = 401;
          req.response.write(jsonEncode({'message': 'Invalid Google token'}));
        } else {
          req.response.cookies.add(Cookie('refresh_token', 'google-refresh'));
          req.response.write(jsonEncode({'accessToken': 'google-access'}));
        }
      } else if (req.uri.path == '/api/users/me') {
        expect(req.method, 'GET');
        expect(req.headers.value('Authorization'), 'Bearer google-access');
        req.response.write(
          jsonEncode({'id': 'google-user', 'fullName': 'Google Name'}),
        );
      } else if (req.uri.path == '/api/auth/refresh') {
        expect(req.cookies.single.value, 'google-refresh');
        req.response.write(jsonEncode({'accessToken': 'google-access'}));
      }
      await req.response.close();
    });
  });
  tearDown(() async {
    auth.client.close(force: true);
    await server.close(force: true);
  });
  test(
    'Google exchange clears pending OTP, keeps Google profile and restores session',
    () async {
      auth.pendingName = 'Stale registration name';
      auth.pendingVerification = true;
      await auth.loginWithGoogle('google-id-token');
      expect(auth.pendingVerification, isFalse);
      expect(auth.pendingName, isNull);
      expect(auth.profile?['fullName'], 'Google Name');
      expect(calls, ['POST /api/auth/oauth2/google', 'GET /api/users/me']);
      final restored = AuthSession(baseUrl: auth.baseUrl);
      try {
        expect(await restored.restore(), isTrue);
        expect(restored.profile?['id'], 'google-user');
      } finally {
        restored.client.close(force: true);
      }
    },
  );
  test('Rejected Google token leaves no authenticated session', () async {
    await auth.loginWithGoogle('valid');
    await expectLater(
      auth.loginWithGoogle('rejected'),
      throwsA(isA<AuthFailure>()),
    );
    expect(auth.profile, isNull);
    expect(await auth.restore(), isFalse);
  });
  test('Empty token is rejected without a network call', () async {
    await expectLater(auth.loginWithGoogle('  '), throwsA(isA<AuthFailure>()));
    expect(calls, isEmpty);
  });
}
