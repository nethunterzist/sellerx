package com.ecommerce.sellerx.activitylog;

import com.ecommerce.sellerx.auth.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class ActivityLogController {
    private final ActivityLogService activityLogService;
    private final JwtService jwtService;

    @GetMapping("/activity-logs")
    public ResponseEntity<List<ActivityLogDto>> getActivityLogs(
            HttpServletRequest request,
            @RequestParam(defaultValue = "20") int limit) {
        Long userId = jwtService.getUserIdFromToken(request);
        List<ActivityLogDto> logs = activityLogService.getActivityLogs(userId, limit);
        return ResponseEntity.ok(logs);
    }
}
