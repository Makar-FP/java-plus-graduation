package ru.practicum.eventservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.eventservice.dto.request.RequestEventDto;

import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class EventResultRequestStatusDto {

    private List<RequestEventDto> confirmedRequests;

    private List<RequestEventDto> rejectedRequests;
}
