#!/bin/bash
# Description: Find files containing a keyword and extract unique filenames.
# Usage: ./find_and_extract.sh KEYWORD

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 KEYWORD"
    exit 1
fi

KEYWORD=$1

echo "Searching for '$KEYWORD' and listing matching files:"
# --no-color is used for clean piping
# cut -d: -f1 gets the filename part of the output
ambs --no-color "$KEYWORD" . | cut -d: -f1 | sort -u
