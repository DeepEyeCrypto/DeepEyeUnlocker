import sys
import json
import argparse
from ios_backup.dfu import dfu_detect_mode

def main():
    parser = argparse.ArgumentParser(description="DeepEye iOS Backup & Forensics CLI")
    subparsers = parser.add_subparsers(dest="command")

    # DFU Commands
    dfu_state_parser = subparsers.add_parser("dfu-state")
    dfu_state_parser.add_argument("udid", nargs="?", default=None)

    # Activation Commands
    activation_parser = subparsers.add_parser("activation-state")
    activation_parser.add_argument("udid", nargs="?", default=None)
    
    args = parser.parse_args()

    if args.command == "dfu-state":
        res = {"mode": dfu_detect_mode(args.udid)}
        print(json.dumps(res))
    elif args.command == "activation-state":
        from ios_backup.activation import check_activation_lock
        print(json.dumps(check_activation_lock(args.udid)))
    elif args.command == "apple-id-state":
        from ios_backup.apple_id import get_apple_id_state
        print(json.dumps(get_apple_id_state(args.udid)))
    elif args.command == "remove-apple-id":
        from ios_backup.apple_id import remove_apple_id_direct
        # udid is required here
        print(json.dumps(remove_apple_id_direct(args.udid)))
    elif args.command == "screentime-hash":
        from ios_backup.screentime import extract_screentime_hash
        print(json.dumps(extract_screentime_hash(args.udid))) # using udid arg as path here
    elif args.command == "mdm-state":
        from ios_backup.mdm import detect_mdm
        print(json.dumps(detect_mdm(args.udid)))
    elif args.command == "list-profiles":
        from ios_backup.mdm import list_profiles
        print(json.dumps(list_profiles(args.udid)))
    elif args.command == "hello-state":
        from ios_backup.bypass import check_hello_screen_state
        print(json.dumps(check_hello_screen_state(args.udid)))
    elif args.command == "create-vault":
        # expects json string in udid argument for metadata/artifacts
        payload = json.loads(args.udid)
        from ios_backup.vault import create_deepvault_v2
        print(json.dumps(create_deepvault_v2(payload['dir'], payload['meta'], payload['files'])))
    elif args.command == "pwn-state":
        from ios_backup.ramdisk import check_pwned_dfu
        print(json.dumps(check_pwned_dfu(args.udid)))
    elif args.command == "gaster-pwn":
        from ios_backup.ramdisk import run_gaster_pwn
        print(json.dumps(run_gaster_pwn(args.udid)))
    elif args.command == "device-identity":
        from ios_backup.identity import get_device_identity
        print(json.dumps(get_device_identity(args.udid)))
    elif args.command == "activation-matrix":
        from ios_backup.activation_matrix import build_activation_matrix
        print(json.dumps(build_activation_matrix(args.udid)))
    elif args.command == "temp-activate":
        from ios_backup.activation_bypass import run_temp_activation
        print(json.dumps(run_temp_activation(args.udid)))
    elif args.command == "activation-persistence":
        from ios_backup.activation_bypass import check_persistence
        print(json.dumps(check_persistence(args.udid)))
    elif args.command == "activation-record":
        from ios_backup.activation_ticket import parse_activation_record
        print(json.dumps(parse_activation_record(args.udid))) # Path passed as udid arg in CLI loop
    elif args.command == "mount-ramdisk":
        from ios_backup.extraction import mount_partitions
        print(json.dumps(mount_partitions()))
    elif args.command == "mass-extract":
        from ios_backup.extraction import mass_extract
        print(json.dumps(mass_extract(args.udid))) # Save path passed as udid arg
    else:
        parser.print_help()

if __name__ == "__main__":
    main()
