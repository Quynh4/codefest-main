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
        System.out.println("Inventory: "+ hero.getInventory());
        int x = info.getPlayer().getX();
        int y = info.getPlayer().getY();
        // Weapon: chỉ nhặt nếu không trùng class với vũ khí đã có
        for (Weapon w : info.getGameMap().getListWeapons()) {
            if (w.getX() == x && w.getY() == y) {
//                boolean isDuplicate = false;
                if (hero.getInventory().getGun() != null && hero.getInventory().getGun().getId().equals(w.getId())) {
//                    isDuplicate = true;

                    hero.revokeItem(hero.getInventory().getGun().getId());
                    System.out.println("[LOG] Duplicate weapon found, skipping: " + hero.getInventory().getGun().getId());
                }
                if (hero.getInventory().getMelee() != null && hero.getInventory().getMelee().getId().equals(w.getId())) {

                    hero.revokeItem(hero.getInventory().getMelee().getId());
                    System.out.println("[LOG] Duplicate weapon found, skipping: " + hero.getInventory().getMelee().getId());
                }
                if (hero.getInventory().getThrowable() != null && hero.getInventory().getThrowable().getId().equals(w.getId())) {
                    hero.throwItem(info.getRandomDirection(),hero.getInventory().getThrowable().getRange());
                    System.out.println("[LOG] Duplicate weapon found, skipping: " + hero.getInventory().getThrowable().getId());
                }
                if (hero.getInventory().getSpecial() != null && hero.getInventory().getSpecial().getId().equals(w.getId())) {
                    hero.revokeItem(hero.getInventory().getSpecial().getId());
                    System.out.println("[LOG] Duplicate weapon found, skipping: " + w.getId());
                }
                System.out.println("[LOG] Picking up new weapon: " + w.getId());
                hero.pickupItem();

                return;
            }
        }

        // HealingItem: luôn nhặt
        for (HealingItem h : info.getGameMap().getListHealingItems()) {
            if (h.getX() == x && h.getY() == y) {
                System.out.println("[LOG] Found healing item at current position: " + h);
                hero.pickupItem();
                return;
            }
        }

        // Armor: chỉ nhặt nếu không trùng class với giáp đã có
        for (Armor a : info.getGameMap().getListArmors()) {
            if (a.getX() == x && a.getY() == y) {
                boolean isDuplicate = false;
                if (hero.getInventory().getArmor() != null && hero.getInventory().getArmor().getId().equals(a.getId())) {
                    isDuplicate = true;
                }
                if (hero.getInventory().getHelmet() != null && hero.getInventory().getHelmet().getId().equals(a.getId())) {
                    isDuplicate = true;
                }

                if (!isDuplicate) {
                    System.out.println("[LOG] Picking up new armor: " + a);
                    hero.pickupItem();
                } else {
                    System.out.println("[LOG] Duplicate armor found, skipping: " + a);
                }
                return;
            }
        }

        // Không có item tại vị trí → đi tìm rương
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
