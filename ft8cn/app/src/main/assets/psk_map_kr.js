/* 한국어판(0.93-kr10): PSK Reporter 지도 화면을 '지도만 보이게' 접고 영어 라벨을 한국어로 바꾼다.
 *
 * GridPskFragment 가 pskreporter.info/pskmap.html 을 다 읽은 뒤 이 파일을 그대로 넣어 준다.
 * 하는 일
 *   1) 사이트 위쪽 머리글(OVERVIEW·MAP·…), 검색 폼, 통계 줄, 아래쪽 안내를 숨겨
 *      지도가 WebView 세로를 거의 다 채우게 한다(사이트 스스로 크기를 다시 계산하게 한다).
 *   2) 지도 위 오른쪽에 작은 [설정] 단추를 띄우고, 누르면 검색 폼을 지도 위에 겹쳐서 연다.
 *   3) 폼·표시 옵션·지도 표시 정보의 영어 라벨을 한국어로 바꾼다
 *      (사이트가 나중에 다시 그리는 부분도 따라간다).
 *   4) 'Website problem? (0 null)' 같은 수신 실패 문구와 감시 상태를 한국어로 바꿔 지도 아래에 보여 준다.
 * 여러 번 넣어도 안전하도록(다시 읽힐 때) 스스로 한 번만 준비한다. 화면에 다시 적용만 하려면
 * window.__pskKrApply() 를 부르면 된다.
 */
(function () {
    'use strict';

    var doc = document;
    var STYLE_ID = 'psk-kr-style';
    var REAPPLY_MS = [0, 250, 800, 1600, 3000, 6000];
    var POPUP_ROOTS = ['#popup', '#myInfoWindow', '#logbookdiv', '#countrydiv'];

    //------------------------------------------------------------------ 스타일
    var CSS = [
        'html,body{margin:0!important;padding:0!important;overflow:hidden!important;',
        'background:#000!important;color:#111!important}',
        /* 사이트 위쪽 머리글(로고·OVERVIEW·MAP·DEVELOPER·STATS·FAQ·ABOUT US·CONTACT) */
        'header.site-header,.site-header.map-header{display:none!important}',
        /* 휴대전화용 접이식 패널과 아래쪽 안내 */
        '#mobilecontainer,#mobilecontainerinner,#bottom,#container2{display:none!important}',
        /* 지도 위 상표 링크와 통계 줄(영어 문장이 여러 줄을 차지한다) */
        '#mapbrand{display:none!important}',
        '#stats1{display:none!important}',
        /* 검색 폼은 기본으로 접어 두고, [설정]을 눌렀을 때만 지도 위에 겹쳐서 연다 */
        '#container1{display:none!important}',
        'body.psk-kr-open #container1{display:block!important;position:fixed!important;',
        'left:4px!important;right:4px!important;top:4px!important;max-height:80%!important;',
        'overflow:auto!important;background:#fff!important;border-radius:10px!important;',
        'padding:6px 8px!important;box-sizing:border-box!important;',
        'box-shadow:0 10px 28px rgba(0,0,0,.65)!important;z-index:2147483600!important}',
        'body.psk-kr-open #container1 *{color:#111!important}',
        'body.psk-kr-open #container1 select,body.psk-kr-open #container1 input{',
        'color:#111!important;background:#fff!important;border:1px solid #999!important;',
        'border-radius:4px!important;padding:3px 4px!important;margin:2px 0!important;',
        'font-size:13px!important;max-width:100%!important}',
        'body.psk-kr-open #container1 a{color:#0a58ca!important;text-decoration:none!important;',
        'margin-right:10px!important;font-size:13px!important}',
        'body.psk-kr-open #container1 select{max-width:46vw!important}',
        /* 지도 표시 옵션 창: 설명 칸이 줄바꿈으로 길어지지 않게 하고, 작은 화면에서는 창 안에서 넘긴다 */
        '#optiontable td{white-space:nowrap!important}',
        '#optiontable{font-size:13px!important}',
        '.ui-dialog{max-height:88vh!important;max-width:96vw!important;',
        'display:flex!important;flex-direction:column!important}',
        '.ui-dialog .ui-dialog-content{overflow:auto!important}',
        '#mapconreportmap{width:100%!important;margin:0!important;background:#000!important;',
        'border:0!important}',
        /* 지도 위 [설정] 단추 */
        '#psk-kr-btn{position:fixed!important;top:6px!important;right:6px!important;',
        'z-index:2147483550!important;background:rgba(0,0,0,.55)!important;color:#fff!important;',
        'border:1px solid rgba(255,255,255,.45)!important;border-radius:14px!important;',
        'font:12px/1.1 sans-serif!important;padding:6px 10px!important;cursor:pointer!important}',
        'body.psk-kr-open #psk-kr-btn{display:none!important}',
        /* 폼 안 [닫기] 줄 */
        '#psk-kr-close{float:right!important;background:#eee!important;color:#111!important;',
        'border:1px solid #999!important;border-radius:6px!important;font:12px/1.1 sans-serif!important;',
        'padding:4px 10px!important;cursor:pointer!important;margin:0 0 4px 6px!important}',
        /* 감시 상태·수신 실패 안내(지도 아래 한 줄) */
        '#psk-kr-status{position:fixed!important;left:0!important;right:0!important;bottom:0!important;',
        'z-index:2147483580!important;background:rgba(0,0,0,.7)!important;color:#fff!important;',
        'font:11px/1.35 sans-serif!important;padding:4px 8px!important;display:none!important}',
        'body.psk-kr-status #psk-kr-status{display:block!important}'
    ].join('');

    //------------------------------------------------------------------ 낱말 사전
    // 화면에 그대로 나오는 영어(공백은 한 칸으로 정리해서 비교한다)
    var TEXT_KO = {
        // 검색 폼의 연결 낱말
        'On': '밴드',
        ', show': ', 표시',
        'show': '표시',
        'using': '모드',
        'over the last': '최근',
        // 폼 안 드롭다운
        'all bands': '전체 밴드',
        'signals': '신호',
        'countries': '국가',
        'sent/rcvd by': '송·수신',
        'sent by': '송신(내가 보냄)',
        'rcvd by': '수신(내가 받음)',
        'the callsign': '콜사인',
        'country of callsign': '콜사인의 국가',
        'grid square': '그리드',
        'anyone': '모두',
        'all modes': '모든 모드',
        '24 hours': '24시간',
        '12 hours': '12시간',
        '6 hours': '6시간',
        '3 hours': '3시간',
        '2 hours': '2시간',
        '1 hour': '1시간',
        '30 minutes': '30분',
        '15 minutes': '15분',
        '2 months': '2개월',
        '1 month': '1개월',
        '2 weeks': '2주',
        '1 week': '1주',
        '3 months': '3개월',
        '6 months': '6개월',
        '1 year': '1년',
        'Go!': '조회',
        'Display options': '표시 옵션',
        'Permalink': '고정 링크',
        'Legend': '범례',
        // 통계 줄
        'Large markers are monitors.': '큰 표시는 수신 감시국입니다.',
        'Reception reports shown as times.': '수신 기록은 시각으로 표시됩니다.',
        'Reception records:': '수신 기록:',
        'Hide statistics': '통계 숨기기',
        'Show unseen rx': '안 보인 수신 표시',
        'Show unseen tx': '안 보인 송신 표시',
        'Show grid': '그리드 표시',
        'Show snr': 'SNR 표시',
        'Hide menu bar': '메뉴 막대 숨기기',
        'Hide connecting lines': '연결선 숨기기',
        'Show connecting lines always': '연결선 항상 표시',
        'No auto pan/zoom': '자동 이동·확대 끄기',
        'Map type': '지도 종류',
        'Hide everything but the map': '지도만 남기기',
        // 표시 옵션 창
        'Display Options': '표시 옵션',
        'Save and Close': '저장하고 닫기',
        'Minutes for Sparkly markers': '반짝 표시 유지(분)',
        'Darkness for night shadow (0-1)': '야간 음영 농도(0~1)',
        'Show all': '전체 표시',
        'Show LoTW/eQSL': 'LoTW/eQSL 표시',
        'Show LoTW': 'LoTW 표시',
        'Show eSQL': 'eQSL 표시',
        'Show eQSL': 'eQSL 표시',
        'transmitters': '송신국',
        'timeout for worked markers': '교신 완료 표시 유지',
        'as distance units': '거리 단위',
        'Automatic': '자동',
        'miles': '마일',
        'km': 'km',
        'no': '없음',
        'Mercator': '메르카토르',
        'Azimuthal EA': '방위 등거리(EA)',
        'Azimuthal ED': '방위 정거리(ED)',
        // 지도 표시를 눌렀을 때
        'Show Logbook': '로그북 보기',
        'Show country': '국가 정보 보기',
        'Show statistics': '통계 보기',
        'Show logbook': '로그북 보기',
        'Show time': '시각 보기'
    };

    // 표시 옵션 표의 줄은 영어 글자가 아니라 입력 이름(input name)으로 찾아 바꾼다.
    var OPTION_KO = {
        hideunrec: '약한 감시국 숨기기',
        blankifnone: '보고 없으면 감시국 숨기기',
        hidepink: '분홍 얼룩 숨기기',
        hidenight: '야간 음영 숨기기',
        nightforspot: '음영 기준을 관측 시각으로',
        hidelight: '도시 불빛 숨기기',
        showtx: '안 보인 송신 표시',
        showrx: '안 보인 수신 표시',
        showgrid: '그리드 표시',
        showsnr: 'SNR 표시',
        monitorfreq: '감시국을 주파수 순으로',
        suppressbadqrg: '잘못된 주파수 감추기',
        hidestats: '통계 숨기기',
        hidemax: '지도만 남기기',
        hidemenubar: '메뉴 막대 숨기기',
        hidelines: '연결선 숨기기',
        showlines: '연결선 항상 표시',
        hidetime: '관측 시각 숨기기',
        noautozoom: '자동 이동·확대 끄기',
        showsnrinlogbook: '로그북에 SNR',
        textalwaysblack: '시각 글자를 항상 검게',
        sortmodes: '모드를 알파벳순으로',
        fullscreen: '지도 전체 화면',
        maptype: '지도 종류',
        azcenter: '방위 지도 중심 그리드',
        center: '지도 중심'
    };

    // 지도 표시(수신국 정보) 팝업 안의 영어 낱말
    var POPUP_KO = [
        [/^Rx at /, '수신 '],
        [/^Tx at /, '송신 '],
        [/^By /, '보낸 곳: '],
        [/^From /, '보낸 곳: '],
        [/^ by /, ' ← '],
        [/^ in /, '위치: '],
        [/^Loc /, '그리드 '],
        [/ Loc /g, ' · 그리드 '],
        [/Frequency: /g, '주파수: '],
        [/Distance: /g, '거리: '],
        [/ bearing /g, ' 방위 '],
        [/Using: /g, '장비: '],
        [/Antenna: /g, '안테나: '],
        [/Rig: /g, '무전기: '],
        [/Last LoTW upload: /g, 'LoTW 마지막 업로드: '],
        [/Last Tx at /g, '마지막 송신 '],
        [/^Last Tx /, '마지막 송신 '],
        [/^Tx /, '송신 ']
    ];

    // 지도 팝업 위치 줄에 나오는 나라 이름(자주 보는 곳만)
    var POPUP_COUNTRY = [
        [/Republic of Korea/g, '대한민국'],
        [/South Korea/g, '대한민국'],
        [/Japan/g, '일본'],
        [/China/g, '중국'],
        [/Taiwan/g, '대만'],
        [/Hong Kong/g, '홍콩'],
        [/Mongolia/g, '몽골'],
        [/Russia/g, '러시아'],
        [/Kazakhstan/g, '카자흐스탄'],
        [/United States/g, '미국'],
        [/Canada/g, '캐나다'],
        [/Australia/g, '호주'],
        [/New Zealand/g, '뉴질랜드'],
        [/Indonesia/g, '인도네시아'],
        [/Thailand/g, '태국'],
        [/Philippines/g, '필리핀'],
        [/Vietnam/g, '베트남'],
        [/Malaysia/g, '말레이시아'],
        [/Singapore/g, '싱가포르'],
        [/India/g, '인도'],
        [/Germany/g, '독일'],
        [/France/g, '프랑스'],
        [/United Kingdom/g, '영국'],
        [/Italy/g, '이탈리아'],
        [/Spain/g, '스페인'],
        [/Brazil/g, '브라질'],
        [/Argentina/g, '아르헨티나']
    ];

    var MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
        'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

    // 'Fri, 27 Nov 2026 06:34:00 GMT' → '2026-11-27 06:34 UTC'
    function formatGmtDate(text) {
        return text.replace(/\w{3},\s*(\d{1,2})\s*(\w{3})\s*(\d{4})\s*(\d\d:\d\d):\d\d\s*GMT/g
            , function (all, day, mon, year, hhmm) {
                var m = MONTHS.indexOf(mon);
                if (m < 0) {
                    return all;
                }
                var d = parseInt(day, 10);
                return year + '-' + (m < 9 ? '0' : '') + (m + 1) + '-'
                    + (d < 10 ? '0' : '') + d + ' ' + hhmm + ' UTC';
            });
    }

    // '9 mins ago' → '9분 전'
    function formatAgo(text) {
        return text.replace(/(\d+)\s*(secs?|seconds?)\s*ago/gi, '$1초 전')
            .replace(/(\d+)\s*(mins?|minutes?)\s*ago/gi, '$1분 전')
            .replace(/(\d+)\s*(hrs?|hours?)\s*ago/gi, '$1시간 전')
            .replace(/(\d+)\s*days?\s*ago/gi, '$1일 전');
    }

    //------------------------------------------------------------------ 도구
    function norm(s) {
        return (s == null ? '' : String(s)).replace(/\u00a0/g, ' ').replace(/\s+/g, ' ').trim();
    }

    function ensureStyle() {
        var el = doc.getElementById(STYLE_ID);
        if (!el) {
            el = doc.createElement('style');
            el.id = STYLE_ID;
            (doc.head || doc.documentElement).appendChild(el);
        }
        if (el.textContent !== CSS) {
            el.textContent = CSS;
        }
    }

    // 사이트가 휴대전화로 알아보고 검색 폼을 접이식 패널 안으로 옮겨 놓았으면 되돌린다.
    function moveFormBack() {
        var c1 = doc.getElementById('container1');
        if (!c1) {
            return;
        }
        var host = doc.querySelector('.left-col') || doc.querySelector('.qcontainer');
        if (host && c1.parentNode !== host && c1.parentNode && c1.parentNode.id
            && c1.parentNode.id.indexOf('mobilecontainer') >= 0) {
            host.appendChild(c1);
        }
    }

    // 지도 크기를 다시 계산한다(사이트 함수는 window.onresize 에 붙어 있다)
    function resizeMap() {
        try {
            if (typeof window.setMapSize === 'function') {
                window.setMapSize();
            } else if (typeof window.onresize === 'function') {
                window.onresize();
            }
        } catch (e) { /* 사이트가 아직 준비되지 않았으면 다음 차례에 다시 한다 */ }
        try {
            if (window.reportMap && typeof window.reportMap.updateSize === 'function') {
                window.reportMap.updateSize();
            }
        } catch (e2) { /* 무시 */ }
    }

    //------------------------------------------------------------------ 한국어로 바꾸기
    function translatePopupNode(node) {
        var raw = node.nodeValue;
        if (!raw) {
            return;
        }
        var out = raw;
        for (var i = 0; i < POPUP_KO.length; i++) {
            out = out.replace(POPUP_KO[i][0], POPUP_KO[i][1]);
        }
        for (var c = 0; c < POPUP_COUNTRY.length; c++) {
            out = out.replace(POPUP_COUNTRY[c][0], POPUP_COUNTRY[c][1]);
        }
        out = formatAgo(formatGmtDate(out));
        if (out !== raw) {
            node.nodeValue = out;
        }
    }

    function translateTextNode(node) {
        var raw = node.nodeValue;
        if (!raw) {
            return;
        }
        var n = norm(raw);
        if (!n) {
            return;
        }
        var ko = TEXT_KO[n];
        if (ko == null) {
            return;
        }
        var lead = raw.match(/^\s*/)[0];
        var tail = raw.match(/\s*$/)[0];
        node.nodeValue = lead + ko + tail;
    }

    function walk(root, forPopup) {
        if (!root || !root.childNodes) {
            return;
        }
        var stack = [root];
        while (stack.length) {
            var el = stack.pop();
            if (el.nodeType === 3) {
                if (forPopup) {
                    translatePopupNode(el);
                }
                translateTextNode(el);
                continue;
            }
            if (el.nodeType !== 1) {
                continue;
            }
            var tag = el.tagName;
            if (tag === 'SCRIPT' || tag === 'STYLE' || tag === 'CANVAS') {
                continue;
            }
            if (tag === 'OPTION') {
                var ok = TEXT_KO[norm(el.textContent)];
                if (ok != null) {
                    el.textContent = ok;
                }
            }
            var kids = el.childNodes;
            for (var i = kids.length - 1; i >= 0; i--) {
                stack.push(kids[i]);
            }
        }
    }

    function translateOptions() {
        // 폼 안 드롭다운의 값 목록(사이트가 다시 만들 때마다 따라간다)
        var selects = doc.querySelectorAll('#topform select, #options select');
        for (var i = 0; i < selects.length; i++) {
            var opts = selects[i].options || [];
            for (var j = 0; j < opts.length; j++) {
                var ko = TEXT_KO[norm(opts[j].textContent)];
                if (ko != null) {
                    opts[j].textContent = ko;
                }
            }
        }
        // 표시 옵션 표의 오른쪽 설명 칸(입력 이름으로 찾는다)
        var rows = doc.querySelectorAll('#optiontable tr');
        for (var r = 0; r < rows.length; r++) {
            var field = rows[r].querySelector('input[name],select[name]');
            if (!field) {
                continue;
            }
            var label = OPTION_KO[field.name];
            if (!label) {
                continue;
            }
            var cells = rows[r].querySelectorAll('td');
            var cell = cells.length ? cells[cells.length - 1] : null;
            if (cell && norm(cell.textContent) !== label) {
                cell.textContent = label;
            }
        }
        // 조회 단추(글자는 value 속성에 들어 있다)
        var go = doc.querySelector('#topform input[type=submit], #topform #gobutton');
        if (go && go.value !== '조회') {
            go.value = '조회';
        }
        // 표시 옵션 창의 제목과 닫기 단추(jQuery UI 가 그린 틀)
        var titles = doc.querySelectorAll('.ui-dialog-title');
        for (var t = 0; t < titles.length; t++) {
            if (norm(titles[t].textContent) === 'Display Options') {
                titles[t].textContent = '표시 옵션';
            }
        }
        var closes = doc.querySelectorAll('.ui-dialog-titlebar-close');
        for (var c = 0; c < closes.length; c++) {
            if (closes[c].getAttribute('title') !== '닫기') {
                closes[c].setAttribute('title', '닫기');
            }
        }
    }

    function translate() {
        var roots = ['#topform', '#options', '#stats1', '#stats2'];
        for (var i = 0; i < roots.length; i++) {
            walk(doc.querySelector(roots[i]), false);
        }
        for (var p = 0; p < POPUP_ROOTS.length; p++) {
            walk(doc.querySelector(POPUP_ROOTS[p]), true);
        }
        translateOptions();
    }

    //------------------------------------------------------------------ 감시 상태·수신 실패 안내
    function statusKorean(text) {
        if (!text) {
            return '';
        }
        var t = text;
        if (/Website problem|Unable to fetch/i.test(t)) {
            return '지도 자료를 받지 못했습니다. 잠시 뒤 [조회]를 다시 눌러 주세요.' + (t.indexOf('(') >= 0 ? ' ' + t.substring(t.indexOf('(')) : '');
        }
        if (/No reception reports|no reports/i.test(t)) {
            return '이 조건에서는 아직 수신 보고가 없습니다.';
        }
        var m = t.match(/^Monitoring\s+(\S+)\s*\(last heard\s+(.*?)\)\s*\.?\s*(.*)$/i);
        if (m) {
            var ago = formatAgo(m[2]).replace(/\s*전$/, '');
            t = m[1] + ' 감시 중 · 마지막 수신 ' + ago + ' 전';
            if (m[3]) {
                t += ' · ' + m[3];
            }
        }
        t = t.replace(/Live updates\.?/gi, '실시간 갱신 중')
            .replace(/Automatic refresh in progress\.?/gi, '자동 새로고침 중')
            .replace(/Automatic refresh in (\d+) minutes?\.?/gi, '$1분 뒤 자동 새로고침')
            .replace(/Reception records:\s*([\d,]+)/gi, '수신 기록 $1')
            .replace(/reception reports for/gi, '수신 보고:')
            .replace(/are shown as times/gi, '')
            .replace(/\s+/g, ' ')
            .trim();
        return t;
    }

    function updateStatus() {
        var warn = doc.getElementById('warning');
        var text = warn ? norm(warn.textContent) : '';
        var box = doc.getElementById('psk-kr-status');
        if (!box) {
            return;
        }
        if (!text) {
            if (box.textContent) {
                box.textContent = '';
            }
            doc.body.classList.remove('psk-kr-status');
            return;
        }
        var ko = statusKorean(text);
        if (box.textContent !== ko) {
            box.textContent = ko;
        }
        doc.body.classList.add('psk-kr-status');
    }

    //------------------------------------------------------------------ 지도 위 단추
    function buildUi() {
        if (!doc.body) {
            return;
        }
        if (!doc.getElementById('psk-kr-btn')) {
            var btn = doc.createElement('div');
            btn.id = 'psk-kr-btn';
            btn.setAttribute('role', 'button');
            btn.textContent = '설정';
            btn.addEventListener('click', function () {
                doc.body.classList.add('psk-kr-open');
                moveFormBack();
                translate();
            }, false);
            doc.body.appendChild(btn);
        }
        var form = doc.getElementById('topform');
        if (form && !doc.getElementById('psk-kr-close')) {
            var close = doc.createElement('div');
            close.id = 'psk-kr-close';
            close.setAttribute('role', 'button');
            close.textContent = '닫기';
            close.addEventListener('click', function () {
                doc.body.classList.remove('psk-kr-open');
            }, false);
            form.parentNode.insertBefore(close, form);
            // 조회를 누르면 폼을 접어 지도를 다시 크게 본다
            form.addEventListener('submit', function () {
                window.setTimeout(function () {
                    doc.body.classList.remove('psk-kr-open');
                }, 400);
            }, false);
        }
        if (!doc.getElementById('psk-kr-status')) {
            var status = doc.createElement('div');
            status.id = 'psk-kr-status';
            doc.body.appendChild(status);
        }
    }

    //------------------------------------------------------------------ 바뀐 화면 따라가기
    function watch() {
        if (window.__pskKrObserver || typeof MutationObserver !== 'function') {
            return;
        }
        var timer = 0;
        var observer = new MutationObserver(function () {
            if (timer) {
                return;
            }
            timer = window.setTimeout(function () {
                timer = 0;
                translate();
                updateStatus();
                resizeMap();
            }, 250);
        });
        var targets = ['#contents1', '#topform', '#options', '#stats1', '#warning', '#popup'];
        for (var i = 0; i < targets.length; i++) {
            var el = doc.querySelector(targets[i]);
            if (el) {
                observer.observe(el, { childList: true, subtree: true, characterData: true });
            }
        }
        window.__pskKrObserver = observer;
    }

    function apply() {
        try {
            ensureStyle();
            moveFormBack();
            buildUi();
            translate();
            updateStatus();
            watch();
            resizeMap();
        } catch (e) { /* 한 번 실패해도 다음 차례에 다시 시도한다 */ }
    }

    window.__pskKrApply = apply;

    // 문서가 다 만들어졌는지에 따라 바로 또는 기다렸다가 적용하고,
    // 사이트가 스스로 크기를 다시 잡는 때가 있어 몇 번 더 적용한다.
    function boot() {
        apply();
        for (var i = 0; i < REAPPLY_MS.length; i++) {
            window.setTimeout(apply, REAPPLY_MS[i]);
        }
        window.addEventListener('load', apply, false);
        window.addEventListener('resize', function () {
            window.setTimeout(apply, 150);
        }, false);
    }

    if (doc.readyState === 'loading') {
        doc.addEventListener('DOMContentLoaded', boot, false);
    } else {
        boot();
    }
})();
