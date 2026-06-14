---
title: 'Part 6 — Subagents & Orchestration'
summary: The top of the stack — one agent becomes many. Subagents give an agent two superpowers it can't have alone: context isolation and parallelism. Learn the tool, the patterns, and the verification discipline that turn a single agent into a coordinated system — taught through the very multi-agent loop that wrote this book.
---

# Part 6 — Subagents & Orchestration

> **A single agent has two hard limits: a finite context window, and one pair of hands.** It can hold
> only so much before the goal drowns in detail, and it does one thing at a time. **Subagents** lift
> both limits at once. A subagent is a *fresh agent* the parent spawns to do a sub-task in its **own
> clean context**, returning only its **result** — so the parent stays focused — and **many can run
> at once** — so the work goes wide instead of long. This Part is the summit of the whole stack, and
> the CCA exam's largest domain (Agentic Architecture & Orchestration, ~27%). It is also, fittingly,
> the most self-aware: **this book was written by exactly the multi-agent loop it now teaches.**

## The whole stack, pointed at one goal

Look back at the climb. Part 1 gave you *judgment* (the 4 D's). Part 2 put it to work in *Claude
Code*. Part 3 went under the hood to the *API*. Part 4 connected the model to the world with *MCP*.
Part 5 packaged expertise as *Skills*. Each rung made a *single* agent more capable. This Part is what
happens when one capable agent becomes **many**, coordinated — and every earlier rung shows up:
subagents are delegated to (Part 1), spawned by Claude Code (Part 2), powered by API calls (Part 3),
can wield MCP tools (Part 4) and load Skills (Part 5). Orchestration is where the stack composes into
a system.

## Taught by the thing itself

We've said "this book was built by subagents" several times; this is the Part where we open the hood
on that claim. Authoring fifty-one chapters inside one agent's context is impossible — by chapter
twenty the window is full of chapters one through nineteen and the agent loses the thread. So instead,
a **parent** wrote a tight spec per chapter and spawned a **subagent per chapter**; each child read
the exemplar and wrote *one* chapter in its *own* context, then returned a two-line summary; the
parent kept only the summaries — and then **independently re-verified every child's work** rather than
trusting its report. Context isolation made the book *possible*; parallelism made it *fast*; the
verification loop made it *correct*. Chapter 8 dissects that exact system as the capstone. You are
reading its output.

## Chapters

1. **[Why subagents](/cortex/the-claude-stack/subagents-and-orchestration/why-subagents)** — context
   isolation and parallelism: the two limits of a lone agent, lifted.
2. **[The Agent tool & types](/cortex/the-claude-stack/subagents-and-orchestration/the-agent-tool-and-types)** —
   how you spawn one, and the agent types (Explore, Plan, general-purpose, custom).
3. **[Prompting subagents well](/cortex/the-claude-stack/subagents-and-orchestration/prompting-subagents-well)** —
   the spec, the return contract, and what to delegate vs. keep.
4. **[Parallel fan-out](/cortex/the-claude-stack/subagents-and-orchestration/parallel-fan-out)** —
   running many at once (the nine agents that built a book).
5. **[Orchestration patterns](/cortex/the-claude-stack/subagents-and-orchestration/orchestration-patterns)** —
   pipeline, parallel, loop-until-dry, verify-each.
6. **[The Workflow tool](/cortex/the-claude-stack/subagents-and-orchestration/the-workflow-tool)** —
   deterministic, scripted orchestration, and when to reach for it.
7. **[Verification & adversarial review](/cortex/the-claude-stack/subagents-and-orchestration/verification-and-adversarial-review)** —
   never trust a self-report; judge panels and refutation.
8. **[Capstone — a multi-agent Cortex](/cortex/the-claude-stack/subagents-and-orchestration/capstone-multi-agent-cortex)** —
   design the system that auto-authors and verifies a chapter (this loop).

---

**Begin:** start with the *why* — what are the two walls a single agent hits, and how does spawning a
fresh one in its own context knock both down at once? →
[1. Why subagents](/cortex/the-claude-stack/subagents-and-orchestration/why-subagents)
