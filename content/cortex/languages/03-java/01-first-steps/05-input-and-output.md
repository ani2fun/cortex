---
title: Input & Output
summary: I/O is method calls on stream objects — System.out for formatted output (printf), and a Scanner over System.in to turn typed characters into typed values. nextInt reads a real number ready for arithmetic; the read→compute→output skeleton; and the bad-input crash, building on every earlier Tier 0 chapter.
prereqs: []
---

# Input & Output — Talking With the User

A program becomes useful the moment it can take input from a person and respond. Both halves are just **method calls on stream objects**: `System.out` is an object you send text to (`println`, `printf`), and a `Scanner` wraps `System.in` — the keyboard — to turn the characters someone types into typed *values*. The chapter turns on one idea that ties the tier together: reading input is **parsing**. `Scanner`'s `nextInt()` hands you a real `int`, ready for the arithmetic of [Tutorial 3](/cortex/languages/java/first-steps/numbers-and-arithmetic) — and when the typed text is not a number, the parse fails loudly.

Every output below was produced by compiling and running the code. One practical note about *this page's* runner first:

> **A note on the Run button and typed input.** The sandbox behind ▶ Run compiles and runs your code but cannot pause to prompt you for keyboard input. So the real `Scanner(System.in)` examples here are shown as **static** code, with the exact output they produce *for a stated entry* (verified by running them with that input supplied). Beside each, a **runnable twin** points the Scanner at a fixed text source instead of the keyboard — the methods behave identically — so you can still click Run and experiment. On your own machine, `Scanner(System.in)` pauses and waits for you to type.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

## Table of contents

1. [Building output with `printf`](#1-building-output-with-printf)
2. [Reading input with `Scanner`](#2-reading-input-with-scanner)
3. [Reading numbers: `nextInt` and `nextDouble`](#3-reading-numbers-nextint-and-nextdouble)
4. [A first interactive program](#4-a-first-interactive-program)
5. [When the input is bad](#5-when-the-input-is-bad)
6. [Mental-model summary](#6-mental-model-summary)
7. [Gotcha checklist](#7-gotcha-checklist)

---

## 1. Building output with `printf`

You already have `println` for plain lines. For *formatted* output — a number to two decimals, values slotted into a template — Java has `printf` and its sibling `String.format`. A **format string** holds placeholders: `%s` (any value as text), `%d` (a whole number), `%f` (a decimal), `%n` (a newline). The values to fill them follow, in order.

```java run
public class Main {
    public static void main(String[] args) {
        String name = "Ada";
        int score = 95;
        double pi = 3.14159;
        System.out.printf("%s scored %d points%n", name, score);
        System.out.printf("pi to 2 places: %.2f%n", pi);
        String line = String.format("%s = %d", "score", score);
        System.out.println(line);
    }
}
```

**Output:**
```
Ada scored 95 points
pi to 2 places: 3.14
score = 95
```

**Analysis.** `printf` filled `%s` with `name` (`"Ada"`), `%d` with `score` (`95`), and `%n` ended the line. `%.2f` rendered `pi` rounded to two decimals (`3.14`). `String.format` does the same formatting but *returns* the result as a String instead of printing it — useful when you want to keep the text. `%d` is for integers, `%f` for floating-point, `%s` for anything as text, and `%n` is the portable newline.

**Intuition.**
*Mechanism.* `printf` walks the format string and, at each `%…`, consumes the next argument and renders it according to the specifier — `%d` as a decimal integer, `%.2f` as a decimal with two fraction digits.

*Concrete bite.* The specifier must match the argument's type, or `printf` throws at run time:

```java run
public class Main {
    public static void main(String[] args) {
        System.out.printf("%d%n", "not a number");
    }
}
```

**Output** *(a thrown exception):*
```
Exception in thread "main" java.util.IllegalFormatConversionException: d != java.lang.String
```

`%d` demands an integer, but a String was supplied, so `printf` throws `IllegalFormatConversionException` (read it as "`d` ≠ `String`"). 

*Earned rule.* Match each specifier to its value's type — `%d` for integers, `%f` for decimals, `%s` for anything — and use `%.Nf` to fix the number of decimals, `%n` for line breaks. The cost of `printf`'s power is that mismatches are *run-time* errors: the compiler does not check format strings against their arguments, so a wrong `%d`/`%s` surfaces only when that line runs.

---

## 2. Reading input with `Scanner`

To read what a person types, make a `Scanner` over `System.in` (the keyboard) and ask it for input. `Scanner` lives in Java's library under the full name `java.util.Scanner`; the `import` line at the top of the file lets you call it by its short name, `Scanner`. `nextLine()` returns the whole line typed; `next()` returns the next whitespace-separated word.

```java
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("What's your name? ");
        String name = sc.nextLine();
        System.out.println("Hello, " + name + "!");
    }
}
```

**Output** *(when you type `Ada` and press Enter):*
```
What's your name? Hello, Ada!
```

**Analysis.** `new Scanner(System.in)` built a Scanner reading the keyboard. `nextLine()` waited for a line and returned it as a String, which we greeted. (On this page the prompt and greeting appear together because the sandbox does not echo your keystrokes the way a terminal does; on your own machine you would see `Ada` where you typed it, between the prompt and the greeting.) Here is the **runnable twin** — identical except its Scanner reads a fixed String, so you can click Run:

```java run
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner("Ada");   // a fixed source, instead of the keyboard
        String name = sc.nextLine();
        System.out.println("Hello, " + name + "!");
    }
}
```

**Output:**
```
Hello, Ada!
```

**Intuition.**
*Mechanism.* A `Scanner` is a reader over a **source**. `new Scanner(System.in)` reads the keyboard; `new Scanner("Ada")` reads a fixed string — and `nextLine`, `next`, `nextInt` behave identically over either. It pulls input on demand, each call consuming a little more.

*Concrete bite.* Ask for input that is not there and it throws:

```java run
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner("");   // empty source — nothing to read
        String line = sc.nextLine();
        System.out.println(line);
    }
}
```

**Output** *(a thrown exception):*
```
Exception in thread "main" java.util.NoSuchElementException: No line found
```

The source is empty, so `nextLine()` has nothing to return and throws `NoSuchElementException`. (This is also why an interactive `Scanner(System.in)` program, clicked Run here with no keyboard to read, would fail — which is exactly why the real examples on this page are shown statically.)

*Earned rule.* Create one Scanner over `System.in` for real input; point one at a String only to demo or test. The cost is that a Scanner assumes input exists — read past the end and it throws — so a robust program checks `hasNextLine()` / `hasNextInt()` first, a guard you'll write properly once you have conditionals in Tier 1.

---

## 3. Reading numbers: `nextInt` and `nextDouble`

`nextInt()` reads the next token and parses it as an `int`; `nextDouble()` parses a `double`. The Scanner does the text-to-number conversion for you, so what you get back is a real number, ready for arithmetic.

```java
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter your age: ");
        int age = sc.nextInt();
        System.out.println("Next year you'll be " + (age + 1));
    }
}
```

**Output** *(when you type `36`):*
```
Enter your age: Next year you'll be 37
```

**Analysis.** `nextInt()` read `36` and returned the **int** `36` — not the text `"36"` — so `age + 1` is real arithmetic, `37`. The parentheses earn their keep: from [Tutorial 4](/cortex/languages/java/first-steps/strings-the-basics), `"… be " + age + 1` would concatenate to `"…be 361"`; `(age + 1)` forces the addition first. The runnable twin reads the number from a fixed source:

```java run
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner("36");
        int age = sc.nextInt();
        System.out.println("Next year you'll be " + (age + 1));
    }
}
```

**Output:**
```
Next year you'll be 37
```

**Intuition.**
*Mechanism.* `nextInt()` reads characters up to the next whitespace and parses them into an `int`. Crucially, it stops *before* the newline that ends the line — it consumes the number token, not the line.

*Concrete bite.* That leftover newline is the famous Scanner trap: a `nextLine()` after a `nextInt()` reads the *rest of the number's line* (empty), not the next line:

```java run
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner("36\nAda\n");
        int age = sc.nextInt();
        String name = sc.nextLine();   // surprise: the rest of the "36" line — empty
        System.out.println("age=" + age + " name=[" + name + "]");
    }
}
```

**Output:**
```
age=36 name=[]
```

`nextInt()` read `36` and stopped before its newline; the following `nextLine()` returned everything left on that same line — nothing — so `name` is empty (`[]`), not `"Ada"`. The fix is an extra `sc.nextLine()` after the `nextInt()` to swallow the leftover newline.

*Earned rule.* `nextInt`/`nextDouble` read a *token* and leave the rest of the line (newline included) unread; when you mix them with `nextLine`, consume that leftover newline first. The cost of Scanner's token model is exactly this trap — when a `nextLine()` after a `nextInt()` comes back empty, this is why.

---

## 4. A first interactive program

Put it together: read two numbers, add them, report the result — the **read → compute → output** skeleton that underlies countless programs.

```java
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("First number: ");
        int a = sc.nextInt();
        System.out.print("Second number: ");
        int b = sc.nextInt();
        System.out.printf("%d + %d = %d%n", a, b, a + b);
    }
}
```

**Output** *(when you type `7`, then `5`):*
```
First number: Second number: 7 + 5 = 12
```

**Analysis.** Two `nextInt()` calls read `7` and `5` as ints, then `printf` reported the sum. Reading with `nextInt` (rather than as text) is what makes `a + b` arithmetic (`12`) and not concatenation (`"75"`). The runnable twin supplies both numbers from one fixed source — the single space separates the two tokens:

```java run
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner("7 5");   // imagine these were typed
        int a = sc.nextInt();
        int b = sc.nextInt();
        System.out.printf("%d + %d = %d%n", a, b, a + b);
    }
}
```

**Output:**
```
7 + 5 = 12
```

**Intuition.**
*Mechanism.* `nextInt()` skips leading whitespace and reads one integer token, so `"7 5"` yields `7` then `5` across two calls. The read → compute → output shape is the spine of interactive programs.

*Concrete bite.* Drop the number-ness — read as text and add — and `+` flips back to concatenation:

```java run
public class Main {
    public static void main(String[] args) {
        String a = "7", b = "5";   // as text, not numbers
        System.out.println(a + b);
    }
}
```

**Output:**
```
75
```

Two strings joined → `"75"`, not `12`. Reading with `nextInt` (or converting with `Integer.parseInt`) is precisely what avoids the `+` trap from [Tutorial 4](/cortex/languages/java/first-steps/strings-the-basics), now at the input boundary.

*Earned rule.* Structure interactive programs as read → convert → compute → output, and read numbers *as numbers* (`nextInt`/`nextDouble`) so `+` stays arithmetic. The cost of reading numbers as text and converting late is the concatenation bug — convert at the boundary.

---

## 5. When the input is bad

Parsing can fail. `nextInt()` accepts only something that *is* an `int`; a word or a decimal makes it throw `InputMismatchException`. And `Integer.parseInt`, which converts a String you already hold, throws `NumberFormatException` on non-numbers. On real input, neither is rare.

```java run
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner("seven");
        int n = sc.nextInt();
        System.out.println(n);
    }
}
```

**Output** *(a thrown exception):*
```
Exception in thread "main" java.util.InputMismatchException
```

`nextInt()` found `seven`, which is not an integer, and threw — the program halted before printing. A user who types `seven` instead of `7` hits exactly this. The same failure, when you parse a String yourself:

```java run
public class Main {
    public static void main(String[] args) {
        System.out.println(Integer.parseInt("42"));
        System.out.println(Integer.parseInt("3.5"));
    }
}
```

**Output** *(then an error):*
```
42
```
```
Exception in thread "main" java.lang.NumberFormatException: For input string: "3.5"
```

`Integer.parseInt("42")` worked and printed `42`; `Integer.parseInt("3.5")` failed — `"3.5"` is not a *whole* number — with `NumberFormatException`, halting the program.

**Intuition.**
*Mechanism.* Both `nextInt` and `Integer.parseInt` validate as they parse: if the characters do not form an `int`, there is no value to return, so they throw rather than guess.

*Concrete bite.* The outputs above are the demonstration — `42` prints, then the bad parse throws and execution stops. Real user input is unpredictable, so this is not an edge case; it is Tuesday.

*Earned rule.* Convert at the boundary and assume conversion *can* fail on real input. The cost of unhandled bad input is a crash, right at the parse, with everything after it skipped. Recovering gracefully — re-prompting instead of crashing — needs `try`/`catch`, which is Tutorial 24 in Tier 4; for now, know that it happens, and where.

---

## 6. Mental-model summary

| Principle | Consequence |
|---|---|
| Output and input are method calls on stream objects | `System.out.printf(...)` formats; `new Scanner(System.in)` reads |
| `printf` placeholders must match argument types | `%d` with a String throws `IllegalFormatConversionException` at run time |
| A `Scanner` reads tokens from a source on demand | Reading past the end throws `NoSuchElementException`; guard with `hasNext…` |
| `nextInt`/`nextDouble` parse a number you can compute with | `age + 1` adds; reading as text would concatenate (`"36" + 1` → `"361"`) |
| `nextInt` leaves the line's newline unread | A following `nextLine()` returns empty — consume the newline first |
| Parsing validates and throws on bad input | `nextInt("seven")` / `parseInt("3.5")` crash; recover with try/catch (Tutorial 24) |

## 7. Gotcha checklist

- **`IllegalFormatConversionException` →** a `printf` specifier doesn't match its value (`%d` given a String); fix the specifier or the argument.
- **A `nextLine()` after `nextInt()` is empty →** the leftover newline; add an extra `sc.nextLine()` after the `nextInt()` to consume it.
- **`InputMismatchException` from `nextInt()` →** the next token isn't an integer (a word or decimal); read it differently or validate first.
- **`NumberFormatException: For input string: "…"` →** `Integer.parseInt` got non-numeric text; check the text, or handle the exception (Tutorial 24).
- **`+` concatenated input instead of adding →** you read or kept the value as text; read with `nextInt`/`nextDouble`, or parse before computing, and parenthesise (`+ (a + b)`).
- **`NoSuchElementException` on Run →** an interactive `Scanner(System.in)` program has no keyboard here; use the runnable-twin pattern (a String source) to experiment.

---

*Predict, then check.* Take the §4 runnable twin and predict its output if the source string were `"10 20"` instead of `"7 5"`. Now predict what happens if the source were `"ten 20"` — which line throws, and with what exception? Finally, change the twin to read two numbers and print their **average** to two decimals with `printf` (hints: from [Tutorial 3](/cortex/languages/java/first-steps/numbers-and-arithmetic), `nextInt` gives `int`s, so force floating-point division before the `%.2f`; reach for `nextDouble` if you'd rather read decimals directly). Build it and confirm.

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
