package com.ssafy.dartservice.portfolio;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HoldingRepository {

	@Select("""
		SELECT h.id,
		       h.user_id AS userId,
		       h.stock_code AS stockCode,
		       s.stock_name AS stockName,
		       s.market,
		       s.sector,
		       h.quantity,
		       h.purchase_price AS purchasePrice,
		       h.purchase_date AS purchaseDate,
		       h.created_at AS createdAt,
		       h.updated_at AS updatedAt
		FROM holdings h
		JOIN stocks s ON s.stock_code = h.stock_code
		WHERE h.user_id = #{userId}
		ORDER BY h.purchase_date DESC, h.id DESC
		""")
	List<Holding> findAllByUserId(@Param("userId") Long userId);

	@Select("""
		SELECT h.id,
		       h.user_id AS userId,
		       h.stock_code AS stockCode,
		       s.stock_name AS stockName,
		       s.market,
		       s.sector,
		       h.quantity,
		       h.purchase_price AS purchasePrice,
		       h.purchase_date AS purchaseDate,
		       h.created_at AS createdAt,
		       h.updated_at AS updatedAt
		FROM holdings h
		JOIN stocks s ON s.stock_code = h.stock_code
		WHERE h.id = #{id}
		  AND h.user_id = #{userId}
		""")
	Optional<Holding> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

	@Select("""
		SELECT h.id,
		       h.user_id AS userId,
		       h.stock_code AS stockCode,
		       s.stock_name AS stockName,
		       s.market,
		       s.sector,
		       h.quantity,
		       h.purchase_price AS purchasePrice,
		       h.purchase_date AS purchaseDate,
		       h.created_at AS createdAt,
		       h.updated_at AS updatedAt
		FROM holdings h
		JOIN stocks s ON s.stock_code = h.stock_code
		WHERE h.user_id = #{userId}
		  AND h.stock_code = #{stockCode}
		""")
	Optional<Holding> findByUserIdAndStockCode(@Param("userId") Long userId, @Param("stockCode") String stockCode);

	@Select("SELECT COUNT(*) > 0 FROM stocks WHERE stock_code = #{stockCode}")
	boolean existsStock(@Param("stockCode") String stockCode);

	@Insert("""
		INSERT INTO holdings (user_id, stock_code, quantity, purchase_price, purchase_date)
		VALUES (#{userId}, #{stockCode}, #{quantity}, #{purchasePrice}, #{purchaseDate})
		""")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	void insert(Holding holding);

	@Update("""
		UPDATE holdings
		SET stock_code = #{stockCode},
		    quantity = #{quantity},
		    purchase_price = #{purchasePrice},
		    purchase_date = #{purchaseDate}
		WHERE id = #{id}
		  AND user_id = #{userId}
		""")
	int update(Holding holding);

	@Delete("""
		DELETE FROM holdings
		WHERE id = #{id}
		  AND user_id = #{userId}
		""")
	int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

	@Select("SELECT content FROM portfolio WHERE user_id = #{userId} AND holdings_hash = #{hash}")
	String findCachedDiagnosis(@Param("userId") Long userId, @Param("hash") String hash);

	@Insert("""
		INSERT INTO portfolio (user_id, holdings_hash, content)
		VALUES (#{userId}, #{hash}, #{content})
		ON DUPLICATE KEY UPDATE holdings_hash = #{hash}, content = #{content}, generated_at = CURRENT_TIMESTAMP
		""")
	void saveDiagnosis(@Param("userId") Long userId, @Param("hash") String hash, @Param("content") String content);

	@Delete("DELETE FROM portfolio WHERE user_id = #{userId}")
	void deleteDiagnosis(@Param("userId") Long userId);
}
