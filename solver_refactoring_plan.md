# High-Level Refactoring Plan: Java Solver for GCP

This document outlines the refactoring plan for deploying the Java-based timetable solver to Google Cloud Platform (GCP), enabling asynchronous processing, and integrating it with a frontend application via the existing API backend.

## 1. Review of Prior Analysis (Summary)

An analysis of the existing Java solver components revealed the following:

*   **`DanceTimetableSolver.java`**: The core engine using Google OR-Tools for CP-SAT model creation, constraint definition, and objective functions. It loads OR-Tools native libraries and processes `SolverInput` to produce `SolverOutput`. Its logic for slot conversion based on `effectiveDayWindows` is central.
*   **`SolverInput.java`**: A DTO aggregating all necessary data for a solver run, including configuration (`slotMinutes`, `effectiveDayWindows`) and lists of JPA entities (Instructors, Rooms, Priority Requests, Class Definitions, etc.). This is currently constructed by `SolverServiceImpl`.
*   **`SolverOutput.java`**: A DTO for returning solver results, including `stageAResults` (as `LockedBlockDTOs`), a console log, and success/status messages.
*   **`SolverService.java`**: An interface defining the contract for solver operations (`triggerSolver`, `getJobStatus`, `getStageAResults`), implying an asynchronous pattern.
*   **`SolverServiceImpl.java`**: The current implementation orchestrating solver runs. It fetches data from JPA repositories, populates `SolverInput`, invokes `danceTimetableSolver.executeSolve()`, and returns a `SolverJobResponseDTO`. It contains TODOs for full asynchronous job management.

## 2. Proposed GCP Architecture

*   **Recommended GCP Service: Cloud Run**
    *   **Justification:**
        *   **Scalability:** Scales automatically (including to zero), cost-effective.
        *   **Ease of Deployment:** Deploys Docker containers, ideal for Java applications with native dependencies (OR-Tools).
        *   **Management of Native Dependencies:** Containerization ensures a consistent runtime environment.
        *   **Statelessness:** Aligns with a solver processing input and producing output per request. The current `DanceTimetableSolver` fits this model.
        *   **Integration:** Easily integrates with Pub/Sub, Cloud Tasks, IAM, Load Balancing.
    *   **Alternatives Considered & Why Cloud Run is Preferred:**
        *   *Cloud Functions:* Native library management is more complex; execution time limits can be an issue.
        *   *App Engine Standard:* Does not easily support native libraries.
        *   *App Engine Flex:* Viable (supports Docker), but Cloud Run offers finer-grained scalability and potentially simpler configuration for stateless containers.
        *   *Google Kubernetes Engine (GKE):* Overkill for a single solver service unless a broader microservices strategy requires its advanced orchestration.

*   **How the Solver Fits into Cloud Run:**
    *   The Java solver application will be packaged as a Docker image, including the JAR and OR-Tools native libraries.
    *   A Cloud Run service created from this image will expose an HTTP endpoint (e.g., `/solve`) or be triggered by Pub/Sub.
    *   Cloud Run manages container instances to handle requests.

    ```mermaid
    graph LR
        A[Frontend/Other Services] --> B{API Gateway / Load Balancer};
        B --> C[Existing Spring Boot API Backend];
        C -- HTTP Request (SolverInput JSON) / PubSub Message --> D[Cloud Run: Solver Service (Container)];
        D -- Access (if needed for specific configs) --> E[Cloud SQL/Firestore];
        D -- HTTP Response (SolverOutput JSON) / Writes to DB --> C;
        C -- Response --> A;

        subgraph GCP Project
            D
            E
            F[GCP Pub/Sub]
            G[GCP Datastore/Firestore for Job Status/Results]
        end
        C -.-> F;
        F -.-> D;
        D -.-> G;
        C -.-> G;
    ```

## 3. Identify Refactoring Areas

*   **`DanceTimetableSolver.java`**: Core logic largely unchanged. Ensure robust `Loader.loadNativeLibraries()` in Docker.
*   **`SolverInput.java` / `SolverOutput.java`**: Ensure easy JSON serialization/deserialization. Flatten/simplify if the solver service is fully decoupled.
*   **`SolverServiceImpl.java` (in existing Spring Boot API)**: Significant changes. Shifts from *executing* to *invoking* the remote Cloud Run solver. Data-fetching logic to prepare `SolverInput` JSON remains. HTTP client call replaces direct solver invocation.
*   **`SolverService.java` (interface in existing Spring Boot API)**: Interface remains; implementation changes.
*   **New Components for Cloud Run Service:**
    *   Lightweight Java application (e.g., Spring Boot, Javalin, SparkJava, or plain servlets) for the Cloud Run container.
    *   HTTP controller/handler in this new application to:
        *   Receive HTTP request (with `SolverInput` JSON) or Pub/Sub message.
        *   Deserialize JSON to `SolverInput`.
        *   Instantiate and run `DanceTimetableSolver`.
        *   Serialize `SolverOutput` to JSON (for HTTP response) or store results (for async).
*   **Configuration:**
    *   `pom.xml` (or `build.gradle`): For Cloud Run service dependencies.
    *   `Dockerfile`: For the Cloud Run service container image.

## 4. Outline Refactoring Changes

*   **Input Handling (Cloud Run Service):**
    *   Receives `SolverInput` as a JSON payload via HTTP POST or Pub/Sub message.
    *   **Responsibility for `SolverInput` population:** The existing `SolverServiceImpl.java` (in the main Spring Boot API) fetches data from JPA repositories, constructs `SolverInput`, serializes to JSON, and sends it to the Cloud Run service. This keeps the Cloud Run solver focused on computation.
    *   `SolverInput` DTO adapted for easy JSON deserialization.

*   **Output Handling (Cloud Run Service):**
    *   **Synchronous (if simple/fast):** Returns `SolverOutput` as JSON in HTTP response.
    *   **Asynchronous (recommended for longer solves):** Immediately returns a job ID. Writes full `SolverOutput` to Cloud Storage or Firestore upon completion. Client polls or uses notifications.

*   **Statelessness & Lifecycle (Cloud Run Service):**
    *   Inherently stateless; each request/message triggers a new invocation.
    *   `DanceTimetableSolver`'s design (initializing per run) fits well.
    *   Lifecycle is per-request/message.

*   **OR-Tools Dependency (Cloud Run Service):**
    *   **Strategy:** Package OR-Tools native libraries directly into the Docker image.
        1.  Include OR-Tools Java JAR in `pom.xml`.
        2.  In `Dockerfile`:
            *   Copy native libraries (e.g., `libjniortools.so` for Linux) into the container.
            *   Place them in a directory on the Java library path (e.g., `/usr/lib` or custom path via `ENV JAVA_OPTS="-Djava.library.path=/app/lib/native"`).
            *   `Loader.loadNativeLibraries();` should find them.
        *   Ensure base Docker image (e.g., OpenJDK) is compatible.

*   **Service Layer:**
    *   **Existing Spring Boot App (`SolverServiceImpl.java`):**
        *   Uses an HTTP client (e.g., `RestTemplate`, `WebClient`) or Pub/Sub client to interact with the Cloud Run solver.
        *   Still responsible for preparing `SolverInput` JSON.
        *   Handles responses/job status for asynchronous operations.
    *   **New Cloud Run Solver Service:**
        *   Minimal service layer. An HTTP request handler or Pub/Sub message listener.
        *   Deserializes input, calls `DanceTimetableSolver`, serializes/stores output.
        *   Expects **pre-aggregated input**.

*   **API Design (for new Cloud Run Solver Service - if HTTP invoked directly):**
    *   **Endpoint:** `POST /solve`
    *   **Request (application/json):** JSON representation of `SolverInput`.
    *   **Response (application/json):**
        *   **Success (200 OK):** JSON representation of `SolverOutput`.
        *   **Error (4xx/5xx):** JSON error object.
    *   *(Note: For the Pub/Sub driven approach, this direct HTTP API on the solver might be internal or not exposed publicly if the main API is the sole entry point).*

## 5. Asynchronous Processing (Recommended)

*   **Approach: Pub/Sub + Cloud Run Worker + Firestore/Datastore**

    ```mermaid
    sequenceDiagram
        participant FE as Frontend
        participant API as Existing Spring Boot API
        participant PubSub as GCP Pub/Sub
        participant SolverCR as Solver Cloud Run Service
        participant DB_FS as GCP Datastore/Firestore (for Job Status/Results)

        FE->>+API: POST /trigger-solve (SolveRequestDTO)
        API->>API: Generate JobID
        API->>API: Prepare SolverInput JSON
        API->>+PubSub: Publish Message (JobID, SolverInput JSON) to 'solve-requests' topic
        API-->>-FE: HTTP 202 Accepted (SolverJobResponseDTO: JobID, Status: PENDING)
        PubSub-->>+SolverCR: Delivers Message (triggers Solver)
        SolverCR->>SolverCR: Execute Solve Logic
        SolverCR->>+DB_FS: Store SolverOutput (against JobID), Update Status: COMPLETED/FAILED
        SolverCR-->>-PubSub: Acknowledge Message

        loop Poll for Status
            FE->>+API: GET /job-status/{JobID}
            API->>+DB_FS: Get Status for JobID
            DB_FS-->>-API: Job Status
            API-->>-FE: SolverJobResponseDTO (Status, Message)
        end

        alt Job Completed
            FE->>+API: GET /job-results/{JobID}
            API->>+DB_FS: Get Results for JobID
            DB_FS-->>-API: SolverOutput Data
            API-->>-FE: Results
        end
    ```

    **Steps:**
    1.  **Trigger:** Client calls `SolverServiceImpl.triggerSolver()` in the Spring Boot API.
    2.  **Job Creation & Enqueue (API Backend):**
        *   Generates `jobId`.
        *   Prepares `SolverInput` JSON.
        *   Publishes a message to GCP **Pub/Sub topic** (e.g., `solve-requests`) with `jobId` and `SolverInput` JSON.
        *   Immediately returns `SolverJobResponseDTO` (`jobId`, status `PENDING`) to the client.
    3.  **Processing (Cloud Run Solver):**
        *   Cloud Run solver service subscribes to the Pub/Sub topic.
        *   Receives message, deserializes `SolverInput`, runs `DanceTimetableSolver`.
    4.  **Result Storage & Status Update (Cloud Run Solver):**
        *   Writes `SolverOutput` and final status (`COMPLETED`, `FAILED`) to **Firestore** or **Cloud SQL**, keyed by `jobId`.
    5.  **Client Retrieval (via API Backend):**
        *   Client polls `SolverServiceImpl.getJobStatus(jobId)`. This endpoint queries Firestore/Cloud SQL.
        *   Once `COMPLETED`, client calls `SolverServiceImpl.getStageAResults(jobId)` (etc.), which fetches results from Firestore/Cloud SQL.

## 6. Deployment to GCP & Frontend Invocation Workflow

**A. Deploying the Solver Service to Cloud Run**

1.  **Prerequisites:** GCP project, `gcloud` CLI, Docker.
2.  **Containerize (`Dockerfile` for Solver Service):**
    ```dockerfile
    # Base image (e.g., OpenJDK 17)
    FROM eclipse-temurin:17-jdk-jammy
    WORKDIR /app

    # OR-Tools Native Libraries
    RUN mkdir -p /app/lib/native
    # COPY native-libs/linux-x86-64/libjniortools.so /app/lib/native/libjniortools.so # Adjust path
    # Ensure this path contains the .so file for Linux x64 compatible with Cloud Run
    # Example: Download and extract OR-Tools release, then copy the relevant .so file
    # ADD https://github.com/google/or-tools/releases/download/v9.9/or-tools_amd64_java_v9.9.3992.tar.gz /tmp/
    # RUN tar -xzf /tmp/or-tools_amd64_java_v9.9.3992.tar.gz -C /tmp \
    # && cp /tmp/or-tools_java_v9.9.3992/lib/libjniortools.so /app/lib/native/
    # RUN cp /tmp/or-tools_java_v9.9.3992/lib/com.google.ortools.jar /app/lib/ # If not using Maven for ortools-java

    ENV JAVA_OPTS="-Djava.library.path=/app/lib/native"

    # Application JAR
    COPY target/solver-service-1.0.0.jar app.jar # Adjust path

    EXPOSE 8080 # Port application listens on
    ENTRYPOINT ["java", "-jar", "app.jar"]
    ```
    *   **OR-Tools Natives:** Ensure `libjniortools.so` (for Linux x86-64) is correctly placed and accessible via `java.library.path`. The commented `ADD` and `RUN` lines show one way to fetch it during build if not vendored.

3.  **Build Docker Image:**
    ```bash
    docker build -t gcr.io/YOUR_PROJECT_ID/solver-service:v1 .
    ```
4.  **Push Image to GCR/Artifact Registry:**
    ```bash
    gcloud auth configure-docker
    docker push gcr.io/YOUR_PROJECT_ID/solver-service:v1
    ```
5.  **Deploy to Cloud Run:**
    ```bash
    gcloud run deploy solver-service \
        --image gcr.io/YOUR_PROJECT_ID/solver-service:v1 \
        --platform managed \
        --region YOUR_GCP_REGION \
        --port 8080 \
        --memory 1Gi --cpu 1 \ # Adjust as needed
        # For Pub/Sub triggered, direct invocation might not be needed from public internet
        # If HTTP triggered by main API: --no-allow-unauthenticated and set up IAM
        # If Pub/Sub triggered: Configure Pub/Sub push subscription to invoke this service
        # --set-env-vars KEY1=VALUE1
    ```
    *   **Authentication:** Secure with IAM. The invoker (main API service account or Pub/Sub push subscription service account) needs `roles/run.invoker`.

6.  **Set up Pub/Sub (for asynchronous flow):**
    *   Create Pub/Sub topic (e.g., `solve-requests`).
    *   Create a Pub/Sub push subscription:
        *   **Target Type:** Cloud Run service.
        *   **Service:** Select your deployed `solver-service`.
        *   **Path:** `/` (or specific path if your service expects Pub/Sub messages on a sub-path).
        *   **Service Account:** A service account with `roles/run.invoker` for the `solver-service` and `roles/pubsub.subscriber` (or more specific permissions).

**B. Frontend Invocation Workflow (Asynchronous via Pub/Sub)**

(Mermaid diagram and detailed steps as previously outlined, showing frontend interaction with the API backend, which then uses Pub/Sub to trigger the Cloud Run solver, and polls for results stored in Firestore/Cloud SQL.)

1.  **User Action (Frontend):** Initiates solve.
2.  **Frontend to API Backend (POST `/api/trigger-solve`):** Sends `SolveRequestDTO`.
3.  **API Backend - Job Initiation:** Generates `jobId`, prepares `SolverInput` JSON.
4.  **API Backend - Publish to Pub/Sub:** Sends message with `jobId` and `SolverInput` JSON to `solve-requests` topic.
5.  **API Backend - Immediate Response to Frontend (HTTP 202):** Returns `jobId` and `PENDING` status.
6.  **Pub/Sub to Cloud Run Solver:** Delivers message, triggers solver instance.
7.  **Cloud Run Solver - Processing:** Deserializes input, executes `DanceTimetableSolver`.
8.  **Cloud Run Solver - Store Results & Status:** Writes `SolverOutput` and final status to Firestore/Cloud SQL against `jobId`.
9.  **Cloud Run Solver - Acknowledge Pub/Sub Message.**
10. **Frontend - Poll for Status (GET `/api/job-status/{jobId}`):** Periodically checks job status.
11. **API Backend - Retrieve Status:** Queries Firestore/Cloud SQL for status.
12. **API Backend - Return Status to Frontend.**
13. **Frontend - Update UI.**
14. **Frontend - Fetch Results (on `COMPLETED` status, GET `/api/job-results/{jobId}`):**
15. **API Backend - Retrieve Results:** Fetches from Firestore/Cloud SQL.
16. **API Backend - Return Results to Frontend.**
17. **Frontend - Display Results.**

This detailed plan should provide a solid foundation for the refactoring effort.