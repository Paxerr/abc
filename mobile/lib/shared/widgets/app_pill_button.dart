import 'package:mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_shapes.dart';
import '../../core/theme/app_typography.dart';

enum AppButtonVariant { primary, secondary, frosted, coral, outline }

class AppPillButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;
  final IconData? leadingIcon;
  final IconData? trailingIcon;
  final AppButtonVariant variant;
  final double height;
  final double? width;
  final double fontSize;
  final bool isLoading;

  const AppPillButton({
    super.key,
    required this.label,
    this.onPressed,
    this.leadingIcon,
    this.trailingIcon,
    this.variant = AppButtonVariant.primary,
    this.height = 48.0,
    this.width,
    this.fontSize = 14.0,
    this.isLoading = false,
  });

  @override
  Widget build(BuildContext context) {
    Color bg;
    Color fg;
    Border? border;

    switch (variant) {
      case AppButtonVariant.primary:
        bg = AppColors.secondary;
        fg = AppColors.onSecondary;
        break;
      case AppButtonVariant.secondary:
        bg = AppColors.surfaceContainerLow;
        fg = AppColors.onSurface;
        break;
      case AppButtonVariant.frosted:
        bg = Colors.white.withValues(alpha: 0.2);
        fg = AppColors.inverseOnSurface;
        break;
      case AppButtonVariant.coral:
        bg = AppColors.primaryContainer;
        fg = AppColors.onPrimary;
        break;
      case AppButtonVariant.outline:
        bg = Colors.transparent;
        fg = AppColors.secondary;
        border = Border.all(color: AppColors.borderSubtle);
        break;
    }

    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: isLoading ? null : onPressed,
        borderRadius: AppShapes.radiusFull,
        child: Ink(
          width: width,
          height: height,
          decoration: BoxDecoration(
            color: bg,
            borderRadius: AppShapes.radiusFull,
            border: border,
            boxShadow: variant == AppButtonVariant.primary
                ? [
                    BoxShadow(
                      color: AppColors.secondary.withValues(alpha: 0.25),
                      blurRadius: 12,
                      offset: const Offset(0, 4),
                    ),
                  ]
                : null,
          ),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              mainAxisSize: width == null ? MainAxisSize.min : MainAxisSize.max,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                if (isLoading) ...[
                  SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: fg,
                    ),
                  ),
                  const SizedBox(width: 8),
                ] else if (leadingIcon != null) ...[
                  Icon(leadingIcon, size: 18, color: fg),
                  const SizedBox(width: 8),
                ],
                Flexible(
                  child: LocalizedText(
                    label,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    textAlign: TextAlign.center,
                    style: AppTypography.labelLg(color: fg).copyWith(
                      fontSize: fontSize,
                    ),
                  ),
                ),
                if (trailingIcon != null) ...[
                  const SizedBox(width: 8),
                  Icon(trailingIcon, size: 18, color: fg),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
