pipeline {
  agent { label 'docker-maven-trivy' }
  stages {
    stage('Trivy FS Scan') {
      steps {
        sh 'trivy fs --exit-code 1 --severity HIGH,CRITICAL .'
      }
    }
  }
}