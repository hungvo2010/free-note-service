#!/bin/bash
# Description: Batch replace 'OLD_VALUE' with 'NEW_VALUE' in the entire project.
# Usage: ./batch_replace.sh OLD_VALUE NEW_VALUE

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 OLD_VALUE NEW_VALUE"
    exit 1
fi

OLD_VALUE=$1
NEW_VALUE=$2

echo "Replacing '$OLD_VALUE' with '$NEW_VALUE' in all files..."
ambr --no-interactive "$OLD_VALUE" "$NEW_VALUE" .
echo "Done."
