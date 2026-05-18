package org.saul.ciudadelas.in.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CardDTO {
    private Long id;
    private Long gold;
    private String name;
    private String description;
    private boolean undestructible;
    private int color;
}
