#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script para gerar documento completo com todo o código do TaskGo
Classifica Frontend vs Backend automaticamente
"""

import os
from pathlib import Path
from datetime import datetime

OUTPUT_FILE = "CODIGO_COMPLETO_TASKGO.md"
BASE_PATH = Path("app/src/main/java/com/taskgoapp/taskgo")

def classify_file(file_path):
    """Classifica se é Frontend ou Backend"""
    path_str = str(file_path)
    if "functions" in path_str:
        return "[BACKEND]"
    if "firestore.rules" in path_str or "firestore.indexes" in path_str:
        return "[BACKEND]"
    return "[FRONTEND]"

def get_section(file_path):
    """Obtém a seção do arquivo"""
    try:
        # Converter para Path se necessário
        if isinstance(file_path, str):
            file_path = Path(file_path)
        if isinstance(BASE_PATH, str):
            base = Path(BASE_PATH)
        else:
            base = BASE_PATH
        
        # Tentar obter caminho relativo
        try:
            relative_path = str(file_path.relative_to(base))
        except ValueError:
            # Se não conseguir, usar caminho absoluto e extrair a parte relevante
            file_str = str(file_path)
            base_str = str(base)
            if file_str.startswith(base_str):
                relative_path = file_str[len(base_str):].lstrip(os.sep)
            else:
                # Se não estiver no base, usar o nome do arquivo
                relative_path = file_path.name
        
        parts = relative_path.split(os.sep)
        
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
    except:
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
    files = sorted(BASE_PATH.rglob("*.kt"))
    print(f"Encontrados {len(files)} arquivos")
    
    # Agrupar por seção
    sections = {}
    for file in files:
        try:
            section = get_section(file)
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
                classification = classify_file(file)
                relative_path = str(file.relative_to(Path.cwd())).replace("\\", "/")
                
                print(f"  Processando: {relative_path}")
                
                # Título do arquivo - usar caminho relativo a partir de BASE_PATH
                try:
                    relative_path = str(file.relative_to(BASE_PATH)).replace("\\", "/")
                except:
                    relative_path = str(file).replace("\\", "/")
                
                # Título do arquivo
                with open(OUTPUT_FILE, 'a', encoding='utf-8') as f:
                    f.write(f"\n## {classification}: {relative_path}\n\n")
                    f.write("```kotlin\n")
                
                # Conteúdo do arquivo
                try:
                    with open(file, 'r', encoding='utf-8') as src:
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
