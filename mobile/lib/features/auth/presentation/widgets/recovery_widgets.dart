import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';

/// The recovery flow uses the ocean accent without changing the app theme.
const recoveryTeal = AppColors.secondary;
const recoveryInk = Color(0xFF202928);
const recoveryMuted = Color(0xFF778380);

class RecoveryProgress extends StatelessWidget {
  const RecoveryProgress({super.key, required this.step, required this.onBack});
  final int step;
  final VoidCallback? onBack;

  @override
  Widget build(BuildContext context) => Row(
    children: [
      IconButton(
        onPressed: onBack,
        icon: const Icon(Icons.chevron_left_rounded),
        tooltip: MaterialLocalizations.of(context).backButtonTooltip,
      ),
      const SizedBox(width: 12),
      for (var i = 0; i < 3; i++)
        Expanded(
          child: Padding(
            padding: const EdgeInsets.only(right: 7),
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 280),
              height: 5,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(8),
                color: i <= step ? recoveryTeal : const Color(0xFFE8ECEA),
              ),
            ),
          ),
        ),
      const SizedBox(width: 6),
      Text(
        '${step + 1}/3',
        style: const TextStyle(fontSize: 12, color: recoveryMuted),
      ),
    ],
  );
}

class RecoveryIllustration extends StatelessWidget {
  const RecoveryIllustration({super.key, required this.step});
  final int step;

  @override
  Widget build(BuildContext context) => SizedBox(
    width: 82,
    height: 86,
    child: Stack(
      alignment: Alignment.bottomLeft,
      children: [
        Icon(
          [
            Icons.lock_outline_rounded,
            Icons.mark_email_unread_outlined,
            Icons.key_rounded,
          ][step],
          size: 66,
          color: recoveryInk,
        ),
        Positioned(
          top: 0,
          right: 6,
          child: step == 0
              ? const Text(
                  '?',
                  style: TextStyle(
                    fontSize: 26,
                    color: recoveryTeal,
                    fontWeight: FontWeight.w700,
                  ),
                )
              : Icon(
                  step == 1 ? Icons.more_horiz : Icons.auto_awesome,
                  size: 24,
                  color: recoveryTeal,
                ),
        ),
      ],
    ),
  );
}

class RecoverySuccessMark extends StatelessWidget {
  const RecoverySuccessMark({super.key});
  @override
  Widget build(BuildContext context) => TweenAnimationBuilder<double>(
    tween: Tween(begin: 0, end: 1),
    duration: const Duration(milliseconds: 700),
    curve: Curves.easeOutCubic,
    builder: (context, value, child) => CustomPaint(
      painter: _CelebrationPainter(value),
      child: SizedBox(
        width: 170,
        height: 170,
        child: Center(
          child: Transform.scale(
            scale: 0.65 + value * 0.35,
            child: Container(
              width: 90,
              height: 90,
              decoration: const BoxDecoration(
                color: Color(0xFF32B875),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.check_rounded,
                size: 54,
                color: Colors.white,
              ),
            ),
          ),
        ),
      ),
    ),
  );
}

class _CelebrationPainter extends CustomPainter {
  _CelebrationPainter(this.progress);
  final double progress;
  @override
  void paint(Canvas canvas, Size size) {
    final center = size.center(Offset.zero);
    for (var i = 0; i < 12; i++) {
      final angle = i * math.pi / 6;
      final direction = Offset(math.cos(angle), math.sin(angle));
      final paint = Paint()
        ..color = [
          recoveryTeal,
          const Color(0xFF79D4E2),
          const Color(0xFF32B875),
        ][i % 3].withValues(alpha: progress)
        ..strokeWidth = 3
        ..strokeCap = StrokeCap.round;
      canvas.drawLine(
        center + direction * (55 + progress * 14),
        center + direction * (58 + progress * 19),
        paint,
      );
    }
  }

  @override
  bool shouldRepaint(_CelebrationPainter oldDelegate) =>
      oldDelegate.progress != progress;
}
