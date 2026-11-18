#!/bin/zsh
# 빌드 후 백그라운드 실행 (터미널을 닫아도 계속 동작). 로그: logs/out.log
cd "$(dirname "$0")"
[ -f gooday.pid ] && kill -0 $(cat gooday.pid) 2>/dev/null && { echo "이미 실행 중 (pid $(cat gooday.pid))"; exit 0; }
./mvnw -q -DskipTests package || exit 1
mkdir -p logs
nohup java -jar target/gooday-0.0.1-SNAPSHOT.jar > logs/out.log 2>&1 &
echo $! > gooday.pid
echo "Gooday 시작 (pid $!) → http://localhost:8100  /  폰: http://$(ipconfig getifaddr en0):8100"
