SHELL=/bin/bash

VERSION := $(shell grep -Po '^version=\K.*' gradle.properties)

publish:
	@echo "###### Assembling & running checks ######"
	@./gradlew build
	@echo "###### Pushing latest changes ######"
	@git status;git add .;git commit -m '$(VERSION)';git push origin
	@echo "###### Creating release ######"
	@$(MAKE) create-gh-release

create-gh-release:
	@gh release create $(VERSION) --generate-notes --repo w2sv/SimpleStorage