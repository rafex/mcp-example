#!/usr/bin/env bash

set -euo pipefail

cd "$(dirname "$0")/.."

PATH="$HOME/.local/bin:$PATH" just run-mcp-client openweather python --catalog-only
