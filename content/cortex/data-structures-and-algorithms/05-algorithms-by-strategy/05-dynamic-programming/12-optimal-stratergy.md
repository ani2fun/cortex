---
title: "Optimal Game Strategy"
summary: "Two players take coins from either end of a row, both optimal — how much can the first player GUARANTEE? An adversarial interval DP: dp[i][j] = max over your two moves, but the opponent then leaves you the WORSE remainder (a min). The min IS the adversary; replace it with max and you've assumed a cooperative opponent."
prereqs:
  - 05-algorithms-by-strategy/05-dynamic-programming/06-longest-palindromic-subsequence
---

## Why It Exists

A row of coins sits between you and an opponent. Each turn, the player to move takes a coin — but only from *either end* of the row. Both of you play optimally. The question isn't "what's the most I could grab if my opponent blunders?" — it's "what can I **guarantee**, no matter how cleverly they play?"

That word *guarantee* is what makes this a new kind of DP. Every problem so far optimised against a fixed input; here the "input" fights back. The recurrence has to bake in a worst-case assumption about the opponent's reply — a **min** nested inside your **max**. That alternating max/min is the seed of minimax, game-tree search, and every turn-based strategy AI. And because the subproblem is a contiguous slice `[i..j]`, it's an [interval DP](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/longest-palindromic-subsequence) — filled by length, just like LPS.

## See It Work

`dp[i][j]` = the most the player-to-move can guarantee from the slice `coins[i..j]`. Take the left coin or the right coin; whichever you take, the opponent then plays optimally on the rest and **leaves you the worse of the two remainders** — that's the `min`.

```python run viz=grid
def optimal_strategy(coins):
    n = len(coins)
    dp = [[0] * n for _ in range(n)]
    for i in range(n):
        dp[i][i] = coins[i]                                   # one coin left: take it
    for length in range(2, n + 1):                            # interval DP: grow by length
        for i in range(n - length + 1):
            j = i + length - 1
            inner = dp[i + 1][j - 1] if i + 1 <= j - 1 else 0
            take_left  = coins[i] + min(dp[i + 2][j] if i + 2 <= j else 0, inner)  # opp leaves the worse
            take_right = coins[j] + min(inner, dp[i][j - 2] if i <= j - 2 else 0)
            dp[i][j] = max(take_left, take_right)             # you pick your better option
    return dp[0][n - 1]

print(optimal_strategy([8, 15, 3, 7]))   # 22
print(optimal_strategy([2, 2, 2, 2]))    # 4
```

```java run viz=grid
public class Main {
    static int optimalStrategy(int[] coins) {
        int n = coins.length;
        int[][] dp = new int[n][n];
        for (int i = 0; i < n; i++) dp[i][i] = coins[i];
        for (int len = 2; len <= n; len++)
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                int inner = (i + 1 <= j - 1) ? dp[i + 1][j - 1] : 0;
                int takeLeft  = coins[i] + Math.min(i + 2 <= j ? dp[i + 2][j] : 0, inner);
                int takeRight = coins[j] + Math.min(inner, i <= j - 2 ? dp[i][j - 2] : 0);
                dp[i][j] = Math.max(takeLeft, takeRight);
            }
        return dp[0][n - 1];
    }
    public static void main(String[] args) {
        System.out.println(optimalStrategy(new int[]{8, 15, 3, 7}));   // 22
        System.out.println(optimalStrategy(new int[]{2, 2, 2, 2}));    // 4
    }
}
```

Both print `22` then `4`. From `[8,15,3,7]` the first player guarantees 22 by taking the `7` first (which denies the opponent any good reply that reaches the `15`); `[2,2,2,2]` splits evenly, 4 each. Cost `O(n²)` time and space.

## How It Works

Your turn is a `max` (pick your better move); the opponent's turn is a `min` *from your point of view* (they take whatever leaves you worst):

```d2
direction: right
cell: "dp[i][j]\n(most the mover guarantees on coins[i..j])" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
left: "TAKE LEFT coins[i]\nopponent then plays [i+1..j]\n-> you get min(dp[i+2][j], dp[i+1][j-1])" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
right: "TAKE RIGHT coins[j]\nopponent then plays [i..j-1]\n-> you get min(dp[i+1][j-1], dp[i][j-2])" {style.fill: "#fde68a"; style.stroke: "#d97706"}
pick: "dp[i][j] = max(left, right)\n(your max OVER the opponent's min)" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
cell -> left
cell -> right
left -> pick
right -> pick
```

<p align="center"><strong>Adversarial DP = your <code>max</code> wrapped around the opponent's <code>min</code>. After you take an end, the opponent plays optimally and hands you back the <em>worse</em> of the two slices they could leave — so you plan against their best, not their kindest.</strong></p>

Two things to keep:

- **The `min` is the entire adversarial idea.** It encodes "the opponent will leave me whichever remainder is worse for me." Drop it — assume the opponent leaves you the *better* slice — and you're no longer solving a game; you're daydreaming ([Trace It](#trace-it) makes this vivid).
- **There's a slicker one-line formulation: track the *margin*.** Let `dp[i][j]` = the best achievable *(my score − opponent's score)* on `coins[i..j]`. Then `dp[i][j] = max(coins[i] − dp[i+1][j], coins[j] − dp[i][j-1])`: whatever margin the *opponent* can build on the leftover is *subtracted* from yours, because their gain is your loss. The first player wins (or ties) exactly when `dp[0][n-1] ≥ 0`. Same `O(n²)` interval DP, no awkward `i+2`/`j-2` indices — you'll use it in [Your Turn](#your-turn).

> **Key takeaway.** Optimal game strategy is an **adversarial interval DP**: your `max` over the opponent's `min`. `dp[i][j]` = guaranteed value on slice `[i..j]`, base `dp[i][i] = coins[i]`, filled by length. The subtractive *margin* form `dp[i][j] = max(coins[i] − dp[i+1][j], coins[j] − dp[i][j-1])` is the elegant equivalent — first player wins iff `dp[0][n-1] ≥ 0`.

## Trace It

The `min` is doing all the work, and it's easy to write the wrong aggregator there. If you assume the opponent will hand you the *better* leftover — a `max` instead of `min` — the recurrence still type-checks and runs. It just answers a different, fantasy question.

**Predict before you run:** for coins `[1, 100, 1]`, the first player can only *guarantee* a small amount (the opponent will grab the 100). But what does the buggy `max` version — "opponent always leaves me the better slice" — claim the first player gets?

```python run viz=grid
def guaranteed(coins, adversarial):
    n = len(coins)
    dp = [[0] * n for _ in range(n)]
    for i in range(n):
        dp[i][i] = coins[i]
    combine = min if adversarial else max                     # min = real opponent; max = "cooperative"
    for length in range(2, n + 1):
        for i in range(n - length + 1):
            j = i + length - 1
            inner = dp[i + 1][j - 1] if i + 1 <= j - 1 else 0
            take_left  = coins[i] + combine(dp[i + 2][j] if i + 2 <= j else 0, inner)
            take_right = coins[j] + combine(inner, dp[i][j - 2] if i <= j - 2 else 0)
            dp[i][j] = max(take_left, take_right)
    return dp[0][n - 1]

print("adversarial (min):", guaranteed([1, 100, 1], adversarial=True))
print("buggy (max):      ", guaranteed([1, 100, 1], adversarial=False))
```

<details>
<summary><strong>Reveal</strong></summary>

Adversarial gives `2`; the buggy `max` claims `101`. With `[1,100,1]`, whatever end you take first (a `1`), the rest is `[100, 1]` (or `[1, 100]`) and it's the *opponent's* turn — a real opponent grabs the `100`, leaving you the last `1`, so you guarantee just `1 + 1 = 2`. The buggy version assumes the opponent politely takes a `1` and *hands you* the `100`, inflating your "guarantee" to `101`. That `min`→`max` swap silently replaces your adversary with a collaborator: it's the difference between "what can I force?" and "what could I get if my opponent threw the game?" In adversarial DP the opponent's turn is *always* a worst-case-for-you aggregation — the `min` (or, in the margin form, the *minus*) is the only thing that makes the answer a real guarantee. Lose it and you compute an upper bound no rational opponent would ever let you reach.

</details>

## Your Turn

**Predict the Winner** ([LeetCode 486](https://leetcode.com/problems/predict-the-winner/)) — does the first player win (or tie)? This is the *margin* formulation from above: `dp[i][j] = max(nums[i] − dp[i+1][j], nums[j] − dp[i][j-1])`, and the answer is `dp[0][n-1] ≥ 0`. No `min` in sight — the subtraction *is* the adversary.

```python run viz=grid
def predict_winner(nums):
    n = len(nums)
    dp = [[0] * n for _ in range(n)]                          # dp[i][j] = best (mine - opponent's) on [i..j]
    for i in range(n):
        dp[i][i] = nums[i]
    for length in range(2, n + 1):
        for i in range(n - length + 1):
            j = i + length - 1
            dp[i][j] = max(nums[i] - dp[i + 1][j],            # opponent's margin on the rest is MY loss
                           nums[j] - dp[i][j - 1])
    return dp[0][n - 1] >= 0                                  # first player at least ties

print(predict_winner([1, 5, 2]))       # False
print(predict_winner([1, 5, 233, 7]))  # True
```

```java run viz=grid
public class Main {
    static boolean predictWinner(int[] nums) {
        int n = nums.length;
        int[][] dp = new int[n][n];
        for (int i = 0; i < n; i++) dp[i][i] = nums[i];
        for (int len = 2; len <= n; len++)
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                dp[i][j] = Math.max(nums[i] - dp[i + 1][j], nums[j] - dp[i][j - 1]);
            }
        return dp[0][n - 1] >= 0;
    }
    public static void main(String[] args) {
        System.out.println(predictWinner(new int[]{1, 5, 2}));       // false
        System.out.println(predictWinner(new int[]{1, 5, 233, 7}));  // true
    }
}
```

Both print `false` then `true`. In `[1,5,2]` the second player can always answer to come out ahead (margin −2 for player 1); in `[1,5,233,7]` the first player grabs the `7` to expose and then claim the `233`, winning easily. The subtractive form collapses the whole max-min dance into one signed recurrence — the cleanest expression of "their optimal play counts against me."

## Reflect & Connect

- **Adversarial DP = max over min.** Your move maximises; the opponent's move minimises *your* outcome. That alternation is minimax — the backbone of game-tree search and (with pruning) alpha-beta. Most two-player perfect-information games are this shape.
- **The margin trick erases the min.** Tracking *(my score − opponent's)* turns max-min into a single `max(a[i] − dp[…], a[j] − dp[…])`. Subtraction encodes the adversary because the opponent's gain is definitionally your loss. Win iff the margin is `≥ 0`.
- **It's interval DP.** State is a slice `[i..j]`, filled by increasing length — same engine as [LPS](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/longest-palindromic-subsequence) and [matrix-chain multiplication](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/matrix-chain-multiplication). The adversarial twist is *what* you aggregate, not *how* you fill.
- **Greedy fails here too — and worse.** Grabbing the larger end can *expose* an even larger coin to the opponent (take `8` from `[8,15,3,7]` and they pounce on the `15`). In adversarial settings a locally greedy move doesn't just cost you value, it actively *arms* your opponent.
- **The `min`/minus is non-negotiable.** Replace it with `max`/plus and you've modelled a cooperative opponent — an upper bound no real adversary permits. The single most common bug in game DP is forgetting whose turn minimises.

## Recall

<details>
<summary><strong>Q:</strong> What is the adversarial (total) recurrence for optimal game strategy?</summary>

**A:** `dp[i][j] = max(coins[i] + min(dp[i+2][j], dp[i+1][j-1]), coins[j] + min(dp[i+1][j-1], dp[i][j-2]))`. You `max` over taking either end; the inner `min` is the opponent leaving you the worse remainder. Base `dp[i][i] = coins[i]`, filled by length.

</details>
<details>
<summary><strong>Q:</strong> Why is there a <code>min</code> inside the <code>max</code>?</summary>

**A:** After you take an end, it's the opponent's turn; they play optimally for themselves, which means leaving you the *worse* of the two possible remainders. The `min` encodes worst-case-for-you — the definition of "guarantee."

</details>
<details>
<summary><strong>Q:</strong> What is the subtractive (margin) formulation, and when does the first player win?</summary>

**A:** `dp[i][j] = max(coins[i] − dp[i+1][j], coins[j] − dp[i][j-1])`, where `dp` is *(mover's score − opponent's score)*. The opponent's best margin on the leftover is subtracted (their gain is your loss). The first player wins or ties iff `dp[0][n-1] ≥ 0`.

</details>
<details>
<summary><strong>Q:</strong> What happens if you replace the <code>min</code> with <code>max</code>?</summary>

**A:** You model a *cooperative* opponent who hands you the better remainder — an unachievable upper bound (e.g. `[1,100,1]` jumps from a guaranteed `2` to a fantasy `101`). The result is no longer a guarantee.

</details>
<details>
<summary><strong>Q:</strong> Why does this fill by length, and what family does it belong to?</summary>

**A:** `dp[i][j]` depends on shorter inner slices (`dp[i+1][j-1]`, `dp[i+2][j]`, `dp[i][j-2]`), so they must be computed first — increasing-length order. It's interval DP, the same family as longest palindromic subsequence and matrix-chain multiplication.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., §15 — interval DP and optimal substructure; minimax appears in the game-tree literature built on the same max-min alternation.
- **GeeksforGeeks**, "Optimal Strategy for a Game" — the canonical coin-row formulation with both the min-of-opponent and subtractive recurrences.
- **LeetCode** 486 (Predict the Winner) and 877 (Stone Game) are the canonical drills; the `22`/`4`, the adversarial-vs-cooperative `2`/`101` on `[1,100,1]`, and the `false`/`true` winner calls above all come from the runnable blocks — re-run to verify.
