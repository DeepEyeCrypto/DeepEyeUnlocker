def luhn_check(imei: str) -> bool:
    """Luhn algorithm IMEI validation."""
    if not imei.isdigit() or len(imei) != 15:
        return False
    total = 0
    for i, d in enumerate(reversed(imei)):
        n = int(d)
        if i % 2 == 1:
            n *= 2
            if n > 9:
                n -= 9
        total += n
    return total % 10 == 0

def extract_tac(imei: str) -> str:
    """Extract Type Allocation Code (first 8 digits)."""
    return imei[:8] if len(imei) >= 8 else ""

def get_manufacturer_from_tac(tac: str) -> str:
    """Known TAC → manufacturer mapping."""
    tac_map = {
        "35279310": "Apple iPhone 14 Pro",
        "35279311": "Apple iPhone 14 Pro Max",
        "35845519": "Apple iPhone 15 Pro",
        "86631903": "Samsung Galaxy S24",
        "35674511": "Realme 14x",
    }
    return tac_map.get(tac, f"Unknown (TAC: {tac})")
