#!/bin/bash
# Run this in a fast loop to test the burst capacity
for i in {1..25}; do curl -i http://localhost:8080/api/v1/orders/history; done