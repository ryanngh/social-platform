package com.ryan.socialplatform.relationship;

import com.ryan.socialplatform.relationship.dto.CloseFriendResponse;
import com.ryan.socialplatform.relationship.service.CloseFriendService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/close-friends")
public class CloseFriendController {

    private final CloseFriendService closeFriendService;

    public CloseFriendController(CloseFriendService closeFriendService) {
        this.closeFriendService = closeFriendService;
    }

    @PostMapping("/{friendId}")
    public ResponseEntity<CloseFriendResponse> add(
            @PathVariable UUID friendId,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        return ResponseEntity.ok(closeFriendService.add(friendId, currentUserId));
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> remove(
            @PathVariable UUID friendId,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        closeFriendService.remove(friendId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<CloseFriendResponse>> getMyCloseFriends(
            @AuthenticationPrincipal UUID currentUserId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(closeFriendService.getCloseFriends(currentUserId, pageable));
    }
}