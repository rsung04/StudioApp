package com.example.studioapp_api.solver;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class OperatingHoursSpan {
    private LocalTime start;
    private LocalTime end;
}