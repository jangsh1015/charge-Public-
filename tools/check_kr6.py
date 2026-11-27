#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""0.93-kr6 변경(디코드 2열 · 호출+로그 2분할)에 대한 추가 정적 검증.

check_source.py · check_kr4.py 가 보지 않는 것들을 확인한다.
  1) main_activity 3개 변형(폰·가로·태블릿 가로)에 splitCallLogLayout 과 그 자식 3개가 있고,
     기본 상태가 gone 인지
  2) MainActivity 가 두 패널을 붙이고(LogFragment·MyCallingFragment) 목적지에 따라 보여 주는지
  3) CallingListFragment 의 2열 보기가 태블릿 가로 + 패널 밖 조건으로만 켜지는지
  4) LogFragment 의 컨텍스트 메뉴 가드가 있는지
  5) 버전·문서 표기가 0.93-kr6 으로 맞는지
"""
import glob
import io
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP = os.path.join(ROOT, "ft8cn", "app", "src", "main")
RES = os.path.join(APP, "res")
JAVA = os.path.join(APP, "java", "com", "bg7yoz", "ft8cn")

errors = []


def err(msg):
    errors.append(msg)


def read(path):
    return io.open(path, encoding="utf-8").read()


def strip_comments(txt):
    txt = re.sub(r"/\*.*?\*/", "", txt, flags=re.S)
    return re.sub(r"//[^\n]*", "", txt)


# --- 1) main_activity 변형 ---------------------------------------------------
VARIANTS = ["layout", "layout-land", "layout-sw600dp-land"]
NEED_IDS = ["splitCallLogLayout", "splitCallingOnlyContainer", "splitLogContainer",
            "splitCallLogHandle"]

for variant in VARIANTS:
    path = os.path.join(RES, variant, "main_activity.xml")
    if not os.path.exists(path):
        err("%s/main_activity.xml 없음" % variant)
        continue
    txt = read(path)
    try:
        root = ET.fromstring(txt)
    except Exception as e:  # noqa: BLE001
        err("%s/main_activity.xml XML 오류: %s" % (variant, e))
        continue
    for need in NEED_IDS:
        if ("@+id/" + need) not in txt:
            err("%s/main_activity.xml 에 @+id/%s 없음" % (variant, need))

    # splitCallLogLayout 요소를 찾아 자식 구성을 확인한다
    split = None
    for el in root.iter():
        if el.get("{http://schemas.android.com/apk/res/android}id") == "@+id/splitCallLogLayout":
            split = el
            break
    if split is None:
        err("%s/main_activity.xml 에서 splitCallLogLayout 요소를 찾지 못함" % variant)
        continue
    A = "{http://schemas.android.com/apk/res/android}"
    if split.get(A + "visibility") != "gone":
        err("%s: splitCallLogLayout 은 기본 gone 이어야 함" % variant)
    if split.get(A + "orientation") != "horizontal":
        err("%s: splitCallLogLayout 은 가로(horizontal)여야 함" % variant)
    kids = list(split)
    child_ids = [k.get(A + "id") for k in kids]
    for need in ["splitCallingOnlyContainer", "splitCallLogHandle", "splitLogContainer"]:
        if ("@+id/" + need) not in child_ids:
            err("%s: splitCallLogLayout 의 자식에 %s 없음 (실제: %s)" % (variant, need, child_ids))
    if child_ids and child_ids[0] != "@+id/splitCallingOnlyContainer":
        err("%s: 왼쪽 첫 자식이 splitCallingOnlyContainer 가 아님 (%s)" % (variant, child_ids[0]))
    if child_ids and child_ids[-1] != "@+id/splitLogContainer":
        err("%s: 오른쪽 마지막 자식이 splitLogContainer 가 아님 (%s)" % (variant, child_ids[-1]))
    containers = [k for k in kids if k.tag.endswith("FragmentContainerView")]
    if len(containers) != 2:
        err("%s: splitCallLogLayout 안의 FragmentContainerView 가 2개가 아님 (%d)"
            % (variant, len(containers)))
    handle = [k for k in kids if k.get(A + "id") == "@+id/splitCallLogHandle"]
    if handle and handle[0].get(A + "layout_width") != "16dp":
        err("%s: splitCallLogHandle 폭이 16dp 가 아님 (%s)"
            % (variant, handle[0].get(A + "layout_width")))

main_txt = read(os.path.join(JAVA, "MainActivity.java"))
main_code = strip_comments(main_txt)

# --- 2) MainActivity 배선 ----------------------------------------------------
CHECKS = [
    ("initTabletCallLogLayout 메서드", r"private\s+void\s+initTabletCallLogLayout\s*\(\s*\)"),
    ("호출 패널 MyCallingFragment 부착", r"new MyCallingFragment\(\)"),
    ("QSO 로그 패널 LogFragment 부착", r"new LogFragment\(\)"),
    ("호출 패널 태그", r"FRAGMENT_TAG_SPLIT_CALL_ONLY"),
    ("로그 패널 태그", r"FRAGMENT_TAG_SPLIT_LOG"),
    ("호출 탭 목적지 분기", r"destinationId\s*==\s*R\.id\.menu_nav_mycalling"),
    ("마지막 화면 저장", r"saveLastMode\("),
    ("마지막 화면 복원", r"restoreStartMode\("),
    ("lastMode 키", r"KEY_LAST_MODE\s*=\s*\"lastMode\""),
    ("좌우 비율 저장 키", r"KEY_CALL_LOG_RATIO\s*=\s*\"callLogRatio\""),
    ("경계선 드래그 처리", r"class\s+CallLogHandleTouchListener\s+implements\s+View\.OnTouchListener"),
    ("패널 프래그먼트 판별", r"public\s+static\s+boolean\s+isSplitPanelFragment\s*\("),
]
for label, pattern in CHECKS:
    if not re.search(pattern, main_code):
        err("MainActivity 에 %s 가 없음 (%s)" % (label, pattern))

# 두 패널을 같은 트랜잭션에서 붙이는지(대략 확인): 두 add 호출이 400자 안에 있는지
adds = [m.start() for m in re.finditer(r"transaction\.add\(R\.id\.split(CallingOnly|Log)Container", main_code)]
if len(adds) < 2:
    err("호출·로그 패널을 transaction.add 로 붙이는 코드가 2개가 아님 (%d)" % len(adds))
elif adds[1] - adds[0] > 400:
    err("호출·로그 패널 add 가 한 트랜잭션으로 묶여 있지 않은 듯(거리 %d자)" % (adds[1] - adds[0]))

# updateSplitVisibility 안에서 두 패널을 모두 제어하는지
m = re.search(r"private\s+void\s+updateSplitVisibility\([^)]*\)\s*\{(.*?)\n    \}",
              main_code, flags=re.S)
if not m:
    err("updateSplitVisibility 본문을 찾지 못함")
else:
    body = m.group(1)
    for label, pattern in [
        ("splitLayout 표시 제어", r"splitLayout\.setVisibility"),
        ("splitCallLogLayout 표시 제어", r"splitCallLogLayout\.setVisibility"),
        ("NavHost 숨김 처리", r"navHostContainer\.setVisibility"),
    ]:
        if not re.search(pattern, body):
            err("updateSplitVisibility 안에 %s 가 없음" % label)

# --- 3) 디코드 2열 ----------------------------------------------------------
calling = strip_comments(read(os.path.join(JAVA, "ui", "CallingListFragment.java")))
if "new GridLayoutManager(requireContext(), 2)" not in calling:
    err("CallingListFragment 에 GridLayoutManager(requireContext(), 2) 가 없음")
if "new LinearLayoutManager(requireContext())" not in calling:
    err("CallingListFragment 의 1열(LinearLayoutManager) 경로가 사라짐")
if "import androidx.recyclerview.widget.GridLayoutManager;" not in calling:
    err("CallingListFragment 에 GridLayoutManager import 없음")
if "MainActivity.isSplitPanelFragment(getTag())" not in calling:
    err("CallingListFragment 의 2열 조건에 패널 제외(isSplitPanelFragment) 가 없음")
m = re.search(r"View\s+onCreateView[\s\S]{0,4000}?setLayoutManager\(([^)]*)\)", calling)
if not m:
    err("CallingListFragment 에서 LayoutManager 설정 줄을 찾지 못함")
elif "createCallingListLayoutManager" not in m.group(1):
    err("LayoutManager 설정이 createCallingListLayoutManager() 를 쓰지 않음 (%s)" % m.group(1).strip())

# --- 4) LogFragment 컨텍스트 메뉴 가드 ---------------------------------------
logf = strip_comments(read(os.path.join(JAVA, "ui", "LogFragment.java")))
m = re.search(r"public\s+boolean\s+onContextItemSelected\(@NonNull MenuItem item\)\s*\{(.*?)\n    \}",
              logf, flags=re.S)
if not m:
    err("LogFragment.onContextItemSelected 를 찾지 못함")
else:
    body = m.group(1)
    if "isMyListItemView(item.getActionView())" not in body:
        err("LogFragment.onContextItemSelected 에 자기 목록 확인 가드가 없음")
    first = [ln.strip() for ln in body.strip().splitlines() if ln.strip()][:2]
    if not first or "isMyListItemView" not in first[0]:
        err("LogFragment 가 position 을 읽기 전에 가드하지 않음 (%s)" % first)
if "private boolean isMyListItemView(View view)" not in logf:
    err("LogFragment 에 isMyListItemView 헬퍼가 없음")
if "import android.view.ViewParent;" not in logf:
    err("LogFragment 에 ViewParent import 없음")

# --- 5) 로그 제목 막대 색 -----------------------------------------------------
log_layout = read(os.path.join(RES, "layout", "fragment_log.xml"))
m = re.search(r'android:id="@\+id/log_fragment_bar"[\s\S]{0,400}?android:background="([^"]+)"',
              log_layout)
if not m:
    err("fragment_log.xml 의 log_fragment_bar 배경색을 찾지 못함")
elif m.group(1) != "@color/list_header_bar_color":
    err("fragment_log.xml 제목 막대 색이 list_header_bar_color 가 아님 (%s)" % m.group(1))

colors = read(os.path.join(RES, "values", "colors.xml"))
if 'name="list_header_bar_color"' not in colors:
    err("values/colors.xml 에 list_header_bar_color 없음")

# --- 6) 버전·문서 -----------------------------------------------------------
gradle = read(os.path.join(ROOT, "ft8cn", "app", "build.gradle"))
if "versionCode 6" not in gradle:
    err("build.gradle versionCode 가 6 이 아님")
if "versionName '0.93-kr6'" not in gradle:
    err("build.gradle versionName 이 0.93-kr6 이 아님")

for name, path in [("readme_ko.txt", os.path.join(APP, "assets", "readme_ko.txt")),
                   ("readme_en.txt", os.path.join(APP, "assets", "readme_en.txt")),
                   ("README_KR.md", os.path.join(ROOT, "README_KR.md"))]:
    if "0.93-kr6" not in read(path):
        err("%s 에 0.93-kr6 내역이 없음" % name)

# 정보 창의 변경 내역은 새 버전이 맨 위에 와야 한다
for name in ["readme_ko.txt", "readme_en.txt"]:
    txt = read(os.path.join(APP, "assets", name))
    versions = re.findall(r"^\s{2}(0\.93-kr\d)\s*$", txt, flags=re.M)
    if not versions:
        err("%s 에서 버전 목록을 찾지 못함" % name)
    elif versions[0] != "0.93-kr6":
        err("%s 의 첫 버전이 0.93-kr6 이 아님 (%s)" % (name, versions[0]))

if errors:
    print("문제 %d건:" % len(errors))
    for e in errors:
        print("  [오류] " + e)
    sys.exit(1)
print("0.93-kr6 추가 검증 통과 (main_activity 변형 %d개, 호출+로그 2분할, 디코드 2열)"
      % len(VARIANTS))
