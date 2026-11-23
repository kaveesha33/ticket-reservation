package com.example.ticketreservation.config;

import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Event;
import com.example.ticketreservation.model.Role;

import com.example.ticketreservation.repository.UserRepository;
import com.example.ticketreservation.repository.EventRepository;

import org.springframework.boot.CommandLineRunner;

import org.springframework.data.mongodb.core.MongoTemplate;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;

    private final EventRepository eventRepository;

    private final PasswordEncoder passwordEncoder;

    private final MongoTemplate mongoTemplate;

    public DataLoader(
            UserRepository userRepository,
            EventRepository eventRepository,
            PasswordEncoder passwordEncoder,
            MongoTemplate mongoTemplate) {

        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.passwordEncoder = passwordEncoder;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {

        System.out.println("Starting initial database seeding...");

        // Create initial user accounts
        seedUsers();

        // Create sample events
        seedEvents();

        // Ensure reservations collection exists
        createReservationsCollection();

        System.out.println("Database seeding completed.");
    }

    // Seed administrator, organizer and customer accounts
    private void seedUsers() {

        createUser(
                "admin",
                "admin123",
                "System Administrator",
                Role.ADMIN
        );

        createUser(
                "organizer",
                "organizer123",
                "Event Organizer",
                Role.ORGANIZER
        );

        createUser(
                "customer",
                "customer123",
                "Test Customer",
                Role.CUSTOMER
        );
    }

    // Create a user if the username does not already exist
    private void createUser(
            String username,
            String password,
            String fullName,
            Role role) {

        if (userRepository.existsByUsername(username)) {

            System.out.println(
                    "User already exists: " + username
            );

            return;
        }

        AppUser user = new AppUser();

        user.setUsername(username);

        // Store BCrypt password hash
        user.setPassword(
                passwordEncoder.encode(password)
        );

        user.setFullName(fullName);

        user.setRole(role);

        userRepository.save(user);

        System.out.println(
                "Created user: " + username + " (" + role + ")"
        );
    }

    // Create sample events if no events exist
    private void seedEvents() {

        if (eventRepository.count() > 0) {

            System.out.println("Sample events already exist.");

            return;
        }

        Event event = new Event();

        event.setName("Just a lil Concert");

        event.setDescription("You should come to this concert, it's gonna be lit!");

        event.setVenue("VMD Park");

        event.setEventDate(
                LocalDateTime.of(2030, 12, 31, 18, 0)
        );

        event.setTotalSeats(20);

        event.setAvailableSeats(20);

        event.setTicketPrice(
                new BigDecimal("1000.00")
        );

        event.setOrganizerUsername("organizer");

        eventRepository.save(event);

        System.out.println("Created sample event: " + event.getName());
    }

    // Create reservations collection if it does not exist
    private void createReservationsCollection() {

        if (!mongoTemplate.collectionExists("reservations")) {

            mongoTemplate.createCollection("reservations");

            System.out.println("Created reservations collection.");
        }
    }
}