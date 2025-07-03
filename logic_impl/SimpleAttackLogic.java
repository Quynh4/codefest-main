package com.example.logic_impl;

import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;

import java.io.IOException;

public class SimpleAttackLogic extends GameLogicHandler {

    public SimpleAttackLogic(Hero hero) {
        super(hero);
    }

    @Override
    public void handleTurn() throws IOException {
        if (info.getPlayer() == null || info.getPlayer().getHealth() == 0) return;

        if (hero.getInventory().getGun() == null && hero.getInventory().getMelee() == null && hero.getInventory().getThrowable() == null) {
            System.out.println("Searching for weapon...");
            Weapon nearest = info.getNearestGun();
            moveToOrPickup(nearest);
        } else {
            Player enemy = info.getNearestPlayer();
            System.out.println("Enemy found. Engaging...");
            approachAndAttack(enemy);
        }
    }
}
