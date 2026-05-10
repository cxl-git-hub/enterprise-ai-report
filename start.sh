#!/bin/bash
# ============================================================
# Enterprise AI Reporting Platform - 一键启动脚本
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_banner() {
    echo -e "${CYAN}"
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║     Enterprise AI Automated Reporting Platform          ║"
    echo "║     企业级AI自动化报表平台 - 一键启动                    ║"
    echo "╚══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

print_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[!]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

# 检查依赖
check_dependencies() {
    print_step "检查依赖..."

    if ! command -v docker &> /dev/null; then
        print_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    print_success "Docker $(docker --version | cut -d' ' -f3 | tr -d ',')"

    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose 未安装"
        exit 1
    fi
    print_success "Docker Compose 已安装"

    echo ""
}

# 检查端口
check_ports() {
    print_step "检查端口占用..."

    local ports=(3000 3306 6379 8080 8081 9000 9001)
    local occupied=()

    for port in "${ports[@]}"; do
        if lsof -i :"$port" &> /dev/null || ss -tlnp | grep -q ":$port " 2>/dev/null; then
            occupied+=("$port")
        fi
    done

    if [ ${#occupied[@]} -gt 0 ]; then
        print_warn "以下端口被占用: ${occupied[*]}"
        echo -n "是否继续? (y/N): "
        read -r response
        if [[ ! "$response" =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        print_success "所有端口可用"
    fi
    echo ""
}

# 配置环境变量
setup_env() {
    print_step "配置环境变量..."

    if [ ! -f "ai-service-python/.env" ]; then
        cp ai-service-python/.env.example ai-service-python/.env
        print_success "已创建 AI 服务 .env 文件"
    else
        print_warn "AI 服务 .env 文件已存在，跳过"
    fi

    # 检查 LLM_API_KEY
    if grep -q "sk-your-api-key" ai-service-python/.env 2>/dev/null; then
        echo ""
        print_warn "请配置 AI 模型 API Key:"
        echo -n "LLM_API_KEY (留空跳过): "
        read -r api_key
        if [ -n "$api_key" ]; then
            sed -i "s|LLM_API_KEY=sk-your-api-key|LLM_API_KEY=$api_key|" ai-service-python/.env
            print_success "API Key 已配置"
        fi
    fi
    echo ""
}

# 启动服务
start_services() {
    print_step "启动所有服务..."

    cd docker

    # 拉取镜像
    print_step "拉取基础镜像..."
    docker-compose pull 2>/dev/null || docker compose pull 2>/dev/null || true

    # 构建并启动
    print_step "构建并启动服务..."
    docker-compose up -d --build 2>/dev/null || docker compose up -d --build 2>/dev/null

    cd ..
    echo ""
}

# 等待服务就绪
wait_for_services() {
    print_step "等待服务启动..."

    # 等待 MySQL
    echo -n "  MySQL: "
    for i in $(seq 1 60); do
        if docker exec ai-report-mysql mysqladmin ping -h localhost --silent 2>/dev/null; then
            print_success "就绪"
            break
        fi
        if [ "$i" -eq 60 ]; then
            print_error "超时"
        fi
        sleep 2
    done

    # 等待 Redis
    echo -n "  Redis: "
    for i in $(seq 1 30); do
        if docker exec ai-report-redis redis-cli -a redis123456 ping 2>/dev/null | grep -q PONG; then
            print_success "就绪"
            break
        fi
        if [ "$i" -eq 30 ]; then
            print_error "超时"
        fi
        sleep 1
    done

    # 等待后端
    echo -n "  Backend: "
    for i in $(seq 1 60); do
        if curl -s http://localhost:8080/api/auth/login -o /dev/null -w "%{http_code}" 2>/dev/null | grep -q "40[0-9]"; then
            print_success "就绪"
            break
        fi
        if [ "$i" -eq 60 ]; then
            print_error "超时"
        fi
        sleep 2
    done

    # 等待 AI 服务
    echo -n "  AI Service: "
    for i in $(seq 1 30); do
        if curl -s http://localhost:8081/api/health -o /dev/null 2>/dev/null; then
            print_success "就绪"
            break
        fi
        if [ "$i" -eq 30 ]; then
            print_warn "未就绪(可稍后重试)"
        fi
        sleep 2
    done

    # 等待前端
    echo -n "  Frontend: "
    for i in $(seq 1 30); do
        if curl -s http://localhost:3000 -o /dev/null 2>/dev/null; then
            print_success "就绪"
            break
        fi
        if [ "$i" -eq 30 ]; then
            print_warn "未就绪(可稍后重试)"
        fi
        sleep 2
    done

    echo ""
}

# 显示访问信息
show_info() {
    echo -e "${GREEN}"
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║                    🎉 启动成功!                         ║"
    echo "╠══════════════════════════════════════════════════════════╣"
    echo "║                                                          ║"
    echo "║  访问地址:                                               ║"
    echo "║    🖥️  前端:       http://localhost:3000                  ║"
    echo "║    ☕ 后端API:    http://localhost:8080                   ║"
    echo "║    🐍 AI服务:     http://localhost:8081                   ║"
    echo "║    📦 MinIO:      http://localhost:9001                   ║"
    echo "║                                                          ║"
    echo "║  默认账号:                                               ║"
    echo "║    👤 用户名: admin                                      ║"
    echo "║    🔑 密码:   admin123                                   ║"
    echo "║                                                          ║"
    echo "╚══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# 主函数
main() {
    print_banner
    check_dependencies
    check_ports
    setup_env
    start_services
    wait_for_services
    show_info
}

# 处理参数
case "${1:-}" in
    stop)
        echo "停止所有服务..."
        cd docker && docker-compose down 2>/dev/null || docker compose down 2>/dev/null
        echo "已停止"
        ;;
    restart)
        echo "重启所有服务..."
        cd docker && docker-compose restart 2>/dev/null || docker compose restart 2>/dev/null
        echo "已重启"
        ;;
    logs)
        cd docker && docker-compose logs -f "${2:-}" 2>/dev/null || docker compose logs -f "${2:-}" 2>/dev/null
        ;;
    status)
        cd docker && docker-compose ps 2>/dev/null || docker compose ps 2>/dev/null
        ;;
    *)
        main
        ;;
esac
