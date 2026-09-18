# ---- CI/CD deploy identity (used by GitHub Actions to build/push/deploy) ----

resource "google_service_account" "deployer" {
  account_id   = "varianty-deployer"
  display_name = "Varianty backend deployer (GitHub Actions)"

  depends_on = [google_project_service.apis]
}

resource "google_project_iam_member" "deployer_roles" {
  for_each = toset([
    "roles/run.admin",
    "roles/artifactregistry.writer",
    "roles/iam.serviceAccountUser",
  ])

  project = var.project_id
  role    = each.value
  member  = "serviceAccount:${google_service_account.deployer.email}"
}

# ---- Workload Identity Federation: lets GitHub Actions impersonate the
# deployer service account without a long-lived JSON key. ----

resource "google_iam_workload_identity_pool" "github" {
  workload_identity_pool_id = "github-pool"
  display_name              = "GitHub Actions"

  depends_on = [google_project_service.apis]
}

resource "google_iam_workload_identity_pool_provider" "github" {
  workload_identity_pool_id          = google_iam_workload_identity_pool.github.workload_identity_pool_id
  workload_identity_pool_provider_id = "github-provider"
  display_name                       = "GitHub OIDC"

  attribute_mapping = {
    "google.subject"       = "assertion.sub"
    "attribute.repository" = "assertion.repository"
  }
  attribute_condition = "assertion.repository == \"${var.github_repo}\""

  oidc {
    issuer_uri = "https://token.actions.githubusercontent.com"
  }
}

resource "google_service_account_iam_member" "deployer_wif_binding" {
  service_account_id = google_service_account.deployer.name
  role                = "roles/iam.workloadIdentityUser"
  member              = "principalSet://iam.googleapis.com/${google_iam_workload_identity_pool.github.name}/attribute.repository/${var.github_repo}"
}

# ---- Cloud Run runtime identity (used by the deployed container itself,
# kept separate from the CI deploy identity for least privilege). ----

resource "google_service_account" "cloud_run_runtime" {
  account_id   = "varianty-backend-run"
  display_name = "Varianty backend Cloud Run runtime"

  depends_on = [google_project_service.apis]
}

resource "google_project_iam_member" "cloud_run_runtime_cloudsql" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.cloud_run_runtime.email}"
}
