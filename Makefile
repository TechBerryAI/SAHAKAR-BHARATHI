# Everyday commands for Sahakar Bharati.
# Run `make` or `make help` to list them.

.DEFAULT_GOAL := help

ifeq ($(OS),Windows_NT)
PYTHON := py -3.12
VENV_PY := backend/.venv/Scripts/python.exe
VENV_PIP := backend/.venv/Scripts/pip.exe
else
PYTHON := $(shell command -v python3.12 >/dev/null 2>&1 && echo python3.12 || echo python3)
VENV_PY := backend/.venv/bin/python
VENV_PIP := backend/.venv/bin/pip
endif

.PHONY: help start android android-prod android-logs \
	test test-backend test-web test-android test-scripts \
	lint lint-backend lint-web \
	install install-backend install-web migrate api web hooks secrets \
	load-test-api load-test load-test-register load-test-browse load-test-both load-test-cleanup

# Load-test knobs (override on the command line, e.g. USERS=10,20 DURATION=30)
USERS ?= 15,20,30
DURATION ?= 20
LOAD_TEST_PORT ?= 8000
LOAD_TEST_ARGS ?=

help: ## Show available commands
	@awk 'BEGIN {FS = ":.*##"; printf "\nSahakar Bharati\n\n"} /^[a-zA-Z0-9_-]+:.*##/ {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2} END {printf "\n"}' $(MAKEFILE_LIST)

start: ## Start the API (http://127.0.0.1:8000) and the web app (http://localhost:5174)
	node scripts/start.js

android: ## Build the Android app and open it on an emulator or USB phone
	node scripts/start-android.js $(ANDROID_ARGS)

android-prod: ## Open the Android app against the live API
	node scripts/start-android.js --prod $(ANDROID_ARGS)

android-logs: ## Open the Android app and stream its logs
	node scripts/start-android.js --logs $(ANDROID_ARGS)

test: ## Run backend, web, Android unit, and script tests
	node scripts/test-all.js

test-backend: ## Run backend pytest. Example: make test-backend ARGS="-k attendance"
	node scripts/test-all.js backend $(ARGS)

test-web: ## Run the web Vitest suite
	node scripts/test-all.js web $(ARGS)

test-android: ## Run Android JVM unit tests (no emulator)
	node scripts/test-all.js android $(ARGS)

test-scripts: ## Run the Node tests under scripts/
	node scripts/test-all.js scripts $(ARGS)

lint: lint-backend lint-web ## Run backend ruff, web ESLint, and the web typecheck

lint-backend: ## Run ruff on the backend
	$(VENV_PY) -m ruff check backend

lint-web: ## Run ESLint and the TypeScript check
	npm --prefix web run lint
	npm --prefix web run typecheck

install: install-backend install-web ## Create the backend virtualenv and install web dependencies

install-backend: ## Create backend/.venv and install the package with dev tools
	$(PYTHON) -m venv backend/.venv
	$(VENV_PIP) install -e './backend[dev]'

install-web: ## Install web dependencies from the lockfile
	npm ci --prefix web

migrate: ## Apply Alembic migrations to the database in backend/.env
	cd backend && ../$(VENV_PY) -m alembic upgrade head

api: ## Run only the API on http://127.0.0.1:8000
	cd backend && ../$(VENV_PY) -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

web: ## Run only the web app on http://localhost:5174
ifeq ($(OS),Windows_NT)
	cd web && set VITE_API_BASE_URL=http://127.0.0.1:8000&& npm run dev -- --port 5174 --strictPort
else
	cd web && VITE_API_BASE_URL=http://127.0.0.1:8000 npm run dev -- --port 5174 --strictPort
endif

load-test-api: ## Start API with mocked SMS for load tests (http://127.0.0.1:8000)
	cd backend && \
		DB_POOL_SIZE=$${DB_POOL_SIZE:-10} \
		DB_MAX_OVERFLOW=$${DB_MAX_OVERFLOW:-2} \
		DB_POOL_TIMEOUT_SECONDS=$${DB_POOL_TIMEOUT_SECONDS:-60} \
		REGISTER_RATE_LIMIT=$${REGISTER_RATE_LIMIT:-50000} \
		OTP_SEND_IP_RATE_LIMIT=$${OTP_SEND_IP_RATE_LIMIT:-50000} \
		OTP_SEND_RATE_LIMIT=$${OTP_SEND_RATE_LIMIT:-500} \
		OTP_CONFIRM_RATE_LIMIT=$${OTP_CONFIRM_RATE_LIMIT:-50000} \
		LOGIN_RATE_LIMIT=$${LOGIN_RATE_LIMIT:-500} \
		LOGIN_IP_RATE_LIMIT=$${LOGIN_IP_RATE_LIMIT:-50000} \
		OTP_RESEND_SECONDS=$${OTP_RESEND_SECONDS:-5} \
		../$(VENV_PY) scripts/load_test_server.py --host 0.0.0.0 --port $(LOAD_TEST_PORT)

load-test: ## Simultaneous registration load test (needs load-test-api). USERS=15,20,30
	cd backend && ../$(VENV_PY) scripts/load_test.py \
		--scenario register \
		--base-url http://127.0.0.1:$(LOAD_TEST_PORT) \
		--users $(USERS) \
		--cleanup-after \
		$(LOAD_TEST_ARGS)

load-test-register: load-test ## Alias for make load-test

load-test-browse: ## Concurrent browse load test (needs load-test-api). USERS=15,20,30 DURATION=20
	cd backend && ../$(VENV_PY) scripts/load_test.py \
		--scenario browse \
		--base-url http://127.0.0.1:$(LOAD_TEST_PORT) \
		--users $(USERS) \
		--duration $(DURATION) \
		--cleanup-after \
		$(LOAD_TEST_ARGS)

load-test-both: ## Register burst then browse (needs load-test-api). USERS=15,20,30
	cd backend && ../$(VENV_PY) scripts/load_test.py \
		--scenario both \
		--base-url http://127.0.0.1:$(LOAD_TEST_PORT) \
		--users $(USERS) \
		--duration $(DURATION) \
		--cleanup-after \
		$(LOAD_TEST_ARGS)

load-test-cleanup: ## Delete disposable load-test users (70000000xx)
	cd backend && ../$(VENV_PY) scripts/load_test.py --cleanup

hooks: ## Use the repository git hooks
	git config core.hooksPath .githooks

secrets: ## Check tracked files for secrets
	scripts/check-secrets.sh
