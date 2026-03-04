#!/usr/bin/env python3
"""gen_seed.py — Generate V2__seed_device_profiles.sql (1 164 models, 27 brands)."""

import json, os, sys

OUT = os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "..",
    "deepeye-universal", "cloud-services", "device-db",
    "migrations", "V2__seed_device_profiles.sql",
)

# ── Feature-set shorthand ────────────────────────────────────────────────
QC  = [1,2,3,4,5,7,8,10,14,15,16,18,19,21,22,23,24]   # Qualcomm flagship
MTK = [1,2,3,4,5,7,8,10,14,17,18,19,21,22,24]          # MediaTek full
SAF = [1,2,3,4,5,6,7,9,10,13,14,18,19,20,21,22,23,24]  # Samsung flagship
SAB = [1,2,6,7,9,10,13,14,18,20]                        # Samsung budget
HWF = [1,2,3,4,5,7,8,10,12,14,18,19,21,22,24]           # Huawei
XIF = [1,2,3,4,5,7,8,10,11,14,18,19,21,22,24]           # Xiaomi / Redmi
BDG = [1,2,6,7,14,18]                                    # budget generic
BDM = [1,2,6,7,14,17,18]                                 # budget MediaTek

rows = []  # (brand, model, series, year, dtype, chip, eng, unlock, feats, frp)

def a(brand, model, series, year, dtype, chip, eng, unlock, feats, frp="UNKNOWN"):
    rows.append((brand, model, series, year, dtype, chip, eng, unlock, feats, frp))

def p(brand, spec, series, chip, eng, unlock, feats, dtype="Smartphone"):
    """Parse compact 'Name:Year,Name:Year,...' spec."""
    for item in spec.split(","):
        item = item.strip()
        if not item:
            continue
        name, yr = item.rsplit(":", 1)
        a(brand, name.strip(), series, int(yr), dtype, chip, eng, unlock, feats)

# ═══════════════════════════════════════════════════════════════════════════
# ASUS  (26 = 12 ZenFone + 14 ROG Phone)
# ═══════════════════════════════════════════════════════════════════════════
p("ASUS",
  "ZenFone 4:2017,ZenFone 4 Pro:2017,ZenFone 5:2018,ZenFone 5Z:2018,"
  "ZenFone 6:2019,ZenFone 7:2020,ZenFone 7 Pro:2020,ZenFone 8:2021,"
  "ZenFone 8 Flip:2021,ZenFone 9:2022,ZenFone 10:2023,ZenFone 11 Ultra:2024",
  "ZenFone", "qualcomm", "qualcomm", True, QC, "Flagship")
p("ASUS",
  "ROG Phone:2018,ROG Phone 2:2019,ROG Phone 3:2020,ROG Phone 5:2021,"
  "ROG Phone 5s:2021,ROG Phone 5s Pro:2021,ROG Phone 6:2022,"
  "ROG Phone 6 Pro:2022,ROG Phone 7:2023,ROG Phone 7 Ultimate:2023,"
  "ROG Phone 8:2024,ROG Phone 8 Pro:2024,ROG Phone 9:2025,"
  "ROG Phone 9 Pro:2025",
  "ROG Phone", "qualcomm", "qualcomm", True, QC, "Gaming")

# ═══════════════════════════════════════════════════════════════════════════
# BlackBerry  (15)
# ═══════════════════════════════════════════════════════════════════════════
p("BlackBerry",
  "Q10:2013,Z10:2013,Z30:2013,Passport:2014,Q5:2014,Z3:2014,"
  "Classic:2015,Leap:2015,DTEK50:2016,DTEK60:2016,Priv:2016,"
  "KEYone:2017,Motion:2017,KEY2:2018,KEY2 LE:2018",
  "BlackBerry", "qualcomm", "qualcomm", False, QC, "Enterprise")

# ═══════════════════════════════════════════════════════════════════════════
# CAT  (23)
# ═══════════════════════════════════════════════════════════════════════════
p("CAT",
  "B15:2013,B100:2014,B15Q:2014,B25:2014,S30:2015,S40:2015,S50c:2015,"
  "S60:2016,S31:2017,S41:2017,S48c:2018,S61:2018,S52:2019,S42:2020,"
  "S42 H+:2020,S62:2020,S62 Pro:2020,S22 Flip:2022,S75:2023,"
  "S63:2024,S64 Pro:2024,S65 5G:2025,S66 5G:2025",
  "Rugged", "mediatek", "mediatek", False, MTK, "Rugged")

# ═══════════════════════════════════════════════════════════════════════════
# Essential  (1)
# ═══════════════════════════════════════════════════════════════════════════
a("Essential", "Essential Phone PH-1", "Phone", 2017, "Flagship",
  "qualcomm", "qualcomm", True, QC)

# ═══════════════════════════════════════════════════════════════════════════
# Fairphone  (7)
# ═══════════════════════════════════════════════════════════════════════════
p("Fairphone",
  "Fairphone 1:2013,Fairphone 2:2015,Fairphone 3:2019,"
  "Fairphone 3+:2020,Fairphone 4:2021,Fairphone 5:2023,Fairphone 6:2025",
  "Fairphone", "qualcomm", "qualcomm", True, QC, "Sustainable")

# ═══════════════════════════════════════════════════════════════════════════
# Google  (40 = 22 qualcomm + 18 tensor)
# ═══════════════════════════════════════════════════════════════════════════
p("Google",
  "Nexus One:2010,Nexus S:2010,Galaxy Nexus:2011,Nexus 4:2012,"
  "Nexus 5:2013,Nexus 6:2014,Nexus 5X:2015,Nexus 6P:2015,"
  "Pixel:2016,Pixel XL:2016,Pixel 2:2017,Pixel 2 XL:2017,"
  "Pixel 3:2018,Pixel 3 XL:2018,Pixel 3a:2019,Pixel 3a XL:2019,"
  "Pixel 4:2019,Pixel 4 XL:2019,Pixel 4a:2020,"
  "Pixel 4a 5G:2020,Pixel 5:2020,Pixel 5a 5G:2021",
  "Pixel", "qualcomm", "qualcomm", True, QC, "Flagship")
p("Google",
  "Pixel 6:2021,Pixel 6 Pro:2021,Pixel 6a:2022,"
  "Pixel 7:2022,Pixel 7 Pro:2022,Pixel 7a:2023,"
  "Pixel 8:2023,Pixel 8 Pro:2023,Pixel 8a:2024,"
  "Pixel 9:2024,Pixel 9 Pro:2024,Pixel 9 Pro XL:2024,"
  "Pixel 9 Pro Fold:2024,Pixel 9a:2025,"
  "Pixel Fold:2023,Pixel Tablet:2023,"
  "Pixel Watch:2022,Pixel Watch 2:2023",
  "Pixel", "tensor", "qualcomm", True, QC, "Flagship")

# ═══════════════════════════════════════════════════════════════════════════
# HTC  (15)
# ═══════════════════════════════════════════════════════════════════════════
p("HTC",
  "One M7:2013,One M8:2014,One M9:2015,One A9:2015,10:2016,"
  "U Play:2017,U Ultra:2017,U11:2017,U11+:2017,U11 Life:2017,"
  "U12+:2018,Desire 12:2018,Desire 20 Pro:2020,"
  "Wildfire E:2019,Wildfire E3:2021",
  "HTC", "qualcomm", "qualcomm", False, QC, "Smartphone")

# ═══════════════════════════════════════════════════════════════════════════
# Honor  (60 = 10 numbered-old + 5 View + 10 X + 15 Magic + 20 numbered-new)
# ═══════════════════════════════════════════════════════════════════════════
p("Honor",
  "Honor 7:2015,Honor 7X:2017,Honor 8:2016,Honor 8X:2018,"
  "Honor 8 Pro:2017,Honor 9:2017,Honor 9X:2019,Honor 9X Pro:2019,"
  "Honor Play 3:2019,Honor Play 4:2020",
  "Honor", "qualcomm", "qualcomm", True, QC, "Smartphone")
p("Honor",
  "View 10:2017,View 20:2019,View 30:2019,View 30 Pro:2019,View 40:2021",
  "View", "qualcomm", "qualcomm", True, QC, "Smartphone")
p("Honor",
  "X6:2022,X7:2022,X7a:2023,X8:2022,X8a:2023,"
  "X8b:2024,X9:2023,X9a:2024,X9b:2024,X10:2025",
  "X", "qualcomm", "qualcomm", True, QC, "Smartphone")
p("Honor",
  "Magic 3:2021,Magic 3 Pro:2021,Magic 4:2022,"
  "Magic 4 Pro:2022,Magic 4 Ultimate:2022,"
  "Magic 5:2023,Magic 5 Pro:2023,Magic 5 Lite:2023,"
  "Magic 6:2024,Magic 6 Pro:2024,Magic 6 Lite:2024,"
  "Magic 6 RSR:2024,Magic 7:2025,Magic 7 Pro:2025,"
  "Magic 7 Lite:2025",
  "Magic", "qualcomm", "qualcomm", True, QC, "Flagship")
p("Honor",
  "Honor 50:2021,Honor 50 Pro:2021,Honor 50 Lite:2021,"
  "Honor 60:2021,Honor 60 Pro:2021,"
  "Honor 70:2022,Honor 70 Pro:2022,Honor 70 Lite:2022,"
  "Honor 80:2023,Honor 80 Pro:2023,Honor 80 GT:2023,"
  "Honor 90:2023,Honor 90 Pro:2023,Honor 90 GT:2024,"
  "Honor 90 Lite:2023,Honor 200:2024,Honor 200 Pro:2024,"
  "Honor 200 Lite:2024,Honor 200 Smart:2024,Honor 300:2025",
  "Honor", "qualcomm", "qualcomm", True, QC, "Smartphone")

# ═══════════════════════════════════════════════════════════════════════════
# Huawei  (66 = 24 P + 18 Mate + 15 Nova + 9 Y)
# ═══════════════════════════════════════════════════════════════════════════
p("Huawei",
  "P8:2015,P8 Lite:2015,P9:2016,P9 Plus:2016,"
  "P10:2017,P10 Plus:2017,P10 Lite:2017,"
  "P20:2018,P20 Pro:2018,P20 Lite:2018,"
  "P30:2019,P30 Pro:2019,P30 Lite:2019,"
  "P40:2020,P40 Pro:2020,P40 Pro+:2020,P40 Lite:2020,"
  "P50:2021,P50 Pro:2021,"
  "P60:2023,P60 Pro:2023,"
  "P70:2024,P70 Pro:2024,P70 Art:2024",
  "P", "kirin", "qualcomm", False, HWF, "Flagship")
p("Huawei",
  "Mate 8:2015,Mate 9:2016,Mate 9 Pro:2016,"
  "Mate 10:2017,Mate 10 Pro:2017,"
  "Mate 20:2018,Mate 20 Pro:2018,Mate 20 X:2018,"
  "Mate 30:2019,Mate 30 Pro:2019,"
  "Mate 40:2020,Mate 40 Pro:2020,"
  "Mate 50:2022,Mate 50 Pro:2022,"
  "Mate 60:2023,Mate 60 Pro:2023,"
  "Mate X3:2023,Mate X5:2023",
  "Mate", "kirin", "qualcomm", False, HWF, "Flagship")
p("Huawei",
  "Nova 2:2017,Nova 3:2018,Nova 3i:2018,Nova 4:2018,"
  "Nova 5:2019,Nova 5T:2019,Nova 5 Pro:2019,"
  "Nova 7:2020,Nova 7i:2020,Nova 8:2021,Nova 8 Pro:2021,"
  "Nova 9:2021,Nova 10:2022,Nova 11:2023,Nova 12:2024",
  "Nova", "kirin", "qualcomm", False, HWF, "Smartphone")
p("Huawei",
  "Y5:2017,Y6:2017,Y7:2018,Y8:2019,Y9:2019,"
  "Y6p:2020,Y7p:2020,Y9s:2019,Y9a:2020",
  "Y", "kirin", "qualcomm", False, BDG, "Budget")

# ═══════════════════════════════════════════════════════════════════════════
# Infinix  (117 = 43 Hot + 24 Note + 23 Smart + 16 Zero + 4 GT + 7 S)
# ═══════════════════════════════════════════════════════════════════════════
p("Infinix",
  "Hot 7:2019,Hot 8:2019,Hot 8 Lite:2019,Hot 9:2020,Hot 9 Play:2020,"
  "Hot 9 Pro:2020,Hot 10:2020,Hot 10 Play:2020,Hot 10 Lite:2020,"
  "Hot 10i:2021,Hot 10S:2021,Hot 10S NFC:2021,Hot 10T:2021,"
  "Hot 11:2021,Hot 11 Play:2021,Hot 11S:2021,Hot 11S NFC:2021,"
  "Hot 11 5G:2022,Hot 12:2022,Hot 12 Play:2022,Hot 12i:2022,"
  "Hot 12 Pro:2022,Hot 20:2022,Hot 20 Play:2022,Hot 20i:2022,"
  "Hot 20S:2022,Hot 20 5G:2022,Hot 20e:2022,"
  "Hot 30:2023,Hot 30 Play:2023,Hot 30i:2023,Hot 30 5G:2023,"
  "Hot 30 NFC:2023,Hot 40:2024,Hot 40 Play:2024,Hot 40i:2024,"
  "Hot 40 Pro:2024,Hot 40S:2024,"
  "Hot 50:2025,Hot 50 Play:2025,Hot 50i:2025,Hot 50 Pro:2025,"
  "Hot 50 5G:2025",
  "Hot", "mediatek", "mediatek", False, MTK, "Budget")
p("Infinix",
  "Note 7:2020,Note 7 Lite:2020,Note 8:2020,Note 8i:2021,"
  "Note 10:2021,Note 10 Pro:2021,Note 11:2021,Note 11 Pro:2021,"
  "Note 11i:2022,Note 11S:2022,Note 12:2022,Note 12 Pro:2022,"
  "Note 12 G96:2022,Note 12 VIP:2022,Note 12i:2022,"
  "Note 30:2023,Note 30 Pro:2023,Note 30 5G:2023,Note 30i:2023,"
  "Note 40:2024,Note 40 Pro:2024,Note 40 Pro 5G:2024,"
  "Note 40X:2024,Note 40S:2024",
  "Note", "mediatek", "mediatek", False, MTK, "Smartphone")
p("Infinix",
  "Smart 2:2018,Smart 2 HD:2018,Smart 3:2019,Smart 3 Plus:2019,"
  "Smart 4:2020,Smart 4c:2020,Smart 4 Plus:2020,"
  "Smart 5:2020,Smart 5 Pro:2021,Smart 5A:2021,"
  "Smart 6:2021,Smart 6 Plus:2022,Smart 6 HD:2022,Smart 6 NFC:2022,"
  "Smart 7:2023,Smart 7 HD:2023,Smart 7 Plus:2023,"
  "Smart 8:2024,Smart 8 HD:2024,Smart 8 Plus:2024,Smart 8 Pro:2024,"
  "Smart 9:2025,Smart 9 HD:2025",
  "Smart", "mediatek", "mediatek", False, BDM, "Budget")
p("Infinix",
  "Zero 5G:2022,Zero 8:2020,Zero 8i:2021,Zero X:2021,"
  "Zero X Pro:2021,Zero X Neo:2021,Zero 20:2022,Zero 30:2023,"
  "Zero 30 5G:2023,Zero Book:2023,Zero Flip:2024,"
  "Zero Ultra:2022,Zero Flip 2:2025,Zero Ultra 2:2024,"
  "Zero 40:2024,Zero 40 5G:2024",
  "Zero", "mediatek", "mediatek", False, MTK, "Flagship")
p("Infinix",
  "GT 10 Pro:2023,GT 20 Pro:2024,GT Pro:2024,GT 7:2025",
  "GT", "mediatek", "mediatek", False, MTK, "Gaming")
p("Infinix",
  "S1:2016,S5 Pro:2020,S5 Lite:2020,S5 Plus:2020,"
  "S1 Pro:2016,S3 Lite:2018,S4:2019",
  "S", "mediatek", "mediatek", False, MTK, "Smartphone")

# ═══════════════════════════════════════════════════════════════════════════
# Karbonn  (23)
# ═══════════════════════════════════════════════════════════════════════════
p("Karbonn",
  "Titanium S5:2013,Titanium S5 Plus:2013,Titanium S9:2014,"
  "Titanium S99:2014,Titanium S200:2014,Titanium Octane:2014,"
  "Titanium Octane Plus:2014,Titanium Mach One:2015,"
  "Titanium Mach Two:2015,Titanium Mach Five:2015,"
  "Frames S7:2017,Frames S9:2017,"
  "Vue 1:2019,Vue 1 Pro:2019,"
  "Alfa A120:2018,Alfa A7:2018,Alfa A9:2018,"
  "Aura Note 2:2018,Aura Power:2018,"
  "K9 Smart:2017,K9 Smart Plus:2018,"
  "K9 Smart Selfie:2018,K9 Smart Grand:2018",
  "Karbonn", "mediatek", "mediatek", False, BDM, "Budget")

# ═══════════════════════════════════════════════════════════════════════════
# LG  (18)
# ═══════════════════════════════════════════════════════════════════════════
p("LG",
  "G4:2015,G5:2016,G6:2017,G7 ThinQ:2018,"
  "G8 ThinQ:2019,G8X ThinQ:2019,"
  "V20:2016,V30:2017,V30 ThinQ:2018,"
  "V40 ThinQ:2018,V50 ThinQ:2019,V60 ThinQ:2020,"
  "Velvet:2020,Wing:2020,Q60:2019,"
  "K51S:2020,Stylo 6:2020,Stylo 7:2021",
  "LG", "qualcomm", "qualcomm", False, QC, "Smartphone")

# ═══════════════════════════════════════════════════════════════════════════
# Lava  (41 = 4 Agni + 7 Blaze + 16 Z + 3 O + 3 Storm + 4 Yuva + 4 other)
# ═══════════════════════════════════════════════════════════════════════════
p("Lava",
  "Agni:2021,Agni 2:2022,Agni 3:2024,Agni 5G:2023",
  "Agni", "mediatek", "mediatek", False, MTK, "Smartphone")
p("Lava",
  "Blaze:2022,Blaze 5G:2022,Blaze 2:2023,Blaze 2 5G:2023,"
  "Blaze Pro:2023,Blaze Curve 5G:2024,Blaze X:2024",
  "Blaze", "mediatek", "mediatek", False, MTK, "Smartphone")
p("Lava",
  "Z1:2016,Z2:2016,Z3:2017,Z4:2017,Z6:2017,Z10:2017,"
  "Z20:2018,Z25:2018,Z50:2019,Z61:2019,Z70:2020,"
  "Z80:2020,Z81:2021,Z91:2021,Z92:2022,Z93:2022",
  "Z", "mediatek", "mediatek", False, BDM, "Budget")
p("Lava",
  "O1:2021,O2:2021,O3:2022",
  "O", "mediatek", "mediatek", False, BDM, "Budget")
p("Lava",
  "Storm:2022,Storm 5G:2022,Storm Pro:2023",
  "Storm", "mediatek", "mediatek", False, MTK, "Smartphone")
p("Lava",
  "Yuva 2:2022,Yuva 3:2023,Yuva 3 Pro:2023,Yuva 5G:2023",
  "Yuva", "mediatek", "mediatek", False, MTK, "Budget")
p("Lava",
  "Benco V80:2021,Iris 50:2017,MyZ 40:2020,MyZ 51:2021",
  "Lava", "mediatek", "mediatek", False, BDM, "Budget")

# ═══════════════════════════════════════════════════════════════════════════
# Micromax  (37 = 6 In + 31 Canvas)
# ═══════════════════════════════════════════════════════════════════════════
p("Micromax",
  "In 1:2020,In 1b:2020,In 2b:2021,In 2c:2021,"
  "In Note 1:2020,In Note 2:2021",
  "In", "mediatek", "mediatek", False, MTK, "Budget")
p("Micromax",
  "Canvas 1:2017,Canvas 2:2017,Canvas 2 Plus:2017,"
  "Canvas Blaze:2013,Canvas Doodle 2:2013,Canvas Doodle 3:2014,"
  "Canvas Fire 3:2015,Canvas Fire 4:2015,"
  "Canvas Gold:2014,Canvas HD:2013,Canvas Hue:2015,"
  "Canvas Infinity:2017,Canvas Juice 2:2014,Canvas Juice 3:2015,"
  "Canvas Knight:2014,Canvas Magnus:2014,"
  "Canvas Mega:2017,Canvas Nitro:2014,Canvas Pep:2015,"
  "Canvas Play:2014,Canvas Power:2017,"
  "Canvas Selfie:2015,Canvas Sliver 5:2015,"
  "Canvas Spark:2015,Canvas Tab:2015,"
  "Canvas Turbo:2013,Canvas Unite 2:2014,"
  "Canvas Unite 4:2016,Canvas Xpress:2014,"
  "Canvas Xpress 2:2015,Canvas 6:2016",
  "Canvas", "mediatek", "mediatek", False, BDM, "Budget")

# ═══════════════════════════════════════════════════════════════════════════
# Motorola  (70 = 43 G + 14 E + 10 Edge + 3 other)
# ═══════════════════════════════════════════════════════════════════════════
p("Motorola",
  "Moto G4:2016,Moto G4 Plus:2016,Moto G5:2017,Moto G5 Plus:2017,"
  "Moto G5S:2017,Moto G5S Plus:2017,Moto G6:2018,Moto G6 Plus:2018,"
  "Moto G7:2019,Moto G7 Play:2019,Moto G7 Plus:2019,"
  "Moto G7 Power:2019,Moto G8:2020,Moto G8 Plus:2019,"
  "Moto G8 Power:2020,Moto G9:2020,Moto G9 Play:2020,"
  "Moto G9 Plus:2020,Moto G9 Power:2020,"
  "Moto G10:2021,Moto G20:2021,Moto G22:2022,"
  "Moto G30:2021,Moto G31:2021,Moto G32:2022,"
  "Moto G40 Fusion:2021,Moto G41:2022,Moto G42:2022,"
  "Moto G50:2021,Moto G51 5G:2021,Moto G52:2022,"
  "Moto G53 5G:2023,Moto G54:2023,Moto G60:2021,"
  "Moto G62:2022,Moto G71:2022,Moto G72:2022,"
  "Moto G73:2023,Moto G82:2022,Moto G84:2023,"
  "Moto G85:2024,Moto G100:2021,Moto G200:2021",
  "Moto G", "qualcomm", "qualcomm", True, QC, "Smartphone")
p("Motorola",
  "Moto E5:2018,Moto E5 Plus:2018,Moto E6:2019,"
  "Moto E6 Plus:2019,Moto E7:2020,Moto E7 Plus:2020,"
  "Moto E7 Power:2021,Moto E13:2023,Moto E14:2024,"
  "Moto E20:2021,Moto E22:2022,Moto E30:2021,"
  "Moto E32:2022,Moto E40:2021",
  "Moto E", "qualcomm", "qualcomm", True, BDG, "Budget")
p("Motorola",
  "Edge:2020,Edge+:2020,Edge 20:2021,Edge 20 Pro:2021,"
  "Edge 30:2022,Edge 30 Pro:2022,Edge 40:2023,"
  "Edge 40 Pro:2023,Edge 50 Pro:2024,Edge 50 Ultra:2024",
  "Edge", "qualcomm", "qualcomm", True, QC, "Flagship")
p("Motorola",
  "Razr 40:2023,Razr 40 Ultra:2023,ThinkPhone:2023",
  "Motorola", "qualcomm", "qualcomm", True, QC, "Flagship")

# ═══════════════════════════════════════════════════════════════════════════
# Nokia  (38 = 25 numbered + 8 G + 5 C)
# ═══════════════════════════════════════════════════════════════════════════
p("Nokia",
  "Nokia 1:2018,Nokia 1.3:2020,Nokia 1.4:2021,"
  "Nokia 2:2017,Nokia 2.1:2018,Nokia 2.3:2019,Nokia 2.4:2020,"
  "Nokia 3:2017,Nokia 3.1:2018,Nokia 3.4:2020,"
  "Nokia 4.2:2019,Nokia 5:2017,Nokia 5.1:2018,"
  "Nokia 5.3:2020,Nokia 5.4:2020,"
  "Nokia 6:2017,Nokia 6.1:2018,Nokia 6.2:2019,"
  "Nokia 7:2017,Nokia 7.1:2018,Nokia 7.2:2019,"
  "Nokia 8:2017,Nokia 8.1:2019,Nokia 8.3 5G:2020,"
  "Nokia 9 PureView:2019",
  "Nokia", "mediatek", "mediatek", False, MTK, "Smartphone")
p("Nokia",
  "G10:2021,G11:2022,G20:2021,G21:2022,"
  "G22:2023,G42:2023,G50:2021,G60:2022",
  "G", "mediatek", "mediatek", False, MTK, "Smartphone")
p("Nokia",
  "C01:2021,C02:2022,C12:2022,C21:2022,C31:2022",
  "C", "mediatek", "mediatek", False, BDM, "Budget")

# ═══════════════════════════════════════════════════════════════════════════
# Nothing  (7)
# ═══════════════════════════════════════════════════════════════════════════
p("Nothing",
  "Phone (1):2022,Phone (2):2023,Phone (2a):2024,"
  "Phone (2a) Plus:2024,Phone (3):2025,"
  "Phone (3a):2025,CMF Phone 1:2024",
  "Nothing", "qualcomm", "qualcomm", True, QC, "Smartphone")

# ═══════════════════════════════════════════════════════════════════════════
# OPPO  (59 = 14 Find X + 23 Reno + 22 A)
# ═══════════════════════════════════════════════════════════════════════════
p("OPPO",
  "Find X:2018,Find X2:2020,Find X2 Pro:2020,"
  "Find X3:2021,Find X3 Pro:2021,Find X3 Lite:2021,"
  "Find X5:2022,Find X5 Pro:2022,"
  "Find X6:2023,Find X6 Pro:2023,"
  "Find X7:2024,Find X7 Ultra:2024,"
  "Find X8:2024,Find X8 Pro:2024",
  "Find X", "qualcomm", "qualcomm", False, QC, "Flagship")
p("OPPO",
  "Reno:2019,Reno 2:2019,Reno 3:2020,Reno 3 Pro:2020,"
  "Reno 4:2020,Reno 4 Pro:2020,Reno 5:2021,Reno 5 Pro:2021,"
  "Reno 6:2021,Reno 6 Pro:2021,Reno 7:2022,Reno 7 Pro:2022,"
  "Reno 8:2022,Reno 8 Pro:2022,"
  "Reno 9:2023,Reno 9 Pro:2023,"
  "Reno 10:2023,Reno 10 Pro:2023,Reno 10 Pro+:2023,"
  "Reno 11:2024,Reno 11 Pro:2024,"
  "Reno 12:2024,Reno 12 Pro:2024",
  "Reno", "qualcomm", "qualcomm", False, QC, "Smartphone")
p("OPPO",
  "A15:2020,A16:2021,A17:2022,A18:2023,"
  "A36:2022,A38:2023,A53:2020,A54:2021,"
  "A55:2021,A56:2022,A57:2022,A58:2023,"
  "A59:2023,A76:2022,A77:2022,A78:2023,"
  "A79:2023,A80:2024,A96:2022,A97:2022,"
  "A98:2023,A99:2024",
  "A", "qualcomm", "qualcomm", False, BDG, "Budget")

# ═══════════════════════════════════════════════════════════════════════════
# OnePlus  (39 = 28 numbered + 11 Nord)
# ═══════════════════════════════════════════════════════════════════════════
p("OnePlus",
  "OnePlus One:2014,OnePlus 2:2015,OnePlus 3:2016,"
  "OnePlus 3T:2016,OnePlus 5:2017,OnePlus 5T:2017,"
  "OnePlus 6:2018,OnePlus 6T:2018,"
  "OnePlus 7:2019,OnePlus 7 Pro:2019,"
  "OnePlus 7T:2019,OnePlus 7T Pro:2019,"
  "OnePlus 8:2020,OnePlus 8 Pro:2020,OnePlus 8T:2020,"
  "OnePlus 9:2021,OnePlus 9 Pro:2021,"
  "OnePlus 9R:2021,OnePlus 9RT:2021,"
  "OnePlus 10 Pro:2022,OnePlus 10T:2022,OnePlus 10R:2022,"
  "OnePlus 11:2023,OnePlus 11R:2023,"
  "OnePlus 12:2024,OnePlus 12R:2024,"
  "OnePlus 13:2025,OnePlus 13R:2025",
  "OnePlus", "qualcomm", "qualcomm", True, QC, "Flagship")
p("OnePlus",
  "Nord:2020,Nord 2:2021,Nord 2T:2022,Nord 3:2023,"
  "Nord CE:2021,Nord CE 2:2022,Nord CE 3:2023,"
  "Nord CE 4:2024,Nord N10:2020,Nord N20:2022,"
  "Nord 4:2024",
  "Nord", "qualcomm", "qualcomm", True, QC, "Smartphone")

# ═══════════════════════════════════════════════════════════════════════════
# Razer  (2)
# ═══════════════════════════════════════════════════════════════════════════
p("Razer",
  "Razer Phone:2017,Razer Phone 2:2018",
  "Razer", "qualcomm", "qualcomm", True, QC, "Gaming")

# ═══════════════════════════════════════════════════════════════════════════
# Realme  (71 = 36 numbered + 22 C + 8 GT + 5 Narzo)
# ═══════════════════════════════════════════════════════════════════════════
p("Realme",
  "Realme 1:2018,Realme 2:2018,Realme 2 Pro:2018,"
  "Realme 3:2019,Realme 3 Pro:2019,Realme 3i:2019,"
  "Realme 5:2019,Realme 5 Pro:2019,Realme 5i:2020,"
  "Realme 5s:2019,Realme 6:2020,Realme 6 Pro:2020,"
  "Realme 6i:2020,Realme 7:2020,Realme 7 Pro:2020,"
  "Realme 7i:2020,Realme 8:2021,Realme 8 Pro:2021,"
  "Realme 8i:2021,Realme 8s:2021,"
  "Realme 9:2022,Realme 9 Pro:2022,Realme 9 Pro+:2022,"
  "Realme 9i:2022,Realme 10:2022,Realme 10 Pro:2022,"
  "Realme 10 Pro+:2022,Realme 11:2023,Realme 11 Pro:2023,"
  "Realme 11 Pro+:2023,Realme 12:2024,Realme 12 Pro:2024,"
  "Realme 12 Pro+:2024,Realme 13:2024,Realme 13 Pro:2024,"
  "Realme 13 Pro+:2024",
  "Realme", "mediatek", "mediatek", False, MTK, "Smartphone")
p("Realme",
  "C1:2018,C2:2019,C3:2020,C11:2020,C12:2020,"
  "C15:2020,C17:2020,C20:2021,C21:2021,C25:2021,"
  "C30:2022,C31:2022,C33:2022,C35:2022,"
  "C51:2023,C53:2023,C55:2023,"
  "C61:2024,C63:2024,C65:2024,C67:2024,C75:2025",
  "C", "mediatek", "mediatek", False, BDM, "Budget")
p("Realme",
  "GT:2021,GT Neo 2:2021,GT Neo 3:2022,"
  "GT 2 Pro:2022,GT 3:2023,"
  "GT 5:2023,GT 5 Pro:2023,GT 7 Pro:2024",
  "GT", "mediatek", "mediatek", False, MTK, "Flagship")
p("Realme",
  "Narzo 20:2020,Narzo 30:2021,Narzo 50:2022,"
  "Narzo 60:2023,Narzo 70:2024",
  "Narzo", "mediatek", "mediatek", False, MTK, "Budget")

# ═══════════════════════════════════════════════════════════════════════════
# RedMagic  (13)
# ═══════════════════════════════════════════════════════════════════════════
p("RedMagic",
  "Red Magic 3:2019,Red Magic 3S:2019,Red Magic 5G:2020,"
  "Red Magic 5S:2020,Red Magic 6:2021,Red Magic 6 Pro:2021,"
  "Red Magic 6S Pro:2021,Red Magic 7:2022,"
  "Red Magic 7 Pro:2022,Red Magic 8 Pro:2023,"
  "Red Magic 8S Pro:2023,Red Magic 9 Pro:2024,"
  "Red Magic 10 Pro:2025",
  "RedMagic", "qualcomm", "qualcomm", True, QC, "Gaming")

# ═══════════════════════════════════════════════════════════════════════════
# Samsung  (138 = 35 S + 9 Note + 10 Z + 45 A + 25 M + 14 F)
# ═══════════════════════════════════════════════════════════════════════════
# -- S series  (samsung_exynos)
p("Samsung",
  "Galaxy S3:2012,Galaxy S4:2013,Galaxy S5:2014,"
  "Galaxy S6:2015,Galaxy S6 Edge:2015,"
  "Galaxy S7:2016,Galaxy S7 Edge:2016,"
  "Galaxy S8:2017,Galaxy S8+:2017,"
  "Galaxy S9:2018,Galaxy S9+:2018,"
  "Galaxy S10:2019,Galaxy S10+:2019,Galaxy S10e:2019,"
  "Galaxy S20:2020,Galaxy S20+:2020,Galaxy S20 Ultra:2020,"
  "Galaxy S20 FE:2020,"
  "Galaxy S21:2021,Galaxy S21+:2021,Galaxy S21 Ultra:2021,"
  "Galaxy S21 FE:2022,"
  "Galaxy S22:2022,Galaxy S22+:2022,Galaxy S22 Ultra:2022,"
  "Galaxy S23:2023,Galaxy S23+:2023,Galaxy S23 Ultra:2023,"
  "Galaxy S23 FE:2023,"
  "Galaxy S24:2024,Galaxy S24+:2024,Galaxy S24 Ultra:2024,"
  "Galaxy S25:2025,Galaxy S25+:2025,Galaxy S25 Ultra:2025",
  "Galaxy S", "samsung_exynos", "samsung", False, SAF, "Flagship")
# -- Note series  (samsung_exynos)
p("Samsung",
  "Galaxy Note 3:2013,Galaxy Note 4:2014,Galaxy Note 5:2015,"
  "Galaxy Note 8:2017,Galaxy Note 9:2018,"
  "Galaxy Note 10:2019,Galaxy Note 10+:2019,"
  "Galaxy Note 20:2020,Galaxy Note 20 Ultra:2020",
  "Galaxy Note", "samsung_exynos", "samsung", False, SAF, "Flagship")
# -- Z series  (samsung_exynos)
p("Samsung",
  "Galaxy Z Flip:2020,Galaxy Z Flip 3:2021,"
  "Galaxy Z Flip 4:2022,Galaxy Z Flip 5:2023,"
  "Galaxy Z Flip 6:2024,"
  "Galaxy Z Fold 2:2020,Galaxy Z Fold 3:2021,"
  "Galaxy Z Fold 4:2022,Galaxy Z Fold 5:2023,"
  "Galaxy Z Fold 6:2024",
  "Galaxy Z", "samsung_exynos", "samsung", False, SAF, "Foldable")
# -- A series  (mediatek)
p("Samsung",
  "Galaxy A01:2020,Galaxy A02:2021,Galaxy A02s:2021,"
  "Galaxy A03:2021,Galaxy A03s:2021,"
  "Galaxy A04:2022,Galaxy A04e:2022,Galaxy A04s:2022,"
  "Galaxy A05:2023,Galaxy A05s:2023,"
  "Galaxy A10:2019,Galaxy A10s:2019,"
  "Galaxy A11:2020,Galaxy A12:2020,"
  "Galaxy A13:2022,Galaxy A14:2023,Galaxy A15:2024,"
  "Galaxy A20:2019,Galaxy A20s:2019,"
  "Galaxy A21:2020,Galaxy A21s:2020,"
  "Galaxy A22:2021,Galaxy A23:2022,Galaxy A24:2023,"
  "Galaxy A25:2023,"
  "Galaxy A30:2019,Galaxy A30s:2019,"
  "Galaxy A31:2020,Galaxy A32:2021,"
  "Galaxy A33:2022,Galaxy A34:2023,Galaxy A35:2024,"
  "Galaxy A40:2019,Galaxy A50:2019,Galaxy A50s:2019,"
  "Galaxy A51:2020,Galaxy A52:2021,Galaxy A52s:2021,"
  "Galaxy A53:2022,Galaxy A54:2023,Galaxy A55:2024,"
  "Galaxy A70:2019,Galaxy A71:2020,"
  "Galaxy A72:2021,Galaxy A73:2022",
  "Galaxy A", "mediatek", "mediatek", False, SAB, "Mid-range")
# -- M series  (mediatek)
p("Samsung",
  "Galaxy M01:2020,Galaxy M02:2021,Galaxy M10:2019,"
  "Galaxy M11:2020,Galaxy M12:2021,Galaxy M13:2022,"
  "Galaxy M14:2023,Galaxy M20:2019,Galaxy M21:2020,"
  "Galaxy M22:2021,Galaxy M23:2022,"
  "Galaxy M30:2019,Galaxy M30s:2019,"
  "Galaxy M31:2020,Galaxy M31s:2020,Galaxy M32:2021,"
  "Galaxy M33:2022,Galaxy M34:2023,"
  "Galaxy M40:2019,Galaxy M42:2021,"
  "Galaxy M51:2020,Galaxy M52:2021,"
  "Galaxy M53:2022,Galaxy M54:2023,Galaxy M55:2024",
  "Galaxy M", "mediatek", "mediatek", False, SAB, "Mid-range")
# -- F series  (mediatek)
p("Samsung",
  "Galaxy F12:2021,Galaxy F13:2022,Galaxy F14:2023,"
  "Galaxy F15:2024,Galaxy F22:2021,Galaxy F23:2022,"
  "Galaxy F34:2023,Galaxy F41:2020,Galaxy F42:2021,"
  "Galaxy F54:2023,Galaxy F55:2024,"
  "Galaxy F62:2021,Galaxy F65:2024,Galaxy F06:2025",
  "Galaxy F", "mediatek", "mediatek", False, SAB, "Budget")

# ═══════════════════════════════════════════════════════════════════════════
# Tecno  (33 = 12 Camon + 13 Spark + 5 Phantom + 3 Pova)
# ═══════════════════════════════════════════════════════════════════════════
p("Tecno",
  "Camon 15:2020,Camon 16:2020,Camon 17:2021,"
  "Camon 17 Pro:2021,Camon 18:2021,Camon 18 Premier:2021,"
  "Camon 19:2022,Camon 19 Pro:2022,"
  "Camon 20:2023,Camon 20 Pro:2023,"
  "Camon 30:2024,Camon 30 Pro:2024",
  "Camon", "mediatek", "mediatek", False, MTK, "Smartphone")
p("Tecno",
  "Spark 5:2020,Spark 6:2020,Spark 7:2021,"
  "Spark 7 Pro:2021,Spark 8:2021,Spark 8C:2022,"
  "Spark 8P:2022,Spark 9:2022,Spark 9T:2022,"
  "Spark 10:2023,Spark 10 Pro:2023,"
  "Spark 20:2024,Spark 20 Pro:2024",
  "Spark", "mediatek", "mediatek", False, BDM, "Budget")
p("Tecno",
  "Phantom X:2021,Phantom X2:2023,Phantom X2 Pro:2023,"
  "Phantom V Fold:2023,Phantom V Flip:2023",
  "Phantom", "mediatek", "mediatek", False, MTK, "Flagship")
p("Tecno",
  "Pova:2020,Pova 2:2021,Pova 3:2022",
  "Pova", "mediatek", "mediatek", False, MTK, "Smartphone")

# ═══════════════════════════════════════════════════════════════════════════
# Vivo  (59 = 28 Y + 15 V + 9 X + 7 iQOO)
# ═══════════════════════════════════════════════════════════════════════════
p("Vivo",
  "Y01:2022,Y02:2022,Y02s:2022,"
  "Y11:2019,Y12:2019,Y12s:2020,"
  "Y15:2019,Y15s:2021,"
  "Y16:2022,Y17:2019,Y17s:2023,"
  "Y19:2019,Y20:2020,Y20s:2021,"
  "Y21:2021,Y22:2022,"
  "Y27:2023,Y28:2024,"
  "Y30:2020,Y33:2022,Y33s:2021,"
  "Y35:2022,Y36:2023,"
  "Y50:2020,Y51:2020,Y53s:2021,"
  "Y55:2022,Y56:2023",
  "Y", "mediatek", "mediatek", False, BDM, "Budget")
p("Vivo",
  "V5:2016,V7:2017,V9:2018,V11:2018,V15:2019,"
  "V17:2019,V19:2020,V20:2020,V21:2021,"
  "V23:2022,V25:2022,V27:2023,"
  "V29:2023,V30:2024,V40:2024",
  "V", "mediatek", "mediatek", False, MTK, "Smartphone")
p("Vivo",
  "X50:2020,X60:2021,X70 Pro:2021,"
  "X80 Pro:2022,X90 Pro:2023,"
  "X100:2024,X100 Pro:2024,"
  "X200:2025,X200 Pro:2025",
  "X", "qualcomm", "qualcomm", False, QC, "Flagship")
p("Vivo",
  "iQOO Z6:2022,iQOO Z7:2023,iQOO Z9:2024,"
  "iQOO Neo 7:2022,iQOO 12:2024,"
  "iQOO 13:2025,iQOO Neo 9:2024",
  "iQOO", "qualcomm", "qualcomm", False, QC, "Gaming")

# ═══════════════════════════════════════════════════════════════════════════
# Xiaomi  (133 = 37 Mi/Xiaomi + 21 Redmi + 27 Redmi Note + 14 Redmi K
#              + 20 Poco + 8 Mix + 3 Max + 3 CIVI)
# ═══════════════════════════════════════════════════════════════════════════
p("Xiaomi",
  "Mi 5:2016,Mi 5s:2016,Mi 6:2017,"
  "Mi 8:2018,Mi 8 Pro:2018,"
  "Mi 9:2019,Mi 9T:2019,Mi 9T Pro:2019,"
  "Mi 10:2020,Mi 10 Pro:2020,"
  "Mi 10T:2020,Mi 10T Pro:2020,Mi 10T Lite:2020,"
  "Mi 11:2021,Mi 11 Pro:2021,Mi 11 Ultra:2021,"
  "Mi 11 Lite:2021,Mi 11T:2021,Mi 11T Pro:2021,"
  "Xiaomi 12:2022,Xiaomi 12 Pro:2022,"
  "Xiaomi 12T:2022,Xiaomi 12T Pro:2022,Xiaomi 12 Lite:2022,"
  "Xiaomi 13:2023,Xiaomi 13 Pro:2023,"
  "Xiaomi 13T:2023,Xiaomi 13T Pro:2023,Xiaomi 13 Lite:2023,"
  "Xiaomi 14:2024,Xiaomi 14 Pro:2024,"
  "Xiaomi 14T:2024,Xiaomi 14T Pro:2024,Xiaomi 14 Ultra:2024,"
  "Xiaomi 15:2025,Xiaomi 15 Pro:2025,Xiaomi 15 Ultra:2025",
  "Xiaomi", "qualcomm", "qualcomm", True, XIF, "Flagship")
p("Xiaomi",
  "Redmi 7:2019,Redmi 7A:2019,Redmi 8:2019,Redmi 8A:2019,"
  "Redmi 9:2020,Redmi 9A:2020,Redmi 9C:2020,Redmi 9T:2021,"
  "Redmi 10:2021,Redmi 10A:2022,Redmi 10C:2022,"
  "Redmi 10 5G:2022,Redmi 12:2023,Redmi 12C:2023,"
  "Redmi 12 5G:2023,Redmi 13:2024,Redmi 13C:2024,"
  "Redmi 13 5G:2024,Redmi 14:2025,Redmi 14C:2025,"
  "Redmi 14 5G:2025",
  "Redmi", "mediatek", "mediatek", True, XIF, "Smartphone")
p("Xiaomi",
  "Redmi Note 7:2019,Redmi Note 7 Pro:2019,"
  "Redmi Note 8:2019,Redmi Note 8 Pro:2019,Redmi Note 8T:2019,"
  "Redmi Note 9:2020,Redmi Note 9 Pro:2020,Redmi Note 9S:2020,"
  "Redmi Note 9T:2021,"
  "Redmi Note 10:2021,Redmi Note 10 Pro:2021,"
  "Redmi Note 10S:2021,Redmi Note 10 5G:2021,"
  "Redmi Note 11:2022,Redmi Note 11 Pro:2022,"
  "Redmi Note 11S:2022,Redmi Note 11 Pro 5G:2022,"
  "Redmi Note 12:2023,Redmi Note 12 Pro:2023,"
  "Redmi Note 12 Pro 5G:2023,Redmi Note 12S:2023,"
  "Redmi Note 13:2024,Redmi Note 13 Pro:2024,"
  "Redmi Note 13 Pro 5G:2024,"
  "Redmi Note 14:2025,Redmi Note 14 Pro:2025,"
  "Redmi Note 14 Pro 5G:2025",
  "Redmi Note", "mediatek", "mediatek", True, XIF, "Smartphone")
p("Xiaomi",
  "Redmi K20:2019,Redmi K20 Pro:2019,"
  "Redmi K30:2020,Redmi K30 Pro:2020,"
  "Redmi K40:2021,Redmi K40 Pro:2021,"
  "Redmi K50:2022,Redmi K50 Pro:2022,"
  "Redmi K60:2023,Redmi K60 Pro:2023,"
  "Redmi K70:2024,Redmi K70 Pro:2024,"
  "Redmi K80:2025,Redmi K80 Pro:2025",
  "Redmi K", "qualcomm", "qualcomm", True, XIF, "Flagship")
p("Xiaomi",
  "Poco F1:2018,Poco F3:2021,Poco F4:2022,"
  "Poco F5:2023,Poco F6:2024,Poco F6 Pro:2024,"
  "Poco X3:2020,Poco X3 Pro:2021,"
  "Poco X4 Pro:2022,Poco X5:2023,Poco X5 Pro:2023,"
  "Poco X6:2024,Poco X6 Pro:2024,"
  "Poco M3:2020,Poco M4 Pro:2022,Poco M5:2022,"
  "Poco M5s:2022,Poco M6 Pro:2024,"
  "Poco C40:2022,Poco C55:2023",
  "Poco", "qualcomm", "qualcomm", True, XIF, "Smartphone")
p("Xiaomi",
  "Mi Mix:2016,Mi Mix 2:2017,Mi Mix 2S:2018,"
  "Mi Mix 3:2018,Mi Mix 4:2021,"
  "Mi Mix Fold:2021,Mi Mix Fold 2:2022,Mi Mix Fold 3:2023",
  "Mi Mix", "qualcomm", "qualcomm", True, XIF, "Flagship")
p("Xiaomi",
  "Mi Max:2016,Mi Max 2:2017,Mi Max 3:2018",
  "Mi Max", "qualcomm", "qualcomm", True, XIF, "Tablet")
p("Xiaomi",
  "Xiaomi CIVI:2021,Xiaomi CIVI 2:2022,Xiaomi CIVI 3:2023",
  "CIVI", "qualcomm", "qualcomm", True, XIF, "Smartphone")

# ═══════════════════════════════════════════════════════════════════════════
# ZTE  (13 = 9 Axon + 4 Blade)
# ═══════════════════════════════════════════════════════════════════════════
p("ZTE",
  "Axon 7:2016,Axon 9 Pro:2018,Axon 10 Pro:2019,"
  "Axon 11:2020,Axon 20:2020,Axon 30:2021,"
  "Axon 40 Ultra:2022,Axon 50 Ultra:2023,Axon 60 Ultra:2024",
  "Axon", "qualcomm", "qualcomm", False, QC, "Flagship")
p("ZTE",
  "Blade V8:2017,Blade V10:2019,Blade V30:2021,Blade V50:2024",
  "Blade", "qualcomm", "qualcomm", False, BDG, "Budget")


# ═══════════════════════════════════════════════════════════════════════════
#  Validate + write
# ═══════════════════════════════════════════════════════════════════════════
from collections import Counter
brand_counts = Counter(r[0] for r in rows)
expected = {
    "ASUS": 26, "BlackBerry": 15, "CAT": 23, "Essential": 1,
    "Fairphone": 7, "Google": 40, "HTC": 15, "Honor": 60,
    "Huawei": 66, "Infinix": 117, "Karbonn": 23, "LG": 18,
    "Lava": 41, "Micromax": 37, "Motorola": 70, "Nokia": 38,
    "Nothing": 7, "OPPO": 59, "OnePlus": 39, "Razer": 2,
    "Realme": 71, "RedMagic": 13, "Samsung": 138, "Tecno": 33,
    "Vivo": 59, "Xiaomi": 133, "ZTE": 13,
}
ok = True
for brand, n in sorted(expected.items()):
    actual = brand_counts.get(brand, 0)
    if actual != n:
        print(f"  MISMATCH  {brand}: expected {n}, got {actual}", file=sys.stderr)
        ok = False
total = len(rows)
if total != 1164:
    print(f"  TOTAL MISMATCH: expected 1164, got {total}", file=sys.stderr)
    ok = False
if not ok:
    sys.exit(1)

# ── Write SQL ─────────────────────────────────────────────────────────────
def esc(s):
    return s.replace("'", "''") if s else s

with open(OUT, "w") as f:
    f.write(
        "-- ============================================================\n"
        "-- V2__seed_device_profiles.sql\n"
        "-- DeepEye Universal — Full device seed\n"
        "-- 27 brands | 1164 models\n"
        "-- Flyway migration | PostgreSQL 15\n"
        "-- ============================================================\n\n"
        "INSERT INTO device_profiles\n"
        "    (brand, model, series, release_year, device_type,\n"
        "     chipset, engine, bootloader_unlockable, supported_functions, frp_state)\n"
        "VALUES\n"
    )
    for i, (brand, model, series, year, dtype, chip, eng, unlock, feats, frp) in enumerate(rows):
        sep = "," if i < total - 1 else ""
        bl = "TRUE" if unlock else "FALSE"
        line = (
            f"  ('{esc(brand)}', '{esc(model)}', '{esc(series)}', {year}, "
            f"'{esc(dtype)}', '{chip}', '{eng}', {bl}, "
            f"'{json.dumps(feats)}'::jsonb, '{frp}'){sep}\n"
        )
        f.write(line)
    f.write("ON CONFLICT DO NOTHING;\n")

print(f"OK — wrote {total} models ({len(brand_counts)} brands) → {OUT}")
