# Dữ liệu khởi tạo cho MySQL trong Docker

Mọi file `.sql` đặt ở đây được MySQL chạy **một lần**, khi volume `mysql-data` còn trống
(theo thứ tự tên file). Ứng dụng dùng `ddl-auto=validate` nên CSDL phải có sẵn đủ bảng.

Xuất CSDL đang dùng (container `mysql` hiện tại) thành file khởi tạo:

    docker exec mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 --single-transaction --set-gtid-purged=OFF --routines --triggers --databases GiupViecDB' > database/init/01-GiupViecDB.sql

Muốn nạp lại từ đầu: `docker compose down -v` (xóa volume, mất dữ liệu trong container) rồi `docker compose up -d`.
File `.sql` ở đây bị .gitignore vì chứa dữ liệu khách hàng.
