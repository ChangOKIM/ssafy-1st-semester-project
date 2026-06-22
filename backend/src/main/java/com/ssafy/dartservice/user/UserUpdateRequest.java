package com.ssafy.dartservice.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
	@NotBlank(message = "이메일을 입력해 주세요.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	String email,

	@NotBlank(message = "이름을 입력해 주세요.")
	@Size(max = 50, message = "이름은 50자 이하여야 합니다.")
	String name
) {
}
