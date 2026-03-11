# ExpertLink Consult System

A full-stack consulting system built with Spring Boot (backend) and Vue.js (frontend).

## Prerequisites

- Java 17
- Node.js 20.19.0 or >=22.12.0
- MySQL 8.0+
- Maven

## Installation

1. Clone the repository:
```bash
git clone https://github.com/xqr0505/CPT202.git
```

2. Set up the database:
   - Create a MySQL database named `expertlink_dev`
   - Set environment variable `MYSQL_PASSWORD` to your MySQL password

## Running the Application

### Backend

1. Navigate to the backend directory:
```bash
cd backend
```

2. Run the Spring Boot application:
```bash
mvn spring-boot:run
```

The backend will start on `http://localhost:8081`

### Frontend

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Run the development server:
```bash
npm run dev
```

The frontend will start on `http://localhost:5173`

## API Documentation

Once both services are running, visit `http://localhost:8081/swagger-ui.html` for API documentation.