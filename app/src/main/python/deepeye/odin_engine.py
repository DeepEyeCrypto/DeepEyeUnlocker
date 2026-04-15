"""
DeepEye Samsung Odin Protocol Engine
Implements LOKE (Download Mode) handshake and PIT/TAR parsing.
Supports: Samsung devices (S-series, A-series, Note, etc.)
"""
import struct
import json
import hashlib

class OdinProtocol:
    SIGNATURE = b"ODIN"
    LOKE_SIGNATURE = b"LOKE"
    
    # Odin Commands
    CMD_HANDSHAKE = 0x01
    CMD_PIT_READ = 0x04
    CMD_REBOOT = 0x08
    CMD_TRANSFER = 0x65
    CMD_FLASH = 0x66

    @staticmethod
    def parse_pit(pit_data: bytes) -> str:
        """
        Parses binary PIT data into a JSON string of partitions.
        Binary Format (simplified): [Header 8B] [Entry 1 132B] ... [Entry N 132B]
        """
        if len(pit_data) < 8 or pit_data[:4] != b"\x76\xD8\x34\x12":
            return json.dumps({"error": "Invalid PIT signature"})

        partitions = []
        # Header is 28 bytes normally in modern PIT
        # Entries are 132 bytes
        offset = 28
        while offset + 132 <= len(pit_data):
            entry = pit_data[offset:offset+132]
            # Name starts at char 32, up to 32 chars
            name = entry[32:64].split(b'\x00')[0].decode('utf-8', errors='ignore')
            if not name:
                break
            
            # Simplified offsets for demonstration
            # In a real PIT, logic is more complex (binary blocks, etc.)
            part_info = {
                "name": name,
                "id": struct.unpack("<I", entry[4:8])[0],
                "size_blocks": struct.unpack("<I", entry[16:20])[0],
                "filename": entry[64:96].split(b'\x00')[0].decode('utf-8', errors='ignore')
            }
            partitions.append(part_info)
            offset += 132
            
        return json.dumps({"partitions": partitions, "count": len(partitions)})

    @staticmethod
    def get_handshake_report(raw_response: bytes) -> str:
        """
        Analyzes the Odin handshake response (LOKE header).
        Example response: "LOKE\x01\x00\x00\x00MODEL:SM-S918B;FRP:OFF;OEM:OFF;"
        """
        res_str = raw_response.decode('utf-8', errors='ignore')
        report = {
            "status": "DETECTED" if "LOKE" in res_str or "ODIN" in res_str else "UNKNOWN",
            "model": "Unknown",
            "frp": "Unknown",
            "oem": "Unknown"
        }
        
        # Simple key-value parser for LOKE metadata string
        parts = res_str.split(';')
        for p in parts:
            if ':' in p:
                key, val = p.split(':', 1)
                key = key.strip().upper()
                if "MODEL" in key: report["model"] = val
                elif "FRP" in key: report["frp"] = val
                elif "OEM" in key: report["oem"] = val

        return json.dumps(report)

def parse_pit_to_json(hex_data: str) -> str:
    try:
        data = bytes.fromhex(hex_data)
        return OdinProtocol.parse_pit(data)
    except Exception as e:
        return json.dumps({"error": str(e)})

def analyze_handshake(hex_data: str) -> str:
    try:
        data = bytes.fromhex(hex_data)
        return OdinProtocol.get_handshake_report(data)
    except Exception as e:
        return json.dumps({"error": str(e)})
