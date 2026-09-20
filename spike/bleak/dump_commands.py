"""Write one golden fixture per command to spike/fixtures/commands/. No BLE needed.

Usage: python spike/bleak/dump_commands.py
Re-run whenever catproto.py constants change.
"""

from pathlib import Path

from catproto import draw_mode, feed, get_info, get_state, hexdump, set_energy, set_quality

FIXTURES = Path(__file__).resolve().parent.parent / "fixtures" / "commands"

COMMANDS = {
    "get_state": get_state(),
    "get_info": get_info(),
    "set_quality_default": set_quality(),
    "set_energy_default": set_energy(),
    "draw_mode_image": draw_mode(True),
    "draw_mode_text": draw_mode(False),
    "feed_default": feed(),
}

if __name__ == "__main__":
    FIXTURES.mkdir(parents=True, exist_ok=True)
    for name, pkt in COMMANDS.items():
        (FIXTURES / f"{name}.hex").write_text(hexdump(pkt) + "\n")
        print(f"{name:22s} {hexdump(pkt)}")
    print(f"\nWrote {len(COMMANDS)} fixtures to {FIXTURES}")
