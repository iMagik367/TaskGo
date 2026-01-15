#!/bin/bash

# Script para instalar a extensão Trigger Email from Firestore
# Uso: ./install-email-extension.sh

PROJECT_ID="${1:-task-go-ee85f}"
EXTENSION_ID="firebase/firestore-send-email"
LOCATION=""
UNINSTALL_ONLY=false
CHECK_ONLY=false

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Função para exibir mensagens
info() {
    echo -e "${CYAN}$1${NC}"
}

success() {
    echo -e "${GREEN}$1${NC}"
}

warning() {
    echo -e "${YELLOW}$1${NC}"
}

error() {
    echo -e "${RED}$1${NC}"
}

# Verificar argumentos
while [[ $# -gt 0 ]]; do
    case $1 in
        --location|-l)
            LOCATION="$2"
            shift 2
            ;;
        --uninstall-only|-u)
            UNINSTALL_ONLY=true
            shift
            ;;
        --check-only|-c)
            CHECK_ONLY=true
            shift
            ;;
        --help|-h)
            echo "Uso: $0 [PROJECT_ID] [OPÇÕES]"
            echo ""
            echo "Opções:"
            echo "  --location, -l LOCATION    Especificar região (ex: us-central1)"
            echo "  --uninstall-only, -u       Apenas desinstalar extensões existentes"
            echo "  --check-only, -c           Apenas verificar configuração"
            echo "  --help, -h                 Mostrar esta ajuda"
            exit 0
            ;;
        *)
            PROJECT_ID="$1"
            shift
            ;;
    esac
done

echo "========================================"
info "  Instalação da Extensão de Email"
info "  Trigger Email from Firestore"
echo "========================================"
echo ""

# Verificar se Firebase CLI está instalado
if ! command -v firebase &> /dev/null; then
    error "❌ Firebase CLI não encontrado. Instale com: npm install -g firebase-tools"
    exit 1
else
    FIREBASE_VERSION=$(firebase --version 2>&1 | head -n 1)
    success "✅ Firebase CLI encontrado: $FIREBASE_VERSION"
fi

# Verificar se gcloud CLI está instalado
if ! command -v gcloud &> /dev/null; then
    error "❌ gcloud CLI não encontrado. Instale do site oficial do Google Cloud."
    exit 1
else
    GCLOUD_VERSION=$(gcloud --version 2>&1 | head -n 1)
    success "✅ gcloud CLI encontrado: $GCLOUD_VERSION"
fi

echo ""

# Passo 1: Verificar região do Firestore
info "🔍 Passo 1: Verificando região do Firestore..."
echo ""

DATABASES_JSON=$(gcloud firestore databases list --project="$PROJECT_ID" --format=json 2>&1)

if [ $? -ne 0 ]; then
    error "❌ Erro ao listar bancos de dados Firestore"
    exit 1
fi

# Verificar se jq está instalado para parse JSON
if command -v jq &> /dev/null; then
    DB_COUNT=$(echo "$DATABASES_JSON" | jq '. | length')
    
    if [ "$DB_COUNT" -eq 0 ]; then
        error "❌ Nenhum banco de dados Firestore encontrado!"
        exit 1
    fi
    
    DEFAULT_DB=$(echo "$DATABASES_JSON" | jq -r '.[] | select(.name == "(default)")')
    
    if [ -z "$DEFAULT_DB" ]; then
        error "❌ Banco de dados '(default)' não encontrado!"
        warning "Bancos encontrados:"
        echo "$DATABASES_JSON" | jq -r '.[] | "  - \(.name) em \(.locationId)"'
        exit 1
    fi
    
    FIRESTORE_LOCATION=$(echo "$DATABASES_JSON" | jq -r '.[] | select(.name == "(default)") | .locationId')
    success "✅ Firestore encontrado na região: $FIRESTORE_LOCATION"
    
    # Se Location não foi especificada, usar a do Firestore
    if [ -z "$LOCATION" ]; then
        LOCATION="$FIRESTORE_LOCATION"
        warning "📍 Usando região do Firestore: $LOCATION"
    else
        if [ "$LOCATION" != "$FIRESTORE_LOCATION" ]; then
            warning "⚠️ AVISO: Região especificada ($LOCATION) difere da região do Firestore ($FIRESTORE_LOCATION)"
            warning "   Isso pode causar problemas. Recomendado usar: $FIRESTORE_LOCATION"
            read -p "   Continuar mesmo assim? (s/N) " confirm
            if [[ ! "$confirm" =~ ^[Ss]$ ]]; then
                error "❌ Operação cancelada."
                exit 0
            fi
        fi
    fi
else
    # Fallback sem jq - tentar extrair manualmente
    warning "⚠️ jq não encontrado. Tentando extrair região manualmente..."
    FIRESTORE_LOCATION=$(echo "$DATABASES_JSON" | grep -oP 'locationId["\s:]+"\K[^"]+' | head -n 1)
    
    if [ -z "$FIRESTORE_LOCATION" ]; then
        error "❌ Não foi possível determinar a região do Firestore"
        warning "Instale jq para melhor suporte: sudo apt-get install jq (Linux) ou brew install jq (Mac)"
        warning "Ou verifique manualmente: gcloud firestore databases list --project=$PROJECT_ID"
        exit 1
    fi
    
    success "✅ Firestore encontrado na região: $FIRESTORE_LOCATION"
    
    if [ -z "$LOCATION" ]; then
        LOCATION="$FIRESTORE_LOCATION"
    fi
fi

if [ "$CHECK_ONLY" = true ]; then
    echo ""
    success "✅ Verificação concluída. Região: $LOCATION"
    exit 0
fi

echo ""

# Passo 2: Verificar extensões instaladas
info "🔍 Passo 2: Verificando extensões instaladas..."
echo ""

EXTENSIONS_JSON=$(firebase ext:list --project="$PROJECT_ID" --json 2>&1)

if [ $? -eq 0 ] && [ -n "$EXTENSIONS_JSON" ]; then
    if command -v jq &> /dev/null; then
        EMAIL_EXTENSIONS=$(echo "$EXTENSIONS_JSON" | jq -r '.result[]? | select(.ref | contains("firestore-send-email")) | .instanceId')
        
        if [ -n "$EMAIL_EXTENSIONS" ]; then
            warning "Extensões de email encontradas:"
            echo "$EMAIL_EXTENSIONS" | while read -r instance_id; do
                warning "  - $instance_id"
            done
            
            if [ "$UNINSTALL_ONLY" = false ]; then
                echo ""
                warning "⚠️ Extensão de email já instalada!"
                read -p "Deseja desinstalar antes de reinstalar? (s/N) " action
                
                if [[ "$action" =~ ^[Ss]$ ]]; then
                    echo "$EMAIL_EXTENSIONS" | while read -r instance_id; do
                        warning "🗑️ Desinstalando: $instance_id..."
                        firebase ext:uninstall "$instance_id" --project="$PROJECT_ID" --force > /dev/null 2>&1
                        success "✅ Desinstalado: $instance_id"
                    done
                else
                    error "❌ Operação cancelada. Desinstale manualmente primeiro."
                    exit 0
                fi
            else
                echo "$EMAIL_EXTENSIONS" | while read -r instance_id; do
                    warning "🗑️ Desinstalando: $instance_id..."
                    firebase ext:uninstall "$instance_id" --project="$PROJECT_ID" --force > /dev/null 2>&1
                    success "✅ Desinstalado: $instance_id"
                done
                echo ""
                success "✅ Desinstalação concluída!"
                exit 0
            fi
        else
            success "✅ Nenhuma extensão de email encontrada."
        fi
    else
        warning "⚠️ jq não encontrado. Pulando verificação de extensões."
    fi
else
    success "✅ Nenhuma extensão instalada ou erro ao listar."
fi

if [ "$UNINSTALL_ONLY" = true ]; then
    exit 0
fi

echo ""

# Passo 3: Verificar APIs habilitadas
info "✅ Passo 3: Verificando APIs necessárias..."
echo ""

REQUIRED_APIS=(
    "cloudfunctions.googleapis.com"
    "firestore.googleapis.com"
    "cloudbuild.googleapis.com"
    "secretmanager.googleapis.com"
    "run.googleapis.com"
)

for api in "${REQUIRED_APIS[@]}"; do
    if gcloud services list --enabled --project="$PROJECT_ID" --filter="name:$api" --format="value(name)" 2>&1 | grep -q "$api"; then
        success "  ✅ $api"
    else
        warning "  ⚠️ Habilitando $api..."
        gcloud services enable "$api" --project="$PROJECT_ID" > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            success "  ✅ $api habilitada"
        else
            warning "  ⚠️ Erro ao habilitar $api"
        fi
    fi
done

echo ""

# Passo 4: Instalar extensão
info "📦 Passo 4: Instalando extensão..."
echo ""
warning "⚠️ IMPORTANTE: Você precisará fornecer os seguintes parâmetros durante a instalação:"
warning "   - SMTP Connection URI (ex: smtps://user:pass@smtp.example.com:465)"
warning "   - Default From Email"
warning "   - Default Reply To Email"
echo ""
info "📍 Região que será usada: $LOCATION"
echo ""

read -p "Continuar com a instalação? (s/N) " confirm
if [[ ! "$confirm" =~ ^[Ss]$ ]]; then
    error "❌ Operação cancelada."
    exit 0
fi

echo ""
success "Iniciando instalação interativa..."
echo ""

if firebase ext:install "$EXTENSION_ID" --project="$PROJECT_ID"; then
    echo ""
    success "✅ Instalação iniciada!"
    echo ""
    warning "⚠️ NOTA: A instalação pode levar alguns minutos para completar."
    warning "   Verifique o progresso no Firebase Console ou com:"
    info "   firebase ext:list --project=$PROJECT_ID"
else
    error "❌ Erro durante a instalação"
    echo ""
    warning "💡 Dicas:"
    warning "   1. Verifique se você tem permissões no projeto"
    warning "   2. Verifique se o billing está habilitado"
    warning "   3. Tente instalar manualmente: firebase ext:install $EXTENSION_ID --project=$PROJECT_ID"
    exit 1
fi

echo ""

# Passo 5: Verificar instalação
info "🔍 Passo 5: Verificando instalação..."
echo ""

sleep 5

if command -v jq &> /dev/null; then
    EXTENSIONS_JSON=$(firebase ext:list --project="$PROJECT_ID" --json 2>&1)
    
    if [ $? -eq 0 ]; then
        INSTALLED=$(echo "$EXTENSIONS_JSON" | jq -r '.result[]? | select(.ref | contains("firestore-send-email"))')
        
        if [ -n "$INSTALLED" ]; then
            success "✅ Extensão instalada:"
            echo "$INSTALLED" | jq -r '"   - \(.ref)\n     Estado: \(.state)"'
        else
            warning "⚠️ Extensão ainda não aparece na lista (pode estar sendo instalada)"
        fi
    fi
fi

echo ""
echo "========================================"
success "  Instalação Concluída!"
echo "========================================"
echo ""
warning "📝 Próximos passos:"
info "   1. Verifique o status: firebase ext:list --project=$PROJECT_ID"
info "   2. Verifique as Cloud Functions: gcloud functions list --project=$PROJECT_ID"
info "   3. Configure os parâmetros SMTP se necessário"
info "   4. Teste enviando um email através do Firestore"
echo ""

















