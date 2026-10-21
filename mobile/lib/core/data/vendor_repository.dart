import '../auth/auth_session.dart';
import 'catalog_repository.dart';

class PublicVendor {
  final String id, businessName, address, badgeTier;
  final double ratingAvg;
  final int ratingCount, activeServicesCount;

  PublicVendor.fromJson(Map<String, dynamic> json)
    : id = json['id'] as String,
      businessName = json['businessName'] as String,
      address = json['address'] as String? ?? '',
      badgeTier = json['badgeTier'] as String? ?? 'NONE',
      ratingAvg = (json['ratingAvg'] as num?)?.toDouble() ?? 0,
      ratingCount = (json['ratingCount'] as num?)?.toInt() ?? 0,
      activeServicesCount = (json['activeServicesCount'] as num?)?.toInt() ?? 0;
}

class VendorRepository {
  VendorRepository({AuthSession? session})
    : session = session ?? AuthSession.instance;
  final AuthSession session;

  Future<PublicVendor> profile(String id) async {
    final json = await session.requestJson(
      '/api/vendors/${Uri.encodeComponent(id)}',
      authenticated: false,
    );
    if (json is! Map<String, dynamic> ||
        json['id'] is! String ||
        json['businessName'] is! String) {
      throw AuthFailure('Dữ liệu máy chủ không hợp lệ.');
    }
    return PublicVendor.fromJson(json);
  }

  Future<CatalogPage> services(String id, {int page = 0}) async {
    final json = await session.requestJson(
      '/api/vendors/${Uri.encodeComponent(id)}/services?page=$page&size=20',
      authenticated: false,
    );
    if (json is! Map<String, dynamic> ||
        json['content'] is! List ||
        json['page'] is! num ||
        json['totalElements'] is! num) {
      throw AuthFailure('Dữ liệu máy chủ không hợp lệ.');
    }
    return CatalogPage(
      (json['content'] as List)
          .map(
            (item) => CatalogRepository.serviceFromJson({
              ...item as Map<String, dynamic>,
              'vendorId': id,
            }),
          )
          .toList(),
      (json['page'] as num).toInt(),
      (json['totalElements'] as num).toInt(),
    );
  }
}
