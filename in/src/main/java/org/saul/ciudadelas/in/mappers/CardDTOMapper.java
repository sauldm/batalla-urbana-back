package org.saul.ciudadelas.in.mappers;

import org.saul.ciudadelas.in.dto.CardDTO;
import org.saul.ciudadelas.domain.game.deck_cards.cards.Card;

import java.util.ArrayList;
import java.util.List;

public class CardDTOMapper {

    public static CardDTO toCardDTO(Card card){
        CardDTO cardDTO = new CardDTO();
        cardDTO.setColor(card.getColor().ordinal());
        cardDTO.setId(card.getId());
        cardDTO.setName(card.getName());
        cardDTO.setUndestructible(card.isUndestructible());
        cardDTO.setGold(card.getPrice());
        cardDTO.setDescription(card.getDescription());
        return cardDTO;
    }

    public static List<CardDTO> toCardDTOList(List<? extends Card> cards) {
        List<CardDTO> dtos = new ArrayList<>();
        for (Card card : cards) {
            dtos.add(toCardDTO(card));
        }
        return dtos;
    }
}
