#!/bin/zsh
cd "$(dirname "$0")"
if [ -f gooday.pid ] && kill $(cat gooday.pid) 2>/dev/null; then echo "Gooday 종료"; else echo "실행 중이 아님"; fi
rm -f gooday.pid
