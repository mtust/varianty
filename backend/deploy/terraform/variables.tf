variable "project_id" {
  description = "GCP project ID to deploy into."
  type        = string
}

variable "region" {
  description = "Region for Cloud Run, Artifact Registry and Cloud SQL."
  type        = string
  default     = "europe-central2"
}

variable "github_repo" {
  description = "GitHub repo allowed to impersonate the deploy service account, as \"org/repo\" (e.g. \"myroslav/varianty\")."
  type        = string
}

variable "service_name" {
  description = "Cloud Run service name for the backend."
  type        = string
  default     = "varianty-backend"
}

variable "artifact_repository" {
  description = "Artifact Registry repository name for backend images."
  type        = string
  default     = "varianty"
}

variable "cloudsql_instance_name" {
  description = "Cloud SQL instance name."
  type        = string
  default     = "varianty-db"
}

variable "cloudsql_tier" {
  description = "Cloud SQL machine tier."
  type        = string
  default     = "db-f1-micro"
}

variable "cloudsql_deletion_protection" {
  description = "Prevent accidental `terraform destroy` of the Cloud SQL instance."
  type        = bool
  default     = true
}

variable "db_name" {
  description = "Application database name."
  type        = string
  default     = "varianty"
}

variable "db_user" {
  description = "Application database user."
  type        = string
  default     = "varianty"
}

variable "allow_unauthenticated" {
  description = "Allow public (unauthenticated) invocations of the Cloud Run service. The mobile app calls this API directly, so this defaults to true."
  type        = bool
  default     = true
}
