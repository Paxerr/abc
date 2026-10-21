import 'package:mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/data/mock_database_data.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../shared/widgets/app_pill_button.dart';
import '../widgets/booking_slot_sheet.dart';

class ExperienceDetailScreen extends StatefulWidget {
  final ServiceModel service;
  final VoidCallback? onAddToCart;
  final Future<void> Function()? onToggleFavorite;
  final bool favoriteBusy;
  final VoidCallback? onOpenVendor;
  final VoidCallback? onOpenBooking;
  final Function(ServiceModel service, ServiceSlotModel slot, int guests)?
      onBookNow;

  const ExperienceDetailScreen({
    super.key,
    required this.service,
    this.onAddToCart,
    this.onToggleFavorite,
    this.favoriteBusy = false,
    this.onOpenVendor,
    this.onOpenBooking,
    this.onBookNow,
  });

  @override
  State<ExperienceDetailScreen> createState() => _ExperienceDetailScreenState();
}

class _ExperienceDetailScreenState extends State<ExperienceDetailScreen> {
  late bool _isFavorite;
  int _currentImageIndex = 0;
  bool _isDescriptionExpanded = false;

  @override
  void initState() {
    super.initState();
    _isFavorite = widget.service.isFavorite;
  }

  String _formatPrice(int price) {
    final str = price.toString();
    final buffer = StringBuffer();
    int count = 0;
    for (int i = str.length - 1; i >= 0; i--) {
      buffer.write(str[i]);
      count++;
      if (count % 3 == 0 && i > 0) {
        buffer.write('.');
      }
    }
    return '${buffer.toString().split('').reversed.join()} đ';
  }

  void _openBookingSheet() {
    if (widget.onOpenBooking != null) {
      widget.onOpenBooking!();
      return;
    }
    final slots =
        MockDatabaseData.getSlotsForService(widget.service.id, DateTime.now());
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) {
        return BookingSlotSheet(
          service: widget.service,
          availableSlots: slots,
          onConfirm: (slot, guests, isInstantCheckout) {
            if (isInstantCheckout) {
              if (widget.onBookNow != null) {
                widget.onBookNow!(widget.service, slot, guests);
              } else {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: LocalizedText(
                      'Đặt ${widget.service.name} ($guests khách, ${slot.startTime} - ${slot.endTime}) thành công!',
                    ),
                    backgroundColor: AppColors.secondary,
                  ),
                );
              }
            } else {
              if (widget.onAddToCart != null) {
                widget.onAddToCart!();
              }
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: LocalizedText(
                    'Đã thêm ${widget.service.name} vào giỏ hàng!',
                  ),
                  backgroundColor: AppColors.secondary,
                ),
              );
            }
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final service = widget.service;
    final images = service.imageUrls.isNotEmpty
        ? service.imageUrls
        : [
            'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=800&auto=format&fit=crop&q=80'
          ];

    return Scaffold(
      backgroundColor: AppColors.surface,
      body: Stack(
        children: [
          // MAIN SCROLL CONTENT
          SingleChildScrollView(
            padding: const EdgeInsets.only(bottom: 100),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 1. HERO IMAGE CAROUSEL
                Stack(
                  children: [
                    SizedBox(
                      height: 320,
                      width: double.infinity,
                      child: PageView.builder(
                        itemCount: images.length,
                        onPageChanged: (idx) {
                          setState(() => _currentImageIndex = idx);
                        },
                        itemBuilder: (context, index) {
                          return Image.network(
                            images[index],
                            fit: BoxFit.cover,
                            errorBuilder: (context, error, stackTrace) =>
                                Container(
                              color: AppColors.secondaryContainer,
                              child: const Center(
                                child: Icon(Icons.beach_access,
                                    size: 48, color: Colors.white),
                              ),
                            ),
                          );
                        },
                      ),
                    ),
                    // Gradient overlay
                    Positioned.fill(
                      child: Container(
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            begin: Alignment.topCenter,
                            end: Alignment.bottomCenter,
                            colors: [
                              Colors.black.withValues(alpha: 0.4),
                              Colors.transparent,
                              AppColors.surface.withValues(alpha: 0.8),
                              AppColors.surface,
                            ],
                            stops: const [0.0, 0.4, 0.85, 1.0],
                          ),
                        ),
                      ),
                    ),
                    // Image Index Pill
                    Positioned(
                      bottom: 24,
                      right: AppShapes.gutterMobile,
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: Colors.black.withValues(alpha: 0.5),
                          borderRadius: AppShapes.radiusFull,
                        ),
                        child: LocalizedText(
                          '${_currentImageIndex + 1} / ${images.length}',
                          style: AppTypography.labelSm(color: Colors.white),
                        ),
                      ),
                    ),
                  ],
                ),

                // 2. MAIN DETAILS
                Padding(
                  padding: const EdgeInsets.symmetric(
                      horizontal: AppShapes.gutterMobile),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Badges
                      Wrap(
                        spacing: 8,
                        runSpacing: 6,
                        children: [
                          GestureDetector(
                            onTap: widget.onOpenVendor,
                            child: Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 10, vertical: 4),
                            decoration: BoxDecoration(
                              color: AppColors.secondary,
                              borderRadius: AppShapes.radiusFull,
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                const Icon(Icons.verified,
                                    size: 13, color: AppColors.onSecondary),
                                const SizedBox(width: 4),
                                LocalizedText(
                                  service.vendorName,
                                  style: AppTypography.labelSm(
                                    color: AppColors.onSecondary,
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                              ],
                            ),
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 10, vertical: 4),
                            decoration: BoxDecoration(
                              color: AppColors.surfaceContainerHigh,
                              borderRadius: AppShapes.radiusFull,
                            ),
                            child: LocalizedText(
                              service.categoryName,
                              style: AppTypography.labelSm(
                                color: AppColors.secondary,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 10),

                      // Title
                      LocalizedText(
                        service.name,
                        style: AppTypography.headlineLgMobile(
                          color: AppColors.onSurface,
                        ),
                      ),
                      const SizedBox(height: 8),

                      // Rating & Location row
                      Row(
                        children: [
                          const Icon(
                            Icons.star,
                            color: AppColors.starRating, // Coral, NOT YELLOW
                            size: 18,
                          ),
                          const SizedBox(width: 4),
                          LocalizedText(
                            service.avgRating.toStringAsFixed(1),
                            style: AppTypography.labelLg(
                              color: AppColors.onSurface,
                            ),
                          ),
                          const SizedBox(width: 4),
                          Expanded(
                            child: LocalizedText(
                              '(${service.ratingCount} đánh giá)',
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: AppTypography.bodySm(
                                color: AppColors.onSurfaceVariant,
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.schedule,
                                  size: 16, color: AppColors.tertiary),
                              const SizedBox(width: 4),
                              LocalizedText(
                                '${service.durationMinutes} phút',
                                style: AppTypography.labelMd(
                                  color: AppColors.onSurface,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),

                      // Location text
                      Row(
                        children: [
                          const Icon(Icons.location_on,
                              size: 16, color: AppColors.tertiary),
                          const SizedBox(width: 4),
                          Expanded(
                            child: LocalizedText(
                              '${service.locationName} • ${service.address}',
                              style: AppTypography.bodySm(
                                color: AppColors.onSurfaceVariant,
                              ),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),

                      // 3. REAL-TIME OCEAN & SAFETY CARD
                      Container(
                        padding: const EdgeInsets.all(AppShapes.spaceSm),
                        decoration: BoxDecoration(
                          color:
                              AppColors.secondaryContainer.withValues(alpha: 0.35),
                          borderRadius: AppShapes.radiusDefault,
                          border: Border.all(
                            color: AppColors.secondaryContainer,
                            width: 0.8,
                          ),
                        ),
                        child: Row(
                          children: [
                            Container(
                              width: 36,
                              height: 36,
                              decoration: const BoxDecoration(
                                color: AppColors.secondary,
                                shape: BoxShape.circle,
                              ),
                              child: const Icon(Icons.waves,
                                  color: AppColors.onSecondary, size: 20),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  LocalizedText(
                                    'Điều kiện biển hôm nay: Rất tốt',
                                    style: AppTypography.labelMd(
                                      color: AppColors.onSecondaryContainer,
                                      fontWeight: FontWeight.w700,
                                    ),
                                  ),
                                  LocalizedText(
                                    'Sóng 0.4m • Gió 8 km/h • Đủ điều kiện an toàn xuất bến',
                                    style: AppTypography.bodySm(
                                      color: AppColors.onSurfaceVariant,
                                    ).copyWith(fontSize: 12),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 20),

                      // 4. HIGHLIGHTS & INCLUSIONS
                      LocalizedText(
                        'Điểm nổi bật của trải nghiệm',
                        style: AppTypography.headlineSm(
                          color: AppColors.onSurface,
                        ),
                      ),
                      const SizedBox(height: 10),
                      _buildInclusionItem(
                        Icons.camera_alt_outlined,
                        'Miễn phí ảnh máy cơ & flycam',
                        'Chụp ảnh chất lượng cao bắt trọn khoảnh khắc bình minh.',
                      ),
                      _buildInclusionItem(
                        Icons.security_outlined,
                        'Trang thiết bị an toàn tiêu chuẩn',
                        'Áo phao đạt chuẩn, dây leash và ván chèo chuyên dụng.',
                      ),
                      _buildInclusionItem(
                        Icons.sports_outlined,
                        'Huấn luyện viên kèm 1:5',
                        'Hỗ trợ kỹ thuật chèo, cứu hộ và đồng hành suốt chuyến.',
                      ),
                      const SizedBox(height: 16),

                      // 5. DESCRIPTION
                      LocalizedText(
                        'Mô tả chi tiết',
                        style: AppTypography.headlineSm(
                          color: AppColors.onSurface,
                        ),
                      ),
                      const SizedBox(height: 8),
                      LocalizedText(
                        service.description,
                        style: AppTypography.bodyMd(
                          color: AppColors.onSurface,
                        ),
                        maxLines: _isDescriptionExpanded ? null : 4,
                        overflow: _isDescriptionExpanded
                            ? TextOverflow.visible
                            : TextOverflow.ellipsis,
                      ),
                      InkWell(
                        onTap: () {
                          setState(() {
                            _isDescriptionExpanded = !_isDescriptionExpanded;
                          });
                        },
                        child: Padding(
                          padding: const EdgeInsets.symmetric(vertical: 4),
                          child: LocalizedText(
                            _isDescriptionExpanded ? 'Thu gọn' : 'Xem thêm',
                            style: AppTypography.labelMd(
                              color: AppColors.secondary,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: 16),

                      // 6. WAIVER AGREEMENT NOTICE
                      Container(
                        padding: const EdgeInsets.all(AppShapes.spaceSm),
                        decoration: BoxDecoration(
                          color: AppColors.surfaceContainerLow,
                          borderRadius: AppShapes.radiusDefault,
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                const Icon(Icons.gavel,
                                    size: 16, color: AppColors.primary),
                                const SizedBox(width: 6),
                                Expanded(
                                  child: LocalizedText(
                                    'Cam kết miễn trừ trách nhiệm',
                                    style: AppTypography.labelMd(
                                      color: AppColors.onSurface,
                                      fontWeight: FontWeight.w700,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 6),
                            LocalizedText(
                              service.waiverContent,
                              style: AppTypography.bodySm(
                                color: AppColors.onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 20),

                      // 7. REVIEWS PREVIEW
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Expanded(
                            child: LocalizedText(
                              'Đánh giá từ khách hàng (${MockDatabaseData.reviews.length})',
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: AppTypography.headlineSm(
                                color: AppColors.onSurface,
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.star,
                                  color: AppColors.starRating, size: 16),
                              const SizedBox(width: 4),
                              LocalizedText(
                                service.avgRating.toStringAsFixed(1),
                                style: AppTypography.labelMd(
                                  color: AppColors.onSurface,
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),

                      ...MockDatabaseData.reviews.map((rev) {
                        return Container(
                          margin: const EdgeInsets.only(bottom: 12),
                          padding: const EdgeInsets.all(AppShapes.spaceSm),
                          decoration: BoxDecoration(
                            color: AppColors.surfaceContainerLowest,
                            borderRadius: AppShapes.radiusDefault,
                            border: Border.all(color: AppColors.borderSubtle),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  CircleAvatar(
                                    radius: 16,
                                    backgroundColor:
                                        AppColors.secondaryContainer,
                                    child: LocalizedText(
                                      rev.customerName[0],
                                      style: AppTypography.labelSm(
                                        color: AppColors.onSecondaryContainer,
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: [
                                        LocalizedText(
                                          rev.customerName,
                                          style: AppTypography.labelMd(
                                            color: AppColors.onSurface,
                                            fontWeight: FontWeight.w700,
                                          ),
                                        ),
                                        LocalizedText(
                                          'Tháng 10/2024',
                                          style: AppTypography.bodySm(
                                            color: AppColors.outline,
                                          ).copyWith(fontSize: 11),
                                        ),
                                      ],
                                    ),
                                  ),
                                  Row(
                                    children: List.generate(
                                      rev.rating,
                                      (_) => const Icon(
                                        Icons.star,
                                        size: 14,
                                        color: AppColors.starRating,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 8),
                              LocalizedText(
                                rev.comment,
                                style: AppTypography.bodySm(
                                  color: AppColors.onSurface,
                                ),
                              ),
                            ],
                          ),
                        );
                      }),
                    ],
                  ),
                ),
              ],
            ),
          ),

          // TOP NAVIGATION BAR (OVERLAY)
          Positioned(
            top: 0,
            left: 0,
            right: 0,
            child: SafeArea(
              child: Padding(
                padding: const EdgeInsets.symmetric(
                    horizontal: AppShapes.gutterMobile, vertical: 8),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    CircleAvatar(
                      backgroundColor:
                          AppColors.surface.withValues(alpha: 0.85),
                      child: IconButton(
                        icon: const Icon(Icons.arrow_back,
                            color: AppColors.onSurface),
                        onPressed: () => Navigator.pop(context),
                      ),
                    ),
                    Row(
                      children: [
                        CircleAvatar(
                          backgroundColor:
                              AppColors.surface.withValues(alpha: 0.85),
                          child: IconButton(
                            icon: const Icon(Icons.share,
                                color: AppColors.onSurface),
                            onPressed: () {
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(
                                    content: LocalizedText('Đã sao chép liên kết chia sẻ!')),
                              );
                            },
                          ),
                        ),
                        const SizedBox(width: 8),
                        CircleAvatar(
                          backgroundColor:
                              AppColors.surface.withValues(alpha: 0.85),
                          child: IconButton(
                            icon: Icon(
                              (widget.onToggleFavorite != null ? widget.service.isFavorite : _isFavorite)
                                  ? Icons.favorite
                                  : Icons.favorite_border,
                              color: (widget.onToggleFavorite != null ? widget.service.isFavorite : _isFavorite)
                                  ? AppColors.primary
                                  : AppColors.onSurface,
                            ),
                            onPressed: widget.favoriteBusy ? null : () async {
                              if (widget.onToggleFavorite != null) {
                                await widget.onToggleFavorite!();
                              } else {
                                setState(() => _isFavorite = !_isFavorite);
                              }
                            },
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),

          // STICKY BOTTOM BAR (PRICE & BOOKING BUTTON)
          Positioned(
            bottom: 0,
            left: 0,
            right: 0,
            child: Container(
              padding: EdgeInsets.only(
                left: AppShapes.gutterMobile,
                right: AppShapes.gutterMobile,
                top: 12,
                bottom: MediaQuery.of(context).padding.bottom + 12,
              ),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                boxShadow: AppShapes.bottomNavShadow,
                border: Border(
                  top: BorderSide(
                    color: AppColors.borderSubtle.withValues(alpha: 0.8),
                    width: 0.5,
                  ),
                ),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        LocalizedText(
                          'Giá trọn gói',
                          style: AppTypography.bodySm(
                            color: AppColors.onSurfaceVariant,
                          ),
                        ),
                        RichText(
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          text: TextSpan(
                            children: [
                              TextSpan(
                                text: tr(context, _formatPrice(service.price)),
                                style: AppTypography.headlineMd(
                                  color: AppColors.primary,
                                ),
                              ),
                              TextSpan(
                                text: tr(context, ' / khách'),
                                style: AppTypography.bodySm(
                                  color: AppColors.onSurfaceVariant,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 8),
                  AppPillButton(
                    label: 'Chọn lịch & đặt',
                    variant: AppButtonVariant.primary,
                    trailingIcon: Icons.calendar_today,
                    onPressed: _openBookingSheet,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInclusionItem(IconData icon, String title, String subtitle) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(6),
            decoration: BoxDecoration(
              color: AppColors.secondaryContainer.withValues(alpha: 0.4),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(icon, size: 18, color: AppColors.secondary),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                LocalizedText(
                  title,
                  style: AppTypography.labelMd(
                    color: AppColors.onSurface,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                LocalizedText(
                  subtitle,
                  style: AppTypography.bodySm(
                    color: AppColors.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
