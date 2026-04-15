"""
DeepEye QFIL Tools
Qualcomm Flash Image Loader compatible functions
Rawprogram XML parser + Patch XML generator
"""
import hashlib
import json
import xml.etree.ElementTree as ET

def parse_rawprogram_xml(xml_str: str) -> str:
    """
    Parse rawprogram_unsparse.xml from QFIL package.
    Returns JSON list of partition flash entries.
    """
    results = []
    if not xml_str.strip():
        return json.dumps(results)
    try:
        root = ET.fromstring(xml_str)
        for prog in root.findall('.//program'):
            results.append({
                "label":       prog.get("label", ""),
                "filename":    prog.get("filename", ""),
                "start_sector":int(prog.get("start_sector", 0)),
                "num_sectors": int(prog.get(
                    "num_partition_sectors", 0)),
                "sector_size": int(prog.get(
                    "SECTOR_SIZE_IN_BYTES", 512)),
                "sparse":      prog.get("sparse", "false"),
                "physical_partition": int(prog.get(
                    "physical_partition_number", 0)),
                "read_back_verify": prog.get(
                    "read_back_verify", "0")
            })
    except ET.ParseError as e:
        results.append({"error": str(e)})
    return json.dumps(results)

def parse_patch_xml(xml_str: str) -> str:
    """
    Parse patch0.xml from QFIL package.
    Returns JSON list of sector patches to apply.
    """
    patches = []
    if not xml_str.strip():
        return json.dumps(patches)
    try:
        root = ET.fromstring(xml_str)
        for patch in root.findall('.//patch'):
            patches.append({
                "filename":    patch.get("filename", ""),
                "start_sector":int(patch.get("start_sector", 0)),
                "offset":      int(patch.get("byte_offset", 0)),
                "size":        int(patch.get("size_in_bytes", 4)),
                "value":       patch.get("value", "0x0"),
                "what":        patch.get("what", ""),
            })
    except ET.ParseError as e:
        patches.append({"error": str(e)})
    return json.dumps(patches)

def generate_rawprogram_xml(partitions_json: str) -> str:
    """
    Generate rawprogram_unsparse.xml from partition list.
    Used for creating custom flash packages.
    """
    partitions = json.loads(partitions_json)
    lines = ['<?xml version="1.0" ?>', '<data>']
    sector = 0
    for p in partitions:
        name      = p.get("name", "unknown")
        num_sects = p.get("sectors", 8192)
        size      = p.get("sector_size", 4096)
        filename  = p.get("filename", f"{name}.img")
        lines.append(
            f'  <program SECTOR_SIZE_IN_BYTES="{size}" '
            f'filename="{filename}" '
            f'label="{name}" '
            f'num_partition_sectors="{num_sects}" '
            f'physical_partition_number="0" '
            f'read_back_verify="0" '
            f'sparse="false" '
            f'start_sector="{sector}"/>'
        )
        sector += num_sects
    lines.append('</data>')
    return '\n'.join(lines)

def validate_flash_package(file_list_json: str) -> str:
    """
    Validate a QFIL flash package (OFP/zip contents).
    Checks for required files.
    """
    file_list = json.loads(file_list_json)
    required = [
        "prog_firehose",     # programmer .mbn/.elf
        "rawprogram",        # rawprogram_unsparse.xml
        "patch0.xml",        # patch file
    ]
    optional = [
        "gpt_main0.bin",
        "gpt_backup0.bin",
        "contents.xml",
        "partition.xml",
    ]
    missing = []
    found   = []
    for req in required:
        if any(req.lower() in f.lower() for f in file_list):
            found.append(req)
        else:
            missing.append(req)
    opt_found = [
        opt for opt in optional
        if any(opt.lower() in f.lower() for f in file_list)
    ]
    return json.dumps({
        "valid":         len(missing) == 0,
        "missing":       missing,
        "found":         found,
        "optional_found":opt_found,
        "total_files":   len(file_list),
        "package_type":  "QFIL_COMPATIBLE"
                         if len(missing) == 0 else "INCOMPLETE"
    })

def calculate_flash_size(partitions_json: str) -> str:
    """Calculate total flash size from partition list."""
    partitions = json.loads(partitions_json)
    total_sectors = sum(
        p.get("num_sectors", 0) for p in partitions
    )
    sector_size   = partitions[0].get("sector_size", 4096) \
                    if partitions else 4096
    total_bytes   = total_sectors * sector_size
    return json.dumps({
        "total_sectors":     total_sectors,
        "sector_size":       sector_size,
        "total_bytes":       total_bytes,
        "total_mb":          round(total_bytes / (1024**2), 2),
        "total_gb":          round(total_bytes / (1024**3), 2),
        "partition_count":   len(partitions),
    })

def get_frp_partition_info(storage: str = "ufs") -> str:
    """
    Return FRP partition info for EDL erase.
    This is how EDL-based FRP bypass works.
    """
    if storage == "emmc":
        res = {
            "partition":   "frp",
            "start_sector": 6336,
            "num_sectors":  16,
            "sector_size":  512,
            "storage":      "emmc",
            "method":       "EDL_ERASE",
            "xml": (
                '<?xml version="1.0" ?><data>'
                '<erase SECTOR_SIZE_IN_BYTES="512" '
                'label="frp" '
                'physical_partition_number="0"/>'
                '</data>'
            )
        }
    else:  # ufs
        res = {
            "partition":   "frp",
            "start_sector": 8192,
            "num_sectors":  32,
            "sector_size":  4096,
            "storage":      "ufs",
            "method":       "EDL_ERASE",
            "xml": (
                '<?xml version="1.0" ?><data>'
                '<erase SECTOR_SIZE_IN_BYTES="4096" '
                'label="frp" '
                'physical_partition_number="0"/>'
                '</data>'
            )
        }
    return json.dumps(res)
