---
title: 'Part 3 — Building with the Claude API'
summary: Under the agent is an engine — one HTTP endpoint, POST /v1/messages. Learn it from first principles: messages and roles, system prompts, structured output, tool use, streaming, caching, vision, and the token economics that decide your bill — then design where it would slot into a system that doesn't use it yet.
---

# Part 3 — Building with the Claude API

> **Everything you saw in Part 2 bottoms out here.** Claude Code, claude.ai, the agent that wrote
> this book — all of them are, underneath, programs making HTTP requests to a single endpoint:
> `POST /v1/messages`. This Part takes you under the hood. You'll learn the request/response shape,
> then the features layered onto that one endpoint — system prompts, structured output, tool use,
> streaming, caching, vision — and the token economics that turn "it works" into "it works at a
> price you can afford." This is the heart of the CCA exam's *Prompt Engineering & Structured
> Output* domain.

## A confession, and why it's the best teacher here

Here is a fact we keep coming back to: **the system serving these pages does not call the Claude API
at all.** Cortex runs your code in a sandbox (`go-judge`), stores content as Markdown, and serves it
with a ZIO server — but nowhere does it ask a language model anything. There is no AI tutor, no hint
generator, no "explain this error" button.

That is not an embarrassment; it's the **single most useful teaching setup in this whole book.** In
every other Part we read real code that *uses* the technology. Here, you get the rarer and more
valuable exercise: hold a real system in your hands that *doesn't* use the API, learn the API from
nothing, and then **design exactly where and how it would fit** — which endpoint, which model, what
prompt, what it would cost, what could go wrong. That is what an architect actually does. So this
Part ends (Chapter 10) by designing Cortex's missing AI tutor end-to-end. Everything before it is
the toolbox you'll need to do that well.

## A note on the runnable examples

The Claude API lives across the network, and our code sandbox has **no internet** (by design — it's
where untrusted code runs). So the "Build It" blocks here don't make real API calls. Instead, each
one **models the mechanics in plain Python** — the stateless request/response, the tool loop, the
cache-prefix rule, the token math — so you can *run* the logic and watch it behave. Alongside them,
plain (non-running) snippets show the **real SDK calls** you'd write against a live key. Learn the
shape from the model; copy the real call from the snippet.

## Chapters

1. **[The Messages API](/cortex/the-claude-stack/building-with-the-claude-api/the-messages-api)** —
   one endpoint, request and response, and the statelessness that shapes everything.
2. **[System prompts & prompting](/cortex/the-claude-stack/building-with-the-claude-api/system-prompts-and-prompting)** —
   steering the model: role, context, format, constraints, examples.
3. **[Multi-turn & context](/cortex/the-claude-stack/building-with-the-claude-api/multi-turn-and-context)** —
   conversations on a stateless API, the context window, and managing history.
4. **[Structured output](/cortex/the-claude-stack/building-with-the-claude-api/structured-output)** —
   getting JSON you can trust, not prose you have to parse.
5. **[Tool use](/cortex/the-claude-stack/building-with-the-claude-api/tool-use)** — letting the model
   call your functions: the tool loop, end to end.
6. **[Streaming](/cortex/the-claude-stack/building-with-the-claude-api/streaming)** — tokens as they're
   generated, and why long responses need it.
7. **[Prompt caching](/cortex/the-claude-stack/building-with-the-claude-api/prompt-caching)** — pay once
   for a big prefix, reuse it cheaply; the prefix-match rule.
8. **[Vision & multimodal](/cortex/the-claude-stack/building-with-the-claude-api/vision-and-multimodal)** —
   images and PDFs as input alongside text.
9. **[Tokens, limits & errors](/cortex/the-claude-stack/building-with-the-claude-api/tokens-limits-errors)** —
   the economics and failure modes: counting, pricing, rate limits, retries.
10. **[The Claude API in Cortex](/cortex/the-claude-stack/building-with-the-claude-api/claude-api-in-cortex)** —
    **GAP capstone:** design the AI tutor this system doesn't have yet.

---

**Begin:** strip the SDK away and look at the wire — what does one call to Claude actually consist
of, and why does "the API remembers nothing" shape every program you'll write against it? →
[1. The Messages API](/cortex/the-claude-stack/building-with-the-claude-api/the-messages-api)
