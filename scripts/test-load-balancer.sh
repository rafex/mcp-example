#!/bin/bash

# Script para probar el balanceo de carga Round Robin
# entre backends Python y Java

set -euo pipefail

echo "=== Probando Balanceo de Carga Round Robin ==="
echo ""

# Test 1: Probar balanceo con 10 peticiones
echo "1. Probando distribución de 10 peticiones a /hello"
echo "---------------------------------------------------"
python3 - <<'PY'
import subprocess
import re

results = []
for i in range(1, 11):
    result = subprocess.run(
        ["curl", "-s", "-D", "-", f"http://localhost:8085/hello?name=User{i}", "-o", "/dev/null"],
        capture_output=True, text=True
    )
    # Combinar stdout y stderr ya que curl puede enviar headers a stderr o stdout
    output = result.stdout + result.stderr
    # Buscar header X-Powered-By (insensible a mayúsculas/minúsculas)
    match = re.search(r"x-powered-by:\s*(\w+)", output, re.IGNORECASE)
    if match:
        results.append(match.group(1).capitalize())

from collections import Counter
counts = Counter(results)
print(f"Python: {counts.get('Python', 0)} peticiones")
print(f"Java: {counts.get('Java', 0)} peticiones")
print(f"Total: {len(results)} peticiones")
PY

echo ""

# Test 2: Probar endpoints específicos
echo "2. Probando endpoints específicos"
echo "-----------------------------------"

echo "Hello API Python:"
curl -s -D - "http://localhost:8085/hello/python?name=Test" -o /dev/null 2>&1 | grep -i "x-powered-by"

echo "Hello API Java:"
curl -s -D - "http://localhost:8085/hello/java?name=Test" -o /dev/null 2>&1 | grep -i "x-powered-by"

echo ""

# Test 3: Probar Date API
echo "3. Probando Date API"
echo "---------------------"

echo "Date API balanceada:"
curl -s -D - -H "Authorization: Bearer dev-date-token" -H "X-Date-Client: mcp-date-client" \
  "http://localhost:8085/date/time?location=mx-central" -o /dev/null 2>&1 | grep -i "x-powered-by"

echo "Date API Python:"
curl -s -D - -H "Authorization: Bearer dev-date-token" -H "X-Date-Client: mcp-date-client" \
  "http://localhost:8085/date/python/time?location=us" -o /dev/null 2>&1 | grep -i "x-powered-by"

echo "Date API Java:"
curl -s -D - -H "Authorization: Bearer dev-date-token" -H "X-Date-Client: mcp-date-client" \
  "http://localhost:8085/date/java/time?location=es" -o /dev/null 2>&1 | grep -i "x-powered-by"

echo ""

# Test 4: Ver estado del balanceador
echo "4. Estado del balanceador"
echo "-------------------------"
curl -s http://localhost:8085/status | python3 -m json.tool

echo ""
echo "=== Prueba completada ==="
