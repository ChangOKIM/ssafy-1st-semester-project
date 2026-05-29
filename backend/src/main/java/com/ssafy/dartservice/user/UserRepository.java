package com.ssafy.dartservice.user;

import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserRepository {

	@Select("SELECT COUNT(*) > 0 FROM users WHERE email = #{email}")
	boolean existsByEmail(@Param("email") String email);

	@Select("""
		SELECT id, email, password, name, role, created_at
		FROM users
		WHERE email = #{email}
		""")
	@Results(id = "userMap", value = {
		@Result(column = "created_at", property = "createdAt")
	})
	Optional<User> findByEmail(@Param("email") String email);

	@Select("""
		SELECT id, email, password, name, role, created_at
		FROM users
		WHERE id = #{id}
		""")
	@Results(id = "userMapById", value = {
		@Result(column = "created_at", property = "createdAt")
	})
	Optional<User> findById(@Param("id") Long id);

	@Insert("""
		INSERT INTO users (email, password, name, role, created_at)
		VALUES (#{email}, #{password}, #{name}, #{role}, #{createdAt})
		""")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	void insert(User user);
}
