#!/bin/bash

# LocalStack initialization script
# Runs when LocalStack container becomes ready
# Creates AWS resources (S3 buckets, SQS queues, etc.) for local development

set -e

echo "Initializing LocalStack for trade-demo-backend..."

# Wait for LocalStack to be fully ready
awslocal s3 mb s3://cdp-example-bucket || echo "Bucket already exists"

# Create SQS queue
awslocal sqs create-queue --queue-name cdp-example-queue || echo "Queue already exists"

# Create SNS topic
awslocal sns create-topic --name cdp-example-topic || echo "Topic already exists"

echo "LocalStack initialization complete!"
