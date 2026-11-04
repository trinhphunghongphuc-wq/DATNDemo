package com.dto.auth;

import com.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải có từ 3 đến 50 ký tự")
    private String username;

    @NotNull(message = "Mật khẩu phải dài hơn 8 ký tự và có ít nhất 1 chữ in hoa")
    @Pattern(
            regexp = "(?s)^(?=.{9,}$)(?=.*\\p{Lu}).*$",
            message = "Mật khẩu phải dài hơn 8 ký tự và có ít nhất 1 chữ in hoa"
    )
    private String password;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    private Role role;
    private String companyName;
    private String companyAddress;

    public RegisterRequest() {
    }

    public RegisterRequest(
            String username,
            String password,
            String email,
            Role role
    ) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }
}