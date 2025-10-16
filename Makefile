.PHONY: help start stop test verify build build-image clean logs ps check-java

# Set TMPDIR for LocalStack (macOS workaround)
export TMPDIR := $(HOME)/.tmp

help: ## Show available commands
	@echo "Available commands:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36mmake %-20s\033[0m %s\n", $$1, $$2}'

check-java:
	@if [ -z "$$JAVA_HOME" ]; then \
		echo "Error: JAVA_HOME is not set"; \
		exit 1; \
	fi

start: ## Run backend with MongoDB and LocalStack (default)
	@mkdir -p "$(TMPDIR)/localstack"
	docker compose up --build

stop: ## Stop all services and remove volumes
	@echo "Stopping Docker services..."
	@docker compose down -v
	@echo "Killing any remaining Java processes on port 8085..."
	@lsof -ti :8085 | xargs kill -9 2>/dev/null || true
	@pkill -f "mvn.*spring-boot:run" 2>/dev/null || true
	@echo "All services stopped."

test: check-java ## Run unit tests (no Docker required)
	mvn test

verify: check-java ## Run all tests including integration tests (requires Docker)
	mvn clean verify

build: check-java ## Build the project
	mvn clean install

build-image: ## Build Docker image (for use with frontend)
	docker compose build trade-demo-backend

clean: check-java ## Clean Maven build and remove test containers
	@echo "Cleaning Maven build artifacts..."
	@mvn clean
	@echo "Removing Testcontainers (if any)..."
	@docker rm -f $$(docker ps -aq --filter "label=org.testcontainers=true") 2>/dev/null || true
	@echo "Clean complete."

logs: ## Show logs from all running services
	docker compose logs -f

ps: ## Show status of all services
	docker compose ps