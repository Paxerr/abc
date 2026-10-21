import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../l10n/app_localizations.dart';

class AuthFailure implements Exception {
  final String message;
  final int status;
  AuthFailure(this.message, [this.status = 0]);
  @override
  String toString() => message;
}

class AuthSession {
  static final instance = AuthSession();
  final FlutterSecureStorage storage;
  final HttpClient client;
  final String baseUrl;
  AuthSession({
    FlutterSecureStorage? storage,
    HttpClient? client,
    String? baseUrl,
  }) : storage = storage ?? const FlutterSecureStorage(),
       client = client ?? HttpClient(),
       baseUrl =
           baseUrl ??
           const String.fromEnvironment(
             'API_BASE_URL',
             defaultValue: 'http://127.0.0.1:8080',
           );
  String? _access, _refresh;
  String? pendingName;
  String? pendingEmail;
  bool remember = true;
  bool pendingVerification = false;
  Map<String, dynamic>? profile;
  Future<void>? _refreshing;
  String get _key => 'auth.session.$baseUrl';

  Future<Map<String, dynamic>> request(
    String path, {
    String method = 'GET',
    Map<String, dynamic>? data,
    bool retry = true,
    bool authenticated = true,
  }) async {
    final uri = Uri.parse('$baseUrl$path');
    if (kReleaseMode && uri.scheme != 'https')
      throw AuthFailure('Backend phải dùng HTTPS.');
    try {
      final req = await client
          .openUrl(method, uri)
          .timeout(const Duration(seconds: 15));
      req.followRedirects = false;
      req.headers.contentType = ContentType.json;
      req.headers.set('Accept-Language', AppLanguage.instance.code);
      if (authenticated && _access != null)
        req.headers.set('Authorization', 'Bearer $_access');
      if (_refresh != null &&
          (path == '/api/auth/refresh' || path == '/api/auth/logout')) {
        req.cookies.add(Cookie('refresh_token', _refresh!));
      }
      if (data != null) req.write(jsonEncode(data));
      final response = await req.close().timeout(const Duration(seconds: 20));
      final text = await utf8.decoder
          .bind(response)
          .join()
          .timeout(const Duration(seconds: 20));
      Map<String, dynamic> body = {};
      try {
        if (text.isNotEmpty) body = jsonDecode(text) as Map<String, dynamic>;
      } catch (_) {}
      if (response.statusCode == 401 &&
          retry &&
          !pendingVerification &&
          !path.startsWith('/api/auth/')) {
        await refresh();
        return request(path, method: method, data: data, retry: false);
      }
      if (response.statusCode >= 300) {
        throw AuthFailure(
          body['message']?.toString() ??
              'Không thể thực hiện yêu cầu. Vui lòng thử lại.',
          response.statusCode,
        );
      }
      for (final cookie in response.cookies) {
        if (cookie.name == 'refresh_token' && cookie.value.isNotEmpty)
          _refresh = cookie.value;
      }
      if (body['accessToken'] is String) _access = body['accessToken'];
      return body;
    } on SocketException {
      throw AuthFailure(
        'Không kết nối được máy chủ. Kiểm tra mạng và địa chỉ backend.',
      );
    } on TimeoutException {
      throw AuthFailure('Máy chủ phản hồi quá lâu. Vui lòng thử lại.');
    }
  }

  Future<void> _save() => !remember
      ? storage.delete(key: _key)
      : storage.write(
          key: _key,
          value: jsonEncode({
            'refresh': _refresh,
            'access': pendingVerification ? _access : null,
            'pending': pendingVerification,
            'name': pendingName,
            'email': pendingEmail,
          }),
        );
  Future<bool> restore() async {
    final raw = await storage.read(key: _key);
    if (raw == null) return false;
    try {
      final saved = jsonDecode(raw) as Map<String, dynamic>;
      _refresh = saved['refresh'] as String?;
      _access = saved['access'] as String?;
      pendingVerification = saved['pending'] == true;
      pendingName = saved['name'] as String?;
      pendingEmail = saved['email'] as String?;
      if (pendingVerification) return true;
      await refresh();
      await loadProfile();
      return true;
    } on AuthFailure catch (e) {
      if (e.status != 401) rethrow;
      await clear();
      return false;
    } on FormatException {
      await clear();
      return false;
    }
  }

  Future<void> loginWithGoogle(String idToken) async {
    if (idToken.trim().isEmpty)
      throw AuthFailure('Thiếu thông tin xác thực Google.');
    await clear();
    try {
      await request(
        '/api/auth/oauth2/google',
        method: 'POST',
        data: {'idToken': idToken},
        authenticated: false,
        retry: false,
      );
      await loadProfile();
      await _save();
    } catch (_) {
      await clear();
      rethrow;
    }
  }

  Future<void> login(String email, String password) async {
    await request(
      '/api/auth/login',
      method: 'POST',
      data: {'email': email.trim(), 'password': password},
    );
    pendingVerification = false;
    if (pendingEmail?.toLowerCase() != email.trim().toLowerCase())
      pendingName = null;
    pendingEmail = null;
    await _save();
    await loadProfile();
  }

  Future<void> register(String name, String email, String password) async {
    await request(
      '/api/auth/register',
      method: 'POST',
      data: {'email': email.trim(), 'password': password},
    );
    pendingName = name.trim();
    pendingEmail = email.trim();
    pendingVerification = true;
    await _save();
  }

  Future<void> requestPasswordReset(String email) async {
    await request(
      '/api/auth/forgot-password',
      method: 'POST',
      data: {'email': email.trim()},
      authenticated: false,
      retry: false,
    );
  }

  Future<void> resetPassword(String email, String otp, String password) async {
    await request(
      '/api/auth/reset-password',
      method: 'POST',
      data: {'email': email.trim(), 'otp': otp.trim(), 'newPassword': password},
      authenticated: false,
      retry: false,
    );
    await clear();
  }

  Future<void> sendOtp() async {
    await request('/api/auth/otp/send', method: 'POST');
  }

  Future<void> verify(String code) async {
    if (pendingVerification)
      await request(
        '/api/auth/otp/verify',
        method: 'POST',
        data: {'code': code.trim()},
      );
    pendingVerification = false;
    await _save();
    await loadProfile();
  }

  Future<void> loadProfile() async {
    profile = await request('/api/users/me');
    if (pendingName?.isNotEmpty == true) {
      profile = await request(
        '/api/users/me',
        method: 'PATCH',
        data: {'fullName': pendingName},
      );
      pendingName = null;
      await _save();
    }
  }

  Future<void> refresh() =>
      _refreshing ??= _doRefresh().whenComplete(() => _refreshing = null);
  Future<void> _doRefresh() async {
    if (_refresh == null) throw AuthFailure('Phiên đăng nhập đã hết hạn.', 401);
    await request('/api/auth/refresh', method: 'POST', retry: false);
    await _save();
  }

  Future<void> logout() async {
    try {
      await request('/api/auth/logout', method: 'POST', retry: false);
    } finally {
      await clear();
    }
  }

  Future<void> clear() async {
    _access = null;
    _refresh = null;
    profile = null;
    pendingName = null;
    pendingEmail = null;
    pendingVerification = false;
    await storage.delete(key: _key);
  }

  static bool strongPassword(String value) =>
      value.length <= 100 &&
      RegExp(
        r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$',
      ).hasMatch(value);
}
