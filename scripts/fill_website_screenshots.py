#!/usr/bin/env bash
# Thin wrapper → geoking-tools/bin/fill_website_screenshots.py
export GK_SCRIPT=fill_website_screenshots.py
exec "$(cd "$(dirname "$0")" && pwd)/_geoking-wrapper.sh" "$@"
