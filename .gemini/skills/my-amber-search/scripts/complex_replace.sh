#!/bin/bash
# Description: Replace multi-line code blocks using search/replace files.
# Usage: ./complex_replace.sh search_file.txt replace_file.txt

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 search_file.txt replace_file.txt"
    exit 1
fi

SEARCH_FILE=$1
REPLACE_FILE=$2

echo "Performing complex replacement using files..."
ambr --key-from-file "$SEARCH_FILE" --rep-from-file "$REPLACE_FILE" .
echo "Done."
