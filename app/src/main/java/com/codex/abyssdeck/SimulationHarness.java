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
        } else if (s.mode == GameCore.MODE_SKILL_SPEC) {
            GameCore.chooseSkillSpec(s, chooseSkillSpec(s));
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
                GameCore.pickRelicReward(s, chooseRelicReward(s));
            } else if (!s.cardRewards.isEmpty()) {
                GameCore.pickRewardCard(s, chooseRewardCard(s));
            } else {
                GameCore.skipReward(s);
            }
        } else if (s.mode == GameCore.MODE_TALENT) {
            GameCore.chooseTalent(s, chooseTalent(s));
        } else if (s.mode == GameCore.MODE_SHOP) {
            if (!s.shopRelics.isEmpty() && s.gold >= GameCore.shopRelicPrice(s)) {
                GameCore.shopBuyRelic(s, 0);
            } else if (shouldScoutShop(s)) {
                GameCore.shopScoutBuild(s);
            } else if (!s.shopCards.isEmpty() && s.gold >= GameCore.shopCardPrice(s, GameCore.card(s.shopCards.get(0)))) {
                GameCore.shopBuyCard(s, 0);
            } else {
                GameCore.leaveShop(s);
            }
        } else if (s.mode == GameCore.MODE_REST) {
            if (s.hp < s.maxHp * 0.55f) {
                GameCore.restHeal(s);
            } else if (shouldAttuneRest(s)) {
                GameCore.restAttuneBuild(s);
            } else if (hasUpgradableCard(s)) {
                GameCore.restChoose(s, "rest_upgrade");
                upgradeFirst(s);
            } else {
                GameCore.restHeal(s);
            }
        } else if (s.mode == GameCore.MODE_EVENT) {
            GameCore.eventChoose(s, chooseEvent(s));
        } else if (s.mode == GameCore.MODE_DECK) {
            if ("event_remove_hp".equals(s.pendingAction) || "event_transform_bonus".equals(s.pendingAction)) {
                GameCore.deckPick(s, 0);
            } else {
                upgradeFirst(s);
            }
        }
    }

    private static int chooseEvent(GameCore.State s) {
        if (s.eventId == 0) return s.hp > s.maxHp * 0.45f ? 0 : 1;
        if (s.eventId == 1) return s.hp < s.maxHp * 0.55f ? 1 : 0;
        if (s.eventId == 3) return s.gold >= 85 ? 0 : 1;
        if (s.eventId == 4) return hasUpgradableCard(s) && s.deck.size() <= 32 ? 0 : 1;
        if (s.eventId == 5) return s.hp > s.maxHp * 0.45f && s.potions.size() < GameCore.potionLimit(s) ? 0 : 1;
        if (s.eventId == 6) return s.deck.size() > 30 ? 0 : 1;
        if (s.eventId == 7) return s.maxHp >= 60 ? 0 : 1;
        if (s.eventId == 9) return s.gold >= 90 ? 1 : 0;
        if (s.eventId == 10) return s.potions.size() <= 1 ? 0 : 1;
        if (s.eventId == 12) return s.gold >= 70 ? 0 : 1;
        if (s.eventId == 13) return s.maxHp >= 58 && s.relics.contains("rift_compass") ? 0 : 1;
        if (s.eventId == 14) return hasUpgradableCard(s) ? 0 : 1;
        if (s.eventId == 15) return s.hp > s.maxHp * 0.55f && s.deck.size() >= 22 ? 1 : 0;
        return s.eventId % 2;
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

    private static boolean shouldScoutShop(GameCore.State s) {
        return !s.shopScoutUsed && s.deck.size() < 36 && s.gold >= GameCore.shopServicePrice(s, "shop_scout")
                && (s.act >= 2 || s.relics.size() >= 3 || s.shopCards.size() <= 2);
    }

    private static boolean shouldAttuneRest(GameCore.State s) {
        return s.deck.size() < 34 && (s.act >= 2 || s.currentRoute == GameCore.ROUTE_FORGE)
                && s.hp >= s.maxHp * 0.72f;
    }

    private static int chooseRewardCard(GameCore.State s) {
        int best = 0;
        int bestScore = -9999;
        for (int i = 0; i < s.cardRewards.size(); i++) {
            GameCore.CardDef d = GameCore.card(s.cardRewards.get(i).id);
            if (d == null) continue;
            int score = d.rarity * 8 + d.damage * 2 + d.block * 2 + d.draw * 6 + d.energyGain * 8
                    + d.burn * 4 + d.bind * 4 + d.vulnerable * 7 + d.skillChargeGain * 7
                    + d.gainSteelEngine * 10 + d.gainAshEngine * 10 + d.gainWildEngine * 10 + d.gainVoidEngine * 10
                    + d.goldGain * 2 + (d.goldDamage ? 8 : 0) + (d.goldBlock ? 8 : 0)
                    + (d.createEcho ? 8 : 0) + (d.createPotion ? 8 : 0) + (d.createWound ? 5 : 0)
                    + (d.upgradeRandom ? 8 : 0) + d.scry * 2 - d.cost * 2;
            if (d.profession.equals(s.profession)) score += 24;
            if (d.skillChargeGain > 0) score += 8;
            score += GameCore.skillSpecCardBonus(s, d) * 5;
            if ("通用".equals(d.origin) || d.origin.equals(s.origin)) score += 4;
            if (d.rarity == 2 && d.profession.equals(s.profession)) score += 10;
            if (GameCore.PROF_WARDEN.equals(s.profession) && (d.block > 0 || d.blockToDamage || d.gainSteelEngine > 0)) score += 12;
            if (GameCore.PROF_DUELIST.equals(s.profession) && (d.cost == 0 || d.comboDamage > 0 || d.draw > 0)) score += 12;
            if (GameCore.PROF_ALCHEMIST.equals(s.profession) && (d.createPotion || d.burn > 0 || d.bind > 0 || d.spreadStatus)) score += 12;
            if (GameCore.PROF_RANGER.equals(s.profession) && (d.bind > 0 || d.aoe || d.bindToDraw)) score += 12;
            if (GameCore.PROF_ARCANIST.equals(s.profession) && (d.exhaust || d.createEcho || d.exhaustTopDiscard || d.exhaustForDamage)) score += 12;
            if (GameCore.PROF_MERCHANT.equals(s.profession) && (d.goldGain > 0 || d.goldDamage || d.goldBlock)) score += 12;
            if (GameCore.PROF_BLOODBOUND.equals(s.profession) && (d.hpLoss > 0 || d.heal > 0 || d.createWound)) score += 12;
            if (GameCore.PROF_WEAVER.equals(s.profession) && (d.scry > 0 || d.upgradeRandom || d.draw > 0)) score += 12;
            if (GameCore.PROF_SUMMONER.equals(s.profession) && (d.createEcho || d.bind > 0 || d.type == 1)) score += 12;
            if (GameCore.PROF_HEXER.equals(s.profession) && (d.vulnerable > 0 || d.createWound || d.addStatusToEnemy || d.spreadStatus)) score += 12;
            if (GameCore.PROF_INSCRIBER.equals(s.profession) && (d.upgradeRandom || d.scry > 0 || d.vulnerable > 0 || d.bind > 0
                    || d.addStatusToEnemy || d.createWound || d.exhaustTopDiscard || d.skillChargeGain > 0)) score += 12;
            if (GameCore.PROF_TUNER.equals(s.profession) && (d.draw > 0 || d.energyGain > 0 || d.skillChargeGain > 0 || d.cost == 0
                    || d.vulnerable > 0 || d.createEcho || d.comboDamage > 0)) score += 12;
            if (GameCore.PROF_ADJUDICATOR.equals(s.profession) && (d.skillChargeGain > 0 || d.vulnerable > 0 || d.bind > 0
                    || d.upgradeRandom || d.scry > 0 || d.draw > 0 || d.type == 1)) score += 12;
            if (GameCore.PROF_ASTROLOGER.equals(s.profession) && (d.scry > 0 || d.draw > 0 || d.createEcho
                    || d.skillChargeGain > 0 || d.upgradeRandom || d.energyGain > 0 || d.cost == 0)) score += 12;
            if (isHybridCore(d)) score += 14;
            if (isConfluenceCore(d)) score += 16;
            if ("tuner_grand_cadence".equals(d.id) || "tuner_loop".equals(d.id)) score += 12;
            if ("adjudicator_final_decree".equals(d.id) || "adjudicator_overrule".equals(d.id)
                    || "adjudicator_audit".equals(d.id)) score += 12;
            if ("astrologer_grand_orrery".equals(d.id) || "astrologer_overstar".equals(d.id)
                    || "astrologer_ephemeris".equals(d.id)) score += 12;
            if ("hybrid_rift_engine".equals(d.id)) score += 10;
            if (s.relics.contains("split_anvil") && (d.upgradeRandom || d.rarity == 2)
                    && (d.burn > 0 || d.bind > 0 || d.vulnerable > 0 || d.addStatusToEnemy || d.createWound)) score += 16;
            if (s.relics.contains("echo_ledger") && (d.exhaust || d.createEcho || d.draw > 0 || d.goldGain > 0)) score += 12;
            if (s.relics.contains("bloodspark_contract") && (d.hpLoss > 0 || d.createWound || d.goldGain > 0 || d.burn > 0 || d.vulnerable > 0)) score += 14;
            if (s.relics.contains("mosaic_core") && isHybridCore(d)) score += 16;
            if (s.relics.contains("starforge_lens") && (isHybridCore(d) || d.skillChargeGain > 0 || d.upgradeRandom || d.scry > 0)) score += 16;
            if (s.relics.contains("confluence_map") && isHybridCore(d)) score += 18;
            if (s.relics.contains("prism_gear") && (isHybridCore(d) || isConfluenceCore(d))) score += 20;
            if (s.deck.size() > 34 && d.cost >= 2 && d.draw == 0) score -= 6;
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private static int chooseRelicReward(GameCore.State s) {
        int best = 0;
        int bestScore = -9999;
        for (int i = 0; i < s.relicRewards.size(); i++) {
            String id = s.relicRewards.get(i);
            int score = 12;
            if ("sapphire_cell".equals(id) || "ink_fountain".equals(id) || "obsidian_core".equals(id)) score += 26;
            else if ("cracked_compass".equals(id) || "scarlet_dice".equals(id) || "runic_shackle".equals(id)) score += 20;
            else if ("ruby_branch".equals(id) || "black_bread".equals(id) || "blood_contract".equals(id)) score += 16;
            if (GameCore.PROF_WARDEN.equals(s.profession) && ("aegis_throne".equals(id) || "command_banner".equals(id) || "warden_plate".equals(id))) score += 34;
            if (GameCore.PROF_DUELIST.equals(s.profession) && ("finale_rapier".equals(id) || "flash_heel".equals(id) || "tempo_metronome".equals(id))) score += 34;
            if (GameCore.PROF_ALCHEMIST.equals(s.profession) && ("solar_crucible".equals(id) || "catalyst_pump".equals(id) || "alchemist_case".equals(id) || "glass_vials".equals(id))) score += 34;
            if (GameCore.PROF_RANGER.equals(s.profession) && ("apex_compass".equals(id) || "hawk_fletching".equals(id) || "ranger_map".equals(id))) score += 34;
            if (GameCore.PROF_ARCANIST.equals(s.profession) && ("singularity_orb".equals(id) || "echo_prism".equals(id) || "void_abacus".equals(id) || "arcane_ink".equals(id))) score += 34;
            if (GameCore.PROF_MERCHANT.equals(s.profession) && ("kingmaker_seal".equals(id) || "ledger_stamp".equals(id) || "merchant_key".equals(id) || "tithe_box".equals(id))) score += 34;
            if (GameCore.PROF_BLOODBOUND.equals(s.profession) && ("blood_crown".equals(id) || "crimson_seal".equals(id) || "scar_talisman".equals(id))) score += 34;
            if (GameCore.PROF_WEAVER.equals(s.profession) && ("clockwork_loom".equals(id) || "pattern_spool".equals(id) || "mirror_anvil".equals(id) || "polished_cog".equals(id))) score += 34;
            if (GameCore.PROF_SUMMONER.equals(s.profession) && ("spirit_processional".equals(id) || "spirit_bell".equals(id) || "root_drum".equals(id))) score += 34;
            if (GameCore.PROF_HEXER.equals(s.profession) && ("fallen_crown".equals(id) || "hex_tablet".equals(id) || "curse_censer".equals(id))) score += 34;
            if (GameCore.PROF_INSCRIBER.equals(s.profession) && ("living_codex".equals(id) || "engraver_stylus".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "curse_censer".equals(id) || "stormglass_seal".equals(id))) score += 34;
            if (GameCore.PROF_TUNER.equals(s.profession) && ("conductor_baton".equals(id) || "tuning_fork".equals(id)
                    || "tempo_metronome".equals(id) || "echo_ledger".equals(id) || "confluence_map".equals(id))) score += 34;
            if (GameCore.PROF_ADJUDICATOR.equals(s.profession) && ("judgment_codex".equals(id) || "verdict_seal".equals(id)
                    || "stormglass_seal".equals(id) || "tempo_metronome".equals(id) || "confluence_map".equals(id))) score += 34;
            if (GameCore.PROF_ASTROLOGER.equals(s.profession) && ("celestial_orrery".equals(id) || "star_compass".equals(id)
                    || "echo_ledger".equals(id) || "confluence_map".equals(id) || "prism_gear".equals(id)
                    || "tempo_metronome".equals(id) || "tuning_fork".equals(id))) score += 34;
            if ("confluence_map".equals(id) || "prism_gear".equals(id) || "mosaic_core".equals(id) || "starforge_lens".equals(id)) score += 28;
            if ("split_anvil".equals(id) && (GameCore.PROF_WEAVER.equals(s.profession) || GameCore.PROF_INSCRIBER.equals(s.profession)
                    || GameCore.PROF_ALCHEMIST.equals(s.profession) || GameCore.PROF_HEXER.equals(s.profession))) score += 28;
            if ("echo_ledger".equals(id) && (GameCore.PROF_ARCANIST.equals(s.profession) || GameCore.PROF_SUMMONER.equals(s.profession)
                    || GameCore.PROF_DUELIST.equals(s.profession) || GameCore.PROF_MERCHANT.equals(s.profession))) score += 28;
            if ("bloodspark_contract".equals(id) && (GameCore.PROF_BLOODBOUND.equals(s.profession) || GameCore.PROF_MERCHANT.equals(s.profession)
                    || GameCore.PROF_HEXER.equals(s.profession) || GameCore.PROF_ALCHEMIST.equals(s.profession))) score += 28;
            score += GameCore.skillSpecRelicBonus(s, id) * 14;
            if (s.relics.contains(id)) score -= 100;
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
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
            else if ("pact_summon".equals(id)) score += GameCore.PROF_SUMMONER.equals(s.profession) || GameCore.PROF_ARCANIST.equals(s.profession) ? 38 : 18;
            else if ("pact_hex".equals(id)) score += GameCore.PROF_HEXER.equals(s.profession) || GameCore.PROF_BLOODBOUND.equals(s.profession) ? 38 : 14;
            else if ("pact_forge".equals(id)) score += GameCore.PROF_WEAVER.equals(s.profession) || GameCore.PROF_WARDEN.equals(s.profession)
                    || GameCore.PROF_INSCRIBER.equals(s.profession) ? 36 : 18;
            else if ("pact_merchant".equals(id)) score += GameCore.PROF_MERCHANT.equals(s.profession) ? 40 : 16;
            if (GameCore.PROF_SUMMONER.equals(s.profession) && ("pact_void".equals(id) || "pact_hunter".equals(id))) score += 18;
            if (GameCore.PROF_HEXER.equals(s.profession) && ("pact_blood".equals(id) || "pact_void".equals(id))) score += 18;
            if (GameCore.PROF_SUMMONER.equals(s.profession) && "pact_summon".equals(id)) score += 20;
            if (GameCore.PROF_HEXER.equals(s.profession) && "pact_hex".equals(id)) score += 20;
            if (GameCore.PROF_INSCRIBER.equals(s.profession) && ("pact_forge".equals(id) || "pact_hex".equals(id) || "pact_sprinter".equals(id))) score += 18;
            if (GameCore.PROF_TUNER.equals(s.profession) && ("pact_sprinter".equals(id) || "pact_void".equals(id) || "pact_hunter".equals(id))) score += 18;
            if (GameCore.PROF_ADJUDICATOR.equals(s.profession) && ("pact_guardian".equals(id) || "pact_forge".equals(id)
                    || "pact_hunter".equals(id) || "pact_sprinter".equals(id))) score += 18;
            if (GameCore.PROF_ASTROLOGER.equals(s.profession) && ("pact_sprinter".equals(id) || "pact_void".equals(id)
                    || "pact_forge".equals(id) || "pact_hunter".equals(id))) score += 18;
            if (s.ascension >= 6 && "pact_blood".equals(id) && !GameCore.PROF_BLOODBOUND.equals(s.profession)) score -= 8;
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private static int chooseTalent(GameCore.State s) {
        int best = 0;
        int bestScore = -9999;
        for (int i = 0; i < s.talentChoices.size(); i++) {
            int score = GameCore.talentSynergyScore(s, s.talentChoices.get(i));
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private static int chooseSkillSpec(GameCore.State s) {
        int best = 0;
        int bestScore = -9999;
        for (int i = 0; i < s.skillSpecChoices.size(); i++) {
            String id = s.skillSpecChoices.get(i);
            int score = 0;
            if ("spec_mastery".equals(id)) score += 34;
            else if ("spec_resonance".equals(id)) score += 30;
            else if ("spec_tempo".equals(id)) score += 28;
            else if ("spec_burst".equals(id)) score += GameCore.PROF_DUELIST.equals(s.profession) || GameCore.PROF_RANGER.equals(s.profession) ? 32 : 22;
            else if ("spec_sustain".equals(id)) score += GameCore.PROF_WARDEN.equals(s.profession) || GameCore.PROF_BLOODBOUND.equals(s.profession) ? 32 : 20;
            else if ("spec_control".equals(id)) score += GameCore.PROF_RANGER.equals(s.profession) || GameCore.PROF_HEXER.equals(s.profession)
                    || GameCore.PROF_INSCRIBER.equals(s.profession) ? 31 : 24;
            else if ("spec_assembly".equals(id)) score += GameCore.PROF_WEAVER.equals(s.profession) || GameCore.PROF_MACHINIST.equals(s.profession)
                    || GameCore.PROF_ASTROLOGER.equals(s.profession) ? 31 : 24;
            else if ("spec_echoflow".equals(id)) score += GameCore.PROF_ARCANIST.equals(s.profession) || GameCore.PROF_SUMMONER.equals(s.profession)
                    || GameCore.PROF_CHRONOMANCER.equals(s.profession) ? 33 : 26;
            else if ("spec_markchain".equals(id)) score += GameCore.PROF_RANGER.equals(s.profession) || GameCore.PROF_TUNER.equals(s.profession)
                    || GameCore.PROF_ADJUDICATOR.equals(s.profession) || GameCore.PROF_HEXER.equals(s.profession) ? 33 : 25;
            if (s.ascension >= 6 && "spec_sustain".equals(id)) score += 10;
            if (s.ascension >= 6 && "spec_burst".equals(id)) score -= 4;
            if (s.ascension >= 6 && ("spec_markchain".equals(id) || "spec_control".equals(id))) score += 4;
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
                        + d.skillChargeGain * 7 + d.vulnerable * 5 + d.goldGain * 2 + (d.goldDamage ? 8 : 0)
                        + (d.goldBlock ? 8 : 0) + (d.exhaust ? 2 : 0) + (d.upgradeRandom ? 8 : 0)
                        + (d.createEcho ? 6 : 0) + (d.createWound ? 4 : 0) + (d.aoe ? 8 : 0)
                        + (d.spreadStatus ? 10 : 0) + (d.bindToDraw ? 8 : 0) + (d.burnToBlock ? 7 : 0)
                        - d.hpLoss * 2;
                if (s.combatQuest == GameCore.QUEST_BREW && d.createPotion) score += 22;
                if (s.combatQuest == GameCore.QUEST_SKILL && d.skillChargeGain > 0) score += 18;
                if (s.combatQuest == GameCore.QUEST_ECHO && (d.exhaust || d.createEcho || c.temp)) score += 18;
                if (s.combatQuest == GameCore.QUEST_BLOODCOIN
                        && (d.hpLoss > 0 || d.goldGain > 0 || d.goldDamage || d.goldBlock || "wound".equals(c.id))) score += 18;
                if (s.combatQuest == GameCore.QUEST_FORGE && (c.upgraded || d.upgradeRandom || d.scry > 0)) score += 18;
                if (s.combatQuest == GameCore.QUEST_TREASURE && (d.goldGain > 0 || d.goldDamage || d.goldBlock)) score += 18;
                if (s.combatQuest == GameCore.QUEST_CONFLUENCE && (isHybridCore(d) || isConfluenceCore(d))) score += 24;
                if (s.combatQuest == GameCore.QUEST_MARK && (d.bind > 0 || d.vulnerable > 0 || d.comboDamage > 0
                        || "tuner_note".equals(c.id) || "tuner_harmonic".equals(c.id) || "tuner_grand_cadence".equals(c.id))) score += 20;
                if (s.combatQuest == GameCore.QUEST_OVERLOAD && d.skillChargeGain > 0) score += 24;
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
                if (GameCore.PROF_INSCRIBER.equals(s.profession) && (c.upgraded || d.upgradeRandom || d.vulnerable > 0 || d.bind > 0
                        || d.addStatusToEnemy || d.createWound || d.exhaustTopDiscard || "wound".equals(c.id) || "daze".equals(c.id))) {
                    score += 12;
                }
                if (GameCore.PROF_TUNER.equals(s.profession) && (d.draw > 0 || d.energyGain > 0 || d.skillChargeGain > 0 || d.cost == 0
                        || d.vulnerable > 0 || d.createEcho || d.comboDamage > 0)) {
                    score += 12;
                }
                if (GameCore.PROF_ADJUDICATOR.equals(s.profession) && (d.skillChargeGain > 0 || d.vulnerable > 0 || d.bind > 0
                        || d.upgradeRandom || d.scry > 0 || d.draw > 0 || d.type == 1)) {
                    score += 12;
                }
                if (GameCore.PROF_ASTROLOGER.equals(s.profession) && (d.scry > 0 || d.draw > 0 || d.createEcho || c.temp
                        || d.skillChargeGain > 0 || d.upgradeRandom || d.energyGain > 0 || d.cost == 0)) {
                    score += 12;
                }
                if (s.talents.contains("t_duelist_gambit") && s.cardsPlayedThisTurn >= 3) score += 10;
                if (s.talents.contains("t_alchemist_distiller") && d.createPotion) score += 12;
                if (s.talents.contains("t_weaver_quicksilver") && c.temp) score += 10;
                if (s.talents.contains("t_warden_armory") && d.type == 1) score += 6;
                if (s.talents.contains("t_warden_vanguard") && d.type == 1) score += 10;
                if (s.talents.contains("t_duelist_masterstep") && s.cardsPlayedThisTurn >= 4) score += 14;
                if (s.talents.contains("t_alchemist_grandbrew") && (d.createPotion || d.burn > 0 || d.bind > 0 || d.spreadStatus)) score += 12;
                if (s.talents.contains("t_ranger_apex") && (d.bind > 0 || d.vulnerable > 0 || d.bindToDraw)) score += 10;
                if (s.talents.contains("t_arcanist_singularity") && (d.exhaust || d.createEcho || c.temp)) score += 12;
                if (s.talents.contains("t_merchant_monopoly") && (d.goldGain > 0 || d.goldDamage || d.goldBlock)) score += 12;
                if (s.talents.contains("t_bloodbound_hemocraft") && (d.hpLoss > 0 || "wound".equals(c.id))) score += 12;
                if (s.talents.contains("t_weaver_grandpattern") && (c.upgraded || d.upgradeRandom || d.scry > 0)) score += 12;
                if (s.talents.contains("t_summoner_overflow") && (d.createEcho || c.temp || "summoner_sprite".equals(c.id) || "summoner_wisp".equals(c.id))) score += 12;
                if (s.talents.contains("t_hexer_abysscurse") && (d.vulnerable > 0 || d.createWound || d.addStatusToEnemy || "wound".equals(c.id) || "daze".equals(c.id))) score += 12;
                if (s.talents.contains("t_inscriber_grandcodex") && (c.upgraded || d.upgradeRandom || d.vulnerable > 0 || d.bind > 0 || d.addStatusToEnemy || "wound".equals(c.id) || "daze".equals(c.id))) score += 12;
                if (s.talents.contains("t_shared_apothecary") && d.createPotion) score += 7;
                if ("warden_aegisline".equals(c.id) && s.block >= 20) score += 14;
                if ("duelist_bladesong".equals(c.id) && s.cardsPlayedThisTurn >= 3) score += 16;
                if ("alchemist_sunsteel".equals(c.id) && (s.burnPower + s.bindPower >= 2 || s.potions.size() < GameCore.potionLimit(s))) score += 14;
                if ("ranger_predator".equals(c.id) && target >= 0 && s.enemies.get(target).bind > 0) score += 14;
                if ("arcanist_eventhorizon".equals(c.id)) score += 12;
                if ("merchant_kingmaker".equals(c.id) && s.gold >= 120) score += 14;
                if ("blood_apotheosis".equals(c.id) && s.hp < s.maxHp) score += 14;
                if ("weaver_clockwork".equals(c.id)) score += 12;
                if ("summoner_procession".equals(c.id)) score += 14;
                if ("hexer_crownfall".equals(c.id)) score += 14;
                if ("inscriber_codex".equals(c.id)) score += 14;
                if ("tuner_grand_cadence".equals(c.id) || "tuner_loop".equals(c.id) || "tuner_overclock".equals(c.id)) score += 14;
                if ("adjudicator_final_decree".equals(c.id) || "adjudicator_overrule".equals(c.id)
                        || "adjudicator_audit".equals(c.id) || "adjudicator_writ".equals(c.id)) score += 14;
                if ("astrologer_grand_orrery".equals(c.id) || "astrologer_overstar".equals(c.id)
                        || "astrologer_ephemeris".equals(c.id) || "astrologer_chart".equals(c.id)) score += 14;
                if (isHybridCore(d)) score += 14;
                if (isConfluenceCore(d)) score += 16 + s.confluenceChain * 2;
                if ("hybrid_rift_engine".equals(c.id)) score += 10;
                if (s.relics.contains("loom_shuttle") && d.scry > 0) score += 6;
                if (s.relics.contains("void_abacus") && d.exhaust) score += 6;
                if (s.relics.contains("tempo_metronome") && s.cardsPlayedThisTurn == 3) score += 12;
                if (s.relics.contains("vital_sprout") && d.heal > 0) score += 7;
                if (s.relics.contains("tithe_box") && d.goldGain > 0) score += 7;
                if (s.relics.contains("polished_cog") && c.upgraded) score += 4;
                if (s.relics.contains("scar_talisman") && "wound".equals(c.id)) score += 12;
                if (d.skillChargeGain > 0 && s.professionSkillCharge >= GameCore.PROF_SKILL_MAX - 4) score += 12;
                if (d.skillChargeGain > 0 && s.professionSkillCharge >= GameCore.PROF_SKILL_MAX) score += 10;
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
                if (s.relics.contains("engraver_stylus") && (c.upgraded || d.upgradeRandom || d.vulnerable > 0 || d.bind > 0 || d.addStatusToEnemy)) score += 6;
                if (s.relics.contains("emberroot_charm") && (d.burn > 0 || d.bind > 0)) score += 8;
                if (s.relics.contains("stormglass_seal") && ((d.draw > 0 && d.block > 0) || (d.damage > 0 && (d.burn > 0 || d.bind > 0 || d.vulnerable > 0)))) score += 8;
                if (s.relics.contains("curse_censer") && (d.exhaust || c.temp || d.type == 3 || d.createWound)) score += 9;
                if (s.relics.contains("bloodcoin_broach") && (d.hpLoss > 0 || d.goldGain > 0 || d.goldDamage || d.goldBlock || "wound".equals(c.id))) score += 8;
                if (s.relics.contains("mirror_anvil") && (c.upgraded || d.upgradeRandom)) score += 8;
                if (s.relics.contains("rift_compass") && isOffPoolCard(s, d)) score += 10;
                if (s.relics.contains("split_anvil") && (c.upgraded || d.upgradeRandom)
                        && (d.burn > 0 || d.bind > 0 || d.vulnerable > 0 || d.addStatusToEnemy || d.createWound)) score += 12;
                if (s.relics.contains("echo_ledger") && (d.exhaust || d.createEcho || c.temp || d.goldGain > 0)) score += 11;
                if (s.relics.contains("bloodspark_contract") && (d.hpLoss > 0 || d.createWound || d.goldGain > 0 || d.burn > 0 || d.vulnerable > 0 || "wound".equals(c.id))) score += 12;
                if (s.relics.contains("mosaic_core") && isHybridCore(d)) score += 14;
                if (s.relics.contains("starforge_lens") && (isHybridCore(d) || d.skillChargeGain > 0 || d.upgradeRandom || d.scry > 0)) score += 14;
                if (s.relics.contains("confluence_map") && isHybridCore(d)) score += 14;
                if (s.relics.contains("prism_gear") && (isHybridCore(d) || isConfluenceCore(d))) score += 16;
                if (s.relics.contains("aegis_throne") && d.type == 1) score += 9;
                if (s.relics.contains("finale_rapier") && d.type == 0 && s.cardsPlayedThisTurn >= 3) score += 10;
                if (s.relics.contains("solar_crucible") && (d.createPotion || d.burn > 0 || d.bind > 0)) score += 10;
                if (s.relics.contains("apex_compass") && d.bind > 0) score += 10;
                if (s.relics.contains("singularity_orb") && (d.exhaust || d.createEcho || c.temp)) score += 10;
                if (s.relics.contains("kingmaker_seal") && (d.goldGain > 0 || d.goldDamage || d.goldBlock)) score += 10;
                if (s.relics.contains("blood_crown") && (d.hpLoss > 0 || "wound".equals(c.id))) score += 10;
                if (s.relics.contains("clockwork_loom") && (c.upgraded || d.upgradeRandom || d.scry > 0)) score += 10;
                if (s.relics.contains("spirit_processional") && (d.createEcho || c.temp || d.bind > 0)) score += 10;
                if (s.relics.contains("fallen_crown") && (d.vulnerable > 0 || d.createWound || "wound".equals(c.id) || "daze".equals(c.id))) score += 10;
                if (s.relics.contains("living_codex") && (c.upgraded || d.upgradeRandom || d.vulnerable > 0 || d.bind > 0 || d.addStatusToEnemy || "wound".equals(c.id) || "daze".equals(c.id))) score += 10;
                if (s.relics.contains("tuning_fork") && (d.draw > 0 || d.energyGain > 0 || d.skillChargeGain > 0 || c.temp)) score += 10;
                if (s.relics.contains("conductor_baton") && (d.draw > 0 || d.energyGain > 0 || d.skillChargeGain > 0 || d.vulnerable > 0)) score += 12;
                if (s.relics.contains("verdict_seal") && (d.skillChargeGain > 0 || d.vulnerable > 0 || d.bind > 0 || d.draw > 0 || d.type == 1)) score += 10;
                if (s.relics.contains("judgment_codex") && (d.skillChargeGain > 0 || d.vulnerable > 0 || d.bind > 0 || c.upgraded)) score += 12;
                if (s.relics.contains("star_compass") && (d.scry > 0 || d.draw > 0 || d.createEcho || c.temp || d.skillChargeGain > 0)) score += 10;
                if (s.relics.contains("celestial_orrery") && (d.scry > 0 || d.createEcho || c.temp || d.upgradeRandom || c.upgraded)) score += 12;
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
        int overload = GameCore.professionSkillOverload(s);
        if (s.combatQuest == GameCore.QUEST_SKILL) {
            return true;
        }
        if (s.combatQuest == GameCore.QUEST_OVERLOAD && GameCore.professionSkillOverload(s) > 0) {
            return true;
        }
        if (s.hp < s.maxHp * 0.35f && (GameCore.PROF_WARDEN.equals(s.profession) || GameCore.PROF_BLOODBOUND.equals(s.profession))) {
            return true;
        }
        if (GameCore.PROF_MERCHANT.equals(s.profession) && s.gold < 25) {
            return false;
        }
        if (overload >= 3) {
            return true;
        }
        return hasSkillRelic(s) || s.turn >= 2 || s.enemies.size() > 1 || s.combatKind == 'E' || s.combatKind == 'B';
    }

    private static boolean hasSkillRelic(GameCore.State s) {
        return s.relics.contains("command_banner") || s.relics.contains("flash_heel")
                || s.relics.contains("catalyst_pump") || s.relics.contains("hawk_fletching")
                || s.relics.contains("echo_prism") || s.relics.contains("ledger_stamp")
                || s.relics.contains("crimson_seal") || s.relics.contains("pattern_spool")
                || s.relics.contains("spirit_bell") || s.relics.contains("hex_tablet")
                || s.relics.contains("aegis_throne") || s.relics.contains("finale_rapier")
                || s.relics.contains("solar_crucible") || s.relics.contains("apex_compass")
                || s.relics.contains("singularity_orb") || s.relics.contains("kingmaker_seal")
                || s.relics.contains("blood_crown") || s.relics.contains("clockwork_loom")
                || s.relics.contains("spirit_processional") || s.relics.contains("fallen_crown")
                || s.relics.contains("engraver_stylus") || s.relics.contains("living_codex")
                || s.relics.contains("split_anvil") || s.relics.contains("echo_ledger")
                || s.relics.contains("bloodspark_contract") || s.relics.contains("confluence_map") || s.relics.contains("prism_gear")
                || s.relics.contains("mosaic_core") || s.relics.contains("starforge_lens")
                || s.relics.contains("tuning_fork") || s.relics.contains("conductor_baton")
                || s.relics.contains("verdict_seal") || s.relics.contains("judgment_codex")
                || s.relics.contains("star_compass") || s.relics.contains("celestial_orrery");
    }

    private static boolean isOffPoolCard(GameCore.State s, GameCore.CardDef d) {
        boolean offOrigin = !"通用".equals(d.origin) && !d.origin.equals(s.origin);
        boolean offProfession = d.profession.length() > 0 && !d.profession.equals(s.profession);
        return offOrigin || offProfession;
    }

    private static boolean isHybridCore(GameCore.CardDef d) {
        return d != null && ("hybrid_forgebrand".equals(d.id) || "hybrid_echo_step".equals(d.id)
                || "hybrid_blood_tithe".equals(d.id) || "hybrid_guard_conduit".equals(d.id)
                || "hybrid_plague_brew".equals(d.id) || "hybrid_coinwall".equals(d.id)
                || "hybrid_bloodcharge".equals(d.id) || "hybrid_echo_vial".equals(d.id)
                || "hybrid_hexdance".equals(d.id) || "hybrid_spirit_anvil".equals(d.id)
                || "hybrid_rift_engine".equals(d.id));
    }

    private static boolean isConfluenceCore(GameCore.CardDef d) {
        return d != null && ("confluence_chord".equals(d.id) || "prism_anchor".equals(d.id)
                || "apex_confluence".equals(d.id));
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
