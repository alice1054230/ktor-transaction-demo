#!/bin/bash

docker compose -p transaction-local-test -f "./postgresql/docker-compose.yml" up -d