# Working notes

## How this user likes to be taught

- **Terse.** Long answers get skimmed and re-asked. When a one-line answer exists, lead with it
  and keep the surrounding detail optional. Evidence of this: a question was re-sent verbatim
  after being answered with six paragraphs — the answer was buried, not missing.
- **Facts over framing.** Wants specific file paths, line numbers, and verified behaviour. Has
  been visibly unimpressed by claims that turned out to be over-generalised.
- **Corrections land well.** When told something reported as a bug is actually deliberate, the
  response is a decision ("nio2 has same handler like nio, ssl only support legacy") rather than
  an argument. Record the decision, move on.
- **Prefers minimal diffs.** "just ioc and setter, no other changes" and "dont change any" —
  scope discipline is explicit and repeated.
- **Verification matters.** Compiles get run, not assumed. Distinguish "I read this" from
  "I ran this" — the distinction has been respected and should continue.

## Working context

- Deep familiarity with this codebase; doesn't need code explained that they wrote.
- Comfortable with the architecture vocabulary (module, interface, seam, adapter, depth,
  leverage, locality) after the architecture review — reusable in lessons.
- `CONTEXT.md` is the domain glossary (Draft, Shape, Action, Connection, Room, Participant,
  senderId) and is a glossary only — no implementation detail goes in it.
- `docs/adr/0001-in-memory-draft-store.md` exists; ADRs live in `docs/adr/`.

## Open threads (not this mission)

- Architecture candidates 3–8 from the review are still unexplored: frame reader duplication,
  Room membership, disk store interface, config readers, duplicate route registry.
- Uncommitted-ish: work sits on branch `refactor/transport-pairing-and-store-seam`. I have not
  been asked to commit the frame-reader work — do not assume it exists.
