package ewm.request.controller;


import ewm.request.service.PublicRequestService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequestMapping("/admin/requests")
public class AdminRequestController {
    final PublicRequestService requestService;

    @GetMapping("/confirmed")
    Map<Long, Long> getConfirmedRequestsMap(@RequestParam List<Long> eventIds) {
        return requestService.getConfirmedRequestsMap(eventIds);
    }
}
