package com.example.logic_impl;

import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.Element;
import jsclub.codefest.sdk.model.ElementType;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.support_items.SupportItem;

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
        Player player = info.getPlayer();
        Node currentNode = new Node(player.getX(), player.getY());

        System.out.println("[BOT] Current Position: (" + currentNode.x + "," + currentNode.y + ")");
        System.out.println("[BOT] HP: " + player.getHealth());

        if (player.getHealth() < 60 && hero.getInventory().getListSupportItem() != null
                && !hero.getInventory().getListSupportItem().isEmpty()) {
            SupportItem supportItem = hero.getInventory().getListSupportItem().getFirst();
            System.out.println("[BOT] Use support item" + supportItem.getId());
            hero.useItem(supportItem.getId());
            return;
        }

        if (!info.isInSafeZone()) {
            System.out.println("[BOT] Not in safe zone → Moving to center.");
            moveToCenterNode();
            return;
        }

        // Kiểm tra xem có đồ gần không
        Element nearElement = info.getNearAssetElement();
        if (nearElement != null) handleNearElement();

        // Nếu có vũ khí → tấn công
        ElementType weapon = info.checkHaveWeapon();
        if (weapon != null) {
            System.out.println("[BOT] Has weapon: " + weapon + " → Searching for enemy.");
            findNAttackPlayerORChest(weapon);
        } else {
            // Nếu không có vũ khí → đi tìm đồ
            System.out.println("[BOT] No weapon found → Searching for assets.");
            findAssests();
        }
    }
}
