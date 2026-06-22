package com.ssafy.dartservice.investor;

import com.ssafy.dartservice.user.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvestorProfile {

	private Long id;
	private User user;
	private Long userId;
	private String investmentExperience;
	private String riskTolerance;
	private String investmentGoals;
	private BigDecimal investableAmount;
	private String preferredSectors;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public InvestorProfile() {
	}

	private InvestorProfile(User user, String investmentExperience, String riskTolerance, String investmentGoals, BigDecimal investableAmount, String preferredSectors) {
		this.user = user;
		this.userId = user.getId();
		this.investmentExperience = investmentExperience;
		this.riskTolerance = riskTolerance;
		this.investmentGoals = investmentGoals;
		this.investableAmount = investableAmount;
		this.preferredSectors = preferredSectors;
		this.createdAt = LocalDateTime.now();
		this.updatedAt = this.createdAt;
	}

	public static InvestorProfile create(User user, String investmentExperience, String riskTolerance, String investmentGoals, BigDecimal investableAmount, String preferredSectors) {
		return new InvestorProfile(user, investmentExperience, riskTolerance, investmentGoals, investableAmount, preferredSectors);
	}

	public void update(String investmentExperience, String riskTolerance, String investmentGoals, BigDecimal investableAmount, String preferredSectors) {
		this.investmentExperience = investmentExperience;
		this.riskTolerance = riskTolerance;
		this.investmentGoals = investmentGoals;
		this.investableAmount = investableAmount;
		this.preferredSectors = preferredSectors;
		this.updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getInvestmentExperience() {
		return investmentExperience;
	}

	public void setInvestmentExperience(String investmentExperience) {
		this.investmentExperience = investmentExperience;
	}

	public String getRiskTolerance() {
		return riskTolerance;
	}

	public void setRiskTolerance(String riskTolerance) {
		this.riskTolerance = riskTolerance;
	}

	public String getInvestmentGoal() {
		return investmentGoals;
	}

	public void setInvestmentGoal(String investmentGoal) {
		this.investmentGoals = investmentGoal;
	}

	public String getInvestmentGoals() {
		return investmentGoals;
	}

	public void setInvestmentGoals(String investmentGoals) {
		this.investmentGoals = investmentGoals;
	}

	public BigDecimal getInvestableAmount() {
		return investableAmount;
	}

	public void setInvestableAmount(BigDecimal investableAmount) {
		this.investableAmount = investableAmount;
	}

	public String getPreferredSectors() {
		return preferredSectors;
	}

	public void setPreferredSectors(String preferredSectors) {
		this.preferredSectors = preferredSectors;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
