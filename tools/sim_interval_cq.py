#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""FT8TransmitSignal.intervalCQAllowTransmit() 의 동작을 그대로 옮긴 시뮬레이터.

15초 슬롯을 진행시키며, 어느 슬롯에 실제로 송신하는지 출력한다.
(자바 코드를 컴파일할 수 없는 환경에서 주기·횟수·전환 동작을 확인하기 위한 것)
"""

MIN = 60 * 1000
SLOT = 15 * 1000


class Tx:
    def __init__(self, interval_minutes, count):
        self.interval_minutes = interval_minutes
        self.count = count
        self.burst_left = 0
        self.next_burst = 0
        self.function_order = 6
        self.to_callsign = "CQ"
        self.activated = True

    # 자바 isInCQState()
    def is_in_cq(self):
        return self.function_order == 6 and self.to_callsign == "CQ"

    # 자바 intervalCQAllowTransmit()
    def allow(self, now):
        if self.interval_minutes <= 0:
            self.burst_left = 0
            self.next_burst = 0
            return True
        if not self.is_in_cq():
            self.burst_left = 0
            self.next_burst = 0
            return True
        if self.burst_left > 0:
            self.burst_left -= 1
            return True
        if self.next_burst == 0 or now >= self.next_burst:
            self.burst_left = max(1, self.count) - 1
            self.next_burst = now + max(1, self.interval_minutes) * MIN
            return True
        return False

    # 자바 resetIntervalCQSchedule()
    def reset(self):
        self.burst_left = 0
        self.next_burst = 0


def run(interval_minutes, count, minutes=35, my_parity=1, qso=None):
    """my_parity 슬롯(15초 단위 짝/홀)에만 내 차례가 온다고 본다."""
    tx = Tx(interval_minutes, count)
    txs = []
    for slot in range(0, minutes * MIN // SLOT):
        now = slot * SLOT
        if qso:
            start, end = qso
            if start <= slot < end:       # 상대와 교신 중 (functionOrder 1~5)
                tx.function_order = 1
                tx.to_callsign = "JA1XXX"
            elif slot == end:             # 교신 종료 -> resetToCQ()
                tx.reset()
                tx.function_order = 6
                tx.to_callsign = "CQ"
        if slot % 2 != my_parity % 2:
            continue                      # 내 송신 차례가 아님
        if tx.allow(now):
            txs.append(now / MIN)
    return txs


def fmt(t):
    return "%02d:%04.1f" % (int(t), (t - int(t)) * 60)


def show(title, txs):
    print(title)
    print("   송신 %d회 : %s" % (len(txs), ", ".join(fmt(t) for t in txs)))
    gaps = [round(txs[i + 1] - txs[i], 2) for i in range(len(txs) - 1)]
    print("   간격(분): %s" % gaps)


show("[1] 간헐 CQ 10분마다 · 3회 (35분 운용)", run(10, 3, 35))
show("[2] 간헐 CQ 30분마다 · 2회 (95분 운용)", run(30, 2, 95))
show("[3] 간헐 CQ 5분마다 · 5회 (20분 운용)", run(5, 5, 20))
show("[4] 꺼짐(연속) · 10분 운용", run(0, 3, 10))
show("[5] 10분·3회 + 12분쯤 JA1XXX 교신(8슬롯) 후 복귀",
     run(10, 3, 30, qso=(52, 60)))
