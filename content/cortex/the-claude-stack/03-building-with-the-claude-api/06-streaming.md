---
title: '6. Streaming'
summary: A plain `messages.create()` makes you wait for the entire response before you see a single word; streaming delivers it token-by-token over Server-Sent Events as the model writes it — same content, very different feel — and for long outputs it's the difference between a responsive UI and a request that times out.
---

# 6. Streaming

## TL;DR

> By default `client.messages.create(...)` returns the **whole** response at once — you wait for *all*
> of it, then get one `Message`. **Streaming** instead delivers the answer **incrementally**, as the
> model generates it, over **Server-Sent Events (SSE)**. With the SDK helper:
> `with client.messages.stream(...) as stream: for text in stream.text_stream: print(text, end="")`,
> then `stream.get_final_message()` hands you the complete assembled `Message` anyway. Stream for two
> reasons: **UX** (show progress token-by-token instead of a long freeze) and **timeout-avoidance**
> (large `max_tokens` outputs — tens of thousands of tokens — can blow the non-streaming HTTP request's
> timeout). The crucial thing: streaming changes **delivery**, *not* content. The bytes are identical;
> only *when* you see them differs.

## 1. Motivation

Back in Chapter 1 we imagined Cortex's **"explain this error" button** — a learner runs a failing
program, and Claude writes a beginner-friendly explanation. In Chapter 10 we'll actually design that
AI tutor (it's the headline GAP: Cortex's code-runner is **go-judge**, not an LLM — the app makes *no*
Claude calls today). Suppose we've built it with what we know so far: one `messages.create()` call,
read the text out of `response.content`, render it.

It works. But watch a learner use it. They click "Explain." Then… nothing. The button spins. Two
seconds. Five. Ten. A long, frozen blank — because `messages.create()` doesn't return until the model
has finished writing *every* word. A three-paragraph explanation might take ten seconds to generate,
and the learner stares at a dead screen for the whole ten, with no idea whether it's working or hung.

Now recall how Cortex's **code runner already behaves**: when you run code, the execution output and
trace **stream** into the panel — you watch lines appear as the program prints them. That feels alive.
The AI explanation, sitting next to it, should feel the same: words appearing as Claude writes them,
not a ten-second freeze ending in a wall of text. That's **streaming**, and it's the subject of this
chapter. There's also a harder reason than feel — for *long* outputs, not streaming can make the
request **fail outright** — but we'll get to that in §6.

## 2. Intuition (Analogy)

Two ways to get a long answer from a friend who's far away.

**Option A — a letter.** You write your question, mail it. Your friend writes a full reply, mails it
back. You get the *whole* thing at once — but only once it's *completely* finished and the envelope
arrives. While they're still writing paragraph three, you have *nothing*. If it's a long letter, the
wait feels endless, and you can't even tell whether they've started.

**Option B — a phone call.** You ask your question and your friend starts *talking*. You hear each
word as it's spoken — "NameError… means… you used…". You don't wait for them to finish the whole
thought before the first words reach you. For a long answer, the call feels **responsive**: there's
always something happening, you're never staring at silence.

Non-streaming `messages.create()` is the **letter**: complete, but only at the very end. Streaming is
the **phone call**: each word as it's generated. Here's the part that trips people up — **the content
is the same either way.** The friend says the exact same sentences on the call as they'd write in the
letter. Streaming doesn't change *what* you get; it changes *when* the pieces arrive. And just like you
can jot down a call word-for-word and end up with the full transcript, streaming lets you *accumulate*
the pieces back into the complete message (that's `get_final_message()`).

| | **Non-streaming** (`messages.create`) — the letter | **Streaming** (`messages.stream`) — the phone call |
|---|---|---|
| When you see the first word | Only when the *whole* answer is done | As soon as the model writes it |
| What arrives | One complete `Message` | A sequence of small text deltas |
| Feel for a long answer | A long, blank freeze | Responsive — progress the whole time |
| Risk on very long outputs | HTTP request can **time out** | Connection stays alive, delivering chunks |
| Getting the whole thing | It *is* the whole thing | Accumulate deltas, or call `get_final_message()` |
| Content | Identical | Identical |

## 3. Formal Definition

**Streaming** returns a single Messages API response **incrementally**, as a sequence of **Server-Sent
Events (SSE)** — a standard where the server holds the HTTP connection open and pushes named events as
data becomes available — instead of buffering the entire response and returning it in one body.

You opt in by using the SDK's streaming helper (or, at the HTTP level, sending `"stream": true`). The
content produced is exactly what a non-streaming call would produce; what changes is that you receive
it in pieces over time.

A streamed response is an **ordered event sequence**:

| Event | What it carries | Fires |
|---|---|---|
| `message_start` | The `Message` shell — id, role, model, empty `content`, initial `usage` | Once, at the very beginning |
| `content_block_start` | A new content block opening (e.g. `{"type": "text"}`) | When each block begins |
| `content_block_delta` | An **incremental piece** — for text, `delta.type == "text_delta"` and the chunk is `delta.text` | Many times — the actual streamed tokens |
| `content_block_stop` | The current block is complete | When each block finishes |
| `message_delta` | Top-level updates — the final **`stop_reason`** and output-token **`usage`** | Once, near the end |
| `message_stop` | The response is complete | Once, at the very end |

The block events nest and repeat: `message_start`, then for each block `content_block_start` →
(`content_block_delta`)* → `content_block_stop`, then `message_delta`, then `message_stop`. To
reconstruct the text, concatenate every `content_block_delta.text` in order.

The Python SDK wraps this in helpers so you rarely touch raw events:

| Term | Meaning |
|---|---|
| `client.messages.stream(...)` | A **context manager** (`with ... as stream:`) that opens the stream and accumulates state for you. |
| `stream.text_stream` | An iterator that yields **just the text deltas** as plain strings — the common case. |
| `stream.get_final_message()` | After iterating, returns the **complete assembled `Message`** — same object a non-streaming call would have returned (full `content`, `stop_reason`, `usage`). |
| text delta | One `content_block_delta` with `delta.type == "text_delta"`; the chunk is `event.delta.text`. |

> The mental model in one line: **streaming is the same response, reshaped from one blob into a
> sequence of deltas.** `text_stream` lets you react to each delta (UX); `get_final_message()` lets you
> still treat the result as one whole `Message` (timeout protection without losing the assembled
> answer). You don't have to choose — do both.

## 4. Worked Example

Here is the event sequence for a short streamed reply, then the **real SDK call** that consumes it.

```mermaid
sequenceDiagram
  participant App as your program
  participant API as Anthropic API
  App->>API: messages.stream(model, max_tokens, messages)
  API-->>App: message_start  (empty Message shell)
  API-->>App: content_block_start  (type: "text")
  API-->>App: content_block_delta  delta.text = "NameError "
  API-->>App: content_block_delta  delta.text = "means you "
  API-->>App: content_block_delta  delta.text = "used a variable…"
  API-->>App: content_block_stop
  API-->>App: message_delta  (stop_reason="end_turn", usage)
  API-->>App: message_stop
  Note over App: text_stream yielded each delta;<br/>get_final_message() returns the whole Message
```

The real streaming call (this hits the network and needs the `anthropic` package + an API key, so it
does **not** run in our sandbox — it's here so you've seen the genuine thing):

```python
import anthropic

client = anthropic.Anthropic()  # reads ANTHROPIC_API_KEY from the environment

with client.messages.stream(
    model="claude-opus-4-8",
    max_tokens=1024,
    messages=[
        {"role": "user", "content": "Explain a Python NameError to a beginner in two sentences."},
    ],
) as stream:
    # text_stream yields just the text deltas, in order, as they arrive.
    for text in stream.text_stream:
        print(text, end="", flush=True)   # flush=True so each chunk shows immediately

    # After the loop, you still get the complete Message — same object
    # messages.create() would have returned.
    final = stream.get_final_message()

print()                                    # newline after the streamed text
print(final.stop_reason)                   # -> "end_turn"
print(final.usage.output_tokens)           # -> the output token count
```

Three things to notice. (1) `stream.text_stream` is the 90% case — it hands you strings, you don't
parse events. (2) `flush=True` matters: without it your terminal may buffer the whole output and you
lose the live feel. (3) `get_final_message()` means streaming costs you nothing — you still end up
with the full `Message`, `stop_reason` and `usage` included, just as if you'd called `create()`.

## 5. Build It

We can't hit the network here, so we'll **model** streaming in deterministic stdlib Python. A generator
`stream_response(text)` plays the role of `stream.text_stream` — it `yield`s the response in chunks
(word by word). One consumer prints each delta *as it arrives* (progressive) **and** accumulates the
deltas into the final message. We contrast it with a non-streaming `messages_create()` that returns
only the whole string, at the end. The canned text stands in for the model's output so the run is
reproducible.

```python run
"""
Modelling Claude's streaming locally — no network, no anthropic SDK.

We mimic the SSE event sequence (message_start -> content_block_delta* ->
message_delta -> message_stop) with a plain Python generator that yields text
deltas. A progressive consumer prints each delta as it "arrives" and
accumulates them into the final message. We contrast with a non-streaming
create() that hands back only the whole string at the end.
"""

import time

# A fixed, canned "model output". The real model would generate this
# token-by-token; here we just decide it up front so the run is deterministic.
CANNED = (
    "NameError means you used a variable before defining it. "
    "Define count above the loop, then run again."
)


# ---- 1. The streaming side: a generator that yields text deltas ----------

def stream_response(text):
    """Stand-in for `stream.text_stream`.

    Yields the response in chunks (here: word by word, space-preserving),
    exactly like a content_block_delta carrying delta.text on the wire.
    """
    words = text.split(" ")
    for i, word in enumerate(words):
        # Re-attach the space we split on, except after the final word, so the
        # concatenation of all deltas reconstructs `text` byte-for-byte.
        delta = word if i == len(words) - 1 else word + " "
        # A real stream would pause here while the model generates the next
        # token. time.sleep(0) keeps the run instant but marks the yield point.
        time.sleep(0)
        yield delta


def consume_streaming(text):
    """The UX win: show progress, and still end up with the whole message.

    Returns (final_message, delta_count) — the assembled string plus how many
    deltas we saw, standing in for a token count.
    """
    parts = []
    count = 0
    print("[streaming] ", end="", flush=True)
    for delta in stream_response(text):
        print(delta, end="", flush=True)  # appears progressively, left to right
        parts.append(delta)               # accumulate, like get_final_message()
        count += 1
    print()  # end the progressive line
    final_message = "".join(parts)
    return final_message, count


# ---- 2. The non-streaming side: one value, only at the end ---------------

def messages_create(text):
    """Stand-in for `client.messages.create()` (no stream).

    The caller is blocked until the whole response exists, then gets it in one
    piece. Same content as the stream — different DELIVERY.
    """
    parts = []
    for word in text.split(" "):       # the model still generates word by word
        time.sleep(0)                  # ...but the caller sees none of it
        parts.append(word)
    return " ".join(parts)             # the single return value, at the end


# ---- 3. Run both and prove they deliver identical content ----------------

def main():
    print("Claude is explaining a learner's error. Watch the two deliveries.\n")

    print("--- STREAMING (token-by-token, responsive) ---")
    final_message, count = consume_streaming(CANNED)
    print(f"[final message]: {final_message}")
    print(f"[token count]: {count} deltas\n")

    print("--- NON-STREAMING (one blob, at the end) ---")
    whole = messages_create(CANNED)
    print("[create() returns]: (caller was blocked until now)")
    print(f"[whole response]: {whole}\n")

    print("--- Same content, different delivery? ---")
    same = (final_message == whole)
    print(f"streamed assembly == non-streamed return: {same}")
    print(f"length: {len(final_message)} chars")

    # Streaming changes WHEN you see the bytes, never WHICH bytes. If this ever
    # failed, the model would mean something different over the two channels.
    assert same, "delivery must not change content"
    assert final_message == CANNED
    assert count == len(CANNED.split(" "))
    print("\nOK: streaming changed the delivery, not the message.")


if __name__ == "__main__":
    main()
```

Run it. The streaming branch prints the explanation **progressively** (in a real terminal you'd see it
build up left to right), then reports the assembled `[final message]` and a `[token count]: 17 deltas`
— our stand-in for `usage.output_tokens`. The non-streaming branch prints the *same* 100-character
explanation, but only as a single `[whole response]` at the end. The final check confirms
`streamed assembly == non-streamed return: True`: **identical content, different delivery.**

**Now make it concrete.** The `consume_streaming` function is doing both jobs at once — the `print` in
the loop is the *UX* half (what `text_stream` gives you), and the `parts.append` is the *accumulation*
half (what `get_final_message()` gives you). Delete the `print` and you have a silent
`messages.create()`; delete the `parts.append` and you have a pretty UI that forgets the answer. The
SDK lets you keep both, which is exactly why "stream for UX *and* call `get_final_message()`" is the
default advice.

## 6. Trade-offs & Complexity

| Non-streaming (`messages.create`) | Streaming (`messages.stream`) |
|---|---|
| Dead simple — one call, one `Message`, read `content` | Slightly more code: iterate deltas, then assemble |
| Long answers = a long, frozen wait (bad UX) | Progress token-by-token (responsive UX) |
| Large `max_tokens` (tens of thousands of tokens) risks an **HTTP timeout** → the whole request fails | Connection stays alive delivering chunks → no timeout |
| You get `content` / `stop_reason` / `usage` directly | Same, via `get_final_message()` after the loop |
| Fine for short, non-interactive calls (a label, a tiny extraction) | The default for long or user-facing responses |
| Total latency to *full* answer: identical | Total latency to *full* answer: identical |

The trade is small extra plumbing in exchange for responsiveness *and* timeout safety — and crucially
it costs you **nothing in capability**: `get_final_message()` hands back the same complete `Message`.
Note the last row: streaming does **not** make the full answer arrive sooner — the model takes just as
long to finish either way. What streaming changes is *when you see the first parts* and whether the
connection survives a long generation. The rule of thumb: **default to streaming for anything long or
interactive; reach for plain `create()` only for short, non-interactive calls** where the simplicity
wins and timeouts are a non-issue.

## 7. Edge Cases & Failure Modes

- **Forgetting `flush=True`.** Your terminal (or buffer) may hold the text until the end, defeating the
  whole point — you stream but it *looks* non-streamed. Always `print(text, end="", flush=True)`.
- **Long output without streaming → timeout.** A request with a large `max_tokens` (e.g. well over
  ~16K, up toward the 64K–128K ceiling) can take long enough that the non-streaming HTTP request times
  out and the *entire* call fails — you get nothing, not even partial text. The fix is to stream; the
  SDK even guards against this and will refuse some large non-streaming requests for exactly this
  reason. Stream, and use `get_final_message()` if you still want the whole thing.
- **Assuming the stream gives you `stop_reason`/`usage` as you go.** The final `stop_reason` and output
  `usage` arrive on the **`message_delta`** event near the *end* — not at the start. If you need them,
  read them from `get_final_message()` after the loop, not from the first delta.
- **A delta isn't always text.** When thinking or tool use is enabled, the stream interleaves other
  block types (e.g. `thinking_delta`, tool-input deltas). `text_stream` already filters to *just*
  text; if you iterate raw events instead, check `event.type` and `event.delta.type` before reading
  `.text` — assuming every delta is text will break (the Chapter 1 "`content` is a list of blocks"
  lesson, now in motion).
- **A dropped connection mid-stream.** Because the response arrives over a held-open connection, a
  network blip can interrupt it after some deltas — you have a *partial* message. Handle the relevant
  SDK errors (e.g. a connection error) and decide whether to retry the whole call.
- **Streaming doesn't speed up the full answer.** A common misconception: it feels faster, but
  total time to the *complete* response is the same. Don't reach for streaming to cut total latency —
  reach for it for *perceived* responsiveness and to dodge timeouts.

## 8. Practice

> **Exercise 1 — Why does the AI-tutor button freeze?** A teammate ships Cortex's "explain this error"
> feature using a single `messages.create()` call, and learners complain the button "hangs for ten
> seconds then dumps a wall of text." Nothing is broken. In one or two sentences, explain what's
> happening and the one-line change that fixes the feel.

<details>
<summary><strong>Answer</strong></summary>

`messages.create()` is **non-streaming**: it doesn't return until the model has finished writing the
*entire* explanation, so the UI has nothing to show for the whole generation time — hence the
ten-second freeze, then everything at once (the §2 *letter*). Nothing is failing; it's just the
wrong delivery for a long, user-facing response.

The fix is to **stream**: switch to `with client.messages.stream(...) as stream:` and render each
chunk from `stream.text_stream` as it arrives (with `flush=True`), so words appear token-by-token —
the same live feel as Cortex's existing code-runner output. You can still call
`stream.get_final_message()` afterward if you need the assembled text, `stop_reason`, or `usage`.

</details>

> **Exercise 2 — Order the events.** A single streamed text reply produces these event types, but
> shuffled: `message_stop`, `content_block_delta`, `message_start`, `content_block_stop`,
> `message_delta`, `content_block_start`. Put them in the order they fire, and say which one carries
> the final `stop_reason` and output `usage`.

<details>
<summary><strong>Answer</strong></summary>

The order is:

1. `message_start` — the empty `Message` shell (id, role, model).
2. `content_block_start` — the text block opens.
3. `content_block_delta` — the streamed text chunks (this one repeats, once per chunk).
4. `content_block_stop` — the text block closes.
5. `message_delta` — top-level updates: the final **`stop_reason`** and output-token **`usage`**.
6. `message_stop` — the response is complete.

The final `stop_reason` and output `usage` ride on **`message_delta`**, near the *end* — not on
`message_start`. So if you're tracking why the model stopped or how many tokens it produced, read them
after the deltas (in practice, from `get_final_message()`), not from the first event.

</details>

> **Exercise 3 — Same content, two channels.** A reviewer worries that switching the tutor from
> `create()` to `stream()` might subtly change *what* Claude says, or that you'll lose the ability to
> get the whole message and its token count. Reassure them, and name the helper that returns the
> complete `Message`.

<details>
<summary><strong>Answer</strong></summary>

Streaming changes **delivery, not content**. The model generates the same response either way; the
only difference is that streaming hands it to you in **deltas as they're produced** instead of one blob
at the end (the §2 *phone call vs letter* — the friend says the same sentences on the call as in the
letter). Our §5 Build It demonstrates exactly this: the streamed assembly and the non-streamed return
are byte-for-byte equal (`True`).

And you lose nothing: after iterating `stream.text_stream`, call **`stream.get_final_message()`** to
get the complete `Message` — full `content`, `stop_reason`, and `usage` (token counts) — the same
object `messages.create()` would have returned. That's why the standard pattern is "stream for the UX
*and* call `get_final_message()`": responsive output, with the whole answer still in hand.

</details>

```quiz
{
  "prompt": "What does streaming change about a Claude Messages API response?",
  "input": "Choose one:",
  "options": [
    "How the response is delivered (incrementally, token-by-token over SSE) — not its content, which is identical to a non-streaming call",
    "The actual words the model produces — streamed answers are shorter than non-streamed ones",
    "The total time to generate the full answer — streaming makes the complete response finish sooner",
    "Whether you can recover the whole message — once you stream, get_final_message() is unavailable"
  ],
  "answer": "How the response is delivered (incrementally, token-by-token over SSE) — not its content, which is identical to a non-streaming call"
}
```

## In the Wild

- **[Anthropic — Streaming Messages](https://docs.claude.com/en/docs/build-with-claude/streaming)** —
  the SSE event sequence (`message_start` → content blocks → `message_delta` → `message_stop`), event
  shapes, and the `stream=true` parameter. The primary source for this chapter.
- **[Anthropic SDKs — `messages.stream` & `text_stream`](https://docs.claude.com/en/api/client-sdks)**
  — the official `anthropic` libraries' streaming helpers (`messages.stream(...)`, `text_stream`,
  `get_final_message()`) that wrap the raw events.
- **[MDN — Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)** —
  the underlying web standard: a server holding an HTTP connection open and pushing named `event:` /
  `data:` frames, which is exactly the transport Claude streaming rides on.

---

**Next:** streaming reshapes *one* response for delivery. But when many requests share a large, stable
chunk of context — a long system prompt, a big document — you're paying to re-process those same tokens
every call. There's a way to make the API *remember* that prefix and charge a fraction for it. →
[7. Prompt caching](/cortex/the-claude-stack/building-with-the-claude-api/prompt-caching)
