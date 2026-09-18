resource "google_artifact_registry_repository" "backend" {
  repository_id = var.artifact_repository
  location      = var.region
  format        = "DOCKER"
  description   = "Varianty backend container images"

  depends_on = [google_project_service.apis]
}
