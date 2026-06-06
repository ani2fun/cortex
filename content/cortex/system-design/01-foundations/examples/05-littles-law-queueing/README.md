# Little's Law queueing simulator

A small **M/M/1** and **M/M/c** event-driven queueing simulator. Use it to *see* the moment latency explodes when utilisation crosses ~70% — and to verify Little's Law (`L = λ × W`) on synthetic data.

## What it shows you

The simulator generates a Poisson-arrival workload of jobs, each requiring a service time drawn from an exponential distribution, and feeds them through `c` identical servers (default `c=1`). It records the queueing delay and total response time of every job, then prints:

- **λ** (lambda) — the *measured* arrival rate.
- **µ** (mu) — the *measured* service rate of one server.
- **ρ** (rho) — the utilisation (`λ / (c × µ)`).
- **W** — mean response time (queue wait + service).
- **L** — mean number of jobs in the system (computed from the trace).
- **L vs λ × W** — the residual; if Little's Law holds, this is ≈ 0.

A single command runs a sweep across utilisation levels from 0.1 to 0.95 and prints a table that looks roughly like:

```
ρ      λ       µ       L         W           L - λ·W   verdict
0.10   1.0    10.0    0.11     0.111 s       0.001     LL holds
0.50   5.0    10.0    1.02     0.204 s      -0.005     LL holds
0.70   7.0    10.0    2.34     0.334 s      -0.007     LL holds
0.90   9.0    10.0    8.72     0.969 s       0.000     LL holds — but W has 9× exploded
0.95   9.5    10.0   17.83     1.876 s      -0.020     LL holds — and W has 17× exploded
```

The point is not that Little's Law holds (it always does — it is a tautology of conservation). The point is that **W explodes superlinearly** as ρ crosses 0.7 — which is why production systems target `ρ ≤ 0.6–0.7` and not 1.0.

## Quick start

```bash
just test    # runs pytest
just demo    # runs the utilisation-sweep table
```

## Project layout

```
05-littles-law-queueing/
├── README.md
├── justfile
├── pyproject.toml
├── src/queueing_lab/
│   ├── __init__.py
│   ├── simulator.py    ← M/M/c event-driven simulator (~150 lines)
│   └── demo.py         ← utilisation sweep producing the headline table
└── tests/
    └── test_simulator.py
```

## Why event-driven?

A naïve "step every microsecond" simulator would be slow and noisy. We use **discrete-event simulation** — advance a virtual clock from event to event (arrival or completion). It is the standard textbook approach and lets us simulate a million jobs in under a second.

## What's *not* in here

- Multi-class priority queues, batch arrivals, or arbitrary distributions. Plain M/M/c is enough to demonstrate the law and feel the cliff.
- Real-time visualisation. If you want a chart, pipe the per-job log to a notebook; the simulator emits CSV-friendly output.
