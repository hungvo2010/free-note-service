# free-note-service

## Commits

Keep every commit atomic: one concern per commit, and the tree must build at every commit.

- Think about atomicity while making the change, not when committing. Group edits as you go so each group can be committed on its own.
- Never lump unrelated changes into one commit, even when they touch the same file. Stage them separately.
- Order commits so each one compiles by itself. A change that removes a method belongs with the change that removes its last caller.
- Never add a `Co-Authored-By` trailer.
