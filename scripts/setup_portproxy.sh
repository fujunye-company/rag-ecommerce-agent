#!/bin/bash
# WSL → Windows 端口转发设置
# Android 模拟器用 10.0.2.2 访问 Windows 宿主机
# 本脚本将 Windows:8000 转发到 WSL:8000

WSL_IP=$(hostname -I | awk '{print $1}')
echo "WSL IP: $WSL_IP"
echo "Setting up port proxy: Windows:8000 → WSL:8000"
echo ""
echo "在 Windows PowerShell (管理员) 中运行:"
echo "  netsh interface portproxy add v4tov4 listenport=8000 listenaddress=0.0.0.0 connectport=8000 connectaddress=$WSL_IP"
echo ""
echo "验证:"
echo "  netsh interface portproxy show all"
echo ""
echo "删除 (如需):"
echo "  netsh interface portproxy delete v4tov4 listenport=8000 listenaddress=0.0.0.0"
