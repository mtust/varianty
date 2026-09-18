resource "random_password" "jwt_secret" {
  length  = 48
  special = false
}

locals {
  secrets = {
    "varianty-db-username" = var.db_user
    "varianty-db-password" = random_password.db_password.result
    "varianty-jwt-secret"  = random_password.jwt_secret.result
  }
}

resource "google_secret_manager_secret" "this" {
  for_each = local.secrets

  secret_id = each.key

  replication {
    auto {}
  }

  depends_on = [google_project_service.apis]
}

resource "google_secret_manager_secret_version" "this" {
  for_each = local.secrets

  secret      = google_secret_manager_secret.this[each.key].id
  secret_data = each.value
}

# Both the CI deploy identity (reads secrets to pass them to `deploy-cloudrun`)
# and the Cloud Run runtime identity (reads them at container start) need
# access, scoped to just these three secrets rather than the whole project.
resource "google_secret_manager_secret_iam_member" "deployer_access" {
  for_each = google_secret_manager_secret.this

  secret_id = each.value.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.deployer.email}"
}

resource "google_secret_manager_secret_iam_member" "cloud_run_runtime_access" {
  for_each = google_secret_manager_secret.this

  secret_id = each.value.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloud_run_runtime.email}"
}
