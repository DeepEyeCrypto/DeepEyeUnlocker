# Qualcomm Firehose Programmer ELFs

Place firehose programmer ELFs here for EDL mode operations.

## Required Files

| Filename | Chipset | Devices |
|----------|---------|---------|
| `sm8650_ufs_firehose.elf` | SD 8 Gen 3 | Galaxy S24 Ultra |
| `sm8550_ufs_firehose.elf` | SD 8 Gen 2 | Galaxy S23, POCO F5, Redmi Note 13 Pro+ |
| `sm8475_ufs_firehose.elf` | SD 8+ Gen 1 | Various |
| `sm8450_ufs_firehose.elf` | SD 8 Gen 1 | Galaxy S22 |
| `sm8350_ufs_firehose.elf` | SD 888 | Galaxy S21 |
| `sm8250_ufs_firehose.elf` | SD 865/870 | Mi 10, Poco F3 |
| `sm8150_ufs_firehose.elf` | SD 855 | Galaxy S10 (QC) |
| `sm7450_ufs_firehose.elf` | SD 7 Gen 1 | Motorola Edge |
| `sm7325_ufs_firehose.elf` | SD 778G | Various |
| `sm7250_ufs_firehose.elf` | SD 765G | Pixel 5 |
| `sm7225_emmc_firehose.elf` | SD 750G | Galaxy A42/A52 |
| `sm6450_emmc_firehose.elf` | SD 6 Gen 1 | Redmi 12 5G |
| `sm6375_emmc_firehose.elf` | SD 695 | Redmi Note 11 5G |
| `sm6115_emmc_firehose.elf` | SD 662 | Redmi Note 10/11 4G |
| `msm8998_ufs_firehose.elf` | SD 835 | Various |
| `msm8953_emmc_firehose.elf` | SD 625 | Redmi Note 4/4X |
| `msm8937_emmc_firehose.elf` | SD 430/435 | Budget range |
| `msm8909_emmc_firehose.elf` | SD 210/212 | Ultra budget |

## Download Source

```bash
brew install git-lfs
git lfs install
git clone https://github.com/bkerler/Loaders /tmp/qc-loaders
cd /tmp/qc-loaders && git lfs pull
```

Then run:
```bash
bash scripts/copy_programmers.sh
```

## Validate

```bash
python3 scripts/validate_programmer.py app/src/main/assets/prog/*.elf
```

## Secure Boot Note

For Secure Boot locked devices, the programmer must match the device's `PK_HASH`.
Filename format from bkerler/Loaders: `msmid_pkhash8bytes.bin`
Search: https://www.temblast.com/ref/loaders.htm
