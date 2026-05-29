package com.ssafy.dartservice.user;

import java.time.LocalDateTime;

public class User {

	private Long id;
	private String email;
	private String password;
	private String name;
	private UserRole role = UserRole.USER;
	private LocalDateTime createdAt;

	public User() {
	}

	private User(String email, String password, String name) {
		this.email = email;
		this.password = password;
		this.name = name;
		this.role = UserRole.USER;
		this.createdAt = LocalDateTime.now();
	}

	public static User create(String email, String encodedPassword, String name) {
		return new User(email, encodedPassword, name);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public UserRole getRole() {
		return role;
	}

	public void setRole(UserRole role) {
		this.role = role;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
