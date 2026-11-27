#!/usr/bin/env bash
# =============================================================================
#  FT8CN 한국어판(간헐 CQ) - 우분투 / WSL 원클릭 빌드 스크립트
#
#  사용법 (우분투 또는 윈도우 WSL Ubuntu 터미널에서):
#      chmod +x build_apk.sh
#      ./build_apk.sh
#
#  하는 일:
#    1) JDK 17, unzip, wget, curl 설치 (이미 있으면 건너뜀)
#    2) Android SDK Command-line Tools 설치 (이미 있으면 건너뜀)
#    3) platforms;android-33, build-tools;33.0.2, platform-tools 설치 + 라이선스 동의
#    4) ./gradlew assembleDebug 실행
#    5) 만들어진 APK 를 이 폴더와 윈도우 다운로드 폴더에 복사
#
#  환경 변수로 바꿀 수 있는 것:
#    ANDROID_SDK_ROOT  SDK 설치 위치 (기본: $HOME/Android/Sdk)
#    SKIP_APT=1        apt 설치 단계를 건너뜀 (이미 JDK 가 있을 때)
# =============================================================================
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/ft8cn"          # gradle 루트 (settings.gradle 이 있는 곳)
SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
CMDLINE_TOOLS_DIR="$SDK_ROOT/cmdline-tools/latest"
APK_OUT_NAME="FT8CN_KR.apk"

info()  { printf '\033[1;36m[정보]\033[0m %s\n' "$*"; }
ok()    { printf '\033[1;32m[완료]\033[0m %s\n' "$*"; }
warn()  { printf '\033[1;33m[주의]\033[0m %s\n' "$*"; }
die()   { printf '\033[1;31m[오류]\033[0m %s\n' "$*" >&2; exit 1; }

info "FT8CN 한국어판 빌드 시작"
info "소스 폴더 : $PROJECT_DIR"
info "SDK 폴더  : $SDK_ROOT"

[ -f "$PROJECT_DIR/gradlew" ] || die "gradlew 를 찾을 수 없습니다. 이 스크립트를 소스 최상위 폴더에 두세요."

# ---------------------------------------------------------------- sudo
if [ "$(id -u)" = "0" ]; then
    SUDO=""
else
    command -v sudo >/dev/null 2>&1 || die "sudo 가 없습니다. 관리자 계정으로 실행하거나 sudo 를 설치하세요."
    SUDO="sudo"
fi

# ---------------------------------------------------------------- 1) 기본 패키지
need_apt=0
command -v javac >/dev/null 2>&1 || need_apt=1
command -v unzip >/dev/null 2>&1 || need_apt=1
command -v wget  >/dev/null 2>&1 || need_apt=1
command -v curl  >/dev/null 2>&1 || need_apt=1

if [ "${SKIP_APT:-0}" = "1" ]; then
    info "SKIP_APT=1 : apt 설치 단계를 건너뜁니다."
elif [ "$need_apt" = "1" ]; then
    info "필요한 패키지를 설치합니다 (JDK 17, unzip, wget, curl). sudo 암호를 물을 수 있습니다."
    $SUDO apt-get update -y || die "apt-get update 실패"
    DEBIAN_FRONTEND=noninteractive $SUDO apt-get install -y \
        openjdk-17-jdk unzip wget curl ca-certificates || die "패키지 설치 실패"
else
    ok "JDK/압축 도구가 이미 설치되어 있습니다."
fi

# ---------------------------------------------------------------- JAVA_HOME
if [ -z "${JAVA_HOME:-}" ]; then
    if [ -x /usr/lib/jvm/java-17-openjdk-amd64/bin/javac ]; then
        export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
    else
        _javac="$(command -v javac || true)"
        [ -n "$_javac" ] && export JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$_javac")")")"
    fi
fi
export PATH="$JAVA_HOME/bin:$PATH"

if command -v java >/dev/null 2>&1; then
    JAVA_VER_RAW="$(java -version 2>&1 | head -n 1)"
    info "Java : $JAVA_VER_RAW"
    JAVA_MAJOR="$(printf '%s' "$JAVA_VER_RAW" | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p')"
    if [ -n "${JAVA_MAJOR:-}" ] && [ "$JAVA_MAJOR" -lt 11 ]; then
        die "이 프로젝트(AGP 7.4.1)는 JDK 11 이상이 필요합니다. JDK 17 을 설치하세요: sudo apt install openjdk-17-jdk"
    fi
else
    die "java 를 찾을 수 없습니다."
fi

# ---------------------------------------------------------------- 2) cmdline-tools
mkdir -p "$SDK_ROOT"

if [ -x "$CMDLINE_TOOLS_DIR/bin/sdkmanager" ]; then
    ok "Android 명령줄 도구가 이미 있습니다."
else
    info "Android SDK 명령줄 도구를 내려받습니다…"
    TMP_ZIP="$(mktemp -u /tmp/cmdline-tools-XXXXXX.zip)"
    # 구글 배포 파일 이름이 바뀌므로 알려진 것들을 차례로 시도한다.
    for BUILD in 13114758 11076708 10406996 9477386; do
        URL="https://dl.google.com/android/repository/commandlinetools-linux-${BUILD}_latest.zip"
        info "  시도: $URL"
        if curl -fL --retry 3 --connect-timeout 20 -o "$TMP_ZIP" "$URL"; then
            ok "  내려받기 성공 ($BUILD)"
            DL_OK=1
            break
        fi
        DL_OK=0
    done
    [ "${DL_OK:-0}" = "1" ] || die "Android 명령줄 도구를 내려받지 못했습니다. 인터넷 연결을 확인하세요."

    mkdir -p "$SDK_ROOT/cmdline-tools"
    rm -rf "$SDK_ROOT/cmdline-tools/tmp-unzip"
    unzip -q "$TMP_ZIP" -d "$SDK_ROOT/cmdline-tools/tmp-unzip" || die "압축 해제 실패"
    rm -rf "$CMDLINE_TOOLS_DIR"
    mv "$SDK_ROOT/cmdline-tools/tmp-unzip/cmdline-tools" "$CMDLINE_TOOLS_DIR"
    rm -rf "$SDK_ROOT/cmdline-tools/tmp-unzip" "$TMP_ZIP"
    ok "명령줄 도구 설치 위치: $CMDLINE_TOOLS_DIR"
fi

export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$CMDLINE_TOOLS_DIR/bin:$SDK_ROOT/platform-tools:$PATH"

# ---------------------------------------------------------------- 3) SDK 패키지
info "SDK 패키지 설치 (platform-tools, android-33, build-tools 33.0.2)…"
yes | sdkmanager --sdk_root="$SDK_ROOT" --licenses >/dev/null 2>&1 || true
sdkmanager --sdk_root="$SDK_ROOT" --install \
    "platform-tools" "platforms;android-33" "build-tools;33.0.2" || die "SDK 패키지 설치 실패"

# ---------------------------------------------------------------- 4) 빌드
printf 'sdk.dir=%s\n' "$SDK_ROOT" > "$PROJECT_DIR/local.properties"
chmod +x "$PROJECT_DIR/gradlew"

info "gradle 빌드 시작 (처음에는 gradle 배포판을 내려받아 몇 분 걸립니다)…"
cd "$PROJECT_DIR" || die "소스 폴더로 이동 실패"
export GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8"
./gradlew --no-daemon assembleDebug || die "빌드 실패. 위 gradle 오류 메시지를 확인하세요."

APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
[ -f "$APK_PATH" ] || die "APK 가 만들어지지 않았습니다: $APK_PATH"
ok "APK 생성: $APK_PATH"

# ---------------------------------------------------------------- 5) 복사
cp -f "$APK_PATH" "$SCRIPT_DIR/$APK_OUT_NAME"
ok "사본 저장: $SCRIPT_DIR/$APK_OUT_NAME"

copied=""
if grep -qi microsoft /proc/version 2>/dev/null; then
    for d in /mnt/c/Users/*/Downloads; do
        [ -d "$d" ] || continue
        if cp -f "$APK_PATH" "$d/$APK_OUT_NAME" 2>/dev/null; then
            copied="$d/$APK_OUT_NAME"
            ok "윈도우 다운로드 폴더로 복사: $copied"
            break
        fi
    done
    if [ -z "$copied" ]; then
        warn "윈도우 다운로드 폴더(/mnt/c/Users/사용자/Downloads)를 찾지 못했습니다."
        warn "APK 파일을 탐색기로 직접 복사하세요: \\\\wsl$\\$(hostname)\\$(printf '%s' "$SCRIPT_DIR" | sed 's|^/||; s|/|\\\\|g')\\$APK_OUT_NAME"
    fi
fi

cat <<EOM

────────────────────────────────────────────────────────────
빌드가 끝났습니다.

  APK 파일 : $APK_PATH
$( [ -n "$copied" ] && printf '  복사본   : %s\n' "$copied" )
────────────────────────────────────────────────────────────

갤럭시 S10 에 설치하는 방법 (둘 중 하나):
  1) APK 파일을 USB 케이블로 휴대폰에 옮긴 뒤, 휴대폰의 '내 파일'에서 눌러 설치
     (설정 > 애플리케이션 > 특수 접근 > 알 수 없는 앱 설치 허용 필요)
  2) WSL 에서 ADB 로 바로 설치 (휴대폰에서 USB 디버깅을 켠 경우)
     $SDK_ROOT/platform-tools/adb install -r "$APK_PATH"

참고:
  - 원본 FT8CN 과 패키지 이름이 달라(com.bg7yoz.ft8cn.kr) 나란히 설치됩니다.
  - 휴대폰 언어가 한국어이면 화면이 한국어로 나옵니다.
  - 간헐 CQ 설정은 [설정] 화면의 [간헐 CQ] / [1회 CQ 송신] 항목입니다.
EOM
