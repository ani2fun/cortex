---
title: '8. Vision & multimodal'
summary: Claude doesn't only read text — you can drop image and PDF blocks into a user message's content list, so the model actually *sees* a diagram, screenshot, chart, or scanned page alongside your words. The catch: pixels cost input tokens.
---

# 8. Vision & multimodal

## TL;DR

> Everything so far put **text** into the `messages` array. But a user message's **`content` can be a
> *list* that mixes text and *image* blocks** — and Claude sees them together. An image block is
> `{"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": <base64>}}`
> (or `"source": {"type": "url", ...}`); PDFs use a `{"type": "document", ...}` block, optionally with
> `"citations": {"enabled": true}`. So you can ask Claude to read a diagram, transcribe a screenshot,
> interpret a chart, or extract fields from a scanned page — tasks pure text can't reach. The price:
> **images cost *input* tokens by resolution** — roughly proportional to pixel count, so a big image
> can be a few thousand tokens. If you don't need full detail, **downsample before sending** (cost
> control — ties straight into Chapter 9).

## 1. Motivation

Cortex teaches code, and learners get stuck in a very visual way. A beginner runs the workbench, the
sandbox spits a wall of red, and they paste… nothing — because they don't yet know *which* line of the
traceback matters, so they screenshot the whole thing and want to ask "what's wrong here?" Another
learner is working a trees problem and has sketched a **binary tree on paper** to reason about it; they
want to point a phone at it and ask "is this balanced?" A third is staring at a Big-O **chart** in a
chapter and wants the curve explained.

Every one of those is an *image* question. With a text-only model, the AI tutor we keep sketching is
deaf to all of them: the learner would have to retype the traceback by hand, transcribe the tree into
some notation, describe the chart in words — exactly the friction we're trying to remove. The fix isn't
a cleverer prompt. It's a **different kind of input**: we hand the model the picture.

And here's the thing — *this very session* already used image-capable tools. The `visualize` MCP server
renders SVG/HTML the user can see, and the `computer-use` server takes screenshots of a desktop and
reasons over them. The model on the other end of those tools is *looking* at pixels. Vision is not
exotic; it's the same Messages API with a richer `content` list. (**GAP:** Cortex's app has **no
multimodal input today** — a learner cannot upload a screenshot or a photo into the platform. This
chapter is how we'd build that.)

## 2. Intuition (Analogy)

Remember the **brilliant amnesiac intern**? Up to now you've been stuck describing a painting to them
**over the phone**. You can say "there's a tree, the root is at the top, it leans heavily to the left,
there's a deep chain of nodes on one side" — and they're sharp, so they infer a lot. But it's
second-hand, lossy, and slow, and the moment the picture is at all detailed your words can't keep up.

**Vision is finally handing them the painting to look at.** Same intern, same conversation — but now
they *see* the thing you're asking about instead of reconstructing it from your narration. Or, if you
prefer the clinic: a doctor who, until now, could only listen to the patient *describe* the pain can at
last put the **X-ray on the lightbox** and read it directly. The diagnosis was always possible in
principle from a good enough description; seeing it is faster, surer, and catches what words drop.

| | Text-only (describe it over the phone) | **Multimodal (hand over the painting)** |
|---|---|---|
| What the model gets | Your *words about* the image | The **image itself**, pixels and all |
| Who does the seeing | You — then you narrate | **The model**, directly |
| Lossiness | High — you summarize, you miss things | Low — nothing is paraphrased away |
| Good for | Things you can fully put in words | Diagrams, screenshots, charts, scans, photos |
| Cost | Just your text tokens | Text **+ image tokens** (by resolution) |

## 3. Formal Definition

A **multimodal message** is an ordinary `{"role": "user", "content": ...}` message whose `content` is a
**list of content blocks** instead of a bare string. Each block is typed:

| Block | Shape | Meaning |
|---|---|---|
| Text | `{"type": "text", "text": "..."}` | A piece of prompt text — your question or instructions. |
| Image (base64) | `{"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": <base64 str>}}` | An inline image. `media_type` is the MIME type; `data` is the file's bytes, base64-encoded. |
| Image (url) | `{"type": "image", "source": {"type": "url", "url": "https://..."}}` | Same, but Anthropic fetches the image from a URL instead of you inlining it. |
| Document (PDF) | `{"type": "document", "source": {...}}` | A PDF/document — `source` can be `base64`, `url`, or a `file_id`. Add `"citations": {"enabled": true}` to get cited extractions. |

The model reads the blocks **in order, together** — text and images interleaved as one prompt. So this
content means "look at this image, then answer the question about it":

```json
"content": [
  { "type": "image", "source": { "type": "base64", "media_type": "image/png", "data": "iVBORw0KG..." } },
  { "type": "text",  "text": "What is in this image?" }
]
```

| Term | Definition |
|---|---|
| **Content block** | One typed element of a message's `content` list (`text`, `image`, `document`, and later `tool_use`/`tool_result`). |
| **`source`** | *How* to obtain a non-text block's bytes: `base64` (inline), `url` (fetched), or `file_id` (uploaded via the Files API). |
| **`media_type`** | The MIME type of an image — `image/png`, `image/jpeg`, `image/gif`, `image/webp`. PDFs are `application/pdf` on a `document` block. |
| **Image tokens** | The **input** tokens an image consumes, scaling roughly with its **pixel count** (resolution), not its file size on disk. |
| **Downsampling** | Shrinking an image's resolution before sending to cut its token cost when full detail isn't needed. |
| **Citations** | A `document` option (`"citations": {"enabled": true}`) that makes Claude attach source references to claims it extracts from the PDF. |

The load-bearing facts: **(1)** mixing modalities is just *a list with different block types* — there's
no separate "vision endpoint," it's the same `POST /v1/messages`; and **(2)** images are **priced in
input tokens by resolution**, so a single large screenshot can quietly cost a few thousand tokens before
the model writes a word.

## 4. Worked Example

The flow is the familiar one — only the request's `content` got richer:

```mermaid
flowchart LR
  IMG["screenshot / diagram<br/>(image bytes)"] -->|base64 encode| B["build content list<br/>[image block, text block]"]
  Q["your question<br/>(text)"] --> B
  B -->|POST /v1/messages| A["Anthropic API"]
  A --> M["Claude<br/>(sees image + text)"]
  M --> A
  A -->|content, usage<br/>(input incl. image tokens)| Y["your program"]
  classDef s fill:#1f2937,stroke:#6366f1,color:#e5e7eb,rx:6,ry:6;
  class A,M s;
```

Here is the **real SDK call** for "read this screenshot." It makes a network request and reads a file
off disk, so it does **not** run in our sandbox — it's here so you've seen the genuine article. Notice
`content` is a *list*: the image block, then the text.

```python
import base64
import anthropic

client = anthropic.Anthropic()  # reads ANTHROPIC_API_KEY from the env

# 1) read the image bytes and base64-encode them
with open("error_screenshot.png", "rb") as f:
    img_b64 = base64.standard_b64encode(f.read()).decode("utf-8")

# 2) one user message whose content MIXES an image block and a text block
response = client.messages.create(
    model="claude-opus-4-8",
    max_tokens=1024,
    messages=[
        {
            "role": "user",
            "content": [
                {
                    "type": "image",
                    "source": {
                        "type": "base64",
                        "media_type": "image/png",
                        "data": img_b64,
                    },
                },
                {"type": "text", "text": "This is a Python error a learner hit. What went wrong, and how do they fix it?"},
            ],
        }
    ],
)

text = next(b.text for b in response.content if b.type == "text")
print(text)
print(response.usage.input_tokens)  # includes the IMAGE tokens, not just the text
```

Two details earn their keep. First, the image goes in as a **block in the `content` list**, exactly
beside the text block — that single list is how you "say something about a picture." Second,
`response.usage.input_tokens` will be **far larger** than the handful of tokens your sentence is worth,
because the image's pixels are folded into the input count. That gap is the whole economics of vision,
and it's what we'll make concrete next.

## 5. Build It

We can't hit the network or ship real image bytes here, so we'll **model the mechanics** in plain
Python: a `build_message(text, images)` that assembles the `content` list (a text block plus image
blocks with *placeholder* base64), and an `estimate_tokens(content)` that prices text by `len // 4` and
images by `pixels // 750` "image tokens." Then we ask the **same question** two ways — text-only, and
with a 1024×768 image — and watch the image's cost appear.

```python run
# A content-block BUILDER + token ESTIMATOR for multimodal messages.
# No network, no real images: we fake base64 so the SHAPE is real.

def image_block(width, height, media_type="image/png"):
    """One image content block. The real SDK puts a base64 string in
    `data`; we stand in a placeholder and carry (w, h) for pricing."""
    return {
        "type": "image",
        "source": {
            "type": "base64",
            "media_type": media_type,
            "data": "<base64-bytes-omitted>",   # real call: base64 of the file
        },
        "_px": (width, height),   # bookkeeping for our estimator only
    }

def text_block(text):
    return {"type": "text", "text": text}

def build_message(text, images=()):
    """Assemble a user message whose content is a LIST: image blocks
    first (so the model 'sees' them), then the text question."""
    content = [image_block(w, h) for (w, h) in images]
    content.append(text_block(text))
    return {"role": "user", "content": content}

PX_PER_IMAGE_TOKEN = 750   # ~pixels per image token (illustrative, fixed)

def estimate_tokens(content):
    """Text blocks ~ len//4 tokens; image blocks ~ pixels // divisor."""
    text_toks = image_toks = 0
    for block in content:
        if block["type"] == "text":
            text_toks += len(block["text"]) // 4
        elif block["type"] == "image":
            w, h = block["_px"]
            image_toks += (w * h) // PX_PER_IMAGE_TOKEN
    return {"text": text_toks, "image": image_toks, "total": text_toks + image_toks}

def shape(content):
    return [b["type"] for b in content]   # what the message 'looks like'

question = "What data structure is drawn in this image, and is it balanced?"

text_only  = build_message(question)                        # no image
with_image = build_message(question, images=[(1024, 768)])  # + one image

print("=== text-only message ===")
print("content shape:", shape(text_only["content"]))
c1 = estimate_tokens(text_only["content"])
print("tokens -> text:", c1["text"], "image:", c1["image"], "total:", c1["total"])

print()
print("=== same question + a 1024x768 image ===")
print("content shape:", shape(with_image["content"]))
c2 = estimate_tokens(with_image["content"])
print("tokens -> text:", c2["text"], "image:", c2["image"], "total:", c2["total"])

print()
px = 1024 * 768
print("the image alone:", px, "px //", PX_PER_IMAGE_TOKEN, "=", px // PX_PER_IMAGE_TOKEN, "image tokens")
print("added by the image:", c2["total"] - c1["total"], "tokens (text cost is identical)")

small = build_message(question, images=[(512, 384)])  # downsample: 1/4 the pixels
c3 = estimate_tokens(small["content"])
print()
print("=== downsampled to 512x384 ===")
print("image tokens:", c3["image"], "(was", c2["image"], ") -> ~4x cheaper for 1/4 the pixels")
```

Running it prints the two **content shapes** — `['text']` vs `['image', 'text']` — and the cost of each.
The text-only ask is ~**15 tokens**. Add the 1024×768 image and the total jumps to ~**1063**: the
sentence still costs 15, but the image alone is `786432 // 750 = 1048` image tokens. **Downsample** to
512×384 (a quarter of the pixels) and the image drops to **262** tokens — roughly 4× cheaper. That last
move is the practical takeaway: resolution is a dial you control, and it's the cheapest lever you have on
a vision bill (Chapter 9). Try editing the image dimensions, or pass **two** images, and watch the
estimate track the pixels.

## 6. Trade-offs & Complexity

| Choice | Cost | Benefit | Reach for it when |
|---|---|---|---|
| **Send the image** vs describe it in text | Image input tokens (by resolution) | The model sees detail you'd lose in words | The answer depends on *what the picture actually shows* |
| **Full resolution** vs downsample | More tokens, slower, pricier | Maximum detail (fine print, dense diagrams) | Small text / fine structure matters; otherwise downsample |
| **base64 (inline)** vs **url** source | Bigger request body; you host nothing | One self-contained call; no public URL needed | The image is local/private; use `url` when it's already hosted |
| **Image** vs **`document` (PDF)** block | PDFs can be many pages → many tokens | Native multi-page text+layout, optional citations | The source is a real PDF/scan, not a single picture |
| **Citations** off vs on | Slightly more output structure | Traceable, source-anchored extractions | You need to *trust and verify* what was pulled from the doc |

The complexity that bites is **token cost ∝ pixels, summed over every image**. One screenshot is cheap;
a gallery of high-res photos, or a 40-page scanned PDF, can dwarf your text and blow past context limits
(Chapter 3) and budgets (Chapter 9) before the model says a word. Multimodal is a capability *and* a
spending decision — size your inputs deliberately.

## 7. Edge Cases & Failure Modes

- **Assuming `content` is still a string.** The instant you go multimodal it must be a **list of
  blocks**. Passing a string *plus* an image won't work — wrap your text in a `{"type": "text", ...}`
  block and put it in the list.
- **Wrong or mismatched `media_type`.** Labeling a JPEG as `image/png` (or sending an unsupported
  format) gets rejected. Use the true MIME type; supported images are **PNG, JPEG, GIF, WebP** (PDFs go
  on a `document` block as `application/pdf`).
- **Forgetting to base64-encode (or double-encoding).** `data` must be the **base64 text** of the bytes
  — not the raw bytes, not a file path, and not base64'd twice. Decode-encode mismatches surface as
  corrupt-image errors.
- **Surprise token bill from huge images.** A full-resolution phone photo can be **thousands** of input
  tokens each. If you don't need the detail, **downsample first** — it's the §5 lever and the cheapest
  one.
- **Tiny / illegible text in the image.** If the screenshot is too small or blurry, the model may
  misread fine print. Send a higher-resolution crop of the relevant region rather than a downscaled
  whole.
- **Expecting it to "remember" the image next turn.** Statelessness (Chapter 1) still rules — the image
  only exists in the turn whose `content` carried it. To keep referring to it, keep that message in your
  resent history (and keep paying its tokens, or summarize it to text).
- **A `url` source that isn't reachable.** If Anthropic can't fetch your URL (private host, expired
  link), the call fails. Use a `base64` source for anything not publicly and durably hosted.

## 8. Practice

> **Exercise 1 — Build the content.** A learner uploads `tree.png` (a hand-drawn binary tree) and asks
> "is this balanced?" Sketch the single `messages` array you'd send: the message's role, and the
> `content` list with the image and the question.

<details>
<summary><strong>Answer</strong></summary>

One user message whose `content` is a **list** — the image block, then the text question:

```python
messages = [
    {
        "role": "user",
        "content": [
            {"type": "image",
             "source": {"type": "base64", "media_type": "image/png", "data": tree_png_b64}},
            {"type": "text", "text": "This is a hand-drawn binary tree. Is it height-balanced? Explain."},
        ],
    }
]
```

`tree_png_b64` is `base64.standard_b64encode(open("tree.png","rb").read()).decode()`. The model reads the
two blocks together, so it answers *about the picture*. (A `system` prompt could carry the "you are a
patient DSA tutor" persona — Chapter 2 — but it's the `content` list that makes this multimodal.)

</details>

> **Exercise 2 — Price it.** Using the §5 estimator (text `len // 4`, image `pixels // 750`), a learner
> sends a 60-character question plus one 1600×1200 photo. Roughly how many input tokens is that, and
> which part dominates? Name the one-line lever to cut it.

<details>
<summary><strong>Answer</strong></summary>

- Text: `60 // 4 = 15` tokens.
- Image: `1600 * 1200 = 1,920,000` px, `// 750 = 2560` image tokens.
- Total ≈ **2575**, and the **image dominates** (~2560 of 2575 — over 99%). The 15-token sentence is a
  rounding error next to the pixels.

The lever is **downsampling**: halve each dimension to 800×600 and the image falls to `480000 // 750 =
640` tokens — a 4× cut for a quarter of the pixels, with little loss if you don't need the fine detail
(§6, and Chapter 9 on cost).

</details>

> **Exercise 3 — Image vs PDF.** A learner wants Claude to extract the fields from a **scanned 12-page
> insurance PDF** and cite where each value came from. Would you send images or a `document` block, and
> what option turns on the citations?

<details>
<summary><strong>Answer</strong></summary>

Send a **`document` block** (`{"type": "document", "source": {...}}`) with the PDF as its source
(`base64`, `url`, or a `file_id`) — not 12 separate image blocks. A `document` handles **multi-page**
text and layout natively, and is the right primitive for a real PDF/scan.

Turn on citations with **`"citations": {"enabled": true}`** on that block, so Claude anchors each
extracted value to its place in the source — exactly the *verify what it pulled* property §6 calls for.
Mind the cost (§7): 12 scanned pages can be a lot of input tokens.

</details>

```quiz
{
  "prompt": "How do you send an image to Claude alongside a question, and what does it cost?",
  "input": "Choose one:",
  "options": [
    "Put an image block and a text block together in the user message's `content` list; the image adds INPUT tokens roughly proportional to its resolution (pixel count)",
    "Call a separate /v1/vision endpoint with the image as the whole body; images are billed per file, not by tokens",
    "Paste a description of the image into the text — the model can't take real images, only text about them",
    "Set an `image_url` top-level field next to `model`; images are free because they aren't tokens"
  ],
  "answer": "Put an image block and a text block together in the user message's `content` list; the image adds INPUT tokens roughly proportional to its resolution (pixel count)"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Anthropic — Vision](https://docs.claude.com/en/docs/build-with-claude/vision)** — the canonical
  guide to image blocks: base64 vs url sources, supported formats, multiple images, and how image tokens
  are counted. The primary source for this chapter.
- **[Anthropic — PDF support](https://docs.claude.com/en/docs/build-with-claude/pdf-support)** — the
  `document` block in depth: base64/url/file_id sources, page limits, and combining text + visuals from a
  PDF.
- **[Anthropic — Citations](https://docs.claude.com/en/docs/build-with-claude/citations)** — enabling
  `citations` so Claude anchors document extractions to their source — the "trust but verify" layer over
  PDFs.

---

**Next:** images made the *input* rich — and quietly expensive. So how are tokens actually counted and
priced, what limits and rate caps will you hit, and how do you handle errors and retries gracefully? →
[9. Tokens, limits & errors](/cortex/the-claude-stack/building-with-the-claude-api/tokens-limits-errors)
