#!/bin/bash

CHILD_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIwMUtIUFFNQUhaQVQ5N0tHVkgwTkg2RTlKWSIsInR5cGUiOiJDSElMRCIsImNvY3VrVW5pcXVlSWQiOiIwMUtIUFFNQUhaQVQ5N0tHVkgwTkg2RTlKWSIsImViZXZleW5VbmlxdWVJZCI6IjAxS0hQQkdTQTg3QzE5UVFBRUU5ODBGOTE2IiwiaWF0IjoxNzcxODUwNzU5LCJleHAiOjE3NzQ0NDI3NTl9.Y5gDuwINJVcF91jJK18NjisH43G97MCS8AMesAGhAw4"

# Alan İÇİ konum — Okul polygonunun merkezi (geofence-breach GELMEMELİ)
curl -X POST http://localhost:8081/child/location \
  -H "Authorization: Bearer $CHILD_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"lat": 41.0953, "lng": 29.0095, "recordedAt": "2026-02-23T13:00:00Z"}'
