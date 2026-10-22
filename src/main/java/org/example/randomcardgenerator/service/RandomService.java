package org.example.randomcardgenerator.service;

import org.example.randomcardgenerator.card.Card;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomService {
    private final Random random = new Random();

    public List<Card> pickRandomCards(List<Card> pool, int count) {
        if (pool == null || pool.isEmpty()) return new ArrayList<>();

        List<Card> selection = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int index = random.nextInt(pool.size());
            selection.add(pool.get(index));
        }
        return selection;
    }
}