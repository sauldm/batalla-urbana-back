package org.saul.ciudadelas.in;

import org.saul.ciudadelas.domain.game.Game;
import org.saul.ciudadelas.domain.game.PlayerClassification;
import org.saul.ciudadelas.in.dto.*;
import org.saul.ciudadelas.in.ws.WebSocketSender;
import org.saul.ciudadelas.domain.GameEvent;
import org.saul.ciudadelas.domain.game.players.Player;
import org.saul.ciudadelas.services.GameService;
import org.saul.ciudadelas.services.LobbyService;
import org.saul.ciudadelas.services.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;


import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.saul.ciudadelas.in.Constants.*;


@RestController
public class GameController {

    private GameService gameService;
    private LobbyService lobbyService;
    private PlayerService playerService;
    private WebSocketSender webSocketSender;


    public GameController(GameService gameService, LobbyService lobbyService, PlayerService playerService, WebSocketSender webSocketSender) {
        this.gameService = gameService;
        this.lobbyService = lobbyService;
        this.playerService = playerService;
        this.webSocketSender = webSocketSender;
    }


    @MessageMapping("/getLobbies")
    public void getLobbies(){
        lobbyService.getLobbiesWithLessThan2Players();
        webSocketSender.publishLobies();

    }

    @MessageMapping("/getPlayers/{id}")
    public void getLobbyPlayers(@DestinationVariable UUID id){
        webSocketSender.publishPlayersLobby(id, lobbyService.getAllPlayers(id));
    }

    @PostMapping("/api/lobbies")
    public UUID createLobby() {
        UUID lobbyId = UUID.randomUUID();
        lobbyService.createNewLobby(lobbyId);

        webSocketSender.publishLobies();
        return lobbyId;
    }
    @PostMapping("/api/joinLobby")
    public ResponseEntity<String> joinLobby(@RequestBody LobbyAddPlayer dto) {
        Player player = lobbyService.addPlayerToLobby(
                dto.getId(),
                playerService.getOrCreatePlayer(dto.getNickName()).getNickName()
        );

        webSocketSender.publishLobies();
        webSocketSender.publishPlayersLobby(
                dto.getId(),
                lobbyService.getAllPlayers(dto.getId())
        );

        return ResponseEntity.ok(player.getNickName());
    }

    @MessageMapping("/unjoinLobby")
    public void unjoinLobby(@Payload LobbyAddPlayer dto){
        lobbyService.removePlayerFromLobby(dto.getId(), dto.getNickName());
        webSocketSender.publishPlayersLobby(dto.getId(), lobbyService.getAllPlayers(dto.getId()));
        webSocketSender.publishLobies();
    }


    @PostMapping("api/createGame")
    public ResponseEntity<UUID> startNewGame(@RequestBody LobbyDTO lobbyDTO){
        if (gameService.existsGame(lobbyDTO.getId())) throw new RuntimeException("El game no puede existir antes de crearlo");
        if (lobbyDTO.getPlayers() == null) throw new RuntimeException("Los players no pueden ser null");
        List<Player> players = lobbyDTO.getPlayers()
                .stream()
                .map(playerService::findByName)
                .flatMap(Optional::stream)
                .toList();

        if (players.size() != PLAYERS_PER_GAME) {
            throw new RuntimeException("No se han encontrado todos los jugadores");
        }
        Game game = gameService.startGame(lobbyDTO.getId(),players);
        lobbyService.deleteLobby(lobbyDTO.getId());
        webSocketSender.sendGameStarted(game.getId());
        return ResponseEntity.ok(game.getId());

    }

    @MessageMapping("/game/{id}/state")
    public void getGameState(@DestinationVariable UUID id){
        GameEvent gameEvent = gameService.getCurrentGameEvent(id);
        webSocketSender.publishEvent(gameEvent);
        webSocketSender.sendPrivateInfo(gameEvent);
    }

    @MessageMapping("/game/{id}/private")
    public void requestPrivateInfo(
            @DestinationVariable UUID id,
            Principal principal
    ) {
        Player player = gameService.getPlayer(id, principal.getName());
        System.out.println("request: "+ principal.getName());
        webSocketSender.sendToPlayer(id,principal,player);
    }



    @MessageMapping("/deleteGame/{id}")
    public void deleteGame(@DestinationVariable UUID gameId){
        GameEvent gameEvent = gameService.deleteGame(gameId);
        webSocketSender.publishEvent(gameEvent);
        webSocketSender.sendPrivateInfo(gameEvent);

    }

    @MessageMapping("/nextStep")
    public void nextStep(UUID gameId, Principal principal){
        GameEvent gameEvent = gameService.nextStep((gameId));
        webSocketSender.publishEvent(gameEvent);
        webSocketSender.sendPrivateInfo(gameEvent);
    }

    @MessageMapping("/CharacterHability")
    public void executePlayerHability(ExecuteCharacterHabilityDTO executeCharacterHabilityDTO, Principal principal){
        if (executeCharacterHabilityDTO.getGameId() == null) throw new RuntimeException("El id del game no puede ser nulo");
        if (executeCharacterHabilityDTO.getCharacterId() == null) throw new RuntimeException("El id del character no puede ser nulo");
        if (executeCharacterHabilityDTO.getTargetId() == null) throw new RuntimeException("El id del target no puede ser nulo");

        UUID gameId = (executeCharacterHabilityDTO.getGameId());

        GameEvent gameEvent = gameService.executePlayerCharacterAbility(
                gameId,
                executeCharacterHabilityDTO.getCharacterId(),
                executeCharacterHabilityDTO.getTargetId()
        );
        webSocketSender.publishEvent(gameEvent);
        webSocketSender.sendPrivateInfo(gameEvent);
    }

    @MessageMapping("/DistrictHability")
    public void executeDistrictHability(ExecuteDistrictHabilityDTO executeDistrictHabilityDTO, Principal principal){
        if (executeDistrictHabilityDTO.getGameId() == null) throw new RuntimeException("El id del game no puede ser nulo");
        if (executeDistrictHabilityDTO.getDistrictId() == null) throw new RuntimeException("El id del district no puede ser nulo");

        UUID gameId = (executeDistrictHabilityDTO.getGameId());

        GameEvent gameEvent = gameService.executePlayerDistrictAbility(
                gameId,
                executeDistrictHabilityDTO.getDistrictId()
        );

        webSocketSender.publishEvent(gameEvent);
        webSocketSender.sendPrivateInfo(gameEvent);
    }

    @MessageMapping("/build")
    public void buildDistrict(BuildDistrictDTO buildDistrictDTO, Principal principal){
        if (buildDistrictDTO.getGameId() == null) throw new RuntimeException("El id del game no puede ser nulo");
        if (buildDistrictDTO.getDistrictId() == null) throw new RuntimeException("El id del district no puede ser nulo");
        if (buildDistrictDTO.getCharacterId() == null) throw new RuntimeException("El id del character no puede ser nulo");

        UUID gameId = (buildDistrictDTO.getGameId());
        GameEvent gameEvent = gameService.buildDistrict(
                gameId,
                buildDistrictDTO.getDistrictId(),
                buildDistrictDTO.getCharacterId()
        );

        webSocketSender.publishEvent(gameEvent);
        webSocketSender.sendPrivateInfo(gameEvent);
    }

    @MessageMapping("/chooseCoin")
    public void chooseCoin(UUID gameId, Principal principal){
        if (gameId == null) throw new RuntimeException("El id del game no puede ser nulo");

        GameEvent gameEvent = gameService.playerChooseCoins((gameId));

        webSocketSender.publishEvent(gameEvent);
        webSocketSender.sendPrivateInfo(gameEvent);
    }

    @MessageMapping("/chooseCard")
    public void chooseCards(UUID gameId, Principal principal){
        if (gameId == null) throw new RuntimeException("El id del game no puede ser nulo");

        GameEvent gameEvent = gameService.playerChooseCards((gameId));

        webSocketSender.publishEvent(gameEvent);
        webSocketSender.sendPrivateInfo(gameEvent);
    }

    @GetMapping("/api/getClassificationTable")
    public List<PlayerClassification> getClassificationTable(){
        return playerService.getClassificationTable();
    }
}
