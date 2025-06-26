package com.example.solverservice.solver; // Updated package

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class OperatingHoursSpan {
    private LocalTime start;
    private LocalTime end;
}