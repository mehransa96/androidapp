#!/usr/bin/env python3
"""
Hanita original music generator.

Every track in app/src/main/res/raw is composed and rendered by this script from
scratch (additive / physical-model synthesis written below). No samples, loops or
recordings by anyone else are used, so the audio is an original work of the Hanita
project and is released under CC0 1.0 (see licenses/Hanita-Music-CC0.txt).

Usage:  python3 tools/music/compose_tracks.py [output_dir] [track_id ...]
Needs:  numpy and ffmpeg (with libmp3lame) on PATH.

Each track is a seamless loop: the reverb tail and any notes that ring past the
loop point are folded back onto the beginning, so "repeat" plays without a gap.
"""
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

SR = 44100
TWO_PI = 2 * np.pi


# ----------------------------------------------------------------------------- basics

def midi_hz(m, a4=440.0):
    return a4 * 2.0 ** ((m - 69) / 12.0)


NOTE = {"C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3, "E": 4, "F": 5, "F#": 6,
        "Gb": 6, "G": 7, "G#": 8, "Ab": 8, "A": 9, "A#": 10, "Bb": 10, "B": 11}


def n(name):
    """'C4' -> midi number."""
    pitch = name[:-1]
    octave = int(name[-1])
    return 12 * (octave + 1) + NOTE[pitch]


def t_axis(dur):
    return np.arange(int(dur * SR)) / SR


def fft_filter(x, lo=None, hi=None, order=2):
    """Zero-phase Butterworth-shaped band filter applied in the frequency domain."""
    if lo is None and hi is None:
        return x
    size = len(x)
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(size, 1 / SR)
    g = np.ones_like(f)
    if hi is not None:
        g /= np.sqrt(1 + (f / hi) ** (2 * order))
    if lo is not None:
        with np.errstate(divide="ignore"):
            g /= np.sqrt(1 + (lo / np.maximum(f, 1e-6)) ** (2 * order))
    return np.fft.irfft(spec * g, size)


def adsr(length, a=0.01, d=0.1, s=0.7, r=0.2, sr=SR):
    total = length
    a_n, d_n, r_n = int(a * sr), int(d * sr), int(r * sr)
    s_n = max(0, total - a_n - d_n - r_n)
    env = np.concatenate([
        np.linspace(0, 1, max(a_n, 1), endpoint=False),
        np.linspace(1, s, max(d_n, 1), endpoint=False),
        np.full(s_n, s),
        np.linspace(s, 0, max(r_n, 1)),
    ])
    if len(env) < total:
        env = np.pad(env, (0, total - len(env)))
    return env[:total]


def pink_noise(length, rng):
    white = rng.standard_normal(length)
    spec = np.fft.rfft(white)
    f = np.fft.rfftfreq(length, 1 / SR)
    f[0] = 1
    spec /= np.sqrt(f)
    out = np.fft.irfft(spec, length)
    return out / (np.max(np.abs(out)) + 1e-9)


def brown_noise(length, rng):
    out = np.cumsum(rng.standard_normal(length))
    out = fft_filter(out, lo=20)
    return out / (np.max(np.abs(out)) + 1e-9)


# ----------------------------------------------------------------------------- instruments

def piano(freq, dur, vel=0.8, rng=None):
    t = t_axis(dur + 1.5)
    B = 0.0004
    out = np.zeros_like(t)
    brightness = 0.6 + 0.4 * vel
    for k in range(1, 12):
        fk = freq * k * np.sqrt(1 + B * k * k)
        if fk > 12000:
            break
        amp = (1 / k ** 1.25) * (brightness ** (k - 1))
        decay = (0.55 + 0.42 * k) * (freq / 261.6) ** 0.35
        for detune in (-0.12, 0.12):
            out += amp * np.exp(-t * decay) * np.sin(TWO_PI * (fk + detune) * t + k)
    hammer = np.exp(-t * 300) * (rng.standard_normal(len(t)) if rng is not None else 0) * 0.02
    out = out + hammer
    release = np.ones_like(t)
    rel_start = int(dur * SR)
    if rel_start < len(t):
        release[rel_start:] = np.exp(-np.arange(len(t) - rel_start) / SR * 7)
    attack = np.minimum(1, t / 0.004)
    return out * release * attack * vel * 0.18


def electric_piano(freq, dur, vel=0.7):
    t = t_axis(dur + 1.2)
    index = 1.6 * np.exp(-t * 3) + 0.25
    mod = np.sin(TWO_PI * freq * t)
    out = np.sin(TWO_PI * freq * t + index * mod)
    out += 0.12 * np.exp(-t * 9) * np.sin(TWO_PI * freq * 14.0 * t)  # tine
    env = np.exp(-t * 1.1) * np.minimum(1, t / 0.003)
    rel = int(dur * SR)
    if rel < len(t):
        env[rel:] *= np.exp(-np.arange(len(t) - rel) / SR * 9)
    return out * env * vel * 0.22


def pluck(freq, dur, vel=0.8, rng=None, brightness=0.5, decay=0.996):
    """Karplus-Strong plucked string (block-vectorised)."""
    total = int((dur + 1.0) * SR)
    period = max(2, int(round(SR / freq)))
    buf = np.zeros(total + period + 1)
    burst = rng.uniform(-1, 1, period)
    burst = fft_filter(np.tile(burst, 4), hi=1500 + 6000 * brightness)[:period]
    buf[1:period + 1] = burst
    pos = period + 1
    while pos < len(buf):
        end = min(pos + period, len(buf))
        cur = buf[pos - period:end - period]
        prev = buf[pos - period - 1:end - period - 1]
        buf[pos:end] = decay * 0.5 * (cur + prev)
        pos = end
    out = buf[1:total + 1]
    rel = int(dur * SR)
    env = np.ones(total)
    if rel < total:
        env[rel:] = np.exp(-np.arange(total - rel) / SR * 6)
    return out * env * vel * 0.5


def bell(freq, dur, vel=0.7, ratios=(1.0, 2.76, 5.40, 8.93), decays=(2.2, 4.5, 8.0, 12.0)):
    t = t_axis(dur + 2.0)
    out = np.zeros_like(t)
    for i, (r, d) in enumerate(zip(ratios, decays)):
        out += (0.6 ** i) * np.exp(-t * d) * np.sin(TWO_PI * freq * r * t)
    return out * np.minimum(1, t / 0.002) * vel * 0.25


def marimba(freq, dur, vel=0.8):
    return bell(freq, min(dur, 0.6), vel, ratios=(1.0, 4.0, 9.9), decays=(5.0, 14.0, 30.0)) * 1.3


def steel_pan(freq, dur, vel=0.8):
    return bell(freq, dur, vel, ratios=(1.0, 2.0, 3.0, 4.02), decays=(3.0, 4.0, 7.0, 9.0))


def saw_voice(freq, length, detune_cents=0.0, vibrato=0.0, vib_rate=5.0, phase=0.0):
    t = np.arange(length) / SR
    f = freq * 2 ** (detune_cents / 1200)
    inst = f * (1 + vibrato * np.sin(TWO_PI * vib_rate * t) * np.minimum(1, t / 0.6))
    ph = np.cumsum(inst) / SR + phase
    return 2 * (ph % 1.0) - 1


def pad(freqs, dur, attack=1.2, release=1.8, cutoff=1800, voices=5, spread=14, rng=None):
    length = int((dur + release) * SR)
    out = np.zeros(length)
    for f in freqs:
        for v in range(voices):
            det = (v - (voices - 1) / 2) * spread / max(1, voices - 1) * 2
            out += saw_voice(f, length, det, phase=rng.random() if rng is not None else 0)
    out = fft_filter(out, lo=60, hi=cutoff, order=2)
    env = adsr(length, a=attack, d=0.5, s=0.85, r=release)
    return out * env / (len(freqs) * voices) * 0.6


def soft_pad(freqs, dur, attack=2.0, release=2.5):
    """Very gentle sine/triangle pad for meditation tracks."""
    length = int((dur + release) * SR)
    t = np.arange(length) / SR
    out = np.zeros(length)
    for f in freqs:
        for det in (-0.3, 0.0, 0.31):
            out += np.sin(TWO_PI * (f + det) * t) + 0.18 * np.sin(TWO_PI * 2 * (f + det) * t)
    env = adsr(length, a=attack, d=0.5, s=0.9, r=release)
    return out * env / (len(freqs) * 3) * 0.5


def strings(freq, dur, vel=0.7, attack=0.35, cutoff=2600):
    length = int((dur + 0.8) * SR)
    out = np.zeros(length)
    for det in (-6, 0, 7):
        out += saw_voice(freq, length, det, vibrato=0.004, vib_rate=5.2)
    out = fft_filter(out, lo=70, hi=cutoff, order=2)
    env = adsr(length, a=attack, d=0.3, s=0.85, r=0.7)
    return out * env * vel * 0.12


def flute(freq, dur, vel=0.7, rng=None):
    length = int((dur + 0.4) * SR)
    t = np.arange(length) / SR
    vib = 1 + 0.006 * np.sin(TWO_PI * 5.0 * t) * np.minimum(1, t / 0.4)
    ph = np.cumsum(freq * vib) / SR
    out = np.sin(TWO_PI * ph) + 0.25 * np.sin(TWO_PI * 2 * ph) + 0.08 * np.sin(TWO_PI * 3 * ph)
    breath = fft_filter(rng.standard_normal(length), lo=freq * 1.5, hi=freq * 6) * 0.08
    env = adsr(length, a=0.09, d=0.2, s=0.8, r=0.35)
    return (out + breath) * env * vel * 0.2


def bass(freq, dur, vel=0.8, cutoff=700, saw_mix=0.5):
    length = int((dur + 0.15) * SR)
    t = np.arange(length) / SR
    sine = np.sin(TWO_PI * freq * t)
    saw = fft_filter(saw_voice(freq, length), hi=cutoff, order=3)
    env = adsr(length, a=0.005, d=0.15, s=0.7, r=0.1)
    return (sine * (1 - saw_mix) + saw * saw_mix) * env * vel * 0.45


# drums

def kick(vel=1.0):
    t = t_axis(0.45)
    f = 45 + 95 * np.exp(-t * 28)
    ph = np.cumsum(f) / SR
    body = np.sin(TWO_PI * ph) * np.exp(-t * 7.5)
    click = np.exp(-t * 600) * 0.4
    return (body + click) * vel * 0.9


def snare(rng, vel=1.0):
    t = t_axis(0.3)
    noise = fft_filter(rng.standard_normal(len(t)), lo=1200, hi=7000) * np.exp(-t * 17)
    tone = np.sin(TWO_PI * 190 * t) * np.exp(-t * 22)
    return (noise * 0.9 + tone * 0.5) * vel * 0.45


def clap(rng, vel=1.0):
    t = t_axis(0.25)
    noise = fft_filter(rng.standard_normal(len(t)), lo=900, hi=5000)
    env = np.zeros_like(t)
    for off in (0.0, 0.011, 0.022):
        env += np.where(t >= off, np.exp(-(t - off) * 70), 0)
    env += np.where(t >= 0.03, np.exp(-(t - 0.03) * 14), 0) * 0.7
    return noise * env * vel * 0.35


def hat(rng, vel=1.0, open_=False):
    t = t_axis(0.35 if open_ else 0.08)
    noise = fft_filter(rng.standard_normal(len(t)), lo=7000, hi=15000)
    return noise * np.exp(-t * (9 if open_ else 55)) * vel * 0.22


def shaker(rng, vel=1.0):
    t = t_axis(0.12)
    noise = fft_filter(rng.standard_normal(len(t)), lo=4000, hi=11000)
    env = np.sin(np.pi * np.minimum(1, t / 0.12)) ** 2
    return noise * env * vel * 0.12


def rim(vel=1.0):
    t = t_axis(0.08)
    return np.sin(TWO_PI * 1700 * t) * np.exp(-t * 90) * vel * 0.25


# ----------------------------------------------------------------------------- mixer

class Mix:
    def __init__(self, seconds, seed):
        self.length = int(seconds * SR)
        self.tail = int(6 * SR)
        self.rng = np.random.default_rng(seed)
        self.buses = {}
        self.sends = {}

    def bus(self, name, send=0.0):
        if name not in self.buses:
            self.buses[name] = np.zeros((2, self.length + self.tail))
            self.sends[name] = send
        return self.buses[name]

    def add(self, bus, sig, at, pan=0.0, gain=1.0, send=None):
        b = self.bus(bus, send if send is not None else 0.2)
        start = int(at * SR)
        if start >= b.shape[1]:
            return
        end = min(b.shape[1], start + len(sig))
        seg = sig[:end - start] * gain
        left = np.cos((pan + 1) * np.pi / 4)
        right = np.sin((pan + 1) * np.pi / 4)
        b[0, start:end] += seg * left * np.sqrt(2)
        b[1, start:end] += seg * right * np.sqrt(2)

    def add_stereo(self, bus, left, right, at=0.0, gain=1.0, send=None):
        b = self.bus(bus, send if send is not None else 0.2)
        start = int(at * SR)
        end = min(b.shape[1], start + len(left))
        b[0, start:end] += left[:end - start] * gain
        b[1, start:end] += right[:end - start] * gain

    def duck(self, bus, times, depth=0.6, release=0.18):
        """Sidechain-style ducking of a bus at the given times (seconds)."""
        b = self.buses.get(bus)
        if b is None:
            return
        env = np.ones(b.shape[1])
        r = int(release * SR * 2)
        curve = 1 - depth * np.exp(-np.arange(r) / (release * SR))
        for tm in times:
            s = int(tm * SR)
            e = min(len(env), s + r)
            if s < len(env):
                env[s:e] = np.minimum(env[s:e], curve[:e - s])
        b *= env

    def render(self, reverb_seconds=2.5, wet=0.3, hi_cut=None, lo_cut=None):
        dry = np.zeros((2, self.length + self.tail))
        send = np.zeros_like(dry)
        for name, b in self.buses.items():
            dry += b
            send += b * self.sends[name]
        if wet > 0 and np.any(send):
            ir_len = int(reverb_seconds * SR)
            t = np.arange(ir_len) / SR
            decay = np.exp(-t * 6.9 / reverb_seconds)
            for ch in range(2):
                ir = self.rng.standard_normal(ir_len) * decay
                ir = fft_filter(ir, lo=150, hi=6000)
                ir[: int(0.012 * SR)] *= np.linspace(0, 1, int(0.012 * SR))  # pre-delay feel
                ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
                size = dry.shape[1] + ir_len
                conv = np.fft.irfft(np.fft.rfft(send[ch], size) * np.fft.rfft(ir, size), size)
                dry[ch] += wet * conv[: dry.shape[1]] * 3.0
        out = dry
        # fold everything after the loop point back to the start -> seamless loop
        loop = out[:, : self.length].copy()
        rest = out[:, self.length:]
        loop[:, : rest.shape[1]] += rest[:, : self.length]
        for ch in range(2):
            loop[ch] = fft_filter(loop[ch], lo=lo_cut or 25, hi=hi_cut)
        return loop


def master(stereo, target_rms_db=-17.0, ceiling=0.89):
    rms = np.sqrt(np.mean(stereo ** 2)) + 1e-9
    gain = 10 ** (target_rms_db / 20) / rms
    x = stereo * gain
    # gentle soft clip / limiter
    x = np.tanh(x / ceiling) * ceiling
    peak = np.max(np.abs(x))
    if peak > ceiling:
        x *= ceiling / peak
    return x


def write_mp3(stereo, path, bitrate="96k"):
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav_path = tmp.name
    data = (np.clip(stereo.T, -1, 1) * 32767).astype("<i2")
    with wave.open(wav_path, "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data.tobytes())
    subprocess.run([
        "ffmpeg", "-y", "-loglevel", "error", "-i", wav_path,
        "-codec:a", "libmp3lame", "-b:a", bitrate, "-map_metadata", "-1",
        "-metadata", "artist=Hanita Studio", "-metadata", "copyright=CC0 1.0 - Hanita project",
        path,
    ], check=True)
    os.unlink(wav_path)


# ----------------------------------------------------------------------------- helpers for writing music

def chord(root_name, kind):
    shapes = {
        "maj": [0, 4, 7], "min": [0, 3, 7], "maj7": [0, 4, 7, 11], "min7": [0, 3, 7, 10],
        "7": [0, 4, 7, 10], "min9": [0, 3, 7, 10, 14], "maj9": [0, 4, 7, 11, 14],
        "13": [0, 4, 10, 14, 21], "7b9": [0, 4, 10, 13], "add9": [0, 4, 7, 14], "sus2": [0, 2, 7],
        "sus4": [0, 5, 7], "min11": [0, 3, 7, 10, 17],
    }
    root = n(root_name)
    return [root + i for i in shapes[kind]]


def hz_list(midis, a4=440.0):
    return [midi_hz(m, a4) for m in midis]


# ----------------------------------------------------------------------------- tracks

def track_period_piano_calm():
    bpm, bars = 60, 16
    beat = 60 / bpm
    m = Mix(bars * 4 * beat, 11)
    prog = [("F3", "maj7"), ("A2", "min7"), ("D3", "min9"), ("Bb2", "maj7")]
    melody = [  # (bar offset beat, midi, length beats) written per 4-bar phrase
        [(0, n("A5"), 2), (2, n("G5"), 1), (3, n("F5"), 1)],
        [(0, n("E5"), 3), (3, n("C5"), 1)],
        [(0, n("D5"), 1.5), (1.5, n("E5"), 0.5), (2, n("F5"), 2)],
        [(0, n("D5"), 4)],
    ]
    for bar in range(bars):
        root, kind = prog[bar % 4]
        notes = chord(root, kind)
        t0 = bar * 4 * beat
        m.add("bass", piano(midi_hz(notes[0] - 12), 3.8 * beat, 0.55, m.rng), t0, -0.1, 0.9, 0.35)
        arp = notes[1:] + [notes[1] + 12]
        for i, idx in enumerate([0, 1, 2, 1, 3, 1, 2, 1]):
            if bar % 8 >= 6 and i % 2:  # breathe: thinner arpeggio near phrase ends
                continue
            v = 0.32 + 0.08 * (i % 2 == 0)
            m.add("arp", piano(midi_hz(arp[idx % len(arp)]), 0.9 * beat, v, m.rng), t0 + i * 0.5 * beat,
                  0.25 * np.sin(i), 1.0, 0.45)
        if bar >= 4:
            for (off, midi, length) in melody[bar % 4]:
                m.add("melody", piano(midi_hz(midi), length * beat, 0.6, m.rng), t0 + off * beat, 0.05, 0.85, 0.5)
        if bar % 4 == 0:
            m.add("pad", soft_pad(hz_list(notes[:3]), 16 * beat - 2), t0, 0.0, 0.35, 0.6)
    out = m.render(reverb_seconds=3.5, wet=0.35, hi_cut=9000)
    return master(out, -19)


def track_period_rain_nature():
    seconds = 64
    m = Mix(seconds, 12)
    L = m.length + m.tail
    rain_l = fft_filter(pink_noise(L, m.rng), lo=250, hi=5500)
    rain_r = fft_filter(pink_noise(L, m.rng), lo=250, hi=5500)
    slow = 0.85 + 0.15 * np.sin(TWO_PI * np.arange(L) / SR / 23.0)
    m.add_stereo("rain", rain_l * slow * 0.55, rain_r * slow * 0.55, send=0.05)
    # individual drops on leaves / window
    for _ in range(2600):
        at = m.rng.uniform(0, seconds)
        f = m.rng.uniform(1800, 5200)
        t = t_axis(0.03)
        drop = np.sin(TWO_PI * f * t * (1 + 0.6 * t / 0.03)) * np.exp(-t * m.rng.uniform(120, 260))
        m.add("drops", drop * m.rng.uniform(0.04, 0.16), at, m.rng.uniform(-0.9, 0.9), send=0.3)
    # low rumble far away
    rumble = fft_filter(brown_noise(L, m.rng), lo=25, hi=160)
    swell = np.clip(np.sin(TWO_PI * np.arange(L) / SR / 32.0), 0, None) ** 3
    m.add_stereo("rumble", rumble * swell * 0.35, rumble * swell * 0.35, send=0.2)
    # a soft, distant pad so it reads as music for rest, not only noise
    for i, (root, kind) in enumerate([("E3", "min7"), ("C3", "maj7"), ("G3", "maj7"), ("D3", "add9")]):
        m.add("pad", soft_pad(hz_list(chord(root, kind)[:3]), 15), i * 16, 0.0, 0.22, 0.7)
    out = m.render(reverb_seconds=3.0, wet=0.25, hi_cut=8000)
    return master(out, -21)


def binaural_track(base_hz, beat_hz, drone_root, seconds, seed, a4=440.0, chord_kind="sus2"):
    m = Mix(seconds, seed)
    L = m.length + m.tail
    t = np.arange(L) / SR
    breath = 0.75 + 0.25 * np.sin(TWO_PI * t / 10.0)
    left = np.sin(TWO_PI * base_hz * t) * breath
    right = np.sin(TWO_PI * (base_hz + beat_hz) * t) * breath
    m.add_stereo("tone", left * 0.16, right * 0.16, send=0.1)
    root = n(drone_root)
    notes = chord(drone_root, chord_kind)
    for i in range(0, int(seconds), 16):
        voicing = notes if (i // 16) % 2 == 0 else [notes[0], notes[1] + 12, notes[2]]
        m.add("pad", soft_pad([midi_hz(x, a4) for x in voicing], 15), i, 0.0, 0.5, 0.6)
    m.add("drone", soft_pad([midi_hz(root - 12, a4)], seconds - 2, attack=4, release=3), 0, 0, 0.6, 0.3)
    # sparse singing-bowl strikes
    for k, at in enumerate(np.arange(4, seconds, 12)):
        m.add("bowl", bell(midi_hz(root + 12 + (7 if k % 2 else 0), a4), 4, 0.35,
                           ratios=(1.0, 2.71, 5.12), decays=(0.5, 1.2, 2.5)), at, 0.3 * (-1) ** k, 1, 0.5)
    out = m.render(reverb_seconds=5.0, wet=0.4, hi_cut=7000)
    return master(out, -21)


def track_period_frequency_528():
    return binaural_track(528.0, 4.0, "C3", 64, 13)


def track_luteal_frequency_432():
    # instruments tuned to A4 = 432 Hz, soft alpha (6 Hz) binaural difference
    m_out = binaural_track(216.0, 6.0, "A2", 64, 14, a4=432.0, chord_kind="min7")
    return m_out


def track_period_ambient_rest():
    seconds = 64
    m = Mix(seconds, 15)
    prog = [("E3", "min9"), ("C3", "maj7"), ("G2", "maj9"), ("D3", "sus2")]
    for i, (root, kind) in enumerate(prog):
        notes = chord(root, kind)
        m.add("pad", pad(hz_list(notes), 15.5, attack=3, release=4, cutoff=1100, rng=m.rng), i * 16, 0, 0.9, 0.7)
        m.add("sub", soft_pad([midi_hz(notes[0] - 12)], 15, attack=3, release=3), i * 16, 0, 0.6, 0.2)
    # slow, high glassy notes
    for k, at in enumerate(np.arange(3, seconds, 5.5)):
        midi = [n("B5"), n("G5"), n("E5"), n("D5"), n("F#5")][k % 5]
        m.add("glass", bell(midi_hz(midi), 3, 0.18), at, 0.5 * np.sin(k), 1, 0.8)
    out = m.render(reverb_seconds=6.0, wet=0.45, hi_cut=6000)
    return master(out, -21)


def track_follicular_morning_joy():
    bpm, bars = 96, 24
    beat = 60 / bpm
    m = Mix(bars * 4 * beat, 21)
    prog = [("G2", "maj"), ("D3", "maj"), ("E3", "min"), ("C3", "add9")]
    pattern = [0, 2, 1, 2, 3, 2, 1, 2]  # travis-like picking over chord tones
    for bar in range(bars):
        root, kind = prog[bar % 4]
        notes = chord(root, kind)
        voicing = [notes[0], notes[1] + 12, notes[2] + 12, notes[0] + 24]
        t0 = bar * 4 * beat
        for i, idx in enumerate(pattern):
            at = t0 + i * 0.5 * beat + m.rng.normal(0, 0.006)
            m.add("guitar", pluck(midi_hz(voicing[idx]), 0.9, 0.55 + 0.1 * (i % 4 == 0), m.rng, 0.55),
                  at, -0.3 + 0.2 * (idx % 2), 1, 0.25)
        m.add("bass", pluck(midi_hz(notes[0] - 12), 1.8, 0.5, m.rng, 0.2), t0, 0, 0.8, 0.1)
        if bar >= 8 and bar % 2 == 0:
            line = [n("B4"), n("D5"), n("E5"), n("D5"), n("B4"), n("A4")]
            for j, midi in enumerate(line):
                m.add("lead", bell(midi_hz(midi), 0.6, 0.25, ratios=(1, 2, 3), decays=(3, 6, 9)),
                      t0 + j * beat * 0.66, 0.35, 1, 0.4)
    # birds in the morning
    for _ in range(26):
        at = m.rng.uniform(0, bars * 4 * beat)
        add_bird(m, at)
    out = m.render(reverb_seconds=2.2, wet=0.22, hi_cut=11000)
    return master(out, -17)


def add_bird(m, at):
    rng = m.rng
    base = rng.uniform(2600, 4200)
    pan = rng.uniform(-0.9, 0.9)
    for c in range(rng.integers(2, 6)):
        dur = rng.uniform(0.05, 0.13)
        t = t_axis(dur)
        sweep = base * (1 + rng.uniform(-0.25, 0.35) * t / dur)
        ph = np.cumsum(sweep) / SR
        chirp = np.sin(TWO_PI * ph) * np.sin(np.pi * t / dur) ** 2
        m.add("birds", chirp * rng.uniform(0.03, 0.07), at + c * rng.uniform(0.09, 0.2), pan, send=0.35)


def track_follicular_acoustic_start():
    bpm, bars = 100, 24
    beat = 60 / bpm
    m = Mix(bars * 4 * beat, 22)
    prog = [("D3", "maj"), ("A2", "maj"), ("B2", "min"), ("G2", "maj")]
    strum = [(0, 1, 0.9), (1, 1, 0.6), (1.5, -1, 0.5), (2.5, -1, 0.55), (3, 1, 0.7), (3.5, -1, 0.5)]
    for bar in range(bars):
        root, kind = prog[bar % 4]
        notes = chord(root, kind)
        voicing = [notes[0], notes[2], notes[0] + 12, notes[1] + 12, notes[2] + 12, notes[0] + 24]
        t0 = bar * 4 * beat
        for (off, direction, vel) in strum:
            order = voicing if direction > 0 else voicing[::-1]
            for s, midi in enumerate(order):
                m.add("guitar", pluck(midi_hz(midi), 0.5 * beat * 2, vel * 0.32, m.rng, 0.6, 0.995),
                      t0 + off * beat + s * 0.011, -0.25 + s * 0.1, 1, 0.2)
        m.add("bass", bass(midi_hz(notes[0] - 12), 1.8 * beat, 0.55, 500, 0.3), t0, 0, 1, 0.05)
        m.add("bass", bass(midi_hz(notes[0] - 12), 0.9 * beat, 0.45, 500, 0.3), t0 + 2.5 * beat, 0, 1, 0.05)
        if bar >= 4:
            for i in range(8):
                m.add("perc", shaker(m.rng, 0.8 + 0.4 * (i % 2)), t0 + i * 0.5 * beat, 0.4, 1, 0.1)
            m.add("perc", kick(0.6), t0, 0, 0.8, 0.0)
            m.add("perc", kick(0.45), t0 + 2.5 * beat, 0, 0.8, 0.0)
            m.add("perc", rim(0.8), t0 + beat, -0.2, 1, 0.2)
            m.add("perc", rim(0.8), t0 + 3 * beat, -0.2, 1, 0.2)
        if bar >= 12:
            melody = [n("F#5"), n("E5"), n("D5"), n("E5"), n("F#5"), n("A5"), n("F#5"), n("E5")]
            m.add("lead", flute(midi_hz(melody[(bar * 2) % 8]), 1.6 * beat, 0.45, m.rng), t0, 0.2, 1, 0.4)
            m.add("lead", flute(midi_hz(melody[(bar * 2 + 1) % 8]), 1.6 * beat, 0.45, m.rng), t0 + 2 * beat, 0.2, 1, 0.4)
    out = m.render(reverb_seconds=1.8, wet=0.2, hi_cut=11000)
    return master(out, -16)


def track_follicular_positive_vibes():
    bpm, bars = 80, 20
    beat = 60 / bpm
    m = Mix(bars * 4 * beat, 23)
    scale = [n("C5"), n("D5"), n("E5"), n("G5"), n("A5"), n("C6"), n("D6")]
    motif = [0, 2, 4, 3, 2, 4, 5, 4, 3, 1, 2, 0, 1, 3, 2, 1]
    prog = [("C3", "add9"), ("A2", "min7"), ("F2", "maj9"), ("G2", "sus4")]
    for bar in range(bars):
        t0 = bar * 4 * beat
        notes = chord(*prog[bar % 4])
        if bar % 2 == 0:
            m.add("pad", soft_pad(hz_list([x + 12 for x in notes[:3]]), 8 * beat - 0.5, attack=1.0, release=2.0),
                  t0, 0, 0.45, 0.6)
        m.add("bass", bell(midi_hz(notes[0] - 12), 2.5, 0.5, ratios=(1, 2), decays=(1.2, 3)), t0, 0, 0.9, 0.2)
        for i in range(8):
            idx = motif[(bar * 8 + i) % len(motif)]
            if (bar % 4 == 3 and i >= 6):
                continue
            midi = scale[(idx + (bar // 4) % 2) % len(scale)]
            m.add("kalimba", bell(midi_hz(midi), 0.8, 0.5 + 0.1 * (i % 4 == 0),
                                  ratios=(1.0, 5.93, 13.7), decays=(3.0, 18.0, 35.0)),
                  t0 + i * 0.5 * beat + m.rng.normal(0, 0.008), 0.4 * np.sin(i * 1.3), 1, 0.4)
        if bar >= 4:
            for i in range(4):
                m.add("perc", shaker(m.rng, 0.6), t0 + (i + 0.5) * beat, -0.4, 1, 0.1)
    out = m.render(reverb_seconds=2.8, wet=0.3, hi_cut=11000)
    return master(out, -18)


def track_follicular_gentle_breeze():
    bpm, bars = 72, 18
    beat = 60 / bpm
    m = Mix(bars * 4 * beat, 24)
    prog = [("F3", "maj7"), ("D3", "min7"), ("Bb2", "maj7"), ("C3", "sus4"), ("F3", "add9"), ("A2", "min7"),
            ("G2", "min7"), ("C3", "7"), ("F3", "maj7")]
    phrase = [(0, n("C5"), 1.5), (1.5, n("D5"), 0.5), (2, n("F5"), 2), (4, n("E5"), 1), (5, n("D5"), 1),
              (6, n("C5"), 2), (8, n("A4"), 1.5), (9.5, n("Bb4"), 0.5), (10, n("C5"), 2), (12, n("D5"), 1),
              (13, n("F5"), 1), (14, n("E5"), 2)]
    for bar in range(bars):
        t0 = bar * 4 * beat
        notes = chord(*prog[bar % len(prog)])
        for k, midi in enumerate(notes[:3]):
            m.add("strings", strings(midi_hz(midi), 4 * beat - 0.1, 0.55, attack=0.8, cutoff=1800), t0,
                  -0.4 + 0.4 * k, 1, 0.5)
        m.add("bass", strings(midi_hz(notes[0] - 12), 4 * beat - 0.1, 0.6, attack=0.5, cutoff=900), t0, 0, 1, 0.3)
    for start in (4 * 4 * beat, 10 * 4 * beat):
        for off, midi, length in phrase:
            m.add("flute", flute(midi_hz(midi), length * beat, 0.6, m.rng), start + off * beat, 0.15, 1, 0.45)
    L = m.length + m.tail
    wind = fft_filter(pink_noise(L, m.rng), lo=300, hi=1600)
    gust = 0.5 + 0.5 * np.sin(TWO_PI * np.arange(L) / SR / 13.0) ** 2
    m.add_stereo("wind", wind * gust * 0.12, np.roll(wind, 9000) * gust * 0.12, send=0.1)
    out = m.render(reverb_seconds=3.0, wet=0.32, hi_cut=10000)
    return master(out, -18)


def pop_drums(m, t0, beat, kick_pattern, snare_beats, hats=8, open_hat=True, clap_=False, vel=1.0):
    for k in kick_pattern:
        m.add("drums", kick(vel), t0 + k * beat, 0, 1, 0.0)
    for s in snare_beats:
        m.add("drums", (clap(m.rng, vel) if clap_ else snare(m.rng, vel)), t0 + s * beat, 0, 1, 0.15)
    step = 4 / hats
    for i in range(hats):
        m.add("drums", hat(m.rng, 0.6 + 0.3 * (i % 2 == 0)), t0 + i * step * beat, 0.3, 1, 0.05)
    if open_hat:
        m.add("drums", hat(m.rng, 0.6, open_=True), t0 + 3.5 * beat, 0.3, 1, 0.1)


def track_ovulation_high_energy():
    bpm, bars = 120, 32
    beat = 60 / bpm
    m = Mix(bars * 4 * beat, 31)
    prog = [("A2", "min"), ("F2", "maj"), ("C3", "maj"), ("G2", "maj")]
    hook = [n("E5"), n("E5"), n("D5"), n("C5"), n("D5"), n("E5"), n("G5"), n("E5")]
    kicks = []
    for bar in range(bars):
        t0 = bar * 4 * beat
        notes = chord(*prog[bar % 4])
        intro = bar < 4
        if not intro:
            pop_drums(m, t0, beat, [0, 1, 2, 3], [1, 3], hats=8, clap_=True)
            kicks += [t0 + k * beat for k in range(4)]
        # off-beat supersaw stabs
        for i in range(4):
            m.add("synth", pad(hz_list([x + 12 for x in notes]), 0.35 * beat, 0.01, 0.15, 3200, 5, 22, m.rng),
                  t0 + (i + 0.5) * beat, 0, 1.3, 0.3)
        for i in range(8):
            m.add("bass", bass(midi_hz(notes[0] - 12 if i % 2 == 0 else notes[0]), 0.45 * beat, 0.7, 900, 0.6),
                  t0 + i * 0.5 * beat, 0, 1, 0.0)
        if bar >= 8 and (bar // 4) % 2 == 0:
            for j, midi in enumerate(hook):
                m.add("lead", pluck(midi_hz(midi), 0.45 * beat, 0.6, m.rng, 0.9, 0.997), t0 + j * 0.5 * beat,
                      0.15, 1, 0.35)
        elif bar >= 8:
            m.add("lead", pad(hz_list([notes[2] + 12, notes[0] + 24]), 3.5 * beat, 0.05, 0.4, 4000, 5, 18, m.rng),
                  t0, 0, 0.9, 0.35)
    m.duck("synth", kicks, 0.55)
    m.duck("bass", kicks, 0.7)
    out = m.render(reverb_seconds=1.6, wet=0.2, hi_cut=14000)
    return master(out, -14)


def track_ovulation_empowered_beat():
    bpm, bars = 100, 26
    beat = 60 / bpm
    m = Mix(bars * 4 * beat, 32)
    prog = [("E2", "min7"), ("E2", "min7"), ("A2", "7"), ("A2", "7")]
    bass_line = [(0, 0, 0.5), (0.75, 12, 0.25), (1.5, 10, 0.25), (2, 7, 0.5), (2.75, 5, 0.25), (3.25, 3, 0.5)]
    for bar in range(bars):
        t0 = bar * 4 * beat
        notes = chord(*prog[bar % 4])
        pop_drums(m, t0, beat, [0, 0.75, 2.5], [1, 3], hats=16 if bar >= 4 else 8, open_hat=bar % 2 == 1)
        for (off, interval, length) in bass_line:
            m.add("bass", bass(midi_hz(notes[0] + interval), length * beat, 0.8, 1300, 0.7), t0 + off * beat, 0, 1, 0.0)
        for off in (0.5, 1.75, 2.5, 3.75):
            m.add("keys", electric_piano(midi_hz(notes[1] + 12), 0.25 * beat, 0.55), t0 + off * beat, -0.3, 1, 0.2)
            m.add("keys", electric_piano(midi_hz(notes[3] + 12), 0.25 * beat, 0.5), t0 + off * beat, 0.3, 1, 0.2)
        if bar >= 8 and bar % 4 in (0, 2):
            riff = [n("B4"), n("D5"), n("E5"), n("G5"), n("E5"), n("D5")]
            for j, midi in enumerate(riff):
                m.add("brass", strings(midi_hz(midi), 0.3 * beat, 0.9, attack=0.02, cutoff=4500),
                      t0 + j * 0.5 * beat, 0.1, 1.2, 0.3)
    out = m.render(reverb_seconds=1.4, wet=0.16, hi_cut=14000)
    return master(out, -14)


def track_ovulation_pure_joy():
    bpm, bars = 120, 32
    beat = 60 / bpm
    m = Mix(bars * 4 * beat, 33)
    prog = [("E3", "maj"), ("B2", "maj"), ("C#3", "min"), ("A2", "maj")]
    kicks = []
    for bar in range(bars):
        t0 = bar * 4 * beat
        notes = chord(*prog[bar % 4])
        arp = [notes[0] + 12, notes[1] + 12, notes[2] + 12, notes[0] + 24]
        seq = [0, 1, 2, 3, 2, 1, 2, 3, 0, 1, 2, 3, 2, 3, 1, 2]
        for i, idx in enumerate(seq):
            m.add("arp", pluck(midi_hz(arp[idx]), 0.2, 0.42, m.rng, 0.95, 0.994), t0 + i * 0.25 * beat,
                  0.5 * np.sin(i * 0.8), 1, 0.35)
        m.add("pad", pad(hz_list(notes), 4 * beat - 0.1, 0.3, 0.6, 2500, 4, 16, m.rng), t0, 0, 0.6, 0.4)
        m.add("bass", bass(midi_hz(notes[0] - 12), 1.8 * beat, 0.7, 700, 0.4), t0, 0, 1)
        m.add("bass", bass(midi_hz(notes[0] - 12), 1.5 * beat, 0.6, 700, 0.4), t0 + 2 * beat, 0, 1)
        if bar >= 8:
            pop_drums(m, t0, beat, [0, 1, 2, 3], [1, 3], hats=8, clap_=True, vel=0.85)
            kicks += [t0 + k * beat for k in range(4)]
        if bar >= 16 and bar % 2 == 0:
            melody = [n("G#5"), n("B5"), n("C#6"), n("B5"), n("G#5"), n("F#5"), n("E5")]
            for j, midi in enumerate(melody):
                m.add("lead", bell(midi_hz(midi), 0.4, 0.4, ratios=(1, 2, 4), decays=(3, 6, 10)),
                      t0 + j * 0.5 * beat, -0.15, 1, 0.4)
    m.duck("pad", kicks, 0.45)
    out = m.render(reverb_seconds=2.0, wet=0.22, hi_cut=14000)
    return master(out, -15)


def track_ovulation_sunshine_groove():
    bpm, bars = 110, 32
    beat = 60 / bpm
    m = Mix(bars * 4 * beat, 34)
    prog = [("C3", "maj"), ("G2", "maj"), ("A2", "min"), ("F2", "maj")]
    for bar in range(bars):
        t0 = bar * 4 * beat
        notes = chord(*prog[bar % 4])
        # dembow-light groove
        for k in (0, 2):
            m.add("drums", kick(0.85), t0 + k * beat, 0, 1, 0.0)
        for s in (0.75, 1.5, 2.75, 3.5):
            m.add("drums", rim(0.9), t0 + s * beat, -0.2, 1, 0.15)
        for i in range(8):
            m.add("drums", shaker(m.rng, 0.9 + 0.5 * (i % 2)), t0 + i * 0.5 * beat, 0.35, 1, 0.1)
        for off, interval in ((0, 0), (1.5, 7), (2, 12), (3, 7)):
            m.add("bass", bass(midi_hz(notes[0] - 12 + interval), 0.45 * beat, 0.7, 600, 0.3), t0 + off * beat, 0, 1)
        for off in (0.5, 1.5, 2.5, 3.5):
            for k, midi in enumerate(notes):
                m.add("marimba", marimba(midi_hz(midi + 12), 0.3, 0.45), t0 + off * beat + k * 0.004,
                      -0.35 + 0.35 * k, 1, 0.25)
        if bar >= 8:
            line = [n("E5"), n("G5"), n("A5"), n("G5"), n("E5"), n("D5"), n("C5"), n("D5")]
            if (bar // 2) % 2 == 0:
                for j, midi in enumerate(line):
                    m.add("pan", steel_pan(midi_hz(midi), 0.4, 0.45), t0 + j * 0.5 * beat, 0.25, 1, 0.35)
    out = m.render(reverb_seconds=1.8, wet=0.2, hi_cut=13000)
    return master(out, -15)


def track_luteal_lofi_chill():
    bpm, bars = 72, 20
    beat = 60 / bpm
    m = Mix(bars * 4 * beat, 41)
    prog = [("D3", "min9"), ("G2", "13"), ("C3", "maj9"), ("A2", "7b9")]
    swing = 0.08 * beat
    for bar in range(bars):
        t0 = bar * 4 * beat
        notes = chord(*prog[bar % 4])
        for off in (0, 2.5):
            for k, midi in enumerate(notes[1:]):
                m.add("keys", electric_piano(midi_hz(midi + 12), 1.4 * beat, 0.5), t0 + off * beat + k * 0.012,
                      -0.2 + 0.15 * k, 1, 0.35)
        m.add("bass", bass(midi_hz(notes[0] - 12), 1.6 * beat, 0.6, 350, 0.15), t0, 0, 1, 0.0)
        m.add("bass", bass(midi_hz(notes[0] - 12 + 7), 0.8 * beat, 0.5, 350, 0.15), t0 + 2.5 * beat, 0, 1, 0.0)
        if bar >= 2:
            for k in (0, 1.75, 2.5):
                m.add("drums", kick(0.75), t0 + k * beat, 0, 0.9, 0.0)
            for s in (1, 3):
                m.add("drums", snare(m.rng, 0.7), t0 + s * beat + 0.01, 0, 0.8, 0.25)
            for i in range(8):
                at = t0 + i * 0.5 * beat + (swing if i % 2 else 0)
                m.add("drums", hat(m.rng, 0.45 + 0.25 * (i % 2 == 0)), at, 0.3, 0.9, 0.05)
        if bar >= 8 and bar % 4 in (0, 1):
            mel = [n("A5"), n("G5"), n("F5"), n("E5"), n("D5"), n("C5")]
            for j, midi in enumerate(mel):
                if j % 2 == 0 or bar % 4 == 0:
                    m.add("lead", electric_piano(midi_hz(midi), 0.6 * beat, 0.35), t0 + j * 0.66 * beat, 0.3, 1, 0.45)
    L = m.length + m.tail
    crackle = np.zeros(L)
    idx = m.rng.integers(0, L, 1800)
    crackle[idx] = m.rng.uniform(-1, 1, len(idx))
    crackle = fft_filter(crackle, lo=1500, hi=9000) * 0.5
    hiss = fft_filter(m.rng.standard_normal(L), lo=3000, hi=10000) * 0.012
    m.add_stereo("vinyl", crackle + hiss, np.roll(crackle, 777) + hiss, send=0.0)
    out = m.render(reverb_seconds=1.8, wet=0.22, hi_cut=7500)   # warm, tape-like top end
    return master(out, -17)


def track_luteal_warm_acoustic():
    bpm, bars = 60, 16
    beat = 60 / bpm
    m = Mix(bars * 4 * beat, 42)
    prog = [("Bb2", "maj7"), ("G2", "min7"), ("Eb2", "maj7"), ("F2", "sus4")]
    cello_line = [n("D3"), n("Bb2"), n("G2"), n("A2"), n("F3"), n("D3"), n("Eb3"), n("C3")]
    for bar in range(bars):
        t0 = bar * 4 * beat
        notes = chord(*prog[bar % 4])
        for k, midi in enumerate(notes[1:]):
            m.add("strings", strings(midi_hz(midi + 12), 4 * beat - 0.05, 0.42, attack=1.0, cutoff=2000), t0,
                  -0.5 + 0.5 * k, 1, 0.5)
        m.add("bass", strings(midi_hz(notes[0] - 12), 4 * beat - 0.05, 0.5, attack=0.6, cutoff=700), t0, 0, 1, 0.3)
        if bar >= 2:
            for half in range(2):
                midi = cello_line[(bar * 2 + half) % len(cello_line)]
                m.add("cello", strings(midi_hz(midi), 2 * beat - 0.05, 0.9, attack=0.25, cutoff=2400),
                      t0 + half * 2 * beat, 0.1, 1.1, 0.45)
        if bar >= 6:
            for i, idx in enumerate([0, 2, 3, 2]):
                m.add("guitar", pluck(midi_hz(notes[idx % len(notes)] + 12), 1.2, 0.35, m.rng, 0.3, 0.996),
                      t0 + i * beat, -0.25, 1, 0.3)
    out = m.render(reverb_seconds=3.2, wet=0.35, hi_cut=8500)
    return master(out, -18)


def track_luteal_pink_noise_zen():
    # 4-7-8 breathing: inhale 4 s (noise swells), hold 7 s (steady), exhale 8 s (fades) = 19 s
    cycles = 4
    seconds = 19 * cycles
    m = Mix(seconds, 43)
    L = m.length + m.tail
    t = np.arange(L) / SR
    phase = t % 19
    env = np.where(phase < 4, 0.25 + 0.75 * np.sin(np.pi / 2 * phase / 4) ** 2,
                   np.where(phase < 11, 1.0, 0.25 + 0.75 * np.cos(np.pi / 2 * (phase - 11) / 8) ** 2))
    noise_l = fft_filter(pink_noise(L, m.rng), lo=80, hi=4000)
    noise_r = fft_filter(pink_noise(L, m.rng), lo=80, hi=4000)
    m.add_stereo("noise", noise_l * env * 0.35, noise_r * env * 0.35, send=0.05)
    m.add("drone", soft_pad([midi_hz(n("D2")), midi_hz(n("A2"))], seconds - 2, attack=4, release=3), 0, 0, 0.6, 0.3)
    for c in range(cycles):
        base = c * 19
        m.add("cue", bell(midi_hz(n("A5")), 2.5, 0.22), base, -0.2, 1, 0.5)          # breathe in
        m.add("cue", bell(midi_hz(n("E5")), 2.5, 0.16), base + 4, 0.0, 1, 0.5)       # hold
        m.add("cue", bell(midi_hz(n("D5")), 3.0, 0.18), base + 11, 0.2, 1, 0.5)      # breathe out
    out = m.render(reverb_seconds=4.0, wet=0.3, hi_cut=7000)
    return master(out, -21)


def track_nature_ocean_waves():
    seconds = 72
    m = Mix(seconds, 51)
    L = m.length + m.tail
    t = np.arange(L) / SR
    for ch_shift in (0, 1):
        body = fft_filter(brown_noise(L, m.rng), lo=40, hi=900)
        foam = fft_filter(pink_noise(L, m.rng), lo=900, hi=7000)
        period = 9.0 + ch_shift * 0.7
        ph = ((t + ch_shift * 2.3) % period) / period
        swell = np.sin(np.pi * np.clip(ph / 0.6, 0, 1)) ** 2
        crash = np.where(ph > 0.45, np.exp(-(ph - 0.45) * 7) * np.clip((ph - 0.45) / 0.05, 0, 1), 0)
        sig = body * (0.25 + 0.6 * swell) + foam * crash * 0.6
        if ch_shift == 0:
            left = sig
        else:
            right = sig
    m.add_stereo("sea", left * 0.6, right * 0.6, send=0.08)
    for i, (root, kind) in enumerate([("D3", "maj7"), ("B2", "min7"), ("G2", "maj9"), ("A2", "sus4")]):
        m.add("pad", soft_pad(hz_list(chord(root, kind)[:3]), 17), i * 18, 0, 0.18, 0.6)
    out = m.render(reverb_seconds=3.0, wet=0.2, hi_cut=9000)
    return master(out, -20)


def track_nature_forest_birds():
    seconds = 64
    m = Mix(seconds, 52)
    L = m.length + m.tail
    stream_l = fft_filter(m.rng.standard_normal(L), lo=500, hi=3500)
    stream_r = fft_filter(m.rng.standard_normal(L), lo=500, hi=3500)
    g = fft_filter(m.rng.standard_normal(L), hi=6)
    gurgle = 0.7 + 0.3 * g / (np.max(np.abs(g)) + 1e-9)
    m.add_stereo("stream", stream_l * gurgle * 0.05, stream_r * gurgle * 0.05, send=0.1)
    leaves = fft_filter(pink_noise(L, m.rng), lo=1500, hi=6000)
    m.add_stereo("leaves", leaves * 0.04, np.roll(leaves, 5000) * 0.04, send=0.1)
    for _ in range(70):
        add_bird(m, m.rng.uniform(0, seconds))
    for k, at in enumerate(np.arange(2, seconds, 8)):
        m.add("flute", flute(midi_hz([n("G5"), n("E5"), n("A5"), n("D5")][k % 4]), 2.5, 0.25, m.rng), at,
              -0.3, 1, 0.6)
    out = m.render(reverb_seconds=2.4, wet=0.25, hi_cut=12000)
    return master(out, -20)


TRACKS = {
    "period_piano_calm": track_period_piano_calm,
    "period_rain_nature": track_period_rain_nature,
    "period_frequency_528": track_period_frequency_528,
    "period_ambient_rest": track_period_ambient_rest,
    "follicular_morning_joy": track_follicular_morning_joy,
    "follicular_acoustic_start": track_follicular_acoustic_start,
    "follicular_positive_vibes": track_follicular_positive_vibes,
    "follicular_gentle_breeze": track_follicular_gentle_breeze,
    "ovulation_high_energy": track_ovulation_high_energy,
    "ovulation_empowered_beat": track_ovulation_empowered_beat,
    "ovulation_pure_joy": track_ovulation_pure_joy,
    "ovulation_sunshine_groove": track_ovulation_sunshine_groove,
    "luteal_lofi_chill": track_luteal_lofi_chill,
    "luteal_frequency_432": track_luteal_frequency_432,
    "luteal_warm_acoustic": track_luteal_warm_acoustic,
    "luteal_pink_noise_zen": track_luteal_pink_noise_zen,
    "nature_ocean_waves": track_nature_ocean_waves,
    "nature_forest_birds": track_nature_forest_birds,
}


def main():
    out_dir = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
        os.path.dirname(__file__), "..", "..", "app", "src", "main", "res", "raw")
    wanted = sys.argv[2:] or list(TRACKS)
    os.makedirs(out_dir, exist_ok=True)
    for name in wanted:
        audio = TRACKS[name]()
        path = os.path.join(out_dir, name + ".mp3")
        write_mp3(audio, path)
        print(f"{name}: {audio.shape[1] / SR:.1f}s -> {path}")


if __name__ == "__main__":
    main()
