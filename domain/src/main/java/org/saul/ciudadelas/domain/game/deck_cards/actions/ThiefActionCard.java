package org.saul.ciudadelas.domain.game.deck_cards.actions;

import org.saul.ciudadelas.domain.exception.ExpectedGameError;
import org.saul.ciudadelas.domain.exception.InternalGameException;
import org.saul.ciudadelas.domain.game.EventMessage;
import org.saul.ciudadelas.domain.game.Events;
import org.saul.ciudadelas.domain.game.Game;
import org.saul.ciudadelas.domain.game.RoundEvent;
import org.saul.ciudadelas.domain.game.deck_cards.Color;
import org.saul.ciudadelas.domain.game.deck_cards.OtherPlayerActionCharacterCard;
import org.saul.ciudadelas.domain.game.deck_cards.cards.CharacterCard;

public class ThiefActionCard extends CharacterCard implements OtherPlayerActionCharacterCard {


    public ThiefActionCard() {
        super(2L,"Saqueador", Color.GREY,false,"Elige al personaje al que quieres robar",1,0L);
    }

    @Override
    public void execute(Game game, Long characterCardId) {
        CharacterCard characterRobbed = game.findCharacterCardById(characterCardId);
        if (characterCardId == null) throw new InternalGameException("La carta no puede ser nula");
        if (game.getActualRound().getActualTurn().getPlayer() == game.findPlayerByCharacterId(characterCardId)){
            game.getEventsBuffer().add(new EventMessage(Events.MESSAGE,"El jugador no puede elegirse a si mismo"));
            return;
        }
        if (characterRobbed.getClass().equals(AssassinActionCard.class)) throw new InternalGameException("No puedes robar al verdugo");
        RoundEvent event = new RoundEvent(characterCardId, (actualGame) -> {
            actualGame.stoleCharacterGold(characterRobbed,this);
        });
        game.getActualRound().getActualTurn().characterHabilityUsed();
        game.addRoundEvent(event);
        if (game.getActualRound().getTurnByCharacter(characterCardId) == null){
            game.getEventsBuffer().add(new EventMessage(Events.CHARACTER_HABILITY_USED,"Ha usado la habilidad de "+game.findCharacterCardById(this.getId()).getName()));
            return;
        }
        game.getActualRound().getTurnByCharacter(characterCardId).characterRobbed();
        game.setCharacterRobbed(characterCardId);
        game.getEventsBuffer().add(new EventMessage(Events.CHARACTER_HABILITY_USED,"Ha usado la habilidad de "+game.findCharacterCardById(this.getId()).getName()));

    }
}
