# Ticket Reservation System

A secure RESTful Ticket Reservation System developed
using Spring Boot, Spring Security, JWT Authentication,
MongoDB and Postman.

## Technologies Used

- Java 21
- Spring Boot 3.2.12
- Spring Security 6
- JWT Authentication
- MongoDB
- Spring Data MongoDB
- Maven
- Postman

## Features

- User registration and authentication
- JWT-based authentication
- Role-based access control
- Event management
- Ticket reservations
- Atomic seat reservation
- Reservation cancellation
- Administrator dashboard statistics

## User Roles

- ADMIN
- ORGANIZER
- CUSTOMER

## Running the Application

1. Install Java 21.
2. Install and start MongoDB.
3. Configure the required environment variables.
4. Run the application:

   ./mvnw spring-boot:run

## Database

Database: ticket_reservation_db

Collections:
- users
- events
- reservations

## Course

Full-Stack Software Development

University of Kelaniya
Academic Year 2024/2025 - Semester II