package com.kanbana.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateColumnRequestDTO {

    @NotBlank(message = "Column title must not be blank")
    @Size(max = 255, message = "Column title must not exceed 255 characters")
    private String title;

    public CreateColumnRequestDTO() {}

    public CreateColumnRequestDTO(String title) {
        this.title = title;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
