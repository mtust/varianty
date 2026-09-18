output "workload_identity_provider" {
  description = "Value for the GCP_WORKLOAD_IDENTITY_PROVIDER GitHub secret."
  value       = google_iam_workload_identity_pool_provider.github.name
}

output "deploy_service_account_email" {
  description = "Value for the GCP_SERVICE_ACCOUNT GitHub secret."
  value       = google_service_account.deployer.email
}

output "cloudsql_connection_name" {
  description = "Value for the GCP_CLOUDSQL_CONNECTION_NAME GitHub secret."
  value       = google_sql_database_instance.postgres.connection_name
}

output "artifact_registry_repository" {
  description = "Artifact Registry repo URL backend-deploy.yml pushes images to."
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_repository}"
}

output "cloud_run_url" {
  description = "Public URL of the Cloud Run service."
  value       = google_cloud_run_v2_service.backend.uri
}
