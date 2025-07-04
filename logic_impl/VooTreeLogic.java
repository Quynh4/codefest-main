package com.example.logic_impl;

import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.Element;
import jsclub.codefest.sdk.model.ElementType;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.players.Player;

import java.io.IOException;

public class VooTreeLogic extends GameLogicHandler {
    public VooTreeLogic(Hero hero) {
        super(hero);
    }

    @Override
    public void handleTurn() throws IOException {
        if (info.getPlayer() == null || info.getPlayer().getHealth() == 0) {
            System.out.println("[BOT] Player is null or dead. Skipping turn.");
            return;
        }

        GameMap gameMap = info.getGameMap();
        Player player = info.getPlayer();
        Node currentNode = new Node(player.getX(), player.getY());

        System.out.println("[BOT] Current Position: (" + currentNode.x + "," + currentNode.y + ")");
        System.out.println("[BOT] HP: " + player.getHealth());

        if (!info.isInSafeZone()) {
            System.out.println("[BOT] Not in safe zone → Moving to center.");
            moveToCenterNode();
            return;
        }

        // Kiểm tra xem có đồ gần không
        Element nearElement = info.getNearAssetElement(gameMap, hero, currentNode);
        if (nearElement != null) {
            System.out.println("[BOT] Found nearby item: " + nearElement.getType() + " at (" + nearElement.getX() + "," + nearElement.getY() + ")");
            if(nearElement.getType() == ElementType.CHEST)
                hero.attack(info.getDirectionTo(nearElement));
            else{
            // Nếu đứng đúng ô đồ
            if (info.isReach(currentNode, new Node(nearElement.getX(), nearElement.getY()))) {

                    String idToRevoke = info.getInventoryIdIfFullInventory(hero, nearElement);
                    if (idToRevoke != null) {
                        if (nearElement.getType() == ElementType.HEALING_ITEM) {
                            System.out.println("[BOT] Inventory full → Using healing item: " + idToRevoke);
                            hero.useItem(idToRevoke);
                        } else {
                            System.out.println("[BOT] Inventory full → Revoking item: " + idToRevoke);
                            hero.revokeItem(idToRevoke);
                        }
                    } else {
                        System.out.println("[BOT] Picking up item.");
                        hero.pickupItem();
                    }
                    return;
                }
                else {
                // Chưa tới nơi → đi về hướng đó
                String moveDir = info.getDirectionTo(nearElement);
                System.out.println("[BOT] Moving toward item. Direction: " + moveDir);
                hero.move(moveDir);
                return;
            }
            }
        }

        // Nếu có vũ khí → tấn công
        ElementType weapon = info.checkHaveWeapon();
        if (weapon != null) {
            System.out.println("[BOT] Has weapon: " + weapon + " → Searching for enemy.");
            attackPlayer(gameMap, player, currentNode, weapon);
        } else {
            // Nếu không có vũ khí → đi tìm đồ
            System.out.println("[BOT] No weapon found → Searching for assets.");
            findAssests(gameMap, player, currentNode);
        }
    }
}
