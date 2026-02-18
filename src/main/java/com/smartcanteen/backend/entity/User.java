package com.smartcanteen.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name="user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;

    @Column(unique = true,nullable = false)
    private String email;

    private String password;

    private String role;

    // Default Constructor
    public User() {
    }
    // Constructor
    public User(String role, String password, String email, String name) {
        this.role = role;
        this.password = password;
        this.email = email;
        this.name = name;
    }

    // Getter and Setters


    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
