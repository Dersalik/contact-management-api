#!/bin/bash
set -e

# Configuration
ENVIRONMENT=$1
IMAGE_TAG=$2
NAMESPACE="contact-api-${ENVIRONMENT}"

if [ -z "$ENVIRONMENT" ] || [ -z "$IMAGE_TAG" ]; then
  echo "Usage: $0 <environment> <image-tag>"
  echo "Example: $0 dev v1.0.0"
  exit 1
fi

echo "Deploying to $ENVIRONMENT environment with image tag $IMAGE_TAG"

# Ensure namespace exists
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Create secrets for MongoDB
if [[ "$ENVIRONMENT" == "dev" ]]; then
  # For dev, use simpler credentials
  MONGO_USER="devuser"
  MONGO_PASS="devpassword"
  JWT_SECRET="dev-secret-key-please-change-in-production"
elif [[ "$ENVIRONMENT" == "prod" ]]; then
  # For prod, generate secure credentials
  MONGO_USER="produser"
  MONGO_PASS=$(openssl rand -base64 32)
  JWT_SECRET=$(openssl rand -base64 64)
else
  echo "Unknown environment: $ENVIRONMENT"
  exit 1
fi

# Create MongoDB secret
kubectl create secret generic mongodb-secrets \
  --namespace=$NAMESPACE \
  --from-literal=root-username=$MONGO_USER \
  --from-literal=root-password=$MONGO_PASS \
  --dry-run=client -o yaml | kubectl apply -f -

# Create JWT secret
kubectl create secret generic contact-api-secrets \
  --namespace=$NAMESPACE \
  --from-literal=jwt-secret=$JWT_SECRET \
  --dry-run=client -o yaml | kubectl apply -f -

# Deploy MongoDB first
MONGODB_YAML=$(cat k8s/mongodb.yml)
echo "$MONGODB_YAML" | kubectl apply -n $NAMESPACE -f -

# Wait for MongoDB to be ready
echo "Waiting for MongoDB to be ready..."
kubectl rollout status statefulset/mongodb -n $NAMESPACE --timeout=5m

# Replace placeholder with actual image tag in deployment YAML
DEPLOYMENT_YAML=$(cat k8s/deployment.yml | sed "s|\${DOCKER_IMAGE}|${DOCKER_HUB_USERNAME}/contact-management-api:${IMAGE_TAG}|g")

# Apply deployment
echo "$DEPLOYMENT_YAML" | kubectl apply -n $NAMESPACE -f -

# Wait for deployment to complete
echo "Waiting for API deployment to complete..."
kubectl rollout status deployment/contact-management-api -n $NAMESPACE --timeout=5m

echo "Deployment completed successfully!"
echo "API is available at: https://api.contacts.example.com (if DNS is configured)"

# Print some useful commands
echo ""
echo "Useful commands:"
echo "kubectl get pods -n $NAMESPACE"
echo "kubectl logs -f deployment/contact-management-api -n $NAMESPACE"
echo "kubectl port-forward service/contact-management-api 8080:80 -n $NAMESPACE"