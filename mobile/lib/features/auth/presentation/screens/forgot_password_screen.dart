import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../../core/auth/auth_session.dart';
import '../../../../core/l10n/app_localizations.dart';
import 'session_gate.dart';
import '../widgets/recovery_widgets.dart';

class ForgotPasswordScreen extends StatefulWidget {
  final String? initialEmail;
  final AuthSession? auth;
  const ForgotPasswordScreen({super.key, this.initialEmail, this.auth});
  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  final _form = GlobalKey<FormState>();
  late final _email = TextEditingController(text: widget.initialEmail);
  final _otp = TextEditingController();
  final _password = TextEditingController();
  final _confirm = TextEditingController();
  AuthSession get _auth => widget.auth ?? AuthSession.instance;
  int _step = 0;
  bool _busy = false, _done = false, _obscure = true;
  String? _error;
  int _seconds = 0;
  Timer? _timer;

  @override
  void dispose() {
    _timer?.cancel();
    for (final controller in [_email, _otp, _password, _confirm]) {
      controller.dispose();
    }
    super.dispose();
  }

  void _cooldown() {
    _timer?.cancel();
    _seconds = 60;
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) return;
      setState(() => _seconds--);
      if (_seconds == 0) timer.cancel();
    });
  }

  Future<void> _send({bool resend = false}) async {
    if (_busy || (!resend && !_form.currentState!.validate())) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await _auth.requestPasswordReset(_email.text);
      if (!mounted) return;
      setState(() {
        _step = 1;
        _otp.clear();
        _cooldown();
      });
    } on AuthFailure catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) {
        setState(
          () => _error = 'Không thể thực hiện yêu cầu. Vui lòng thử lại.',
        );
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _reset() async {
    if (_busy || !_form.currentState!.validate()) return;
    FocusScope.of(context).unfocus();
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await _auth.resetPassword(_email.text, _otp.text, _password.text);
      if (!mounted) return;
      _timer?.cancel();
      _password.clear();
      _confirm.clear();
      _otp.clear();
      setState(() => _done = true);
    } on AuthFailure catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) {
        setState(
          () => _error = 'Không thể thực hiện yêu cầu. Vui lòng thử lại.',
        );
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _login() => Navigator.of(context).pushAndRemoveUntil(
    MaterialPageRoute(builder: (_) => const SessionGate()),
    (_) => false,
  );

  void _back() {
    if (_busy) return;
    if (_step > 0) {
      FocusScope.of(context).unfocus();
      setState(() {
        _step--;
        _error = null;
      });
    } else {
      Navigator.of(context).maybePop();
    }
  }

  void _next() {
    if (!_form.currentState!.validate()) return;
    FocusScope.of(context).unfocus();
    // This only advances the form. The existing reset endpoint validates OTP.
    setState(() {
      _step = 2;
      _error = null;
    });
  }

  InputDecoration _decoration(String label, IconData icon, {Widget? suffix}) =>
      InputDecoration(
        hintText: tr(context, label),
        prefixIcon: Icon(icon, size: 21, color: recoveryMuted),
        suffixIcon: suffix,
        filled: true,
        fillColor: const Color(0xFFF4F6F5),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 22,
          vertical: 19,
        ),
        hintStyle: const TextStyle(color: recoveryMuted, fontSize: 14),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(30),
          borderSide: BorderSide.none,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(30),
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(30),
          borderSide: const BorderSide(color: recoveryTeal, width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(30),
          borderSide: const BorderSide(color: Colors.redAccent),
        ),
        errorMaxLines: 3,
      );

  Widget _button(String text, VoidCallback action) => SizedBox(
    width: double.infinity,
    height: 54,
    child: FilledButton(
      onPressed: _busy ? null : action,
      style: FilledButton.styleFrom(
        backgroundColor: recoveryTeal,
        foregroundColor: Colors.white,
        disabledBackgroundColor: recoveryTeal.withValues(alpha: .35),
        shape: const StadiumBorder(),
        textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
      ),
      child: _busy
          ? const SizedBox(
              width: 22,
              height: 22,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                color: Colors.white,
              ),
            )
          : LocalizedText(text),
    ),
  );

  Widget _requirement(String label, bool met) => Row(
    mainAxisSize: MainAxisSize.min,
    children: [
      Icon(
        met ? Icons.check_circle_rounded : Icons.radio_button_unchecked,
        size: 16,
        color: met ? recoveryTeal : recoveryMuted,
      ),
      const SizedBox(width: 6),
      LocalizedText(
        label,
        style: TextStyle(
          fontSize: 12,
          color: met ? recoveryTeal : recoveryMuted,
        ),
      ),
    ],
  );

  Widget _otpInput() => Column(
    children: [
      Stack(
        alignment: Alignment.topCenter,
        children: [
          ExcludeSemantics(
            child: Row(
              children: [
                for (var i = 0; i < 6; i++) ...[
                  if (i > 0) const SizedBox(width: 8),
                  Expanded(
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 180),
                      height: 60,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(
                          color: _error != null
                              ? Colors.redAccent
                              : (i == _otp.text.length
                                    ? recoveryTeal
                                    : const Color(0xFFCCD5D2)),
                          width: 1.5,
                        ),
                      ),
                      child: Text(
                        i < _otp.text.length ? _otp.text[i] : '',
                        style: const TextStyle(
                          fontSize: 24,
                          fontWeight: FontWeight.w600,
                          color: recoveryInk,
                        ),
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ),
          TextFormField(
            key: const ValueKey('recovery-otp'),
            controller: _otp,
            enabled: !_busy,
            keyboardType: TextInputType.number,
            autofillHints: const [AutofillHints.oneTimeCode],
            inputFormatters: [
              FilteringTextInputFormatter.digitsOnly,
              LengthLimitingTextInputFormatter(6),
            ],
            style: const TextStyle(
              color: Colors.transparent,
              height: 1.5,
              fontSize: 24,
            ),
            cursorColor: Colors.transparent,
            enableInteractiveSelection: true,
            decoration: InputDecoration(
              labelText: tr(context, 'Mã OTP'),
              floatingLabelBehavior: FloatingLabelBehavior.never,
              labelStyle: const TextStyle(color: Colors.transparent),
              filled: false,
              contentPadding: const EdgeInsets.symmetric(vertical: 14),
              border: InputBorder.none,
              enabledBorder: InputBorder.none,
              focusedBorder: InputBorder.none,
              errorBorder: InputBorder.none,
              focusedErrorBorder: InputBorder.none,
            ),
            onChanged: (_) => setState(() => _error = null),
            onFieldSubmitted: (_) => _next(),
            validator: (v) => RegExp(r'^\d{6}$').hasMatch(v ?? '')
                ? null
                : tr(context, 'Nhập mã OTP gồm 6 số.'),
          ),
        ],
      ),
      const SizedBox(height: 20),
      _button('Tiếp tục', _next),
      const SizedBox(height: 8),
      TextButton(
        onPressed: _busy || _seconds > 0 ? null : () => _send(resend: true),
        style: TextButton.styleFrom(foregroundColor: recoveryTeal),
        child: Text(
          '${tr(context, "Gửi lại mã")}${_seconds > 0 ? " (${_seconds ~/ 60}:${(_seconds % 60).toString().padLeft(2, '0')})" : ""}',
        ),
      ),
    ],
  );

  Widget _fields() {
    if (_step == 0) {
      return Column(
        children: [
          TextFormField(
            key: const ValueKey('recovery-email'),
            controller: _email,
            enabled: !_busy,
            keyboardType: TextInputType.emailAddress,
            autofillHints: const [AutofillHints.email],
            autocorrect: false,
            decoration: _decoration('Email', Icons.mail_outline_rounded),
            validator: (v) =>
                RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$').hasMatch(v?.trim() ?? '')
                ? null
                : tr(context, 'Nhập email hợp lệ.'),
          ),
          const SizedBox(height: 22),
          _button('Gửi mã xác nhận', () => _send()),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const LocalizedText(
                'Bạn đã nhớ mật khẩu?',
                style: TextStyle(color: recoveryMuted, fontSize: 13),
              ),
              TextButton(
                onPressed: _busy ? null : _login,
                style: TextButton.styleFrom(foregroundColor: recoveryInk),
                child: const LocalizedText('Đăng nhập'),
              ),
            ],
          ),
        ],
      );
    }
    if (_step == 1) {
      return _otpInput();
    }
    final strong = AuthSession.strongPassword(_password.text);
    final matches = _confirm.text.isNotEmpty && _confirm.text == _password.text;
    final strength = [
      _password.text.length >= 8,
      RegExp(r'[A-Z]').hasMatch(_password.text) &&
          RegExp(r'[a-z]').hasMatch(_password.text),
      RegExp(r'[0-9]').hasMatch(_password.text),
      RegExp(r'[@$!%*?&]').hasMatch(_password.text),
    ].where((v) => v).length;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TextFormField(
          key: const ValueKey('recovery-password'),
          controller: _password,
          enabled: !_busy,
          obscureText: _obscure,
          autocorrect: false,
          enableSuggestions: false,
          onChanged: (_) => setState(() {}),
          decoration: _decoration(
            'Mật khẩu mới',
            Icons.lock_outline_rounded,
            suffix: IconButton(
              tooltip: tr(context, 'Hiện/ẩn mật khẩu'),
              onPressed: () => setState(() => _obscure = !_obscure),
              icon: Icon(
                _obscure
                    ? Icons.visibility_off_outlined
                    : Icons.visibility_outlined,
                color: recoveryMuted,
                size: 21,
              ),
            ),
          ),
          validator: (v) => AuthSession.strongPassword(v ?? '')
              ? null
              : tr(
                  context,
                  'Dùng 8–100 ký tự gồm chữ hoa, chữ thường, số và @\$!%*?&.',
                ),
        ),
        const SizedBox(height: 14),
        TextFormField(
          key: const ValueKey('recovery-confirm'),
          controller: _confirm,
          enabled: !_busy,
          obscureText: _obscure,
          autocorrect: false,
          enableSuggestions: false,
          onChanged: (_) => setState(() {}),
          decoration: _decoration(
            'Nhập lại mật khẩu mới',
            Icons.password_rounded,
          ),
          validator: (v) => v == _password.text
              ? null
              : tr(context, 'Mật khẩu nhập lại không khớp.'),
        ),
        const SizedBox(height: 24),
        Row(
          children: [
            for (var i = 0; i < 4; i++)
              Expanded(
                child: Container(
                  height: 4,
                  margin: const EdgeInsets.only(right: 6),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(5),
                    color: i < strength
                        ? recoveryTeal
                        : const Color(0xFFE8ECEA),
                  ),
                ),
              ),
          ],
        ),
        const SizedBox(height: 14),
        Wrap(
          spacing: 16,
          runSpacing: 10,
          children: [
            _requirement('Mật khẩu đủ mạnh', strong),
            _requirement('Mật khẩu khớp', matches),
          ],
        ),
        const SizedBox(height: 26),
        _button('Đặt lại mật khẩu', _reset),
        TextButton(
          onPressed: _busy ? null : _back,
          style: TextButton.styleFrom(foregroundColor: recoveryTeal),
          child: const LocalizedText('Nhập lại mã OTP'),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) => PopScope(
    canPop: !_busy && !_done && _step == 0,
    onPopInvokedWithResult: (didPop, result) {
      if (!didPop && !_busy) {
        if (_done) {
          _login();
        } else {
          _back();
        }
      }
    },
    child: Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) => SingleChildScrollView(
            child: Center(
              child: ConstrainedBox(
                constraints: BoxConstraints(
                  maxWidth: 480,
                  minHeight: constraints.maxHeight,
                ),
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(24, 16, 24, 24),
                  child: _done
                      ? SizedBox(
                          height: constraints.maxHeight > 500
                              ? constraints.maxHeight - 40
                              : 500,
                          child: Column(
                            children: [
                              const Spacer(flex: 3),
                              const RecoverySuccessMark(),
                              const SizedBox(height: 18),
                              const LocalizedText(
                                'Đã cập nhật mật khẩu',
                                textAlign: TextAlign.center,
                                style: TextStyle(
                                  fontSize: 27,
                                  fontWeight: FontWeight.w700,
                                  color: recoveryInk,
                                ),
                              ),
                              const SizedBox(height: 12),
                              const LocalizedText(
                                'Đăng nhập bằng mật khẩu mới để tiếp tục trải nghiệm.',
                                textAlign: TextAlign.center,
                                style: TextStyle(
                                  color: recoveryMuted,
                                  height: 1.6,
                                ),
                              ),
                              const Spacer(flex: 4),
                              _button('Quay lại đăng nhập', _login),
                            ],
                          ),
                        )
                      : Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            RecoveryProgress(
                              step: _step,
                              onBack: _busy ? null : _back,
                            ),
                            const SizedBox(height: 38),
                            AnimatedSwitcher(
                              duration: const Duration(milliseconds: 280),
                              transitionBuilder: (child, animation) =>
                                  FadeTransition(
                                    opacity: animation,
                                    child: SlideTransition(
                                      position: Tween(
                                        begin: const Offset(.05, 0),
                                        end: Offset.zero,
                                      ).animate(animation),
                                      child: child,
                                    ),
                                  ),
                              child: Column(
                                key: ValueKey(_step),
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: [
                                  Align(
                                    alignment: Alignment.centerLeft,
                                    child: RecoveryIllustration(step: _step),
                                  ),
                                  const SizedBox(height: 22),
                                  LocalizedText(
                                    [
                                      'Quên mật khẩu?',
                                      'Kiểm tra email của bạn',
                                      'Tạo mật khẩu mới',
                                    ][_step],
                                    style: const TextStyle(
                                      fontSize: 27,
                                      fontWeight: FontWeight.w700,
                                      color: recoveryInk,
                                      height: 1.2,
                                    ),
                                  ),
                                  const SizedBox(height: 10),
                                  LocalizedText(
                                    [
                                      'Đừng lo, hãy nhập email tài khoản để nhận mã đặt lại mật khẩu.',
                                      'Nhập mã gồm 6 số được gửi đến email của bạn.',
                                      'Tạo mật khẩu an toàn để dùng cho lần đăng nhập tiếp theo.',
                                    ][_step],
                                    style: const TextStyle(
                                      fontSize: 14,
                                      height: 1.6,
                                      color: recoveryMuted,
                                    ),
                                  ),
                                  if (_step == 1)
                                    Text(
                                      _email.text.trim(),
                                      style: const TextStyle(
                                        fontWeight: FontWeight.w600,
                                        height: 1.6,
                                        color: recoveryInk,
                                      ),
                                    ),
                                ],
                              ),
                            ),
                            const SizedBox(height: 30),
                            Form(key: _form, child: _fields()),
                            if (_error != null)
                              Padding(
                                padding: const EdgeInsets.only(top: 16),
                                child: Semantics(
                                  liveRegion: true,
                                  child: Text(
                                    tr(context, _error!),
                                    textAlign: TextAlign.center,
                                    style: const TextStyle(
                                      color: Colors.redAccent,
                                      height: 1.5,
                                    ),
                                  ),
                                ),
                              ),
                          ],
                        ),
                ),
              ),
            ),
          ),
        ),
      ),
    ),
  );
}
