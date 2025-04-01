package com.mapdrop.controllers;

import com.mapdrop.models.Game;
import com.mapdrop.models.User;
import com.mapdrop.payload.response.ErrorResponse;
import com.mapdrop.payload.response.GameListResponse;
import com.mapdrop.payload.response.GameResponse;
import com.mapdrop.payload.response.Response;
import com.mapdrop.repository.GameRepository;
import com.mapdrop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("games")
public class GameController {
    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private UserRepository userRepository;

    private ErrorResponse errorResponse = new ErrorResponse();
    private GameResponse gameResponse = new GameResponse();
    private GameListResponse gameListResponse = new GameListResponse();

    private int gameCost = 30;

    @PostMapping("{user_id}")
    public ResponseEntity<Response<?>> createGame(Authentication authentication, @RequestBody Game game, @PathVariable(name = "user_id") int userId) {
        User playingUser = this.userRepository.findById(userId).orElse(null);
        if (playingUser == null) {
            errorResponse.set("not found");
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        if (!playingUser.getUsername().equals(authentication.getName())) {
            errorResponse.set("forbidden request");
            return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
        }

        game.setUser(playingUser);
        game.setCreatedAt(LocalDateTime.now());
        game.setUpdatedAt(LocalDateTime.now());
        playingUser.setTotalPoints(playingUser.getTotalPoints() - gameCost);

        try {
            gameResponse.set(this.gameRepository.save(game));
        } catch (Exception e) {
            errorResponse.set("Bad request");
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(gameResponse, HttpStatus.CREATED);
    }
    @PutMapping("{user_id}/{game_id}")
    public ResponseEntity<Response<?>> updateGame(Authentication authentication, @PathVariable(name = "game_id") int gameID, @RequestBody Game updatedGame, @PathVariable(name = "user_id") int userId) {
        User playingUser = this.userRepository.findById(userId).orElse(null);
        if (playingUser == null) {
            errorResponse.set("not found");
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        Game gameToUpdate = this.gameRepository.findById(gameID).orElse(null);
        if (gameToUpdate == null) {
            errorResponse.set("not found");
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        if (!playingUser.getUsername().equals(authentication.getName())) {
            errorResponse.set("forbidden request");
            return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
        }
        gameToUpdate.setLatitude_guess(updatedGame.getLatitude_guess());
        gameToUpdate.setLongitude_guess(updatedGame.getLongitude_guess());
        gameToUpdate.setLatitude_real(updatedGame.getLatitude_real());
        gameToUpdate.setLongitude_real(updatedGame.getLongitude_real());
        gameToUpdate.setPoints(updatedGame.getPoints());
        gameToUpdate.setLocation(updatedGame.getLocation());
        gameToUpdate.setUpdatedAt(LocalDateTime.now());
        playingUser.setTotalPoints(playingUser.getTotalPoints() + updatedGame.getPoints() + gameCost);

        try {
            gameResponse.set(this.gameRepository.save(gameToUpdate));
        } catch (Exception e) {
            errorResponse.set("Bad request");
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(gameResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{user_id}/history")
    public ResponseEntity<Response<?>> getGameHistory(@PathVariable(name = "user_id") int userId) {
        User playingUser = this.userRepository.findById(userId).orElse(null);
        if (playingUser == null) {
            errorResponse.set("not found");
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }
        List<Game> history = playingUser.getGamesHistory();
        Collections.sort(history, Collections.reverseOrder());
        gameListResponse.set(history.subList(0, Math.min(5, playingUser.getGamesHistory().size())));
        return ResponseEntity.ok(gameListResponse);
    }
}
