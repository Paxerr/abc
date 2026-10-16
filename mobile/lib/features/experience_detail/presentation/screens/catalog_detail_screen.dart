import '../../../../core/data/library_store.dart';
import 'package:flutter/material.dart';
import '../../../../core/data/catalog_repository.dart';
import '../../../../core/l10n/app_localizations.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';

/// Public catalog details. Booking is connected separately from catalog reads.
class CatalogDetailScreen extends StatefulWidget {
  const CatalogDetailScreen({
    super.key,
    required this.serviceId,
    this.repository,
    this.library,
  });
  final String serviceId;
  final CatalogRepository? repository;
  final LibraryStore? library;
  @override
  State<CatalogDetailScreen> createState() => _CatalogDetailScreenState();
}

class _CatalogDetailScreenState extends State<CatalogDetailScreen> {
  late final LibraryStore _library;
  ServiceModel? _service;
  String? _error;
  bool _loading = true;
  @override
  void initState() {
    super.initState();
    _library = widget.library ?? LibraryStore.instance;
    _library.addListener(_changed);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _library.loadWishlist();
    });
    _load();
  }

  void _changed() {
    if (mounted) setState(() {});
  }

  @override
  void dispose() {
    _library.removeListener(_changed);
    super.dispose();
  }

  Future<void> _toggle() async {
    final service = _service;
    if (service == null) return;
    try {
      await _library.toggle(
        SavedService(
          serviceId: service.id,
          name: service.name,
          imageUrl: service.imageUrls.isEmpty ? null : service.imageUrls.first,
        ),
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(e.toString())));
      }
    }
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final service = await (widget.repository ?? CatalogRepository()).detail(
        widget.serviceId,
      );
      if (mounted) {
        setState(() => _service = service);
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted) _library.loadRecent();
        });
      }
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  List<Widget> _section(String title, String value) => value.trim().isEmpty
      ? []
      : [
          const Divider(height: 32),
          LocalizedText(title, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8),
          Text(value),
        ];

  String _time(String value) =>
      value.length >= 5 ? value.substring(0, 5) : value;
  String _date(String value) {
    final date = DateTime.tryParse(value);
    return date == null
        ? value
        : '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}';
  }

  @override
  Widget build(BuildContext context) {
    final service = _service;
    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        title: const LocalizedText('Chi tiết trải nghiệm'),
        actions: [
          IconButton(
            tooltip: translate(
              _library.contains(widget.serviceId)
                  ? 'Bỏ yêu thích'
                  : 'Thêm yêu thích',
              AppLanguage.instance.code,
            ),
            onPressed:
                _service == null ||
                    _loading ||
                    _error != null ||
                    _library.busy(widget.serviceId)
                ? null
                : _toggle,
            icon: Icon(
              _library.contains(widget.serviceId)
                  ? Icons.favorite
                  : Icons.favorite_border,
            ),
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
          ? Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Padding(
                    padding: const EdgeInsets.all(24),
                    child: Text(_error!),
                  ),
                  TextButton(
                    onPressed: _load,
                    child: const LocalizedText('Thử lại'),
                  ),
                ],
              ),
            )
          : service == null
          ? const SizedBox.shrink()
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                physics: const AlwaysScrollableScrollPhysics(),
                children: [
                  if (service.imageUrls.isNotEmpty)
                    SizedBox(
                      height: 280,
                      child: PageView(
                        children: service.imageUrls
                            .map(
                              (url) => Image.network(
                                url,
                                fit: BoxFit.cover,
                                errorBuilder: (_, _, _) => const Icon(
                                  Icons.image_not_supported_outlined,
                                  size: 64,
                                ),
                              ),
                            )
                            .toList(),
                      ),
                    )
                  else
                    const SizedBox(
                      height: 180,
                      child: Icon(Icons.beach_access, size: 64),
                    ),
                  Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          service.name,
                          style: Theme.of(context).textTheme.headlineSmall,
                        ),
                        const SizedBox(height: 12),
                        if (service.categoryName.isNotEmpty)
                          Chip(label: Text(service.categoryName)),
                        if (service.address.isNotEmpty) Text(service.address),
                        const SizedBox(height: 12),
                        Text(
                          '${service.price} đ',
                          style: Theme.of(context).textTheme.titleLarge
                              ?.copyWith(color: AppColors.secondary),
                        ),
                        const SizedBox(height: 8),
                        Row(
                          children: [
                            const Icon(
                              Icons.star,
                              color: Colors.amber,
                              size: 20,
                            ),
                            Text(
                              ' ${service.avgRating.toStringAsFixed(1)} · ${service.ratingCount} ',
                            ),
                            const LocalizedText('đánh giá'),
                          ],
                        ),
                        const Divider(height: 32),
                        const LocalizedText('Giới thiệu'),
                        const SizedBox(height: 8),
                        Text(service.description),
                        ..._section('Nhà cung cấp', service.vendorName),
                        if (service.vendorName.isNotEmpty &&
                            service.vendorBadge != VendorBadgeTier.none)
                          LocalizedText(
                            service.vendorBadge == VendorBadgeTier.topRated
                                ? 'Được đánh giá cao'
                                : 'Đã xác minh',
                          ),
                        if (service.durationMinutes > 0)
                          ..._section(
                            'Thời lượng',
                            tr(context, '${service.durationMinutes} phút'),
                          ),
                        if (service.capacityPerSlot > 0)
                          ..._section(
                            'Sức chứa',
                            tr(context, '${service.capacityPerSlot} khách'),
                          ),
                        ..._section(
                          'Điều kiện tham gia',
                          service.participantConditions,
                        ),
                        if (service.catalogSlots.isNotEmpty) ...[
                          const Divider(height: 32),
                          LocalizedText(
                            'Lịch trải nghiệm',
                            style: Theme.of(context).textTheme.titleMedium,
                          ),
                          for (final slot in service.catalogSlots)
                            Padding(
                              padding: const EdgeInsets.symmetric(vertical: 10),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    '${_date(slot.date)} · ${_time(slot.startTime)} – ${_time(slot.endTime)}',
                                  ),
                                  if (slot.availableCapacity != null)
                                    Text(
                                      tr(
                                        context,
                                        'Còn ${slot.availableCapacity} chỗ',
                                      ),
                                    ),
                                  if (slot.price != null)
                                    Text(
                                      '${slot.price!.round()} đ${slot.pricingUnit == 'PER_PERSON'
                                          ? tr(context, '/người')
                                          : slot.pricingUnit == 'PER_PACKAGE'
                                          ? tr(context, '/gói')
                                          : ''}',
                                    ),
                                  LocalizedText(
                                    slot.bookable
                                        ? 'Có thể đặt'
                                        : 'Không thể đặt',
                                  ),
                                ],
                              ),
                            ),
                        ] else if (service.availableSlots.isNotEmpty)
                          ..._section(
                            'Lịch trải nghiệm',
                            service.availableSlots.join('\n'),
                          ),
                        ..._section(
                          'Chính sách hoàn tiền',
                          service.refundPolicy,
                        ),
                        ..._section(
                          'Chính sách hủy',
                          service.cancellationPolicy,
                        ),
                        ..._section('Quy định an toàn', service.safetyRules),
                        if (service.waiverRequired == true)
                          ..._section(
                            'Cam kết tham gia',
                            tr(
                              context,
                              'Cần xác nhận cam kết trước khi tham gia.',
                            ),
                          ),
                        ..._section('Nội dung cam kết', service.waiverContent),
                        const SizedBox(height: 24),
                        const LocalizedText('Đặt chỗ hiện chưa khả dụng.'),
                      ],
                    ),
                  ),
                ],
              ),
            ),
    );
  }
}
