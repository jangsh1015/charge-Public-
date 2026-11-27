#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""0.93-kr7 변경(원터치 호출·즉시 응답, 무응답 횟수 준수)에 대한 추가 정적 검증.

check_source.py · check_kr4.py · check_kr6.py 가 보지 않는 것들을 확인한다.
  1) 버전 표기가 0.93-kr7 (versionCode 7)이고 문서·readme 에 내역이 맨 위에 있는지
  2) 목록 줄 원터치(짧은 탭) 배선: 어댑터의 탭 처리기 + 두 화면의 탭 처리 함수
  3) 자동 응답 놓침 방지: 딥 디코드도 파싱, 시간 한계 3500/8000ms, 같은 슬롯 중복 방지,
     CQ 대기 중에는 송신 중이어도 목표를 잡는지
  4) 무응답 횟수 준수: RR73 단계도 설정값에서 멈추고, 포기 시 30분 쿨다운 + CQ 복귀
  5) 피클·AI 언급이 사용자 문서에 들어가지 않았는지
"""
import glob
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP = os.path.join(ROOT, "ft8cn", "app", "src", "main")
JAVA = os.path.join(APP, "java", "com", "bg7yoz", "ft8cn")

errors = []


def err(msg):
    errors.append(msg)


def read(path):
    return io.open(path, encoding="utf-8").read()


def strip_comments(txt):
    txt = re.sub(r"/\*.*?\*/", "", txt, flags=re.S)
    return re.sub(r"//[^\n]*", "", txt)


# --- 1) 버전·문서 -----------------------------------------------------------
gradle = read(os.path.join(ROOT, "ft8cn", "app", "build.gradle"))
if "versionCode 7" not in gradle:
    err("build.gradle versionCode 가 7 이 아님")
if "versionName '0.93-kr7'" not in gradle:
    err("build.gradle versionName 이 0.93-kr7 이 아님")

DOCS = [("readme_ko.txt", os.path.join(APP, "assets", "readme_ko.txt")),
        ("readme_en.txt", os.path.join(APP, "assets", "readme_en.txt")),
        ("README_KR.md", os.path.join(ROOT, "README_KR.md"))]
for name, path in DOCS:
    if "0.93-kr7" not in read(path):
        err("%s 에 0.93-kr7 내역이 없음" % name)

for name in ["readme_ko.txt", "readme_en.txt"]:
    txt = read(os.path.join(APP, "assets", name))
    versions = re.findall(r"^\s{2}(0\.93-kr\d)\s*$", txt, flags=re.M)
    if not versions:
        err("%s 에서 버전 목록을 찾지 못함" % name)
    elif versions[0] != "0.93-kr7":
        err("%s 의 첫 버전이 0.93-kr7 이 아님 (%s)" % (name, versions[0]))

# --- 2) 목록 줄 원터치(짧은 탭) ---------------------------------------------
adapter = strip_comments(read(os.path.join(JAVA, "ui", "CallingListAdapter.java")))
for need, why in [
    ("interface OnItemTapHandler", "탭 처리기 인터페이스가 없음"),
    ("setOnItemTapHandler", "탭 처리기 등록 함수가 없음"),
    ("holder.onItemTapHandler = onItemTapHandler", "바인딩 때 탭 처리기를 넘기지 않음"),
    ("getAdapterPosition()", "탭한 줄의 위치를 읽지 않음"),
]:
    if need not in adapter:
        err("CallingListAdapter: " + why)
# 기존 롱프레스 메뉴(컨텍스트 메뉴)는 그대로 살아 있어야 한다
if "setOnCreateContextMenuListener(menuListener)" not in adapter:
    err("CallingListAdapter: 롱프레스 컨텍스트 메뉴가 사라짐")
# setOnItemClickListener 를 쓰는 화면(GridTracker)의 리스너를 덮어쓰지 않아야 한다
if "if (listener != null)" not in adapter:
    err("CallingListAdapter: 기존 OnItemClickListener 우선 처리가 없음")

for fname, mode in [("MyCallingFragment.java", "MY_CALLING"),
                    ("CallingListFragment.java", "CALLING_LIST")]:
    txt = strip_comments(read(os.path.join(JAVA, "ui", fname)))
    for need, why in [
        ("setOnItemTapHandler", "탭 처리기를 등록하지 않음"),
        ("private void onMessageTapped(int position)", "탭 처리 함수가 없음"),
        ("private boolean canCallMessage(Ft8Message message)", "호출 가능 여부 검사가 없음"),
        ("checkIsMyCallsign(callsignFrom)", "내가 보낸 줄(내 송신) 제외가 없음"),
        ("try {", "예외 보호가 없음"),
    ]:
        if need not in txt:
            err("%s: %s" % (fname, why))

calling = strip_comments(read(os.path.join(JAVA, "ui", "CallingListFragment.java")))
if "catch (Exception e)" not in calling:
    err("CallingListFragment: 화면 전환 예외 처리(catch)가 없음")
if "isAdded()" not in calling:
    err("CallingListFragment: 프래그먼트 상태 확인이 없음")

# --- 3) 자동 응답 놓침 방지 --------------------------------------------------
main_vm = strip_comments(read(os.path.join(JAVA, "MainViewModel.java")))
for need, why in [
    ("AUTO_PARSE_LIMIT_MS = 3500", "1차 디코드 시간 한계가 3500ms 가 아님"),
    ("DEEP_DECODE_PARSE_LIMIT_MS = 8000", "딥 디코드 시간 한계가 없음"),
    ("collectSlotMessages", "같은 슬롯 메시지 합치기가 없음"),
    ("lastAutoParsedUtc", "같은 슬롯 중복 처리 방지가 없음"),
    ("getFunctionOrder() == 6", "CQ 대기 판별이 없음"),
    ("canParseWhileTransmitting", "CQ 대기 중 송신과 겹쳐도 목표를 잡는 처리가 없음"),
]:
    if need not in main_vm:
        err("MainViewModel: " + why)
if "!isDeep//" in main_vm or "&& !isDeep" in main_vm:
    err("MainViewModel: 딥 디코드를 통째로 건너뛰는 예전 조건이 남아 있음")
if "<= 2000" in main_vm:
    err("MainViewModel: 예전 2000ms 한계가 남아 있음")

signal = strip_comments(read(os.path.join(JAVA, "ft8transmit", "FT8TransmitSignal.java")))
for need, why in [
    ("public int getFunctionOrder()", "현재 송신 단계를 읽는 함수가 없음"),
    ("CQ 대기 중 호출 발견", "CQ 대기 중 '나를 부른 국' 발견 처리가 없음"),
]:
    if need not in signal:
        err("FT8TransmitSignal: " + why)

# checkCQMeOrFollowCQMessage 의 두 번째 고리에서 resetTargetReport 후 setTransmit 하는지
m = re.search(r"if \(\(GeneralVariables\.checkIsMyCallsign\(msg\.getCallsignTo\(\)\)"
              r"[\s\S]{0,1400}?return true;", signal)
if not m:
    err("FT8TransmitSignal: checkCQMeOrFollowCQMessage 의 응답 고리를 찾지 못함")
else:
    body = m.group(0)
    if "resetTargetReport()" not in body:
        err("FT8TransmitSignal: 나를 부른 국을 잡을 때 resetTargetReport() 가 없음")
    if "mutableFunctionOrder.postValue" not in body:
        err("FT8TransmitSignal: 나를 부른 국을 잡은 뒤 화면(단계)에 알리지 않음")

# --- 4) 무응답 횟수 준수 -----------------------------------------------------
if "noReplyCount >= GeneralVariables.noReplyLimit" not in signal:
    err("FT8TransmitSignal: RR73 단계도 설정한 무응답 횟수에서 멈추는 조건이 없음")
if re.search(r"noReplyCount\s*>\s*GeneralVariables\.noReplyLimit\s*\*\s*2", signal):
    err("FT8TransmitSignal: RR73 단계의 2배 허용(예전 동작)이 남아 있음")
if "boolean targetGaveUp" not in signal or "boolean qsoFinished" not in signal:
    err("FT8TransmitSignal: 교신 완료/포기 구분이 없음")
m = re.search(r"if \(qsoFinished \|\| targetGaveUp\) \{[\s\S]{0,600}?return;", signal)
if not m:
    err("FT8TransmitSignal: 교신 완료/포기 처리 고리를 찾지 못함")
else:
    body = m.group(0)
    if "startNoReplyCooldown" not in body:
        err("FT8TransmitSignal: 포기한 국의 30분 쿨다운이 없음")
    if "resetToCQ()" not in body:
        err("FT8TransmitSignal: 포기 후 CQ 복귀가 없음")

m = re.search(r"private boolean handleNoReplyLimitReached[\s\S]{0,1200}?\n    \}", signal)
if not m:
    err("FT8TransmitSignal: handleNoReplyLimitReached 를 찾지 못함")
else:
    body = m.group(0)
    if "resetToCQ()" not in body:
        err("FT8TransmitSignal: 무응답 중단 시 resetToCQ() 로 정리하지 않음")
    if "startNoReplyCooldown" not in body:
        err("FT8TransmitSignal: 무응답 중단 시 30분 쿨다운이 없음")

# 약한 신호 판정: 목록 첫 메시지 하나만 보지 않고 이 슬롯 전체를 보는지
if "decodedStrongSignal" not in signal:
    err("FT8TransmitSignal: 약한 신호 무응답 계산이 첫 메시지 하나에만 의존함")
if "messages.get(0).isWeakSignal" in signal:
    err("FT8TransmitSignal: 예전 messages.get(0).isWeakSignal 검사가 남아 있음")

# --- 5) 피클·AI 언급 금지 ----------------------------------------------------
for path in [os.path.join(APP, "assets", "readme_ko.txt"),
             os.path.join(APP, "assets", "readme_en.txt"),
             os.path.join(APP, "assets", "no_response_help_ko.txt"),
             os.path.join(APP, "assets", "no_response_help_en.txt"),
             os.path.join(ROOT, "README_KR.md")]:
    txt = read(path)
    for word in ["피클", "Pickle", "픽클"]:
        if word in txt:
            err("%s 에 '%s' 언급이 있음" % (os.path.basename(path), word))

if errors:
    print("문제 %d건:" % len(errors))
    for e in errors:
        print("  [오류] " + e)
    sys.exit(1)
print("0.93-kr7 추가 검증 통과 (원터치 호출·딥 디코드 즉시 응답·무응답 횟수 준수)")
