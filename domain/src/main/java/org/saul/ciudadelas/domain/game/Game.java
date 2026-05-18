package org.saul.ciudadelas.domain.game;

import org.saul.ciudadelas.domain.GameEvent;
import org.saul.ciudadelas.domain.exception.InternalGameException;
import org.saul.ciudadelas.domain.game.deck_cards.DeckCards;
import org.saul.ciudadelas.domain.game.deck_cards.actions.*;
import org.saul.ciudadelas.domain.game.deck_cards.cards.CharacterCard;
import org.saul.ciudadelas.domain.game.deck_cards.cards.DistrictCard;
import org.saul.ciudadelas.domain.game.players.Player;

import java.util.*;

import static org.saul.ciudadelas.domain.game.GameConstants.*;

public class Game {
    private final UUID id;
    private final DeckCards<DistrictCard> deckDistrictCards;
    private final List<Player> players;
    private final DeckCards<CharacterCard> deckCharacterCards;
    private List<Round> rounds;

    private Turn actualTurn;
    private List<RoundEvent> specialRoundEvents;
    private Long characterSkipped;

    private Long characterRobbed;
    private List<EventMessage> eventBuffer;
    private Long characterId;


    private Game(UUID id, DeckCards<DistrictCard> deckDistrictCards, List<Player> players, DeckCards<CharacterCard> deckCharacterCards) {
        this.id = id;
        this.deckDistrictCards = deckDistrictCards;
        this.players = players;
        this.deckCharacterCards = deckCharacterCards;
        this.rounds = new ArrayList<>();
        this.specialRoundEvents = new ArrayList<>();
        this.characterSkipped = null;
    }

    public static Game initializeNewGame(UUID id,DeckCards<DistrictCard> deckDistrictCards, List<Player> players) {
        DeckCards<CharacterCard> deckCharacterCards = getAllCharacterCardsForGame();
        Game game = new Game(id,deckDistrictCards, players, deckCharacterCards);
        game.eventBuffer = new ArrayList<>();
        game.eventBuffer.add(new EventMessage(Events.GAME_STARTED,"El juego ha comenzado"));
        game.deckDistrictCards.addCards(getAllEpicDistrictCards(deckDistrictCards.size()));
        deckDistrictCards.randomizeCards();
        game.players.forEach(player ->{
                    List<DistrictCard> districtCard = deckDistrictCards.getCard(DISTRICT_CARDS_PER_PLAYER);
                    player.addDistrictCardsInHand(districtCard);
                    player.setPrivateDistrictGained(districtCard);
        });
        game.addRound();
        game.actualTurn = game.getActualRound().getActualTurn();
        game.characterIdTurn();
        game.eventBuffer.add(new EventMessage(Events.PRIVATE,"Cartas obtenidas"));

        return game;
    }


    private static List<DistrictCard> getAllEpicDistrictCards(int existingDistrictCardsCount) {
        Long startingId = existingDistrictCardsCount + 1L;
        List<DistrictCard> epicDistrictCards = new ArrayList<>();
        epicDistrictCards.add(new TakeThreeActionCard(startingId));
        //startingId ++;
        return epicDistrictCards;
    }

    private static DeckCards<CharacterCard> getAllCharacterCardsForGame() {
        DeckCards<CharacterCard> deckCharacterCards = new DeckCards<>();
        deckCharacterCards.addCards(List.of(
                new AssassinActionCard(),
                new ThiefActionCard(),
                new WizardActionCard(),
                new KingActionCard(),
                new BishopActionCard(),
                new MerchantActionCard(),
                new ArchitectActionCard(),
                new MilitaryActionCard()
        ));
        return deckCharacterCards;
    }
    public Turn getActualTurn(){
        return actualTurn;
    }

    public List<Player> getPlayers(){
        return players;
    }

    public UUID getId(){
        return id;
    }

    public Long getCharacterId(){
        return characterId;
    }

    public void addTurnSkipped(Long characterId){
        if (characterId == null) throw new InternalGameException("La carta no puede ser nula");
        this.characterSkipped = characterId;

    }

    public Long getCharacterSkipped(){
        return this.characterSkipped;
    }

    public Long getCharacterRobbed(){
        return this.characterRobbed;
    }

    public List<DistrictCard> getDistrictCards(int numberOfCards) {
        return deckDistrictCards.getCard(numberOfCards);
    }

    private void addRound() {
        characterRobbed = null;
        characterSkipped = null;
        rounds.add(Round.initializeRound(getNewTurns(), this));
        actualTurn = getActualRound().getActualTurn();
        characterIdTurn();
        getActualRound().startTurn(this);
    }

    public GameEvent clearEventsBuffer(){
        return new GameEvent(this,eventBuffer);
    }

    private List<Turn> getNewTurns() {
        List<Turn> turns = new ArrayList<>();
        CharacterCard randomCharacterCard;
        for (Player player : players) {
            for (int j = 0; j < CHARACTER_CARDS_PER_PLAYER; j++) {
                randomCharacterCard = deckCharacterCards.getRandomCard();
                player.addCharacterCard(randomCharacterCard);
                turns.add(new Turn(player, randomCharacterCard));
            }
        }
        Collections.sort(turns);
        return turns;
    }


    public void addRoundEvent(RoundEvent roundEvent) {
        if (roundEvent == null) throw new InternalGameException("El evento no puede ser nulo");
        getActualRound().addRoundEvent(roundEvent);
    }

    public void setCharacterRobbed(Long characterCardId){
        characterRobbed = characterCardId;
    }

    public void stoleCharacterGold(CharacterCard characterRobed, CharacterCard characterThief) {
        if (characterRobed == null) throw new InternalGameException("La carta no puede ser nula");
        if (characterThief == null) throw new InternalGameException("La carta no puede ser nula");
        Player playerThief = findPlayerByCharacterId(characterThief.getId());
        if (playerThief == null) throw new InternalGameException("El jugador que roba no puede ser nulo");
        Player playerRobed = findPlayerByCharacterId(characterRobed.getId());
        if (characterRobbed.equals(characterSkipped)){
            eventBuffer.add(new EventMessage(Events.MESSAGE,"Personaje verdugo, no puede ser robado"));
            return;
        }
        eventBuffer.add(new EventMessage(Events.CHARACTER_CARD_STEALED, "El personaje "+characterRobed.getName()+" ha sido robado por el "+characterThief.getName()));
        playerThief.addGold(playerRobed.getAllGold());
    }

    public void stopCharacterPlaying(Long characterCardId) {
        if (characterCardId == null) throw new InternalGameException("La carta no puede ser nula");
        getActualRound().skipCharacterTurn(characterCardId);
        eventBuffer.add(new EventMessage(Events.CHARACTER_CARD_ELIMINATED,"El personaje "+findCharacterCardById(characterCardId).getName()+" ha sido eliminado esta ronda"));

    }

    public void characterIdTurn(){
        characterId = actualTurn.getCharacterId();
    }

    public boolean characterIsNotInRound(Long characterCardId) {
        if (characterCardId == null) throw new InternalGameException("La carta no puede ser nula");
        return getActualRound().characterIsNotInRound(characterCardId);
    }

    public void nextStep() {
        if (!actualTurn.isTurnCompleted()) {
            System.out.println("No se puede pasar de turno, el turno actual no ha finalizado");
            return;
        }

        if (actualTurn.getPlayer().districtCardsBuilt() >= MAX_DISTRICTS_TO_BUILD_GAME) {
            eventBuffer.add(new EventMessage(Events.GAME_ENDED, "El juego ha acabado"));
            actualTurn.getPlayer().sumPoints(3);
            for (Player player:players){
                player.sumAllPoints();
            }
            getWiner();
            return;
        }
        if (getActualRound().isLastTurn()) {
            clearPlayerCharacterCards();
            eventBuffer.add(new EventMessage(Events.NEXT_ROUND, "Siguiente ronda"));
            addRound();
            actualTurn = getActualRound().getActualTurn();
            characterIdTurn();
            return;
        }
        getActualRound().nextTurn(this);
        actualTurn = getActualRound().getActualTurn();
        characterIdTurn();
    }

    private void getWiner() {
        players.stream()
                .max(Comparator.comparingInt(Player::getPoints))
                .ifPresent(Player::addWin);
    }

    public void clearPlayerCharacterCards(){
        for (Player player : players) {
            deckCharacterCards.addCards(player.clearCharacterCards());
        }
    }

    public Player findPlayerByCharacterId(Long characterCardId) {
        if (characterCardId == null) throw new InternalGameException("La carta no puede ser nula");
        for (Player player : players) {
            if (player.haveCharacter(characterCardId) != null) return player;
        }
        return null;
    }

    public Round getActualRound() {
        return rounds.getLast();
    }

    public void swapHandsWithPlayer(CharacterCard actualCharacter, Long targetPlayerId) {
        if (actualCharacter == null) throw new InternalGameException("El jugador no puede ser nulo");
        if (targetPlayerId == null) throw new InternalGameException("La carta no puede ser nula");
        Player actualPlayer = findPlayerByCharacterId(actualCharacter.getId());
        Player targetPlayer = players.stream().filter(player -> player.getId().equals(targetPlayerId)).findFirst().orElse(null);
        if (actualPlayer == null) throw new InternalGameException("El jugador no puede ser nulo");
        if (targetPlayer == null) throw new InternalGameException("El jugador objetivo no puede ser nulo");
        if (actualPlayer == targetPlayer) throw new InternalGameException("El jugador no puede elegirse a si mismo");

        List<DistrictCard> tempHand = new ArrayList<>(targetPlayer.getAllDistrictCardsInHand());
        targetPlayer.addDistrictCardsInHand(actualPlayer.getAllDistrictCardsInHand());
        actualPlayer.addDistrictCardsInHand(tempHand);
        eventBuffer.add(new EventMessage(Events.HANDS_SWAPPED,"Se ha intercambiado cartas con "+ targetPlayer.getNickName()));

        actualTurn.characterHabilityUsed();
    }

    public void executePlayerCharacterAbility(Long characterCardActionId, Long targetId) {
        if (!isTurnCharacter(characterCardActionId)) throw new InternalGameException("No es el turno de ese personaje");
        actualTurn.executeCharacterHability(this, characterCardActionId, targetId);
    }

    public void executeDistrictAbility(Long districtCardId){
        if (districtCardId == null) throw new InternalGameException("La carta no puede ser nula");
        actualTurn.executeDistrictAbility(this, districtCardId);
    }

    public void destroyDistrictOfOtherPlayer(Long districtCardId, CharacterCard actualCharacter) {
        if (actualCharacter == null) throw new InternalGameException("El jugador no puede ser nulo");
        if (districtCardId == null) throw new InternalGameException("La carta no puede ser nula");

        Player playerTarget = findPlayerByDistrictCardIdBuilt(districtCardId);
        if (playerTarget == null) throw new InternalGameException("El jugador objetivo no puede ser nulo");

        Player actualPlayer = findPlayerByCharacterId(actualCharacter.getId());
        if (actualPlayer == null) throw new InternalGameException("El jugador no puede ser nulo");

        DistrictCard districtCard = playerTarget.findDistrictCardBuilt(districtCardId);
        if (districtCard == null) throw new InternalGameException("La carta de distrito no puede ser nula");


        if (actualPlayer.getGold() < (districtCard.getPrice() - 1)) {
            eventBuffer.add(new EventMessage(Events.IMPOSIBLE_ACTION, "No tienes suficiente oro"));

            return;
        }
        if (playerTarget.findCharacterUndestructible() != null) {
            eventBuffer.add(new EventMessage(Events.IMPOSIBLE_ACTION, "Este distrito es indestructible"));
            return;
        }

        if (districtCard.isUndestructible()) {
            eventBuffer.add(new EventMessage(Events.IMPOSIBLE_ACTION, "Este distrito es indestructible"));
            return;
        }

        actualPlayer.removeGold(districtCard.getPrice()-1);
        this.deckDistrictCards.addCard(playerTarget.getDistrictCardBuilt(districtCardId));
        eventBuffer.add(new EventMessage(Events.DISTRICT_CARD_DESTROYED, districtCard.getName()+" ha sido destruida"));
        getEventsBuffer().add(new EventMessage(Events.CHARACTER_HABILITY_USED,"Ha usado la habilidad de "+findCharacterCardById(actualCharacter.getId()).getName()));

        actualTurn.characterHabilityUsed();
    }


    public Player findPlayerByDistrictCardIdBuilt(Long districtCardId) {
        if (districtCardId == null) throw new InternalGameException("La carta no puede ser nula");
        for (Player player : players) {
            if (player.findDistrictCardBuilt(districtCardId) != null) return player;
        }
        return null;
    }

    public void swapCardsWithGame(WizardActionCard wizardActionCard) {
        if (wizardActionCard == null) throw new InternalGameException("La carta no puede ser nula");
        if (deckDistrictCards.size() == 0) {
            getEventsBuffer().add(new EventMessage(Events.IMPOSIBLE_ACTION,"No quedan cartas en el mazo"));
            return;
        }
        Player actualPlayer = findPlayerByCharacterId(wizardActionCard.getId());

        List<DistrictCard> playerCards = actualPlayer.getAllDistrictCardsInHand();
        List<DistrictCard> gameCards = deckDistrictCards.getCard(playerCards.size());
        actualPlayer.addDistrictCardsInHand(gameCards);
        deckDistrictCards.addCards(playerCards);
        actualTurn.characterHabilityUsed();
        eventBuffer.add(new EventMessage(Events.HANDS_SWAPPED,actualPlayer.getNickName()+" ha intercambiado cartas con el mazo"));

    }

    public CharacterCard findCharacterCardById(Long characterCardId) {
        CharacterCard characterCard = getActualRound().findCharacterById(characterCardId) ;
        if (characterCard == null){
            characterCard = deckCharacterCards.haveThisCard(characterCardId);
        }
        return characterCard;
    }
    public void buildDistrictCard(Long districtCardId, Long characterCardId) {
        if (!isTurnCharacter(characterCardId)) throw new InternalGameException("No es el turno de ese personaje");

        CharacterCard characterCard = findCharacterCardById(characterCardId);
        if (characterCard == null) throw new InternalGameException("La carta no puede ser nula");
        if (!characterCard.canBuildDistrict(getActualRound().getDistrictsBuiltThisTurn())) {
            eventBuffer.add(new EventMessage(Events.IMPOSIBLE_ACTION, "Ya no puedes construir más"));
            return;
        }

        Player player = findPlayerByCharacterId(characterCardId);
        if (player == null) throw new InternalGameException("El jugador no puede ser nulo");

        DistrictCard districtCard = player.findDistrictCardInHand(districtCardId);
        if (districtCard == null) throw new InternalGameException("La carta no puede ser nula");
        if (player.getGold() < (districtCard.getPrice())){
            eventBuffer.add(new EventMessage(Events.IMPOSIBLE_ACTION, "Oro insuficiente"));
            return;
        }

        player.removeGold(districtCard.getPrice());
        player.buildDistrictCard(player.getDistrictCardFromHand(districtCardId));
        getActualRound().incrementDistrictsBuiltThisTurn();
    }

    public boolean isTurnCharacter(Long characterCardId){
        return actualTurn.getCharacterId().equals(characterCardId);
    }



    public void characterChooseCoins(){
        getActualRound().playerAddCoins(TURN_PLAYER_GOLD);
        eventBuffer.add(new EventMessage(Events.CHOOSED_COINS, "El jugador ha elegido oro"));
    }

    public void characterChooseCards(){
        getActualRound().playerAddDistrictCard(this,TURN_DISTRICT_CARD_PLAYER);
        eventBuffer.add(new EventMessage(Events.CHOOSED_CARDS, "El jugador ha elegido cartas"));
    }

    public List<EventMessage> getEventsBuffer() {
        return eventBuffer;
    }

    @Override
    public String toString() {
        return "Game{" +
                "id=" + id +
                ", players=" + players +
                '}';
    }

    public DeckCards<DistrictCard> districtCards() {
        return deckDistrictCards;
    }
}