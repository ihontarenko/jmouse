---
name: commit-message
description: Compose friendly commit messages
---

When writing a commit message:

1. Run `git diff main...HEAD` to see all changes on this branch
2. Add short prefix of project to commit message. e.g. [FE], [UI], [BE]  
3. Add second short prefix that will indicate kind of changes. e.g. [FIX], [BUG], [FEATURE], etc.  
4. Take main changes following this format:

## What
One sentence explaining what this commit does.

## Changes
- List any new files or deleted