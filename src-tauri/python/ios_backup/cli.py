from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

if __package__ in {None, ""}:
    package_root = Path(__file__).resolve().parent.parent
    if str(package_root) not in sys.path:
        sys.path.insert(0, str(package_root))

from ios_backup.activation import check_activation_lock
from ios_backup.activation_matrix import build_activation_matrix
from ios_backup.activation_bypass import check_persistence, run_temp_activation
from ios_backup.apple_id import get_apple_id_state, remove_apple_id_direct
from ios_backup.bypass import check_hello_screen_state
from ios_backup.dfu import dfu_detect_mode
from ios_backup.extraction import mass_extract, mount_partitions
from ios_backup.identity import get_device_identity
from ios_backup.manifest import parse_manifest
from ios_backup.mdm import detect_mdm, list_profiles
from ios_backup.ramdisk import check_pwned_dfu, run_gaster_pwn
from ios_backup.screentime import extract_hash, extract_screentime_hash
from ios_backup.ticket import find_backup_tickets, parse_activation_record
from ios_backup.vault import create_deepvault_v2


def _print_json(payload: object) -> int:
    print(json.dumps(payload))
    return 0


def _handle_hash(args: argparse.Namespace) -> int:
    result = extract_hash(args.backup_path)
    if "error" in result:
        print(result["error"], file=sys.stderr)
        return 1

    output_path = Path(args.output).expanduser().resolve()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(result, indent=2))
    print(1)
    return 0


def _handle_crack(args: argparse.Namespace) -> int:
    hash_info = extract_hash(args.backup_path)
    if "error" in hash_info:
        print(hash_info["error"], file=sys.stderr)
        return 1

    wordlist = Path(args.wordlist).expanduser().resolve()
    if not wordlist.exists():
        print(f"Wordlist not found: {wordlist}", file=sys.stderr)
        return 1

    hashcat_cmd = [
        "hashcat",
        "-m",
        str(hash_info["hashcat_mode"]),
        "-a",
        "0",
        f"{hash_info['hash']}:{hash_info['salt']}:{hash_info['iterations']}",
        str(wordlist),
    ]
    completed = subprocess.run(hashcat_cmd, check=False)
    return int(completed.returncode)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="DeepEye iOS Backup & Forensics CLI")
    subparsers = parser.add_subparsers(dest="command", required=True)

    for command in ("dfu-state", "activation-state", "apple-id-state", "mdm-state", "hello-state", "pwn-state", "device-identity", "activation-matrix"):
        cmd = subparsers.add_parser(command)
        cmd.add_argument("udid", nargs="?", default=None)

    remove_apple_id = subparsers.add_parser("remove-apple-id")
    remove_apple_id.add_argument("udid")

    screentime_hash = subparsers.add_parser("screentime-hash")
    screentime_hash.add_argument("backup_path")

    list_profiles_cmd = subparsers.add_parser("list-profiles")
    list_profiles_cmd.add_argument("udid")

    create_vault = subparsers.add_parser("create-vault")
    create_vault.add_argument("payload")

    gaster_pwn = subparsers.add_parser("gaster-pwn")
    gaster_pwn.add_argument("udid", nargs="?", default=None)

    temp_activate = subparsers.add_parser("temp-activate")
    temp_activate.add_argument("udid")

    activation_persistence = subparsers.add_parser("activation-persistence")
    activation_persistence.add_argument("udid")

    activation_record = subparsers.add_parser("activation-record")
    activation_record.add_argument("path")

    scan_tickets = subparsers.add_parser("scan-tickets")
    scan_tickets.add_argument("backup_path")

    subparsers.add_parser("mount-ramdisk")

    mass_extract_cmd = subparsers.add_parser("mass-extract")
    mass_extract_cmd.add_argument("save_path")

    info_cmd = subparsers.add_parser("info")
    info_cmd.add_argument("backup_path")

    hash_cmd = subparsers.add_parser("hash")
    hash_cmd.add_argument("backup_path")
    hash_cmd.add_argument("--output", required=True)

    screentime_cmd = subparsers.add_parser("screentime")
    screentime_cmd.add_argument("backup_path")
    screentime_cmd.add_argument("--password", default=None)

    crack_cmd = subparsers.add_parser("crack")
    crack_cmd.add_argument("backup_path")
    crack_cmd.add_argument("--wordlist", required=True)

    args = parser.parse_args(argv)

    if args.command == "dfu-state":
        return _print_json({"mode": dfu_detect_mode(args.udid)})
    if args.command == "activation-state":
        return _print_json(check_activation_lock(args.udid))
    if args.command == "apple-id-state":
        return _print_json(get_apple_id_state(args.udid))
    if args.command == "remove-apple-id":
        return _print_json(remove_apple_id_direct(args.udid))
    if args.command == "screentime-hash":
        return _print_json(extract_screentime_hash(args.backup_path))
    if args.command == "mdm-state":
        return _print_json(detect_mdm(args.udid))
    if args.command == "list-profiles":
        return _print_json(list_profiles(args.udid))
    if args.command == "hello-state":
        return _print_json(check_hello_screen_state(args.udid))
    if args.command == "create-vault":
        payload = json.loads(args.payload)
        return _print_json(create_deepvault_v2(payload["dir"], payload["meta"], payload["files"]))
    if args.command == "pwn-state":
        return _print_json(check_pwned_dfu(args.udid))
    if args.command == "gaster-pwn":
        return _print_json(run_gaster_pwn(args.udid))
    if args.command == "device-identity":
        return _print_json(get_device_identity(args.udid))
    if args.command == "activation-matrix":
        return _print_json(build_activation_matrix(args.udid))
    if args.command == "temp-activate":
        return _print_json(run_temp_activation(args.udid))
    if args.command == "activation-persistence":
        return _print_json(check_persistence(args.udid))
    if args.command == "activation-record":
        return _print_json(parse_activation_record(args.path))
    if args.command == "scan-tickets":
        return _print_json(find_backup_tickets(args.backup_path))
    if args.command == "mount-ramdisk":
        return _print_json(mount_partitions())
    if args.command == "mass-extract":
        return _print_json(mass_extract(args.save_path))
    if args.command == "info":
        return _print_json(parse_manifest(args.backup_path))
    if args.command == "hash":
        return _handle_hash(args)
    if args.command == "screentime":
        result = extract_screentime_hash(args.backup_path)
        if "error" not in result:
            result["password_provided"] = bool(args.password)
        return _print_json(result)
    if args.command == "crack":
        return _handle_crack(args)

    parser.print_help()
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
