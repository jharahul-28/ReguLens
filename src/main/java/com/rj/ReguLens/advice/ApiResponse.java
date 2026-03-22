package com.rj.ReguLens.advice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
public class ApiResponse<T> {
    @JsonFormat(pattern = "hh:mm:ss dd:MM:yyyy")
    LocalDateTime timeStamp;
    T data;
    ApiError error;

    ApiResponse() {
        this.timeStamp = LocalDateTime.now();
    }

    ApiResponse(T data){
        this();
        this.data = data;
    }

    ApiResponse(ApiError error){
        this();
        this.error = error;
    }
}
