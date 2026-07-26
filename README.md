# 🚀 CI/CD DevSecOps Pipeline on AWS EKS

A fully automated, end-to-end **DevSecOps CI/CD pipeline** built with Jenkins that takes a Java application from source code to a live, running deployment on **AWS EKS (Kubernetes)** — with security scanning and code quality gates baked in at every stage.

---

## 📐 Pipeline Overview

```
Code Push (GitHub)
      │
      ▼
   Jenkins Pipeline
      │
      ├── 1. Trivy FS Scan          → scan source code for vulnerabilities & secrets
      ├── 2. Build & SonarQube      → Maven build, unit tests, static code analysis + quality gate
      ├── 3. ECR Login              → authenticate Docker with AWS ECR
      ├── 4. Build Image            → containerize the app with Docker
      ├── 5. Trivy Image Scan       → scan the built image for HIGH/CRITICAL CVEs
      ├── 6. Push to ECR            → push versioned & latest image tags
      ├── 7. Update Deployment      → inject new image tag into k8s manifest
      └── 8. Deploy to Kubernetes   → rolling update on AWS EKS
      │
      ▼
Live App (exposed via AWS LoadBalancer)
```

---

## 🛠️ Tech Stack

| Category            | Tool / Service                     |
|---------------------|-------------------------------------|
| CI/CD Orchestration  | Jenkins                             |
| Build Tool           | Maven                               |
| Code Quality         | SonarQube                           |
| Security Scanning    | Trivy (filesystem + image scans)    |
| Containerization     | Docker                              |
| Container Registry   | AWS ECR                             |
| Orchestration        | AWS EKS (Kubernetes)                |
| Language             | Java (embedded HTTP server)         |

---

## 📂 Project Structure

```
.
├── src/                    # Java application source code
├── pom.xml                 # Maven build config
├── Dockerfile              # Container build instructions
├── deploy-svc.yaml         # Kubernetes Deployment + Service manifest
├── Jenkinsfile             # Full CI/CD pipeline definition
└── README.md
```

---

## 🔍 What Each Stage Does

**1. Trivy FS Scan**
Scans the repository's filesystem for known vulnerabilities and accidentally committed secrets before any build happens.

**2. Build & SonarQube**
Compiles the app, runs unit tests, and pushes results to SonarQube for static analysis. The pipeline waits on SonarQube's Quality Gate before proceeding.

**3. ECR Login**
Authenticates Docker with AWS ECR using credentials securely injected via Jenkins.

**4. Build Image**
Builds a lightweight Docker image (`eclipse-temurin:21-jre-jammy` base) containing the compiled application.

**5. Trivy Image Scan**
Scans the built image itself (OS packages, libraries) for HIGH/CRITICAL vulnerabilities.

**6. Push to ECR**
Pushes the image to AWS ECR, tagged with both the Jenkins build number and `latest`.

**7. Update Deployment**
Updates the Kubernetes manifest (`deploy-svc.yaml`) to point to the newly built image tag.

**8. Deploy to Kubernetes**
Applies the manifest to the EKS cluster and performs a rolling update with automatic rollback if the new pods fail to become ready within the timeout window.

---

## ☸️ Kubernetes Deployment Strategy

To safely deploy on a small, resource-constrained EKS cluster, the rolling update strategy is tuned to avoid scheduling failures:

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 0
    maxUnavailable: 1
```

This ensures Kubernetes never tries to schedule an extra "surge" pod that the cluster doesn't have room for — old pods are terminated first, then new ones are scheduled in their place.

---

## 🌐 Accessing the App

The service is exposed via an AWS **LoadBalancer**:

```yaml
apiVersion: v1
kind: Service
spec:
  type: LoadBalancer
  ports:
    - port: 80
      targetPort: 8080
```

Once deployed, Kubernetes provisions an AWS ELB automatically — the app is reachable on port `80` via the ELB's public DNS name.

---

## 🧩 Key Challenges Solved

- Jenkins credential ID mismatches between SonarQube token configuration and Jenkinsfile references
- AWS IAM authentication and signature errors during ECR login
- Dockerfile build context issues after repo restructuring
- Balancing strict security gates (Trivy severity thresholds) against build velocity
- Unit test failures introduced by application code changes
- Kubernetes pod scheduling failures due to insufficient node resources during rolling updates

---

## 📌 Notes

This is a learning/demo project intended to showcase a complete DevSecOps workflow. A few settings (like Trivy's `--exit-code 0` for image scans) are relaxed for demo purposes and should be tightened (`--exit-code 1`) for production use.
