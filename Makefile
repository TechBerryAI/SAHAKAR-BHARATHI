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
	install install-backend install-web migrate api web hooks secrets

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
	cd backend && $(VENV_PY) -m ruff check

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
	cd backend && $(VENV_PY) -m alembic upgrade head

api: ## Run only the API on http://127.0.0.1:8000
	cd backend && $(VENV_PY) -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

web: ## Run only the web app on http://localhost:5174
ifeq ($(OS),Windows_NT)
	cd web && set VITE_API_BASE_URL=http://127.0.0.1:8000&& npm run dev -- --port 5174 --strictPort
else
	cd web && VITE_API_BASE_URL=http://127.0.0.1:8000 npm run dev -- --port 5174 --strictPort
endif

hooks: ## Use the repository git hooks
	git config core.hooksPath .githooks

secrets: ## Check tracked files for secrets
	scripts/check-secrets.sh
