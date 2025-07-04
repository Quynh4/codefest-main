package com.example;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.Element;
import jsclub.codefest.sdk.model.ElementType;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.obstacles.ObstacleTag;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static jsclub.codefest.sdk.algorithm.PathUtils.checkInsideSafeArea;

public class GameInfoProvider {
    static List<ElementType> LISTASSETTYPES = new ArrayList<ElementType>() {{
        add(ElementType.CHEST);
        add(ElementType.MELEE);
        add(ElementType.ARMOR);
        add(ElementType.GUN);
        add(ElementType.THROWABLE);
        add(ElementType.BULLET);
        add(ElementType.HEALING_ITEM);

    }};
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
        List<Node> nodes = new ArrayList<>();
        nodes.addAll(
                gameMap.getListObstacles().stream()
                        .filter(obstacle ->
                                obstacle.getType() == ElementType.TRAP ||
                                obstacle.getType() == ElementType.CHEST ||
                                !obstacle.getTags().contains(ObstacleTag.CAN_GO_THROUGH)
                        )
                        .toList()
        );
        nodes.addAll(gameMap.getListEnemies());
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

    public Obstacle getNearestChest() {
        List<Obstacle> chests = gameMap.getListObstacles().stream()
                .filter(e -> e.getType() == ElementType.CHEST)
                .toList();

        return chests.stream()
                .min((a, b) -> Double.compare(PathUtils.distance(player, a), PathUtils.distance(player, b)))
                .orElse(null);
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

    public String getDirectionTo(Node to) {
        int dx = to.getX() - player.getX();
        int dy = to.getY() - player.getY();

        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "r" : "l";
        } else {
            return dy > 0 ? "u" : "d";
        }
    }

    public String getDirectionToAdjacent(Node target) {
        int dx = target.getX() - player.getX();
        int dy = player.getY() - target.getY();

        if (Math.abs(dx) + Math.abs(dy) == 1) {
            if (dx == 1) return "r";
            if (dx == -1) return "l";
            if (dy == 1) return "d";
            if (dy == -1) return "u";
        }
        return null;
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


    public List<Weapon> getWeaponByType(ElementType type) {
        return gameMap.getListWeapons().stream()
                .filter(w -> w.getType() == type)
                .collect(Collectors.toList());
    }

    public boolean isInSafeZone() {
        int mapSize = gameMap.getMapSize();
        int safeZone = gameMap.getSafeZone();
        return checkInsideSafeArea(player, safeZone, mapSize);
    }

    public Element getNearAssetElement(GameMap gameMap, Hero hero, Node currentNode) throws IOException {
        // Lấy tọa độ hiện tại của Hero
        int heroX = currentNode.getX();
        int heroY = currentNode.getY();

        // Các tọa độ xung quanh Hero cách 1 ô
        int[][] directions = {
                {0,0},//chính ô hero
                {0, 1},  // Phía trên (North)
                {0, -1}, // Phía dưới (South)
                {1, 0},  // Phía phải (East)
                {-1, 0}// Phía trái (West)

        };
        // Duyệt qua các ô xung quanh hero
        for (int[] direction : directions) {
            int newX = heroX + direction[0];
            int newY = heroY + direction[1];
            // Lấy Element ở tọa độ (newX, newY)
            Element element = gameMap.getElementByIndex(newX, newY);
            // Kiểm tra nếu element tồn tại và thuộc loại trong LISTASSETTYPES
            if (element != null && LISTASSETTYPES.contains(element.getType())
                    && checkPlayerInNode(newX,newY) ==null) {
                System.out.println("Found item " + element.getId());
                return element;  // Trả về element nếu thỏa mãn điều kiện
            }
        }
        // Nếu không tìm thấy, trả về null
        return null;
    }

    public Player checkPlayerInNode(int x,int y){
        List<Player> listPlayer= gameMap.getOtherPlayerInfo();
        for(Player p: listPlayer){
            if(p.x==x && p.y==y) return p;
        }
        return null;
    }

    public String getInventoryIdIfFullInventory(Hero hero, Element nearAssetElement) {
        // Kiểm tra loại của nearAssetElement và xác định xem kho có bị đầy với loại đó không
        System.out.println(nearAssetElement.getType());
        switch (nearAssetElement.getType()) {
            case MELEE:
                System.out.println("MELEE "+hero.getInventory().getMelee());
                // Kiểm tra xem đã có vũ khí cận chiến chưa
                if (hero.getInventory().getMelee() != null && !Objects.equals(hero.getInventory().getMelee().getId(), "HAND")) {
                    return hero.getInventory().getMelee().getId(); // Nếu đã có, trả về ID của nearAssetElement
                }
                break;

            case GUN:
                System.out.println("GUN "+hero.getInventory().getGun());
                // Kiểm tra xem đã có vũ khí xạ chiến chưa
                if (hero.getInventory().getGun() != null) {
                    return hero.getInventory().getGun().getId();
                }
                break;

            case THROWABLE:
                System.out.println("THROWABLE "+hero.getInventory().getThrowable());
                // Kiểm tra xem đã có vũ khí ném chưa
                if (hero.getInventory().getThrowable() != null) {
                    return hero.getInventory().getThrowable().getId();
                }
                break;

            case HEALING_ITEM:
                // Kiểm tra xem đã có đủ 4 vật phẩm hồi máu chưa
                System.out.println("HEALING ITEM "+hero.getInventory().getListSupportItem());
                if (hero.getInventory().getListSupportItem().size() >= 4) {
                    return hero.getInventory().getListSupportItem().get(0).getId();
                }
                break;

            case ARMOR:
                // Kiểm tra loại giáp: VEST -> BODY, POT hoặc HELMET -> HEAD
                String armorId = nearAssetElement.getId();
                if (hero.getInventory().getArmor() != null) {
                    return hero.getInventory().getArmor().getId();
                }
                break;
        }

        return null; // Nếu chưa đầy với loại này, trả về null
    }

    public boolean isReach(Node x, Node y) {
        return x.x == y.x && x.y == y.y;
    }

    public Element getNearElement(Node currentNode, GameMap gameMap, ElementType elementType, int distance) {
        // Lấy tọa độ hiện tại của node
        int currentX = currentNode.getX();
        int currentY = currentNode.getY();
        if(elementType==ElementType.PLAYER){
            for (int i = 1; i <= distance; i++) {
                if(checkPlayerInNode(currentX + i, currentY)!=null) return checkPlayerInNode(currentX + i, currentY);
                if(checkPlayerInNode(currentX - i, currentY)!=null) return checkPlayerInNode(currentX - i, currentY);
                if(checkPlayerInNode(currentX, currentY + i)!=null) return checkPlayerInNode(currentX, currentY + i);
                if(checkPlayerInNode(currentX, currentY - i)!=null) return checkPlayerInNode(currentX, currentY - i);
            }
            return null;
        }
        // Duyệt qua các ô trong khoảng cách xác định chỉ theo hàng hoặc cột
        for (int i = 1; i <= distance; i++) {
            // Kiểm tra các ô theo cùng hàng (theo trục X)
            // Tọa độ hàng giữ nguyên, chỉ thay đổi cột (X +/- i)
            Element elementAtRight = gameMap.getElementByIndex(currentX + i, currentY);
            if (elementAtRight.getType() == elementType &&  isInSafeZone()) {
                return elementAtRight;
            }
            Element elementAtLeft = gameMap.getElementByIndex(currentX - i, currentY);
            if (elementAtLeft.getType() == elementType && isInSafeZone()) {
                return elementAtLeft;
            }
            // Kiểm tra các ô theo cùng cột (theo trục Y)
            // Tọa độ cột giữ nguyên, chỉ thay đổi hàng (Y +/- i)
            Element elementAtUp = gameMap.getElementByIndex(currentX, currentY + i);
            if (elementAtUp.getType() == elementType && isInSafeZone()) {
                return elementAtUp;
            }
            Element elementAtDown = gameMap.getElementByIndex(currentX, currentY - i);
            if (elementAtDown.getType() == elementType && isInSafeZone()) {
                return elementAtDown;
            }
        }
        // Nếu không tìm thấy phần tử, log và trả về false
        System.out.println("No matching element found near the current node.");
        return null;
    }

    public Player findPlayer(Element element) {
        for (Player p : gameMap.getOtherPlayerInfo()) {
            if (p.getX() == element.getX() && p.getY() == element.getY()) return p;
        }
        return null;
    }

    public Node getNearestAsset(List<Node> assets, Node currentNode, List<Node> restrictedNodes) {
        double minDistance = Double.MAX_VALUE;
        Node nearest = null;

        for (Node node : assets) {
            if (node.x == player.x && node.y == player.y) continue;

            String path = PathUtils.getShortestPath(gameMap, restrictedNodes, currentNode, node, false);
            if (path != null && path.length() < minDistance) {
                minDistance = path.length();
                nearest = node;
            }
        }
        return nearest;
    }

    public List<Node> getListAssets() {
        List<Node> list = new ArrayList<>();
        list.addAll(gameMap.getAllGun());
        list.addAll(gameMap.getAllMelee());
        list.addAll(gameMap.getAllThrowable());
        list.addAll(gameMap.getListArmors());
        list.addAll(gameMap.getListSupportItems());
        list.addAll(gameMap.getListObstacles().stream()
                .filter(e -> e.getType() == ElementType.CHEST)
                .toList());
        return list;
    }

    public void getChestItems(Node chest, Node currentNode) throws IOException {
        hero.attack(getRelativeDirection(chest, currentNode));
    }

    public String getRelativeDirection(Node target, Node current) {
        int dx = target.x - current.x;
        int dy = target.y - current.y;

        if (dx == 1 && dy == 0) return "r";
        if (dx == -1 && dy == 0) return "l";
        if (dx == 0 && dy == 1) return "u";
        if (dx == 0 && dy == -1) return "d";
        return "";
    }

    public ElementType checkHaveWeapon(){
        if(!hero.getInventory().getMelee().getId().equals("HAND")) return ElementType.MELEE;
        if(hero.getInventory().getGun()!=null) return ElementType.GUN;
        if(hero.getInventory().getThrowable()!=null) return ElementType.THROWABLE;
        return null;
    }

}
