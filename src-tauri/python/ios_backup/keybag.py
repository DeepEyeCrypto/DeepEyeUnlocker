from __future__ import annotations


def parse_tlv(payload: bytes | bytearray | memoryview | str) -> list[dict[str, object]]:
    """Parse a big-endian keybag TLV blob into a list of tag/value dictionaries."""
    if isinstance(payload, str):
        normalized = payload.replace(" ", "").replace(":", "")
        try:
            raw = bytes.fromhex(normalized)
        except ValueError as exc:
            raise ValueError("TLV payload string must be hex encoded") from exc
    else:
        raw = bytes(payload)

    entries: list[dict[str, object]] = []
    offset = 0
    while offset < len(raw):
        if offset + 8 > len(raw):
            raise ValueError(f"Truncated TLV header at offset {offset}")

        tag = int.from_bytes(raw[offset:offset + 4], "big")
        offset += 4
        length = int.from_bytes(raw[offset:offset + 4], "big")
        offset += 4

        end = offset + length
        if end > len(raw):
            raise ValueError(f"TLV value overruns payload for tag 0x{tag:08X}")

        value = raw[offset:end]
        entries.append(
            {
                "tag": f"0x{tag:08X}",
                "length": length,
                "value": value.hex(),
            }
        )
        offset = end

    return entries