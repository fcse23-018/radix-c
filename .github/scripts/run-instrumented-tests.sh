#!/usr/bin/env bash
# Runs the device-only suite on the CI emulator and publishes failures in a form that is
# readable without repository credentials (annotations and the public step summary).
set -uo pipefail

summary="${GITHUB_STEP_SUMMARY:-/dev/null}"
log=/tmp/connected-android-test.log
report_dir=app/build/outputs/androidTest-results

adb_state=$(timeout 20 adb devices 2>&1 | tr '\n' ' ' || true)
boot=$(timeout 20 adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)
abi=$(timeout 20 adb shell getprop ro.product.cpu.abi 2>/dev/null | tr -d '\r' || true)
echo "::notice title=instrumented-start::adb='${adb_state}' boot='${boot}' abi='${abi}'"

{
  echo "### Instrumented test diagnostics"
  echo
  echo "- boot_completed: \`${boot}\`"
  echo "- abi: \`${abi}\`"
  echo "- kvm: \`$(ls -l /dev/kvm 2>/dev/null || echo missing)\`"
  echo "- adb devices:"
  echo '```'
  echo "${adb_state}"
  echo '```'
} >>"$summary"

set +e
timeout 2400 bash ./gradlew connectedDebugAndroidTest --stacktrace 2>&1 | tee "$log"
status=${PIPESTATUS[0]}
set -e

xml_count=$(find "$report_dir" -name '*.xml' 2>/dev/null | wc -l | tr -d ' ')
echo "::notice title=instrumented-end::gradle_exit=${status} xml_files=${xml_count}"

python3 - "$report_dir" "$log" "$summary" <<'PY'
import glob
import os
import sys
import xml.etree.ElementTree as ET

report_dir, log_path, summary = sys.argv[1], sys.argv[2], sys.argv[3]
files = sorted(glob.glob(os.path.join(report_dir, "**", "*.xml"), recursive=True))
rows = []
failures = []
for path in files:
    try:
        root = ET.parse(path).getroot()
    except Exception as exc:  # noqa: BLE001 - report parsing problems, keep going
        rows.append(("parse-error", str(exc), False))
        continue
    for case in root.iter("testcase"):
        name = f"{case.get('classname', '?')}.{case.get('name', '?')}"
        problems = list(case.findall("failure")) + list(case.findall("error"))
        if problems:
            message = (problems[0].get("message") or problems[0].text or "failed").strip()
            message = " ".join(message.split())[:1200]
            failures.append((name, message))
            rows.append((name, message, False))
        else:
            rows.append((name, "passed", True))

with open(summary, "a", encoding="utf-8") as out:
    out.write("\n### Instrumented test results\n\n")
    if not rows:
        out.write("No test XML was produced; the emulator, install or build step failed before tests ran.\n")
    for name, detail, ok in rows:
        suffix = "" if ok else f": {detail}"
        out.write(f"- {'PASS' if ok else 'FAIL'} `{name}`{suffix}\n")

for name, message in failures:
    print(f"::error title={name}::{message}")
if not files:
    with open(log_path, encoding="utf-8", errors="replace") as handle:
        tail = handle.read().splitlines()[-30:]
    escaped = "%0A".join(line.replace("%", "%25").replace("\r", "") for line in tail)
    print(f"::error title=instrumented::No androidTest XML produced. Gradle tail: {escaped}")
PY

if [ "$status" -ne 0 ]; then
  echo "::group::Last 150 Gradle lines"
  timeout 20 tail -n 150 "$log" || true
  echo "::endgroup::"
  echo "::group::Recent logcat"
  timeout 30 adb logcat -d -t 400 2>/dev/null | tail -n 200 || true
  echo "::endgroup::"
fi

exit "$status"
