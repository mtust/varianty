# Terraform seeds the Cloud Run service so all the surrounding wiring
# (Cloud SQL connection, secrets, service account) exists from the start.
# The actual application image is deployed by .github/workflows/backend-deploy.yml
# via `deploy-cloudrun`, so the container image is intentionally left out of
# Terraform's management (see lifecycle.ignore_changes below) to avoid every
# `terraform apply` reverting a real deploy back to the placeholder image.

resource "google_cloud_run_v2_service" "backend" {
  name     = var.service_name
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"

  template {
    service_account = google_service_account.cloud_run_runtime.email

    scaling {
      min_instance_count = 0
      max_instance_count = 3
    }

    containers {
      image = "us-docker.pkg.dev/cloudrun/container/hello"

      ports {
        container_port = 8080
      }

      env {
        name  = "DATABASE_URL"
        value = "jdbc:postgresql:///${var.db_name}?cloudSqlInstance=${google_sql_database_instance.postgres.connection_name}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
      }

      env {
        name = "DATABASE_USERNAME"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.this["varianty-db-username"].secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "DATABASE_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.this["varianty-db-password"].secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "JWT_SECRET"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.this["varianty-jwt-secret"].secret_id
            version = "latest"
          }
        }
      }

      volume_mounts {
        name       = "cloudsql"
        mount_path = "/cloudsql"
      }
    }

    volumes {
      name = "cloudsql"
      cloud_sql_instance {
        instances = [google_sql_database_instance.postgres.connection_name]
      }
    }
  }

  depends_on = [
    google_project_service.apis,
    google_secret_manager_secret_version.this,
  ]

  lifecycle {
    ignore_changes = [
      template[0].containers[0].image,
    ]
  }
}

resource "google_cloud_run_v2_service_iam_member" "public_invoker" {
  count = var.allow_unauthenticated ? 1 : 0

  name     = google_cloud_run_v2_service.backend.name
  location = google_cloud_run_v2_service.backend.location
  role     = "roles/run.invoker"
  member   = "allUsers"
}
