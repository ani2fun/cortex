"""Event-driven simulator for an M/M/c queue.

The "M/M/c" notation, due to Kendall:

  - **M** — Markovian (Poisson) arrivals: inter-arrival times are exponentially
            distributed with rate ``arrival_rate``.
  - **M** — Markovian service: service times are exponentially distributed
            with rate ``service_rate``.
  - **c** — number of identical parallel servers behind a single FIFO queue.

The simulator advances time by jumping from event to event — never by stepping
microseconds — so simulating millions of jobs is fast and exact.

Outputs include the per-job response times *and* the time-weighted average of
``L`` (the number of jobs in the system), which we use to validate Little's
Law on synthetic data.
"""

import heapq
import math
import random
from dataclasses import dataclass, field
from typing import Iterable


@dataclass
class JobRecord:
    """A single job's lifecycle for post-processing."""
    arrived_at: float
    service_started_at: float
    departed_at: float

    @property
    def wait_time(self) -> float:
        """Time spent waiting in queue (excluding service)."""
        return self.service_started_at - self.arrived_at

    @property
    def response_time(self) -> float:
        """Total time from arrival to departure (queue + service)."""
        return self.departed_at - self.arrived_at


@dataclass
class Stats:
    """Summary of one simulator run.

    Attributes:
        c: Number of servers in the run.
        arrival_rate_measured: Measured λ (jobs / unit time) over the trace.
        service_rate_measured: Measured µ for one server (jobs / unit time).
        utilisation: ρ = λ / (c × µ).
        mean_response_time_W: Mean response time across all completed jobs.
        mean_jobs_in_system_L: Time-weighted average number of jobs in the
            system over the simulated horizon.
        littles_law_residual: ``L - λ × W``. By Little's Law this is ≈ 0
            for any stable run; we compute it explicitly to validate.
    """
    c: int
    arrival_rate_measured: float
    service_rate_measured: float
    utilisation: float
    mean_response_time_W: float
    mean_jobs_in_system_L: float
    littles_law_residual: float


class Simulator:
    """Discrete-event M/M/c simulator with a single FIFO queue.

    Parameters:
        arrival_rate: λ in jobs/unit-time.
        service_rate: µ per server in jobs/unit-time.
        n_servers:    c.
        n_jobs:       how many jobs to simulate. The default of 50_000 gives
                      smooth percentile estimates without measurable noise.
        seed:         PRNG seed for reproducibility.
    """

    def __init__(
        self,
        arrival_rate: float,
        service_rate: float,
        n_servers: int = 1,
        n_jobs: int = 50_000,
        seed: int = 42,
    ):
        if arrival_rate <= 0 or service_rate <= 0:
            raise ValueError("rates must be positive")
        if n_servers <= 0:
            raise ValueError("n_servers must be ≥ 1")
        # Stability check: ρ = λ / (c µ) must be < 1, otherwise the queue
        # grows without bound and W is undefined.
        rho = arrival_rate / (n_servers * service_rate)
        if rho >= 1.0:
            raise ValueError(
                f"unstable: λ={arrival_rate}, cµ={n_servers * service_rate}, "
                f"ρ={rho:.3f} ≥ 1"
            )

        self.arrival_rate = arrival_rate
        self.service_rate = service_rate
        self.n_servers = n_servers
        self.n_jobs = n_jobs
        self.rng = random.Random(seed)

    def run(self) -> tuple[list[JobRecord], Stats]:
        """Simulate the workload and return per-job records + summary stats."""
        # Event queue holds (time, kind, payload). kinds: "arrival", "departure".
        events: list[tuple[float, str, int]] = []
        # FIFO of arrival times for jobs currently waiting for a server.
        queue: list[float] = []
        # Servers are tracked as a count of busy ones; we don't need identities.
        busy = 0
        records: list[JobRecord] = []

        # Generate all arrivals up front so the trace is deterministic.
        t = 0.0
        for _ in range(self.n_jobs):
            t += self.rng.expovariate(self.arrival_rate)
            heapq.heappush(events, (t, "arrival", 0))

        # Running time-integral of (jobs in system). Updated *before* each
        # event by adding `in_system * (now - last_event_time)`. This way the
        # area under the L(t) step-function is exactly correct.
        in_system_area = 0.0
        in_system = 0
        last_event_time = 0.0

        while events:
            now, kind, _ = heapq.heappop(events)
            # Accumulate area under L(t) for the interval [last_event_time, now]
            # using the count that was in effect *just before* this event.
            in_system_area += in_system * (now - last_event_time)
            last_event_time = now

            if kind == "arrival":
                in_system += 1
                if busy < self.n_servers:
                    # A server is free — start service immediately.
                    busy += 1
                    service_time = self.rng.expovariate(self.service_rate)
                    heapq.heappush(
                        events, (now + service_time, "departure", 0)
                    )
                    records.append(JobRecord(
                        arrived_at=now,
                        service_started_at=now,
                        departed_at=now + service_time,
                    ))
                else:
                    # All servers busy — the job waits in queue.
                    queue.append(now)
            else:  # departure
                in_system -= 1
                busy -= 1
                if queue:
                    # A waiting job is now picked up by the freed server.
                    arrival_t = queue.pop(0)
                    busy += 1
                    service_time = self.rng.expovariate(self.service_rate)
                    departed_at = now + service_time
                    heapq.heappush(events, (departed_at, "departure", 0))
                    records.append(JobRecord(
                        arrived_at=arrival_t,
                        service_started_at=now,
                        departed_at=departed_at,
                    ))

        # --- Statistics ------------------------------------------------
        # Sort records by departure time so percentiles are stable.
        records.sort(key=lambda r: r.departed_at)

        horizon = last_event_time
        # Measured arrival rate = (# arrivals) / horizon.
        lam = len(records) / horizon
        # Per-server service rate. Each record's busy-time is (departed - started),
        # which by definition is the *service time* that one server spent on it.
        # So total_busy is the sum across all jobs of one-server-time;
        #   per-server-busy = total_busy / c (because c servers shared the load).
        # Per-server service rate = jobs-served-per-server / time-busy-per-server.
        # M/M/1 sanity check: total_busy ≈ N / µ ⇒ µ = N / total_busy ✓.
        total_busy = sum(r.departed_at - r.service_started_at for r in records)
        if total_busy > 0:
            mu = len(records) / total_busy
        else:
            mu = math.nan

        rho = lam / (self.n_servers * mu)
        W = sum(r.response_time for r in records) / len(records)

        L = in_system_area / horizon if horizon > 0 else 0.0
        residual = L - lam * W

        return records, Stats(
            c=self.n_servers,
            arrival_rate_measured=lam,
            service_rate_measured=mu,
            utilisation=rho,
            mean_response_time_W=W,
            mean_jobs_in_system_L=L,
            littles_law_residual=residual,
        )

    def percentiles(
        self, records: Iterable[JobRecord], qs: Iterable[float]
    ) -> dict[float, float]:
        """Convenience: return response-time percentiles for plotting."""
        rs = sorted(r.response_time for r in records)
        out: dict[float, float] = {}
        n = len(rs)
        for q in qs:
            idx = min(n - 1, int(q * n))
            out[q] = rs[idx]
        return out
