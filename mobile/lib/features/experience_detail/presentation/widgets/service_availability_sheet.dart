import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../../core/l10n/app_localizations.dart';
import '../../../../core/models/danasea_models.dart';

/// Read-only availability. Booking submission is a separate integration.
class ServiceAvailabilitySheet extends StatefulWidget {
  const ServiceAvailabilitySheet({super.key, required this.service});
  final ServiceModel service;

  @override
  State<ServiceAvailabilitySheet> createState() =>
      _ServiceAvailabilitySheetState();
}

class _ServiceAvailabilitySheetState extends State<ServiceAvailabilitySheet> {
  String? _optionId;

  @override
  void initState() {
    super.initState();
    _optionId = widget.service.options.firstOrNull?.id;
  }

  @override
  Widget build(BuildContext context) {
    final slots = widget.service.catalogSlots
        .where((slot) => _optionId == null || slot.optionId == _optionId)
        .toList();
    return SafeArea(
      child: SizedBox(
        height: MediaQuery.sizeOf(context).height * 0.7,
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: LocalizedText(
                      'Lịch trải nghiệm',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ),
                  IconButton(
                    onPressed: () => Navigator.pop(context),
                    tooltip: MaterialLocalizations.of(
                      context,
                    ).closeButtonTooltip,
                    icon: const Icon(Icons.close),
                  ),
                ],
              ),
              Text(
                widget.service.name,
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const LocalizedText('Khung giờ trong 30 ngày tới'),
              if (widget.service.options.isNotEmpty)
                DropdownButton<String>(
                  isExpanded: true,
                  value: _optionId,
                  items: widget.service.options
                      .map(
                        (option) => DropdownMenuItem(
                          value: option.id,
                          child: Text(
                            option.name,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      )
                      .toList(),
                  onChanged: (id) => setState(() => _optionId = id),
                ),
              const SizedBox(height: 12),
              Expanded(
                child: slots.isEmpty
                    ? const Center(
                        child: LocalizedText(
                          'Chưa có khung giờ trong khoảng thời gian này.',
                        ),
                      )
                    : ListView.separated(
                        itemCount: slots.length,
                        separatorBuilder: (_, _) => const Divider(),
                        itemBuilder: (context, index) {
                          final slot = slots[index];
                          final date = DateTime.tryParse(slot.date);
                          final unit = switch (slot.pricingUnit) {
                            'PER_PACKAGE' => '/gói',
                            'PER_PERSON' => '/người',
                            _ => '',
                          };
                          return ListTile(
                            contentPadding: EdgeInsets.zero,
                            leading: Icon(
                              slot.bookable
                                  ? Icons.event_available
                                  : Icons.event_busy,
                            ),
                            title: Text(
                              date == null
                                  ? slot.date
                                  : DateFormat('dd/MM/yyyy').format(date),
                            ),
                            subtitle: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text('${slot.startTime} – ${slot.endTime}'),
                                LocalizedText(
                                  slot.bookable
                                      ? 'Có thể đặt'
                                      : 'Không thể đặt',
                                ),
                                LocalizedText(
                                  'Còn lại: ${slot.availableCapacity ?? 0}',
                                ),
                                if (slot.price != null)
                                  Text(
                                    '${NumberFormat.decimalPattern('vi').format(slot.price)} ₫${tr(context, unit)}',
                                  ),
                              ],
                            ),
                          );
                        },
                      ),
              ),
              const SizedBox(height: 12),
              const LocalizedText('Đặt chỗ hiện chưa khả dụng.'),
            ],
          ),
        ),
      ),
    );
  }
}
