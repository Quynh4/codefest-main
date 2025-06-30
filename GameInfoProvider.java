package com.example;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;

import java.util.*;
import java.util.stream.Collectors;

import static jsclub.codefest.sdk.algorithm.PathUtils.checkInsideSafeArea;

public class GameInfoProvider {
    private final Hero hero;
    private final GameMap gameMap;
    private final Player player;
    private final List<Node> nodesToAvoid;

    public GameInfoProvider(Hero hero) {
        this.hero = hero;
        this.gameMap = hero.getGameMap();
        this.player = gameMap.getCurrentPlayer();
        this.nodesToAvoid = computeNodesToAvoid();
    }

    private List<Node> computeNodesToAvoid() {
        List<Node> nodes = new ArrayList<>(gameMap.getListIndestructibles());
        nodes.removeAll(gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        nodes.addAll(gameMap.getOtherPlayerInfo());
        return nodes;
    }

    public List<Node> getNodesToAvoid() {
        return nodesToAvoid;
    }

    public Hero getHero() {
        return hero;
    }

    public GameMap getGameMap() {
        return gameMap;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isOnGunNode() {
        return gameMap.getAllGun().stream()
                .anyMatch(gun -> gun.x == player.x && gun.y == player.y);
    }

    public void goPickupGun() throws java.io.IOException {
        if (!isOnGunNode()) {
            String path = findPathToNearestGun(false);
            if (path != null) {
                hero.move(path);
            }
        } else {
            hero.pickupItem();
        }
    }

    public String findPathToNearestGun(boolean skipDarkArea) {
        int[] Dx = {-1, 1, 0, 0}; // trái, phải, trên, dưới
        int[] Dy = {0, 0, -1, 1};
        String[] directions = {"l", "r", "u", "d"};

        int mapSize = gameMap.getMapSize();
        int safeZone = gameMap.getSafeZone();

        // Danh sách Node bị cấm
        List<Obstacle> initThings = gameMap.getListIndestructibles();
        List<Obstacle> canGoThroughs = gameMap.getObstaclesByTag("CAN_GO_THROUGH");
        List<Node> blockedNodes = new ArrayList<>(initThings);
        blockedNodes.removeAll(canGoThroughs);
        blockedNodes.addAll(nodesToAvoid);

        boolean[][] blocked = new boolean[mapSize][mapSize];
        for (Node node : blockedNodes) {
            if (node.x >= 0 && node.x < mapSize && node.y >= 0 && node.y < mapSize) {
                blocked[node.x][node.y] = true;
            }
        }

        Set<String> gunPositions = gameMap.getAllGun().stream()
                .map(n -> n.x + "," + n.y)
                .collect(Collectors.toSet());

        int[][] trace = new int[mapSize][mapSize];
        for (int[] row : trace) Arrays.fill(row, -1);
        boolean[][] visited = new boolean[mapSize][mapSize];

        Queue<Node> queue = new LinkedList<>();
        queue.add(player);
        visited[player.x][player.y] = true;

        while (!queue.isEmpty()) {
            Node u = queue.poll();

            String key = u.x + "," + u.y;
            if (gunPositions.contains(key)) {
                // Truy vết đường đi
                StringBuilder path = new StringBuilder();
                int x = u.x, y = u.y;
                while (x != player.x || y != player.y) {
                    int dir = trace[x][y];
                    path.append(directions[dir]);
                    x -= Dx[dir];
                    y -= Dy[dir];
                }
                return path.reverse().toString();
            }

            for (int dir = 0; dir < 4; dir++) {
                int x = u.x + Dx[dir];
                int y = u.y + Dy[dir];
                if (x >= 0 && y >= 0 && x < mapSize && y < mapSize && !visited[x][y] && !blocked[x][y]) {
                    if (skipDarkArea && !checkInsideSafeArea(new Node(x, y), safeZone, mapSize)) {
                        continue;
                    }
                    visited[x][y] = true;
                    trace[x][y] = dir;
                    queue.add(new Node(x, y));
                }
            }
        }

        return null; // Không tìm thấy súng
    }

    public Weapon getNearestGun() {
        return getNearestWeaponFromList(gameMap.getAllGun());
    }

    public Weapon getNearestMelee() {
        return getNearestWeaponFromList(gameMap.getAllMelee());
    }

    public Weapon getNearestThrowable() {
        return getNearestWeaponFromList(gameMap.getAllThrowable());
    }

    public Weapon getNearestWeapon() {
        return getNearestWeaponFromList(gameMap.getListWeapons());
    }

    private Weapon getNearestWeaponFromList(List<Weapon> weapons) {
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

    public Player getNearestPlayer() {
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

    public String getDirectionTo(Player to) {
        int dx = to.getX() - player.getX();
        int dy = to.getY() - player.getY();

        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "r" : "l";
        } else {
            return dy > 0 ? "d" : "u";
        }
    }

    public String getRandomDirection() {
        String[] directions = {"u", "d", "l", "r"};
        return directions[new Random().nextInt(directions.length)];
    }


    public boolean isEnemyShootable(Player enemy) {
        int x1 = player.getX(), y1 = player.getY();
        int x2 = enemy.getX(), y2 = enemy.getY();

        if (x1 == x2) {
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            if (maxY - minY <= 4) {
                for (int y = minY + 1; y < maxY; y++) {
                    if (!isWalkable(x1, y)) return false;
                }
                return true;
            }
        }

        if (y1 == y2) {
            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            if (maxX - minX <= 4) {
                for (int x = minX + 1; x < maxX; x++) {
                    if (!isWalkable(x, y1)) return false;
                }
                return true;
            }
        }

        return false;
    }

    public String getShootDirection(Player enemy) {
        int x1 = player.getX(), y1 = player.getY();
        int x2 = enemy.getX(), y2 = enemy.getY();

        if (x1 == x2) return y2 > y1 ? "u" : "d";
        if (y1 == y2) return x2 > x1 ? "r" : "l";
        return null;
    }

    public boolean isWalkable(int x, int y) {
        int mapSize = gameMap.getMapSize();
        if (x < 0 || y < 0 || x >= mapSize || y >= mapSize) return false;

        // Kiểm tra xem (x, y) có nằm trong danh sách node bị chặn
        for (Node n : nodesToAvoid) {
            if (n.getX() == x && n.getY() == y) {
                return false;
            }
        }

        return true;
    }


}
