package com.example.logic_impl;

import com.example.GameInfoProvider;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.Element;
import jsclub.codefest.sdk.model.ElementType;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public abstract class GameLogicHandler {
    protected final Hero hero;
    protected final GameInfoProvider info;
    protected final GameMap gameMap;

    public GameLogicHandler(Hero hero) {
        this.hero = hero;
        this.info = new GameInfoProvider(hero);
        this.gameMap = info.getGameMap();
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
            hero.throwItem(direction);
        } else {
            System.out.println("No weapon equipped!");
        }
    }

    protected void moveToOrPickup(Weapon weapon) throws IOException {
        if (weapon == null) return;

        String path = info.getShortestPathTo(weapon);

        if (path != null) {
            if (path.isEmpty()) {
                hero.pickupItem();
            } else {
                hero.move(path);
            }
        }
    }

    public void moveToCenterNode() throws IOException {
        int mapSize = info.getGameMap().getMapSize();
        boolean[][] visited = new boolean[mapSize][mapSize];

        Queue<Node> queue = new LinkedList<>();
        queue.add(new Node(mapSize / 2, mapSize / 2));

        while (!queue.isEmpty()) {
            Node target = queue.poll();
            if (target.x < 0 || target.y < 0 || target.x >= mapSize || target.y >= mapSize) continue;
            if (visited[target.x][target.y]) continue;
            visited[target.x][target.y] = true;

            String path = info.getShortestPathTo(target);
            if (path != null) {
                hero.move(path);
                return;
            }

            // Thêm 4 hướng để duyệt dần ra xa tâm
            queue.add(new Node(target.x + 1, target.y));
            queue.add(new Node(target.x - 1, target.y));
            queue.add(new Node(target.x, target.y + 1));
            queue.add(new Node(target.x, target.y - 1));
        }

        System.out.println("No path to any central node found.");
    }
//
//    public void attackPlayer(Player player, Node currentNode, ElementType elementType) throws IOException {
//        Player nearestPlayer = info.getNearestPlayer();
//        if (nearestPlayer == null) {
//            findAssests();
//            return;
//        }
//
//        int range = switch (elementType) {
//            case THROWABLE -> 6;
//            case GUN -> 4;
//            default -> 1;
//        };
//
//        Element nearPlayerNode = info.getNearElement(ElementType.PLAYER, range);
//
//        if (nearPlayerNode != null) {
//            Player nearPlayer = info.findPlayer(nearPlayerNode);
//            if (nearPlayer != null && nearPlayer.getHealth() > 0) {
//                String dir = info.getDirectionTo(nearPlayerNode);
//                switch (elementType) {
//                    case THROWABLE -> hero.throwItem(dir);
//                    case MELEE -> hero.attack(dir);
//                    case GUN -> hero.shoot(dir);
//                }
//                return;
//            }
//        }
//
//        String path = PathUtils.getShortestPath(gameMap, info.getNodesToAvoid(), currentNode, nearestPlayer, false);
//        if (path != null) {
//            hero.move(path);
//        } else {
//            findAssests();
//        }
//    }

//    public void findNAttackPlayer(ElementType elementType) throws IOException {
//        Player nearestPlayer = info.getNearestPlayer();
//        if (nearestPlayer == null) {
//            findAssests();
//            return;
//        }
//
//        int[] rangeInfo = getWeaponRangeByType(elementType);
//
//        int effectiveDistance = rangeInfo[1]; // khoảng cách theo hướng đánh
//
//        Element nearPlayerNode = info.getNearElement(ElementType.PLAYER, effectiveDistance);
//
//        if (nearPlayerNode != null) {
//            Player nearPlayer = info.findPlayer(nearPlayerNode);
//            if (nearPlayer != null && nearPlayer.getHealth() > 0) {
//                String dir = info.getDirectionTo(nearPlayerNode);
//                switch (elementType) {
//                    case THROWABLE -> hero.throwItem(dir);
//                    case MELEE -> hero.attack(dir);
//                    case GUN -> hero.shoot(dir);
//                }
//                return;
//            }
//        }
//
//        // Nếu không tấn công được thì di chuyển đến gần người chơi khác
//        String path = info.getShortestPathTo(nearestPlayer);
//        if (path != null) {
//            hero.move(path);
//        } else {
//            findAssests();
//        }
//    }


    public void findNAttackPlayerORChest(ElementType elementType) throws IOException {
        Node nearestPlayerORChest = info.getNearestPlayerORChest();
        if (nearestPlayerORChest == null) {
            findAssests();
            return;
        }

        int[] rangeInfo = getWeaponRangeByType(elementType);

        int effectiveDistance = rangeInfo[1]; // khoảng cách theo hướng đánh

        Element nearPlayerORChestNode = info.getNearElementByListType(
                List.of(ElementType.PLAYER, ElementType.CHEST),
                effectiveDistance);

        if (nearPlayerORChestNode != null) {
            String dir = info.getDirectionTo(nearPlayerORChestNode);
            switch (elementType) {
                case THROWABLE -> hero.throwItem(dir);
                case MELEE -> hero.attack(dir);
                case GUN -> hero.shoot(dir);
            }
            return;
        }

        // Nếu không tấn công được thì di chuyển đến gần người chơi khác
        String path = info.getShortestPathTo(nearestPlayerORChest);
        if (path != null) {
            hero.move(path);
        } else {
            findAssests();
        }
    }

    public void findAssests() throws IOException {
        List<Node> listAssets = info.getListAssets();

        List<Node> restrictedNodes = new ArrayList<>();
        for (Enemy e : gameMap.getListEnemies()) restrictedNodes.add(new Node(e.x, e.y));
        restrictedNodes.addAll(gameMap.getOtherPlayerInfo());

        Node nearestAsset = info.getNearestAsset(listAssets, info.getPlayer(), restrictedNodes);
        if (nearestAsset == null) {
            System.out.println("[BOT] Không có asset nào xung quanh.");
            return;
        }

        Element targetElement = gameMap.getElementByIndex(nearestAsset.x, nearestAsset.y);
        if (targetElement == null) {
            System.out.println("[BOT] Không tìm thấy element tại vị trí asset.");
            return;
        }

        if (targetElement.getType() == ElementType.CHEST) {
            System.out.println("[BOT] Phát hiện chest gần nhất tại: (" + nearestAsset.x + "," + nearestAsset.y + ")");

            Node nearChest = info.getNearElement(ElementType.CHEST, 1);

            // Nếu chest kề bên → tấn công
            if (nearChest != null) {
                String direction = info.getDirectionToAdjacent(nearChest);
                if (direction != null) {
                    System.out.println("[BOT] Chest ở cạnh → Đập chest hướng: " + direction);
                    hero.attack(direction);
                    return;
                }
            }

            // Nếu đứng đúng ô chứa chest → nhặt
            if (info.isReach(nearestAsset)) {
                System.out.println("[BOT] Đứng tại ô chest → Đập chest");
                hero.attack(info.getRelativeDirection(nearestAsset));
                return;
            }

            // Nếu ở xa chest → di chuyển tới
            String path = info.getShortestPathTo(nearestAsset);
            if (path != null) {
                System.out.println("[BOT] Di chuyển đến chest. Path: " + path);
                hero.move(path);
            } else {
                System.out.println("[BOT] Không tìm được đường đến chest.");
            }

        } else {
            // Nếu là đồ bình thường
            if (info.isReach(nearestAsset)) {
                System.out.println("[BOT] Đứng tại vị trí asset → Nhặt đồ");
                hero.pickupItem();
            } else {
                String path = info.getShortestPathTo(nearestAsset);
                if (path != null) {
                    System.out.println("[BOT] Di chuyển đến asset. Path: " + path);
                    hero.move(path);
                } else {
                    System.out.println("[BOT] Không tìm được đường đến asset.");
                }
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

        String path = info.getShortestPathTo(enemy);

        if (path != null) {
            hero.move(path);
            performAttack(path, 1);
        } else {
            hero.move(info.getRandomDirection());
        }
    }


/**
 * Kiểm tra và xử lý item gần người chơi.
 * <p>
 * Nếu là chest thì tấn công nó.
 * Nếu đứng trên item:
 *   - Nếu kho đồ đầy, sử dụng hoặc loại bỏ item cũ.
 *   - Nếu chưa đầy, nhặt item.
 * Nếu chưa đứng trên item thì di chuyển về hướng item.
 */
    public void handleNearElement() throws IOException {
        Element nearElement = info.getNearAssetElement();
        if (nearElement != null) {
            log("[BOT] Found nearby item: %s at (%d,%d)", nearElement.getType(), nearElement.getX(), nearElement.getY());

            if (nearElement.getType() == ElementType.CHEST) {
                String direction = info.getDirectionTo(new Node(nearElement.getX(), nearElement.getY()));
                hero.attack(direction);
                return;
            }

            Node assetNode = new Node(nearElement.getX(), nearElement.getY());
            if (info.isReach(assetNode)) {
                String idToRevoke = info.getInventoryIdIfFullInventory(hero, nearElement);
                if (idToRevoke != null) {
                    if (nearElement.getType() == ElementType.HEALING_ITEM) {
                        log("[BOT] Inventory full → Using healing item: %s", idToRevoke);
                        hero.useItem(idToRevoke);
                    } else {
                        log("[BOT] Inventory full → Revoking item: %s", idToRevoke);
                        hero.revokeItem(idToRevoke);
                    }
                } else {
                    log("[BOT] Picking up item.");
                    hero.pickupItem();
                }
            } else {
                String moveDir = info.getDirectionTo(assetNode);
                log("[BOT] Moving toward item. Direction: %s", moveDir);
                hero.move(moveDir);
            }
        }
    }


    public int[] getWeaponRangeByType(ElementType elementType) {
        return switch (elementType) {
            case GUN -> hero.getInventory().getGun().getRange();
            case MELEE -> hero.getInventory().getMelee().getRange();
            case THROWABLE -> hero.getInventory().getThrowable().getRange();
            case SPECIAL -> hero.getInventory().getThrowable().getRange();
            default -> new int[]{0, 0}; // không có vũ khí, hoặc không tấn công được
        };
    }


    protected void log(String message, Object... args) {
        System.out.printf((message) + "%n", args);
    }

}
