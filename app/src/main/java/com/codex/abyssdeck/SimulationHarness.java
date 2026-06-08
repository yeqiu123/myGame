package com.codex.abyssdeck;

import java.util.ArrayList;

public final class SimulationHarness {
    private SimulationHarness() {
    }

    public static void main(String[] args) {
        int[] depths = {0, 3, 6, 10};
        for (int depth : depths) {
            Result result = runBatch(depth, 40);
            System.out.println("depth=" + depth + " runs=" + result.runs + " wins=" + result.wins
                    + " losses=" + result.losses + " stalled=" + result.stalled + " avgFloor=" + (result.floorSum / Math.max(1, result.runs)));
        }
        System.out.println("professions=" + GameCore.PROFESSIONS.length + " cards=" + GameCore.CARD_LIBRARY.size()
                + " relics=" + GameCore.RELIC_LIBRARY.size() + " potions=" + GameCore.POTION_LIBRARY.size());
    }

    private static Result runBatch(int depth, int count) {
        Result r = new Result();
        for (int i = 0; i < count; i++) {
            GameCore.State s = GameCore.fresh();
            GameCore.chooseDepth(s, depth);
            String[] origins = {GameCore.ORIGIN_STEEL, GameCore.ORIGIN_ASH, GameCore.ORIGIN_WILD, GameCore.ORIGIN_VOID};
            GameCore.chooseOrigin(s, origins[i % origins.length]);
            GameCore.chooseProfession(s, GameCore.PROFESSIONS[i % GameCore.PROFESSIONS.length]);
            int guard = 0;
            while (s.mode != GameCore.MODE_GAME_OVER && s.mode != GameCore.MODE_VICTORY && guard++ < 900) {
                step(s);
            }
            r.runs++;
            r.floorSum += (s.act - 1) * 12 + s.floor;
            if (s.mode == GameCore.MODE_VICTORY) r.wins++;
            else if (s.mode == GameCore.MODE_GAME_OVER) r.losses++;
            else r.stalled++;
        }
        return r;
    }

    private static void step(GameCore.State s) {
        s.ensureRandom();
        if (s.mode == GameCore.MODE_BOON) {
            GameCore.chooseBoon(s, 0);
        } else if (s.mode == GameCore.MODE_MAP) {
            for (int i = 0; i < s.map.size(); i++) {
                if (s.map.get(i).available) {
                    GameCore.mapChoose(s, i);
                    return;
                }
            }
        } else if (s.mode == GameCore.MODE_COMBAT) {
            combat(s);
        } else if (s.mode == GameCore.MODE_REWARD) {
            if (!s.relicRewards.isEmpty()) {
                GameCore.pickRelicReward(s, 0);
            } else if (!s.cardRewards.isEmpty()) {
                GameCore.pickRewardCard(s, 0);
            } else {
                GameCore.skipReward(s);
            }
        } else if (s.mode == GameCore.MODE_TALENT) {
            GameCore.chooseTalent(s, 0);
        } else if (s.mode == GameCore.MODE_SHOP) {
            if (!s.shopRelics.isEmpty() && s.gold >= GameCore.shopRelicPrice(s)) {
                GameCore.shopBuyRelic(s, 0);
            } else if (!s.shopCards.isEmpty() && s.gold >= GameCore.shopCardPrice(s, GameCore.card(s.shopCards.get(0)))) {
                GameCore.shopBuyCard(s, 0);
            } else {
                GameCore.leaveShop(s);
            }
        } else if (s.mode == GameCore.MODE_REST) {
            if (s.hp < s.maxHp * 0.55f) {
                GameCore.restHeal(s);
            } else if (hasUpgradableCard(s)) {
                GameCore.restChoose(s, "rest_upgrade");
                upgradeFirst(s);
            } else {
                GameCore.restHeal(s);
            }
        } else if (s.mode == GameCore.MODE_EVENT) {
            GameCore.eventChoose(s, 0);
        } else if (s.mode == GameCore.MODE_DECK) {
            if ("event_remove_hp".equals(s.pendingAction)) {
                GameCore.deckPick(s, 0);
            } else {
                upgradeFirst(s);
            }
        }
    }

    private static void upgradeFirst(GameCore.State s) {
        for (int i = 0; i < s.deck.size(); i++) {
            if (!s.deck.get(i).upgraded) {
                GameCore.deckPick(s, i);
                return;
            }
        }
        GameCore.closePanel(s);
    }

    private static boolean hasUpgradableCard(GameCore.State s) {
        for (GameCore.Card c : s.deck) {
            GameCore.CardDef d = GameCore.card(c.id);
            if (d != null && d.type != 3 && !c.upgraded) {
                return true;
            }
        }
        return false;
    }

    private static void combat(GameCore.State s) {
        int safety = 0;
        while (s.mode == GameCore.MODE_COMBAT && s.playerTurn && safety++ < 20) {
            int best = -1;
            int bestScore = -9999;
            int target = firstEnemy(s);
            for (int i = 0; i < s.hand.size(); i++) {
                GameCore.Card c = s.hand.get(i);
                GameCore.CardDef d = GameCore.card(c.id);
                if (d == null || GameCore.costOf(s, c, d) > s.energy) continue;
                int score = GameCore.cardDamage(c) * 3 + GameCore.cardBlock(c) * 2 + d.draw * 4 + d.energyGain * 7
                        + d.burn * 4 + d.bind * 3 + d.gainSteelEngine * 12 + d.gainAshEngine * 12
                        + d.gainWildEngine * 12 + d.gainVoidEngine * 12 + d.heal * 4 + d.scry * 2
                        + (d.upgradeRandom ? 8 : 0) + (d.createEcho ? 6 : 0) + (d.createWound ? 4 : 0);
                if (GameCore.PROF_BLOODBOUND.equals(s.profession) && (d.hpLoss > 0 || "wound".equals(c.id))) {
                    score += 14;
                }
                if (GameCore.PROF_WEAVER.equals(s.profession) && (d.scry > 0 || d.upgradeRandom || d.draw > 0)) {
                    score += 10;
                }
                if (d.targetEnemy && target < 0) continue;
                if (score > bestScore) {
                    bestScore = score;
                    best = i;
                }
            }
            if (best >= 0) {
                GameCore.playCard(s, best, target);
            } else {
                GameCore.endTurn(s);
            }
        }
        if (s.mode == GameCore.MODE_COMBAT && s.playerTurn) {
            GameCore.endTurn(s);
        }
    }

    private static int firstEnemy(GameCore.State s) {
        for (int i = 0; i < s.enemies.size(); i++) {
            if (s.enemies.get(i).hp > 0) return i;
        }
        return -1;
    }

    private static final class Result {
        int runs;
        int wins;
        int losses;
        int stalled;
        int floorSum;
    }
}
