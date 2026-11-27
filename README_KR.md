# FT8CN 한국어판 (간헐 CQ 추가) — 빌드용 소스

안드로이드 FT8 앱 **FT8CN**(원본: [N0BOY/FT8CN](https://github.com/N0BOY/FT8CN), 개발 BG7YOZ, MIT 라이선스)의
**한국어 UI + 간헐 CQ(주기적 Auto CQ)** 개조판 소스입니다.
개인 사용 목적으로 만든 빌드이며, 원본의 라이선스와 저작권 표시(`LICENSE`)를 그대로 유지합니다.

앱 패키지 이름은 `com.bg7yoz.ft8cn.kr`, 앱 이름은 `FT8CN KR`, 버전은 `0.93-kr8` 입니다.
패키지 이름이 원본과 달라서 **원본 FT8CN 과 나란히 설치**할 수 있습니다.

---

## 1. 원본과 다른 점

### (1) 한국어 UI 전면 번역
- `ft8cn/app/src/main/res/values-ko/strings.xml` — 원본 `values/strings.xml`(영어)의 모든 번역 대상
  문자열(502개)을 한국어 아마추어무선 용어로 옮겼습니다. (CQ 호출, 송신/수신, 워터폴, SWR, 호출부호,
  그리드, 자동 응답, QSO 로그 등)
- `ft8cn/app/src/main/assets/*_ko.txt` — 앱 안의 도움말과 안내문 26개를 한국어로 새로 썼습니다.
  설정 화면의 ⓘ 버튼과 `정보` 창이 이 파일들을 엽니다.
  `readme_ko.txt`(정보 창)에는 원작(BG7YOZ / N0BOY)과 한국어 번역·간헐 CQ 수정(DS2YTO) 크레딧이 들어 있습니다.
  중국어 원문만 있던 `readme.txt`·`rigaddress.txt` 는 영어(`_en`)와 한국어(`_ko`) 판을 따로 두어,
  한국어·영어 사용자에게 중국어가 보이지 않게 했습니다.
- 언어는 휴대폰의 시스템 언어를 따릅니다. 휴대폰이 한국어이면 자동으로 한국어 화면이 됩니다.
  (안드로이드 13 이상에서는 설정 > 앱 > FT8CN KR > 언어에서 앱 언어만 따로 고를 수도 있습니다.)

### (2) 간헐 CQ (주기적 Auto CQ)
1층·포터블처럼 무전기 발열과 배터리가 걱정되는 자리에서, CQ를 계속 내지 않고
**일정 시간마다 2~3번만 CQ를 내고 나머지는 수신만** 하도록 만든 기능입니다.

- 설정 화면에 두 항목이 추가되었습니다.
  - **간헐 CQ** : `꺼짐(연속)` / `5분마다` / `10분마다` / `15분마다` / `20분마다` / `30분마다` / `60분마다`
  - **1회 CQ 송신** : 한 번에 내는 CQ 횟수 `1~5회` (기본 3회)
- 설정은 데이터베이스에 저장되어 앱을 다시 켜도 유지됩니다. (키: `intervalCQ`, `intervalCQCount`)

동작 방식

| 상태 | 송신 |
|---|---|
| 간헐 CQ 꺼짐 | 원본과 같이 CQ를 계속 냅니다. |
| CQ 묶음 차례 | 내 송신 차례마다 CQ를 정한 횟수만큼 냅니다. (FT8은 15초 슬롯의 내 차례에만 송신하므로 3회면 약 1분) |
| 대기 | 송신하지 않고 **수신·디코드만** 합니다. |
| 누군가 내 호출부호를 부르거나 응답 | **즉시** 평소 자동 응답(Auto Answer) 교신으로 넘어갑니다. |
| 교신 종료 | 다시 간헐 CQ로 돌아가 CQ 묶음을 한 번 내고 대기합니다. |

- 화면 위쪽 대상 표시줄에 `간헐 10분 4:32` 처럼 남은 시간이 표시됩니다.
- 송신 감시(Tx watchdog)가 주기보다 짧으면 대기 중에 자동 송신이 멈출 수 있어 경고를 한 번 보여 줍니다.
  `0.93-kr2`부터는 **간헐 CQ로 대기하는 동안은 송신 감시 시간을 세지 않으므로**, 감시를 짧게 두어도
  대기 중에 꺼지지 않습니다. 그래도 묶음 송신이 긴 경우를 대비해 주기보다 길게 두거나 `무시`로 두면 안전합니다.
- `CQ 자동 추적`/`추적 자동 호출`이 켜져 있으면 대기 중에도 새 국을 먼저 부를 수 있습니다.
  무전기를 조용히 두고 싶으면 두 기능을 끄세요.

### (3) 0.93-kr2에서 더한 것
- `정보` 창을 `readme_ko.txt` 로 연결했습니다. 면책·사용 안내와 함께 원작(BG7YOZ / N0BOY),
  한국어 번역·간헐 CQ 수정(DS2YTO), FT8/FT4 프로토콜(K9AN·G4WJS·K1JT) 크레딧이 들어 있습니다.
- 무전기 모델 목록(`rigaddress_ko.txt`)과 시리얼 연결 끊김 안내, 웹 UI 업로드 버튼 문구를 한국어로 바꿨습니다.
- 간헐 CQ 대기 중에는 송신 감시 시간을 세지 않아, 대기 중에 자동 송신이 꺼지지 않습니다.
- 호출 탭의 확성기 아이콘: 송신 중은 빨간색 깜빡임, **자동 송신이 켜진 대기 중은 주황색**, 꺼짐은 X 아이콘입니다.

### (4) 0.93-kr3에서 더한 것
- **무응답 재호출 방지(30분 쿨다운)**: `추적 자동 호출`이 부른 국이 `무응답 N회 중단`으로 끊기면,
  그 콜사인(대문자로 정리)을 30분 쿨다운 목록(`ConcurrentHashMap<String, Long>`)에 넣고
  30분 동안 추적 자동 호출에서 건너뜁니다. 같은 국이 계속 CQ를 내도 다시 물고 들어가지 않고
  다른 새 CQ 국을 찾습니다. 직접 콜사인을 눌러 부르거나 상대가 먼저 부르면 쿨다운과 관계없이
  응답하며(이때 쿨다운도 지움), 30분이 지난 기록은 확인할 때와 1초 하트비트에서 자동 정리합니다.
- `무응답 N회 중단` 설정은 원래 `무시(0)` ~ `30회`까지 고를 수 있어 `3회`·`5회`·`7회`도 그대로 선택할 수 있습니다.
- **초기 기본 주파수 40m**: `GeneralVariables.band`·`OperationBand.getDefaultBand()`·
  `DatabaseOpr` 의 `bandFreq` 기본값을 `7.074 MHz (40m)`로 바꾸고, 처음 설치해 저장된 밴드가 없을 때는
  `MainActivity.InitData()`에서 밴드 목록 위치(`bandListIndex`)도 기본 밴드로 맞춥니다.
  (대한민국 3급은 14 MHz 대역이 수신만 가능해, 새 기기에서 20m로 시작하던 것을 40m로 바꿈)

### (5) 0.93-kr4에서 더한 것
- **태블릿 가로 3분할**: `res/layout-sw600dp-land/main_activity.xml` 에 3분할 컨테이너를 두고,
  작은 화면 폭이 600dp 이상이면서 가로일 때만 켭니다(`MainActivity.isTabletSplitConfiguration()`).
  - 왼쪽: `SpectrumFragment`(스펙트럼·워터폴), 오른쪽 위: `CallingListFragment`(디코드 목록),
    오른쪽 아래: `MyCallingFragment`(호출). 태블릿 가로에서는 두 목록 프래그먼트가
    `layout-sw600dp-land/fragment_calling_list.xml`·`fragment_my_calling.xml`(스펙트럼 없는 목록 전용)을
    쓰므로 스펙트럼이 겹치지 않습니다. 스마트폰 세로·가로 화면의 기존 2분할은 그대로입니다.
  - 경계선 손잡이(16dp 터치 영역, 가운데 4dp 알약)를 끌어 비율을 바꿉니다. 스펙트럼 30~75%,
    디코드 20~80% 범위로 제한하고, `SharedPreferences`(`tabletSplit/splitWidthRatio`·`splitHeightRatio`)에
    저장해 재시작 후에도 유지합니다.
  - 하단 탭에서 디코드·호출·스펙트럼을 누르면 3분할을 보여 주고, QSO 로그·설정·QRZ 화면은
    기존처럼 전체 화면(NavHost)으로 엽니다. 3분할 패널 프래그먼트는 계속 살아 있어
    탭을 오가도 구성과 스펙트럼이 유지됩니다.
  - 회전 등으로 3분할 레이아웃이 아니게 되면 남은 패널 프래그먼트를 정리해
    `No view found for id` 오류가 나지 않게 했습니다.
- **워터폴·스펙트럼 색감**: `SpectrumColorTable` 을 새로 두고 256단계 색상표를 미리 계산합니다.
  잡음 구간은 `#04070F`~`#0E2755` 딥 네이비, 신호는 청록(`#18CEE2`) → 노랑(`#FFE03A`) →
  주황(`#FF9612`) → 빨강(`#FF2E2E`)으로 올라갑니다. `WaterfallView` 의 원색 파랑 계산을 이 표로 바꾸고,
  `ColumnarView` 는 상단 청록/하단 원색 파랑 그라데이션 대신 막대마다 레벨 색을 칠합니다.
- **IC-705 기준 초기 설정**: `GeneralVariables` 기본값을 `modelNo`=IC-705(무전기 목록에서 이름으로 찾고,
  없으면 2번), `civAddress`=0xA4, `baudRate`=115200, `noReplyLimit`=3 으로 바꿨습니다(SWR·ALC 경보는 원래 기본 ON).
  이미 쓰던 설치본도 혜택을 보도록 앱을 처음 켜는 때 `MainActivity.applyIc705PresetIfNeeded()` 가
  `model`·`instruction`·`civ`·`baudRate`·`swrSwitch`·`alcSwitch`·`noReplyLimit` 7개 값을 DB에 한 번만 넣습니다
  (`ic705Preset/version`=4 로 기록, 이후에는 사용자가 바꾼 값을 다시 덮지 않음).
  제어 방식(`ctrMode`)과 Icom LAN 주소·포트 설정은 건드리지 않습니다.

### (6) 0.93-kr5에서 더한 것
- **끝없는 재호출 방지**: `FT8TransmitSignal` 에 같은 콜사인·같은 단계의 메시지를 몇 번 보냈는지
  세는 안전장치(`isSameMessageRepeatLimitReached()`)를 두었습니다. `무응답 N회 중단`(기본 3회)만큼
  같은 메시지를 보내면 리포트(`R-11` 등) 단계에서도 자동 호출을 멈추고 CQ/대기로 돌아갑니다.
  - 어떤 응답이든 무응답 횟수를 0으로 되돌리던 것을, **단계가 앞으로 나아갔을 때만** 되돌리도록
    바꿨습니다. 같은 리포트를 되풀이하는 응답(제자리걸음)은 무응답으로 셉니다.
  - 상대가 `RR73`/`RRR`을 보내면 그 자리에서 73을 한 번만 보내고 교신을 마친 뒤 CQ로 돌아갑니다.
    `73`/`RR73`/`RRR` 메시지에는 리포트로 답하지 않습니다(그래서 상대의 RR73에 리포트를 다시 보내지 않음).
  - 무응답 한도 처리는 `handleNoReplyLimitReached()` 하나로 모았습니다.
- **교신 목록 색(아이폰 FT8-Improved/WSJT-X식)**: `CallingListAdapter.applyRowStyle()` 이 행마다
  - 내 송신(TX) = 다크 레드 `#B91C1C`→`#991B1B` + 흰 글씨
  - 나를 호출한 수신(RX) = 다크 핑크 `#9D174D`→`#831843` + 연핑크 `#FCE7F3`
  - CQ = 다크 그린 `#166534`→`#14532D` + 연두 `#DCFCE7`
  - 그 밖의 교신 = 다크 네이비 `#1E293B`→`#162238` + `#E2E8F0`
  을 적용합니다(밝은 파란 바탕 제거). 제목 막대는 `#312E81`, 열 제목 줄은 `#1B2437` 로 맞췄습니다.
- **워터폴 위 세로 막대그래프 제거**: `fragment_spectrum.xml` 에서 `ColumnarView` 를 없애고
  `WaterfallView` 를 눈금선(`rulerFrequencyView`) 바로 아래부터 화면 아래까지 채웠습니다.
  시간·대역 표시는 워터폴 위에 겹쳐 보이도록 순서와 제약을 바꿨습니다.
- **도구 버튼 위치**: 태블릿 가로 3분할에서는 전체화면·주파수·볼륨·지구본 버튼이 오른쪽 목록을
  가리지 않도록 왼쪽 워터폴 패널의 오른쪽 아래 구석으로 옮겨 갑니다
  (`MainActivity.updateFloatViewPlacement()`/`placeFloatViewInSplitCorner()`). 경계선을 끌어
  패널 폭이 바뀌면 따라가고, 3분할이 아닐 때는 예전처럼 오른쪽 가장자리에 있습니다.
- **태블릿 하단 탭 화면 고르기**: 3분할이 늘 떠 있던 것을, 탭으로 고르게 바꿨습니다.
  `스펙트럼` 탭은 3분할(스펙트럼+디코드+호출)을 열고, 한 번 더 누르면 `splitSpectrumOnly` 로
  스펙트럼 단독으로 접힙니다. `디코드` 탭은 디코드 목록만, `호출` 탭은 호출 화면만 전체로 엽니다
  (`updateSplitVisibility()` 는 목적지가 `menu_nav_spectrum` 일 때만 3분할을 보여 줌).
  마지막 화면(3분할/단독)은 `tabletSplit/splitPreferred` 에 저장해 재시작해도 유지됩니다.
- **시각 동기화 실패 안내**: `ConfigFragment.syncFailed()` 가 `e.getMessage()`(영어 원문) 대신
  `time_sync_failed_network` 문자열로 "인터넷에 연결되어 있지 않아 시각 동기화에 실패했습니다.
  (무전기 Wi-Fi에 연결된 동안에는 동기화가 되지 않습니다)" 를 보여 줍니다.

### (7) 0.93-kr6에서 더한 것
- **디코드 2열 (태블릿 가로 단독 화면)**: `CallingListFragment` 가 태블릿 가로
  (`orientation == LANDSCAPE && smallestScreenWidthDp >= 600`) 이고 3분할 패널 안이 아닐 때
  `GridLayoutManager(context, 2)` 로 목록을 왼쪽·오른쪽 2열로 보여 줍니다.
  패널 판별은 `MainActivity.isSplitPanelFragment(getTag())` 로 합니다(패널 프래그먼트는
  `splitDecode`·`splitCalling`·`splitSpectrum`·`splitCallOnly`·`splitLog` 태그를 씀).
  셀 좌우 여백은 기존 항목 여백(1dp)을 그대로 써서 2열 사이가 2dp로 떨어지고,
  3분할 안의 목록과 세로·폰 화면은 1열(`LinearLayoutManager`) 그대로입니다.
- **호출 탭 2분할 (태블릿 가로)**: `layout-sw600dp-land/main_activity.xml` 에
  `splitCallLogLayout`(왼쪽 `splitCallingOnlyContainer` = `MyCallingFragment`,
  가운데 드래그 핸들 `splitCallLogHandle`, 오른쪽 `splitLogContainer` = `LogFragment`, 기본 50:50)을
  추가하고, `MainActivity.updateSplitVisibility()` 가 목적지가 `menu_nav_mycalling` 일 때만
  이 패널을 띄웁니다(`splitLayout`·NavHost 는 숨김). 경계선을 끌면 30~70% 범위에서
  좌우 비율이 바뀌고 `tabletSplit/callLogRatio` 에 저장됩니다.
  디코드 목록에서 행을 눌러 호출 화면으로 넘어갈 때도 목적지가 같아져 같은 2분할이 열립니다.
- **화면 기억(`lastMode`)**: `tabletSplit/lastMode` 에 `decode`·`myCalling`·`spectrumSplit`·
  `spectrumOnly` 를 저장해, 앱을 다시 켜면 그 화면으로 시작합니다(기록이 없으면 `splitPreferred`
  값에 따라 3분할 또는 디코드 단독). `QSO 로그`·`설정` 탭은 지금처럼 전체 화면이고,
  `스펙트럼` 탭은 3분할(한 번 더 누르면 스펙트럼 단독) 그대로입니다.
- **QSO 로그 정돈**: 2분할 오른쪽에서도 잘 보이도록 `fragment_log.xml` 의 제목 막대 색을
  밝은 보라(`purple_500`)에서 디코드·호출 목록과 같은 어두운 인디고(`list_header_bar_color`,
  `#312E81`)로 맞췄습니다. 로그 항목은 고정 폭 대신 시작 여백(88dp)+유동 폭이라 좁은 패널에서도
  글씨가 잘리지 않습니다.
- **컨텍스트 메뉴 가드**: QSO 로그 화면이 패널·로그 탭 두 벌 살아 있을 수 있어,
  `LogFragment.onContextItemSelected()` 가 자기 목록 항목(`isMyListItemView`)에만 반응하게 했습니다
  (호출 목록에서 연 메뉴가 로그 항목을 지우거나 QSL 을 바꾸는 일을 막음).

### (8) 0.93-kr7에서 더한 것
- **목록 줄 원터치 호출·응답**: `CallingListAdapter` 에 `OnItemTapHandler` 를 더하고,
  `MyCallingFragment`(호출 창)와 `CallingListFragment`(디코드 창)가 그것을 등록합니다.
  줄을 한 번 짧게 탭하면 `onMessageTapped()` 가 그 줄의 콜사인을 `대상:` 에 넣고 바로 호출합니다.
  - 나를 부른 줄(수신 핑크)은 `doReplyNow()` 로 받은 단계에 맞춰 응답하고,
    CQ(초록)·다른 국은 `doCallNow()` 로 부릅니다.
  - 내 송신(TX) 줄, 콜사인 없는 줄(`<...>`), 텔레메트리·자유문 줄은 탭해도 반응하지 않습니다
    (`canCallMessage()`).
  - 길게 누르면 예전과 같은 컨텍스트 메뉴가 그대로 뜨고, `GridTrackerMainActivity` 처럼
    `setOnItemClickListener` 를 쓰는 화면은 그 리스너가 우선합니다.
  - 태블릿 2열·3분할처럼 옮겨 갈 화면이 없는 구성에서는 `navigateToMyCallFragment()` 가
    `try-catch` 와 `isAdded()` 확인으로 막혀 앱이 꺼지지 않습니다(`MainActivity.isSplitPanelVisible()` 이면 이동하지 않음).
- **자동 응답·CQ 추적이 '나를 부른 국'을 놓치지 않게**: `MainViewModel.afterDecode()`
  - 예전에는 `isDeep`(딥 디코드)이면 자동 프로그램을 통째로 건너뜀 → 이제 CQ 대기(`functionOrder == 6`) 중이거나
    그 슬롯을 아직 처리하지 않았으면 딥 디코드 결과도 `parseMessageToFunction()` 에 태웁니다.
    딥 디코드는 새로 풀린 메시지만 주므로 `collectSlotMessages(utc)` 로 같은 슬롯 메시지와 합쳐 넘깁니다.
  - 시간 한계를 `<= 2000ms` → `AUTO_PARSE_LIMIT_MS 3500ms`(1차) / `DEEP_DECODE_PARSE_LIMIT_MS 8000ms`(딥)로 넓혔습니다.
  - 같은 슬롯을 두 번 처리하지 않도록 `lastAutoParsedUtc` 로 막습니다.
  - CQ를 내는 중에는 내 CQ가 나가는 중이어도 목표를 잡습니다(`canParseWhileTransmitting`).
    이 슬롯의 송신은 이미 정해졌으므로 다음 기회부터 응답이 나가고, 같은 슬롯에 CQ를 또 내지 않습니다.
  - `FT8TransmitSignal.checkCQMeOrFollowCQMessage()` 는 나를 부른 국을 찾으면 `resetTargetReport()` 로
    신호 기록을 비우고 목표·단계를 화면에 알립니다.
- **'무응답 N회 중단'을 설정값대로**:
  - RR73 단계도 설정한 횟수(`noReplyCount >= noReplyLimit`)에서 멈춥니다(예전에는 이 단계만 2배까지 기다려 정해 둔 횟수보다 늦게 멈췄음).
  - 포기한 국은 30분 쿨다운에 넣고(`startNoReplyCooldown`), `resetToCQ()` 로 신호 기록·간헐 CQ 상태를 함께 정리해 CQ(대기)로 돌아갑니다.
  - 약한 신호 판정을 목록 첫 메시지 하나가 아니라 이 슬롯 전체(`decodedStrongSignal`)로 보아, 무응답 횟수가 덜 세어지던 문제를 고쳤습니다.
  - 무응답 후 새 CQ 국으로 넘어갈 때는 그 CQ 메시지의 주파수·시퀀스로 목표를 다시 만들어(`getNewTargetCallsign()`) 송신 위치가 엇갈리지 않습니다.

### (9) 0.93-kr8에서 더한 것
- **QSO 로그 실시간 자동 새로 고침**: `DatabaseOpr` 에 `OnQsoLogChanged` 리스너를 두고, QSO 기록이
  DB에 저장되는 한 곳(`doInsertQSLData()`)에서 저장이 끝나면 리스너를 부릅니다
  (자동 교신 완료 `addQSL_Callsign()` · 웹 UI 업로드 `LogHttpServer` · 공유 로그 가져오기 `ImportSharedLogs` 가
  모두 이 함수를 지나갑니다).
  `MainViewModel` 은 리스너를 등록해 `mutableQsoLogUpdated`(정수 카운터 LiveData) 값을 하나씩 올리고,
  `LogFragment` 는 `observe(getViewLifecycleOwner(), ...)` 로 그 값을 지켜보다가
  `queryByCallsign(mainViewModel.queryKey, 0)` 를 다시 실행해 목록을 그 자리에서 갱신합니다.
  여러 건이 잇따라 저장될 때(공유 로그 가져오기 등)는 250ms 로 미뤄 마지막 한 번만 읽습니다.
  화면에 보이지 않는 동안(패널이 `GONE`, 앱이 백그라운드)에는 읽지 않고, 다시 보일 때 맞춥니다.
- **호출 탭 2분할 로그 패널의 바탕색**: `fragment_log.xml` 루트(`logFrameLayout`)와
  `splitLogContainer` 에 `log_panel_background`(`values` `#FFFFFFFF`, `values-night` `#FF121212` — 앱 창 바탕과 같은 톤)를
  지정했습니다. 예전에는 `splitCallLogLayout` 의 검은 바탕 위에 로그 항목의 반투명 바탕과 어두운 글씨가
  겹쳐, 상단 검색 바 아래가 까맣게 비어 보였습니다.
- **패널을 다시 보일 때 한 번 더 읽기**: `MainActivity.updateSplitVisibility()` 가 호출 탭으로 전환해
  `splitCallLogLayout` 이 `VISIBLE` 이 될 때 `LogFragment.refreshLogPanel()` 을 부릅니다. 예전의
  `refreshIfQueryChanged()` 처럼 검색어가 같을 때 건너뛰지 않고, 보일 때마다 지금 조건으로 다시 읽습니다
  (숨은 동안 새 교신이 저장되거나 지운 기록도 반영). 앱으로 돌아왔을 때(`onResume()`)도 같은 방식으로 한 번 맞춥니다.
- **송신 감시 기본값을 '무시'(제한 없음)로**: `GeneralVariables.DEFAULT_LAUNCH_SUPERVISION` 을 `0` 으로 바꿔,
  `launchSupervision` 의 처음 값과 DB에 값이 없을 때(`DatabaseOpr` 의 설정 읽기) 모두 0이 되게 했습니다.
  자동 송신을 켜 두면 따로 설정하지 않아도 사용자가 직접 끌 때까지 계속 돌아갑니다.
  설정 화면에서 5·15·25분… 을 직접 고르면 그 값이 DB에 저장돼 그대로 쓰입니다.
  예전 기본값(10분 = `600000`)은 설정 목록에 없는 값이라 사용자가 고를 수 없으므로,
  `LEGACY_DEFAULT_LAUNCH_SUPERVISION` 으로 두고 읽을 때 '무시'로 보아 예전 설치도 같게 동작합니다.
  또 `ConfigFragment` 의 스피너가 화면을 열 때 처음 한 번 알리는 것을 '고른 것'으로 보지 않도록
  값이 같으면 DB에 쓰지 않게 했습니다(설정을 열기만 해도 값이 저장되는 일을 막음).
- **QSO 로그 화면의 [PSK] 버튼(내 콜사인·그리드)**: `fragment_log.xml` 상단 바에 있던 지도(핀)
  `locationInMapImageButton` 을 `pskGridButton`(TextView `PSK`)으로 바꿨습니다.
  누르면 `LogFragment.showPskGridDialog()` 가 다이얼로그를 띄워 `GeneralVariables.myCallsign` 과
  `GeneralVariables.getMyMaidenheadGrid()` / `getMyMaidenhead4Grid()` 을 **누르는 순간 읽어** 보여 줍니다
  (설정에서 콜사인을 바꾸면 버튼도 그 콜사인을 그대로 따라감. 콜사인·그리드의 기본값은 DB 설정값이고,
  kr4의 IC-705 초기 설정처럼 콜사인을 코드에 박아 두지는 않았습니다 — 허가받은 콜사인으로 송신해야 하므로
  처음 설치 때 설정 화면에서 직접 넣게 되어 있습니다).
  - `[PSK Reporter 열기]` : `Intent.ACTION_VIEW` 로
    `https://pskreporter.info/pskmap.html?preset&callsign=<내콜사인>&txrx=tx&timerange=86400` 을 엽니다.
  - `[내 교신 지도]` : 예전 지도 버튼과 같은 `GridTrackerMainActivity`(검색어·필터 그대로).
  - 브라우저가 없거나 열지 못하면 `HelpDialog` 로 안내만 하고 앱은 그대로 둡니다(`try-catch`).
  - 인터넷은 `ConnectivityManager`(`ACCESS_NETWORK_STATE` — 허가 창이 뜨지 않는 권한)로 확인해,
    IC-705 와이파이처럼 인터넷이 없으면 다이얼로그에 먼저 안내 문구를 넣습니다. 확인할 수 없으면 열기를 막지 않습니다.
- **스펙트럼 3분할의 [📡 호출·송신] / [📋 QSO 로그] 전환 탭**: `main_activity.xml` 세 변형 모두에서
  오른쪽 아래 칸을 `splitCallingBody`(FrameLayout, `layout_weight 50`)로 묶고 그 안에
  `splitCallingContainer`(`MyCallingFragment`)와 `splitCallingLogContainer`(`LogFragment`)를 겹쳐 두었습니다
  (`splitCallingLogContainer` 는 기본 `gone`). 위쪽 `splitCallingToggleBar` 의 두 탭이
  `MainActivity.showSplitCallingTab()` 으로 보이는 쪽만 켜므로, 로그를 보는 동안에도 호출 창과 송신 상태는
  살아 있습니다. 고른 탭은 `SharedPreferences`(`tabletSplit`/`splitCallTab`)에 기억하고,
  로그 탭을 열 때·3분할이 다시 보일 때(`updateSplitVisibility()`) `LogFragment.refreshLogPanel()` 로
  최신 목록을 읽습니다. 경계선을 끌 때의 높이 비율 계산에서는 탭 높이를 빼고 `splitCallingBody` 에 무게를 줍니다.

---

## 2. 빌드 방법 (우분투 / 윈도우 WSL Ubuntu)

```bash
git clone https://github.com/jangsh1015/charge-Public-.git ft8cn-kr
cd ft8cn-kr
chmod +x build_apk.sh
./build_apk.sh
```

스크립트가 자동으로 하는 일:

1. `openjdk-17-jdk`, `unzip`, `wget`, `curl` 설치 (이미 있으면 건너뜀)
2. Android SDK Command-line Tools 내려받아 `~/Android/Sdk/cmdline-tools/latest` 에 설치
3. `platform-tools`, `platforms;android-33`, `build-tools;33.0.2` 설치 + 라이선스 동의
4. `./gradlew assembleDebug` 실행 → `ft8cn/app/build/outputs/apk/debug/app-debug.apk`
5. APK 를 소스 폴더(`FT8CN_KR.apk`)와 윈도우 다운로드 폴더(`/mnt/c/Users/<사용자>/Downloads/FT8CN_KR.apk`)로 복사

### 빌드 요구 사항 (원본 `build.gradle` 기준)

| 항목 | 값 |
|---|---|
| Gradle | 7.5 (wrapper 자동 다운로드) |
| Android Gradle Plugin | 7.4.1 |
| JDK | **17** (AGP 7.4.1 은 JDK 11 이상 필요) |
| compileSdk / targetSdk | 33 |
| buildToolsVersion | 33.0.2 |
| minSdk | 23 |
| NDK / CMake | **필요 없음** (`app/libs/<abi>/libft8cn.so` 미리 빌드된 라이브러리 사용) |

### 설치

APK 를 휴대폰으로 옮겨 눌러 설치하거나, USB 디버깅을 켠 경우:

```bash
~/Android/Sdk/platform-tools/adb install -r ft8cn/app/build/outputs/apk/debug/app-debug.apk
```

---

## 3. 변경한 파일 목록

새로 만든 파일

| 파일 | 내용 |
|---|---|
| `ft8cn/app/src/main/res/values-ko/strings.xml` | 한국어 문자열 전체 |
| `ft8cn/app/src/main/assets/*_ko.txt` (26개) | 한국어 도움말·정보 창 (원작/수정자 크레딧 포함) |
| `ft8cn/app/src/main/assets/readme_en.txt`, `rigaddress_en.txt` | 정보 창·무전기 목록의 영어판 (중국어 제거) |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/IntervalCqSpinnerAdapter.java` | 간헐 CQ 주기 목록 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/IntervalCqCountSpinnerAdapter.java` | 1회 CQ 횟수 목록 |
| `build_apk.sh` | 우분투/WSL 원클릭 빌드 스크립트 |
| `tools/check_source.py` | 빌드 전 정적 검사(Java 문법·XML·리소스 참조) |
| `tools/check_kr4.py` | (kr4) 리소스 참조·데이터바인딩 필드·3분할 id 검사 |
| `tools/check_kr6.py` | (kr6) 호출+로그 2분할 배선·디코드 2열 조건·버전·문서 표기 검사 |
| `tools/check_kr7.py` | (kr7) 원터치 탭 배선·딥 디코드 즉시 응답·무응답 횟수 준수·버전 표기 검사 |
| `tools/check_kr8.py` | (kr8) QSO 로그 실시간 갱신 배선(DB 알림→LiveData→observe)·로그 패널 바탕색·[PSK] 버튼·3분할 로그 탭·버전 표기 검사 |

고친 파일

| 파일 | 내용 |
|---|---|
| `ft8cn/app/build.gradle` | `applicationId` → `com.bg7yoz.ft8cn.kr`, `versionName` → `0.93-kr8`, `buildToolsVersion 33.0.2` |
| `ft8cn/app/src/main/res/values/strings.xml` | 앱 이름 `FT8CN KR`, 간헐 CQ 문자열 9개 추가 |
| `ft8cn/app/src/main/res/layout/fragment_config.xml` | 설정 화면에 `간헐 CQ`, `1회 CQ 송신` 두 줄 추가 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/GeneralVariables.java` | `intervalCQMinutes`, `intervalCQCount` 추가 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/database/DatabaseOpr.java` | 설정 키 `intervalCQ`, `intervalCQCount` 읽기 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java` | 간헐 CQ 판단 로직, 상태 표시, 송신 감시 경고 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/ConfigFragment.java` | 설정 화면 두 항목 연결, 도움말 버튼 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/MyCallingFragment.java` | 대상 표시줄에 간헐 CQ 남은 시간 표시 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/ConfigFragment.java` | (kr2) 정보 창을 로캘별 `readme_help` 리소스로 연결 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/database/RigNameList.java` | (kr2) 무전기 목록을 로캘별 `rig_address_file`(rigaddress_ko/en)로 읽기 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/connector/CableConnector.java` | (kr2) 시리얼 연결 끊김 안내를 리소스 문자열로 변경 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/html/LogHttpServer.java` | (kr2) 웹 UI 업로드 버튼 문구를 리소스 문자열로 변경 |
| `ft8cn/app/src/main/res/values*/strings.xml` | (kr2) `readme_help`, `rig_address_file`, `html_upload`, `serial_connection_lost` 추가 (zh 계열은 중국어 원문 파일 유지) |
| `ft8cn/app/src/main/res/layout*/main_activity.xml` | (kr6) `splitCallLogLayout`(호출 창 / QSO 로그 2분할) 추가 — 폰·가로·태블릿 3개 변형 모두 `visibility="gone"`. (kr8) `splitLogContainer` 에 `log_panel_background` 바탕색. (kr8) 3분할 오른쪽 아래 칸에 `splitCallingToggleBar`(호출·송신 / QSO 로그 탭)와 `splitCallingBody` 안의 `splitCallingLogContainer` 추가 |
| `ft8cn/app/src/main/res/layout/fragment_log.xml` | (kr6) 제목 막대 색을 `list_header_bar_color`(어두운 인디고)로. (kr8) 목록 바탕을 `log_panel_background` 로. (kr8) 지도(핀) 버튼 → `pskGridButton`(`PSK` 글자) |
| `ft8cn/app/src/main/res/values*/strings.xml`, `values-ko/strings.xml` | (kr8) `psk_button_label`·`psk_grid_title`·`psk_grid_info`·`psk_grid_no_callsign`·`psk_grid_no_internet`·`psk_reporter_open`·`psk_grid_qso_map`·`psk_grid_open_failed`·`split_tab_calling`·`split_tab_qso_log` |
| `ft8cn/app/src/main/res/drawable/split_tab_selected.xml`, `split_tab_normal.xml` | (kr8) 3분할 전환 탭 배경 |
| `ft8cn/app/src/main/res/values*/colors.xml` | (kr8) `split_tab_text_selected`, `split_tab_text_normal` |
| `ft8cn/app/src/main/AndroidManifest.xml` | (kr8) `ACCESS_NETWORK_STATE` (PSK 버튼의 인터넷 확인용, 허가 창 없음) |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/database/DatabaseOpr.java` | (kr8) `OnQsoLogChanged` 리스너 — `doInsertQSLData()` 가 QSO 기록을 저장하면 알림 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/LogFragment.java` | (kr8) `mutableQsoLogUpdated` 관찰로 새 교신 저장 시 목록 자동 갱신, `refreshLogPanel()`·`onResume()` 재읽기. (kr8) [PSK] 버튼 — 내 콜사인·그리드 다이얼로그, PSK Reporter 열기(예외·인터넷 확인), 내 교신 지도 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/MainActivity.java` | (kr8) 호출 탭 2분할이 보일 때 `refreshLogPanel()` 호출. (kr8) 3분할 [호출·송신]/[QSO 로그] 전환 탭 — 탭 부착·전환·기억·재읽기 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/GeneralVariables.java` | (kr8) 송신 감시 기본값 `DEFAULT_LAUNCH_SUPERVISION` → `0`(무시), 예전 기본값(10분) 상수 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/ConfigFragment.java` | (kr8) 송신 감시 스피너 — 값이 같으면 DB에 쓰지 않음(설정을 열기만 해도 저장되던 것 방지) |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/CallingListFragment.java` | (kr6) 태블릿 가로 단독 화면에서 `GridLayoutManager(2)` 2열 보기 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/LogFragment.java` | (kr6) 컨텍스트 메뉴가 자기 목록에만 반응하게 (`isMyListItemView`) |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/CallingListAdapter.java` | (kr7) `OnItemTapHandler` — 줄을 한 번 탭하면 호출·응답(롱프레스 메뉴는 유지) |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/MyCallingFragment.java` | (kr7) 원터치 호출·응답, 응답 함수 `doReplyNow()` 분리 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/CallingListFragment.java` | (kr7) 원터치 호출·응답, 화면 전환 예외 보호 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/MainViewModel.java` | (kr7) 딥 디코드도 자동 응답 판단에 포함, 시간 한계 3500/8000ms, 같은 슬롯 중복 방지. (kr8) `mutableQsoLogUpdated` — DB 저장 알림을 LiveData 로 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java` | (kr7) 나를 부른 국 즉시 목표 설정, RR73 단계도 설정한 무응답 횟수에서 중단·30분 쿨다운 |

## 4. 정적 검사

빌드 전에 아래 명령으로 소스를 점검할 수 있습니다 (파이썬 3 + `pip install javalang`).

```bash
python3 tools/check_source.py     # XML·Java 문법·리소스 참조·문자열
python3 tools/check_kr4.py        # 3분할 패널·데이터바인딩 필드
python3 tools/check_kr6.py        # 호출+로그 2분할·디코드 2열·버전 표기
python3 tools/check_kr7.py        # 원터치 호출·딥 디코드 즉시 응답·무응답 횟수·버전 표기
python3 tools/check_kr8.py        # QSO 로그 실시간 갱신 배선·로그 패널 바탕색·버전 표기
```

검사 항목: 모든 XML well-formed, 모든 Java 파일 문법 파싱, `R.string`/`@string`/`R.id`/`@id`/`R.layout`
참조가 실제로 정의되어 있는지, `values-ko` 가 기본 `values` 와 이름·포맷 지정자(`%s`, `%d`, `%.1f`)가
정확히 맞는지.

## 5. 라이선스와 출처

- 원본: FT8CN — https://github.com/N0BOY/FT8CN (개발 BG7YOZ, MIT License)
- 이 소스는 원본 `release` 브랜치 커밋 `a98fbfa` (2025-01-05, 버전 0.93) 를 기준으로 개조했습니다.
- 이 개조판도 같은 MIT 라이선스를 따르며 `LICENSE` 파일을 그대로 두었습니다.
- FT8/FT4 프로토콜: Steve Franke (K9AN), Bill Somerville (G4WJS), Joe Taylor (K1JT).
- 아마추어무선 면허 없이 송신하는 것은 불법입니다. 각국 법규를 지켜 사용하세요.
