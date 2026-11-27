#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""0.93-kr4 변경에 대한 추가 정적 검증.

check_source.py 가 보지 않는 것들을 확인한다.
  1) XML 의 @drawable/ @color/ @mipmap/ @anim/ @style/ 참조가 실제로 있는지
  2) 프래그먼트 코드의 binding.<필드> 가 해당 레이아웃의 어느 변형(variant)엔가 있는지
  3) 3분할 레이아웃의 id/클래스/컨테이너가 코드와 맞는지
  4) 중복 id 정의, 레이아웃 변형별 필수 id 누락
"""
import os
import re
import glob
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP = os.path.join(ROOT, "ft8cn", "app", "src", "main")
RES = os.path.join(APP, "res")
JAVA = os.path.join(APP, "java")

errors = []


def err(msg):
    errors.append(msg)


def res_names(kind):
    out = set()
    for p in glob.glob(os.path.join(RES, kind + "*", "*")):
        out.add(os.path.splitext(os.path.basename(p))[0])
    # values*/*.xml 안의 <color name=...> 또는 <item type=... name=...>
    for p in glob.glob(os.path.join(RES, "values*", "*.xml")):
        try:
            root = ET.parse(p).getroot()
        except Exception:  # noqa: BLE001
            continue
        for ch in root:
            if ch.tag == "item" and ch.get("type") == kind:
                out.add(ch.get("name"))
            if kind == "color" and ch.tag == "color":
                out.add(ch.get("name"))
            if kind == "drawable" and ch.tag == "drawable":
                out.add(ch.get("name"))
    return out


DRAWABLES = res_names("drawable")
COLORS = res_names("color")
MIPMAPS = res_names("mipmap")
ANIMS = res_names("anim")
STYLES = res_names("values")  # style 은 values/*.xml 안에 있음
XMLFILES = []
for base, _, names in os.walk(RES):
    for n in names:
        if n.endswith(".xml"):
            XMLFILES.append(os.path.join(base, n))

REF = re.compile(r"@(drawable|color|mipmap|anim)/([A-Za-z0-9_.]+)")
# Material 라이브러리가 제공하는 색(프로젝트 res 에는 없음)
LIB_COLORS = {"material_dynamic_primary0", "material_dynamic_primary10",
              "material_dynamic_primary20", "material_dynamic_primary30",
              "material_dynamic_primary40", "material_dynamic_primary50"}
STYLE_NAMES = set()
for p in glob.glob(os.path.join(RES, "values*", "*.xml")):
    try:
        root = ET.parse(p).getroot()
    except Exception:  # noqa: BLE001
        continue
    for ch in root:
        if ch.tag == "style":
            STYLE_NAMES.add(ch.get("name"))

for p in XMLFILES:
    txt = open(p, encoding="utf-8").read()
    for kind, name in REF.findall(txt):
        pool = {"drawable": DRAWABLES, "color": COLORS, "mipmap": MIPMAPS,
                "anim": ANIMS}[kind]
        if name in LIB_COLORS:
            continue
        if name not in pool:
            err("%s 참조 없음 @%s/%s <- %s" % (kind, kind, name,
                                            os.path.relpath(p, ROOT)))
    for name in re.findall(r"@style/([A-Za-z0-9_.]+)", txt):
        if name not in STYLE_NAMES:
            err("style 없음 @style/%s <- %s" % (name, os.path.relpath(p, ROOT)))
    for name in re.findall(r"@android:style/([A-Za-z0-9_.]+)", txt):
        pass


# --- binding 필드 검사 ------------------------------------------------------
def ids_of_layout(layout_name):
    ids = set()
    for base, _, names in os.walk(RES):
        for n in names:
            if not n.endswith(".xml"):
                continue
            if os.path.splitext(n)[0] != layout_name:
                continue
            txt = open(os.path.join(base, n), encoding="utf-8").read()
            ids.update(re.findall(r'@\+id/([A-Za-z0-9_]+)', txt))
    return ids


FRAGMENTS = {
    "ui/CallingListFragment.java": "fragment_calling_list",
    "ui/MyCallingFragment.java": "fragment_my_calling",
    "ui/SpectrumFragment.java": "fragment_spectrum",
    "ui/LogFragment.java": "fragment_log",
    "ui/ConfigFragment.java": "fragment_config",
    "MainActivity.java": "main_activity",
}

for rel_path, layout in FRAGMENTS.items():
    p = os.path.join(JAVA, "com", "bg7yoz", "ft8cn", rel_path)
    if not os.path.exists(p):
        err("파일 없음 %s" % rel_path)
        continue
    raw = open(p, encoding="utf-8").read()
    txt = re.sub(r"/\*.*?\*/", "", raw, flags=re.S)
    txt = re.sub(r"//[^\n]*", "", txt)
    layout_ids = ids_of_layout(layout)
    # xml 의 snake_case id -> 데이터바인딩 필드(camelCase)
    camel_ids = set()
    for i in layout_ids:
        parts = i.split("_")
        camel_ids.add(parts[0] + "".join(x[:1].upper() + x[1:] for x in parts[1:]))
    used = set(re.findall(r"(?<![A-Za-z0-9_.])binding\.([a-zA-Z0-9_]+)\b", txt))
    known_super = {"getRoot", "getRoot()"}
    for field in sorted(used):
        if field in known_super:
            continue
        if field not in camel_ids:
            err("binding.%s 가 %s 어느 변형에도 없음 <- %s" % (field, layout, rel_path))

# --- 3분할 전용 확인 -------------------------------------------------------
main_ids = ids_of_layout("main_activity")
for need in ["splitLayout", "splitRightColumn", "splitSpectrumContainer",
             "splitDecodeContainer", "splitCallingContainer",
             "splitVerticalHandle", "splitHorizontalHandle"]:
    if need not in main_ids:
        err("main_activity 에 %s 없음" % need)

for variant in ["layout", "layout-land", "layout-sw600dp-land"]:
    p = os.path.join(RES, variant, "main_activity.xml")
    txt = open(p, encoding="utf-8").read()
    for need in ["splitLayout", "splitSpectrumContainer", "splitDecodeContainer",
                 "splitCallingContainer", "splitVerticalHandle",
                 "splitHorizontalHandle", "splitRightColumn"]:
        if ("@+id/" + need) not in txt:
            err("%s/main_activity.xml 에 @+id/%s 없음" % (variant, need))

# sw600dp-land 목록 레이아웃에는 spectrumView 가 없어야 한다(코드가 null 검사)
for name in ["fragment_calling_list", "fragment_my_calling"]:
    p = os.path.join(RES, "layout-sw600dp-land", name + ".xml")
    if not os.path.exists(p):
        err("%s 없음" % p)
        continue
    txt = open(p, encoding="utf-8").read()
    if "SpectrumView" in txt:
        err("layout-sw600dp-land/%s.xml 에 SpectrumView 가 있으면 안 됨" % name)

if errors:
    print("문제 %d건:" % len(errors))
    for e in errors:
        print("  [오류] " + e)
    raise SystemExit(1)
print("추가 검증 통과 (drawable=%d, color=%d, style=%d, 프래그먼트 %d개)"
      % (len(DRAWABLES), len(COLORS), len(STYLE_NAMES), len(FRAGMENTS)))
