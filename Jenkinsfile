pipeline {
    agent any
    tools {
        maven 'maven3'
    }
    environment {
        SONAR_IP = '13.48.204.148'
        ECR_REGISTRY = '729591922294.dkr.ecr.eu-north-1.amazonaws.com'
        IMAGE_REPO = "${ECR_REGISTRY}/cwvj-devsecops-demo"
    }
    stages {
        stage('Trivy FS Scan') {
            steps {
                sh 'trivy fs --exit-code 1 --severity HIGH,CRITICAL .'
            }
        }
        stage('Build & Sonar') {
            steps {
                withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                    sh '''
                        mvn clean verify sonar:sonar \
                          -Dsonar.projectKey=cwvj-devsecops-demo \
                          -Dsonar.host.url="http://${SONAR_IP}:9000" \
                          -Dsonar.token="${SONAR_TOKEN}" \
                          -Dsonar.qualitygate.wait=true
                    '''
                }
            }
        }
        stage('ECR Login') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-creds']]) {
                    sh 'aws ecr get-login-password --region eu-north-1 | docker login --username AWS --password-stdin $ECR_REGISTRY'
                }
            }
        }
        stage('Build Image') {
            steps {
                sh 'export DOCKER_BUILDKIT=0 && docker build --platform linux/amd64 -t "$IMAGE_REPO:$BUILD_NUMBER" -t "$IMAGE_REPO:latest" .'
            }
        }
        stage('Trivy Image Scan') {
            steps {
                sh 'trivy image --exit-code 0 --severity HIGH,CRITICAL "$IMAGE_REPO:$BUILD_NUMBER"'
            }
        }
        stage('Push to ECR') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-creds']]) {
                    sh 'aws ecr get-login-password --region eu-north-1 | docker login --username AWS --password-stdin $ECR_REGISTRY'
                    sh 'docker push "$IMAGE_REPO:$BUILD_NUMBER"'
                    sh 'docker push "$IMAGE_REPO:latest"'
                }
            }
        }
        stage('Update Deployment') {
            steps {
                sh 'sed -i "s|image:.*|image: $IMAGE_REPO:$BUILD_NUMBER|g" deploy-svc.yaml'
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-creds']]) {
                    sh '''#!/bin/bash -l
                        aws eks update-kubeconfig \
                          --region eu-north-1 \
                          --name eks-project-cluster \
                          --kubeconfig /home/jenkins/.kube/config

                        kubectl create ns cwvj-devsecops --dry-run=client -o yaml | kubectl apply -f -
                        kubectl apply -f deploy-svc.yaml

                        kubectl rollout status -n cwvj-devsecops deployment/cwvj-devsecops-demo --timeout=120s || {
                            kubectl rollout undo -n cwvj-devsecops deployment/cwvj-devsecops-demo || true
                            exit 1
                        }
                    '''
                }
            }
        }
    }
    post {
        success { echo "Build ${env.BUILD_NUMBER} succeeded" }
        failure { echo "Build ${env.BUILD_NUMBER} failed" }
        always  { echo "Build ${env.BUILD_NUMBER} finished" }
    }
}