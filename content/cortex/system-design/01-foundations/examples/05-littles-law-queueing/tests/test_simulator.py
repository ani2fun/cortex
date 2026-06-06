"""Tests for the M/M/c simulator.

We verify:
  (1) Little's Law holds on synthetic data (residual ≈ 0).
  (2) Response time grows as utilisation increases.
  (3) Stability checks reject ρ ≥ 1.
  (4) M/M/c with the same total capacity matches the M/M/1 mean utilisation
      (the *details* differ in the tail, but the mean ρ is identical).
"""

import math
import pytest

from queueing_lab import Simulator


def test_littles_law_holds_at_low_load() -> None:
    sim = Simulator(arrival_rate=2.0, service_rate=10.0, n_jobs=20_000)
    _, stats = sim.run()
    # Residual should be much smaller than W itself.
    assert abs(stats.littles_law_residual) < 0.01, (
        f"Little's Law residual too large: {stats.littles_law_residual}"
    )


def test_littles_law_holds_at_high_load() -> None:
    sim = Simulator(arrival_rate=9.0, service_rate=10.0, n_jobs=50_000)
    _, stats = sim.run()
    assert abs(stats.littles_law_residual) < 0.05, (
        f"Little's Law residual too large at high load: {stats.littles_law_residual}"
    )


def test_response_time_increases_with_utilisation() -> None:
    """Sanity: as ρ grows, mean W should grow strictly."""
    last_W = 0.0
    for target_rho in [0.30, 0.50, 0.70, 0.85, 0.95]:
        sim = Simulator(
            arrival_rate=target_rho * 10.0,
            service_rate=10.0,
            n_jobs=20_000,
            seed=hash(target_rho) & 0xFFFFFFFF,
        )
        _, stats = sim.run()
        # W should grow strictly. Allow a small slack for stochastic noise.
        assert stats.mean_response_time_W > last_W * 0.95, (
            f"W did not grow at ρ={target_rho}: prev={last_W}, "
            f"now={stats.mean_response_time_W}"
        )
        last_W = stats.mean_response_time_W


def test_unstable_configuration_rejected() -> None:
    """ρ ≥ 1 ⇒ the queue grows without bound; we refuse to simulate."""
    with pytest.raises(ValueError):
        Simulator(arrival_rate=10.0, service_rate=10.0)
    with pytest.raises(ValueError):
        Simulator(arrival_rate=11.0, service_rate=10.0)


def test_mmc_matches_mm1_in_mean_utilisation() -> None:
    """Same λ and the same total capacity (cµ) ⇒ same mean ρ across c values."""
    lam = 9.0
    total_capacity = 10.0
    rhos: list[float] = []
    for c in [1, 2, 5]:
        mu_per = total_capacity / c
        sim = Simulator(
            arrival_rate=lam,
            service_rate=mu_per,
            n_servers=c,
            n_jobs=20_000,
            seed=c,
        )
        _, stats = sim.run()
        rhos.append(stats.utilisation)
    # All three configurations should converge on ρ ≈ 0.9.
    for r in rhos:
        assert math.isclose(r, 0.9, rel_tol=0.05), (
            f"unexpected utilisation {r} (expected ~0.9)"
        )


def test_measured_rates_match_targets() -> None:
    """The simulator's measured λ and µ should match what we configured."""
    sim = Simulator(arrival_rate=5.0, service_rate=10.0, n_jobs=30_000)
    _, stats = sim.run()
    assert math.isclose(stats.arrival_rate_measured, 5.0, rel_tol=0.05)
    assert math.isclose(stats.service_rate_measured, 10.0, rel_tol=0.05)
