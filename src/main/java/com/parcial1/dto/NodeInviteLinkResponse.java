package com.parcial1.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeInviteLinkResponse {
    private String token;
    private String inviteLink;
}