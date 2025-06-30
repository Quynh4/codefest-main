package com.example;

import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.List;

public abstract class GameLogicHandler {
    protected final Hero hero;
    protected final GameInfoProvider info;

    public GameLogicHandler(Hero hero) {
        this.hero = hero;
        this.info = new GameInfoProvider(hero);
    }

    public abstract void handleTurn() throws IOException;

    protected void performAttack(String direction, int distance) throws IOException {
        System.out.println("Attacking in direction: " + direction);

        if (hero.getInventory().getGun() != null) {
            System.out.println("Using gun to shoot!");
            hero.shoot(direction);
        } else if (hero.getInventory().getMelee() != null) {
            System.out.println("Using melee to attack!");
            hero.attack(direction);
        } else if (hero.getInventory().getThrowable() != null) {
            System.out.println("Throwing item at distance: " + distance);
            hero.throwItem(direction, distance);
        } else {
            System.out.println("No weapon equipped!");
        }
    }

    protected void moveToOrPickup(Weapon weapon) throws IOException {
        if (weapon == null) return;

        String path = PathUtils.getShortestPath(
                info.getGameMap(),
                info.getNodesToAvoid(),
                info.getPlayer(),
                weapon,
                false
        );

        if (path != null) {
            if (path.isEmpty()) {
                hero.pickupItem();
            } else {
                hero.move(path);
            }
        }
    }

    protected void approachAndAttack(Player enemy) throws IOException {
        if (enemy == null) {
            hero.move(info.getRandomDirection());
            return;
        }

        if (hero.getInventory().getGun() != null && info.isEnemyShootable(enemy)) {
            String dir = info.getShootDirection(enemy);
            if (dir != null) {
                hero.shoot(dir);
                return;
            }
        }

        String path = PathUtils.getShortestPath(
                info.getGameMap(),
                info.getNodesToAvoid(),
                info.getPlayer(),
                enemy,
                false
        );

        if (path != null) {
            hero.move(path);
            performAttack(path, 1);
        } else {
            hero.move(info.getRandomDirection());
        }
    }
}
