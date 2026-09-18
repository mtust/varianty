resource "random_password" "db_password" {
  length  = 32
  special = false
}

resource "google_sql_database_instance" "postgres" {
  name             = var.cloudsql_instance_name
  region           = var.region
  database_version = "POSTGRES_16"

  deletion_protection = var.cloudsql_deletion_protection

  settings {
    tier    = var.cloudsql_tier
    edition = "ENTERPRISE"

    backup_configuration {
      enabled = true
    }

    ip_configuration {
      ipv4_enabled = true
    }
  }

  depends_on = [google_project_service.apis]
}

resource "google_sql_database" "app" {
  name     = var.db_name
  instance = google_sql_database_instance.postgres.name
}

resource "google_sql_user" "app" {
  name     = var.db_user
  instance = google_sql_database_instance.postgres.name
  password = random_password.db_password.result
}
