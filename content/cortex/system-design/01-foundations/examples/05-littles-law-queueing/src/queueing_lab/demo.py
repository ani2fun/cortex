"""Utilisation sweep demonstrating the latency cliff and Little's Law.

Run with ``just demo`` (or ``uv run python -m queueing_lab.demo``).
"""

from queueing_lab import Simulator


def run_sweep() -> None:
    print()
    print("=" * 84)
    print("M/M/1 utilisation sweep — c=1 server, µ=10.0 jobs/sec")
    print("=" * 84)
    print(f"{'ρ':>5}   {'λ':>5}   {'µ':>5}   {'L':>7}   {'W (s)':>8}   "
          f"{'L − λW':>8}   percentiles")
    # We hold µ fixed at 10/s and dial λ up to drive ρ.
    mu = 10.0
    for target_rho in [0.10, 0.30, 0.50, 0.70, 0.85, 0.90, 0.95]:
        lam = target_rho * mu
        sim = Simulator(arrival_rate=lam, service_rate=mu, n_servers=1, n_jobs=50_000)
        records, stats = sim.run()
        pcts = sim.percentiles(records, [0.50, 0.95, 0.99])
        print(
            f"{stats.utilisation:>5.2f}   "
            f"{stats.arrival_rate_measured:>5.2f}   "
            f"{stats.service_rate_measured:>5.2f}   "
            f"{stats.mean_jobs_in_system_L:>7.2f}   "
            f"{stats.mean_response_time_W:>8.4f}   "
            f"{stats.littles_law_residual:>+8.4f}   "
            f"p50={pcts[0.50]:.3f}  p95={pcts[0.95]:.3f}  p99={pcts[0.99]:.3f}"
        )
    print()
    print("Observe two things:")
    print("  1. The 'L - λW' column is ≈ 0 at every load — that is Little's Law")
    print("     holding *exactly* on synthetic data.")
    print("  2. W (and especially p99) explodes superlinearly as ρ approaches 1.")
    print("     Going from 70% to 95% utilisation roughly *5× the mean response*")
    print("     and *10× the tail*. This is why production targets ρ ≤ 0.6–0.7.")
    print()
    print("=" * 84)
    print("Pooling vs partitioning — same total capacity, two ways to allocate it")
    print("=" * 84)
    # Setup: total capacity cµ = 10 jobs/sec, total arrival rate λ = 9 jobs/sec.
    # Question: should we run ONE pooled M/M/c queue, or N independent M/M/1
    # queues each with capacity cµ/N and load λ/N?
    #
    # The pooled answer is dramatically better — this is the principle behind
    # connection pools, thread pools, and shared CDN edges.
    print(f"{'config':<32}   {'ρ':>5}   {'W (s)':>8}   {'p99 (s)':>8}")
    lam_total = 9.0
    cap_total = 10.0

    # Config 1: single fat M/M/1 — the theoretical floor for mean response time.
    sim = Simulator(arrival_rate=lam_total, service_rate=cap_total, n_servers=1, n_jobs=50_000)
    _, stats = sim.run()
    pcts = sim.percentiles(_, [0.99])
    print(f"{'1 fat M/M/1 (c=1, µ=10)':<32}   "
          f"{stats.utilisation:>5.2f}   {stats.mean_response_time_W:>8.4f}   {pcts[0.99]:>8.4f}")

    # Config 2: pooled M/M/c with 5 small servers sharing one queue.
    sim = Simulator(arrival_rate=lam_total, service_rate=2.0, n_servers=5, n_jobs=50_000)
    pooled_records, stats = sim.run()
    pcts = sim.percentiles(pooled_records, [0.99])
    print(f"{'pooled M/M/5 (each µ=2)':<32}   "
          f"{stats.utilisation:>5.2f}   {stats.mean_response_time_W:>8.4f}   {pcts[0.99]:>8.4f}")

    # Config 3: 5 *independent* M/M/1 queues — same total capacity, but each
    # only sees 1/5 of the arrivals. Approximate by running one of them.
    sim = Simulator(arrival_rate=lam_total / 5, service_rate=2.0, n_servers=1, n_jobs=50_000)
    partitioned_records, stats = sim.run()
    pcts = sim.percentiles(partitioned_records, [0.99])
    print(f"{'5 independent M/M/1 (each µ=2)':<32}   "
          f"{stats.utilisation:>5.2f}   {stats.mean_response_time_W:>8.4f}   {pcts[0.99]:>8.4f}")

    print()
    print("Two real lessons:")
    print("  1. Pooling beats partitioning. Five servers sharing one queue absorb")
    print("     a burst that would crush five independent queues — even though the")
    print("     total capacity is identical. This is why connection pools and")
    print("     thread pools exist, and why CDN edges share traffic across users.")
    print("  2. Splitting fixed capacity into more small pieces (without adding")
    print("     capacity) makes things *slightly worse*, not better. Horizontal")
    print("     scaling helps only when you ADD capacity. 'Just add more servers'")
    print("     is wrong if you forget to add work for them to do.")


if __name__ == "__main__":
    run_sweep()
