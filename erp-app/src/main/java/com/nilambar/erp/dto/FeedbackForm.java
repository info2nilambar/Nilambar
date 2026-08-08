package com.nilambar.erp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FeedbackForm {

    @NotNull(message = "Pick a rating from 1 to 5.")
    @Min(value = 1, message = "Pick a rating from 1 to 5.")
    @Max(value = 5, message = "Pick a rating from 1 to 5.")
    private Integer rating;

    @Size(max = 1000, message = "Keep the comment under 1000 characters.")
    private String comment;

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
