"""M/M/c queueing simulator.

Demonstrates Little's Law (``L = λ × W``) and shows the response-time cliff
that opens when utilisation crosses ~0.7. Used by Lesson 5 of the System
Design track.
"""

from queueing_lab.simulator import Simulator, Stats

__all__ = ["Simulator", "Stats"]
