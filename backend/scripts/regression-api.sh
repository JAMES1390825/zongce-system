#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8082}"
INITIAL_PASSWORD="${INITIAL_PASSWORD:-123456}"

ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-${INITIAL_PASSWORD}}"

COUNSELOR_USERNAME="${COUNSELOR_USERNAME:-counselor001}"
COUNSELOR_PASSWORD="${COUNSELOR_PASSWORD:-${INITIAL_PASSWORD}}"

STUDENT_USERNAME="${STUDENT_USERNAME:-stu001}"
STUDENT_PASSWORD="${STUDENT_PASSWORD:-${INITIAL_PASSWORD}}"

STUDY_USERNAME="${STUDY_USERNAME:-study001}"
STUDY_PASSWORD="${STUDY_PASSWORD:-${INITIAL_PASSWORD}}"

TARGET_TERM="${TARGET_TERM:-2026-1}"
TARGET_CLASS="${TARGET_CLASS:-}"
NOTIF_STRESS_COUNT="${NOTIF_STRESS_COUNT:-105}"

PASS_COUNT=0
FAIL_COUNT=0
SKIP_COUNT=0

RUN_ID="$(date +%Y%m%d%H%M%S)"
TMP_DIR="$(mktemp -d)"

ADMIN_TOKEN=""
COUNSELOR_TOKEN=""
STUDENT_TOKEN=""
STUDY_TOKEN=""

STUDY_ACCOUNT_ID=""
STUDY_DISABLED=0

log_info() {
  echo "[INFO] $*"
}

log_pass() {
  echo "[PASS] $*"
  PASS_COUNT=$((PASS_COUNT + 1))
}

log_fail() {
  echo "[FAIL] $*"
  FAIL_COUNT=$((FAIL_COUNT + 1))
}

log_skip() {
  echo "[SKIP] $*"
  SKIP_COUNT=$((SKIP_COUNT + 1))
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 2
  fi
}

urlencode() {
  jq -rn --arg v "$1" '$v|@uri'
}

print_json() {
  local body="$1"
  jq . "$body" 2>/dev/null || cat "$body"
}

request_json() {
  local method="$1"
  local path="$2"
  local token="${3:-}"
  local data="${4:-}"

  local out="${TMP_DIR}/resp_${RANDOM}_$(date +%s%N).json"
  local -a cmd=(curl -sS -o "$out" -w "%{http_code}" -X "$method")

  if [[ -n "$token" ]]; then
    cmd+=(-H "Authorization: Bearer ${token}")
  fi
  if [[ -n "$data" ]]; then
    cmd+=(-H "Content-Type: application/json" -d "$data")
  fi

  local code
  code="$("${cmd[@]}" "${BASE_URL}${path}")"
  echo "${code}|${out}"
}

request_form_file() {
  local method="$1"
  local path="$2"
  local token="$3"
  local field_name="$4"
  local file_path="$5"

  local out="${TMP_DIR}/resp_${RANDOM}_$(date +%s%N).json"
  local code

  code="$(curl -sS -o "$out" -w "%{http_code}" -X "$method" \
    -H "Authorization: Bearer ${token}" \
    -F "${field_name}=@${file_path}" \
    "${BASE_URL}${path}")"

  echo "${code}|${out}"
}

split_resp() {
  local raw="$1"
  RESP_CODE="${raw%%|*}"
  RESP_BODY="${raw#*|}"
}

login_token() {
  local username="$1"
  local password="$2"
  local label="$3"

  local payload
  payload="$(jq -cn --arg u "$username" --arg p "$password" '{username:$u,password:$p}')"
  split_resp "$(request_json "POST" "/api/auth/login" "" "$payload")"

  if [[ "$RESP_CODE" != "200" ]]; then
    echo "Login failed for ${label} (HTTP ${RESP_CODE})" >&2
    print_json "$RESP_BODY" >&2
    exit 3
  fi

  local token
  token="$(jq -r '.accessToken // empty' "$RESP_BODY")"
  if [[ -z "$token" ]]; then
    echo "Login response missing accessToken for ${label}" >&2
    print_json "$RESP_BODY" >&2
    exit 3
  fi

  echo "$token"
}

ensure_study_reenabled() {
  if [[ "$STUDY_DISABLED" -eq 1 && -n "$STUDY_ACCOUNT_ID" && -n "$ADMIN_TOKEN" ]]; then
    local payload
    payload='{"enabled":true}'
    split_resp "$(request_json "PUT" "/api/admin/accounts/${STUDY_ACCOUNT_ID}/status" "$ADMIN_TOKEN" "$payload")"
    if [[ "$RESP_CODE" == "200" ]]; then
      echo "[CLEANUP] study account re-enabled"
      STUDY_DISABLED=0
    else
      echo "[CLEANUP] failed to re-enable study account (HTTP ${RESP_CODE})" >&2
      print_json "$RESP_BODY" >&2
    fi
  fi
}

cleanup() {
  ensure_study_reenabled
  rm -rf "$TMP_DIR"
}

run_tc01() {
  log_info "TC-01 counselor must have class binding"
  local username="auto_counselor_${RUN_ID}"
  local payload
  payload="$(jq -cn \
    --arg username "$username" \
    '{username:$username,name:"Auto Counselor",role:"COUNSELOR",department:"QA",className:""}')"

  split_resp "$(request_json "POST" "/api/admin/teachers" "$ADMIN_TOKEN" "$payload")"
  if [[ "$RESP_CODE" == "400" ]]; then
    log_pass "TC-01"
    return
  fi

  log_fail "TC-01 expected HTTP 400 but got ${RESP_CODE}"
  print_json "$RESP_BODY"
}

run_tc02() {
  log_info "TC-02 counselor review list should be class-scoped"
  split_resp "$(request_json "GET" "/api/counselor/reviews?status=PENDING" "$COUNSELOR_TOKEN")"
  if [[ "$RESP_CODE" != "200" ]]; then
    log_fail "TC-02 expected HTTP 200 but got ${RESP_CODE}"
    print_json "$RESP_BODY"
    return
  fi

  local scoped
  scoped="$(jq -r '.scopedClass // ""' "$RESP_BODY")"
  if [[ -z "$scoped" ]]; then
    log_fail "TC-02 scopedClass is empty"
    print_json "$RESP_BODY"
    return
  fi

  if [[ -z "$TARGET_CLASS" ]]; then
    TARGET_CLASS="$scoped"
  fi

  if jq -e --arg cls "$scoped" '(.data // []) | all(.className == $cls)' "$RESP_BODY" >/dev/null; then
    log_pass "TC-02"
    return
  fi

  log_fail "TC-02 found rows outside scoped class"
  print_json "$RESP_BODY"
}

run_tc03() {
  log_info "TC-03 all counselor accounts should have non-empty class"
  split_resp "$(request_json "GET" "/api/admin/teachers?role=COUNSELOR" "$ADMIN_TOKEN")"
  if [[ "$RESP_CODE" != "200" ]]; then
    log_fail "TC-03 expected HTTP 200 but got ${RESP_CODE}"
    print_json "$RESP_BODY"
    return
  fi

  if jq -e '(.data // []) | all(((.className // "") | gsub("^\\s+|\\s+$";"") | length) > 0)' "$RESP_BODY" >/dev/null; then
    log_pass "TC-03"
    return
  fi

  log_fail "TC-03 found counselor account with empty className"
  print_json "$RESP_BODY"
}

run_tc04() {
  log_info "TC-04 declaration should not allow repeated review transitions"
  local item_name="AUTO-DECL-${RUN_ID}"
  local term_q
  local item_q
  term_q="$(urlencode "$TARGET_TERM")"
  item_q="$(urlencode "$item_name")"

  split_resp "$(request_json "POST" "/api/student/declarations/moral?term=${term_q}&itemName=${item_q}&points=1" "$STUDENT_TOKEN")"
  if [[ "$RESP_CODE" != "200" ]]; then
    log_fail "TC-04 submit declaration failed (HTTP ${RESP_CODE})"
    print_json "$RESP_BODY"
    return
  fi

  split_resp "$(request_json "GET" "/api/counselor/reviews?status=PENDING&type=MORAL" "$COUNSELOR_TOKEN")"
  if [[ "$RESP_CODE" != "200" ]]; then
    log_fail "TC-04 list pending reviews failed (HTTP ${RESP_CODE})"
    print_json "$RESP_BODY"
    return
  fi

  local review_id
  review_id="$(jq -r --arg name "$item_name" '(.data // []) | map(select(.itemName == $name)) | last | .id // empty' "$RESP_BODY")"
  if [[ -z "$review_id" ]]; then
    log_fail "TC-04 cannot find newly submitted declaration in review list"
    print_json "$RESP_BODY"
    return
  fi

  split_resp "$(request_json "POST" "/api/counselor/reviews/${review_id}/approve" "$COUNSELOR_TOKEN")"
  if [[ "$RESP_CODE" != "200" ]]; then
    log_fail "TC-04 first approve failed (HTTP ${RESP_CODE})"
    print_json "$RESP_BODY"
    return
  fi

  split_resp "$(request_json "POST" "/api/counselor/reviews/${review_id}/approve" "$COUNSELOR_TOKEN")"
  if [[ "$RESP_CODE" != "400" ]]; then
    log_fail "TC-04 repeated approve should fail with 400, got ${RESP_CODE}"
    print_json "$RESP_BODY"
    return
  fi

  split_resp "$(request_json "POST" "/api/counselor/reviews/${review_id}/reject" "$COUNSELOR_TOKEN" '{"comment":"repeat-check"}')"
  if [[ "$RESP_CODE" != "400" ]]; then
    log_fail "TC-04 reject after approve should fail with 400, got ${RESP_CODE}"
    print_json "$RESP_BODY"
    return
  fi

  log_pass "TC-04"
}

run_tc05() {
  log_info "TC-05 disabled account token should be rejected immediately"
  local keyword_q
  keyword_q="$(urlencode "$STUDY_USERNAME")"
  split_resp "$(request_json "GET" "/api/admin/accounts?keyword=${keyword_q}" "$ADMIN_TOKEN")"
  if [[ "$RESP_CODE" != "200" ]]; then
    log_fail "TC-05 list accounts failed (HTTP ${RESP_CODE})"
    print_json "$RESP_BODY"
    return
  fi

  STUDY_ACCOUNT_ID="$(jq -r --arg u "$STUDY_USERNAME" '(.data // []) | map(select(.username == $u)) | first | .id // empty' "$RESP_BODY")"
  if [[ -z "$STUDY_ACCOUNT_ID" ]]; then
    log_fail "TC-05 cannot find study account id"
    print_json "$RESP_BODY"
    return
  fi

  split_resp "$(request_json "PUT" "/api/admin/accounts/${STUDY_ACCOUNT_ID}/status" "$ADMIN_TOKEN" '{"enabled":false}')"
  if [[ "$RESP_CODE" != "200" ]]; then
    log_fail "TC-05 disable study account failed (HTTP ${RESP_CODE})"
    print_json "$RESP_BODY"
    return
  fi
  STUDY_DISABLED=1

  split_resp "$(request_json "GET" "/api/auth/me" "$STUDY_TOKEN")"
  if [[ "$RESP_CODE" != "401" ]]; then
    log_fail "TC-05 disabled token expected 401, got ${RESP_CODE}"
    print_json "$RESP_BODY"
    return
  fi

  split_resp "$(request_json "PUT" "/api/admin/accounts/${STUDY_ACCOUNT_ID}/status" "$ADMIN_TOKEN" '{"enabled":true}')"
  if [[ "$RESP_CODE" != "200" ]]; then
    log_fail "TC-05 re-enable study account failed (HTTP ${RESP_CODE})"
    print_json "$RESP_BODY"
    return
  fi
  STUDY_DISABLED=0

  STUDY_TOKEN="$(login_token "$STUDY_USERNAME" "$STUDY_PASSWORD" "study")"
  log_pass "TC-05"
}

run_tc06() {
  log_info "TC-06 API precondition: invalid refresh token must be rejected"
  split_resp "$(request_json "POST" "/api/auth/refresh" "" '{"refreshToken":"invalid-token"}')"
  if [[ "$RESP_CODE" == "400" || "$RESP_CODE" == "401" ]]; then
    log_pass "TC-06"
    log_skip "TC-06 UI state cleanup still requires one manual browser check"
    return
  fi

  log_fail "TC-06 invalid refresh expected 400/401, got ${RESP_CODE}"
  print_json "$RESP_BODY"
}

run_tc07() {
  log_info "TC-07 mark-all-read should clear unread notifications even when count > 100"

  split_resp "$(request_json "POST" "/api/notifications/read-all" "$ADMIN_TOKEN")"
  if [[ "$RESP_CODE" != "200" ]]; then
    log_fail "TC-07 baseline read-all failed (HTTP ${RESP_CODE})"
    print_json "$RESP_BODY"
    return
  fi

  local notif_student="auto_notif_${RUN_ID}"
  local csv_file="${TMP_DIR}/tc07_students.csv"
  cat > "$csv_file" <<EOF
studentNo,studentName,className
${notif_student},Auto Loader,${TARGET_CLASS}
EOF

  local i
  for ((i = 1; i <= NOTIF_STRESS_COUNT; i++)); do
    split_resp "$(request_form_file "POST" "/api/admin/students/import" "$ADMIN_TOKEN" "file" "$csv_file")"
    if [[ "$RESP_CODE" != "200" ]]; then
      log_fail "TC-07 notification generation failed at iteration ${i} (HTTP ${RESP_CODE})"
      print_json "$RESP_BODY"
      return
    fi
    if (( i % 20 == 0 )); then
      log_info "TC-07 generated ${i}/${NOTIF_STRESS_COUNT} notifications"
    fi
  done

  split_resp "$(request_json "GET" "/api/notifications/unread-count" "$ADMIN_TOKEN")"
  if [[ "$RESP_CODE" != "200" ]]; then
    log_fail "TC-07 unread-count before read-all failed (HTTP ${RESP_CODE})"
    print_json "$RESP_BODY"
    return
  fi

  local before_count
  before_count="$(jq -r '.count // 0' "$RESP_BODY")"
  if [[ "$before_count" -lt 100 ]]; then
    log_fail "TC-07 expected unread count >= 100 before read-all, got ${before_count}"
    print_json "$RESP_BODY"
    return
  fi

  split_resp "$(request_json "POST" "/api/notifications/read-all" "$ADMIN_TOKEN")"
  if [[ "$RESP_CODE" != "200" ]]; then
    log_fail "TC-07 read-all failed (HTTP ${RESP_CODE})"
    print_json "$RESP_BODY"
    return
  fi

  split_resp "$(request_json "GET" "/api/notifications/unread-count" "$ADMIN_TOKEN")"
  if [[ "$RESP_CODE" != "200" ]]; then
    log_fail "TC-07 unread-count after read-all failed (HTTP ${RESP_CODE})"
    print_json "$RESP_BODY"
    return
  fi

  local after_count
  after_count="$(jq -r '.count // 0' "$RESP_BODY")"
  if [[ "$after_count" != "0" ]]; then
    log_fail "TC-07 expected unread count 0 after read-all, got ${after_count}"
    print_json "$RESP_BODY"
    return
  fi

  log_pass "TC-07"
}

run_tc08() {
  log_info "TC-08 student import should accept quoted comma values"

  local student_no="auto_csv_student_${RUN_ID}"
  local csv_file="${TMP_DIR}/tc08_students.csv"
  cat > "$csv_file" <<EOF
studentNo,studentName,className
${student_no},"Auto,Student",${TARGET_CLASS}
EOF

  split_resp "$(request_form_file "POST" "/api/admin/students/import" "$ADMIN_TOKEN" "file" "$csv_file")"
  if [[ "$RESP_CODE" != "200" ]]; then
    log_fail "TC-08 import failed (HTTP ${RESP_CODE})"
    print_json "$RESP_BODY"
    return
  fi

  local class_q
  class_q="$(urlencode "$TARGET_CLASS")"
  split_resp "$(request_json "GET" "/api/admin/students?className=${class_q}" "$ADMIN_TOKEN")"
  if [[ "$RESP_CODE" != "200" ]]; then
    log_fail "TC-08 verify students list failed (HTTP ${RESP_CODE})"
    print_json "$RESP_BODY"
    return
  fi

  if jq -e --arg u "$student_no" --arg n "Auto,Student" '(.data // []) | any(.username == $u and .name == $n)' "$RESP_BODY" >/dev/null; then
    log_pass "TC-08"
    return
  fi

  log_fail "TC-08 imported student with comma name not found"
  print_json "$RESP_BODY"
}

run_tc09() {
  log_info "TC-09 score import should accept quoted comma values"

  split_resp "$(request_json "GET" "/api/teacher/study/items" "$STUDY_TOKEN")"
  if [[ "$RESP_CODE" != "200" ]]; then
    log_fail "TC-09 get study items failed (HTTP ${RESP_CODE})"
    print_json "$RESP_BODY"
    return
  fi

  local item_code
  item_code="$(jq -r '.data[0].itemCode // empty' "$RESP_BODY")"
  if [[ -z "$item_code" ]]; then
    log_fail "TC-09 no study item available for import"
    print_json "$RESP_BODY"
    return
  fi

  local student_no="auto_csv_student_${RUN_ID}"
  local csv_file="${TMP_DIR}/tc09_scores.csv"
  cat > "$csv_file" <<EOF
studentNo,studentName,className,term,itemCode,score
${student_no},"Auto,Student",${TARGET_CLASS},${TARGET_TERM},${item_code},88.5
EOF

  split_resp "$(request_form_file "POST" "/api/teacher/study/scores/import" "$STUDY_TOKEN" "file" "$csv_file")"
  if [[ "$RESP_CODE" != "200" ]]; then
    log_fail "TC-09 score import failed (HTTP ${RESP_CODE})"
    print_json "$RESP_BODY"
    return
  fi

  local class_q
  local term_q
  class_q="$(urlencode "$TARGET_CLASS")"
  term_q="$(urlencode "$TARGET_TERM")"
  split_resp "$(request_json "GET" "/api/teacher/study/scores?className=${class_q}&term=${term_q}" "$STUDY_TOKEN")"
  if [[ "$RESP_CODE" != "200" ]]; then
    log_fail "TC-09 query imported scores failed (HTTP ${RESP_CODE})"
    print_json "$RESP_BODY"
    return
  fi

  if jq -e --arg sn "$student_no" --arg code "$item_code" --arg name "Auto,Student" \
    '(.data // []) | any(.studentNo == $sn and .itemCode == $code and .studentName == $name)' "$RESP_BODY" >/dev/null; then
    log_pass "TC-09"
    return
  fi

  log_fail "TC-09 imported score row with comma name not found"
  print_json "$RESP_BODY"
}

main() {
  require_cmd curl
  require_cmd jq

  trap cleanup EXIT

  log_info "BASE_URL=${BASE_URL}"
  log_info "TARGET_TERM=${TARGET_TERM}"
  log_info "NOTIF_STRESS_COUNT=${NOTIF_STRESS_COUNT}"

  ADMIN_TOKEN="$(login_token "$ADMIN_USERNAME" "$ADMIN_PASSWORD" "admin")"
  COUNSELOR_TOKEN="$(login_token "$COUNSELOR_USERNAME" "$COUNSELOR_PASSWORD" "counselor")"
  STUDENT_TOKEN="$(login_token "$STUDENT_USERNAME" "$STUDENT_PASSWORD" "student")"
  STUDY_TOKEN="$(login_token "$STUDY_USERNAME" "$STUDY_PASSWORD" "study")"

  if [[ -z "$TARGET_CLASS" ]]; then
    split_resp "$(request_json "GET" "/api/auth/me" "$COUNSELOR_TOKEN")"
    if [[ "$RESP_CODE" == "200" ]]; then
      TARGET_CLASS="$(jq -r '.user.className // ""' "$RESP_BODY")"
    fi
  fi

  if [[ -z "$TARGET_CLASS" ]]; then
    echo "Unable to resolve TARGET_CLASS. Set TARGET_CLASS explicitly and retry." >&2
    exit 4
  fi

  log_info "TARGET_CLASS=${TARGET_CLASS}"

  run_tc01
  run_tc02
  run_tc03
  run_tc04
  run_tc05
  run_tc06
  run_tc07
  run_tc08
  run_tc09

  echo
  echo "=== Regression Summary ==="
  echo "PASS: ${PASS_COUNT}"
  echo "FAIL: ${FAIL_COUNT}"
  echo "SKIP: ${SKIP_COUNT}"

  if [[ "$FAIL_COUNT" -gt 0 ]]; then
    exit 1
  fi
}

main "$@"
