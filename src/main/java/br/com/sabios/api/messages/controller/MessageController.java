package br.com.sabios.api.messages.controller;

import br.com.sabios.api.messages.dto.ConversationResponse;
import br.com.sabios.api.messages.dto.MessageResponse;
import br.com.sabios.api.messages.dto.SendMessageRequest;
import br.com.sabios.api.messages.service.MessageService;
import br.com.sabios.api.users.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "Mensagens", description = "Endpoints para chat e conversas entre clientes e conselheiros")
@SecurityRequirement(name = "bearerAuth")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/conversations")
    @Operation(summary = "Lista as conversas ativas do usuário logado (Cliente ou Conselheiro)")
    public ResponseEntity<List<ConversationResponse>> listMyConversations(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.listUserConversations(user));
    }

    @GetMapping("/conversations/{id}")
    @Operation(summary = "Retorna o histórico de mensagens de uma conversa específica")
    public ResponseEntity<List<MessageResponse>> listMessages(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(messageService.listConversationMessages(user, id));
    }

    @PostMapping
    @Operation(summary = "Envia uma mensagem (cria a conversa automaticamente caso ainda não exista)")
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.sendMessage(user, request));
    }
}
