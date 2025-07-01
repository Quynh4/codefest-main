package com.example.logic_impl;

import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.model.obstacles.Obstacle;

import java.io.IOException;

public class ChestAttackLogic extends GameLogicHandler {
    public ChestAttackLogic(Hero hero) {
        super(hero);
    }

    @Override
    public void handleTurn() throws IOException {
        Obstacle nearestChest = info.getNearestChest();
        System.out.println("[LOG] Nearest chest: " + (nearestChest != null ? nearestChest : "null"));

        if (nearestChest == null) {
            String randomDir = info.getRandomDirection();
            System.out.println("[LOG] No chest found. Moving randomly to: " + randomDir);
            hero.move(randomDir);
            return;
        }

        String direction = info.getDirectionToAdjacent(nearestChest);
        System.out.println("[LOG] Direction to adjacent chest: " + direction);

        if (direction != null) {
            System.out.println("[LOG] Chest is adjacent. Attacking in direction: " + direction);
            hero.attack(direction);
            return;
        }

        String path = PathUtils.getShortestPath(
                info.getGameMap(),
                info.getNodesToAvoid(),
                info.getPlayer(),
                nearestChest,
                false
        );

        if (path != null) {
            System.out.println("[LOG] Moving along shortest path to chest: " + path);
            hero.move(path);
        } else {
            String randomDir = info.getRandomDirection();
            System.out.println("[LOG] No path to chest. Moving randomly to: " + randomDir);
            hero.move(randomDir);
        }
    }
}
