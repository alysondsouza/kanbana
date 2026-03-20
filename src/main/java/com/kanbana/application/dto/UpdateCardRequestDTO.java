package com.kanbana.application.dto;

import jakarta.validation.constraints.Size;

public class UpdateCardRequestDTO {

    @Size(max = 255, message = "Card title must not exceed 255 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    public UpdateCardRequestDTO() {}

    public String getTitle()       { return title; }
    public String getDescription() { return description; }
    public void setTitle(String title)             { this.title = title; }
    public void setDescription(String description) { this.description = description; }
}
