package com.example;

import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "125355";
    private static final String PLAYER_NAME = "Q";
    private static final String SECRET_KEY = "sk-ylFJB0fyQ_63cnYEC4Bqbw:r0brCBio4T5NvxE9Vu58_eh7NoES1vMyDH8Kb4w-4IIoQFVLP3L2kI-EMIsH4sPJ3Szji9g-X_aPpDMd5cotAQ";

    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        Emitter.Listener onMapUpdate = new MapUpdateListener(hero);

        hero.setOnMapUpdate(onMapUpdate);
        hero.start(SERVER_URL);
    }
}

class MapUpdateListener implements Emitter.Listener {
    private final Hero hero;

    public MapUpdateListener(Hero hero) {
        this.hero = hero;
    }

    @Override
    public void call(Object... args) {
        try {
            if (args == null || args.length == 0) return;

            GameMap gameMap = hero.getGameMap();
            gameMap.updateOnUpdateMap(args[0]);
            Player player = gameMap.getCurrentPlayer();

            if (player == null || player.getHealth() == 0) {
                System.out.println("Player is dead or data is not available.");
                return;
            }

            List<Node> nodesToAvoid = Utils.getNodesToAvoid(gameMap);
            Player nearestPlayer = Utils.getNearestPlayer(gameMap, player);

            if (hero.getInventory().getGun() == null && hero.getInventory().getThrowable() == null) {
                handleSearchForWeapon(gameMap, player, nodesToAvoid);
            } else {
                handleCombat(nearestPlayer, nodesToAvoid, player);
            }

        } catch (Exception e) {
            System.err.println("Critical error in call method: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleSearchForWeapon(GameMap gameMap, Player player, List<Node> nodesToAvoid) throws IOException {
        System.out.println("No weapon found. Searching for the nearest weapon.");
        Weapon weapon = Utils.getNearestWeapon(gameMap, player);
        if (weapon == null) return;

        String path = PathUtils.getShortestPath(gameMap, nodesToAvoid, player, weapon, false);
        if (path == null) return;

        if (path.isEmpty()) {
            hero.pickupItem();
        } else {
            hero.move(path);
        }
    }

    private void handleCombat(Player nearestPlayer, List<Node> nodesToAvoid, Player player) throws IOException {
        if (nearestPlayer == null) {
            hero.move(Utils.getRandomDirection());
            return;
        }

        String path = PathUtils.getShortestPath(hero.getGameMap(), nodesToAvoid, player, nearestPlayer, false);
        if (path != null) {
            System.out.println("Moving closer to enemy: " + path);
            hero.move(path);
            performAttack(path, 1);
        } else {
            hero.move(Utils.getRandomDirection());
        }
    }

    private void performAttack(String direction, int distance) throws IOException {
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
}
