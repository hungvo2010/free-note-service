---
name: my-amber-search
description: Use ambs /Amber for code search and replace tool with most efficient search capabilities.
---

# Instructions for Using Amber Search

When searching the codebase, prioritize using the `ambs` tool for speed.

### Basic Usage
- To find definitions: `ambs "class MyClass" .`
- To find usage: `ambs "myFunctionName" .`
  - For regex: `ambs -r "regex_pattern" .`

### Scripting and Automation
- **Batch Replacement:** Use `ambr --no-interactive "OLD" "NEW" .` to replace without prompting.
- **Complex Strings:** Use `--key-from-file` and `--rep-from-file` for multi-line content.
- **Data Extraction:** Use `--no-color` when piping to other tools like `grep` or `sed`.