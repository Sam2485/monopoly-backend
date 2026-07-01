# Docker Deployment Guide for Vyapar

This folder contains configuration files and supporting scripts for containerizing the Vyapar application.

## Prerequisites
- Docker installed on your machine
- Docker Compose installed

## Setup & Running

To build and run the entire stack (PostgreSQL + Spring Boot Backend + React Frontend):

1. Go to the backend root directory (where `docker-compose.yml` is located):
   ```bash
   cd /Users/rajshaikh/Desktop/Vyapar
   ```

2. Run docker-compose up:
   ```bash
   docker-compose up --build
   ```

3. The services will be accessible at:
   - **Frontend**: http://localhost (Port 80)
   - **Backend**: http://localhost:9090
   - **PostgreSQL**: localhost:5432
