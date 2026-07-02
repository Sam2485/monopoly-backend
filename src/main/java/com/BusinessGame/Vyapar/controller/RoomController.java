package com.BusinessGame.Vyapar.controller;

import com.BusinessGame.Vyapar.dto.ApiResponse;
import com.BusinessGame.Vyapar.dto.CreateRoomRequest;
import com.BusinessGame.Vyapar.dto.RoomResponse;
import com.BusinessGame.Vyapar.entity.Game;
import com.BusinessGame.Vyapar.service.RoomService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ApiResponse<RoomResponse> createRoom(@RequestBody CreateRoomRequest request) {
        RoomResponse response = roomService.createRoom(request.getMaxPlayers());
        return ApiResponse.success(response, "Room created successfully");
    }

    @PostMapping("/{roomCode}/join")
    public ApiResponse<RoomResponse> joinRoom(@PathVariable String roomCode) {
        RoomResponse response = roomService.joinRoom(roomCode);
        return ApiResponse.success(response, "Joined room successfully");
    }

    @DeleteMapping("/{roomId}/leave")
    public ApiResponse<Void> leaveRoom(@PathVariable UUID roomId) {
        roomService.leaveRoom(roomId);
        return ApiResponse.success(null, "Left room successfully");
    }

    @PostMapping("/{roomId}/ready")
    public ApiResponse<RoomResponse> readyPlayer(@PathVariable UUID roomId) {
        RoomResponse response = roomService.readyPlayer(roomId);
        return ApiResponse.success(response, "Toggled ready status");
    }

    @PostMapping("/{roomId}/start")
    public ApiResponse<UUID> startGame(@PathVariable UUID roomId) {
        Game game = roomService.startGame(roomId);
        return ApiResponse.success(game.getId(), "Game started successfully");
    }

    @GetMapping("/active")
    public ApiResponse<RoomResponse> getActiveRoom() {
        RoomResponse response = roomService.getActiveRoomForCurrentUser();
        return ApiResponse.success(response, "Fetched active room");
    }
}
