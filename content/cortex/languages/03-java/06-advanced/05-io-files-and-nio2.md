---
title: I/O, Files & NIO.2
summary: I/O moves bytes (binary) or characters (text, via an encoding). NIO.2 — Path and Files — is the modern, concise file API; Files.lines() returns a java.util.stream.Stream, clarifying the name clash between I/O "streams" and the Stream API. Text is bytes through a charset (UTF-8), so a character can be several bytes, and buffering batches the costly system calls. Every behavior shown with verified output.
prereqs: []
---

# I/O, Files & NIO.2 — Reading and Writing the World

A program that can't read or write outside itself is sealed off. **I/O** (input/output) connects it to files, the network, and the console — and it comes in two flavors: **byte** I/O moves raw binary, while **character** I/O moves text, which is bytes interpreted through a **charset** (encoding) like UTF-8. The modern file API, **NIO.2** (`java.nio.file`, since JDK 7), is built on `Path` (a location) and `Files` (static operations), and it makes common tasks one line. It also bridges to the [Streams API](/cortex/languages/java/advanced/functional-java-and-streams): `Files.lines()` returns a `java.util.stream.Stream<String>` — which is the moment to clear up a genuine confusion, because the word "stream" means two different things in Java (an I/O byte stream, and the functional Stream pipeline). Two more realities round it out: text is bytes-through-an-encoding (so one character can be several bytes), and **buffering** batches the expensive system calls that I/O really is.

This uses [streams](/cortex/languages/java/advanced/functional-java-and-streams) and [try-with-resources](/cortex/languages/java/robust-oop/exceptions). Every output below was produced by compiling and running the code (each writes and reads a file in its working directory).

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

## Table of contents

1. [NIO.2: `Path` and `Files`](#1-nio2-path-and-files)
2. [Reading lines as text](#2-reading-lines-as-text)
3. [The `Stream` name clash](#3-the-stream-name-clash)
4. [Bytes vs characters, and buffering](#4-bytes-vs-characters-and-buffering)
5. [Mental-model summary](#5-mental-model-summary)
6. [Gotcha checklist](#6-gotcha-checklist)

---

## 1. NIO.2: `Path` and `Files`

A `Path` names a file or directory location. The `Files` class holds static methods that operate on paths — `writeString`, `readString`, `size`, and dozens more — turning what used to be many lines of stream-and-close ceremony into one call.

```java run
import java.nio.file.*;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Path file = Path.of("greeting.txt");
        Files.writeString(file, "Hello, file!\nSecond line.\n");
        String content = Files.readString(file);
        System.out.print(content);
        System.out.println("size: " + Files.size(file) + " bytes");
    }
}
```

**Output:**
```
Hello, file!
Second line.
size: 26 bytes
```

**Analysis.** `Path.of("greeting.txt")` named a file; `Files.writeString` created it and wrote the text (opening, writing, and closing in one call); `Files.readString` read it all back; `Files.size` reported `26` bytes (the 24 visible characters plus two newlines). No streams to open or close by hand — `Files` handles the resource management internally. (The methods declare `IOException`, a [checked exception](/cortex/languages/java/robust-oop/exceptions), so `main` must handle or declare it.)

**Intuition.**
*Mechanism.* A `Path` is just a value describing a location — it doesn't touch the disk. `Files` methods are where the actual I/O happens, each performing the open/operate/close cycle and translating OS errors into Java exceptions.

*Concrete bite.* Because a `Path` is only a name, operating on one that doesn't exist fails at the `Files` call, not at `Path.of`:

```java run
import java.nio.file.*;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Path missing = Path.of("does-not-exist.txt");
        System.out.println(Files.readString(missing));
    }
}
```

**Output** *(a thrown exception):*
```
Exception in thread "main" java.nio.file.NoSuchFileException: does-not-exist.txt
```

`Path.of(...)` succeeded (it's just a name), and the failure came when `Files.readString` tried to actually read a file that isn't there. The distinction matters: constructing a `Path` never throws for a missing file; the `Files` operation does.

*Earned rule.* Use NIO.2 (`Path` + `Files`) for file work — it's concise, manages resources for you, and gives precise exceptions; reach for raw streams only when you need fine control. The cost is handling `IOException` (file I/O genuinely can fail — missing files, permissions, full disks); the benefit is one-line reads/writes instead of the verbose open-read-close-in-finally boilerplate of the old `java.io` API.

---

## 2. Reading lines as text

Most files are line-oriented text. `Files.write(path, List<String>)` writes each element as a line, and `Files.readAllLines` reads them back into a `List<String>`.

```java run
import java.nio.file.*;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        Path file = Path.of("data.txt");
        Files.write(file, List.of("apple", "banana", "cherry"));
        List<String> lines = Files.readAllLines(file);
        System.out.println("lines: " + lines.size());
        System.out.println(lines.get(1));
    }
}
```

**Output:**
```
lines: 3
banana
```

**Analysis.** `Files.write` wrote three lines (adding a line separator after each); `Files.readAllLines` read them into a `List` of three strings, so `lines.get(1)` is the second, `"banana"`. This is the everyday text-file pattern — write a list of lines, read a list of lines — with the encoding handled (UTF-8 by default) and the file closed for you.

**Intuition.**
*Mechanism.* `readAllLines` reads the *entire* file into memory as a `List<String>`, splitting on line terminators and decoding bytes to characters via the charset. It's eager: convenient for small-to-medium files, all in memory at once.

*Concrete bite.* "All into memory" is the limit: `readAllLines` on a multi-gigabyte log loads the whole thing and can exhaust the heap. For large or unbounded files you want to process line by line *without* holding them all — which is exactly what the streaming version in the next section does.

*Earned rule.* Use `readAllLines`/`readString` for files that comfortably fit in memory (config, small data), and the streaming `Files.lines` for large ones. The cost of the eager methods is memory proportional to file size; the benefit is simplicity — a whole file as a `String` or `List` when you can afford to hold it.

---

## 3. The `Stream` name clash

"Stream" means two unrelated things in Java, and it trips people up. A **java.io stream** (`InputStream`/`OutputStream`) is a flow of *bytes*. A **java.util.stream.Stream** is the *functional pipeline* from Tutorial 28. They're different types from different packages — and `Files.lines()` returns the *latter*, letting you process a file with a stream pipeline.

```java run
import java.nio.file.*;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws IOException {
        Path file = Path.of("nums.txt");
        Files.write(file, List.of("3", "1", "4", "1", "5"));
        long count;
        try (Stream<String> lines = Files.lines(file)) {
            count = lines.filter(s -> Integer.parseInt(s) > 2).count();
        }
        System.out.println("lines > 2: " + count);
    }
}
```

**Output:**
```
lines > 2: 3
```

**Analysis.** `Files.lines(file)` returned a `java.util.stream.Stream<String>` — a lazy pipeline over the file's lines — so we `filter`ed and `count`ed exactly as with any stream (3, 4, 5 are > 2). Crucially it's in a `try`-with-resources: this stream is backed by an *open file*, so it must be closed, unlike the in-memory streams of Tutorial 28. The "stream" here is the functional kind, reading lazily so it never holds the whole file in memory.

**Intuition.**
*Mechanism.* `Files.lines` opens the file and exposes its lines as a lazy `Stream<String>`, reading on demand as the pipeline pulls. Because it holds an OS file handle, it implements `AutoCloseable` and must be closed — hence `try`-with-resources. The unrelated `java.io` byte streams (`FileInputStream`, etc.) are a separate, lower-level world for binary data.

*Concrete bite.* The name clash causes real confusion: an `InputStream` is *not* a `Stream`, has no `map`/`filter`, and isn't interchangeable. And forgetting `try`-with-resources on `Files.lines` leaks the file handle (unlike a `list.stream()`, which holds no resource). Same word, two meanings, two lifecycles.

*Earned rule.* Read "stream" by package: `java.util.stream.Stream` is the functional pipeline (`map`/`filter`/`collect`); `java.io`/`java.nio` streams are byte/character channels. Close `Files.lines` (and any resource-backed stream) with `try`-with-resources. The cost is the terminology overhead; the benefit is that `Files.lines` lets you bring the whole expressive Stream API to a file, processing huge files lazily without loading them.

---

## 4. Bytes vs characters, and buffering

Under text lies bytes. A `String`'s **characters** become **bytes** through a charset, and in UTF-8 a non-ASCII character takes *more than one* byte. So a file's byte count and its character count are not the same:

```java run
import java.nio.file.*;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Path file = Path.of("bytes.txt");
        Files.writeString(file, "Café");
        byte[] bytes = Files.readAllBytes(file);
        String text = Files.readString(file);
        System.out.println("bytes: " + bytes.length);
        System.out.println("chars: " + text.length());
    }
}
```

**Output:**
```
bytes: 5
chars: 4
```

**Analysis.** `"Café"` is **4 characters**, but **5 bytes** on disk: `C`, `a`, `f` are one byte each in UTF-8, while `é` is two. `readAllBytes` sees the raw bytes (`5`); `readString` decodes them back to characters (`4`). This is the byte-vs-character distinction made concrete — text is an *interpretation* of bytes, and which charset you use determines the bytes. (The default has been UTF-8 since JDK 18; relying on a platform default charset was a classic source of "works on my machine" bugs.)

**Intuition.**
*Mechanism.* A `Reader`/`Writer` (or `Files.readString`/`writeString`) decodes/encodes between characters and bytes via a charset; an `InputStream`/`OutputStream` (or `readAllBytes`) moves raw bytes with no interpretation. And each underlying `read`/`write` can be a **system call** — slow — so **buffering** (`BufferedReader`/`BufferedWriter`, which `Files` methods use internally) batches many small operations into few large ones.

*Concrete bite.* Two traps live here. Mixing up byte and character APIs corrupts text (reading UTF-8 bytes as if they were Latin-1 mangles `é`); and doing unbuffered, one-byte-at-a-time I/O is orders of magnitude slower than buffered reads because each call hits the OS. Always specify the charset and read in buffered chunks. (Java's older **serialization** — `Serializable`/`ObjectOutputStream` — turns objects into bytes automatically, but is now discouraged for its security and versioning pitfalls; prefer an explicit format like JSON.)

*Earned rule.* Use character I/O (`Reader`/`Writer`, `readString`) with an explicit UTF-8 charset for text, byte I/O (`InputStream`/`readAllBytes`) for binary, and let buffering (built into the `Files` helpers) batch the system calls. The cost is being deliberate about encoding and resource handling; the benefit is correct, fast I/O that doesn't silently corrupt non-ASCII text or crawl one byte at a time.

---

## 5. Mental-model summary

| Principle | Consequence |
|---|---|
| `Path` names a location; `Files` performs the I/O | `Path.of` never throws for a missing file; the `Files` operation does (`NoSuchFileException`) |
| `readAllLines`/`readString` load the whole file into memory | Fine for small files; a huge file can exhaust the heap |
| `Files.lines()` returns a `java.util.stream.Stream` over lines | Lazy line processing — but it's resource-backed, so close it (`try`-with-resources) |
| "stream" means two things: `java.io` bytes vs `java.util.stream` pipeline | They're unrelated types; an `InputStream` has no `map`/`filter` |
| Text is bytes through a charset; UTF-8 is variable-width | `"Café"` is 4 chars but 5 bytes; specify the charset, and buffer for speed |

## 6. Gotcha checklist

- **`NoSuchFileException` →** the file doesn't exist (or wrong path); creating a `Path` doesn't create a file — the `Files` operation needs it to exist (for reads).
- **`OutOfMemoryError` reading a file →** `readAllLines`/`readString` loaded it all; use `Files.lines` to stream a large file lazily.
- **A resource-backed stream leaked / file stayed open →** `Files.lines` holds a file handle; wrap it in `try`-with-resources.
- **Confused `InputStream` with `Stream` →** different packages and purposes; `java.util.stream.Stream` has `map`/`filter`, byte streams don't.
- **Non-ASCII text got mangled →** a charset mismatch; read/write with an explicit UTF-8 charset, and don't rely on a platform default.

---

*Predict, then check.* Predict the byte count and character count that `Files.writeString` then `readAllBytes`/`readString` report for the string `"a€b"` (the euro sign `€` is 3 bytes in UTF-8). Next, predict whether `Files.readString(Path.of("nope.txt"))` throws at `Path.of` or at `readString`, and the exception. Finally, rewrite the §3 example to *sum* the numbers in the file with a stream, and decide why the `Files.lines` stream needs a `try`-with-resources where a `List.stream()` would not.

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
