#!/usr/bin/env bash
# Chạy unit test thuần (không SpringBootTest), in tổng kết từ surefire
cd "D:/2026/HK1_Nam4/LuanAn/SourceCode/WebsiteCungCapDichVuGiupViec"
L=.superpowers/sdd/2026-10-16-dat-dich-vu-thanh-toan
./mvnw test -Dtest="${1:-com.example.Service.*Test}" > $L/last.log 2>&1; rc=$?
grep -E "Tests run:.*in com|^\[ERROR\].*(FAIL|ERROR|cannot find|expected)" $L/last.log | sed 's/, Time elapsed[^-]*//' | head -30
grep -E "^\[(INFO|WARNING|ERROR)\] Tests run: [0-9]+, Failures" $L/last.log | tail -1
echo "EXIT $rc"
