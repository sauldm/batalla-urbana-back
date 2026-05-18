package org.saul.ciudadelas.domain.game.deck_cards.actions;

import org.saul.ciudadelas.domain.exception.ExpectedGameError;
import org.saul.ciudadelas.domain.game.EventMessage;
import org.saul.ciudadelas.domain.game.Events;
import org.saul.ciudadelas.domain.game.Game;
import org.saul.ciudadelas.domain.game.deck_cards.Color;
import org.saul.ciudadelas.domain.game.deck_cards.cards.DistrictCard;
import org.saul.ciudadelas.domain.game.deck_cards.OptionalEpicCard;
import org.saul.ciudadelas.domain.game.players.Player;

public class TakeThreeActionCard extends DistrictCard implements OptionalEpicCard {

    public TakeThreeActionCard(Long id) {
        super(id,"Pozo de la suerte", Color.PURPLE, false,"Consigue 3 cartas por 2 de oro",5L, 6);
    }

    @Override
    public void execute(Game game, Player player) {
        if (player.getGold() < (2L)){
            game.getEventsBuffer().add(new EventMessage(Events.IMPOSIBLE_ACTION,"No tienes suficientes monedas" ));
            return;
        };
        player.removeGold(2L);
        player.addDistrictCardsInHand(game.getDistrictCards(3));
        game.getEventsBuffer().add(new EventMessage(Events.DISTRICT_HABILITY_USED,player
                .getNickName()+" ha usado la habilidad de "+this.getName()+"\n Gana: 3 cartas\nPierde 2 de oro"));
        game.getActualTurn().addDistrictUsedThisRound(this.getId());
    }
}
