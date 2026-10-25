# Clean Architecture

modules/<module_name>/
├── domain/                  # Lõi nghiệp vụ độc lập framework
│   ├── models/              # Entities, Value Objects, Domain Enums
│   └── services/            # Domain Services nội bộ
├── application/             # Điều phối Use Cases & định nghĩa Ports
│   ├── usecases/            # Các Use Case thực thi luồng nghiệp vụ
│   ├── ports/               # Output Ports (Interfaces tương tác DB, external APIs)
│   ├── dtos/                # Data Transfer Objects
│   └── api/                 # ĐÂY LÀ INTERNAL MODULE API, KHÔNG PHẢI REST API
├── infrastructure/          # Chi tiết công nghệ, external tools, DB
│   ├── persistence/         # JPA Entities, Repositories, Database Adapters
│   ├── redis/               # Redis Adapters, Distributed Cache
│   └── <external_service>/  # Client gọi API bên ngoài (CLI Proxy, OpenMeteo,...)
└── presentation/            # Giao tiếp với thế giới bên ngoài (Inbound Adapters)
    ├── controllers/         # NƠI CHỨA TẤT CẢ @RestController HTTP ENDPOINTS
    └── dtos/                # Request/Response body từ client gửi lên
