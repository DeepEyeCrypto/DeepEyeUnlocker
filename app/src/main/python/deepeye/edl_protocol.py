"""
DeepEye EDL Protocol Engine
Qualcomm Emergency Download Mode
Protocols: Sahara v2 + Firehose XML
Supports: MSM8x, SDM, SM series
Reference: bkerler/edl open-source spec
"""
import hashlib
import json
import struct
from enum import Enum

# ── Sahara Protocol ────────────────────────────────
SAHARA_VERSION       = 2
SAHARA_VERSION_COMPAT= 1
SAHARA_MAX_PACKET    = 0x400

class SaharaCommand(Enum):
    HELLO              = 0x01
    HELLO_RESPONSE     = 0x02
    READ_DATA          = 0x03
    END_TRANSFER       = 0x04
    DONE               = 0x05
    DONE_RESPONSE      = 0x06
    RESET              = 0x07
    RESET_RESPONSE     = 0x08
    MEMORY_DEBUG       = 0x09
    MEMORY_READ        = 0x0A
    CMD_READY          = 0x0B
    SWITCH_MODE        = 0x0C
    EXECUTE            = 0x0D
    EXECUTE_RESPONSE   = 0x0E
    EXECUTE_DATA       = 0x0F
    MEMORY_DEBUG64     = 0x10
    MEMORY_READ64      = 0x11
    RESET_MULTIIMAGE   = 0x12

class SaharaMode(Enum):
    IMAGE_TX_PENDING   = 0x00
    IMAGE_TX_COMPLETE  = 0x01
    MEMORY_DEBUG       = 0x02
    CLIENT_CMD_EXECUTE = 0x03

class SaharaStatus(Enum):
    SUCCESS                     = 0x00
    INVALID_CMD                 = 0x01
    PROTOCOL_MISMATCH           = 0x02
    INVALID_TARGET_PROTOCOL     = 0x03
    INVALID_HOST_PROTOCOL       = 0x04
    INVALID_PACKET_SIZE         = 0x05
    UNEXPECTED_IMAGE_ID         = 0x06
    INVALID_HEADER_SIZE         = 0x07
    INVALID_DATA_SIZE           = 0x08
    INVALID_IMAGE_TYPE          = 0x09
    INVALID_TX_LENGTH           = 0x0A
    INVALID_RX_LENGTH           = 0x0B
    TX_RX_ERROR                 = 0x0C
    READ_DATA_ERROR             = 0x0D
    UNSUPPORTED_NUM_PHDRS       = 0x0E
    INVALID_PHDR_SIZE           = 0x0F
    MULTIPLE_SHARED_SEG         = 0x10
    UNINIT_PHDR_LOC             = 0x11
    FAILED_TO_AUTHENTICATE      = 0x12
    INVALID_IMG_HASH_TABLE_SIZE = 0x13
    FAILED_TO_INIT_CRYPTO       = 0x14
    FAILED_HASH_VERIFICATION    = 0x15
    FAILED_HASH_TABLE_VER       = 0x16
    WAIT_TIMEOUT                = 0x17

# ── Firehose Protocol ──────────────────────────────
class FirehoseCommand(Enum):
    CONFIGURE      = "configure"
    PROGRAM        = "program"
    ERASE          = "erase"
    READ           = "read"
    NOP            = "nop"
    POWER          = "power"
    FIRMWARE_WRITE = "firmwarewrite"
    GET_DIGEST     = "getdigest"
    PEEK           = "peek"
    POKE           = "poke"
    PATCH          = "patch"
    SET_BOOTABLE   = "setbootablestorageDrive"
    USBSPEED       = "usbspeed"

FIREHOSE_STORAGE_TYPES = [
    "ufs", "emmc", "nvme", "spinor", "nand"
]

# Qualcomm chip → Firehose programmer mapping
CHIP_PROGRAMMER_MAP = {
    "MSM8909":  "prog_emmc_firehose_8909.mbn",
    "MSM8916":  "prog_emmc_firehose_8916.mbn",
    "MSM8953":  "prog_emmc_firehose_8953.mbn",
    "MSM8998":  "prog_ufs_firehose_8998_ddr.elf",
    "SDM630":   "prog_ufs_firehose_sdm630.elf",
    "SDM660":   "prog_ufs_firehose_sdm660_ddr.elf",
    "SDM845":   "prog_ufs_firehose_sdm845_ddr.elf",
    "SM6125":   "prog_firehose_sm6125.elf",
    "SM7125":   "prog_firehose_sm7125.elf",
    "SM8150":   "prog_firehose_sm8150.elf",
    "SM8250":   "prog_firehose_sm8250_ddr.elf",
    "SM8350":   "prog_firehose_sm8350.elf",
    "SM8450":   "prog_firehose_sm8450.elf",
    "SM8550":   "prog_firehose_sm8550.elf",
    "SM8650":   "prog_firehose_sm8650.elf",
}

# Common partition table for Snapdragon devices
STANDARD_PARTITIONS = [
    {"name": "xbl",        "slot": "a", "critical": True,  "type": "emmc"},
    {"name": "xbl_config", "slot": "a", "critical": True,  "type": "emmc"},
    {"name": "abl",        "slot": "a", "critical": True,  "type": "emmc"},
    {"name": "aop",        "slot": "a", "critical": True,  "type": "emmc"},
    {"name": "boot",       "slot": "a", "critical": True,  "type": "ufs"},
    {"name": "vendor_boot","slot": "a", "critical": True,  "type": "ufs"},
    {"name": "dtbo",       "slot": "a", "critical": False, "type": "ufs"},
    {"name": "vbmeta",     "slot": "a", "critical": True,  "type": "ufs"},
    {"name": "system",     "slot": "a", "critical": True,  "type": "ufs"},
    {"name": "vendor",     "slot": "a", "critical": True,  "type": "ufs"},
    {"name": "product",    "slot": "a", "critical": False, "type": "ufs"},
    {"name": "userdata",   "slot": "",  "critical": False, "type": "ufs"},
    {"name": "frp",        "slot": "",  "critical": False, "type": "emmc"},
    {"name": "persist",    "slot": "",  "critical": False, "type": "ufs"},
    {"name": "misc",       "slot": "",  "critical": False, "type": "ufs"},
    {"name": "metadata",   "slot": "",  "critical": False, "type": "ufs"},
]


# ── Sahara Packet Builders ─────────────────────────

def build_sahara_hello_response(
    version: int = SAHARA_VERSION,
    version_compat: int = SAHARA_VERSION_COMPAT,
    mode: int = SaharaMode.IMAGE_TX_PENDING.value
) -> bytes:
    """
    Build Sahara HELLO_RESPONSE packet.
    Sent from host → device after receiving HELLO.
    Format: cmd(4) len(4) version(4) compat(4) status(4) mode(4)
    """
    cmd    = SaharaCommand.HELLO_RESPONSE.value
    length = 0x30  # 48 bytes
    status = SaharaStatus.SUCCESS.value
    reserved = b'\x00' * 16  # 4 reserved uint32s
    packet = struct.pack(
        '<IIIIII',
        cmd, length, version,
        version_compat, status, mode
    ) + reserved
    return packet

def parse_sahara_hello(data: bytes) -> dict:
    """
    Parse incoming Sahara HELLO packet from device.
    Returns chip version, mode, and status.
    """
    if len(data) < 0x30:
        return {"error": f"Packet too short: {len(data)} bytes"}
    try:
        cmd, length, version, compat, status, mode = \
            struct.unpack('<IIIIII', data[:24])
        return {
            "command":          cmd,
            "cmd_name":         SaharaCommand(cmd).name
                                if cmd in [e.value for e in SaharaCommand]
                                else f"UNKNOWN_0x{cmd:02X}",
            "length":           length,
            "version":          version,
            "version_compat":   compat,
            "status":           status,
            "mode":             mode,
            "mode_name":        SaharaMode(mode).name
                                if mode in [e.value for e in SaharaMode]
                                else f"UNKNOWN_0x{mode:02X}",
            "compatible":       version >= SAHARA_VERSION_COMPAT,
        }
    except Exception as e:
        return {"error": str(e)}

def build_sahara_done() -> bytes:
    """Build Sahara DONE packet — signal end of transfer."""
    return struct.pack('<II',
        SaharaCommand.DONE.value,
        0x08  # 8 bytes total
    )

def build_sahara_reset() -> bytes:
    """Build Sahara RESET packet."""
    return struct.pack('<II',
        SaharaCommand.RESET.value,
        0x08
    )

def build_sahara_switch_mode(mode: int) -> bytes:
    """
    Build SWITCH_MODE packet.
    Used to switch device to Firehose/memory debug mode.
    """
    return struct.pack('<III',
        SaharaCommand.SWITCH_MODE.value,
        0x0C, mode
    )


# ── Firehose XML Builders ──────────────────────────

def build_firehose_configure_xml(
    memory_name: str = "ufs",
    verbose: int = 0,
    always_validate: int = 1,
    zlp_aware: int = 1,
    skip_storage_init: int = 0,
    target_name: str = ""
) -> str:
    """
    Build Firehose configure XML.
    First command sent after Sahara handshake.
    """
    return (
        '<?xml version="1.0" ?>\n'
        '<data>\n'
        f'  <configure TargetName="{target_name}" '
        f'verbose="{verbose}" '
        f'AlwaysValidate="{always_validate}" '
        f'MaxDigestTableSizeInBytes="2048" '
        f'MaxPayloadSizeToTargetInBytes="1048576" '
        f'ZlpAwareHost="{zlp_aware}" '
        f'SkipStorageInit="{skip_storage_init}" '
        f'MemoryName="{memory_name}"/>\n'
        '</data>'
    )

def build_firehose_program_xml(
    filename: str,
    start_sector: int,
    num_partition_sectors: int,
    physical_partition: int = 0,
    sector_size: int = 512,
    sparse: str = "false",
    read_back_verify: str = "0"
) -> str:
    """
    Build Firehose program XML for flashing a partition.
    """
    return (
        '<?xml version="1.0" ?>\n'
        '<data>\n'
        f'  <program SECTOR_SIZE_IN_BYTES="{sector_size}" '
        f'filename="{filename}" '
        f'label="{filename.split(".")[0]}" '
        f'num_partition_sectors="{num_partition_sectors}" '
        f'physical_partition_number="{physical_partition}" '
        f'read_back_verify="{read_back_verify}" '
        f'sparse="{sparse}" '
        f'start_sector="{start_sector}"/>\n'
        '</data>'
    )

def build_firehose_erase_xml(
    partition_name: str,
    physical_partition: int = 0,
    storage: str = "ufs"
) -> str:
    """Build Firehose erase partition XML."""
    return (
        '<?xml version="1.0" ?>\n'
        '<data>\n'
        f'  <erase '
        f'SECTOR_SIZE_IN_BYTES="4096" '
        f'label="{partition_name}" '
        f'physical_partition_number="{physical_partition}" '
        f'storage="{storage}"/>\n'
        '</data>'
    )

def build_firehose_read_xml(
    filename: str,
    start_sector: int,
    num_sectors: int,
    physical_partition: int = 0,
    sector_size: int = 512
) -> str:
    """Build Firehose read (dump) partition XML."""
    return (
        '<?xml version="1.0" ?>\n'
        '<data>\n'
        f'  <read SECTOR_SIZE_IN_BYTES="{sector_size}" '
        f'filename="{filename}" '
        f'num_partition_sectors="{num_sectors}" '
        f'physical_partition_number="{physical_partition}" '
        f'start_sector="{start_sector}"/>\n'
        '</data>'
    )

def build_firehose_power_xml(
    value: str = "reset",
    delay_ms: int = 3000
) -> str:
    """
    Build Firehose power XML.
    value: 'reset' | 'off' | 'on'
    """
    return (
        '<?xml version="1.0" ?>\n'
        '<data>\n'
        f'  <power DelayInSeconds="{delay_ms // 1000}" '
        f'value="{value}"/>\n'
        '</data>'
    )

def build_firehose_getdigest_xml(
    filename: str,
    start_sector: int,
    num_sectors: int,
    physical_partition: int = 0
) -> str:
    """Build Firehose getdigest for verification."""
    return (
        '<?xml version="1.0" ?>\n'
        '<data>\n'
        f'  <getdigest SECTOR_SIZE_IN_BYTES="4096" '
        f'filename="{filename}" '
        f'num_partition_sectors="{num_sectors}" '
        f'physical_partition_number="{physical_partition}" '
        f'start_sector="{start_sector}"/>\n'
        '</data>'
    )


# ── EDL Session Helpers ────────────────────────────

def detect_chip_from_usb(
    vid: int,
    pid: int
) -> dict:
    """
    Detect Qualcomm chip from USB VID:PID.
    VID 0x05C6 = Qualcomm
    """
    QUALCOMM_VID = 0x05C6
    EDL_PID      = 0x9008
    DIAG_PID     = 0x900E

    is_qualcomm = (vid == QUALCOMM_VID)
    is_edl      = (vid == QUALCOMM_VID and pid == EDL_PID)
    is_diag     = (vid == QUALCOMM_VID and pid == DIAG_PID)

    return {
        "vendor":       "Qualcomm" if is_qualcomm else "Unknown",
        "vid":          f"0x{vid:04X}",
        "pid":          f"0x{pid:04X}",
        "is_edl_mode":  is_edl,
        "is_diag_mode": is_diag,
        "mode":         "EDL" if is_edl
                        else "DIAG" if is_diag
                        else "NORMAL" if is_qualcomm
                        else "NON_QUALCOMM",
        "action":       "Ready for Firehose" if is_edl
                        else "Send EDL command" if is_qualcomm
                        else "Not a Qualcomm device"
    }

def get_programmer_for_chip(chip_name: str) -> dict:
    """
    Return Firehose programmer filename for chip.
    """
    chip_upper = chip_name.upper().replace(" ", "")
    prog = CHIP_PROGRAMMER_MAP.get(chip_upper)
    if prog:
        return {
            "chip":       chip_upper,
            "programmer": prog,
            "found":      True,
            "type":       "ufs" if ".elf" in prog else "emmc"
        }
    # Fuzzy match
    for key, val in CHIP_PROGRAMMER_MAP.items():
        if chip_upper in key or key in chip_upper:
            return {
                "chip":       key,
                "programmer": val,
                "found":      True,
                "fuzzy":      True,
                "type":       "ufs" if ".elf" in val else "emmc"
            }
    return {
        "chip":       chip_upper,
        "programmer": f"prog_firehose_{chip_upper.lower()}.elf",
        "found":      False,
        "note":       "Generic name — verify with device OFP/firmware"
    }

def build_flash_sequence(
    chip: str,
    partitions_to_flash_json: str,
    storage: str = "ufs",
    slot: str = "a",
    session_id: str = ""
) -> dict:
    """
    Build complete EDL flash sequence.
    Returns ordered XML command list.
    """
    partitions_to_flash = json.loads(partitions_to_flash_json)
    programmer = get_programmer_for_chip(chip)
    steps = []

    # Step 1: Sahara hello exchange
    steps.append({
        "phase":     "sahara",
        "step":      1,
        "action":    "SAHARA_HELLO_RESPONSE",
        "data":      build_sahara_hello_response().hex(),
        "note":      "Respond to device hello"
    })

    # Step 2: Send programmer
    steps.append({
        "phase":     "sahara",
        "step":      2,
        "action":    "SEND_PROGRAMMER",
        "filename":  programmer["programmer"],
        "note":      f"Upload {programmer['programmer']} to device RAM"
    })

    # Step 3: Sahara done
    steps.append({
        "phase":     "sahara",
        "step":      3,
        "action":    "SAHARA_DONE",
        "data":      build_sahara_done().hex(),
        "note":      "Signal programmer upload complete"
    })

    # Step 4: Firehose configure
    steps.append({
        "phase":     "firehose",
        "step":      4,
        "action":    "CONFIGURE",
        "xml":       build_firehose_configure_xml(
                         memory_name=storage
                     ),
        "note":      f"Configure {storage.upper()} storage"
    })

    # Step 5+: Flash partitions
    sector = 0
    for i, part in enumerate(partitions_to_flash):
        part_name = part.get("name", f"part_{i}")
        filename  = f"{part_name}_{slot}.img" if \
                    part.get("slot") else f"{part_name}.img"
        num_sectors = part.get("sectors", 8192)
        steps.append({
            "phase":    "firehose",
            "step":     5 + i,
            "action":   "PROGRAM",
            "partition":part_name,
            "xml":      build_firehose_program_xml(
                            filename, sector,
                            num_sectors
                        ),
            "note":     f"Flash {filename}"
        })
        sector += num_sectors

    # Final: Power reset
    steps.append({
        "phase":  "firehose",
        "step":   5 + len(partitions_to_flash),
        "action": "POWER_RESET",
        "xml":    build_firehose_power_xml("reset"),
        "note":   "Reset device after flash"
    })

    return {
        "chip":        chip,
        "programmer":  programmer["programmer"],
        "storage":     storage,
        "slot":        slot,
        "total_steps": len(steps),
        "steps":       steps,
        "session_id":  session_id
    }

def generate_edl_report(
    vid: int, pid: int,
    chip: str, storage: str,
    session_id: str
) -> str:
    """Generate EDL session report as JSON string."""
    detection  = detect_chip_from_usb(vid, pid)
    programmer = get_programmer_for_chip(chip)
    partitions = [
        p for p in STANDARD_PARTITIONS
        if p["type"] == storage or storage == "auto"
    ]
    return json.dumps({
        "session_id":  session_id,
        "tool":        "DeepEye Unlocker v2027.18",
        "usb":         detection,
        "chip":        chip,
        "programmer":  programmer,
        "storage":     storage,
        "partitions":  partitions,
        "partition_count": len(partitions),
        "ready":       detection["is_edl_mode"]
    }, indent=2)
