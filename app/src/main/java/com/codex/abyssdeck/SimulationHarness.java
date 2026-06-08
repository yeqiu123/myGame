package com.codex.abyssdeck;

import java.util.ArrayList;

public final class SimulationHarness {
    private SimulationHarness() {
    }

    public static void main(String[] args) {
        int[] depths = {0, 3, 6, 10};
        for (int depth : depths) {
            Result result = runBatch(depth, 40);
            System.out.println("depth=" + depth + "(" + depthLabel(depth) + ") runs=" + result.runs + " wins=" + result.wins
                    + " losses=" + result.losses + " stalled=" + result.stalled + " avgFloor=" + (result.floorSum / Math.max(1, result.runs))
                    + " routeNodes=" + result.routeNodes);
        }
        System.out.println("professions=" + GameCore.PROFESSIONS.length + " cards=" + GameCore.CARD_LIBRARY.size()
                + " relics=" + GameCore.RELIC_LIBRARY.size() + " potions=" + GameCore.POTION_LIBRARY.size()
                + " talents=" + GameCore.TALENT_LIBRARY.size() + " pacts=" + GameCore.PACT_LIBRARY.size());
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
                step(s, r);
            }
            r.runs++;
            r.floorSum += (s.act - 1) * 12 + s.floor;
            if (s.mode == GameCore.MODE_VICTORY) r.wins++;
            else if (s.mode == GameCore.MODE_GAME_OVER) r.losses++;
            else r.stalled++;
        }
        return r;
    }

    private static void step(GameCore.State s, Result r) {
        s.ensureRandom();
        if (s.mode == GameCore.MODE_BOON) {
            GameCore.chooseBoon(s, chooseBoon(s));
        } else if (s.mode == GameCore.MODE_PACT) {
            GameCore.choosePact(s, choosePact(s));
        } else if (s.mode == GameCore.MODE_MAP) {
            int pick = chooseMapNode(s);
            if (pick >= 0) {
                if (s.map.get(pick).route != GameCore.ROUTE_NONE) {
                    r.routeNodes++;
                }
                GameCore.mapChoose(s, pick);
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
            GameCore.eventChoose(s, s.eventId % 2);
        } else if (s.mode == GameCore.MODE_DECK) {
            if ("event_remove_hp".equals(s.pendingAction) || "event_transform_bonus".equals(s.pendingAction)) {
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

    private static int chooseMapNode(GameCore.State s) {
        int best = -1;
        int bestScore = -9999;
        for (int i = 0; i < s.map.size(); i++) {
            GameCore.MapNode n = s.map.get(i);
            boolean reachable = n.available || (s.relics.contains("night_map") && n.floor == s.floor + 1);
            if (!reachable) continue;
            int score = nodeScore(s, n);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private static int chooseBoon(GameCore.State s) {
        int best = 0;
        int bestScore = -9999;
        for (int i = 0; i < s.boonChoices.size(); i++) {
            String id = s.boonChoices.get(i);
            int score = 0;
            if ("skill_seed".equals(id)) score += 42;
            else if ("profession_pack".equals(id)) score += 36;
            else if ("relic".equals(id)) score += 30;
            else if ("rare_relic_risk".equals(id)) score += 34;
            else if ("thin_start".equals(id)) score += 30;
            else if ("forge_start".equals(id)) score += 28;
            else if ("upgrade".equals(id)) score += 24;
            else if ("remove".equals(id)) score += 22;
            else if ("rare".equals(id)) score += 24;
            else if ("route_cache".equals(id)) score += 20;
            else if ("gold".equals(id)) score += 18;
            else if ("maxhp".equals(id)) score += 16;
            else if ("potion".equals(id)) score += 14;
            else if ("risk".equals(id)) score += 16;
            else if ("brew_start".equals(id)) score += 16;
            else if ("blood_start".equals(id)) score += 12;
            if (GameCore.PROF_ALCHEMIST.equals(s.profession) && ("brew_start".equals(id) || "potion".equals(id))) score += 18;
            if (GameCore.PROF_BLOODBOUND.equals(s.profession) && "blood_start".equals(id)) score += 28;
            if (GameCore.PROF_MERCHANT.equals(s.profession) && ("gold".equals(id) || "route_cache".equals(id))) score += 8;
            if (s.ascension >= 6 && ("risk".equals(id) || "rare_relic_risk".equals(id) || "blood_start".equals(id))) score -= 8;
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private static int choosePact(GameCore.State s) {
        int best = 0;
        int bestScore = -9999;
        for (int i = 0; i < s.pactChoices.size(); i++) {
            String id = s.pactChoices.get(i);
            int score = 0;
            if ("pact_guardian".equals(id)) score += GameCore.PROF_WARDEN.equals(s.profession) ? 34 : 18;
            else if ("pact_sprinter".equals(id)) score += GameCore.PROF_DUELIST.equals(s.profession) || GameCore.PROF_WEAVER.equals(s.profession) ? 34 : 18;
            else if ("pact_brewer".equals(id)) score += GameCore.PROF_ALCHEMIST.equals(s.profession) ? 38 : 16;
            else if ("pact_hunter".equals(id)) score += GameCore.PROF_RANGER.equals(s.profession) ? 32 : 20;
            else if ("pact_void".equals(id)) score += GameCore.PROF_ARCANIST.equals(s.profession) || GameCore.PROF_WEAVER.equals(s.profession) ? 36 : 16;
            else if ("pact_blood".equals(id)) score += GameCore.PROF_BLOODBOUND.equals(s.profession) ? 40 : 10;
            if (GameCore.PROF_SUMMONER.equals(s.profession) && ("pact_void".equals(id) || "pact_hunter".equals(id))) score += 18;
            if (GameCore.PROF_HEXER.equals(s.profession) && ("pact_blood".equals(id) || "pact_void".equals(id))) score += 18;
            if (s.ascension >= 6 && "pact_blood".equals(id) && !GameCore.PROF_BLOODBOUND.equals(s.profession)) score -= 8;
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private static int nodeScore(GameCore.State s, GameCore.MapNode n) {
        int score = 0;
        if (n.type == 'E') score += s.hp > s.maxHp * 0.45f ? 28 : 8;
        else if (n.type == 'C') score += 16;
        else if (n.type == '?') score += 10;
        else if (n.type == '$') score += s.gold >= 90 ? 18 : 6;
        else if (n.type == 'R') score += s.hp < s.maxHp * 0.65f ? 30 : hasUpgradableCard(s) ? 16 : 4;
        else if (n.type == 'B') score += 100;
        if (n.route == GameCore.ROUTE_SUPPLY) score += s.hp < s.maxHp * 0.75f ? 22 : 8;
        else if (n.route == GameCore.ROUTE_RICH) score += 14;
        else if (n.route == GameCore.ROUTE_SECRET) score += 12;
        else if (n.route == GameCore.ROUTE_FORGE) score += hasUpgradableCard(s) ? 16 : 8;
        else if (n.route == GameCore.ROUTE_DANGER) score += s.hp > s.maxHp * 0.55f ? 14 : -8;
        else if (n.route == GameCore.ROUTE_AMBUSH) score += s.hp > s.maxHp * 0.6f ? 10 : -12;
        return score;
    }

    private static String depthLabel(int depth) {
        if (depth >= 10) return "lightless";
        if (depth >= 6) return "nightmare";
        if (depth >= 3) return "tide";
        return "shallow";
    }

    private static void combat(GameCore.State s) {
        int safety = 0;
        while (s.mode == GameCore.MODE_COMBAT && s.playerTurn && safety++ < 20) {
            int target = firstEnemy(s);
            if (target >= 0 && shouldUseProfessionSkill(s)) {
                GameCore.useProfessionSkill(s, target);
                continue;
            }
            int best = -1;
            int bestScore = -9999;
            for (int i = 0; i < s.hand.size(); i++) {
                GameCore.Card c = s.hand.get(i);
                GameCore.CardDef d = GameCore.card(c.id);
                if (d == null || GameCore.costOf(s, c, d) > s.energy) continue;
                int score = GameCore.cardDamage(c) * 3 + GameCore.cardBlock(c) * 2 + d.draw * 4 + d.energyGain * 7
                        + d.burn * 4 + d.bind * 3 + d.gainSteelEngine * 12 + d.gainAshEngine * 12
                        + d.gainWildEngine * 12 + d.gainVoidEngine * 12 + d.heal * 4 + d.scry * 2
                        + d.skillChargeGain * 7 + (d.upgradeRandom ? 8 : 0) + (d.createEcho ? 6 : 0) + (d.createWound ? 4 : 0);
                if (GameCore.PROF_BLOODBOUND.equals(s.profession) && (d.hpLoss > 0 || "wound".equals(c.id))) {
                    score += 14;
                }
                if (GameCore.PROF_WEAVER.equals(s.profession) && (d.scry > 0 || d.upgradeRandom || d.draw > 0)) {
                    score += 10;
                }
                if (GameCore.PROF_SUMMONER.equals(s.profession) && (d.createEcho || c.temp || d.bind > 0)) {
                    score += 12;
                }
                if (GameCore.PROF_HEXER.equals(s.profession) && (d.vulnerable > 0 || d.createWound || "wound".equals(c.id) || "daze".equals(c.id))) {
                    score += 12;
                }
                if (s.talents.contains("t_duelist_gambit") && s.cardsPlayedThisTurn >= 3) score += 10;
                if (s.talents.contains("t_alchemist_distiller") && d.createPotion) score += 12;
                if (s.talents.contains("t_weaver_quicksilver") && c.temp) score += 10;
                if (s.talents.contains("t_warden_armory") && d.type == 1) score += 6;
                if (s.talents.contains("t_shared_apothecary") && d.createPotion) score += 7;
                if (s.relics.contains("loom_shuttle") && d.scry > 0) score += 6;
                if (s.relics.contains("void_abacus") && d.exhaust) score += 6;
                if (s.relics.contains("tempo_metronome") && s.cardsPlayedThisTurn == 3) score += 12;
                if (s.relics.contains("vital_sprout") && d.heal > 0) score += 7;
                if (s.relics.contains("tithe_box") && d.goldGain > 0) score += 7;
                if (s.relics.contains("polished_cog") && c.upgraded) score += 4;
                if (s.relics.contains("scar_talisman") && "wound".equals(c.id)) score += 12;
                if (d.skillChargeGain > 0 && s.professionSkillCharge >= GameCore.PROF_SKILL_MAX - 4) score += 12;
                if (hasSkillRelic(s) && d.skillChargeGain > 0) score += 10;
                if (s.relics.contains("command_banner") && d.type == 1) score += 5;
                if (s.relics.contains("flash_heel") && s.cardsPlayedThisTurn >= 3) score += 5;
                if (s.relics.contains("catalyst_pump") && (d.createPotion || d.burn > 0 || d.bind > 0)) score += 6;
                if (s.relics.contains("hawk_fletching") && d.bind > 0) score += 6;
                if (s.relics.contains("echo_prism") && (d.exhaust || d.createEcho)) score += 6;
                if (s.relics.contains("ledger_stamp") && d.goldGain > 0) score += 6;
                if (s.relics.contains("crimson_seal") && (d.hpLoss > 0 || "wound".equals(c.id))) score += 6;
                if (s.relics.contains("pattern_spool") && (d.upgradeRandom || d.draw > 0)) score += 6;
                if (s.relics.contains("spirit_bell") && (d.createEcho || c.temp || d.bind > 0)) score += 6;
                if (s.relics.contains("hex_tablet") && (d.vulnerable > 0 || d.createWound)) score += 6;
                if (s.relics.contains("emberroot_charm") && (d.burn > 0 || d.bind > 0)) score += 8;
                if (s.relics.contains("stormglass_seal") && ((d.draw > 0 && d.block > 0) || (d.damage > 0 && (d.burn > 0 || d.bind > 0 || d.vulnerable > 0)))) score += 8;
                if (s.relics.contains("curse_censer") && (d.exhaust || c.temp || d.type == 3 || d.createWound)) score += 9;
                if (s.relics.contains("bloodcoin_broach") && (d.hpLoss > 0 || d.goldGain > 0 || d.goldDamage || d.goldBlock || "wound".equals(c.id))) score += 8;
                if (s.relics.contains("mirror_anvil") && (c.upgraded || d.upgradeRandom)) score += 8;
                if (s.relics.contains("rift_compass") && isOffPoolCard(s, d)) score += 10;
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

    private static boolean shouldUseProfessionSkill(GameCore.State s) {
        if (!GameCore.professionSkillReady(s)) return false;
        if (s.hp < s.maxHp * 0.35f && (GameCore.PROF_WARDEN.equals(s.profession) || GameCore.PROF_BLOODBOUND.equals(s.profession))) {
            return true;
        }
        if (GameCore.PROF_MERCHANT.equals(s.profession) && s.gold < 25) {
            return false;
        }
        return hasSkillRelic(s) || s.turn >= 2 || s.enemies.size() > 1 || s.combatKind == 'E' || s.combatKind == 'B';
    }

    private static boolean hasSkillRelic(GameCore.State s) {
        return s.relics.contains("command_banner") || s.relics.contains("flash_heel")
                || s.relics.contains("catalyst_pump") || s.relics.contains("hawk_fletching")
                || s.relics.contains("echo_prism") || s.relics.contains("ledger_stamp")
                || s.relics.contains("crimson_seal") || s.relics.contains("pattern_spool")
                || s.relics.contains("spirit_bell") || s.relics.contains("hex_tablet");
    }

    private static boolean isOffPoolCard(GameCore.State s, GameCore.CardDef d) {
        boolean offOrigin = !"通用".equals(d.origin) && !d.origin.equals(s.origin);
        boolean offProfession = d.profession.length() > 0 && !d.profession.equals(s.profession);
        return offOrigin || offProfession;
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
        int routeNodes;
    }
}
