"""Build source-only PE submission ZIPs with the student's ID prefix."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

root = Path(__file__).resolve().parents[1]
out = root / 'dist'
out.mkdir(exist_ok=True)
for suite, label in [('DeThiSampleNew', 'Media'), ('DeThi_SU26', 'Hotel')]:
    folder = root / suite
    files = []
    for part in ['Code_PE', 'database', 'postman']:
        files.extend(p for p in (folder / part).rglob('*') if p.is_file()
            and not any(x in p.parts for x in ['target', '.idea', '.work'])
            and p.suffix not in ['.iml', '.log', '.class'])
    files += [root / 'REQUIREMENTS.md']
    destination = out / f'DE190468_{label}_Slot16.zip'
    with ZipFile(destination, 'w', ZIP_DEFLATED) as archive:
        for path in sorted(files):
            relative = path.relative_to(folder) if path.is_relative_to(folder) else Path(path.name)
            archive.write(path, str(Path('DE190468_' + label) / relative))
        services = [('UserService', 'user', 8081), ('PostService', 'post', 8082), ('MediaGateway', 'mediagateway', 8080)] if label == 'Media' else [('RoomService', 'room', 8081), ('ReservationService', 'reservation', 8082), ('HotelGateway', 'hotelgateway', 8080)]
        lines = [f'# DE190468 — {label} — Slot 16', '',
            'JDK 21; SQL Server; Spring Boot 4.1.0; Spring Cloud 2025.1.2.',
            'Cả hai service JPA đều dùng spring.jpa.hibernate.ddl-auto=none.', '',
            '1. Chạy database/create-database.sql bằng sqlcmd/SSMS trước khi mở app.',
            '   Chỉ CREATE TABLE khi các bảng chưa tồn tại. Sau đó chạy database/seed.sql.',
            '2. Mặc định SQL localhost:1433, sa/sa; đặt DB_USERNAME/DB_PASSWORD nếu máy khác.',
            '3. Mở ba pom.xml trong Code_PE bằng IntelliJ, dùng JDK 21.',
            '   Chạy mvn clean verify tại từng project, sau đó mở ba terminal riêng:', '']
        lines.extend(f'- Code_PE/DE190468{name}: java -jar target/{artifact}-1.0.0.jar (port {port})' for name, artifact, port in services)
        lines += ['', '4. Swagger gateway: http://localhost:8080/swagger-ui.html.',
            '   Import hai JSON trong postman/, chạy Collection Runner từ đầu đến cuối.',
            '5. Trước khi nộp: dừng ứng dụng, mvn clean tại mỗi project, zip toàn bộ folder.',
            'Chi tiết đặc tả và đối chiếu: REQUIREMENTS.md.', '']
        archive.writestr('DE190468_' + label + '/README.md', '\n'.join(lines))
    with ZipFile(destination) as archive:
        assert archive.testzip() is None
        assert not any('/target/' in name or '.work' in name for name in archive.namelist())
    print(destination.name, len(files), 'source files', destination.stat().st_size, 'bytes')
