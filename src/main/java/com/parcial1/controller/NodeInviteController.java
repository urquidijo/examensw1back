package com.parcial1.controller;

import com.parcial1.dto.AcceptNodeInviteResponse;
import com.parcial1.dto.NodeInviteDetailsResponse;
import com.parcial1.dto.NodeInviteLinkResponse;
import com.parcial1.service.NodeInviteService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/node-invites")
@CrossOrigin
public class NodeInviteController {

    private final NodeInviteService nodeInviteService;

    public NodeInviteController(NodeInviteService nodeInviteService) {
        this.nodeInviteService = nodeInviteService;
    }

    @PostMapping("/projects/{projectId}/nodes/{nodeId}/generate")
    public NodeInviteLinkResponse generateInvite(
            @PathVariable String projectId,
            @PathVariable String nodeId,
            @RequestParam(required = false) String nodeLabel,
            Authentication authentication
    ) {
        return nodeInviteService.createInviteLink(projectId, nodeId, nodeLabel, authentication.getName());
    }

    @GetMapping("/{token}")
    public NodeInviteDetailsResponse validateInvite(@PathVariable String token) {
        return nodeInviteService.validateInvite(token);
    }

    @PostMapping("/{token}/accept")
    public AcceptNodeInviteResponse acceptInvite(
            @PathVariable String token,
            Authentication authentication
    ) {
        return nodeInviteService.acceptInvite(token, authentication.getName());
    }
} 