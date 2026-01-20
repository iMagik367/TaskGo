#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script para gerar documento completo com todo o código do TaskGo
"""

import os
from pathlib import Path
from datetime import datetime

OUTPUT_FILE = "CODIGO_COMPLETO_TASKGO.md"
BASE_PATH = Path("app/src/main/java/com/taskgoapp/taskgo").resolve()

def classify_file(file_path_str):
    """Classifica se é Frontend ou Backend"""
    if "functions" in file_path_str:
        return "[BACKEND]"
    if "firestore.rules" in file_path_str or "firestore.indexes" in file_path_str:
        return "[BACKEND]"
    return "[FRONTEND]"

def get_section(file_path_str):
    """Obtém a seção do arquivo"""
    # Normalizar separadores
    normalized = file_path_str.replace("\\", "/")
    
    # Encontrar a parte após BASE_PATH
    base_str = str(BASE_PATH).replace("\\", "/")
    if base_str in normalized:
        idx = normalized.index(base_str) + len(base_str)
        relative = normalized[idx:].lstrip("/")
    else:
        # Se não encontrar, usar o nome do arquivo
        relative = os.path.basename(normalized)
    
    parts = relative.split("/")
    
    if len(parts) > 0:
        if parts[0] == "feature":
            if len(parts) > 1:
                return f"Features - {parts[1]}"
            return "Features"
        if parts[0] == "core":
            if len(parts) > 1:
                return f"Core - {parts[1]}"
            return "Core"
        if parts[0] == "data":
            if len(parts) > 1:
                return f"Data Layer - {parts[1]}"
            return "Data Layer"
        if parts[0] == "di":
            return "Dependency Injection"
        if parts[0] == "domain":
            return "Domain Layer"
    
    return "Outros"

def main():
    # Cabeçalho
    date_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    header = f"""# Código Completo - TaskGo App
## Análise Completa de Todo o Código

**Data de Criação**: {date_str}
**Versão**: 1.0.97
**Status**: Código Completo para Análise

---

# CLASSIFICAÇÃO: FRONTEND vs BACKEND

## [FRONTEND] (Android App)
- Todas as Features (Telas, ViewModels, Composables)
- Data Layer (Repositórios, Models)
- Core (Location, Firebase Helpers, Utils)
- Dependency Injection (Hilt Modules)
- Domain Layer (Interfaces, Use Cases)

## [BACKEND] (Firebase)
- Cloud Functions (functions/)
- Firestore Security Rules (firestore.rules)
- Firestore Indexes (firestore.indexes.json)

---

"""
    
    # Escrever cabeçalho
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        f.write(header)
    
    # Buscar arquivos
    print("Buscando arquivos...")
    if not BASE_PATH.exists():
        print(f"ERRO: Caminho base não existe: {BASE_PATH}")
        return
    
    files = sorted(BASE_PATH.rglob("*.kt"))
    print(f"Encontrados {len(files)} arquivos")
    
    # Agrupar por seção
    sections = {}
    for file in files:
        try:
            file_str = str(file)
            section = get_section(file_str)
            if section not in sections:
                sections[section] = []
            sections[section].append(file)
        except Exception as e:
            print(f"Erro ao processar arquivo: {file} - {e}")
    
    print(f"Processando {len(sections)} seções...")
    
    total_processed = 0
    total_errors = 0
    
    # Processar cada seção
    for section_name in sorted(sections.keys()):
        section_files = sections[section_name]
        
        print(f"\nSeção: {section_name} ({len(section_files)} arquivos)")
        
        # Título da seção
        with open(OUTPUT_FILE, 'a', encoding='utf-8') as f:
            f.write(f"\n# {section_name}\n\n")
        
        # Processar cada arquivo
        for file in section_files:
            try:
                file_str = str(file)
                classification = classify_file(file_str)
                
                # Obter caminho relativo
                try:
                    relative_path = str(file.relative_to(BASE_PATH)).replace("\\", "/")
                except:
                    # Fallback: usar caminho completo
                    base_str = str(BASE_PATH).replace("\\", "/")
                    file_str_norm = file_str.replace("\\", "/")
                    if base_str in file_str_norm:
                        idx = file_str_norm.index(base_str) + len(base_str)
                        relative_path = file_str_norm[idx:].lstrip("/")
                    else:
                        relative_path = os.path.basename(file_str)
                
                print(f"  Processando: {relative_path}")
                
                # Título do arquivo
                with open(OUTPUT_FILE, 'a', encoding='utf-8') as f:
                    f.write(f"\n## {classification}: {relative_path}\n\n")
                    f.write("```kotlin\n")
                
                # Conteúdo do arquivo
                try:
                    with open(file, 'r', encoding='utf-8', errors='ignore') as src:
                        content = src.read()
                    with open(OUTPUT_FILE, 'a', encoding='utf-8') as f:
                        f.write(content)
                except Exception as e:
                    error_msg = f"// ERRO AO LER ARQUIVO: {e}\n"
                    with open(OUTPUT_FILE, 'a', encoding='utf-8') as f:
                        f.write(error_msg)
                    total_errors += 1
                
                # Fechar bloco de código
                with open(OUTPUT_FILE, 'a', encoding='utf-8') as f:
                    f.write("\n```\n")
                
                total_processed += 1
                
                if total_processed % 10 == 0:
                    print(f"  Progresso: {total_processed}/{len(files)} arquivos processados")
                    
            except Exception as e:
                print(f"  ERRO ao processar arquivo: {file} - {e}")
                total_errors += 1
    
    print("\n" + "=" * 40)
    print("Documento gerado com sucesso!")
    print(f"Arquivo: {OUTPUT_FILE}")
    print(f"Total de arquivos processados: {total_processed}")
    print(f"Total de erros: {total_errors}")
    print("=" * 40)

if __name__ == "__main__":
    main()
