
package com.example;

import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.algorithm.PathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Utils {

    public static Weapon getNearestWeapon(GameMap gameMap, Player player) {
        List<Weapon> allWeapons = new ArrayList<>(gameMap.getListWeapons());
        return getNearestWeaponFromList(player, allWeapons);
    }

    public static Weapon getNearestGun(GameMap gameMap, Player player) {
        return getNearestWeaponFromList(player, gameMap.getAllGun());
    }

    public static Weapon getNearestMelee(GameMap gameMap, Player player) {
        return getNearestWeaponFromList(player, gameMap.getAllMelee());
    }

    public static Weapon getNearestThrowable(GameMap gameMap, Player player) {
        return getNearestWeaponFromList(player, gameMap.getAllThrowable());
    }

    private static Weapon getNearestWeaponFromList(Player player, List<Weapon> weapons) {
        Weapon nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Weapon w : weapons) {
            double distance = PathUtils.distance(player, w);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = w;
            }
        }
        return nearest;
    }

    public static Player getNearestPlayer(GameMap gameMap, Player player) {
        List<Player> others = gameMap.getOtherPlayerInfo();
        Player nearest = null;
        int minDistance = Integer.MAX_VALUE;

        for (Player p : others) {
            if (p.getHealth() > 0) {
                int distance = PathUtils.distance(player, p);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = p;
                }
            }
        }
        return nearest;
    }

    public static String getDirectionTo(Player from, Player to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();

        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "r" : "l";
        } else {
            return dy > 0 ? "d" : "u";
        }
    }

    public static String getRandomDirection() {
        String[] directions = {"u", "d", "l", "r"};
        return directions[new Random().nextInt(directions.length)];
    }

    public static List<Node> getNodesToAvoid(GameMap gameMap) {
        List<Node> nodes = new ArrayList<>(gameMap.getListIndestructibles());
        nodes.removeAll(gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        nodes.addAll(gameMap.getOtherPlayerInfo());
        return nodes;
    }
}
