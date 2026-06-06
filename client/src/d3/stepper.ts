// Step-control state machine for the engine-driven widgets — a plain-DOM port of
// Stepper.scala (the scalajs-react hook). Owns the step index, the play flag, and
// the play-loop timer; calls `onChange(index, playing)` whenever either changes.
// Same semantics: index clamped to range, manual steps pause playback, and
// pressing play at the end rewinds to the start first.

export class Stepper {
  private index = 0;
  private playing = false;
  private timer: number | null = null;

  constructor(
    private readonly stepCount: number,
    private readonly delayMs: number,
    private readonly onChange: (index: number, playing: boolean) => void,
  ) {}

  private clamp(i: number): number {
    const max = Math.max(1, this.stepCount) - 1;
    return Math.max(0, Math.min(max, i));
  }

  get current(): number {
    return this.clamp(this.index);
  }

  get isPlaying(): boolean {
    return this.playing;
  }

  get atEnd(): boolean {
    return this.stepCount === 0 || this.current === this.stepCount - 1;
  }

  private clearTimer(): void {
    if (this.timer !== null) {
      window.clearTimeout(this.timer);
      this.timer = null;
    }
  }

  private emit(): void {
    this.onChange(this.current, this.playing);
  }

  private scheduleTick(): void {
    this.clearTimer();
    if (!this.playing || this.atEnd) return;
    this.timer = window.setTimeout(() => {
      this.index = this.clamp(this.index + 1);
      if (this.atEnd) this.playing = false;
      this.emit();
      this.scheduleTick();
    }, this.delayMs);
  }

  next(): void {
    this.playing = false;
    this.clearTimer();
    this.index = this.clamp(this.index + 1);
    this.emit();
  }

  previous(): void {
    this.playing = false;
    this.clearTimer();
    this.index = this.clamp(this.index - 1);
    this.emit();
  }

  reset(): void {
    this.playing = false;
    this.clearTimer();
    this.index = 0;
    this.emit();
  }

  togglePlay(): void {
    if (this.playing) {
      this.playing = false;
      this.clearTimer();
      this.emit();
    } else {
      if (this.atEnd) this.index = 0;
      this.playing = true;
      this.emit();
      this.scheduleTick();
    }
  }

  destroy(): void {
    this.clearTimer();
  }
}
