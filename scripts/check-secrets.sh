#!/usr/bin/env bash
# Fails if tracked (or staged, with --staged) files include secret files or
# credential-shaped content. Prints file names and rule names only, never the
# matched value. Dependency-free so it runs in CI and as a git hook.
#
#   scripts/check-secrets.sh            # every tracked file
#   scripts/check-secrets.sh --staged   # files staged for commit (pre-commit hook)
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

if [[ "${1:-}" == "--staged" ]]; then
  mapfile -t files < <(git diff --cached --name-only --diff-filter=ACMR)
else
  mapfile -t files < <(git ls-files)
fi

# Paths that must never be committed (real env files, keystores, service accounts).
forbidden_path='(^|/)(\.env(\.[^/]*)?|local\.properties|keystore\.properties|signing\.properties|google-services\.json|GoogleService-Info\.plist)$|\.(jks|keystore|p12|pfx|pem|p8)$|(service-account|firebase-adminsdk)[^/]*\.json$'
allowed_path='(^|/)\.env\.example$'

# name|extended regex. Placeholders such as USER:PASSWORD@HOST and the CI
# throwaway sahakar:sahakar@localhost credential are allowed below.
rules=(
  'private-key|-----BEGIN ([A-Z]+ )?PRIVATE KEY-----'
  'postgres-url-with-password|postgres(ql)?(\+psycopg)?://[^:/@[:space:]]+:[^@[:space:]]{6,}@'
  'google-api-key|AIza[0-9A-Za-z_-]{35}'
  'jwt|eyJhbGciOi[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.'
  'github-token|gh[pousr]_[A-Za-z0-9]{36}'
  'textbee-or-api-key-assignment|(TEXTBEE_API_KEY|API_KEY|SECRET|PASSWORD)[[:space:]]*[:=][[:space:]]*["'"'"']?[A-Za-z0-9_/+-]{20,}'
)
allowed_content='USER:PASSWORD@HOST|sahakar:sahakar@(localhost|127\.0\.0\.1)|<[a-z-]+>'

status=0
for file in "${files[@]}"; do
  [[ -f "$file" ]] || continue
  if [[ "$file" =~ $forbidden_path && ! "$file" =~ $allowed_path ]]; then
    echo "secret-check: forbidden file is tracked: $file"
    status=1
    continue
  fi
  # Skip binaries and lockfiles (integrity hashes look like keys).
  case "$file" in
    *.png|*.jpg|*.jpeg|*.webp|*.ico|*.jar|*package-lock.json) continue ;;
  esac
  for rule in "${rules[@]}"; do
    name="${rule%%|*}"
    pattern="${rule#*|}"
    if grep -E -n -- "$pattern" "$file" | grep -E -v -- "$allowed_content" >/dev/null; then
      lines=$(grep -E -n -- "$pattern" "$file" | grep -E -v -- "$allowed_content" | cut -d: -f1 | paste -sd, -)
      echo "secret-check: $name in $file (line $lines)"
      status=1
    fi
  done
done

if [[ $status -ne 0 ]]; then
  echo "secret-check: FAILED. Remove the secret, rotate it if it was ever pushed, and use environment variables."
else
  echo "secret-check: ok (${#files[@]} files)"
fi
exit $status
