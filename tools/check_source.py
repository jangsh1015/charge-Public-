#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""FT8CN 한국어판 정적 검사 도구.

Java/Android SDK 없이 다음을 검사한다.
  1) 모든 XML 파일이 well-formed 인지
  2) 모든 .java 파일이 문법적으로 파싱되는지 (javalang)
  3) R.string / @string 참조가 기본 values/strings.xml (또는 각 res/values* )에 존재하는지
  4) R.id / @+id / @id 참조가 레이아웃·메뉴에서 정의되어 있는지
  5) R.layout / @layout 참조가 존재하는지
  6) values-ko/strings.xml 의 항목 이름이 기본 strings.xml 과 정확히 일치하는지
     (누락/추가/중복, 포맷 지정자 %s %d %.1f 등 불일치)
  7) strings.xml 값의 이스케이프/포맷 지정자 위생 검사

사용법:  python3 tools/check_source.py [--quiet]
종료코드 0 = 통과, 1 = 문제 있음
"""
import os
import re
import sys
import glob
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP = os.path.join(ROOT, "ft8cn", "app", "src", "main")
RES = os.path.join(APP, "res")
JAVA = os.path.join(APP, "java")

errors = []
warnings = []


def err(msg):
    errors.append(msg)


def warn(msg):
    warnings.append(msg)


def rel(p):
    return os.path.relpath(p, ROOT)


# ---------------------------------------------------------------- 1) XML
def check_xml():
    files = []
    for base, _, names in os.walk(RES):
        for n in names:
            if n.endswith(".xml"):
                files.append(os.path.join(base, n))
    files.append(os.path.join(APP, "AndroidManifest.xml"))
    for f in sorted(files):
        try:
            ET.parse(f)
        except Exception as e:  # noqa: BLE001
            err("XML 파싱 실패 %s : %s" % (rel(f), e))
    return len(files)


# ---------------------------------------------------------------- 2) Java
def check_java():
    try:
        import javalang
    except ImportError:
        warn("javalang 미설치 - Java 문법 검사 건너뜀 (pip install javalang)")
        return 0
    files = []
    for base, _, names in os.walk(JAVA):
        for n in names:
            if n.endswith(".java"):
                files.append(os.path.join(base, n))
    for f in sorted(files):
        try:
            javalang.parse.parse(open(f, encoding="utf-8").read())
        except Exception as e:  # noqa: BLE001
            err("Java 문법 오류 %s : %s" % (rel(f), str(e)[:300]))
    return len(files)


# ---------------------------------------------------------------- 자원 수집
def strings_in(path, with_flags=False):
    root = ET.parse(path).getroot()
    names = {}
    for ch in root:
        if ch.tag == "string":
            names[ch.get("name")] = ch.text or ""
        elif ch.tag == "string-array":
            names[ch.get("name")] = "<array>"
        elif ch.tag == "plurals":
            names[ch.get("name")] = "<plurals>"
    if with_flags:
        flags = {ch.get("name"): (ch.get("translatable") == "false")
                 for ch in root if ch.tag in ("string", "string-array", "plurals")}
        return names, flags
    return names


def strip_java_comments(txt):
    txt = re.sub(r'/\*.*?\*/', '', txt, flags=re.S)
    txt = re.sub(r'//[^\n]*', '', txt)
    return txt


def collect_ids():
    ids = set()
    # values/*.xml 의 <item type="id" name="..."/>
    for p in glob.glob(os.path.join(RES, "values*", "*.xml")):
        try:
            root = ET.parse(p).getroot()
        except Exception:  # noqa: BLE001
            continue
        for ch in root:
            if ch.tag == "item" and ch.get("type") == "id":
                ids.add(ch.get("name"))
    for base, _, names in os.walk(RES):
        for n in names:
            if not n.endswith(".xml"):
                continue
            p = os.path.join(base, n)
            try:
                txt = open(p, encoding="utf-8").read()
            except Exception:  # noqa: BLE001
                continue
            for m in re.finditer(r'@\+id/([A-Za-z0-9_]+)', txt):
                ids.add(m.group(1))
            for m in re.finditer(r'@id/([A-Za-z0-9_]+)', txt):
                ids.add(m.group(1))
    return ids


def collect_layouts():
    out = set()
    for base, _, names in os.walk(RES):
        for n in names:
            if n.endswith(".xml"):
                out.add(os.path.splitext(n)[0])
                sub = os.path.basename(base)
                out.add(sub + "/" + os.path.splitext(n)[0])
    return out


# ---------------------------------------------------------------- 3~5) 참조
def check_refs(base_strings, ids, layouts):
    missing = {}
    for base, _, names in os.walk(APP):
        if os.sep + "res" in base:
            continue
        for n in names:
            if not (n.endswith(".java") or n.endswith(".xml")):
                continue
            p = os.path.join(base, n)
            try:
                txt = open(p, encoding="utf-8").read()
            except Exception:  # noqa: BLE001
                continue
            if n.endswith(".java"):
                txt = strip_java_comments(txt)
            for m in re.finditer(r'\bR\.string\.([A-Za-z0-9_]+)', txt):
                if m.group(1) not in base_strings:
                    missing.setdefault(m.group(1), set()).add(rel(p))
            for m in re.finditer(r'@string/([A-Za-z0-9_]+)', txt):
                if m.group(1) not in base_strings:
                    missing.setdefault(m.group(1), set()).add(rel(p))
            for m in re.finditer(r'\bR\.id\.([A-Za-z0-9_]+)', txt):
                if m.group(1) not in ids:
                    missing.setdefault("#id:" + m.group(1), set()).add(rel(p))
            for m in re.finditer(r'@id/([A-Za-z0-9_]+)', txt):
                if m.group(1) not in ids:
                    missing.setdefault("#id:" + m.group(1), set()).add(rel(p))
            for m in re.finditer(r'\bR\.layout\.([A-Za-z0-9_]+)', txt):
                if m.group(1) not in layouts:
                    missing.setdefault("#layout:" + m.group(1), set()).add(rel(p))
            for m in re.finditer(r'@layout/([A-Za-z0-9_]+)', txt):
                if m.group(1) not in layouts:
                    missing.setdefault("#layout:" + m.group(1), set()).add(rel(p))
    for k, v in sorted(missing.items()):
        err("정의되지 않은 자원 참조 %s  <- %s" % (k, ", ".join(sorted(v)[:4])))


# ---------------------------------------------------------------- 6) values-ko
FMT = re.compile(r'%(?:\d+\$)?[-#+0,(]*\d*(?:\.\d+)?[sdfxXeEgGc%]')


def fmt_set(s):
    return sorted(m.group(0) for m in FMT.finditer(s or ""))


def check_locale(locale, strict=True):
    base_path = os.path.join(RES, "values", "strings.xml")
    loc_path = os.path.join(RES, locale, "strings.xml")
    if not os.path.exists(loc_path):
        err("%s/strings.xml 없음" % locale)
        return
    base, flags = strings_in(base_path, with_flags=True)
    loc = strings_in(loc_path)
    report = err if strict else warn
    missing = [k for k in base if k not in loc and not flags.get(k)]
    extra = [k for k in loc if k not in base]
    if missing:
        report("%s 누락 문자열 %d개: %s" % (locale, len(missing), ", ".join(missing[:12])))
    if extra:
        report("%s 에만 있는 문자열 %d개: %s" % (locale, len(extra), ", ".join(extra[:12])))
    # 중복 이름
    txt = open(loc_path, encoding="utf-8").read()
    names = re.findall(r'<string name="([^"]+)"', txt)
    dup = sorted({n for n in names if names.count(n) > 1})
    if dup:
        report("%s 중복 문자열 이름: %s" % (locale, ", ".join(dup)))
    # 포맷 지정자 비교
    for k in base:
        if k not in loc:
            continue
        if fmt_set(base[k]) != fmt_set(loc[k]):
            report("%s 포맷 불일치 '%s' : base=%s / loc=%s" % (locale, k, fmt_set(base[k]), fmt_set(loc[k])))
    print("  %s: %d개 문자열 (기본 %d개)" % (locale, len(loc), len(base)))


# ---------------------------------------------------------------- 7) 값 위생
def check_values_hygiene(locale):
    p = os.path.join(RES, locale, "strings.xml")
    if not os.path.exists(p):
        return
    txt = open(p, encoding="utf-8").read()
    # 태그 뒤에 붙은 잉여 텍스트 검사 (예: </string>>)
    for i, line in enumerate(txt.splitlines(), 1):
        if re.search(r"</string>>", line):
            err("%s %d행: 문자열 태그 뒤 잉여 '>' 문자" % (locale, i))
    # 이스케이프되지 않은 작은따옴표 (Android 는 \' 필요)
    root = ET.parse(p).getroot()
    for ch in root:
        if ch.tag != "string":
            continue
        val = ch.text or ""
        if "'" in val and "\\'" not in val:
            err("%s '%s' 이스케이프 안 된 작은따옴표: %s" % (locale, ch.get("name"), val[:60]))
        if "@" in val[:1] or (val.startswith("?") ):
            warn("%s '%s' 특수 시작 문자: %s" % (locale, ch.get("name"), val[:40]))


# ---------------------------------------------------------------- main
def main():
    nxml = check_xml()
    njava = check_java()
    base = strings_in(os.path.join(RES, "values", "strings.xml"))
    ids = collect_ids()
    layouts = collect_layouts()
    check_refs(base, ids, layouts)
    for loc in sorted(d for d in os.listdir(RES)
                      if d.startswith("values-") and d != "values-night"):
        if os.path.exists(os.path.join(RES, loc, "strings.xml")):
            check_locale(loc, strict=(loc == "values-ko"))
    check_values_hygiene("values-ko")
    print("\n검사: XML %d개, Java %d개, id %d개, layout %d개, 기본 문자열 %d개"
          % (nxml, njava, len(ids), len(layouts), len(base)))
    for w in warnings[:40]:
        print("  [경고] " + w)
    if warnings[40:]:
        print("  [경고] ... 외 %d건" % (len(warnings) - 40))
    if errors:
        print("\n문제 %d건:" % len(errors))
        for e in errors[:80]:
            print("  [오류] " + e)
        if len(errors) > 80:
            print("  ... 외 %d건" % (len(errors) - 80))
        return 1
    print("\n모든 검사 통과.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
