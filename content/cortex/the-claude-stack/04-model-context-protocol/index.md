---
title: 'Part 4 — Model Context Protocol'
summary: How a model reaches the world without anyone writing a custom integration for every pair. MCP from first principles — the M×N problem, the host/client/server architecture, the three primitives (tools, resources, prompts), transports, building a server, and the two real MCP servers this project already runs.
---

# Part 4 — Model Context Protocol

> **In Part 3 you taught a model to call *your* functions. MCP is how a model calls *anyone's*.**
> The Model Context Protocol is an open standard — think "USB-C for AI" — that lets any AI
> application connect to any tool or data source through one common interface, instead of a bespoke
> integration per pairing. This Part builds MCP from the ground up: why it exists, the
> host/client/server architecture, the three things a server can offer (tools, resources, prompts),
> how the bytes actually move, and how to build one — grounded in the **two MCP servers this very
> repository already runs**. This is the CCA exam's *Tool Design & MCP Integration* domain.

## Why a whole protocol?

Part 3's tool use was powerful but *local*: you defined the tools, in your code, for your one
application. The moment you want the *same* database tool available in Claude Code **and** Claude
Desktop **and** your IDE **and** a teammate's script, you're writing the same integration four times.
And the moment a fifth app appears, or a fifth data source, the work multiplies. That combinatorial
explosion — **M** applications times **N** data sources — is exactly the mess MCP was created to end.
Build one MCP **server** for your data source, and *every* MCP-speaking app can use it, with no
custom glue. Chapter 1 makes this problem concrete; the rest of the Part shows the machinery that
solves it.

## The unfair advantage, again

We keep saying it because it keeps being true: **this project is already an MCP host.** Open
[`.claude/mcp.json`](.claude/mcp.json) and you'll find two MCP servers wired in — `codegraph` (a
code-intelligence graph) and `graphify` (an any-input-to-knowledge-graph tool) — both speaking MCP
over the **stdio** transport. The agent that wrote this book called their tools. So when we explain
the architecture, we point at the real config; when we explain stdio, we point at the real
`command`; when we explain discovery, we replay the real tool list. Chapter 8 dissects them line by
line. (And Chapter 11 confronts the gap: we *use* MCP servers, but we've never *built* one for
Cortex — so we design it.)

## Chapters

1. **[Why MCP exists](/cortex/the-claude-stack/model-context-protocol/why-mcp-exists)** — the M×N
   integration problem, and "USB-C for AI."
2. **[The architecture](/cortex/the-claude-stack/model-context-protocol/the-architecture)** — host,
   client, server, and the two layers (data + transport).
3. **[Tools](/cortex/the-claude-stack/model-context-protocol/tools)** — model-controlled actions
   (`tools/list`, `tools/call`).
4. **[Resources](/cortex/the-claude-stack/model-context-protocol/resources)** — application-controlled
   context data, addressed by URI.
5. **[Prompts](/cortex/the-claude-stack/model-context-protocol/prompts)** — user-controlled,
   reusable interaction templates.
6. **[Transports](/cortex/the-claude-stack/model-context-protocol/transports)** — stdio (local) vs
   Streamable HTTP (remote), over JSON-RPC 2.0.
7. **[Build a server](/cortex/the-claude-stack/model-context-protocol/build-a-server)** — a minimal
   MCP server, the shape of `codegraph` and `graphify`.
8. **[Our MCP servers](/cortex/the-claude-stack/model-context-protocol/our-mcp-servers)** — dissecting
   this repo's real `.claude/mcp.json`.
9. **[Advanced capabilities](/cortex/the-claude-stack/model-context-protocol/advanced-capabilities)** —
   sampling, elicitation, roots, and dynamic tool discovery.
10. **[Security & auth](/cortex/the-claude-stack/model-context-protocol/security-and-auth)** — trust
    boundaries, OAuth, and tool-borne prompt injection.
11. **[Design a Cortex MCP](/cortex/the-claude-stack/model-context-protocol/design-a-cortex-mcp)** —
    **GAP capstone:** a "cortex-content" server we don't have yet.

---

**Begin:** before the architecture, the *problem* — why couldn't we just keep writing one integration
at a time, and what does it cost when there are many apps and many tools? →
[1. Why MCP exists](/cortex/the-claude-stack/model-context-protocol/why-mcp-exists)
