package com.ssafy.dartservice.investor;

import com.ssafy.dartservice.user.User;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InvestorProfileRepository {

	@Select("""
		SELECT id, user_id AS userId,
		       investment_experience AS investmentExperience,
		       risk_tolerance AS riskTolerance,
		       investment_goals AS investmentGoals,
		       preferred_sectors, created_at, updated_at
		FROM users_profile
		WHERE user_id = #{user.id}
		""")
	Optional<InvestorProfile> findByUser(@Param("user") User user);

	@Insert("""
		INSERT INTO users_profile (
			user_id, investment_experience, risk_tolerance, investment_goals,
			preferred_sectors, created_at, updated_at
		)
		VALUES (
			#{userId}, #{investmentExperience}, #{riskTolerance}, #{investmentGoals},
			#{preferredSectors}, #{createdAt}, #{updatedAt}
		)
		""")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	void insert(InvestorProfile profile);

	@Update("""
		UPDATE users_profile
		SET investment_experience = #{investmentExperience},
		    risk_tolerance = #{riskTolerance},
		    investment_goals = #{investmentGoals},
		    preferred_sectors = #{preferredSectors},
		    updated_at = #{updatedAt}
		WHERE id = #{id}
		""")
	void update(InvestorProfile profile);
}
