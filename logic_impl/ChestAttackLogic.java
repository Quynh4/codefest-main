package com.example.logic_impl;

import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.model.armors.Armor;
import jsclub.codefest.sdk.model.healing_items.HealingItem;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.List;

public class ChestAttackLogic extends GameLogicHandler {
    public ChestAttackLogic(Hero hero) {
        super(hero);
    }

    @Override
    public void handleTurn() throws IOException {
        int x = info.getPlayer().getX();
        int y = info.getPlayer().getY();

        // Ưu tiên pickUp nếu có vật phẩm tại vị trí hiện tại
        for (Weapon w : info.getGameMap().getListWeapons()) {
            if (w.getX() == x && w.getY() == y) {
                System.out.println("[LOG] Found weapon at current position: " + w);
                hero.pickupItem(); // chỉ gọi pickUp() duy nhất
                return;
            }
        }
        for (HealingItem h : info.getGameMap().getListHealingItems()) {
            if (h.getX() == x && h.getY() == y) {
                System.out.println("[LOG] Found healing item at current position: " + h);
                hero.pickupItem();
                return;
            }
        }
        for (Armor a : info.getGameMap().getListArmors()) {
            if (a.getX() == x && a.getY() == y) {
                System.out.println("[LOG] Found armor at current position: " + a);
                hero.pickupItem();
                return;
            }
        }

        // Không có item, thì tiếp tục đi tìm rương
        Obstacle nearestChest = info.getNearestChest();
        if (nearestChest == null) {
            String randomDir = info.getRandomDirection();
            System.out.println("[LOG] No chest found. Moving randomly to: " + randomDir);
            hero.move(randomDir);
            return;
        }

        String direction = info.getDirectionToAdjacent(nearestChest);
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
