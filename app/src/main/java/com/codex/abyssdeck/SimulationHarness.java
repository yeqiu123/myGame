package com.codex.abyssdeck;

import java.util.ArrayList;

public final class SimulationHarness {
    private static final int BUILD_OVERLOAD = 0;
    private static final int BUILD_ECHO = 1;
    private static final int BUILD_BREW = 2;
    private static final int BUILD_GOLD = 3;
    private static final int BUILD_BLOOD = 4;
    private static final int BUILD_FORGE = 5;
    private static final int BUILD_STATUS = 6;
    private static final int BUILD_CYCLE = 7;
    private static final int BUILD_GUARD = 8;

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
        if (s.eventId == 17) return s.hp > s.maxHp * 0.6f && s.deck.size() < 34 ? 0 : 1;
        if (s.eventId == 18) return s.hp > s.maxHp * 0.5f && s.deck.size() < 34 ? 0 : 1;
        if (s.eventId == 19) return s.hp > s.maxHp * 0.55f && (!hasBuildCoreTalent(s) || s.deck.size() < 36) ? 0 : 1;
        if (s.eventId == 20) return s.hp > s.maxHp * 0.55f && s.deck.size() < 38 ? 0 : (s.gold >= 55 ? 1 : 0);
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
            int score = d.rarity * 8 + d.damage * 2 + d.block * 2 + d.draw * 6 + d.heal * 5 + d.energyGain * 8
                    + d.burn * 4 + d.bind * 4 + d.vulnerable * 7 + d.skillChargeGain * 7
                    + d.gainSteelEngine * 10 + d.gainAshEngine * 10 + d.gainWildEngine * 10 + d.gainVoidEngine * 10
                    + d.goldGain * 2 + (d.goldDamage ? 8 : 0) + (d.goldBlock ? 8 : 0)
                    + (d.createEcho ? 8 : 0) + (d.createPotion ? 8 : 0) + (d.createWound ? 5 : 0)
                    + (d.upgradeRandom ? 8 : 0) + d.scry * 2 - d.cost * 2;
            if (d.profession.equals(s.profession)) score += 24;
            if (d.skillChargeGain > 0) score += 8;
            int specSignal = GameCore.skillSpecCardBonus(s, d);
            score += specSignal * 5;
            int coreSignal = buildCoreCardSignal(s, d);
            if (coreSignal > 0) {
                score += Math.min(36, coreSignal * 3);
                if (coreSignal >= 8) score += 12;
                if (d.rarity == 2) score += 4;
            }
            if (isResonanceBridgeCard(d)) {
                // 共鸣桥接牌更适合专修、核心天赋和汇流任务成型时拿取。
                score += 12;
                if (s.skillSpec != null && s.skillSpec.length() > 0) score += 6 + Math.max(1, s.skillSpecLevel) * 2;
                if (coreSignal > 0) score += 10;
                if (s.combatQuest == GameCore.QUEST_CONFLUENCE) score += 10 + s.confluenceChain * 2;
            }
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
            if (GameCore.PROF_MACHINIST.equals(s.profession) && (d.upgradeRandom || d.scry > 0 || d.createEcho
                    || d.skillChargeGain > 0 || d.energyGain > 0 || d.block > 0 || isHybridCore(d))) score += 12;
            if (GameCore.PROF_CHRONOMANCER.equals(s.profession) && (d.cost == 0 || d.draw > 0 || d.energyGain > 0
                    || d.skillChargeGain > 0 || d.createEcho || d.exhaust)) score += 12;
            if (GameCore.PROF_PACTMAKER.equals(s.profession) && (d.goldGain > 0 || d.goldDamage || d.goldBlock
                    || d.skillChargeGain > 0 || d.vulnerable > 0 || d.bind > 0 || d.createWound || d.draw > 0 || d.type == 1)) score += 14;
            if (GameCore.PROF_STORMCALLER.equals(s.profession) && isStormcallerSignal(d)) score += 14;
            if (GameCore.PROF_SHADOWDANCER.equals(s.profession) && isShadowdancerSignal(d)) score += 14;
            if (GameCore.PROF_RUNEBLADE.equals(s.profession) && isRunebladeSignal(d)) score += 14;
            if (GameCore.PROF_MEDIUM.equals(s.profession) && isMediumSignal(d)) score += 14;
            if (GameCore.PROF_TACTICIAN.equals(s.profession) && isTacticianSignal(d)) score += 14;
            if (GameCore.PROF_PRISMIST.equals(s.profession) && isPrismistSignal(d)) score += 14;
            if (GameCore.PROF_DREAMWALKER.equals(s.profession) && isDreamwalkerSignal(d)) score += 14;
            if (GameCore.PROF_GARDENER.equals(s.profession) && isGardenerSignal(d)) score += 14;
            if (GameCore.PROF_CHEF.equals(s.profession) && isChefSignal(d)) score += 14;
            if (GameCore.PROF_BARD.equals(s.profession) && isBardSignal(d)) score += 14;
            if (GameCore.PROF_MIRRORIST.equals(s.profession) && isMirroristSignal(d)) score += 14;
            if (GameCore.PROF_PUPPETEER.equals(s.profession) && isPuppeteerSignal(d)) score += 14;
            if (GameCore.PROF_SCAVENGER.equals(s.profession) && isScavengerSignal(d)) score += 14;
            if (GameCore.PROF_LIGHTKEEPER.equals(s.profession) && isLightkeeperSignal(d)) score += 14;
            if (GameCore.PROF_GEOMANCER.equals(s.profession) && isGeomancerSignal(d)) score += 14;
            if (GameCore.PROF_WITCH.equals(s.profession) && isWitchSignal(d)) score += 14;
            if (GameCore.PROF_SHIFTER.equals(s.profession) && isShifterSignal(d)) score += 14;
            if (GameCore.PROF_FATESEER.equals(s.profession) && isFateseerSignal(d)) score += 14;
            if (GameCore.PROF_TIDECALLER.equals(s.profession) && isTidecallerSignal(d)) score += 14;
            if (GameCore.PROF_FROSTBINDER.equals(s.profession) && isFrostbinderSignal(d)) score += 14;
            if (GameCore.PROF_PLAGUEDOCTOR.equals(s.profession) && isPlaguedoctorSignal(d)) score += 14;
            if (GameCore.PROF_ARCHIVIST.equals(s.profession) && isArchivistSignal(d)) score += 14;
            if (GameCore.PROF_VOIDNAVIGATOR.equals(s.profession) && isVoidnavigatorSignal(d)) score += 14;
            if (GameCore.PROF_RELICSMITH.equals(s.profession) && isRelicsmithSignal(d)) score += 14;
            if (GameCore.PROF_BEASTMASTER.equals(s.profession) && isBeastmasterSignal(d)) score += 14;
            if (isBeastmasterCard(d)) score += 14;
            if (GameCore.PROF_DRAGONBINDER.equals(s.profession) && isDragonbinderSignal(d)) score += 14;
            if (isDragonbinderCard(d)) score += 14;
            if (GameCore.PROF_SOULBINDER.equals(s.profession) && isSoulbinderSignal(d)) score += 14;
            if (isSoulbinderCard(d)) score += 14;
            if (GameCore.PROF_STARFORGER.equals(s.profession) && isStarforgerSignal(d)) score += 14;
            if (isStarforgerCard(d)) score += 14;
            if (GameCore.PROF_PATHFINDER.equals(s.profession) && isPathfinderSignal(d)) score += 14;
            if (isPathfinderCard(d)) score += 14;
            if (GameCore.PROF_ARRAYIST.equals(s.profession) && isArrayistSignal(d)) score += 14;
            if (isArrayistCard(d)) score += 14;
            if (GameCore.PROF_GAMBITER.equals(s.profession) && isGambiterSignal(d)) score += 14;
            if (isGambiterCard(d)) score += 14;
            if (GameCore.PROF_GRAVEKEEPER.equals(s.profession) && isGravekeeperSignal(d)) score += 14;
            if (isGravekeeperCard(d)) score += 14;
            if (GameCore.PROF_TREASURER.equals(s.profession) && isTreasurerSignal(d)) score += 14;
            if (isTreasurerCard(d)) score += 14;
            if (GameCore.PROF_DRIFTER.equals(s.profession) && isDrifterSignal(s, d)) score += 14;
            if (isDrifterCard(d)) score += 14;
            if (GameCore.PROF_OATHKEEPER.equals(s.profession) && isOathkeeperSignal(d)) score += 14;
            if (isOathkeeperCard(d)) score += 14;
            if (GameCore.PROF_MOONSINGER.equals(s.profession) && isMoonsingerSignal(d)) score += 14;
            if (isMoonsingerCard(d)) score += 14;
            if (GameCore.PROF_SPY.equals(s.profession) && isSpySignal(d)) score += 14;
            if (isSpyCard(d)) score += 14;
            if (GameCore.PROF_PERFUMER.equals(s.profession) && isPerfumerSignal(d)) score += 14;
            if (isPerfumerCard(d)) score += 14;
            if (GameCore.PROF_CLOCKSMITH.equals(s.profession) && isClocksmithSignal(d)) score += 14;
            if (isClocksmithCard(d)) score += 14;
            if (GameCore.PROF_MINTSMITH.equals(s.profession) && isMintsmithSignal(d)) score += 14;
            if (isMintsmithCard(d)) score += 14;
            if (isHybridCore(d)) score += 14;
            if (isConfluenceCore(d)) score += 16;
            if ("tuner_grand_cadence".equals(d.id) || "tuner_loop".equals(d.id)) score += 12;
            if ("adjudicator_final_decree".equals(d.id) || "adjudicator_overrule".equals(d.id)
                    || "adjudicator_audit".equals(d.id)) score += 12;
            if ("astrologer_grand_orrery".equals(d.id) || "astrologer_overstar".equals(d.id)
                    || "astrologer_ephemeris".equals(d.id)) score += 12;
            if ("machinist_grand_engine".equals(d.id) || "machinist_overdrive".equals(d.id)
                    || "machinist_cogcall".equals(d.id) || "machinist_blueprint".equals(d.id)) score += 12;
            if ("chronomancer_time_engine".equals(d.id) || "chronomancer_overloop".equals(d.id)
                    || "chronomancer_loop".equals(d.id) || "chronomancer_tick".equals(d.id)) score += 12;
            if ("pactmaker_grand_contract".equals(d.id) || "pactmaker_overdeal".equals(d.id)
                    || "pactmaker_witness".equals(d.id) || "pactmaker_collection".equals(d.id)) score += 14;
            if ("medium_grand_seance".equals(d.id) || "medium_overtrance".equals(d.id)
                    || "medium_oracle".equals(d.id) || "medium_binding".equals(d.id)) score += 14;
            if ("tactician_grand_strategy".equals(d.id) || "tactician_overplan".equals(d.id)
                    || "tactician_map".equals(d.id) || "tactician_flank".equals(d.id)) score += 14;
            if ("prismist_grand_spectrum".equals(d.id) || "prismist_overbeam".equals(d.id)
                    || "prismist_lens".equals(d.id) || "prismist_anchor".equals(d.id)
                    || "prismist_spill".equals(d.id)) score += 14;
            if ("dreamwalker_grand_dream".equals(d.id) || "dreamwalker_overdream".equals(d.id)
                    || "dreamwalker_lucid".equals(d.id) || "dreamwalker_bind".equals(d.id)
                    || "dreamwalker_veil".equals(d.id)) score += 14;
            if ("gardener_grand_grove".equals(d.id) || "gardener_overgrowth".equals(d.id)
                    || "gardener_sprout".equals(d.id) || "gardener_rootwall".equals(d.id)
                    || "gardener_compost".equals(d.id) || "gardener_thornbloom".equals(d.id)) score += 14;
            if ("chef_grand_banquet".equals(d.id) || "chef_overcook".equals(d.id)
                    || "chef_prep".equals(d.id) || "chef_stew".equals(d.id)
                    || "chef_spice".equals(d.id) || "chef_sizzle".equals(d.id)) score += 14;
            if ("bard_grand_finale".equals(d.id) || "bard_overcrescendo".equals(d.id)
                    || "bard_note".equals(d.id) || "bard_ballad".equals(d.id)
                    || "bard_chorus".equals(d.id) || "bard_discord".equals(d.id)) score += 14;
            if ("mirrorist_grand_mirror".equals(d.id) || "mirrorist_overimage".equals(d.id)
                    || "mirrorist_shard".equals(d.id) || "mirrorist_guard".equals(d.id)
                    || "mirrorist_reflect".equals(d.id) || "mirrorist_prismcut".equals(d.id)) score += 14;
            if ("puppeteer_grand_stage".equals(d.id) || "puppeteer_overpull".equals(d.id)
                    || "puppeteer_thread".equals(d.id) || "puppeteer_screen".equals(d.id)
                    || "puppeteer_rehearse".equals(d.id) || "puppeteer_needle".equals(d.id)) score += 14;
            if ("scavenger_grand_foundry".equals(d.id) || "scavenger_overhaul".equals(d.id)) score += 18;
            if ("scavenger_pick".equals(d.id) || "scavenger_sort".equals(d.id)
                    || "scavenger_patch".equals(d.id) || "scavenger_magnet".equals(d.id)) score += 14;
            if (isStormcallerCard(d)) score += 14;
            if (isShadowdancerCard(d)) score += 14;
            if (isRunebladeCard(d)) score += 14;
            if (isMediumCard(d)) score += 14;
            if (isTacticianCard(d)) score += 14;
            if (isPrismistCard(d)) score += 14;
            if (isDreamwalkerCard(d)) score += 14;
            if (isGardenerCard(d)) score += 14;
            if (isChefCard(d)) score += 14;
            if (isBardCard(d)) score += 14;
            if (isMirroristCard(d)) score += 14;
            if (isPuppeteerCard(d)) score += 14;
            if (isScavengerCard(d)) score += 14;
            if (isGeomancerCard(d)) score += 14;
            if (isWitchCard(d)) score += 14;
            if (isShifterCard(d)) score += 14;
            if (isFateseerCard(d)) score += 14;
            if (isTidecallerCard(d)) score += 14;
            if (isFrostbinderCard(d)) score += 14;
            if (isPlaguedoctorCard(d)) score += 14;
            if (isArchivistCard(d)) score += 14;
            if (isVoidnavigatorCard(d)) score += 14;
            if (isRelicsmithCard(d)) score += 14;
            if ("hybrid_rift_engine".equals(d.id)) score += 10;
            if (s.relics.contains("split_anvil") && (d.upgradeRandom || d.rarity == 2)
                    && (d.burn > 0 || d.bind > 0 || d.vulnerable > 0 || d.addStatusToEnemy || d.createWound)) score += 16;
            if (s.relics.contains("echo_ledger") && (d.exhaust || d.createEcho || d.draw > 0 || d.goldGain > 0)) score += 12;
            if (s.relics.contains("bloodspark_contract") && (d.hpLoss > 0 || d.createWound || d.goldGain > 0 || d.burn > 0 || d.vulnerable > 0)) score += 14;
            if (s.relics.contains("contract_stamp") && (d.goldGain > 0 || d.goldDamage || d.goldBlock || d.skillChargeGain > 0 || d.type == 1)) score += 16;
            if (s.relics.contains("grand_ledger") && (d.goldGain > 0 || d.hpLoss > 0 || d.createWound || d.vulnerable > 0 || d.bind > 0 || d.skillChargeGain > 0 || d.rarity == 2)) score += 18;
            if (s.relics.contains("storm_rod") && (isStormcallerSignal(d) || d.burn > 0 || d.vulnerable > 0 || d.skillChargeGain > 0)) score += 18;
            if (s.relics.contains("tempest_crown") && (isStormcallerSignal(d) || d.aoe || d.rarity == 2)) score += 20;
            if (s.relics.contains("shadow_sash") && (isShadowdancerSignal(d) || d.comboDamage > 0 || d.skillChargeGain > 0)) score += 18;
            if (s.relics.contains("eclipse_mask") && (isShadowdancerSignal(d) || d.createEcho || d.exhaust || d.rarity == 2)) score += 20;
            if (s.relics.contains("rune_stylus") && (isRunebladeSignal(d) || d.upgradeRandom || d.scry > 0 || d.skillChargeGain > 0)) score += 18;
            if (s.relics.contains("grand_rune_blade") && (isRunebladeSignal(d) || d.rarity == 2 || d.upgradeRandom)) score += 20;
            if (s.relics.contains("spirit_planchette") && (isMediumSignal(d) || d.createEcho || d.exhaust || d.scry > 0 || d.bind > 0)) score += 18;
            if (s.relics.contains("ancestral_planchette") && (isMediumSignal(d) || d.rarity == 2 || d.skillChargeGain > 0 || d.bind > 0)) score += 20;
            if (s.relics.contains("war_table") && (isTacticianSignal(d) || d.block > 0 || d.scry > 0
                    || d.upgradeRandom || d.skillChargeGain > 0 || d.vulnerable > 0)) score += 18;
            if (s.relics.contains("grand_war_room") && (isTacticianSignal(d) || d.rarity == 2
                    || d.upgradeRandom || d.skillChargeGain > 0 || d.block > 0)) score += 20;
            if (s.relics.contains("refraction_dial") && (isPrismistSignal(d) || isHybridCore(d)
                    || isConfluenceCore(d) || d.scry > 0 || d.upgradeRandom || d.skillChargeGain > 0
                    || d.draw > 0)) score += 18;
            if (s.relics.contains("spectrum_crown") && (isPrismistSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.vulnerable > 0 || isHybridCore(d))) score += 20;
            if (s.relics.contains("dreamcatcher_charm") && (isDreamwalkerSignal(d) || d.scry > 0
                    || d.exhaust || d.createEcho || d.skillChargeGain > 0 || d.bind > 0)) score += 18;
            if (s.relics.contains("oneiric_crown") && (isDreamwalkerSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.bind > 0 || d.createEcho)) score += 20;
            if (s.relics.contains("seed_satchel") && (isGardenerSignal(d) || d.heal > 0
                    || d.gainWildEngine > 0 || d.block > 0 || d.bind > 0)) score += 18;
            if (s.relics.contains("verdant_crown") && (isGardenerSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.heal > 0 || d.gainWildEngine > 0 || d.bind > 0)) score += 20;
            if (s.relics.contains("recipe_book") && (isChefSignal(d) || d.heal > 0
                    || d.createPotion || d.burn > 0 || d.bind > 0)) score += 18;
            if (s.relics.contains("banquet_crown") && (isChefSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.heal > 0 || d.createPotion || d.burn > 0 || d.bind > 0)) score += 20;
            if (s.relics.contains("songbook") && (isBardSignal(d) || d.createEcho
                    || d.draw > 0 || d.energyGain > 0 || d.vulnerable > 0 || d.bind > 0)) score += 18;
            if (s.relics.contains("finale_crown") && (isBardSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.createEcho || d.draw > 0 || d.vulnerable > 0)) score += 20;
            if (s.relics.contains("mirror_lens") && (isMirroristSignal(d) || d.scry > 0
                    || d.upgradeRandom || d.createEcho || d.draw > 0 || d.vulnerable > 0)) score += 18;
            if (s.relics.contains("mirror_crown") && (isMirroristSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.upgradeRandom || d.createEcho || d.vulnerable > 0
                    || isHybridCore(d))) score += 20;
            if (s.relics.contains("string_spool") && (isPuppeteerSignal(d) || d.bind > 0
                    || d.createEcho || d.draw > 0 || d.block > 0 || d.skillChargeGain > 0)) score += 18;
            if (s.relics.contains("marionette_crown") && (isPuppeteerSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.createEcho || d.bind > 0 || d.block > 0)) score += 20;
            if (s.relics.contains("scrap_magnet") && (isScavengerSignal(d) || d.draw > 0 || d.goldGain > 0
                    || d.exhaust || d.createWound || d.block > 0 || d.heal > 0 || d.skillChargeGain > 0)) score += 18;
            if (s.relics.contains("scrap_king_crown") && (isScavengerSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.draw > 0 || d.goldGain > 0 || d.vulnerable > 0 || d.bind > 0)) score += 20;
            if (s.relics.contains("faultline_core") && (isGeomancerSignal(d) || d.block > 0
                    || d.upgradeRandom || d.scry > 0 || isHybridCore(d))) score += 18;
            if (s.relics.contains("tectonic_crown") && (isGeomancerSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.vulnerable > 0 || isHybridCore(d))) score += 20;
            if (s.relics.contains("witch_bottle") && (isWitchSignal(d) || d.createPotion || d.burn > 0
                    || d.bind > 0 || d.createEcho || d.heal > 0)) score += 18;
            if (s.relics.contains("witch_moon_crown") && (isWitchSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.createPotion || d.createEcho)) score += 20;
            if (s.relics.contains("phase_lens") && (isShifterSignal(d) || d.cost == 0
                    || d.draw > 0 || d.createEcho || d.energyGain > 0)) score += 18;
            if (s.relics.contains("phase_crown") && (isShifterSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.createEcho || d.energyGain > 0)) score += 20;
            if (s.relics.contains("fate_lantern") && (isFateseerSignal(d) || d.scry > 0
                    || d.upgradeRandom || d.draw > 0 || isHybridCore(d))) score += 18;
            if (s.relics.contains("fate_crown") && (isFateseerSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.upgradeRandom || isHybridCore(d))) score += 20;
            if (s.relics.contains("tide_shell") && (isTidecallerSignal(d) || d.block > 0
                    || d.bind > 0 || d.draw > 0)) score += 18;
            if (s.relics.contains("tide_crown") && (isTidecallerSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.block > 0 || d.bind > 0)) score += 20;
            if (s.relics.contains("frost_chain") && (isFrostbinderSignal(d) || d.block > 0
                    || d.bind > 0 || d.draw > 0 || d.exhaust || d.createWound)) score += 18;
            if (s.relics.contains("frost_crown") && (isFrostbinderSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.block > 0 || d.bind > 0)) score += 20;
            if (s.relics.contains("plague_case") && (isPlaguedoctorSignal(d) || d.createPotion
                    || d.burn > 0 || d.bind > 0 || d.heal > 0 || d.createWound)) score += 18;
            if (s.relics.contains("plague_crown") && (isPlaguedoctorSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.createPotion || d.burn > 0 || d.bind > 0)) score += 20;
            if (s.relics.contains("archive_key") && (isArchivistSignal(d) || d.scry > 0
                    || d.upgradeRandom || d.draw > 0 || d.exhaustTopDiscard)) score += 18;
            if (s.relics.contains("archive_crown") && (isArchivistSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.upgradeRandom || d.vulnerable > 0 || d.bind > 0)) score += 20;
            if (s.relics.contains("void_compass") && (isVoidnavigatorSignal(d) || d.createEcho
                    || d.exhaust || d.scry > 0 || d.draw > 0)) score += 18;
            if (s.relics.contains("void_crown") && (isVoidnavigatorSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.createEcho || d.exhaust || d.vulnerable > 0)) score += 20;
            if (s.relics.contains("relic_chisel") && (isRelicsmithSignal(d) || d.goldGain > 0
                    || d.upgradeRandom || d.block > 0 || d.draw > 0)) score += 18;
            if (s.relics.contains("vault_crown") && (isRelicsmithSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.goldGain > 0 || d.upgradeRandom || d.vulnerable > 0)) score += 20;
            if (s.relics.contains("pathfinder_compass") && (isPathfinderSignal(d) || d.scry > 0
                    || d.draw > 0 || d.block > 0 || d.skillChargeGain > 0)) score += 18;
            if (s.relics.contains("route_crown") && (isPathfinderSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.upgradeRandom || d.bind > 0)) score += 20;
            if (s.relics.contains("array_disc") && (isArrayistSignal(d) || d.cost == 0
                    || d.draw > 0 || d.createEcho || d.block > 0 || d.skillChargeGain > 0)) score += 18;
            if (s.relics.contains("array_crown") && (isArrayistSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.upgradeRandom || d.createEcho)) score += 20;
            if (s.relics.contains("gambit_clock") && (isGambiterSignal(d) || d.cost == 0
                    || d.draw > 0 || d.block > 0 || d.bind > 0 || d.skillChargeGain > 0)) score += 18;
            if (s.relics.contains("checkmate_crown") && (isGambiterSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.upgradeRandom || d.bind > 0)) score += 20;
            if (s.relics.contains("grave_lantern") && (isGravekeeperSignal(d) || d.exhaust || d.exhaustTopDiscard
                    || d.createWound || d.heal > 0 || d.block > 0 || d.draw > 0 || d.skillChargeGain > 0)) score += 18;
            if (s.relics.contains("requiem_crown") && (isGravekeeperSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.exhaust || d.exhaustTopDiscard || d.bind > 0
                    || d.vulnerable > 0)) score += 20;
            if (s.relics.contains("treasury_key") && (isTreasurerSignal(d) || d.goldGain > 0
                    || d.goldDamage || d.goldBlock || d.block > 0 || d.draw > 0 || d.skillChargeGain > 0)) score += 18;
            if (s.relics.contains("audit_crown") && (isTreasurerSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.upgradeRandom || d.goldGain > 0 || d.bind > 0
                    || d.vulnerable > 0)) score += 20;
            if (s.relics.contains("clockwork_key") && (isClocksmithSignal(d) || d.cost == 0
                    || d.draw > 0 || d.createEcho || d.upgradeRandom || d.skillChargeGain > 0)) score += 18;
            if (s.relics.contains("chrono_crown") && (isClocksmithSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.upgradeRandom || d.bind > 0
                    || d.vulnerable > 0)) score += 20;
            if (s.relics.contains("mint_tongs") && (isMintsmithSignal(d) || d.goldGain > 0
                    || d.goldDamage || d.goldBlock || d.burn > 0 || d.block > 0 || d.skillChargeGain > 0)) score += 18;
            if (s.relics.contains("mint_crown") && (isMintsmithSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.upgradeRandom || d.goldGain > 0 || d.burn > 0
                    || d.vulnerable > 0)) score += 20;
            if (s.relics.contains("rift_pass") && (isDrifterSignal(s, d) || isOffPoolCard(s, d)
                    || d.createEcho || d.draw > 0 || d.block > 0 || d.skillChargeGain > 0)) score += 18;
            if (s.relics.contains("junction_crown") && (isDrifterSignal(s, d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.upgradeRandom || d.createEcho
                    || d.vulnerable > 0 || d.bind > 0)) score += 20;
            if (s.relics.contains("oath_seal") && (isOathkeeperSignal(d) || d.block > 0
                    || d.heal > 0 || d.draw > 0 || d.skillChargeGain > 0)) score += 18;
            if (s.relics.contains("judgment_crown") && (isOathkeeperSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.upgradeRandom || d.block > 0 || d.heal > 0
                    || d.vulnerable > 0 || d.bind > 0)) score += 20;
            if (s.relics.contains("moon_lyre") && (isMoonsingerSignal(d) || d.scry > 0
                    || d.draw > 0 || d.createEcho || d.skillChargeGain > 0)) score += 18;
            if (s.relics.contains("eclipse_crown") && (isMoonsingerSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.upgradeRandom || d.createEcho || d.bind > 0
                    || d.vulnerable > 0)) score += 20;
            if (s.relics.contains("cipher_ring") && (isSpySignal(d) || d.cost == 0
                    || d.draw > 0 || d.goldGain > 0 || d.createEcho || d.skillChargeGain > 0)) score += 18;
            if (s.relics.contains("mastermind_crown") && (isSpySignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.upgradeRandom || d.goldGain > 0 || d.bind > 0
                    || d.vulnerable > 0)) score += 20;
            if (s.relics.contains("scent_vial") && (isPerfumerSignal(d) || d.createPotion
                    || d.heal > 0 || d.burn > 0 || d.draw > 0 || d.skillChargeGain > 0)) score += 18;
            if (s.relics.contains("bouquet_crown") && (isPerfumerSignal(d) || d.rarity == 2
                    || d.skillChargeGain > 0 || d.upgradeRandom || d.burn > 0 || d.bind > 0
                    || d.vulnerable > 0)) score += 20;
            if (s.relics.contains("salvage_hook") && isSalvageSignal(d)) score += 18;
            if (s.relics.contains("mosaic_core") && isHybridCore(d)) score += 16;
            if (s.relics.contains("starforge_lens") && (isHybridCore(d) || d.skillChargeGain > 0 || d.upgradeRandom || d.scry > 0)) score += 16;
            if (s.relics.contains("confluence_map") && isHybridCore(d)) score += 18;
            if (s.relics.contains("prism_gear") && (isHybridCore(d) || isConfluenceCore(d))) score += 20;
            if (s.relics.contains("resonance_prism") && (isHybridCore(d) || isConfluenceCore(d) || specSignal >= 4)) score += 20;
            if (s.relics.contains("hybrid_keystone") && (isHybridCore(d) || isConfluenceCore(d) || specSignal >= 4)) score += 20;
            if (s.relics.contains("cascade_lattice") && isCascadeSignal(d)) score += 20;
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
            int coreFocus = activeBuildCoreFocus(s);
            int coreRelicSignal = buildCoreRelicSignal(coreFocus, id);
            if (coreRelicSignal > 0) {
                score += 12 + coreRelicSignal * 7;
            }
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
            if (GameCore.PROF_MACHINIST.equals(s.profession) && ("clockwork_core".equals(id) || "gyro_wrench".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "starforge_lens".equals(id)
                    || "prism_gear".equals(id) || "confluence_map".equals(id))) score += 34;
            if (GameCore.PROF_CHRONOMANCER.equals(s.profession) && ("time_engine".equals(id) || "hourglass_charm".equals(id)
                    || "tempo_metronome".equals(id) || "echo_ledger".equals(id) || "tuning_fork".equals(id)
                    || "moon_lantern".equals(id) || "confluence_map".equals(id) || "overload_etch".equals(id))) score += 34;
            if (GameCore.PROF_PACTMAKER.equals(s.profession) && ("grand_ledger".equals(id) || "contract_stamp".equals(id)
                    || "trial_ledger".equals(id) || "bloodcoin_broach".equals(id) || "curse_censer".equals(id)
                    || "ledger_stamp".equals(id) || "golden_throne".equals(id) || "kingmaker_seal".equals(id))) score += 36;
            if (GameCore.PROF_STORMCALLER.equals(s.profession) && ("tempest_crown".equals(id) || "storm_rod".equals(id)
                    || "stormglass_seal".equals(id) || "charcoal_sigil".equals(id) || "cinder_spoon".equals(id)
                    || "emberroot_charm".equals(id) || "tempo_metronome".equals(id) || "markchain_seal".equals(id)
                    || "pressure_gauge".equals(id) || "overload_etch".equals(id) || "confluence_map".equals(id) || "tuning_fork".equals(id))) score += 36;
            if (GameCore.PROF_SHADOWDANCER.equals(s.profession) && ("eclipse_mask".equals(id) || "shadow_sash".equals(id)
                    || "tempo_metronome".equals(id) || "amber_quill".equals(id) || "void_abacus".equals(id)
                    || "echo_ledger".equals(id) || "tuning_fork".equals(id) || "markchain_seal".equals(id)
                    || "overload_etch".equals(id) || "confluence_map".equals(id) || "echoflow_charm".equals(id))) score += 36;
            if (GameCore.PROF_RUNEBLADE.equals(s.profession) && ("grand_rune_blade".equals(id) || "rune_stylus".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "starforge_lens".equals(id)
                    || "overload_etch".equals(id) || "markchain_seal".equals(id) || "stormglass_seal".equals(id)
                    || "pressure_gauge".equals(id) || "confluence_map".equals(id) || "prism_gear".equals(id) || "discipline_chart".equals(id))) score += 36;
            if (GameCore.PROF_MEDIUM.equals(s.profession) && ("ancestral_planchette".equals(id) || "spirit_planchette".equals(id)
                    || "void_abacus".equals(id) || "echo_ledger".equals(id) || "echoflow_charm".equals(id)
                    || "markchain_seal".equals(id) || "spirit_bell".equals(id) || "spirit_processional".equals(id)
                    || "pressure_gauge".equals(id) || "starforge_lens".equals(id) || "overload_etch".equals(id) || "confluence_map".equals(id)
                    || "prism_gear".equals(id) || "discipline_chart".equals(id) || "tempo_metronome".equals(id))) score += 36;
            if (GameCore.PROF_TACTICIAN.equals(s.profession) && ("grand_war_room".equals(id) || "war_table".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "starforge_lens".equals(id)
                    || "overload_etch".equals(id) || "markchain_seal".equals(id) || "stormglass_seal".equals(id)
                    || "pressure_gauge".equals(id) || "confluence_map".equals(id) || "prism_gear".equals(id) || "discipline_chart".equals(id)
                    || "tempo_metronome".equals(id) || "assembly_frame".equals(id))) score += 36;
            if (GameCore.PROF_PRISMIST.equals(s.profession) && ("spectrum_crown".equals(id) || "refraction_dial".equals(id)
                    || "confluence_map".equals(id) || "prism_gear".equals(id) || "mosaic_core".equals(id)
                    || "starforge_lens".equals(id) || "mirror_anvil".equals(id) || "polished_cog".equals(id)
                    || "overload_etch".equals(id) || "markchain_seal".equals(id) || "stormglass_seal".equals(id)
                    || "pressure_gauge".equals(id) || "discipline_chart".equals(id) || "tempo_metronome".equals(id) || "echoflow_charm".equals(id)
                    || "assembly_frame".equals(id))) score += 36;
            if (GameCore.PROF_DREAMWALKER.equals(s.profession) && ("oneiric_crown".equals(id) || "dreamcatcher_charm".equals(id)
                    || "void_abacus".equals(id) || "echo_ledger".equals(id) || "echoflow_charm".equals(id)
                    || "markchain_seal".equals(id) || "curse_censer".equals(id) || "hex_moon".equals(id)
                    || "pressure_gauge".equals(id) || "starforge_lens".equals(id) || "overload_etch".equals(id) || "discipline_chart".equals(id)
                    || "stormglass_seal".equals(id) || "tempo_metronome".equals(id))) score += 36;
            if (GameCore.PROF_GARDENER.equals(s.profession) && ("verdant_crown".equals(id) || "seed_satchel".equals(id)
                    || "vital_sprout".equals(id) || "leaf_charm".equals(id) || "emberroot_charm".equals(id)
                    || "markchain_seal".equals(id) || "overload_etch".equals(id) || "discipline_chart".equals(id)
                    || "pressure_gauge".equals(id) || "stormglass_seal".equals(id) || "tempo_metronome".equals(id) || "tuning_fork".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "starforge_lens".equals(id)
                    || "confluence_map".equals(id) || "prism_gear".equals(id))) score += 36;
            if (GameCore.PROF_CHEF.equals(s.profession) && ("banquet_crown".equals(id) || "recipe_book".equals(id)
                    || "glass_vials".equals(id) || "cinder_spoon".equals(id) || "green_bell".equals(id)
                    || "emberroot_charm".equals(id) || "markchain_seal".equals(id) || "overload_etch".equals(id)
                    || "pressure_gauge".equals(id) || "discipline_chart".equals(id) || "stormglass_seal".equals(id) || "tempo_metronome".equals(id)
                    || "tuning_fork".equals(id) || "mirror_anvil".equals(id) || "polished_cog".equals(id)
                    || "starforge_lens".equals(id) || "confluence_map".equals(id) || "prism_gear".equals(id))) score += 36;
            if (GameCore.PROF_BARD.equals(s.profession) && ("finale_crown".equals(id) || "songbook".equals(id)
                    || "tempo_metronome".equals(id) || "echo_ledger".equals(id) || "void_abacus".equals(id)
                    || "tuning_fork".equals(id) || "markchain_seal".equals(id) || "overload_etch".equals(id)
                    || "pressure_gauge".equals(id) || "discipline_chart".equals(id) || "stormglass_seal".equals(id) || "echoflow_charm".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "starforge_lens".equals(id)
                    || "confluence_map".equals(id) || "prism_gear".equals(id))) score += 36;
            if (GameCore.PROF_MIRRORIST.equals(s.profession) && ("mirror_crown".equals(id) || "mirror_lens".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "starforge_lens".equals(id)
                    || "confluence_map".equals(id) || "prism_gear".equals(id) || "mosaic_core".equals(id)
                    || "tempo_metronome".equals(id) || "echo_ledger".equals(id) || "echoflow_charm".equals(id)
                    || "markchain_seal".equals(id) || "overload_etch".equals(id) || "discipline_chart".equals(id)
                    || "pressure_gauge".equals(id) || "stormglass_seal".equals(id))) score += 36;
            if (GameCore.PROF_PUPPETEER.equals(s.profession) && ("marionette_crown".equals(id) || "string_spool".equals(id)
                    || "root_drum".equals(id) || "green_bell".equals(id) || "spirit_bell".equals(id)
                    || "spirit_processional".equals(id) || "echo_ledger".equals(id) || "echoflow_charm".equals(id)
                    || "markchain_seal".equals(id) || "overload_etch".equals(id) || "discipline_chart".equals(id)
                    || "pressure_gauge".equals(id) || "stormglass_seal".equals(id) || "tempo_metronome".equals(id) || "confluence_map".equals(id)
                    || "prism_gear".equals(id))) score += 36;
            if (GameCore.PROF_SCAVENGER.equals(s.profession) && ("scrap_king_crown".equals(id) || "scrap_magnet".equals(id)
                    || "empty_coin".equals(id) || "void_abacus".equals(id) || "echo_ledger".equals(id)
                    || "bloodcoin_broach".equals(id) || "curse_censer".equals(id) || "stormglass_seal".equals(id)
                    || "tempo_metronome".equals(id) || "tuning_fork".equals(id) || "markchain_seal".equals(id)
                    || "overload_etch".equals(id) || "discipline_chart".equals(id) || "pressure_gauge".equals(id)
                    || "confluence_map".equals(id) || "prism_gear".equals(id) || "mosaic_core".equals(id)
                    || "starforge_lens".equals(id) || "salvage_hook".equals(id))) score += 36;
            if (GameCore.PROF_LIGHTKEEPER.equals(s.profession) && ("dawn_beacon".equals(id) || "lantern_wick".equals(id)
                    || "hollow_crown".equals(id) || "void_abacus".equals(id) || "echo_ledger".equals(id)
                    || "echoflow_charm".equals(id) || "mirror_anvil".equals(id) || "polished_cog".equals(id)
                    || "starforge_lens".equals(id) || "stormglass_seal".equals(id) || "markchain_seal".equals(id)
                    || "pressure_gauge".equals(id) || "overload_etch".equals(id) || "discipline_chart".equals(id)
                    || "salvage_hook".equals(id) || "confluence_map".equals(id) || "prism_gear".equals(id)
                    || "mosaic_core".equals(id))) score += 36;
            if (GameCore.PROF_GEOMANCER.equals(s.profession) && ("tectonic_crown".equals(id) || "faultline_core".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "split_anvil".equals(id)
                    || "starforge_lens".equals(id) || "stormglass_seal".equals(id) || "markchain_seal".equals(id)
                    || "pressure_gauge".equals(id) || "overload_etch".equals(id) || "discipline_chart".equals(id)
                    || "confluence_map".equals(id) || "prism_gear".equals(id) || "mosaic_core".equals(id)
                    || "resonance_prism".equals(id))) score += 36;
            if (GameCore.PROF_WITCH.equals(s.profession) && ("witch_moon_crown".equals(id) || "witch_bottle".equals(id)
                    || "glass_vials".equals(id) || "cinder_spoon".equals(id) || "green_bell".equals(id)
                    || "emberroot_charm".equals(id) || "stormglass_seal".equals(id) || "curse_censer".equals(id)
                    || "echoflow_charm".equals(id) || "markchain_seal".equals(id) || "pressure_gauge".equals(id)
                    || "overload_etch".equals(id) || "discipline_chart".equals(id) || "salvage_hook".equals(id)
                    || "void_abacus".equals(id) || "echo_ledger".equals(id) || "confluence_map".equals(id)
                    || "prism_gear".equals(id) || "mosaic_core".equals(id) || "resonance_prism".equals(id))) score += 36;
            if (GameCore.PROF_SHIFTER.equals(s.profession) && ("phase_crown".equals(id) || "phase_lens".equals(id)
                    || "tempo_metronome".equals(id) || "amber_quill".equals(id) || "void_abacus".equals(id)
                    || "echo_ledger".equals(id) || "echoflow_charm".equals(id) || "markchain_seal".equals(id)
                    || "pressure_gauge".equals(id) || "overload_etch".equals(id) || "discipline_chart".equals(id)
                    || "confluence_map".equals(id) || "prism_gear".equals(id) || "mosaic_core".equals(id)
                    || "starforge_lens".equals(id) || "resonance_prism".equals(id) || "salvage_hook".equals(id))) score += 36;
            if (GameCore.PROF_FATESEER.equals(s.profession) && ("fate_crown".equals(id) || "fate_lantern".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "tempo_metronome".equals(id)
                    || "markchain_seal".equals(id) || "pressure_gauge".equals(id) || "overload_etch".equals(id)
                    || "discipline_chart".equals(id) || "confluence_map".equals(id) || "prism_gear".equals(id)
                    || "mosaic_core".equals(id) || "starforge_lens".equals(id) || "resonance_prism".equals(id)
                    || "salvage_hook".equals(id) || "echo_ledger".equals(id) || "void_abacus".equals(id))) score += 36;
            if (GameCore.PROF_TIDECALLER.equals(s.profession) && ("tide_crown".equals(id) || "tide_shell".equals(id)
                    || "thorn_ring".equals(id) || "stormglass_seal".equals(id) || "green_bell".equals(id)
                    || "root_drum".equals(id) || "bulwark_core".equals(id) || "markchain_seal".equals(id)
                    || "pressure_gauge".equals(id) || "overload_etch".equals(id) || "discipline_chart".equals(id)
                    || "confluence_map".equals(id) || "prism_gear".equals(id) || "mosaic_core".equals(id)
                    || "starforge_lens".equals(id) || "resonance_prism".equals(id) || "echo_ledger".equals(id)
                    || "void_abacus".equals(id))) score += 36;
            if (GameCore.PROF_FROSTBINDER.equals(s.profession) && ("frost_crown".equals(id) || "frost_chain".equals(id)
                    || "thorn_ring".equals(id) || "hex_moon".equals(id) || "curse_censer".equals(id)
                    || "bulwark_core".equals(id) || "dreamcatcher_charm".equals(id) || "markchain_seal".equals(id)
                    || "pressure_gauge".equals(id) || "overload_etch".equals(id) || "discipline_chart".equals(id)
                    || "confluence_map".equals(id) || "prism_gear".equals(id) || "mosaic_core".equals(id)
                    || "starforge_lens".equals(id) || "resonance_prism".equals(id) || "echo_ledger".equals(id)
                    || "void_abacus".equals(id))) score += 36;
            if (GameCore.PROF_PLAGUEDOCTOR.equals(s.profession) && ("plague_crown".equals(id) || "plague_case".equals(id)
                    || "glass_vials".equals(id) || "cinder_spoon".equals(id) || "green_bell".equals(id)
                    || "emberroot_charm".equals(id) || "stormglass_seal".equals(id) || "curse_censer".equals(id)
                    || "echoflow_charm".equals(id) || "markchain_seal".equals(id) || "pressure_gauge".equals(id)
                    || "overload_etch".equals(id) || "discipline_chart".equals(id) || "salvage_hook".equals(id)
                    || "bulwark_core".equals(id) || "hex_moon".equals(id) || "void_abacus".equals(id)
                    || "echo_ledger".equals(id) || "confluence_map".equals(id) || "prism_gear".equals(id)
                    || "mosaic_core".equals(id) || "starforge_lens".equals(id) || "resonance_prism".equals(id))) score += 36;
            if (GameCore.PROF_ARCHIVIST.equals(s.profession) && ("archive_crown".equals(id) || "archive_key".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "split_anvil".equals(id)
                    || "tempo_metronome".equals(id) || "tuning_fork".equals(id) || "echo_ledger".equals(id)
                    || "echoflow_charm".equals(id) || "markchain_seal".equals(id) || "pressure_gauge".equals(id)
                    || "overload_etch".equals(id) || "discipline_chart".equals(id) || "salvage_hook".equals(id)
                    || "bulwark_core".equals(id) || "stormglass_seal".equals(id) || "void_abacus".equals(id)
                    || "confluence_map".equals(id) || "prism_gear".equals(id) || "mosaic_core".equals(id)
                    || "starforge_lens".equals(id) || "resonance_prism".equals(id))) score += 36;
            if (GameCore.PROF_VOIDNAVIGATOR.equals(s.profession) && ("void_crown".equals(id) || "void_compass".equals(id)
                    || "echo_prism".equals(id) || "void_abacus".equals(id) || "hollow_crown".equals(id)
                    || "rift_compass".equals(id) || "echo_ledger".equals(id) || "echoflow_charm".equals(id)
                    || "markchain_seal".equals(id) || "pressure_gauge".equals(id) || "overload_etch".equals(id)
                    || "discipline_chart".equals(id) || "confluence_map".equals(id) || "prism_gear".equals(id)
                    || "mosaic_core".equals(id) || "starforge_lens".equals(id) || "resonance_prism".equals(id)
                    || "phase_lens".equals(id) || "fate_lantern".equals(id))) score += 36;
            if (GameCore.PROF_RELICSMITH.equals(s.profession) && ("vault_crown".equals(id) || "relic_chisel".equals(id)
                    || "ledger_stamp".equals(id) || "golden_throne".equals(id) || "contract_stamp".equals(id)
                    || "grand_ledger".equals(id) || "mirror_anvil".equals(id) || "polished_cog".equals(id)
                    || "split_anvil".equals(id) || "markchain_seal".equals(id) || "pressure_gauge".equals(id)
                    || "overload_etch".equals(id) || "discipline_chart".equals(id) || "confluence_map".equals(id)
                    || "prism_gear".equals(id) || "mosaic_core".equals(id) || "starforge_lens".equals(id)
                    || "resonance_prism".equals(id) || "bulwark_core".equals(id))) score += 36;
            if (GameCore.PROF_BEASTMASTER.equals(s.profession) && ("alpha_crown".equals(id) || "beast_whistle".equals(id)
                    || "seed_satchel".equals(id) || "verdant_crown".equals(id) || "dreamcatcher_charm".equals(id)
                    || "markchain_seal".equals(id) || "pressure_gauge".equals(id) || "bulwark_core".equals(id)
                    || "confluence_map".equals(id) || "echo_ledger".equals(id) || "overload_etch".equals(id)
                    || "discipline_chart".equals(id) || "resonance_prism".equals(id) || "spirit_bell".equals(id)
                    || "root_drum".equals(id))) score += 36;
            if (GameCore.PROF_DRAGONBINDER.equals(s.profession) && ("elder_dragon_crown".equals(id) || "dragon_sigil".equals(id)
                    || "catalyst_pump".equals(id) || "solar_crucible".equals(id) || "stormglass_seal".equals(id)
                    || "emberroot_charm".equals(id) || "markchain_seal".equals(id) || "pressure_gauge".equals(id)
                    || "bulwark_core".equals(id) || "confluence_map".equals(id) || "echo_ledger".equals(id)
                    || "overload_etch".equals(id) || "discipline_chart".equals(id) || "resonance_prism".equals(id)
                    || "witch_bottle".equals(id) || "recipe_book".equals(id))) score += 36;
            if (GameCore.PROF_SOULBINDER.equals(s.profession) && ("soul_crown".equals(id) || "soul_lantern".equals(id)
                    || "spirit_planchette".equals(id) || "ancestral_planchette".equals(id) || "dreamcatcher_charm".equals(id)
                    || "void_abacus".equals(id) || "echo_ledger".equals(id) || "echoflow_charm".equals(id)
                    || "markchain_seal".equals(id) || "pressure_gauge".equals(id) || "bulwark_core".equals(id)
                    || "confluence_map".equals(id) || "overload_etch".equals(id) || "discipline_chart".equals(id)
                    || "resonance_prism".equals(id) || "plague_case".equals(id) || "frost_chain".equals(id))) score += 36;
            if (GameCore.PROF_STARFORGER.equals(s.profession) && ("star_crown".equals(id) || "star_hammer".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "split_anvil".equals(id)
                    || "starforge_lens".equals(id) || "stormglass_seal".equals(id) || "emberroot_charm".equals(id)
                    || "markchain_seal".equals(id) || "pressure_gauge".equals(id) || "bulwark_core".equals(id)
                    || "confluence_map".equals(id) || "prism_gear".equals(id) || "overload_etch".equals(id)
                    || "discipline_chart".equals(id) || "resonance_prism".equals(id) || "gyro_wrench".equals(id)
                    || "rune_stylus".equals(id))) score += 36;
            if (GameCore.PROF_PATHFINDER.equals(s.profession) && ("route_crown".equals(id) || "pathfinder_compass".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "starforge_lens".equals(id)
                    || "markchain_seal".equals(id) || "pressure_gauge".equals(id) || "bulwark_core".equals(id)
                    || "confluence_map".equals(id) || "prism_gear".equals(id) || "overload_etch".equals(id)
                    || "discipline_chart".equals(id) || "resonance_prism".equals(id) || "fate_lantern".equals(id)
                    || "war_table".equals(id))) score += 36;
            if (GameCore.PROF_ARRAYIST.equals(s.profession) && ("array_crown".equals(id) || "array_disc".equals(id)
                    || "tempo_metronome".equals(id) || "amber_quill".equals(id) || "echo_ledger".equals(id)
                    || "confluence_map".equals(id) || "prism_gear".equals(id) || "mosaic_core".equals(id)
                    || "starforge_lens".equals(id) || "resonance_prism".equals(id) || "tuning_fork".equals(id)
                    || "conductor_baton".equals(id) || "mirror_anvil".equals(id) || "polished_cog".equals(id)
                    || "bulwark_core".equals(id) || "echoflow_charm".equals(id) || "discipline_chart".equals(id)
                    || "overload_etch".equals(id))) score += 36;
            if (GameCore.PROF_GAMBITER.equals(s.profession) && ("checkmate_crown".equals(id) || "gambit_clock".equals(id)
                    || "tempo_metronome".equals(id) || "amber_quill".equals(id) || "flash_heel".equals(id)
                    || "war_table".equals(id) || "verdict_seal".equals(id) || "confluence_map".equals(id)
                    || "prism_gear".equals(id) || "mosaic_core".equals(id) || "starforge_lens".equals(id)
                    || "resonance_prism".equals(id) || "tuning_fork".equals(id) || "conductor_baton".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "bulwark_core".equals(id)
                    || "discipline_chart".equals(id) || "overload_etch".equals(id))) score += 36;
            if (GameCore.PROF_GRAVEKEEPER.equals(s.profession) && ("requiem_crown".equals(id) || "grave_lantern".equals(id)
                    || "void_abacus".equals(id) || "echo_ledger".equals(id) || "echoflow_charm".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "starforge_lens".equals(id)
                    || "stormglass_seal".equals(id) || "markchain_seal".equals(id) || "pressure_gauge".equals(id)
                    || "overload_etch".equals(id) || "discipline_chart".equals(id) || "salvage_hook".equals(id)
                    || "confluence_map".equals(id) || "prism_gear".equals(id) || "resonance_prism".equals(id)
                    || "spirit_planchette".equals(id) || "frost_chain".equals(id) || "plague_case".equals(id)
                    || "soul_lantern".equals(id))) score += 36;
            if (GameCore.PROF_TREASURER.equals(s.profession) && ("audit_crown".equals(id) || "treasury_key".equals(id)
                    || "ledger_stamp".equals(id) || "kingmaker_seal".equals(id) || "contract_stamp".equals(id)
                    || "grand_ledger".equals(id) || "relic_chisel".equals(id) || "vault_crown".equals(id)
                    || "golden_throne".equals(id) || "bloodcoin_broach".equals(id) || "tithe_box".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "split_anvil".equals(id)
                    || "markchain_seal".equals(id) || "pressure_gauge".equals(id) || "bulwark_core".equals(id)
                    || "confluence_map".equals(id) || "prism_gear".equals(id) || "mosaic_core".equals(id)
                    || "starforge_lens".equals(id) || "resonance_prism".equals(id) || "overload_etch".equals(id)
                    || "discipline_chart".equals(id))) score += 36;
            if (GameCore.PROF_DRIFTER.equals(s.profession) && ("junction_crown".equals(id) || "rift_pass".equals(id)
                    || "rift_compass".equals(id) || "void_compass".equals(id) || "phase_lens".equals(id)
                    || "route_crown".equals(id) || "array_disc".equals(id) || "confluence_map".equals(id)
                    || "prism_gear".equals(id) || "mosaic_core".equals(id) || "starforge_lens".equals(id)
                    || "resonance_prism".equals(id) || "echoflow_charm".equals(id) || "hybrid_keystone".equals(id)
                    || "discipline_chart".equals(id) || "overload_etch".equals(id) || "mirror_anvil".equals(id)
                    || "polished_cog".equals(id))) score += 36;
            if (GameCore.PROF_OATHKEEPER.equals(s.profession) && ("judgment_crown".equals(id) || "oath_seal".equals(id)
                    || "command_banner".equals(id) || "aegis_throne".equals(id) || "warden_plate".equals(id)
                    || "vital_sprout".equals(id) || "vigil_bloom".equals(id) || "bulwark_core".equals(id)
                    || "markchain_seal".equals(id) || "pressure_gauge".equals(id) || "stormglass_seal".equals(id)
                    || "war_table".equals(id) || "grand_war_room".equals(id) || "verdict_seal".equals(id)
                    || "judgment_codex".equals(id) || "mirror_anvil".equals(id) || "polished_cog".equals(id)
                    || "split_anvil".equals(id) || "lantern_wick".equals(id) || "dawn_beacon".equals(id)
                    || "discipline_chart".equals(id) || "overload_etch".equals(id) || "confluence_map".equals(id)
                    || "prism_gear".equals(id) || "mosaic_core".equals(id) || "starforge_lens".equals(id)
                    || "resonance_prism".equals(id))) score += 36;
            if (GameCore.PROF_MOONSINGER.equals(s.profession) && ("eclipse_crown".equals(id) || "moon_lyre".equals(id)
                    || "star_compass".equals(id) || "celestial_orrery".equals(id) || "dreamcatcher_charm".equals(id)
                    || "oneiric_crown".equals(id) || "songbook".equals(id) || "finale_crown".equals(id)
                    || "echo_prism".equals(id) || "echo_ledger".equals(id) || "echoflow_charm".equals(id)
                    || "hourglass_charm".equals(id) || "time_engine".equals(id) || "fate_lantern".equals(id)
                    || "fate_crown".equals(id) || "markchain_seal".equals(id) || "pressure_gauge".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "split_anvil".equals(id)
                    || "confluence_map".equals(id) || "prism_gear".equals(id) || "mosaic_core".equals(id)
                    || "starforge_lens".equals(id) || "resonance_prism".equals(id) || "discipline_chart".equals(id)
                    || "overload_etch".equals(id))) score += 36;
            if (GameCore.PROF_SPY.equals(s.profession) && ("mastermind_crown".equals(id) || "cipher_ring".equals(id)
                    || "flash_heel".equals(id) || "ledger_stamp".equals(id) || "kingmaker_seal".equals(id)
                    || "shadow_sash".equals(id) || "eclipse_mask".equals(id) || "scrap_magnet".equals(id)
                    || "scrap_king_crown".equals(id) || "echo_prism".equals(id) || "echo_ledger".equals(id)
                    || "echoflow_charm".equals(id) || "tithe_box".equals(id) || "golden_throne".equals(id)
                    || "markchain_seal".equals(id) || "pressure_gauge".equals(id) || "mirror_anvil".equals(id)
                    || "polished_cog".equals(id) || "split_anvil".equals(id) || "confluence_map".equals(id)
                    || "prism_gear".equals(id) || "mosaic_core".equals(id) || "starforge_lens".equals(id)
                    || "resonance_prism".equals(id) || "discipline_chart".equals(id) || "overload_etch".equals(id))) score += 36;
            if (GameCore.PROF_PERFUMER.equals(s.profession) && ("bouquet_crown".equals(id) || "scent_vial".equals(id)
                    || "catalyst_pump".equals(id) || "solar_crucible".equals(id) || "witch_bottle".equals(id)
                    || "witch_moon_crown".equals(id) || "recipe_book".equals(id) || "banquet_crown".equals(id)
                    || "plague_case".equals(id) || "plague_crown".equals(id) || "vital_sprout".equals(id)
                    || "vigil_bloom".equals(id) || "hex_moon".equals(id) || "stormglass_seal".equals(id)
                    || "markchain_seal".equals(id) || "pressure_gauge".equals(id) || "bloodspark_contract".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "split_anvil".equals(id)
                    || "confluence_map".equals(id) || "prism_gear".equals(id) || "mosaic_core".equals(id)
                    || "starforge_lens".equals(id) || "resonance_prism".equals(id) || "discipline_chart".equals(id)
                    || "overload_etch".equals(id))) score += 36;
            if (GameCore.PROF_CLOCKSMITH.equals(s.profession) && ("chrono_crown".equals(id) || "clockwork_key".equals(id)
                    || "tuning_fork".equals(id) || "conductor_baton".equals(id) || "hourglass_charm".equals(id)
                    || "time_engine".equals(id) || "gyro_wrench".equals(id) || "clockwork_core".equals(id)
                    || "echo_prism".equals(id) || "echo_ledger".equals(id) || "mirror_lens".equals(id)
                    || "mirror_crown".equals(id) || "fate_lantern".equals(id) || "fate_crown".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "split_anvil".equals(id)
                    || "prism_gear".equals(id) || "mosaic_core".equals(id) || "starforge_lens".equals(id)
                    || "resonance_prism".equals(id) || "discipline_chart".equals(id) || "overload_etch".equals(id)
                    || "cascade_lattice".equals(id))) score += 36;
            if (GameCore.PROF_MINTSMITH.equals(s.profession) && ("mint_crown".equals(id) || "mint_tongs".equals(id)
                    || "ledger_stamp".equals(id) || "treasury_key".equals(id) || "audit_crown".equals(id)
                    || "cinder_spoon".equals(id) || "solar_crucible".equals(id) || "scent_vial".equals(id)
                    || "bouquet_crown".equals(id) || "star_hammer".equals(id) || "star_crown".equals(id)
                    || "tithe_box".equals(id) || "kingmaker_seal".equals(id) || "bloodcoin_broach".equals(id)
                    || "mirror_anvil".equals(id) || "polished_cog".equals(id) || "split_anvil".equals(id)
                    || "markchain_seal".equals(id) || "pressure_gauge".equals(id) || "bulwark_core".equals(id)
                    || "confluence_map".equals(id) || "prism_gear".equals(id) || "mosaic_core".equals(id)
                    || "starforge_lens".equals(id) || "resonance_prism".equals(id)
                    || "discipline_chart".equals(id) || "overload_etch".equals(id))) score += 36;
            if ("confluence_map".equals(id) || "prism_gear".equals(id) || "mosaic_core".equals(id)
                    || "starforge_lens".equals(id) || "resonance_prism".equals(id)) score += 28;
            if ("rift_pass".equals(id) || "junction_crown".equals(id)) score += 20;
            if ("oath_seal".equals(id) || "judgment_crown".equals(id)) score += 20;
            if ("moon_lyre".equals(id) || "eclipse_crown".equals(id)) score += 20;
            if ("cipher_ring".equals(id) || "mastermind_crown".equals(id)) score += 20;
            if ("scent_vial".equals(id) || "bouquet_crown".equals(id)) score += 20;
            if ("clockwork_key".equals(id) || "chrono_crown".equals(id)) score += 20;
            if ("mint_tongs".equals(id) || "mint_crown".equals(id)) score += 20;
            if ("split_anvil".equals(id) && (GameCore.PROF_WEAVER.equals(s.profession) || GameCore.PROF_INSCRIBER.equals(s.profession)
                    || GameCore.PROF_ALCHEMIST.equals(s.profession) || GameCore.PROF_HEXER.equals(s.profession)
                    || GameCore.PROF_RUNEBLADE.equals(s.profession) || GameCore.PROF_TACTICIAN.equals(s.profession)
                    || GameCore.PROF_PRISMIST.equals(s.profession) || GameCore.PROF_CHEF.equals(s.profession)
                    || GameCore.PROF_BARD.equals(s.profession) || GameCore.PROF_MIRRORIST.equals(s.profession)
                    || GameCore.PROF_PUPPETEER.equals(s.profession) || GameCore.PROF_SCAVENGER.equals(s.profession)
                    || GameCore.PROF_LIGHTKEEPER.equals(s.profession) || GameCore.PROF_GEOMANCER.equals(s.profession)
                    || GameCore.PROF_WITCH.equals(s.profession))) score += 28;
            if ("echo_ledger".equals(id) && (GameCore.PROF_ARCANIST.equals(s.profession) || GameCore.PROF_SUMMONER.equals(s.profession)
                    || GameCore.PROF_DUELIST.equals(s.profession) || GameCore.PROF_MERCHANT.equals(s.profession)
                    || GameCore.PROF_SHADOWDANCER.equals(s.profession) || GameCore.PROF_MEDIUM.equals(s.profession)
                    || GameCore.PROF_BARD.equals(s.profession) || GameCore.PROF_MIRRORIST.equals(s.profession)
                    || GameCore.PROF_PUPPETEER.equals(s.profession) || GameCore.PROF_SCAVENGER.equals(s.profession)
                    || GameCore.PROF_LIGHTKEEPER.equals(s.profession))) score += 28;
            if ("bloodspark_contract".equals(id) && (GameCore.PROF_BLOODBOUND.equals(s.profession) || GameCore.PROF_MERCHANT.equals(s.profession)
                    || GameCore.PROF_HEXER.equals(s.profession) || GameCore.PROF_ALCHEMIST.equals(s.profession)
                    || GameCore.PROF_CHEF.equals(s.profession))) score += 28;
            if ("contract_stamp".equals(id) || "grand_ledger".equals(id)) score += 18;
            if ("storm_rod".equals(id) || "tempest_crown".equals(id)) score += 20;
            if ("shadow_sash".equals(id) || "eclipse_mask".equals(id)) score += 20;
            if ("rune_stylus".equals(id) || "grand_rune_blade".equals(id)) score += 20;
            if ("spirit_planchette".equals(id) || "ancestral_planchette".equals(id)) score += 20;
            if ("war_table".equals(id) || "grand_war_room".equals(id)) score += 20;
            if ("refraction_dial".equals(id) || "spectrum_crown".equals(id)) score += 20;
            if ("dreamcatcher_charm".equals(id) || "oneiric_crown".equals(id)) score += 20;
            if ("seed_satchel".equals(id) || "verdant_crown".equals(id)) score += 20;
            if ("recipe_book".equals(id) || "banquet_crown".equals(id)) score += 20;
            if ("songbook".equals(id) || "finale_crown".equals(id)) score += 20;
            if ("mirror_lens".equals(id) || "mirror_crown".equals(id)) score += 20;
            if ("string_spool".equals(id) || "marionette_crown".equals(id)) score += 20;
            if ("scrap_magnet".equals(id) || "scrap_king_crown".equals(id)) score += 20;
            if ("lantern_wick".equals(id) || "dawn_beacon".equals(id)) score += 20;
            if ("faultline_core".equals(id) || "tectonic_crown".equals(id)) score += 20;
            if ("witch_bottle".equals(id) || "witch_moon_crown".equals(id)) score += 20;
            if ("phase_lens".equals(id) || "phase_crown".equals(id)) score += 20;
            if ("fate_lantern".equals(id) || "fate_crown".equals(id)) score += 20;
            if ("tide_shell".equals(id) || "tide_crown".equals(id)) score += 20;
            if ("frost_chain".equals(id) || "frost_crown".equals(id)) score += 20;
            if ("plague_case".equals(id) || "plague_crown".equals(id)) score += 20;
            if ("archive_key".equals(id) || "archive_crown".equals(id)) score += 20;
            if ("void_compass".equals(id) || "void_crown".equals(id)) score += 20;
            if ("relic_chisel".equals(id) || "vault_crown".equals(id)) score += 20;
            if ("pathfinder_compass".equals(id) || "route_crown".equals(id)) score += 20;
            if ("array_disc".equals(id) || "array_crown".equals(id)) score += 20;
            if ("gambit_clock".equals(id) || "checkmate_crown".equals(id)) score += 20;
            if ("grave_lantern".equals(id) || "requiem_crown".equals(id)) score += 20;
            if ("treasury_key".equals(id) || "audit_crown".equals(id)) score += 20;
            if ("salvage_hook".equals(id)) score += 20;
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
            else if ("pact_brewer".equals(id)) score += (GameCore.PROF_ALCHEMIST.equals(s.profession)
                    || GameCore.PROF_CHEF.equals(s.profession) || GameCore.PROF_WITCH.equals(s.profession)) ? 38 : 16;
            else if ("pact_hunter".equals(id)) score += GameCore.PROF_RANGER.equals(s.profession) ? 32 : 20;
            else if ("pact_void".equals(id)) score += GameCore.PROF_ARCANIST.equals(s.profession) || GameCore.PROF_WEAVER.equals(s.profession) ? 36 : 16;
            else if ("pact_blood".equals(id)) score += GameCore.PROF_BLOODBOUND.equals(s.profession) ? 40 : 10;
            else if ("pact_summon".equals(id)) score += GameCore.PROF_SUMMONER.equals(s.profession) || GameCore.PROF_ARCANIST.equals(s.profession) ? 38 : 18;
            else if ("pact_hex".equals(id)) score += GameCore.PROF_HEXER.equals(s.profession) || GameCore.PROF_BLOODBOUND.equals(s.profession) ? 38 : 14;
            else if ("pact_forge".equals(id)) score += GameCore.PROF_WEAVER.equals(s.profession) || GameCore.PROF_WARDEN.equals(s.profession)
                    || GameCore.PROF_INSCRIBER.equals(s.profession) ? 36 : 18;
            else if ("pact_merchant".equals(id)) score += GameCore.PROF_MERCHANT.equals(s.profession) ? 40 : 16;
            else if ("pact_specialist".equals(id)) score += 28;
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
            if (GameCore.PROF_MACHINIST.equals(s.profession) && ("pact_forge".equals(id) || "pact_confluence".equals(id)
                    || "pact_sprinter".equals(id) || "pact_hunter".equals(id))) score += 18;
            if (GameCore.PROF_CHRONOMANCER.equals(s.profession) && ("pact_sprinter".equals(id) || "pact_void".equals(id)
                    || "pact_confluence".equals(id) || "pact_hunter".equals(id))) score += 18;
            if (GameCore.PROF_PACTMAKER.equals(s.profession) && ("pact_merchant".equals(id) || "pact_blood".equals(id)
                    || "pact_hex".equals(id) || "pact_suppression".equals(id) || "pact_guardian".equals(id)
                    || "pact_hunter".equals(id))) score += 24;
            if (GameCore.PROF_STORMCALLER.equals(s.profession) && ("pact_sprinter".equals(id) || "pact_hunter".equals(id)
                    || "pact_brewer".equals(id) || "pact_suppression".equals(id) || "pact_confluence".equals(id))) score += 24;
            if (GameCore.PROF_SHADOWDANCER.equals(s.profession) && ("pact_sprinter".equals(id) || "pact_void".equals(id)
                    || "pact_hunter".equals(id) || "pact_suppression".equals(id) || "pact_confluence".equals(id))) score += 24;
            if (GameCore.PROF_RUNEBLADE.equals(s.profession) && ("pact_forge".equals(id) || "pact_suppression".equals(id)
                    || "pact_confluence".equals(id) || "pact_hunter".equals(id) || "pact_guardian".equals(id))) score += 24;
            if (GameCore.PROF_MEDIUM.equals(s.profession) && ("pact_void".equals(id) || "pact_summon".equals(id)
                    || "pact_suppression".equals(id) || "pact_confluence".equals(id) || "pact_hunter".equals(id))) score += 24;
            if (GameCore.PROF_TACTICIAN.equals(s.profession) && ("pact_guardian".equals(id) || "pact_forge".equals(id)
                    || "pact_suppression".equals(id) || "pact_confluence".equals(id) || "pact_hunter".equals(id))) score += 24;
            if (GameCore.PROF_PRISMIST.equals(s.profession) && ("pact_confluence".equals(id) || "pact_forge".equals(id)
                    || "pact_suppression".equals(id) || "pact_hunter".equals(id) || "pact_void".equals(id)
                    || "pact_sprinter".equals(id))) score += 24;
            if (GameCore.PROF_DREAMWALKER.equals(s.profession) && ("pact_void".equals(id) || "pact_suppression".equals(id)
                    || "pact_sprinter".equals(id) || "pact_hunter".equals(id) || "pact_hex".equals(id)
                    || "pact_confluence".equals(id))) score += 24;
            if (GameCore.PROF_GARDENER.equals(s.profession) && ("pact_guardian".equals(id) || "pact_hunter".equals(id)
                    || "pact_suppression".equals(id) || "pact_hex".equals(id) || "pact_confluence".equals(id)
                    || "pact_forge".equals(id))) score += 24;
            if (GameCore.PROF_CHEF.equals(s.profession) && ("pact_brewer".equals(id) || "pact_hex".equals(id)
                    || "pact_hunter".equals(id) || "pact_guardian".equals(id) || "pact_suppression".equals(id)
                    || "pact_confluence".equals(id))) score += 24;
            if (GameCore.PROF_BARD.equals(s.profession) && ("pact_sprinter".equals(id) || "pact_void".equals(id)
                    || "pact_suppression".equals(id) || "pact_confluence".equals(id) || "pact_hunter".equals(id)
                    || "pact_guardian".equals(id))) score += 24;
            if (GameCore.PROF_MIRRORIST.equals(s.profession) && ("pact_forge".equals(id) || "pact_confluence".equals(id)
                    || "pact_void".equals(id) || "pact_sprinter".equals(id) || "pact_suppression".equals(id)
                    || "pact_hunter".equals(id) || "pact_guardian".equals(id))) score += 24;
            if (GameCore.PROF_PUPPETEER.equals(s.profession) && ("pact_summon".equals(id) || "pact_suppression".equals(id)
                    || "pact_guardian".equals(id) || "pact_void".equals(id) || "pact_hunter".equals(id)
                    || "pact_sprinter".equals(id) || "pact_confluence".equals(id))) score += 24;
            if (GameCore.PROF_SCAVENGER.equals(s.profession) && ("pact_merchant".equals(id) || "pact_hex".equals(id)
                    || "pact_sprinter".equals(id) || "pact_void".equals(id) || "pact_suppression".equals(id)
                    || "pact_hunter".equals(id) || "pact_confluence".equals(id) || "pact_guardian".equals(id))) score += 24;
            if (GameCore.PROF_LIGHTKEEPER.equals(s.profession) && ("pact_void".equals(id) || "pact_forge".equals(id)
                    || "pact_suppression".equals(id) || "pact_sprinter".equals(id) || "pact_confluence".equals(id)
                    || "pact_hunter".equals(id) || "pact_guardian".equals(id))) score += 24;
            if (GameCore.PROF_GEOMANCER.equals(s.profession) && ("pact_forge".equals(id) || "pact_suppression".equals(id)
                    || "pact_confluence".equals(id) || "pact_guardian".equals(id) || "pact_hunter".equals(id))) score += 24;
            if (GameCore.PROF_WITCH.equals(s.profession) && ("pact_brewer".equals(id) || "pact_hex".equals(id)
                    || "pact_void".equals(id) || "pact_suppression".equals(id) || "pact_confluence".equals(id)
                    || "pact_hunter".equals(id) || "pact_guardian".equals(id))) score += 24;
            if (GameCore.PROF_SHIFTER.equals(s.profession) && ("pact_sprinter".equals(id) || "pact_void".equals(id)
                    || "pact_suppression".equals(id) || "pact_confluence".equals(id) || "pact_hunter".equals(id)
                    || "pact_guardian".equals(id))) score += 24;
            if (GameCore.PROF_FATESEER.equals(s.profession) && ("pact_forge".equals(id) || "pact_confluence".equals(id)
                    || "pact_sprinter".equals(id) || "pact_suppression".equals(id) || "pact_hunter".equals(id)
                    || "pact_guardian".equals(id))) score += 24;
            if (GameCore.PROF_TIDECALLER.equals(s.profession) && ("pact_guardian".equals(id) || "pact_suppression".equals(id)
                    || "pact_sprinter".equals(id) || "pact_hunter".equals(id) || "pact_confluence".equals(id))) score += 24;
            if (GameCore.PROF_FROSTBINDER.equals(s.profession) && ("pact_hex".equals(id) || "pact_guardian".equals(id)
                    || "pact_suppression".equals(id) || "pact_void".equals(id) || "pact_hunter".equals(id)
                    || "pact_confluence".equals(id))) score += 24;
            if (GameCore.PROF_PLAGUEDOCTOR.equals(s.profession) && ("pact_brewer".equals(id) || "pact_hex".equals(id)
                    || "pact_suppression".equals(id) || "pact_confluence".equals(id) || "pact_hunter".equals(id)
                    || "pact_guardian".equals(id))) score += 24;
            if (GameCore.PROF_ARCHIVIST.equals(s.profession) && ("pact_forge".equals(id) || "pact_suppression".equals(id)
                    || "pact_confluence".equals(id) || "pact_guardian".equals(id) || "pact_sprinter".equals(id)
                    || "pact_void".equals(id) || "pact_hunter".equals(id))) score += 24;
            if (GameCore.PROF_VOIDNAVIGATOR.equals(s.profession) && ("pact_void".equals(id) || "pact_sprinter".equals(id)
                    || "pact_confluence".equals(id) || "pact_suppression".equals(id) || "pact_hunter".equals(id)
                    || "pact_guardian".equals(id))) score += 24;
            if (GameCore.PROF_RELICSMITH.equals(s.profession) && ("pact_merchant".equals(id) || "pact_forge".equals(id)
                    || "pact_guardian".equals(id) || "pact_confluence".equals(id) || "pact_suppression".equals(id)
                    || "pact_hunter".equals(id))) score += 24;
            if (GameCore.PROF_BEASTMASTER.equals(s.profession) && ("pact_summon".equals(id) || "pact_guardian".equals(id)
                    || "pact_suppression".equals(id) || "pact_void".equals(id) || "pact_hunter".equals(id)
                    || "pact_confluence".equals(id))) score += 24;
            if (GameCore.PROF_DRAGONBINDER.equals(s.profession) && ("pact_brewer".equals(id) || "pact_guardian".equals(id)
                    || "pact_suppression".equals(id) || "pact_void".equals(id) || "pact_hunter".equals(id)
                    || "pact_confluence".equals(id))) score += 24;
            if (GameCore.PROF_SOULBINDER.equals(s.profession) && ("pact_void".equals(id) || "pact_summon".equals(id)
                    || "pact_guardian".equals(id) || "pact_suppression".equals(id) || "pact_hex".equals(id)
                    || "pact_confluence".equals(id))) score += 24;
            if (GameCore.PROF_STARFORGER.equals(s.profession) && ("pact_forge".equals(id) || "pact_brewer".equals(id)
                    || "pact_guardian".equals(id) || "pact_suppression".equals(id) || "pact_confluence".equals(id)
                    || "pact_hunter".equals(id))) score += 24;
            if ("pact_specialist".equals(id) && (GameCore.PROF_PACTMAKER.equals(s.profession)
                    || GameCore.PROF_ADJUDICATOR.equals(s.profession) || GameCore.PROF_TUNER.equals(s.profession)
                    || GameCore.PROF_PRISMIST.equals(s.profession) || GameCore.PROF_RELICSMITH.equals(s.profession))) score += 10;
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
            String id = s.talentChoices.get(i);
            int score = GameCore.talentSynergyScore(s, id);
            if (s.skillSpec != null && s.skillSpec.length() > 0 && buildCoreFocus(id) >= 0) {
                score += 10 + Math.max(1, s.skillSpecLevel) * 4;
            }
            if (hasBuildCoreTalent(s) && isSpecFriendlyTalent(id)) {
                score += 4;
            }
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
            else if ("spec_hybrid".equals(id)) score += (GameCore.PROF_PRISMIST.equals(s.profession)
                    || GameCore.PROF_TACTICIAN.equals(s.profession) || GameCore.PROF_MACHINIST.equals(s.profession)
                    || GameCore.PROF_MIRRORIST.equals(s.profession) || GameCore.PROF_GEOMANCER.equals(s.profession)
                    || GameCore.PROF_FATESEER.equals(s.profession) || GameCore.PROF_VOIDNAVIGATOR.equals(s.profession)
                    || GameCore.PROF_RELICSMITH.equals(s.profession) || GameCore.PROF_STARFORGER.equals(s.profession)) ? 34 : 27;
            else if ("spec_cascade".equals(id)) score += (GameCore.PROF_TUNER.equals(s.profession)
                    || GameCore.PROF_SHADOWDANCER.equals(s.profession) || GameCore.PROF_CHRONOMANCER.equals(s.profession)
                    || GameCore.PROF_PRISMIST.equals(s.profession) || GameCore.PROF_BARD.equals(s.profession)
                    || GameCore.PROF_MIRRORIST.equals(s.profession) || GameCore.PROF_ARRAYIST.equals(s.profession)
                    || GameCore.PROF_DRIFTER.equals(s.profession) || GameCore.PROF_MOONSINGER.equals(s.profession)
                    || GameCore.PROF_SPY.equals(s.profession) || GameCore.PROF_PERFUMER.equals(s.profession)) ? 35 : 28;
            else if ("spec_tempo".equals(id)) score += 28;
            else if ("spec_burst".equals(id)) score += (GameCore.PROF_DUELIST.equals(s.profession) || GameCore.PROF_RANGER.equals(s.profession)
                    || GameCore.PROF_SHADOWDANCER.equals(s.profession)) ? 32 : 22;
            else if ("spec_sustain".equals(id)) score += (GameCore.PROF_WARDEN.equals(s.profession) || GameCore.PROF_BLOODBOUND.equals(s.profession)
                    || GameCore.PROF_RUNEBLADE.equals(s.profession) || GameCore.PROF_TACTICIAN.equals(s.profession)
                    || GameCore.PROF_PRISMIST.equals(s.profession) || GameCore.PROF_GARDENER.equals(s.profession)
                    || GameCore.PROF_CHEF.equals(s.profession) || GameCore.PROF_BARD.equals(s.profession)
                    || GameCore.PROF_MIRRORIST.equals(s.profession) || GameCore.PROF_PUPPETEER.equals(s.profession)
                    || GameCore.PROF_SCAVENGER.equals(s.profession)) ? 32 : 20;
            else if ("spec_bulwark".equals(id)) score += (GameCore.PROF_WARDEN.equals(s.profession) || GameCore.PROF_BLOODBOUND.equals(s.profession)
                    || GameCore.PROF_RUNEBLADE.equals(s.profession) || GameCore.PROF_TACTICIAN.equals(s.profession)
                    || GameCore.PROF_PRISMIST.equals(s.profession) || GameCore.PROF_GARDENER.equals(s.profession)
                    || GameCore.PROF_CHEF.equals(s.profession) || GameCore.PROF_BARD.equals(s.profession)
                    || GameCore.PROF_MIRRORIST.equals(s.profession) || GameCore.PROF_PUPPETEER.equals(s.profession)
                    || GameCore.PROF_SCAVENGER.equals(s.profession) || GameCore.PROF_LIGHTKEEPER.equals(s.profession)
                    || GameCore.PROF_GEOMANCER.equals(s.profession)) ? 35 : 24;
            else if ("spec_control".equals(id)) score += (GameCore.PROF_RANGER.equals(s.profession) || GameCore.PROF_HEXER.equals(s.profession)
                    || GameCore.PROF_INSCRIBER.equals(s.profession) || GameCore.PROF_PACTMAKER.equals(s.profession)
                    || GameCore.PROF_STORMCALLER.equals(s.profession) || GameCore.PROF_RUNEBLADE.equals(s.profession)
                    || GameCore.PROF_MEDIUM.equals(s.profession) || GameCore.PROF_TACTICIAN.equals(s.profession)
                    || GameCore.PROF_PRISMIST.equals(s.profession) || GameCore.PROF_GARDENER.equals(s.profession)
                    || GameCore.PROF_CHEF.equals(s.profession) || GameCore.PROF_BARD.equals(s.profession)
                    || GameCore.PROF_MIRRORIST.equals(s.profession) || GameCore.PROF_PUPPETEER.equals(s.profession)
                    || GameCore.PROF_SCAVENGER.equals(s.profession)) ? 31 : 24;
            else if ("spec_assembly".equals(id)) score += (GameCore.PROF_WEAVER.equals(s.profession) || GameCore.PROF_MACHINIST.equals(s.profession)
                    || GameCore.PROF_ASTROLOGER.equals(s.profession) || GameCore.PROF_RUNEBLADE.equals(s.profession)
                    || GameCore.PROF_TACTICIAN.equals(s.profession) || GameCore.PROF_PRISMIST.equals(s.profession)
                    || GameCore.PROF_MIRRORIST.equals(s.profession)) ? 31 : 24;
            else if ("spec_echoflow".equals(id)) score += (GameCore.PROF_ARCANIST.equals(s.profession) || GameCore.PROF_SUMMONER.equals(s.profession)
                    || GameCore.PROF_CHRONOMANCER.equals(s.profession) || GameCore.PROF_SHADOWDANCER.equals(s.profession)
                    || GameCore.PROF_MEDIUM.equals(s.profession) || GameCore.PROF_DREAMWALKER.equals(s.profession)
                    || GameCore.PROF_CHEF.equals(s.profession) || GameCore.PROF_BARD.equals(s.profession)
                    || GameCore.PROF_MIRRORIST.equals(s.profession) || GameCore.PROF_PUPPETEER.equals(s.profession)
                    || GameCore.PROF_SCAVENGER.equals(s.profession)) ? 33 : 26;
            else if ("spec_markchain".equals(id)) score += (GameCore.PROF_RANGER.equals(s.profession) || GameCore.PROF_TUNER.equals(s.profession)
                    || GameCore.PROF_ADJUDICATOR.equals(s.profession) || GameCore.PROF_HEXER.equals(s.profession)
                    || GameCore.PROF_PACTMAKER.equals(s.profession) || GameCore.PROF_STORMCALLER.equals(s.profession)
                    || GameCore.PROF_SHADOWDANCER.equals(s.profession) || GameCore.PROF_MEDIUM.equals(s.profession)
                    || GameCore.PROF_TACTICIAN.equals(s.profession) || GameCore.PROF_PRISMIST.equals(s.profession)
                    || GameCore.PROF_DREAMWALKER.equals(s.profession) || GameCore.PROF_GARDENER.equals(s.profession)
                    || GameCore.PROF_CHEF.equals(s.profession) || GameCore.PROF_BARD.equals(s.profession)
                    || GameCore.PROF_MIRRORIST.equals(s.profession) || GameCore.PROF_PUPPETEER.equals(s.profession)
                    || GameCore.PROF_SCAVENGER.equals(s.profession)) ? 33 : 25;
            else if ("spec_pressure".equals(id)) score += (GameCore.PROF_ALCHEMIST.equals(s.profession) || GameCore.PROF_RANGER.equals(s.profession)
                    || GameCore.PROF_HEXER.equals(s.profession) || GameCore.PROF_TUNER.equals(s.profession)
                    || GameCore.PROF_ADJUDICATOR.equals(s.profession) || GameCore.PROF_PACTMAKER.equals(s.profession)
                    || GameCore.PROF_STORMCALLER.equals(s.profession) || GameCore.PROF_RUNEBLADE.equals(s.profession)
                    || GameCore.PROF_MEDIUM.equals(s.profession) || GameCore.PROF_TACTICIAN.equals(s.profession)
                    || GameCore.PROF_PRISMIST.equals(s.profession) || GameCore.PROF_DREAMWALKER.equals(s.profession)
                    || GameCore.PROF_GARDENER.equals(s.profession) || GameCore.PROF_CHEF.equals(s.profession)
                    || GameCore.PROF_BARD.equals(s.profession) || GameCore.PROF_MIRRORIST.equals(s.profession)
                    || GameCore.PROF_PUPPETEER.equals(s.profession) || GameCore.PROF_SCAVENGER.equals(s.profession)) ? 34 : 26;
            else if ("spec_salvage".equals(id)) score += (GameCore.PROF_SCAVENGER.equals(s.profession) || GameCore.PROF_ARCANIST.equals(s.profession)
                    || GameCore.PROF_MERCHANT.equals(s.profession) || GameCore.PROF_HEXER.equals(s.profession)
                    || GameCore.PROF_INSCRIBER.equals(s.profession) || GameCore.PROF_PACTMAKER.equals(s.profession)
                    || GameCore.PROF_DREAMWALKER.equals(s.profession) || GameCore.PROF_GARDENER.equals(s.profession)
                    || GameCore.PROF_CHEF.equals(s.profession) || GameCore.PROF_BLOODBOUND.equals(s.profession)
                    || GameCore.PROF_MEDIUM.equals(s.profession) || GameCore.PROF_BARD.equals(s.profession)
                    || GameCore.PROF_MIRRORIST.equals(s.profession) || GameCore.PROF_PUPPETEER.equals(s.profession)) ? 33 : 25;
            if (GameCore.PROF_PACTMAKER.equals(s.profession) && ("spec_sustain".equals(id) || "spec_resonance".equals(id)
                    || "spec_mastery".equals(id) || "spec_pressure".equals(id) || "spec_salvage".equals(id))) score += 6;
            if (GameCore.PROF_STORMCALLER.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_tempo".equals(id) || "spec_burst".equals(id) || "spec_pressure".equals(id))) score += 8;
            if (GameCore.PROF_SHADOWDANCER.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_tempo".equals(id) || "spec_burst".equals(id) || "spec_echoflow".equals(id) || "spec_markchain".equals(id))) score += 8;
            if (GameCore.PROF_RUNEBLADE.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_sustain".equals(id) || "spec_assembly".equals(id) || "spec_control".equals(id)
                    || "spec_markchain".equals(id) || "spec_pressure".equals(id) || "spec_salvage".equals(id)
                    || "spec_bulwark".equals(id))) score += 8;
            if (GameCore.PROF_MEDIUM.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_tempo".equals(id) || "spec_echoflow".equals(id) || "spec_markchain".equals(id)
                    || "spec_control".equals(id) || "spec_pressure".equals(id) || "spec_salvage".equals(id))) score += 8;
            if (GameCore.PROF_TACTICIAN.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_sustain".equals(id) || "spec_assembly".equals(id) || "spec_control".equals(id)
                    || "spec_markchain".equals(id) || "spec_tempo".equals(id) || "spec_pressure".equals(id)
                    || "spec_bulwark".equals(id))) score += 8;
            if (GameCore.PROF_PRISMIST.equals(s.profession) && ("spec_resonance".equals(id) || "spec_mastery".equals(id)
                    || "spec_assembly".equals(id) || "spec_markchain".equals(id) || "spec_control".equals(id)
                    || "spec_tempo".equals(id) || "spec_echoflow".equals(id) || "spec_pressure".equals(id)
                    || "spec_bulwark".equals(id))) score += 8;
            if (GameCore.PROF_DREAMWALKER.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_tempo".equals(id) || "spec_echoflow".equals(id) || "spec_control".equals(id)
                    || "spec_markchain".equals(id) || "spec_sustain".equals(id) || "spec_pressure".equals(id)
                    || "spec_salvage".equals(id))) score += 8;
            if (GameCore.PROF_GARDENER.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_sustain".equals(id) || "spec_control".equals(id) || "spec_markchain".equals(id)
                    || "spec_tempo".equals(id) || "spec_pressure".equals(id) || "spec_salvage".equals(id)
                    || "spec_bulwark".equals(id))) score += 8;
            if (GameCore.PROF_CHEF.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_sustain".equals(id) || "spec_control".equals(id) || "spec_markchain".equals(id)
                    || "spec_tempo".equals(id) || "spec_echoflow".equals(id) || "spec_pressure".equals(id)
                    || "spec_salvage".equals(id) || "spec_bulwark".equals(id))) score += 8;
            if (GameCore.PROF_BARD.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_tempo".equals(id) || "spec_echoflow".equals(id) || "spec_markchain".equals(id)
                    || "spec_burst".equals(id) || "spec_sustain".equals(id) || "spec_pressure".equals(id)
                    || "spec_bulwark".equals(id))) score += 8;
            if (GameCore.PROF_MIRRORIST.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_assembly".equals(id) || "spec_echoflow".equals(id) || "spec_markchain".equals(id)
                    || "spec_tempo".equals(id) || "spec_sustain".equals(id) || "spec_pressure".equals(id)
                    || "spec_bulwark".equals(id))) score += 8;
            if (GameCore.PROF_PUPPETEER.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_echoflow".equals(id) || "spec_markchain".equals(id) || "spec_control".equals(id)
                    || "spec_tempo".equals(id) || "spec_sustain".equals(id) || "spec_pressure".equals(id)
                    || "spec_salvage".equals(id) || "spec_bulwark".equals(id))) score += 8;
            if (GameCore.PROF_SCAVENGER.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_tempo".equals(id) || "spec_echoflow".equals(id) || "spec_markchain".equals(id)
                    || "spec_control".equals(id) || "spec_sustain".equals(id) || "spec_pressure".equals(id)
                    || "spec_salvage".equals(id) || "spec_bulwark".equals(id))) score += 8;
            if (GameCore.PROF_LIGHTKEEPER.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_tempo".equals(id) || "spec_echoflow".equals(id) || "spec_control".equals(id)
                    || "spec_markchain".equals(id) || "spec_assembly".equals(id) || "spec_pressure".equals(id)
                    || "spec_salvage".equals(id) || "spec_sustain".equals(id) || "spec_bulwark".equals(id))) score += 8;
            if (GameCore.PROF_GEOMANCER.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_assembly".equals(id) || "spec_pressure".equals(id) || "spec_sustain".equals(id)
                    || "spec_markchain".equals(id) || "spec_control".equals(id) || "spec_tempo".equals(id)
                    || "spec_bulwark".equals(id))) score += 8;
            if (GameCore.PROF_WITCH.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_tempo".equals(id) || "spec_echoflow".equals(id) || "spec_control".equals(id)
                    || "spec_markchain".equals(id) || "spec_pressure".equals(id) || "spec_salvage".equals(id)
                    || "spec_sustain".equals(id) || "spec_bulwark".equals(id))) score += 8;
            if (GameCore.PROF_SHIFTER.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_tempo".equals(id) || "spec_echoflow".equals(id) || "spec_control".equals(id)
                    || "spec_markchain".equals(id) || "spec_pressure".equals(id) || "spec_salvage".equals(id)
                    || "spec_burst".equals(id) || "spec_bulwark".equals(id))) score += 8;
            if (GameCore.PROF_FATESEER.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_tempo".equals(id) || "spec_assembly".equals(id) || "spec_control".equals(id)
                    || "spec_markchain".equals(id) || "spec_pressure".equals(id) || "spec_bulwark".equals(id))) score += 8;
            if (GameCore.PROF_TIDECALLER.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_tempo".equals(id) || "spec_sustain".equals(id) || "spec_control".equals(id)
                    || "spec_markchain".equals(id) || "spec_pressure".equals(id) || "spec_bulwark".equals(id))) score += 8;
            if (GameCore.PROF_FROSTBINDER.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_control".equals(id) || "spec_pressure".equals(id) || "spec_salvage".equals(id)
                    || "spec_echoflow".equals(id) || "spec_bulwark".equals(id) || "spec_sustain".equals(id))) score += 10;
            if (GameCore.PROF_PLAGUEDOCTOR.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_tempo".equals(id) || "spec_echoflow".equals(id) || "spec_control".equals(id)
                    || "spec_markchain".equals(id) || "spec_pressure".equals(id) || "spec_salvage".equals(id)
                    || "spec_sustain".equals(id) || "spec_bulwark".equals(id))) score += 10;
            if (GameCore.PROF_ARCHIVIST.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_tempo".equals(id) || "spec_echoflow".equals(id) || "spec_control".equals(id)
                    || "spec_markchain".equals(id) || "spec_pressure".equals(id) || "spec_salvage".equals(id)
                    || "spec_sustain".equals(id) || "spec_bulwark".equals(id) || "spec_assembly".equals(id))) score += 10;
            if (GameCore.PROF_VOIDNAVIGATOR.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_tempo".equals(id) || "spec_echoflow".equals(id) || "spec_control".equals(id)
                    || "spec_markchain".equals(id) || "spec_pressure".equals(id) || "spec_burst".equals(id)
                    || "spec_bulwark".equals(id) || "spec_assembly".equals(id))) score += 10;
            if (GameCore.PROF_RELICSMITH.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_assembly".equals(id) || "spec_bulwark".equals(id) || "spec_tempo".equals(id)
                    || "spec_control".equals(id) || "spec_markchain".equals(id) || "spec_pressure".equals(id))) score += 10;
            if (GameCore.PROF_BEASTMASTER.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_echoflow".equals(id) || "spec_control".equals(id) || "spec_markchain".equals(id)
                    || "spec_pressure".equals(id) || "spec_sustain".equals(id) || "spec_bulwark".equals(id)
                    || "spec_tempo".equals(id))) score += 10;
            if (GameCore.PROF_DRAGONBINDER.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_echoflow".equals(id) || "spec_control".equals(id) || "spec_markchain".equals(id)
                    || "spec_pressure".equals(id) || "spec_sustain".equals(id) || "spec_bulwark".equals(id)
                    || "spec_tempo".equals(id))) score += 10;
            if (GameCore.PROF_SOULBINDER.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_echoflow".equals(id) || "spec_control".equals(id) || "spec_markchain".equals(id)
                    || "spec_pressure".equals(id) || "spec_sustain".equals(id) || "spec_bulwark".equals(id)
                    || "spec_tempo".equals(id) || "spec_salvage".equals(id))) score += 10;
            if (GameCore.PROF_STARFORGER.equals(s.profession) && ("spec_mastery".equals(id) || "spec_resonance".equals(id)
                    || "spec_assembly".equals(id) || "spec_control".equals(id) || "spec_markchain".equals(id)
                    || "spec_pressure".equals(id) || "spec_bulwark".equals(id) || "spec_tempo".equals(id))) score += 10;
            if (s.ascension >= 6 && "spec_sustain".equals(id)) score += 10;
            if (s.ascension >= 6 && "spec_burst".equals(id)) score -= 4;
            if (s.ascension >= 6 && ("spec_markchain".equals(id) || "spec_control".equals(id)
                    || "spec_pressure".equals(id) || "spec_salvage".equals(id) || "spec_hybrid".equals(id)
                    || "spec_cascade".equals(id))) score += 4;
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
        else if (n.route == GameCore.ROUTE_CONFLUENCE) score += hasBuildCoreTalent(s) || s.act >= 2
                || GameCore.PROF_PRISMIST.equals(s.profession) || GameCore.PROF_TACTICIAN.equals(s.profession)
                || GameCore.PROF_STARFORGER.equals(s.profession) ? 16 : 9;
        else if (n.route == GameCore.ROUTE_STUDY) score += s.skillSpecLevel >= 2
                || GameCore.PROF_PACTMAKER.equals(s.profession) || GameCore.PROF_ADJUDICATOR.equals(s.profession)
                || GameCore.PROF_TUNER.equals(s.profession) || GameCore.PROF_PRISMIST.equals(s.profession)
                || GameCore.PROF_RELICSMITH.equals(s.profession) ? 16 : 9;
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
                int coreSignal = buildCoreCardSignal(s, d);
                if (coreSignal > 0) {
                    score += Math.min(32, coreSignal * 3);
                    if (coreSignal >= 8) score += 12;
                }
                if (s.combatQuest == GameCore.QUEST_BREW && d.createPotion) score += 22;
                if (s.combatQuest == GameCore.QUEST_SKILL && d.skillChargeGain > 0) score += 18;
                if (s.combatQuest == GameCore.QUEST_ECHO && (d.exhaust || d.createEcho || c.temp)) score += 18;
                if (s.combatQuest == GameCore.QUEST_BLOODCOIN
                        && (d.hpLoss > 0 || d.goldGain > 0 || d.goldDamage || d.goldBlock || "wound".equals(c.id))) score += 18;
                if (s.combatQuest == GameCore.QUEST_FORGE && (c.upgraded || d.upgradeRandom || d.scry > 0)) score += 18;
                if (s.combatQuest == GameCore.QUEST_TREASURE && (d.goldGain > 0 || d.goldDamage || d.goldBlock)) score += 18;
                if (s.combatQuest == GameCore.QUEST_CONFLUENCE && (isHybridCore(d) || isConfluenceCore(d))) score += 24;
                if (s.combatQuest == GameCore.QUEST_MARK && (d.bind > 0 || d.vulnerable > 0 || d.comboDamage > 0
                        || "tuner_note".equals(c.id) || "tuner_harmonic".equals(c.id) || "tuner_grand_cadence".equals(c.id)
                        || isStormcallerCard(d) || isShadowdancerCard(d) || isRunebladeCard(d) || isMediumCard(d)
                        || isTacticianCard(d) || isPrismistCard(d) || isDreamwalkerCard(d) || isGardenerCard(d)
                        || isBardCard(d) || isMirroristCard(d) || isPuppeteerCard(d) || isScavengerCard(d)
                        || isGeomancerCard(d) || isWitchCard(d) || isFrostbinderCard(d) || isBeastmasterCard(d)
                        || isDragonbinderCard(d) || isSoulbinderCard(d) || isStarforgerCard(d))) score += 20;
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
                if (GameCore.PROF_MACHINIST.equals(s.profession) && (d.upgradeRandom || d.scry > 0 || d.createEcho || c.temp
                        || d.skillChargeGain > 0 || d.energyGain > 0 || d.block > 0 || isHybridCore(d))) {
                    score += 12;
                }
                if (GameCore.PROF_CHRONOMANCER.equals(s.profession) && (d.cost == 0 || d.draw > 0 || d.energyGain > 0
                        || d.skillChargeGain > 0 || d.createEcho || c.temp || d.exhaust)) {
                    score += 12;
                }
                if (GameCore.PROF_PACTMAKER.equals(s.profession) && (d.goldGain > 0 || d.goldDamage || d.goldBlock
                        || d.skillChargeGain > 0 || d.vulnerable > 0 || d.bind > 0 || d.createWound
                        || d.draw > 0 || d.type == 1 || "wound".equals(c.id) || "daze".equals(c.id))) {
                    score += 15;
                }
                if (GameCore.PROF_STORMCALLER.equals(s.profession) && (isStormcallerSignal(d) || c.temp)) {
                    score += 15;
                    if (d.burn > 0 || d.vulnerable > 0 || d.aoe) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || d.cost == 0 || isStormcallerCard(d))) score += 6;
                    if (stormcallerEnemyPressure(s) >= 8 && (d.aoe || "stormcaller_chain".equals(c.id)
                            || "stormcaller_tempest_crown".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_SHADOWDANCER.equals(s.profession) && (isShadowdancerSignal(d) || c.temp)) {
                    score += 15;
                    if (d.cost == 0 || d.comboDamage > 0 || d.vulnerable > 0) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || d.cost == 0 || isShadowdancerCard(d))) score += 6;
                    if (shadowdancerEnemyPressure(s) >= 8 && ("shadowdancer_mark".equals(c.id)
                            || "shadowdancer_overstrike".equals(c.id) || "shadowdancer_eclipse".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_RUNEBLADE.equals(s.profession) && (isRunebladeSignal(d) || c.temp)) {
                    score += 15;
                    if (c.upgraded || d.upgradeRandom || d.scry > 0 || d.vulnerable > 0) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || d.upgradeRandom || isRunebladeCard(d))) score += 6;
                    if (runebladeEnemyPressure(s) >= 8 && ("runeblade_glyphcut".equals(c.id)
                            || "runeblade_overglyph".equals(c.id) || "runeblade_grand_seal".equals(c.id))) score += 8;
                    if (upgradedDeckCards(s) >= 6 && (d.damage > 0 || d.block > 0 || d.skillChargeGain > 0)) score += 5;
                }
                if (GameCore.PROF_MEDIUM.equals(s.profession) && (isMediumSignal(d) || c.temp)) {
                    score += 15;
                    if (d.createEcho || d.exhaust || d.scry > 0 || d.bind > 0) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || d.createEcho || isMediumCard(d))) score += 6;
                    if (mediumEnemyPressure(s) >= 8 && ("medium_binding".equals(c.id)
                            || "medium_overtrance".equals(c.id) || "medium_grand_seance".equals(c.id))) score += 8;
                    if (tempOrEchoHandCards(s) >= 2 && (d.draw > 0 || d.block > 0 || d.skillChargeGain > 0)) score += 5;
                }
                if (GameCore.PROF_TACTICIAN.equals(s.profession) && (isTacticianSignal(d) || c.temp)) {
                    score += 15;
                    if (d.block > 0 || d.scry > 0 || d.upgradeRandom || d.vulnerable > 0) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || d.upgradeRandom || isTacticianCard(d))) score += 6;
                    if (tacticianEnemyPressure(s) >= 8 && ("tactician_flank".equals(c.id)
                            || "tactician_overplan".equals(c.id) || "tactician_grand_strategy".equals(c.id))) score += 8;
                    if (s.block >= 14 && (d.damage > 0 || d.block > 0 || d.skillChargeGain > 0)) score += 5;
                }
                if (GameCore.PROF_PRISMIST.equals(s.profession) && (isPrismistSignal(d) || c.temp)) {
                    score += 15;
                    if (isHybridCore(d) || isConfluenceCore(d) || d.scry > 0 || d.upgradeRandom || d.vulnerable > 0) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isPrismistCard(d) || isHybridCore(d))) score += 6;
                    if (prismistEnemyPressure(s) >= 8 && ("prismist_spill".equals(c.id)
                            || "prismist_overbeam".equals(c.id) || "prismist_grand_spectrum".equals(c.id))) score += 8;
                    if (s.confluenceChain >= 3 && (d.draw > 0 || d.block > 0 || d.skillChargeGain > 0 || isPrismistCard(d))) score += 5;
                }
                if (GameCore.PROF_DREAMWALKER.equals(s.profession) && (isDreamwalkerSignal(d) || c.temp)) {
                    score += 15;
                    if (d.scry > 0 || d.exhaust || d.createEcho || d.bind > 0 || "wound".equals(c.id) || "daze".equals(c.id)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isDreamwalkerCard(d) || d.createEcho)) score += 6;
                    if (dreamwalkerEnemyPressure(s) >= 8 && ("dreamwalker_bind".equals(c.id)
                            || "dreamwalker_overdream".equals(c.id) || "dreamwalker_grand_dream".equals(c.id))) score += 8;
                    if (tempOrEchoHandCards(s) >= 2 && (d.draw > 0 || d.block > 0 || d.skillChargeGain > 0 || isDreamwalkerCard(d))) score += 5;
                    if (statusDeckCards(s) >= 2 && ("dreamwalker_lucid".equals(c.id) || "dreamwalker_veil".equals(c.id)
                            || "dreamwalker_grand_dream".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_GARDENER.equals(s.profession) && (isGardenerSignal(d) || c.temp)) {
                    score += 15;
                    if (d.heal > 0 || d.gainWildEngine > 0 || d.bind > 0 || d.block > 0
                            || "wound".equals(c.id) || "daze".equals(c.id)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isGardenerCard(d)
                            || d.gainWildEngine > 0 || d.heal > 0)) score += 6;
                    if (gardenerEnemyPressure(s) >= 8 && ("gardener_thornbloom".equals(c.id)
                            || "gardener_overgrowth".equals(c.id) || "gardener_grand_grove".equals(c.id))) score += 8;
                    if ((s.block >= 14 || s.hp < s.maxHp) && (d.heal > 0 || d.block > 0
                            || d.skillChargeGain > 0 || isGardenerCard(d))) score += 5;
                    if (statusDeckCards(s) >= 1 && ("gardener_compost".equals(c.id) || "gardener_rootwall".equals(c.id)
                            || "gardener_grand_grove".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_CHEF.equals(s.profession) && (isChefSignal(d) || c.temp)) {
                    score += 15;
                    if (d.heal > 0 || d.createPotion || d.burn > 0 || d.bind > 0 || d.block > 0
                            || "wound".equals(c.id) || "daze".equals(c.id)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isChefCard(d)
                            || d.createPotion || d.heal > 0)) score += 6;
                    if (chefEnemyPressure(s) >= 8 && ("chef_spice".equals(c.id) || "chef_sizzle".equals(c.id)
                            || "chef_overcook".equals(c.id) || "chef_grand_banquet".equals(c.id))) score += 8;
                    if ((s.potions.size() >= 2 || s.hp < s.maxHp) && (d.heal > 0 || d.block > 0
                            || d.skillChargeGain > 0 || isChefCard(d))) score += 5;
                    if (statusDeckCards(s) >= 1 && ("chef_spice".equals(c.id) || "chef_stew".equals(c.id)
                            || "chef_grand_banquet".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_BARD.equals(s.profession) && (isBardSignal(d) || c.temp)) {
                    score += 15;
                    if (d.cost == 0 || d.draw > 0 || d.energyGain > 0 || d.createEcho || c.temp) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isBardCard(d)
                            || d.createEcho || d.energyGain > 0)) score += 6;
                    if (bardEnemyPressure(s) >= 8 && ("bard_discord".equals(c.id)
                            || "bard_overcrescendo".equals(c.id) || "bard_grand_finale".equals(c.id))) score += 8;
                    if (tempOrEchoHandCards(s) >= 2 && (d.draw > 0 || d.block > 0
                            || d.skillChargeGain > 0 || isBardCard(d))) score += 5;
                }
                if (GameCore.PROF_MIRRORIST.equals(s.profession) && (isMirroristSignal(d) || c.temp)) {
                    score += 15;
                    if (d.scry > 0 || d.upgradeRandom || d.createEcho || c.temp || d.vulnerable > 0) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isMirroristCard(d)
                            || d.upgradeRandom || d.createEcho)) score += 6;
                    if (mirroristEnemyPressure(s) >= 8 && ("mirrorist_prismcut".equals(c.id)
                            || "mirrorist_overimage".equals(c.id) || "mirrorist_grand_mirror".equals(c.id))) score += 8;
                    if ((upgradedDeckCards(s) >= 6 || s.confluenceChain >= 3) && (d.draw > 0 || d.block > 0
                            || d.skillChargeGain > 0 || isMirroristCard(d))) score += 5;
                    if (tempOrEchoHandCards(s) >= 2 && (d.createEcho || d.draw > 0 || isMirroristCard(d))) score += 5;
                }
                if (GameCore.PROF_PUPPETEER.equals(s.profession) && (isPuppeteerSignal(d) || c.temp)) {
                    score += 15;
                    if (d.bind > 0 || d.createEcho || c.temp || d.block > 0 || d.vulnerable > 0) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isPuppeteerCard(d)
                            || d.createEcho || d.bind > 0)) score += 6;
                    if (puppeteerEnemyPressure(s) >= 8 && ("puppeteer_needle".equals(c.id)
                            || "puppeteer_overpull".equals(c.id) || "puppeteer_grand_stage".equals(c.id))) score += 8;
                    if ((tempOrEchoHandCards(s) >= 2 || s.block >= 14) && (d.draw > 0 || d.block > 0
                            || d.skillChargeGain > 0 || isPuppeteerCard(d))) score += 5;
                }
                if (GameCore.PROF_SCAVENGER.equals(s.profession) && (isScavengerSignal(d) || c.temp)) {
                    score += 15;
                    if (d.draw > 0 || d.goldGain > 0 || d.exhaust || d.createWound || "wound".equals(c.id) || "daze".equals(c.id)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isScavengerCard(d)
                            || d.goldGain > 0 || d.draw > 0)) score += 6;
                    if (scavengerEnemyPressure(s) >= 8 && ("scavenger_magnet".equals(c.id)
                            || "scavenger_overhaul".equals(c.id) || "scavenger_grand_foundry".equals(c.id))) score += 8;
                    if ((statusDeckCards(s) >= 1 || s.discard.size() >= 8 || s.gold >= 120)
                            && (d.draw > 0 || d.block > 0 || d.heal > 0 || d.skillChargeGain > 0 || isScavengerCard(d))) score += 5;
                    if (s.hp < s.maxHp && ("scavenger_patch".equals(c.id) || "scavenger_grand_foundry".equals(c.id))) score += 6;
                }
                if (GameCore.PROF_LIGHTKEEPER.equals(s.profession) && (isLightkeeperSignal(d) || c.temp)) {
                    score += 15;
                    if (d.exhaust || d.createEcho || d.scry > 0 || d.upgradeRandom || d.draw > 0 || c.temp) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isLightkeeperCard(d)
                            || d.createEcho || d.exhaust)) score += 6;
                    if (lightkeeperEnemyPressure(s) >= 8 && ("lightkeeper_brand".equals(c.id)
                            || "lightkeeper_overflare".equals(c.id) || "lightkeeper_grand_beacon".equals(c.id))) score += 8;
                    if ((tempOrEchoHandCards(s) >= 2 || statusDeckCards(s) >= 1 || s.exhaust.size() >= 5)
                            && (d.draw > 0 || d.block > 0 || d.skillChargeGain > 0 || isLightkeeperCard(d))) score += 5;
                    if (statusDeckCards(s) >= 1 && ("lightkeeper_prism".equals(c.id) || "lightkeeper_vigil".equals(c.id)
                            || "lightkeeper_grand_beacon".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_GEOMANCER.equals(s.profession) && (isGeomancerSignal(d) || c.temp)) {
                    score += 15;
                    if (d.block > 0 || d.burn > 0 || d.bind > 0 || d.upgradeRandom || d.scry > 0) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isGeomancerCard(d)
                            || d.block > 0 || d.upgradeRandom)) score += 6;
                    if (geomancerEnemyPressure(s) >= 8 && ("geomancer_quake".equals(c.id)
                            || "geomancer_overquake".equals(c.id) || "geomancer_grand_fault".equals(c.id))) score += 8;
                    if ((s.block >= 14 || s.confluenceChain >= 3 || upgradedDeckCards(s) >= 6)
                            && (d.draw > 0 || d.block > 0 || d.skillChargeGain > 0 || isGeomancerCard(d))) score += 5;
                    if (s.confluenceChain >= 3 && ("geomancer_rune".equals(c.id) || "geomancer_geode".equals(c.id)
                            || "geomancer_grand_fault".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_WITCH.equals(s.profession) && (isWitchSignal(d) || c.temp)) {
                    score += 15;
                    if (d.createPotion || d.burn > 0 || d.bind > 0 || d.vulnerable > 0 || d.createEcho) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isWitchCard(d)
                            || d.createPotion || d.heal > 0)) score += 6;
                    if (witchEnemyPressure(s) >= 8 && ("witch_curse".equals(c.id)
                            || "witch_overbrew".equals(c.id) || "witch_grand_cauldron".equals(c.id))) score += 8;
                    if ((s.potions.size() >= 2 || statusDeckCards(s) >= 1 || tempOrEchoHandCards(s) >= 2)
                            && (d.draw > 0 || d.block > 0 || d.heal > 0 || d.skillChargeGain > 0 || isWitchCard(d))) score += 5;
                    if (statusDeckCards(s) >= 1 && ("witch_ward".equals(c.id) || "witch_charm".equals(c.id)
                            || "witch_grand_cauldron".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_SHIFTER.equals(s.profession) && (isShifterSignal(d) || c.temp)) {
                    score += 15;
                    if (d.cost == 0 || d.draw > 0 || d.energyGain > 0 || d.createEcho || c.temp) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isShifterCard(d)
                            || d.draw > 0 || d.energyGain > 0)) score += 6;
                    if (shifterEnemyPressure(s) >= 8 && ("shifter_lance".equals(c.id)
                            || "shifter_overblink".equals(c.id) || "shifter_grand_paradox".equals(c.id))) score += 8;
                    if ((s.cardsPlayedThisTurn >= 3 || tempOrEchoHandCards(s) >= 2 || s.discard.size() >= 6)
                            && (d.draw > 0 || d.block > 0 || d.skillChargeGain > 0 || isShifterCard(d))) score += 5;
                    if (tempOrEchoHandCards(s) >= 2 && ("shifter_screen".equals(c.id) || "shifter_anchor".equals(c.id)
                            || "shifter_grand_paradox".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_FATESEER.equals(s.profession) && (isFateseerSignal(d) || c.temp)) {
                    score += 15;
                    if (d.scry > 0 || d.upgradeRandom || c.upgraded || d.draw > 0 || isHybridCore(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isFateseerCard(d)
                            || d.scry > 0 || d.upgradeRandom)) score += 6;
                    if (fateseerEnemyPressure(s) >= 8 && ("fateseer_thread".equals(c.id)
                            || "fateseer_overfate".equals(c.id) || "fateseer_grand_design".equals(c.id))) score += 8;
                    if ((upgradedDeckCards(s) >= 6 || s.confluenceChain >= 3 || s.cardsPlayedThisTurn >= 3)
                            && (d.draw > 0 || d.block > 0 || d.skillChargeGain > 0 || isFateseerCard(d))) score += 5;
                    if (upgradedDeckCards(s) >= 5 && ("fateseer_veil".equals(c.id) || "fateseer_wheel".equals(c.id)
                            || "fateseer_grand_design".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_TIDECALLER.equals(s.profession) && (isTidecallerSignal(d) || c.temp)) {
                    score += 15;
                    if (d.block > 0 || d.bind > 0 || d.draw > 0 || d.cost == 0 || isTidecallerCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isTidecallerCard(d)
                            || d.block > 0 || d.bind > 0)) score += 6;
                    if (tidecallerEnemyPressure(s) >= 8 && ("tidecaller_surge".equals(c.id)
                            || "tidecaller_overtide".equals(c.id) || "tidecaller_grand_tide".equals(c.id))) score += 8;
                    if ((s.block >= 14 || s.cardsPlayedThisTurn >= 3 || bindDeckCards(s) >= 2)
                            && (d.draw > 0 || d.block > 0 || d.skillChargeGain > 0 || isTidecallerCard(d))) score += 5;
                    if (s.block >= 12 && ("tidecaller_breaker".equals(c.id) || "tidecaller_current".equals(c.id)
                            || "tidecaller_grand_tide".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_FROSTBINDER.equals(s.profession) && (isFrostbinderSignal(d) || c.temp)) {
                    score += 15;
                    if (d.bind > 0 || d.block > 0 || d.draw > 0 || d.exhaust || "wound".equals(c.id)
                            || "daze".equals(c.id) || isFrostbinderCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isFrostbinderCard(d)
                            || d.bind > 0 || d.block > 0)) score += 6;
                    if (frostbinderEnemyPressure(s) >= 8 && ("frostbinder_shatter".equals(c.id)
                            || "frostbinder_overfreeze".equals(c.id) || "frostbinder_grand_winter".equals(c.id))) score += 8;
                    if ((statusDeckCards(s) >= 1 || s.exhaust.size() >= 5 || s.block >= 14)
                            && (d.draw > 0 || d.block > 0 || d.skillChargeGain > 0 || isFrostbinderCard(d))) score += 5;
                    if (statusDeckCards(s) >= 1 && ("frostbinder_ward".equals(c.id) || "frostbinder_rime".equals(c.id)
                            || "frostbinder_grand_winter".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_PLAGUEDOCTOR.equals(s.profession) && (isPlaguedoctorSignal(d) || c.temp)) {
                    score += 15;
                    if (d.createPotion || d.burn > 0 || d.bind > 0 || d.vulnerable > 0 || d.heal > 0
                            || "wound".equals(c.id) || "daze".equals(c.id) || isPlaguedoctorCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isPlaguedoctorCard(d)
                            || d.createPotion || d.heal > 0)) score += 6;
                    if (plaguedoctorEnemyPressure(s) >= 8 && ("plaguedoctor_quarantine".equals(c.id)
                            || "plaguedoctor_overdose".equals(c.id) || "plaguedoctor_grand_plague".equals(c.id))) score += 8;
                    if ((s.potions.size() >= 2 || statusDeckCards(s) >= 1 || s.hp < s.maxHp || s.block >= 14)
                            && (d.draw > 0 || d.block > 0 || d.heal > 0 || d.skillChargeGain > 0 || isPlaguedoctorCard(d))) score += 5;
                    if (statusDeckCards(s) >= 1 && ("plaguedoctor_mask".equals(c.id) || "plaguedoctor_culture".equals(c.id)
                            || "plaguedoctor_grand_plague".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_ARCHIVIST.equals(s.profession) && (isArchivistSignal(d) || c.temp)) {
                    score += 15;
                    if (d.scry > 0 || d.upgradeRandom || d.draw > 0 || d.block > 0 || d.createEcho
                            || "wound".equals(c.id) || "daze".equals(c.id) || isArchivistCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isArchivistCard(d)
                            || d.scry > 0 || d.upgradeRandom)) score += 6;
                    if (archivistEnemyPressure(s) >= 8 && ("archivist_redline".equals(c.id)
                            || "archivist_overfile".equals(c.id) || "archivist_grand_archive".equals(c.id))) score += 8;
                    if ((upgradedDeckCards(s) >= 6 || statusDeckCards(s) >= 1 || s.discard.size() >= 6)
                            && (d.draw > 0 || d.block > 0 || d.skillChargeGain > 0 || isArchivistCard(d))) score += 5;
                    if (upgradedDeckCards(s) >= 5 && ("archivist_seal".equals(c.id) || "archivist_catalog".equals(c.id)
                            || "archivist_grand_archive".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_VOIDNAVIGATOR.equals(s.profession) && (isVoidnavigatorSignal(d) || c.temp)) {
                    score += 15;
                    if (d.createEcho || d.exhaust || d.exhaustTopDiscard || d.scry > 0 || d.draw > 0
                            || c.temp || isVoidnavigatorCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isVoidnavigatorCard(d)
                            || d.createEcho || d.exhaust)) score += 6;
                    if (voidnavigatorEnemyPressure(s) >= 8 && ("voidnavigator_rift".equals(c.id)
                            || "voidnavigator_overjump".equals(c.id) || "voidnavigator_grand_jump".equals(c.id))) score += 8;
                    if ((tempOrEchoHandCards(s) >= 3 || s.exhaust.size() >= 5 || s.confluenceChain >= 3)
                            && (d.draw > 0 || d.block > 0 || d.skillChargeGain > 0 || isVoidnavigatorCard(d))) score += 5;
                    if (tempOrEchoHandCards(s) >= 2 && ("voidnavigator_anchor".equals(c.id)
                            || "voidnavigator_chart".equals(c.id) || "voidnavigator_grand_jump".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_RELICSMITH.equals(s.profession) && (isRelicsmithSignal(d) || c.temp)) {
                    score += 15;
                    if (d.goldGain > 0 || d.goldDamage || d.goldBlock || d.upgradeRandom || d.block > 0
                            || d.draw > 0 || isRelicsmithCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isRelicsmithCard(d)
                            || d.goldGain > 0 || d.upgradeRandom)) score += 6;
                    if (relicsmithEnemyPressure(s) >= 8 && ("relicsmith_unlock".equals(c.id)
                            || "relicsmith_overvault".equals(c.id) || "relicsmith_grand_vault".equals(c.id))) score += 8;
                    if ((s.relics.size() >= 5 || s.gold >= 150 || s.block >= 14)
                            && (d.draw > 0 || d.block > 0 || d.skillChargeGain > 0 || isRelicsmithCard(d))) score += 5;
                    if (s.gold >= 120 && ("relicsmith_lock".equals(c.id) || "relicsmith_gauge".equals(c.id)
                            || "relicsmith_grand_vault".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_BEASTMASTER.equals(s.profession) && (isBeastmasterSignal(d) || c.temp)) {
                    score += 15;
                    if (d.createEcho || c.temp || d.bind > 0 || d.heal > 0 || d.block > 0
                            || d.draw > 0 || isBeastmasterCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isBeastmasterCard(d)
                            || d.createEcho || d.bind > 0)) score += 6;
                    if (beastmasterEnemyPressure(s) >= 8 && ("beastmaster_pounce".equals(c.id)
                            || "beastmaster_overpack".equals(c.id) || "beastmaster_grand_hunt".equals(c.id))) score += 8;
                    if ((tempOrEchoHandCards(s) >= 3 || bindDeckCards(s) >= 3 || s.block >= 14)
                            && (d.draw > 0 || d.block > 0 || d.heal > 0 || d.skillChargeGain > 0 || isBeastmasterCard(d))) score += 5;
                    if (tempOrEchoHandCards(s) >= 2 && ("beastmaster_hide".equals(c.id)
                            || "beastmaster_call".equals(c.id) || "beastmaster_grand_hunt".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_DRAGONBINDER.equals(s.profession) && (isDragonbinderSignal(d) || c.temp)) {
                    score += 15;
                    if (d.burn > 0 || d.createEcho || c.temp || d.heal > 0 || d.block > 0
                            || d.draw > 0 || isDragonbinderCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isDragonbinderCard(d)
                            || d.createEcho || d.burn > 0)) score += 6;
                    if (dragonbinderEnemyPressure(s) >= 8 && ("dragonbinder_talon".equals(c.id)
                            || "dragonbinder_overflame".equals(c.id) || "dragonbinder_grand_oath".equals(c.id))) score += 8;
                    if ((tempOrEchoHandCards(s) >= 3 || statusDeckCards(s) >= 1 || s.block >= 14)
                            && (d.draw > 0 || d.block > 0 || d.heal > 0 || d.skillChargeGain > 0 || isDragonbinderCard(d))) score += 5;
                    if (tempOrEchoHandCards(s) >= 2 && ("dragonbinder_scale".equals(c.id)
                            || "dragonbinder_hatch".equals(c.id) || "dragonbinder_grand_oath".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_SOULBINDER.equals(s.profession) && (isSoulbinderSignal(d) || c.temp)) {
                    score += 15;
                    if (d.createEcho || c.temp || d.exhaust || d.heal > 0 || d.block > 0
                            || d.draw > 0 || isSoulbinderCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isSoulbinderCard(d)
                            || d.createEcho || d.exhaust)) score += 6;
                    if (soulbinderEnemyPressure(s) >= 8 && ("soulbinder_lash".equals(c.id)
                            || "soulbinder_overbind".equals(c.id) || "soulbinder_grand_pact".equals(c.id))) score += 8;
                    if ((tempOrEchoHandCards(s) >= 3 || s.exhaust.size() >= 4 || s.block >= 14)
                            && (d.draw > 0 || d.block > 0 || d.heal > 0 || d.skillChargeGain > 0 || isSoulbinderCard(d))) score += 5;
                    if (tempOrEchoHandCards(s) >= 2 && ("soulbinder_veil".equals(c.id)
                            || "soulbinder_pact".equals(c.id) || "soulbinder_grand_pact".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_STARFORGER.equals(s.profession) && (isStarforgerSignal(d) || c.temp)) {
                    score += 15;
                    if (d.upgradeRandom || c.upgraded || d.burn > 0 || d.block > 0
                            || d.draw > 0 || isStarforgerCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isStarforgerCard(d)
                            || d.upgradeRandom || d.burn > 0)) score += 6;
                    if (starforgerEnemyPressure(s) >= 8 && ("starforger_hammer".equals(c.id)
                            || "starforger_overforge".equals(c.id) || "starforger_grand_star".equals(c.id))) score += 8;
                    if ((upgradedDeckCards(s) >= 7 || statusDeckCards(s) >= 1 || s.block >= 14)
                            && (d.draw > 0 || d.block > 0 || d.skillChargeGain > 0 || isStarforgerCard(d))) score += 5;
                    if (upgradedDeckCards(s) >= 6 && ("starforger_guard".equals(c.id)
                            || "starforger_crucible".equals(c.id) || "starforger_grand_star".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_ARRAYIST.equals(s.profession) && (isArrayistSignal(d) || c.temp)) {
                    score += 15;
                    if (d.cost == 0 || d.draw > 0 || d.energyGain > 0 || d.createEcho || c.temp
                            || d.block > 0 || isArrayistCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isArrayistCard(d)
                            || d.createEcho || d.draw > 0)) score += 6;
                    if (arrayistEnemyPressure(s) >= 8 && ("arrayist_lance".equals(c.id)
                            || "arrayist_overarray".equals(c.id) || "arrayist_grand_array".equals(c.id))) score += 8;
                    if ((tempOrEchoHandCards(s) >= 3 || s.confluenceChain >= 3 || s.block >= 14)
                            && (d.draw > 0 || d.block > 0 || d.skillChargeGain > 0 || isArrayistCard(d))) score += 5;
                    if (tempOrEchoHandCards(s) >= 2 && ("arrayist_bastion".equals(c.id)
                            || "arrayist_pivot".equals(c.id) || "arrayist_grand_array".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_GAMBITER.equals(s.profession) && (isGambiterSignal(d) || c.temp)) {
                    score += 15;
                    if (d.cost == 0 || d.draw > 0 || d.block > 0 || d.bind > 0
                            || d.vulnerable > 0 || isGambiterCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isGambiterCard(d)
                            || d.bind > 0 || d.block > 0)) score += 6;
                    if (gambiterEnemyPressure(s) >= 8 && ("gambiter_gambit".equals(c.id)
                            || "gambiter_fork".equals(c.id) || "gambiter_overmate".equals(c.id)
                            || "gambiter_grand_endgame".equals(c.id))) score += 8;
                    if ((s.cardsPlayedThisTurn >= 3 || s.block >= 14 || s.confluenceChain >= 3)
                            && (d.draw > 0 || d.block > 0 || d.skillChargeGain > 0 || isGambiterCard(d))) score += 5;
                    if (s.block >= 12 && ("gambiter_castle".equals(c.id) || "gambiter_fork".equals(c.id)
                            || "gambiter_grand_endgame".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_GRAVEKEEPER.equals(s.profession) && (isGravekeeperSignal(d) || c.temp)) {
                    score += 15;
                    if (d.exhaust || d.exhaustTopDiscard || d.createWound || d.heal > 0 || d.block > 0
                            || d.draw > 0 || d.bind > 0 || d.vulnerable > 0 || isGravekeeperCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isGravekeeperCard(d)
                            || d.exhaust || d.exhaustTopDiscard || d.heal > 0 || d.block > 0)) score += 6;
                    if (gravekeeperEnemyPressure(s) >= 8 && ("gravekeeper_dirge".equals(c.id)
                            || "gravekeeper_overwake".equals(c.id) || "gravekeeper_grand_requiem".equals(c.id))) score += 8;
                    if (((statusDeckCards(s) + statusHandCards(s)) >= 2 || s.exhaust.size() >= 4 || s.block >= 14)
                            && (d.draw > 0 || d.block > 0 || d.heal > 0 || d.skillChargeGain > 0 || isGravekeeperCard(d))) score += 5;
                    if ((statusDeckCards(s) + statusHandCards(s)) >= 1 && ("gravekeeper_shroud".equals(c.id)
                            || "gravekeeper_interment".equals(c.id) || "gravekeeper_grand_requiem".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_TREASURER.equals(s.profession) && (isTreasurerSignal(d) || c.temp)) {
                    score += 15;
                    if (d.goldGain > 0 || d.goldDamage || d.goldBlock || d.block > 0 || d.draw > 0
                            || d.upgradeRandom || d.bind > 0 || d.vulnerable > 0 || isTreasurerCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isTreasurerCard(d)
                            || d.goldGain > 0 || d.upgradeRandom || d.block > 0)) score += 6;
                    if (treasurerEnemyPressure(s) >= 8 && ("treasurer_collection".equals(c.id)
                            || "treasurer_overledger".equals(c.id) || "treasurer_grand_balance".equals(c.id))) score += 8;
                    if ((s.gold >= 120 || upgradedDeckCards(s) >= 6 || s.block >= 14)
                            && (d.draw > 0 || d.block > 0 || d.upgradeRandom || d.skillChargeGain > 0 || isTreasurerCard(d))) score += 5;
                    if (s.gold >= 100 && ("treasurer_vault".equals(c.id)
                            || "treasurer_audit".equals(c.id) || "treasurer_grand_balance".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_DRIFTER.equals(s.profession) && (isDrifterSignal(s, d) || c.temp)) {
                    score += 15;
                    if (isOffPoolCard(s, d) || d.createEcho || d.draw > 0 || d.block > 0
                            || d.upgradeRandom || d.bind > 0 || d.vulnerable > 0 || isDrifterCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isDrifterCard(d)
                            || isOffPoolCard(s, d) || d.upgradeRandom || d.block > 0)) score += 6;
                    if (drifterCrossPressure(s) >= 8 && ("drifter_raid".equals(c.id)
                            || "drifter_overcross".equals(c.id) || "drifter_grand_junction".equals(c.id))) score += 8;
                    if ((tempOrEchoHandCards(s) >= 2 || s.confluenceChain >= 3 || upgradedDeckCards(s) >= 6)
                            && (d.draw > 0 || d.block > 0 || d.upgradeRandom || d.skillChargeGain > 0
                            || isDrifterCard(d) || isOffPoolCard(s, d))) score += 5;
                    if ((isOffPoolCard(s, d) || c.temp) && ("drifter_scout".equals(c.id)
                            || "drifter_patch".equals(c.id) || "drifter_grand_junction".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_OATHKEEPER.equals(s.profession) && (isOathkeeperSignal(d) || c.temp)) {
                    score += 15;
                    if (d.block > 0 || d.heal > 0 || d.draw > 0 || d.upgradeRandom
                            || d.bind > 0 || d.vulnerable > 0 || isOathkeeperCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isOathkeeperCard(d)
                            || d.block > 0 || d.heal > 0 || d.upgradeRandom)) score += 6;
                    if (oathkeeperPressure(s) >= 8 && ("oathkeeper_smite".equals(c.id)
                            || "oathkeeper_overedict".equals(c.id) || "oathkeeper_grand_judgment".equals(c.id))) score += 8;
                    if ((s.block >= 14 || healingDeckCards(s) >= 3 || upgradedDeckCards(s) >= 6)
                            && (d.draw > 0 || d.block > 0 || d.heal > 0 || d.upgradeRandom
                            || d.skillChargeGain > 0 || isOathkeeperCard(d))) score += 5;
                    if (s.block >= 12 && ("oathkeeper_guard".equals(c.id)
                            || "oathkeeper_sanctuary".equals(c.id) || "oathkeeper_grand_judgment".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_MOONSINGER.equals(s.profession) && (isMoonsingerSignal(d) || c.temp)) {
                    score += 15;
                    if (d.scry > 0 || d.draw > 0 || d.createEcho || d.upgradeRandom
                            || d.bind > 0 || d.vulnerable > 0 || isMoonsingerCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isMoonsingerCard(d)
                            || d.scry > 0 || d.createEcho || d.upgradeRandom)) score += 6;
                    if (moonsingerPressure(s) >= 8 && ("moonsinger_eclipse".equals(c.id)
                            || "moonsinger_overmoon".equals(c.id) || "moonsinger_grand_eclipse".equals(c.id))) score += 8;
                    if ((tempOrEchoHandCards(s) >= 2 || scryDeckCards(s) >= 3 || upgradedDeckCards(s) >= 6)
                            && (d.draw > 0 || d.scry > 0 || d.createEcho || d.upgradeRandom
                            || d.skillChargeGain > 0 || isMoonsingerCard(d))) score += 5;
                    if (tempOrEchoHandCards(s) >= 2 && ("moonsinger_tide".equals(c.id)
                            || "moonsinger_overmoon".equals(c.id) || "moonsinger_grand_eclipse".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_SPY.equals(s.profession) && (isSpySignal(d) || c.temp)) {
                    score += 15;
                    if (d.cost == 0 || d.draw > 0 || d.goldGain > 0 || d.createEcho || d.upgradeRandom
                            || d.bind > 0 || d.vulnerable > 0 || isSpyCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isSpyCard(d)
                            || d.cost == 0 || d.goldGain > 0 || d.upgradeRandom)) score += 6;
                    if (spyPressure(s) >= 8 && ("spy_blackmail".equals(c.id)
                            || "spy_overcover".equals(c.id) || "spy_grand_heist".equals(c.id))) score += 8;
                    if ((s.gold >= 100 || tempOrEchoHandCards(s) >= 2 || upgradedDeckCards(s) >= 6)
                            && (d.draw > 0 || d.cost == 0 || d.goldGain > 0 || d.createEcho
                            || d.skillChargeGain > 0 || isSpyCard(d))) score += 5;
                    if (s.gold >= 100 && ("spy_contact".equals(c.id)
                            || "spy_blackmail".equals(c.id) || "spy_grand_heist".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_PERFUMER.equals(s.profession) && (isPerfumerSignal(d) || c.temp)) {
                    score += 15;
                    if (d.createPotion || d.heal > 0 || d.burn > 0 || d.bind > 0 || d.draw > 0
                            || d.upgradeRandom || isPerfumerCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isPerfumerCard(d)
                            || d.createPotion || d.heal > 0 || d.burn > 0)) score += 6;
                    if (perfumerPressure(s) >= 8 && ("perfumer_caustic".equals(c.id)
                            || "perfumer_overaroma".equals(c.id) || "perfumer_grand_bloom".equals(c.id))) score += 8;
                    if ((healingDeckCards(s) >= 3 || potionCards(s) >= 3 || upgradedDeckCards(s) >= 6)
                            && (d.draw > 0 || d.heal > 0 || d.createPotion || d.upgradeRandom
                            || d.skillChargeGain > 0 || isPerfumerCard(d))) score += 5;
                    if (s.hp < s.maxHp && ("perfumer_mist".equals(c.id)
                            || "perfumer_distill".equals(c.id) || "perfumer_grand_bloom".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_CLOCKSMITH.equals(s.profession) && (isClocksmithSignal(d) || c.temp)) {
                    score += 15;
                    if (d.cost == 0 || d.draw > 0 || d.createEcho || d.upgradeRandom
                            || d.bind > 0 || d.vulnerable > 0 || isClocksmithCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isClocksmithCard(d)
                            || d.cost == 0 || d.createEcho || d.upgradeRandom)) score += 6;
                    if (clocksmithPressure(s) >= 8 && ("clocksmith_rewind".equals(c.id)
                            || "clocksmith_overclock".equals(c.id) || "clocksmith_grand_chronogear".equals(c.id))) score += 8;
                    if ((tempOrEchoHandCards(s) >= 2 || zeroCostDeckCards(s) >= 3 || upgradedDeckCards(s) >= 6)
                            && (d.draw > 0 || d.cost == 0 || d.createEcho || d.upgradeRandom
                            || d.skillChargeGain > 0 || isClocksmithCard(d))) score += 5;
                    if (tempOrEchoHandCards(s) >= 2 && ("clocksmith_gear".equals(c.id)
                            || "clocksmith_overclock".equals(c.id) || "clocksmith_grand_chronogear".equals(c.id))) score += 8;
                }
                if (GameCore.PROF_MINTSMITH.equals(s.profession) && isMintsmithSignal(d)) {
                    score += 15;
                    if (d.goldGain > 0 || d.burn > 0 || d.goldBlock || d.upgradeRandom
                            || d.bind > 0 || d.vulnerable > 0 || isMintsmithCard(d)) score += 5;
                    if (s.professionCharge >= 3 && (d.skillChargeGain > 0 || isMintsmithCard(d)
                            || d.goldGain > 0 || d.burn > 0 || d.upgradeRandom)) score += 6;
                    if (mintsmithPressure(s) >= 8 && ("mintsmith_tax".equals(c.id)
                            || "mintsmith_overmint".equals(c.id) || "mintsmith_grand_mintage".equals(c.id))) score += 8;
                    if ((s.gold >= 100 || burnDeckCards(s) >= 4 || upgradedDeckCards(s) >= 6)
                            && (d.goldGain > 0 || d.burn > 0 || d.upgradeRandom
                            || d.skillChargeGain > 0 || isMintsmithCard(d))) score += 5;
                    if (s.gold >= 120 && ("mintsmith_mold".equals(c.id)
                            || "mintsmith_assay".equals(c.id) || "mintsmith_grand_mintage".equals(c.id))) score += 8;
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
                if (s.talents.contains("t_stormcaller_rod") && (d.burn > 0 || d.skillChargeGain > 0 || isStormcallerCard(d))) score += 12;
                if (s.talents.contains("t_stormcaller_pressure") && (d.cost == 0 || d.draw > 0 || d.energyGain > 0)) score += 12;
                if (s.talents.contains("t_stormcaller_front") && (d.aoe || d.burn > 0 || d.vulnerable > 0)) score += 12;
                if (s.talents.contains("t_stormcaller_grand") && (d.skillChargeGain > 0 || d.burn > 0 || d.draw > 0 || d.energyGain > 0 || c.upgraded)) score += 14;
                if (s.talents.contains("t_shadowdancer_mask") && (d.cost == 0 || d.skillChargeGain > 0 || isShadowdancerCard(d))) score += 12;
                if (s.talents.contains("t_shadowdancer_vanish") && (d.exhaust || d.createEcho || c.temp || d.draw > 0)) score += 12;
                if (s.talents.contains("t_shadowdancer_execution") && (d.vulnerable > 0 || d.comboDamage > 0 || d.damage > 0)) score += 12;
                if (s.talents.contains("t_shadowdancer_grand") && (d.skillChargeGain > 0 || d.draw > 0 || d.energyGain > 0 || c.temp || c.upgraded)) score += 14;
                if (s.talents.contains("t_runeblade_stylus") && (c.upgraded || d.upgradeRandom || d.scry > 0
                        || d.skillChargeGain > 0 || isRunebladeCard(d))) score += 12;
                if (s.talents.contains("t_runeblade_guard") && (d.block > 0 || d.type == 1 || c.upgraded)) score += 12;
                if (s.talents.contains("t_runeblade_execution") && (d.vulnerable > 0 || d.damage > 0 || c.upgraded)) score += 12;
                if (s.talents.contains("t_runeblade_grand") && (c.upgraded || d.skillChargeGain > 0 || d.upgradeRandom
                        || d.scry > 0 || d.rarity == 2)) score += 14;
                if (s.talents.contains("t_medium_oracle") && (d.scry > 0 || d.draw > 0 || d.skillChargeGain > 0 || isMediumCard(d))) score += 12;
                if (s.talents.contains("t_medium_veil") && (d.block > 0 || d.type == 1 || d.createEcho || c.temp)) score += 12;
                if (s.talents.contains("t_medium_binding") && (d.bind > 0 || d.vulnerable > 0 || d.damage > 0 || isMediumCard(d))) score += 12;
                if (s.talents.contains("t_medium_grand") && (d.createEcho || d.exhaust || c.temp || d.skillChargeGain > 0
                        || d.rarity == 2 || d.scry > 0)) score += 14;
                if (s.talents.contains("t_tactician_map") && (d.scry > 0 || d.draw > 0
                        || d.upgradeRandom || d.skillChargeGain > 0 || isTacticianCard(d))) score += 12;
                if (s.talents.contains("t_tactician_bulwark") && (d.block > 0 || d.type == 1 || c.upgraded
                        || isTacticianCard(d))) score += 12;
                if (s.talents.contains("t_tactician_flank") && (d.vulnerable > 0 || d.damage > 0
                        || d.skillChargeGain > 0 || isTacticianCard(d))) score += 12;
                if (s.talents.contains("t_tactician_grand") && (d.block > 0 || d.scry > 0 || d.upgradeRandom
                        || d.skillChargeGain > 0 || d.rarity == 2 || isHybridCore(d) || isTacticianCard(d))) score += 14;
                if (s.talents.contains("t_prismist_lens") && (d.scry > 0 || d.draw > 0
                        || d.upgradeRandom || d.skillChargeGain > 0 || isPrismistCard(d) || isHybridCore(d))) score += 12;
                if (s.talents.contains("t_prismist_anchor") && (d.block > 0 || d.type == 1 || c.upgraded
                        || d.upgradeRandom || isPrismistCard(d) || isConfluenceCore(d))) score += 12;
                if (s.talents.contains("t_prismist_spill") && (d.vulnerable > 0 || d.bind > 0 || d.burn > 0
                        || d.damage > 0 || d.skillChargeGain > 0 || isPrismistCard(d))) score += 12;
                if (s.talents.contains("t_prismist_grand") && (d.draw > 0 || d.skillChargeGain > 0 || d.upgradeRandom
                        || d.rarity == 2 || isHybridCore(d) || isConfluenceCore(d) || isPrismistCard(d))) score += 14;
                if (s.talents.contains("t_dreamwalker_drift") && (d.scry > 0 || d.draw > 0 || c.temp
                        || d.createEcho || isDreamwalkerCard(d))) score += 12;
                if (s.talents.contains("t_dreamwalker_veil") && (d.block > 0 || d.type == 1 || c.temp
                        || d.createEcho || "wound".equals(c.id) || "daze".equals(c.id))) score += 12;
                if (s.talents.contains("t_dreamwalker_lucid") && (d.exhaust || d.scry > 0 || d.upgradeRandom
                        || "wound".equals(c.id) || "daze".equals(c.id) || isDreamwalkerCard(d))) score += 12;
                if (s.talents.contains("t_dreamwalker_grand") && (d.createEcho || d.exhaust || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.scry > 0 || isDreamwalkerCard(d))) score += 14;
                if (s.talents.contains("t_gardener_sprout") && (d.heal > 0 || d.draw > 0 || d.gainWildEngine > 0
                        || isGardenerCard(d))) score += 12;
                if (s.talents.contains("t_gardener_rootwall") && (d.block > 0 || d.type == 1 || d.heal > 0
                        || d.bind > 0 || isGardenerCard(d))) score += 12;
                if (s.talents.contains("t_gardener_compost") && (d.createWound || d.exhaust
                        || "wound".equals(c.id) || "daze".equals(c.id) || isGardenerCard(d))) score += 12;
                if (s.talents.contains("t_gardener_grand") && (d.heal > 0 || d.gainWildEngine > 0 || d.bind > 0
                        || d.skillChargeGain > 0 || d.rarity == 2 || isGardenerCard(d))) score += 14;
                if (s.talents.contains("t_chef_prep") && (d.heal > 0 || d.createPotion || d.draw > 0
                        || isChefCard(d))) score += 12;
                if (s.talents.contains("t_chef_stew") && (d.block > 0 || d.type == 1 || d.heal > 0
                        || d.createEcho || isChefCard(d))) score += 12;
                if (s.talents.contains("t_chef_spice") && (d.burn > 0 || d.bind > 0 || d.createWound
                        || d.exhaust || "wound".equals(c.id) || "daze".equals(c.id) || isChefCard(d))) score += 12;
                if (s.talents.contains("t_chef_grand") && (d.heal > 0 || d.createPotion || d.burn > 0
                        || d.bind > 0 || d.skillChargeGain > 0 || d.rarity == 2 || isChefCard(d))) score += 14;
                if (s.talents.contains("t_bard_note") && (d.cost == 0 || d.draw > 0 || c.temp
                        || isBardCard(d))) score += 12;
                if (s.talents.contains("t_bard_ballad") && (d.block > 0 || d.type == 1
                        || d.createEcho || isBardCard(d))) score += 12;
                if (s.talents.contains("t_bard_chorus") && (d.createEcho || c.temp || d.draw > 0
                        || d.energyGain > 0 || isBardCard(d))) score += 12;
                if (s.talents.contains("t_bard_grand") && (d.cost == 0 || d.draw > 0 || d.createEcho
                        || c.temp || d.vulnerable > 0 || d.bind > 0 || d.skillChargeGain > 0
                        || d.rarity == 2 || isBardCard(d))) score += 14;
                if (s.talents.contains("t_mirrorist_shard") && (d.cost == 0 || d.scry > 0 || c.temp
                        || isMirroristCard(d))) score += 12;
                if (s.talents.contains("t_mirrorist_guard") && (d.block > 0 || d.type == 1
                        || d.upgradeRandom || isMirroristCard(d))) score += 12;
                if (s.talents.contains("t_mirrorist_reflect") && (d.scry > 0 || d.upgradeRandom
                        || d.createEcho || c.temp || isHybridCore(d) || isMirroristCard(d))) score += 12;
                if (s.talents.contains("t_mirrorist_grand") && (d.scry > 0 || d.upgradeRandom
                        || d.createEcho || c.temp || d.skillChargeGain > 0 || d.rarity == 2
                        || isHybridCore(d) || isConfluenceCore(d) || isMirroristCard(d))) score += 14;
                if (s.talents.contains("t_puppeteer_thread") && (d.cost == 0 || d.bind > 0 || c.temp
                        || isPuppeteerCard(d))) score += 12;
                if (s.talents.contains("t_puppeteer_screen") && (d.block > 0 || d.type == 1
                        || d.createEcho || isPuppeteerCard(d))) score += 12;
                if (s.talents.contains("t_puppeteer_rehearse") && (d.createEcho || c.temp
                        || d.draw > 0 || d.skillChargeGain > 0 || isPuppeteerCard(d))) score += 12;
                if (s.talents.contains("t_puppeteer_grand") && (d.bind > 0 || d.createEcho
                        || c.temp || d.block > 0 || d.skillChargeGain > 0 || d.rarity == 2
                        || isPuppeteerCard(d))) score += 14;
                if (s.talents.contains("t_scavenger_salvage") && (d.cost == 0 || d.draw > 0 || d.goldGain > 0
                        || "wound".equals(c.id) || "daze".equals(c.id) || isScavengerCard(d))) score += 12;
                if (s.talents.contains("t_scavenger_patch") && (d.block > 0 || d.heal > 0 || d.type == 1
                        || "wound".equals(c.id) || "daze".equals(c.id) || isScavengerCard(d))) score += 12;
                if (s.talents.contains("t_scavenger_market") && (d.goldGain > 0 || d.draw > 0 || d.exhaust
                        || d.skillChargeGain > 0 || isScavengerCard(d))) score += 12;
                if (s.talents.contains("t_scavenger_grand") && (d.goldGain > 0 || d.draw > 0 || d.exhaust
                        || d.skillChargeGain > 0 || d.rarity == 2 || isScavengerCard(d))) score += 14;
                if (s.talents.contains("t_lightkeeper_keeper") && (d.cost == 0 || d.draw > 0 || c.temp
                        || d.createEcho || isLightkeeperCard(d))) score += 12;
                if (s.talents.contains("t_lightkeeper_vigil") && (d.block > 0 || d.type == 1
                        || d.createEcho || c.temp || isLightkeeperCard(d))) score += 12;
                if (s.talents.contains("t_lightkeeper_prism") && (d.exhaust || d.scry > 0 || d.upgradeRandom
                        || "wound".equals(c.id) || "daze".equals(c.id) || isLightkeeperCard(d))) score += 12;
                if (s.talents.contains("t_lightkeeper_grand") && (d.exhaust || c.temp || d.createEcho
                        || d.scry > 0 || d.upgradeRandom || d.skillChargeGain > 0 || d.rarity == 2
                        || isLightkeeperCard(d))) score += 14;
                if (s.talents.contains("t_geomancer_rune") && (d.cost == 0 || d.scry > 0 || d.upgradeRandom
                        || isHybridCore(d) || isGeomancerCard(d))) score += 12;
                if (s.talents.contains("t_geomancer_mantle") && (d.block > 0 || d.type == 1
                        || d.bind > 0 || isGeomancerCard(d))) score += 12;
                if (s.talents.contains("t_geomancer_quake") && (d.burn > 0 || d.bind > 0 || d.vulnerable > 0
                        || d.skillChargeGain > 0 || isGeomancerCard(d))) score += 12;
                if (s.talents.contains("t_geomancer_grand") && (d.block > 0 || d.burn > 0 || d.bind > 0
                        || d.skillChargeGain > 0 || d.rarity == 2 || isGeomancerCard(d))) score += 14;
                if (s.talents.contains("t_witch_brew") && (d.createPotion || d.cost == 0 || d.draw > 0
                        || d.createEcho || isWitchCard(d))) score += 12;
                if (s.talents.contains("t_witch_ward") && (d.block > 0 || d.heal > 0 || d.type == 1
                        || "wound".equals(c.id) || "daze".equals(c.id) || isWitchCard(d))) score += 12;
                if (s.talents.contains("t_witch_curse") && (d.burn > 0 || d.bind > 0 || d.vulnerable > 0
                        || d.skillChargeGain > 0 || isWitchCard(d))) score += 12;
                if (s.talents.contains("t_witch_grand") && (d.createPotion || d.burn > 0 || d.bind > 0
                        || d.createEcho || d.skillChargeGain > 0 || d.rarity == 2 || isWitchCard(d))) score += 14;
                if (s.talents.contains("t_shifter_slip") && (d.cost == 0 || d.draw > 0 || d.energyGain > 0
                        || d.createEcho || isShifterCard(d))) score += 12;
                if (s.talents.contains("t_shifter_anchor") && (d.block > 0 || d.exhaust || d.exhaustTopDiscard
                        || d.createEcho || c.temp || isShifterCard(d))) score += 12;
                if (s.talents.contains("t_shifter_lance") && (d.vulnerable > 0 || d.bind > 0
                        || d.skillChargeGain > 0 || d.cost == 0 || isShifterCard(d))) score += 12;
                if (s.talents.contains("t_shifter_grand") && (d.cost == 0 || d.draw > 0 || d.energyGain > 0
                        || d.createEcho || d.skillChargeGain > 0 || d.rarity == 2 || isShifterCard(d))) score += 14;
                if (s.talents.contains("t_fateseer_omen") && (d.scry > 0 || d.draw > 0 || d.upgradeRandom
                        || c.upgraded || isFateseerCard(d))) score += 12;
                if (s.talents.contains("t_fateseer_veil") && (d.block > 0 || d.type == 1 || d.scry > 0
                        || d.upgradeRandom || c.upgraded || isFateseerCard(d))) score += 12;
                if (s.talents.contains("t_fateseer_thread") && (d.vulnerable > 0 || d.bind > 0
                        || d.skillChargeGain > 0 || d.scry > 0 || isFateseerCard(d))) score += 12;
                if (s.talents.contains("t_fateseer_grand") && (d.scry > 0 || d.upgradeRandom || c.upgraded
                        || d.draw > 0 || d.skillChargeGain > 0 || d.rarity == 2 || isFateseerCard(d))) score += 14;
                if (s.talents.contains("t_tidecaller_ripple") && (d.cost == 0 || d.draw > 0
                        || d.energyGain > 0 || d.bind > 0 || isTidecallerCard(d))) score += 12;
                if (s.talents.contains("t_tidecaller_breaker") && (d.block > 0 || d.type == 1
                        || d.bind > 0 || d.draw > 0 || isTidecallerCard(d))) score += 12;
                if (s.talents.contains("t_tidecaller_surge") && (d.vulnerable > 0 || d.bind > 0
                        || d.skillChargeGain > 0 || d.block > 0 || isTidecallerCard(d))) score += 12;
                if (s.talents.contains("t_tidecaller_grand") && (d.block > 0 || d.bind > 0
                        || d.draw > 0 || d.skillChargeGain > 0 || d.rarity == 2 || isTidecallerCard(d))) score += 14;
                if (s.talents.contains("t_frostbinder_shard") && (d.cost == 0 || d.draw > 0
                        || d.bind > 0 || "wound".equals(c.id) || "daze".equals(c.id) || isFrostbinderCard(d))) score += 12;
                if (s.talents.contains("t_frostbinder_ward") && (d.block > 0 || d.type == 1
                        || d.draw > 0 || "wound".equals(c.id) || "daze".equals(c.id) || isFrostbinderCard(d))) score += 12;
                if (s.talents.contains("t_frostbinder_shatter") && (d.vulnerable > 0 || d.bind > 0
                        || d.skillChargeGain > 0 || "wound".equals(c.id) || "daze".equals(c.id) || isFrostbinderCard(d))) score += 12;
                if (s.talents.contains("t_frostbinder_grand") && (d.block > 0 || d.bind > 0
                        || d.draw > 0 || d.skillChargeGain > 0 || d.rarity == 2 || isFrostbinderCard(d))) score += 14;
                if (s.talents.contains("t_plaguedoctor_lancet") && (d.createPotion || d.cost == 0 || d.draw > 0
                        || d.burn > 0 || isPlaguedoctorCard(d))) score += 12;
                if (s.talents.contains("t_plaguedoctor_mask") && (d.block > 0 || d.heal > 0 || d.type == 1
                        || "wound".equals(c.id) || "daze".equals(c.id) || isPlaguedoctorCard(d))) score += 12;
                if (s.talents.contains("t_plaguedoctor_quarantine") && (d.burn > 0 || d.bind > 0 || d.vulnerable > 0
                        || d.skillChargeGain > 0 || isPlaguedoctorCard(d))) score += 12;
                if (s.talents.contains("t_plaguedoctor_grand") && (d.createPotion || d.burn > 0 || d.bind > 0
                        || d.heal > 0 || d.skillChargeGain > 0 || d.rarity == 2 || isPlaguedoctorCard(d))) score += 14;
                if (s.talents.contains("t_archivist_index") && (d.scry > 0 || d.draw > 0 || d.cost == 0
                        || d.upgradeRandom || c.upgraded || isArchivistCard(d))) score += 12;
                if (s.talents.contains("t_archivist_seal") && (d.block > 0 || d.type == 1 || d.upgradeRandom
                        || "wound".equals(c.id) || "daze".equals(c.id) || isArchivistCard(d))) score += 12;
                if (s.talents.contains("t_archivist_redline") && (d.vulnerable > 0 || d.bind > 0
                        || d.skillChargeGain > 0 || d.exhaustTopDiscard || isArchivistCard(d))) score += 12;
                if (s.talents.contains("t_archivist_grand") && (d.scry > 0 || d.upgradeRandom || c.upgraded
                        || d.draw > 0 || d.skillChargeGain > 0 || d.rarity == 2 || isArchivistCard(d))) score += 14;
                if (s.talents.contains("t_voidnavigator_beacon") && (d.cost == 0 || d.scry > 0 || d.draw > 0
                        || d.createEcho || c.temp || isVoidnavigatorCard(d))) score += 12;
                if (s.talents.contains("t_voidnavigator_anchor") && (d.block > 0 || d.type == 1 || d.createEcho
                        || d.exhaust || c.temp || isVoidnavigatorCard(d))) score += 12;
                if (s.talents.contains("t_voidnavigator_rift") && (d.vulnerable > 0 || d.bind > 0
                        || d.skillChargeGain > 0 || d.exhaust || d.exhaustTopDiscard || isVoidnavigatorCard(d))) score += 12;
                if (s.talents.contains("t_voidnavigator_grand") && (d.createEcho || c.temp || d.exhaust
                        || d.draw > 0 || d.skillChargeGain > 0 || d.rarity == 2 || isVoidnavigatorCard(d))) score += 14;
                if (s.talents.contains("t_relicsmith_key") && (d.goldGain > 0 || d.goldDamage
                        || d.draw > 0 || d.cost == 0 || isRelicsmithCard(d))) score += 12;
                if (s.talents.contains("t_relicsmith_lock") && (d.block > 0 || d.goldBlock || d.type == 1
                        || d.upgradeRandom || isRelicsmithCard(d))) score += 12;
                if (s.talents.contains("t_relicsmith_gauge") && (d.upgradeRandom || c.upgraded
                        || d.vulnerable > 0 || d.bind > 0 || d.skillChargeGain > 0 || isRelicsmithCard(d))) score += 12;
                if (s.talents.contains("t_relicsmith_grand") && (d.goldGain > 0 || d.goldDamage || d.goldBlock
                        || d.upgradeRandom || d.block > 0 || d.skillChargeGain > 0 || d.rarity == 2
                        || isRelicsmithCard(d))) score += 14;
                if (s.talents.contains("t_beastmaster_claw") && (d.cost == 0 || d.draw > 0 || d.createEcho
                        || c.temp || d.bind > 0 || isBeastmasterCard(d))) score += 12;
                if (s.talents.contains("t_beastmaster_hide") && (d.block > 0 || d.heal > 0 || d.type == 1
                        || d.createEcho || c.temp || isBeastmasterCard(d))) score += 12;
                if (s.talents.contains("t_beastmaster_den") && (d.bind > 0 || d.vulnerable > 0
                        || d.skillChargeGain > 0 || d.createEcho || c.temp || isBeastmasterCard(d))) score += 12;
                if (s.talents.contains("t_beastmaster_grand") && (d.createEcho || c.temp || d.bind > 0
                        || d.heal > 0 || d.skillChargeGain > 0 || d.rarity == 2 || isBeastmasterCard(d))) score += 14;
                if (s.talents.contains("t_dragonbinder_spark") && (d.cost == 0 || d.draw > 0 || d.createEcho
                        || c.temp || d.burn > 0 || isDragonbinderCard(d))) score += 12;
                if (s.talents.contains("t_dragonbinder_scale") && (d.block > 0 || d.heal > 0 || d.type == 1
                        || d.createEcho || c.temp || isDragonbinderCard(d))) score += 12;
                if (s.talents.contains("t_dragonbinder_hatch") && (d.burn > 0 || d.vulnerable > 0
                        || d.skillChargeGain > 0 || d.createEcho || c.temp || isDragonbinderCard(d))) score += 12;
                if (s.talents.contains("t_dragonbinder_grand") && (d.createEcho || c.temp || d.burn > 0
                        || d.heal > 0 || d.skillChargeGain > 0 || d.rarity == 2 || isDragonbinderCard(d))) score += 14;
                if (s.talents.contains("t_soulbinder_thread") && (d.cost == 0 || d.draw > 0 || d.createEcho
                        || c.temp || d.exhaust || d.bind > 0 || isSoulbinderCard(d))) score += 12;
                if (s.talents.contains("t_soulbinder_veil") && (d.block > 0 || d.heal > 0 || d.type == 1
                        || d.createEcho || c.temp || isSoulbinderCard(d))) score += 12;
                if (s.talents.contains("t_soulbinder_pact") && (d.bind > 0 || d.vulnerable > 0
                        || d.skillChargeGain > 0 || d.exhaust || d.createEcho || c.temp || isSoulbinderCard(d))) score += 12;
                if (s.talents.contains("t_soulbinder_grand") && (d.createEcho || c.temp || d.exhaust || d.bind > 0
                        || d.heal > 0 || d.skillChargeGain > 0 || d.rarity == 2 || isSoulbinderCard(d))) score += 14;
                if (s.talents.contains("t_starforger_spark") && (d.cost == 0 || d.draw > 0 || d.upgradeRandom
                        || c.upgraded || d.burn > 0 || isStarforgerCard(d))) score += 12;
                if (s.talents.contains("t_starforger_guard") && (d.block > 0 || d.type == 1
                        || d.upgradeRandom || c.upgraded || isStarforgerCard(d))) score += 12;
                if (s.talents.contains("t_starforger_crucible") && (d.burn > 0 || d.vulnerable > 0
                        || d.skillChargeGain > 0 || d.upgradeRandom || c.upgraded || isStarforgerCard(d))) score += 12;
                if (s.talents.contains("t_starforger_grand") && (d.upgradeRandom || c.upgraded || d.burn > 0
                        || d.block > 0 || d.skillChargeGain > 0 || d.rarity == 2 || isStarforgerCard(d))) score += 14;
                if (s.talents.contains("t_pathfinder_mark") && (d.cost == 0 || d.draw > 0
                        || d.scry > 0 || d.bind > 0 || isPathfinderCard(d))) score += 12;
                if (s.talents.contains("t_pathfinder_shelter") && (d.block > 0 || d.type == 1
                        || d.scry > 0 || d.upgradeRandom || isPathfinderCard(d))) score += 12;
                if (s.talents.contains("t_pathfinder_survey") && (d.scry > 0 || d.vulnerable > 0
                        || d.bind > 0 || d.skillChargeGain > 0 || isPathfinderCard(d))) score += 12;
                if (s.talents.contains("t_pathfinder_grand") && (d.scry > 0 || d.block > 0
                        || d.draw > 0 || d.skillChargeGain > 0 || d.rarity == 2 || isPathfinderCard(d))) score += 14;
                if (s.talents.contains("t_arrayist_glyph") && (d.cost == 0 || d.draw > 0
                        || d.energyGain > 0 || d.createEcho || c.temp || isArrayistCard(d))) score += 12;
                if (s.talents.contains("t_arrayist_bastion") && (d.block > 0 || d.type == 1
                        || d.upgradeRandom || c.temp || isArrayistCard(d))) score += 12;
                if (s.talents.contains("t_arrayist_pivot") && (d.createEcho || c.temp || d.draw > 0
                        || d.skillChargeGain > 0 || isHybridCore(d) || isArrayistCard(d))) score += 12;
                if (s.talents.contains("t_arrayist_grand") && (d.cost == 0 || d.draw > 0
                        || d.block > 0 || d.createEcho || c.temp || d.skillChargeGain > 0
                        || d.rarity == 2 || isArrayistCard(d))) score += 14;
                if (s.talents.contains("t_gambiter_pawn") && (d.cost == 0 || d.draw > 0
                        || d.type == 0 || d.bind > 0 || isGambiterCard(d))) score += 12;
                if (s.talents.contains("t_gambiter_castle") && (d.block > 0 || d.type == 1
                        || d.upgradeRandom || d.draw > 0 || isGambiterCard(d))) score += 12;
                if (s.talents.contains("t_gambiter_fork") && (d.bind > 0 || d.vulnerable > 0
                        || d.skillChargeGain > 0 || isHybridCore(d) || isGambiterCard(d))) score += 12;
                if (s.talents.contains("t_gambiter_grand") && (d.cost == 0 || d.draw > 0
                        || d.block > 0 || d.skillChargeGain > 0 || d.rarity == 2
                        || d.upgradeRandom || isGambiterCard(d))) score += 14;
                if (s.talents.contains("t_gravekeeper_lantern") && (d.draw > 0 || d.exhaust || d.exhaustTopDiscard
                        || d.bind > 0 || "wound".equals(c.id) || "daze".equals(c.id) || isGravekeeperCard(d))) score += 12;
                if (s.talents.contains("t_gravekeeper_shroud") && (d.block > 0 || d.heal > 0 || d.type == 1
                        || "wound".equals(c.id) || "daze".equals(c.id) || isGravekeeperCard(d))) score += 12;
                if (s.talents.contains("t_gravekeeper_interment") && (d.exhaust || d.exhaustTopDiscard || d.createWound
                        || d.bind > 0 || d.skillChargeGain > 0 || "wound".equals(c.id) || "daze".equals(c.id)
                        || isGravekeeperCard(d))) score += 12;
                if (s.talents.contains("t_gravekeeper_grand") && (d.exhaust || d.exhaustTopDiscard || d.heal > 0
                        || d.block > 0 || d.skillChargeGain > 0 || d.rarity == 2
                        || "wound".equals(c.id) || "daze".equals(c.id) || isGravekeeperCard(d))) score += 14;
                if (s.talents.contains("t_treasurer_entry") && (d.goldGain > 0 || d.draw > 0
                        || d.goldDamage || d.goldBlock || isTreasurerCard(d))) score += 12;
                if (s.talents.contains("t_treasurer_vault") && (d.block > 0 || d.goldBlock || d.type == 1
                        || d.skillChargeGain > 0 || isTreasurerCard(d))) score += 12;
                if (s.talents.contains("t_treasurer_audit") && (d.upgradeRandom || c.upgraded
                        || d.vulnerable > 0 || d.bind > 0 || d.skillChargeGain > 0 || isTreasurerCard(d))) score += 12;
                if (s.talents.contains("t_treasurer_grand") && (d.goldGain > 0 || d.goldDamage || d.goldBlock
                        || d.upgradeRandom || d.block > 0 || d.skillChargeGain > 0 || d.rarity == 2
                        || isTreasurerCard(d))) score += 14;
                if (s.talents.contains("t_drifter_scout") && (isOffPoolCard(s, d) || c.temp
                        || d.draw > 0 || d.createEcho || isDrifterCard(d))) score += 12;
                if (s.talents.contains("t_drifter_hideout") && (d.block > 0 || d.type == 1
                        || c.temp || d.createEcho || isDrifterCard(d))) score += 12;
                if (s.talents.contains("t_drifter_patch") && (d.upgradeRandom || c.upgraded
                        || d.vulnerable > 0 || d.bind > 0 || d.skillChargeGain > 0
                        || isOffPoolCard(s, d) || isDrifterCard(d))) score += 12;
                if (s.talents.contains("t_drifter_grand") && (isOffPoolCard(s, d) || c.temp || d.draw > 0
                        || d.upgradeRandom || d.skillChargeGain > 0 || d.rarity == 2
                        || isDrifterCard(d))) score += 14;
                if (s.talents.contains("t_oathkeeper_vow") && (d.block > 0 || d.heal > 0
                        || d.draw > 0 || d.vulnerable > 0 || isOathkeeperCard(d))) score += 12;
                if (s.talents.contains("t_oathkeeper_guard") && (d.block > 0 || d.heal > 0 || d.type == 1
                        || d.skillChargeGain > 0 || isOathkeeperCard(d))) score += 12;
                if (s.talents.contains("t_oathkeeper_sanctum") && (d.upgradeRandom || c.upgraded
                        || d.vulnerable > 0 || d.bind > 0 || d.skillChargeGain > 0 || isOathkeeperCard(d))) score += 12;
                if (s.talents.contains("t_oathkeeper_grand") && (d.block > 0 || d.heal > 0
                        || d.upgradeRandom || d.skillChargeGain > 0 || d.rarity == 2
                        || isOathkeeperCard(d))) score += 14;
                if (s.talents.contains("t_moonsinger_newmoon") && (d.scry > 0 || d.draw > 0
                        || d.vulnerable > 0 || isMoonsingerCard(d))) score += 12;
                if (s.talents.contains("t_moonsinger_crescent") && (d.block > 0 || d.heal > 0
                        || d.draw > 0 || d.type == 1 || isMoonsingerCard(d))) score += 12;
                if (s.talents.contains("t_moonsinger_tide") && (d.createEcho || c.temp || d.upgradeRandom
                        || d.skillChargeGain > 0 || c.upgraded || isMoonsingerCard(d))) score += 12;
                if (s.talents.contains("t_moonsinger_grand") && (d.scry > 0 || d.draw > 0
                        || d.createEcho || c.temp || d.upgradeRandom || d.skillChargeGain > 0
                        || d.rarity == 2 || isMoonsingerCard(d))) score += 14;
                if (s.talents.contains("t_spy_contact") && (d.cost == 0 || d.draw > 0
                        || d.goldGain > 0 || d.vulnerable > 0 || isSpyCard(d))) score += 12;
                if (s.talents.contains("t_spy_smoke") && (d.block > 0 || d.goldGain > 0
                        || d.draw > 0 || d.type == 1 || isSpyCard(d))) score += 12;
                if (s.talents.contains("t_spy_blackmail") && (d.upgradeRandom || c.upgraded
                        || d.vulnerable > 0 || d.bind > 0 || d.skillChargeGain > 0 || isSpyCard(d))) score += 12;
                if (s.talents.contains("t_spy_grand") && (d.cost == 0 || d.draw > 0
                        || d.goldGain > 0 || d.createEcho || c.temp || d.upgradeRandom
                        || d.skillChargeGain > 0 || d.rarity == 2 || isSpyCard(d))) score += 14;
                if (s.talents.contains("t_perfumer_note") && (d.createPotion || d.heal > 0
                        || d.draw > 0 || d.burn > 0 || isPerfumerCard(d))) score += 12;
                if (s.talents.contains("t_perfumer_mist") && (d.block > 0 || d.heal > 0 || d.createPotion
                        || d.type == 1 || isPerfumerCard(d))) score += 12;
                if (s.talents.contains("t_perfumer_distill") && (d.upgradeRandom || c.upgraded
                        || d.burn > 0 || d.bind > 0 || d.skillChargeGain > 0 || isPerfumerCard(d))) score += 12;
                if (s.talents.contains("t_perfumer_grand") && (d.createPotion || d.heal > 0
                        || d.burn > 0 || d.bind > 0 || d.upgradeRandom || d.skillChargeGain > 0
                        || d.rarity == 2 || isPerfumerCard(d))) score += 14;
                if (s.talents.contains("t_clocksmith_tick") && (d.cost == 0 || d.draw > 0
                        || d.energyGain > 0 || d.vulnerable > 0 || isClocksmithCard(d))) score += 12;
                if (s.talents.contains("t_clocksmith_spring") && (d.block > 0 || d.draw > 0
                        || d.type == 1 || d.skillChargeGain > 0 || isClocksmithCard(d))) score += 12;
                if (s.talents.contains("t_clocksmith_gear") && (d.createEcho || c.temp || d.upgradeRandom
                        || c.upgraded || d.skillChargeGain > 0 || isClocksmithCard(d))) score += 12;
                if (s.talents.contains("t_clocksmith_grand") && (d.cost == 0 || d.draw > 0
                        || d.createEcho || c.temp || d.upgradeRandom || d.skillChargeGain > 0
                        || d.rarity == 2 || isClocksmithCard(d))) score += 14;
                if (s.talents.contains("t_mintsmith_spark") && (d.goldGain > 0 || d.burn > 0
                        || d.draw > 0 || d.vulnerable > 0 || isMintsmithCard(d))) score += 12;
                if (s.talents.contains("t_mintsmith_mold") && (d.block > 0 || d.goldBlock
                        || d.type == 1 || d.skillChargeGain > 0 || isMintsmithCard(d))) score += 12;
                if (s.talents.contains("t_mintsmith_tax") && (d.upgradeRandom || d.burn > 0
                        || d.goldGain > 0 || d.bind > 0 || d.skillChargeGain > 0 || isMintsmithCard(d))) score += 12;
                if (s.talents.contains("t_mintsmith_grand") && (d.goldGain > 0 || d.burn > 0
                        || d.upgradeRandom || d.skillChargeGain > 0 || d.rarity == 2 || isMintsmithCard(d))) score += 14;
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
                if ("machinist_grand_engine".equals(c.id) || "machinist_overdrive".equals(c.id)
                        || "machinist_cogcall".equals(c.id) || "machinist_blueprint".equals(c.id)) score += 14;
                if ("chronomancer_time_engine".equals(c.id) || "chronomancer_overloop".equals(c.id)
                        || "chronomancer_loop".equals(c.id) || "chronomancer_tick".equals(c.id)) score += 14;
                if ("pactmaker_grand_contract".equals(c.id) || "pactmaker_overdeal".equals(c.id)
                        || "pactmaker_witness".equals(c.id) || "pactmaker_collection".equals(c.id)
                        || "pactmaker_clause".equals(c.id)) score += 16;
                if ("stormcaller_tempest_crown".equals(c.id) || "stormcaller_overstorm".equals(c.id)) score += 18;
                if ("stormcaller_chain".equals(c.id) || "stormcaller_sparkline".equals(c.id)
                        || "stormcaller_barrier".equals(c.id) || "stormcaller_gust".equals(c.id)) score += 14;
                if ("shadowdancer_eclipse".equals(c.id) || "shadowdancer_overstrike".equals(c.id)) score += 18;
                if ("shadowdancer_blade".equals(c.id) || "shadowdancer_veil".equals(c.id)
                        || "shadowdancer_step".equals(c.id) || "shadowdancer_mark".equals(c.id)) score += 14;
                if ("runeblade_grand_seal".equals(c.id) || "runeblade_overglyph".equals(c.id)) score += 18;
                if ("runeblade_glyphcut".equals(c.id) || "runeblade_ward".equals(c.id)
                        || "runeblade_inscribe".equals(c.id) || "runeblade_cleave".equals(c.id)) score += 14;
                if ("medium_grand_seance".equals(c.id) || "medium_overtrance".equals(c.id)) score += 18;
                if ("medium_whisper".equals(c.id) || "medium_veil".equals(c.id)
                        || "medium_oracle".equals(c.id) || "medium_binding".equals(c.id)) score += 14;
                if ("tactician_grand_strategy".equals(c.id) || "tactician_overplan".equals(c.id)) score += 18;
                if ("tactician_probe".equals(c.id) || "tactician_bulwark".equals(c.id)
                        || "tactician_map".equals(c.id) || "tactician_flank".equals(c.id)) score += 14;
                if ("prismist_grand_spectrum".equals(c.id) || "prismist_overbeam".equals(c.id)) score += 18;
                if ("prismist_ray".equals(c.id) || "prismist_lens".equals(c.id)
                        || "prismist_anchor".equals(c.id) || "prismist_spill".equals(c.id)) score += 14;
                if ("dreamwalker_grand_dream".equals(c.id) || "dreamwalker_overdream".equals(c.id)) score += 18;
                if ("dreamwalker_drift".equals(c.id) || "dreamwalker_veil".equals(c.id)
                        || "dreamwalker_lucid".equals(c.id) || "dreamwalker_bind".equals(c.id)) score += 14;
                if ("gardener_grand_grove".equals(c.id) || "gardener_overgrowth".equals(c.id)) score += 18;
                if ("gardener_sprout".equals(c.id) || "gardener_rootwall".equals(c.id)
                        || "gardener_compost".equals(c.id) || "gardener_thornbloom".equals(c.id)) score += 14;
                if ("chef_grand_banquet".equals(c.id) || "chef_overcook".equals(c.id)) score += 18;
                if ("chef_prep".equals(c.id) || "chef_stew".equals(c.id)
                        || "chef_spice".equals(c.id) || "chef_sizzle".equals(c.id)) score += 14;
                if ("bard_grand_finale".equals(c.id) || "bard_overcrescendo".equals(c.id)) score += 18;
                if ("bard_note".equals(c.id) || "bard_ballad".equals(c.id)
                        || "bard_chorus".equals(c.id) || "bard_discord".equals(c.id)) score += 14;
                if ("mirrorist_grand_mirror".equals(c.id) || "mirrorist_overimage".equals(c.id)) score += 18;
                if ("mirrorist_shard".equals(c.id) || "mirrorist_guard".equals(c.id)
                        || "mirrorist_reflect".equals(c.id) || "mirrorist_prismcut".equals(c.id)) score += 14;
                if ("puppeteer_grand_stage".equals(c.id) || "puppeteer_overpull".equals(c.id)) score += 18;
                if ("puppeteer_thread".equals(c.id) || "puppeteer_screen".equals(c.id)
                        || "puppeteer_rehearse".equals(c.id) || "puppeteer_needle".equals(c.id)) score += 14;
                if ("scavenger_grand_foundry".equals(c.id) || "scavenger_overhaul".equals(c.id)) score += 18;
                if ("scavenger_pick".equals(c.id) || "scavenger_sort".equals(c.id)
                        || "scavenger_patch".equals(c.id) || "scavenger_magnet".equals(c.id)) score += 14;
                if ("geomancer_grand_fault".equals(c.id) || "geomancer_overquake".equals(c.id)) score += 18;
                if ("geomancer_rune".equals(c.id) || "geomancer_mantle".equals(c.id)
                        || "geomancer_quake".equals(c.id) || "geomancer_geode".equals(c.id)) score += 14;
                if ("witch_grand_cauldron".equals(c.id) || "witch_overbrew".equals(c.id)) score += 18;
                if ("witch_brew".equals(c.id) || "witch_ward".equals(c.id)
                        || "witch_curse".equals(c.id) || "witch_charm".equals(c.id)) score += 14;
                if ("shifter_grand_paradox".equals(c.id) || "shifter_overblink".equals(c.id)) score += 18;
                if ("shifter_slip".equals(c.id) || "shifter_screen".equals(c.id)
                        || "shifter_anchor".equals(c.id) || "shifter_lance".equals(c.id)) score += 14;
                if ("frostbinder_grand_winter".equals(c.id) || "frostbinder_overfreeze".equals(c.id)) score += 18;
                if ("frostbinder_shard".equals(c.id) || "frostbinder_ward".equals(c.id)
                        || "frostbinder_rime".equals(c.id) || "frostbinder_shatter".equals(c.id)) score += 14;
                if ("plaguedoctor_grand_plague".equals(c.id) || "plaguedoctor_overdose".equals(c.id)) score += 18;
                if ("plaguedoctor_lancet".equals(c.id) || "plaguedoctor_mask".equals(c.id)
                        || "plaguedoctor_culture".equals(c.id) || "plaguedoctor_quarantine".equals(c.id)) score += 14;
                if ("archivist_grand_archive".equals(c.id) || "archivist_overfile".equals(c.id)) score += 18;
                if ("archivist_index".equals(c.id) || "archivist_seal".equals(c.id)
                        || "archivist_catalog".equals(c.id) || "archivist_redline".equals(c.id)) score += 14;
                if ("pathfinder_grand_route".equals(c.id) || "pathfinder_overroute".equals(c.id)) score += 18;
                if ("pathfinder_mark".equals(c.id) || "pathfinder_shelter".equals(c.id)
                        || "pathfinder_survey".equals(c.id) || "pathfinder_shortcut".equals(c.id)) score += 14;
                if ("arrayist_grand_array".equals(c.id) || "arrayist_overarray".equals(c.id)) score += 18;
                if ("arrayist_glyph".equals(c.id) || "arrayist_bastion".equals(c.id)
                        || "arrayist_pivot".equals(c.id) || "arrayist_lance".equals(c.id)) score += 14;
                if ("gambiter_grand_endgame".equals(c.id) || "gambiter_overmate".equals(c.id)) score += 18;
                if ("gambiter_pawn".equals(c.id) || "gambiter_castle".equals(c.id)
                        || "gambiter_gambit".equals(c.id) || "gambiter_fork".equals(c.id)) score += 14;
                if ("gravekeeper_grand_requiem".equals(c.id) || "gravekeeper_overwake".equals(c.id)) score += 18;
                if ("gravekeeper_lantern".equals(c.id) || "gravekeeper_shroud".equals(c.id)
                        || "gravekeeper_interment".equals(c.id) || "gravekeeper_dirge".equals(c.id)) score += 14;
                if ("treasurer_grand_balance".equals(c.id) || "treasurer_overledger".equals(c.id)) score += 18;
                if ("treasurer_entry".equals(c.id) || "treasurer_vault".equals(c.id)
                        || "treasurer_audit".equals(c.id) || "treasurer_collection".equals(c.id)) score += 14;
                if ("clocksmith_grand_chronogear".equals(c.id) || "clocksmith_overclock".equals(c.id)) score += 18;
                if ("clocksmith_tick".equals(c.id) || "clocksmith_spring".equals(c.id)
                        || "clocksmith_gear".equals(c.id) || "clocksmith_rewind".equals(c.id)) score += 14;
                if ("mintsmith_grand_mintage".equals(c.id) || "mintsmith_overmint".equals(c.id)) score += 18;
                if ("mintsmith_spark".equals(c.id) || "mintsmith_mold".equals(c.id)
                        || "mintsmith_tax".equals(c.id) || "mintsmith_assay".equals(c.id)) score += 14;
                if (isHybridCore(d)) score += 14;
                if (isConfluenceCore(d)) score += 16 + s.confluenceChain * 2;
                if (isCascadeSignal(d)) score += 10 + Math.min(12, s.cardsPlayedThisTurn * 2);
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
                if (s.relics.contains("resonance_prism") && (isHybridCore(d) || isConfluenceCore(d)
                        || GameCore.skillSpecCardBonus(s, d) >= 4)) score += 16;
                if (s.relics.contains("cascade_lattice") && isCascadeSignal(d)) score += 16;
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
                if (s.relics.contains("clockwork_core") && (d.upgradeRandom || d.createEcho || c.upgraded || d.skillChargeGain > 0)) score += 12;
                if (s.relics.contains("time_engine") && (d.skillChargeGain > 0 || d.createEcho || c.temp || d.energyGain > 0 || d.draw > 0)) score += 12;
                if (s.relics.contains("contract_stamp") && (d.goldGain > 0 || d.goldDamage || d.goldBlock || d.skillChargeGain > 0 || d.type == 1)) score += 12;
                if (s.relics.contains("grand_ledger") && (d.goldGain > 0 || d.hpLoss > 0 || d.createWound || d.vulnerable > 0 || d.bind > 0 || d.skillChargeGain > 0 || c.upgraded)) score += 14;
                if (s.relics.contains("storm_rod") && (d.burn > 0 || d.vulnerable > 0 || d.skillChargeGain > 0 || isStormcallerCard(d))) score += 14;
                if (s.relics.contains("tempest_crown") && (d.aoe || d.burn > 0 || d.skillChargeGain > 0 || d.rarity == 2 || isStormcallerCard(d))) score += 16;
                if (s.relics.contains("shadow_sash") && (d.cost == 0 || d.draw > 0 || d.energyGain > 0 || d.skillChargeGain > 0 || isShadowdancerCard(d))) score += 14;
                if (s.relics.contains("eclipse_mask") && (d.cost == 0 || d.createEcho || c.temp || d.skillChargeGain > 0 || d.rarity == 2 || isShadowdancerCard(d))) score += 16;
                if (s.relics.contains("rune_stylus") && (c.upgraded || d.upgradeRandom || d.scry > 0
                        || d.skillChargeGain > 0 || d.vulnerable > 0 || isRunebladeCard(d))) score += 14;
                if (s.relics.contains("grand_rune_blade") && (c.upgraded || d.skillChargeGain > 0 || d.upgradeRandom
                        || d.rarity == 2 || d.vulnerable > 0 || isRunebladeCard(d))) score += 16;
                if (s.relics.contains("spirit_planchette") && (isMediumSignal(d) || c.temp || d.createEcho
                        || d.exhaust || d.scry > 0 || d.bind > 0)) score += 14;
                if (s.relics.contains("ancestral_planchette") && (isMediumSignal(d) || c.temp || d.createEcho
                        || d.exhaust || d.skillChargeGain > 0 || d.rarity == 2 || d.bind > 0)) score += 16;
                if (s.relics.contains("war_table") && (isTacticianSignal(d) || c.temp || d.block > 0
                        || d.scry > 0 || d.upgradeRandom || d.skillChargeGain > 0 || d.vulnerable > 0)) score += 14;
                if (s.relics.contains("grand_war_room") && (isTacticianSignal(d) || c.temp || d.block > 0
                        || d.upgradeRandom || d.skillChargeGain > 0 || d.rarity == 2)) score += 16;
                if (s.relics.contains("refraction_dial") && (isPrismistSignal(d) || c.temp || isHybridCore(d)
                        || isConfluenceCore(d) || d.scry > 0 || d.upgradeRandom || d.skillChargeGain > 0 || d.draw > 0)) score += 14;
                if (s.relics.contains("spectrum_crown") && (isPrismistSignal(d) || c.temp || isHybridCore(d)
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.vulnerable > 0)) score += 16;
                if (s.relics.contains("dreamcatcher_charm") && (isDreamwalkerSignal(d) || c.temp
                        || d.scry > 0 || d.exhaust || d.createEcho || d.skillChargeGain > 0 || d.bind > 0)) score += 14;
                if (s.relics.contains("oneiric_crown") && (isDreamwalkerSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.bind > 0 || d.createEcho)) score += 16;
                if (s.relics.contains("seed_satchel") && (isGardenerSignal(d) || c.temp
                        || d.heal > 0 || d.gainWildEngine > 0 || d.block > 0 || d.bind > 0)) score += 14;
                if (s.relics.contains("verdant_crown") && (isGardenerSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.heal > 0 || d.gainWildEngine > 0 || d.bind > 0)) score += 16;
                if (s.relics.contains("recipe_book") && (isChefSignal(d) || c.temp
                        || d.heal > 0 || d.createPotion || d.burn > 0 || d.bind > 0)) score += 14;
                if (s.relics.contains("banquet_crown") && (isChefSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.heal > 0 || d.createPotion || d.burn > 0 || d.bind > 0)) score += 16;
                if (s.relics.contains("songbook") && (isBardSignal(d) || c.temp
                        || d.createEcho || d.draw > 0 || d.energyGain > 0 || d.vulnerable > 0 || d.bind > 0)) score += 14;
                if (s.relics.contains("finale_crown") && (isBardSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.draw > 0 || d.createEcho || d.vulnerable > 0)) score += 16;
                if (s.relics.contains("mirror_lens") && (isMirroristSignal(d) || c.temp
                        || d.scry > 0 || d.upgradeRandom || d.createEcho || d.draw > 0 || d.vulnerable > 0)) score += 14;
                if (s.relics.contains("mirror_crown") && (isMirroristSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.upgradeRandom || d.createEcho
                        || d.vulnerable > 0 || isHybridCore(d))) score += 16;
                if (s.relics.contains("string_spool") && (isPuppeteerSignal(d) || c.temp
                        || d.bind > 0 || d.createEcho || d.draw > 0 || d.block > 0 || d.skillChargeGain > 0)) score += 14;
                if (s.relics.contains("marionette_crown") && (isPuppeteerSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.createEcho || d.bind > 0 || d.block > 0)) score += 16;
                if (s.relics.contains("scrap_magnet") && (isScavengerSignal(d) || c.temp
                        || d.draw > 0 || d.goldGain > 0 || d.block > 0 || d.heal > 0 || d.skillChargeGain > 0)) score += 14;
                if (s.relics.contains("scrap_king_crown") && (isScavengerSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.draw > 0 || d.goldGain > 0 || d.vulnerable > 0 || d.bind > 0)) score += 16;
                if (s.relics.contains("salvage_hook") && (isSalvageSignal(d) || c.temp)) score += 14;
                if (s.relics.contains("faultline_core") && (isGeomancerSignal(d) || c.temp
                        || d.block > 0 || d.upgradeRandom || d.scry > 0 || isHybridCore(d))) score += 14;
                if (s.relics.contains("tectonic_crown") && (isGeomancerSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.vulnerable > 0 || isHybridCore(d))) score += 16;
                if (s.relics.contains("witch_bottle") && (isWitchSignal(d) || c.temp
                        || d.createPotion || d.burn > 0 || d.bind > 0 || d.createEcho || d.heal > 0)) score += 14;
                if (s.relics.contains("witch_moon_crown") && (isWitchSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.createPotion || d.createEcho)) score += 16;
                if (s.relics.contains("phase_lens") && (isShifterSignal(d) || c.temp
                        || d.cost == 0 || d.draw > 0 || d.energyGain > 0 || d.createEcho)) score += 14;
                if (s.relics.contains("phase_crown") && (isShifterSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.createEcho || d.energyGain > 0)) score += 16;
                if (s.relics.contains("fate_lantern") && (isFateseerSignal(d) || c.temp
                        || d.scry > 0 || d.upgradeRandom || d.draw > 0 || isHybridCore(d))) score += 14;
                if (s.relics.contains("fate_crown") && (isFateseerSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.upgradeRandom || isHybridCore(d))) score += 16;
                if (s.relics.contains("tide_shell") && (isTidecallerSignal(d) || c.temp
                        || d.block > 0 || d.bind > 0 || d.draw > 0)) score += 14;
                if (s.relics.contains("tide_crown") && (isTidecallerSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.block > 0 || d.bind > 0)) score += 16;
                if (s.relics.contains("frost_chain") && (isFrostbinderSignal(d) || c.temp
                        || d.block > 0 || d.bind > 0 || d.draw > 0 || d.exhaust)) score += 14;
                if (s.relics.contains("frost_crown") && (isFrostbinderSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.block > 0 || d.bind > 0)) score += 16;
                if (s.relics.contains("plague_case") && (isPlaguedoctorSignal(d) || c.temp
                        || d.createPotion || d.burn > 0 || d.bind > 0 || d.heal > 0)) score += 14;
                if (s.relics.contains("plague_crown") && (isPlaguedoctorSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.createPotion || d.burn > 0 || d.bind > 0)) score += 16;
                if (s.relics.contains("archive_key") && (isArchivistSignal(d) || c.temp
                        || d.scry > 0 || d.upgradeRandom || d.draw > 0 || d.exhaustTopDiscard)) score += 14;
                if (s.relics.contains("archive_crown") && (isArchivistSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.upgradeRandom || d.vulnerable > 0 || d.bind > 0)) score += 16;
                if (s.relics.contains("pathfinder_compass") && (isPathfinderSignal(d) || c.temp
                        || d.scry > 0 || d.draw > 0 || d.block > 0 || d.skillChargeGain > 0)) score += 14;
                if (s.relics.contains("route_crown") && (isPathfinderSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.upgradeRandom || d.bind > 0)) score += 16;
                if (s.relics.contains("array_disc") && (isArrayistSignal(d) || c.temp
                        || d.cost == 0 || d.draw > 0 || d.createEcho || d.block > 0 || d.skillChargeGain > 0)) score += 14;
                if (s.relics.contains("array_crown") && (isArrayistSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.createEcho || d.upgradeRandom)) score += 16;
                if (s.relics.contains("gambit_clock") && (isGambiterSignal(d) || c.temp
                        || d.cost == 0 || d.draw > 0 || d.block > 0 || d.bind > 0 || d.skillChargeGain > 0)) score += 14;
                if (s.relics.contains("checkmate_crown") && (isGambiterSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.upgradeRandom || d.bind > 0)) score += 16;
                if (s.relics.contains("grave_lantern") && (isGravekeeperSignal(d) || c.temp
                        || d.exhaust || d.exhaustTopDiscard || d.createWound || d.heal > 0 || d.block > 0
                        || d.draw > 0 || d.skillChargeGain > 0)) score += 14;
                if (s.relics.contains("requiem_crown") && (isGravekeeperSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.exhaust || d.exhaustTopDiscard
                        || d.bind > 0 || d.vulnerable > 0)) score += 16;
                if (s.relics.contains("treasury_key") && (isTreasurerSignal(d) || c.temp
                        || d.goldGain > 0 || d.goldDamage || d.goldBlock || d.block > 0
                        || d.draw > 0 || d.skillChargeGain > 0)) score += 14;
                if (s.relics.contains("audit_crown") && (isTreasurerSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.upgradeRandom
                        || d.goldGain > 0 || d.bind > 0 || d.vulnerable > 0)) score += 16;
                if (s.relics.contains("rift_pass") && (isDrifterSignal(s, d) || c.temp
                        || isOffPoolCard(s, d) || d.createEcho || d.draw > 0
                        || d.block > 0 || d.skillChargeGain > 0)) score += 14;
                if (s.relics.contains("junction_crown") && (isDrifterSignal(s, d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.upgradeRandom
                        || d.bind > 0 || d.vulnerable > 0)) score += 16;
                if (s.relics.contains("oath_seal") && (isOathkeeperSignal(d) || c.temp
                        || d.block > 0 || d.heal > 0 || d.draw > 0 || d.skillChargeGain > 0)) score += 14;
                if (s.relics.contains("judgment_crown") && (isOathkeeperSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.upgradeRandom
                        || d.block > 0 || d.heal > 0 || d.vulnerable > 0)) score += 16;
                if (s.relics.contains("moon_lyre") && (isMoonsingerSignal(d) || c.temp
                        || d.scry > 0 || d.draw > 0 || d.createEcho || d.skillChargeGain > 0)) score += 14;
                if (s.relics.contains("eclipse_crown") && (isMoonsingerSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.upgradeRandom
                        || d.createEcho || d.vulnerable > 0)) score += 16;
                if (s.relics.contains("cipher_ring") && (isSpySignal(d) || c.temp
                        || d.cost == 0 || d.draw > 0 || d.goldGain > 0 || d.createEcho
                        || d.skillChargeGain > 0)) score += 14;
                if (s.relics.contains("mastermind_crown") && (isSpySignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.upgradeRandom
                        || d.goldGain > 0 || d.vulnerable > 0)) score += 16;
                if (s.relics.contains("scent_vial") && (isPerfumerSignal(d) || c.temp
                        || d.createPotion || d.heal > 0 || d.burn > 0 || d.draw > 0
                        || d.skillChargeGain > 0)) score += 14;
                if (s.relics.contains("bouquet_crown") && (isPerfumerSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.upgradeRandom
                        || d.burn > 0 || d.vulnerable > 0)) score += 16;
                if (s.relics.contains("clockwork_key") && (isClocksmithSignal(d) || c.temp
                        || d.cost == 0 || d.draw > 0 || d.createEcho || d.skillChargeGain > 0)) score += 14;
                if (s.relics.contains("chrono_crown") && (isClocksmithSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.upgradeRandom
                        || d.bind > 0 || d.vulnerable > 0)) score += 16;
                if (s.relics.contains("mint_tongs") && (isMintsmithSignal(d) || c.temp
                        || d.goldGain > 0 || d.burn > 0 || d.block > 0 || d.skillChargeGain > 0)) score += 14;
                if (s.relics.contains("mint_crown") && (isMintsmithSignal(d) || c.temp
                        || d.skillChargeGain > 0 || d.rarity == 2 || d.upgradeRandom
                        || d.goldGain > 0 || d.burn > 0 || d.vulnerable > 0)) score += 16;
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
        if (hasBuildCoreTalent(s) && s.skillSpec != null && s.skillSpec.length() > 0
                && (overload >= 1 || s.turn >= 2 || s.combatKind == 'E' || s.combatKind == 'B')) {
            return true;
        }
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
        if (GameCore.PROF_PACTMAKER.equals(s.profession)
                && (s.questComplete || s.pactFulfilled > 0 || s.gold >= 140 || s.enemies.size() > 1 || overload >= 1)) {
            return true;
        }
        if (GameCore.PROF_STORMCALLER.equals(s.profession)
                && (s.enemies.size() > 1 || s.professionCharge >= 4 || overload >= 1
                || stormcallerEnemyPressure(s) >= 6 || s.combatKind == 'E' || s.combatKind == 'B')) {
            return true;
        }
        if (GameCore.PROF_SHADOWDANCER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean executionWindow = target >= 0 && (s.enemies.get(target).mark >= 3 || s.enemies.get(target).vulnerable > 0);
            if (s.professionCharge >= 4 || overload >= 1 || executionWindow || s.cardsPlayedThisTurn >= 3
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_RUNEBLADE.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean executionWindow = target >= 0 && (s.enemies.get(target).mark >= 2 || s.enemies.get(target).vulnerable > 0);
            int upgraded = upgradedDeckCards(s);
            if (s.professionCharge >= 4 || overload >= 1 || executionWindow || upgraded >= 8
                    || (upgraded >= 6 && s.block >= 8) || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_MEDIUM.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean seanceWindow = target >= 0 && (s.enemies.get(target).bind >= 2
                    || s.enemies.get(target).mark >= 2 || s.enemies.get(target).vulnerable > 0);
            int handSignals = tempOrEchoHandCards(s);
            if (s.professionCharge >= 4 || overload >= 1 || seanceWindow || handSignals >= 3
                    || (handSignals >= 2 && mediumEnemyPressure(s) >= 6) || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_TACTICIAN.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean formationWindow = target >= 0 && (s.enemies.get(target).mark >= 2 || s.enemies.get(target).vulnerable > 0);
            int upgraded = upgradedDeckCards(s);
            if (s.professionCharge >= 4 || overload >= 1 || formationWindow || s.block >= 16 + s.act * 3
                    || upgraded >= 8 || s.confluenceChain >= 4 || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_PRISMIST.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean refractionWindow = target >= 0 && (s.enemies.get(target).mark >= 2 || s.enemies.get(target).vulnerable > 0);
            int upgraded = upgradedDeckCards(s);
            if (s.professionCharge >= 4 || overload >= 1 || refractionWindow || s.confluenceChain >= 4
                    || upgraded >= 7 || prismistEnemyPressure(s) >= 7 || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_DREAMWALKER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean dreamWindow = target >= 0 && (s.enemies.get(target).bind >= 2
                    || s.enemies.get(target).mark >= 2 || s.enemies.get(target).vulnerable > 0);
            int handSignals = tempOrEchoHandCards(s);
            if (s.professionCharge >= 4 || overload >= 1 || dreamWindow || handSignals >= 3
                    || statusDeckCards(s) >= 2 || dreamwalkerEnemyPressure(s) >= 7
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_GARDENER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean gardenWindow = target >= 0 && (s.enemies.get(target).bind >= 2
                    || s.enemies.get(target).mark >= 2 || s.enemies.get(target).vulnerable > 0);
            if (s.professionCharge >= 4 || overload >= 1 || gardenWindow || s.block >= 16 + s.act * 2
                    || statusDeckCards(s) >= 1 || gardenerEnemyPressure(s) >= 7 || s.hp < s.maxHp * 0.75f
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_CHEF.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean banquetWindow = target >= 0 && (s.enemies.get(target).burn + s.enemies.get(target).bind >= 4
                    || s.enemies.get(target).mark >= 2 || s.enemies.get(target).vulnerable > 0);
            if (s.professionCharge >= 4 || overload >= 1 || banquetWindow || s.potions.size() >= 2
                    || statusDeckCards(s) >= 1 || chefEnemyPressure(s) >= 7 || s.hp < s.maxHp * 0.78f
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_BARD.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean finaleWindow = target >= 0 && (s.enemies.get(target).mark >= 2
                    || s.enemies.get(target).vulnerable > 0 || s.enemies.get(target).bind >= 2);
            if (s.professionCharge >= 4 || overload >= 1 || finaleWindow || tempOrEchoHandCards(s) >= 3
                    || s.cardsPlayedThisTurn >= 3 || bardEnemyPressure(s) >= 7
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_MIRRORIST.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean mirrorWindow = target >= 0 && (s.enemies.get(target).mark >= 2
                    || s.enemies.get(target).vulnerable > 0 || s.enemies.get(target).bind >= 2);
            int upgraded = upgradedDeckCards(s);
            if (s.professionCharge >= 4 || overload >= 1 || mirrorWindow || tempOrEchoHandCards(s) >= 3
                    || upgraded >= 7 || s.confluenceChain >= 4 || mirroristEnemyPressure(s) >= 7
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_PUPPETEER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean puppetWindow = target >= 0 && (s.enemies.get(target).bind >= 3
                    || s.enemies.get(target).mark >= 2 || s.enemies.get(target).vulnerable > 0);
            if (s.professionCharge >= 4 || overload >= 1 || puppetWindow || tempOrEchoHandCards(s) >= 3
                    || s.block >= 16 + s.act * 2 || puppeteerEnemyPressure(s) >= 7
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_SCAVENGER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean salvageWindow = target >= 0 && (s.enemies.get(target).mark >= 2
                    || s.enemies.get(target).vulnerable > 0 || s.enemies.get(target).bind >= 2);
            if (s.professionCharge >= 4 || overload >= 1 || salvageWindow || s.discard.size() >= 8
                    || statusDeckCards(s) >= 1 || s.gold >= 130 || scavengerEnemyPressure(s) >= 7
                    || s.hp < s.maxHp * 0.75f || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_LIGHTKEEPER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean lightWindow = target >= 0 && (s.enemies.get(target).mark >= 2
                    || s.enemies.get(target).vulnerable > 0 || s.enemies.get(target).bind >= 2);
            if (s.professionCharge >= 4 || overload >= 1 || lightWindow || tempOrEchoHandCards(s) >= 3
                    || statusDeckCards(s) >= 1 || s.exhaust.size() >= 5 || lightkeeperEnemyPressure(s) >= 7
                    || s.hp < s.maxHp * 0.75f || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_GEOMANCER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean faultWindow = target >= 0 && (s.enemies.get(target).bind + s.enemies.get(target).burn >= 4
                    || s.enemies.get(target).mark >= 2 || s.enemies.get(target).vulnerable > 0);
            if (s.professionCharge >= 4 || overload >= 1 || faultWindow || s.block >= 18
                    || s.confluenceChain >= 3 || upgradedDeckCards(s) >= 6 || geomancerEnemyPressure(s) >= 7
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_WITCH.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean brewWindow = target >= 0 && (s.enemies.get(target).burn + s.enemies.get(target).bind >= 4
                    || s.enemies.get(target).mark >= 2 || s.enemies.get(target).vulnerable > 0);
            if (s.professionCharge >= 4 || overload >= 1 || brewWindow || s.potions.size() >= 2
                    || statusDeckCards(s) >= 1 || tempOrEchoHandCards(s) >= 3 || witchEnemyPressure(s) >= 7
                    || s.hp < s.maxHp * 0.78f || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_SHIFTER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean blinkWindow = target >= 0 && (s.enemies.get(target).mark >= 2
                    || s.enemies.get(target).vulnerable > 0 || s.enemies.get(target).bind >= 2);
            if (s.professionCharge >= 4 || overload >= 1 || blinkWindow || tempOrEchoHandCards(s) >= 3
                    || s.cardsPlayedThisTurn >= 3 || s.discard.size() >= 8 || shifterEnemyPressure(s) >= 7
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_FATESEER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean fateWindow = target >= 0 && (s.enemies.get(target).mark >= 2
                    || s.enemies.get(target).vulnerable > 0 || s.enemies.get(target).bind >= 2);
            if (s.professionCharge >= 4 || overload >= 1 || fateWindow || upgradedDeckCards(s) >= 7
                    || s.confluenceChain >= 3 || fateseerEnemyPressure(s) >= 7
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_TIDECALLER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean tideWindow = target >= 0 && (s.enemies.get(target).bind >= 3
                    || s.enemies.get(target).vulnerable > 0 || s.enemies.get(target).mark >= 2);
            if (s.professionCharge >= 4 || overload >= 1 || tideWindow || s.block >= 18
                    || bindDeckCards(s) >= 3 || tidecallerEnemyPressure(s) >= 7
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_FROSTBINDER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean frostWindow = target >= 0 && (s.enemies.get(target).bind >= 3
                    || s.enemies.get(target).vulnerable > 0 || s.enemies.get(target).mark >= 2);
            if (s.professionCharge >= 4 || overload >= 1 || frostWindow || s.block >= 18
                    || statusDeckCards(s) >= 1 || s.exhaust.size() >= 5 || frostbinderEnemyPressure(s) >= 7
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_PLAGUEDOCTOR.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean plagueWindow = target >= 0 && (s.enemies.get(target).burn + s.enemies.get(target).bind >= 4
                    || s.enemies.get(target).vulnerable > 0 || s.enemies.get(target).mark >= 2);
            if (s.professionCharge >= 4 || overload >= 1 || plagueWindow || s.potions.size() >= 2
                    || statusDeckCards(s) >= 1 || plaguedoctorEnemyPressure(s) >= 7
                    || s.hp < s.maxHp * 0.75f || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_ARCHIVIST.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean archiveWindow = target >= 0 && (s.enemies.get(target).vulnerable > 0
                    || s.enemies.get(target).mark >= 2 || s.enemies.get(target).bind >= 2);
            if (s.professionCharge >= 4 || overload >= 1 || archiveWindow || upgradedDeckCards(s) >= 6
                    || statusDeckCards(s) >= 1 || s.discard.size() >= 6 || archivistEnemyPressure(s) >= 7
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_VOIDNAVIGATOR.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean jumpWindow = target >= 0 && (s.enemies.get(target).vulnerable > 0
                    || s.enemies.get(target).mark >= 2 || s.enemies.get(target).bind >= 2);
            if (s.professionCharge >= 4 || overload >= 1 || jumpWindow || tempOrEchoHandCards(s) >= 3
                    || s.exhaust.size() >= 5 || s.confluenceChain >= 3 || voidnavigatorEnemyPressure(s) >= 7
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_RELICSMITH.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean vaultWindow = target >= 0 && (s.enemies.get(target).vulnerable > 0
                    || s.enemies.get(target).mark >= 2 || s.enemies.get(target).bind >= 2);
            if (s.professionCharge >= 4 || overload >= 1 || vaultWindow || s.relics.size() >= 5
                    || s.gold >= 150 || s.block >= 18 || relicsmithEnemyPressure(s) >= 7
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_BEASTMASTER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean huntWindow = target >= 0 && (s.enemies.get(target).bind >= 3
                    || s.enemies.get(target).vulnerable > 0 || s.enemies.get(target).mark >= 2);
            if (s.professionCharge >= 4 || overload >= 1 || huntWindow || tempOrEchoHandCards(s) >= 3
                    || bindDeckCards(s) >= 3 || s.block >= 18 || beastmasterEnemyPressure(s) >= 7
                    || s.hp < s.maxHp * 0.75f || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_DRAGONBINDER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean oathWindow = target >= 0 && (s.enemies.get(target).burn >= 4
                    || s.enemies.get(target).vulnerable > 0 || s.enemies.get(target).mark >= 2);
            if (s.professionCharge >= 4 || overload >= 1 || oathWindow || tempOrEchoHandCards(s) >= 3
                    || statusDeckCards(s) >= 1 || s.block >= 18 || dragonbinderEnemyPressure(s) >= 7
                    || s.hp < s.maxHp * 0.75f || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_SOULBINDER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean soulWindow = target >= 0 && (s.enemies.get(target).bind >= 3
                    || s.enemies.get(target).vulnerable > 0 || s.enemies.get(target).mark >= 2);
            if (s.professionCharge >= 4 || overload >= 1 || soulWindow || tempOrEchoHandCards(s) >= 3
                    || s.exhaust.size() >= 4 || statusDeckCards(s) >= 1 || soulbinderEnemyPressure(s) >= 7
                    || s.hp < s.maxHp * 0.75f || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_STARFORGER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean starWindow = target >= 0 && (s.enemies.get(target).burn >= 3
                    || s.enemies.get(target).vulnerable > 0 || s.enemies.get(target).mark >= 2);
            if (s.professionCharge >= 4 || overload >= 1 || starWindow || upgradedDeckCards(s) >= 7
                    || statusDeckCards(s) >= 1 || s.block >= 18 || s.confluenceChain >= 3
                    || starforgerEnemyPressure(s) >= 7 || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_PATHFINDER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean routeWindow = target >= 0 && (s.enemies.get(target).mark >= 2
                    || s.enemies.get(target).bind >= 2 || s.enemies.get(target).vulnerable > 0);
            if (s.professionCharge >= 4 || overload >= 1 || routeWindow || upgradedDeckCards(s) >= 6
                    || s.confluenceChain >= 3 || s.block >= 18 || pathfinderEnemyPressure(s) >= 7
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_ARRAYIST.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean arrayWindow = target >= 0 && (s.enemies.get(target).mark >= 2
                    || s.enemies.get(target).bind >= 2 || s.enemies.get(target).vulnerable > 0);
            if (s.professionCharge >= 4 || overload >= 1 || arrayWindow || s.cardsPlayedThisTurn >= 4
                    || tempOrEchoHandCards(s) >= 3 || s.confluenceChain >= 3 || s.block >= 18
                    || arrayistEnemyPressure(s) >= 7 || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_GAMBITER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean mateWindow = target >= 0 && (s.enemies.get(target).mark >= 2
                    || s.enemies.get(target).bind >= 2 || s.enemies.get(target).vulnerable > 0);
            if (s.professionCharge >= 4 || overload >= 1 || mateWindow || s.cardsPlayedThisTurn >= 4
                    || s.block >= 18 || s.confluenceChain >= 3 || gambiterEnemyPressure(s) >= 7
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_GRAVEKEEPER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean requiemWindow = target >= 0 && (s.enemies.get(target).mark >= 2
                    || s.enemies.get(target).bind >= 2 || s.enemies.get(target).vulnerable > 0);
            if (s.professionCharge >= 4 || overload >= 1 || requiemWindow
                    || (statusDeckCards(s) + statusHandCards(s)) > 0 || s.exhaust.size() >= 4
                    || s.block >= 18 || gravekeeperEnemyPressure(s) >= 7
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_TREASURER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean balanceWindow = target >= 0 && (s.enemies.get(target).mark >= 2
                    || s.enemies.get(target).bind >= 2 || s.enemies.get(target).vulnerable > 0);
            if (s.professionCharge >= 4 || overload >= 1 || balanceWindow
                    || s.gold >= 120 || s.block >= 18 || upgradedDeckCards(s) >= 6
                    || treasurerEnemyPressure(s) >= 7 || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_DRIFTER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean junctionWindow = target >= 0 && (s.enemies.get(target).mark >= 2
                    || s.enemies.get(target).bind >= 2 || s.enemies.get(target).vulnerable > 0);
            if (s.professionCharge >= 4 || overload >= 1 || junctionWindow
                    || offPoolDeckCards(s) + offPoolHandCards(s) >= 5 || tempOrEchoHandCards(s) >= 3
                    || s.confluenceChain >= 3 || upgradedDeckCards(s) >= 6
                    || drifterCrossPressure(s) >= 7 || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_OATHKEEPER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean judgmentWindow = target >= 0 && (s.enemies.get(target).mark >= 2
                    || s.enemies.get(target).bind >= 2 || s.enemies.get(target).vulnerable > 0);
            if (s.professionCharge >= 4 || overload >= 1 || judgmentWindow
                    || s.block >= 18 || healingDeckCards(s) >= 4 || upgradedDeckCards(s) >= 6
                    || oathkeeperPressure(s) >= 7 || s.hp < s.maxHp * 0.55f
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_MOONSINGER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean eclipseWindow = target >= 0 && (s.enemies.get(target).mark >= 2
                    || s.enemies.get(target).bind >= 2 || s.enemies.get(target).vulnerable > 0);
            if (s.professionCharge >= 4 || overload >= 1 || eclipseWindow
                    || tempOrEchoHandCards(s) >= 3 || scryDeckCards(s) >= 4 || upgradedDeckCards(s) >= 6
                    || moonsingerPressure(s) >= 7 || s.cardsPlayedThisTurn >= 4
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_SPY.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean heistWindow = target >= 0 && (s.enemies.get(target).mark >= 2
                    || s.enemies.get(target).bind >= 2 || s.enemies.get(target).vulnerable > 0);
            if (s.professionCharge >= 4 || overload >= 1 || heistWindow
                    || s.gold >= 120 || tempOrEchoHandCards(s) >= 3 || upgradedDeckCards(s) >= 6
                    || spyPressure(s) >= 7 || s.cardsPlayedThisTurn >= 4
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_PERFUMER.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean bloomWindow = target >= 0 && (s.enemies.get(target).burn >= 3
                    || s.enemies.get(target).bind >= 2 || s.enemies.get(target).vulnerable > 0);
            if (s.professionCharge >= 4 || overload >= 1 || bloomWindow
                    || s.potions.size() > 0 || potionCards(s) >= 4 || healingDeckCards(s) >= 4
                    || perfumerPressure(s) >= 7 || s.hp < s.maxHp * 0.65f
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_CLOCKSMITH.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean timingWindow = target >= 0 && (s.enemies.get(target).mark >= 2
                    || s.enemies.get(target).bind >= 2 || s.enemies.get(target).vulnerable > 0);
            if (s.professionCharge >= 4 || overload >= 1 || timingWindow
                    || zeroCostDeckCards(s) >= 3 || tempOrEchoHandCards(s) >= 2 || upgradedDeckCards(s) >= 6
                    || clocksmithPressure(s) >= 7 || s.cardsPlayedThisTurn >= 4
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (GameCore.PROF_MINTSMITH.equals(s.profession)) {
            int target = firstEnemy(s);
            boolean burnWindow = target >= 0 && (s.enemies.get(target).burn >= 4
                    || s.enemies.get(target).bind >= 2 || s.enemies.get(target).vulnerable > 0);
            if (s.professionCharge >= 4 || overload >= 1 || burnWindow
                    || s.gold >= 120 || burnDeckCards(s) >= 4 || upgradedDeckCards(s) >= 6
                    || mintsmithPressure(s) >= 7 || s.block >= 18
                    || s.combatKind == 'E' || s.combatKind == 'B') {
                return true;
            }
        }
        if (overload >= 3) {
            return true;
        }
        return hasSkillRelic(s) || s.turn >= 2 || s.enemies.size() > 1 || s.combatKind == 'E' || s.combatKind == 'B';
    }

    private static boolean isSpecFriendlyTalent(String id) {
        return id != null && (id.endsWith("_grand") || id.startsWith("t_core_")
                || "t_tuner_resonance".equals(id) || "t_adjudicator_docket".equals(id)
                || "t_astrologer_ephemeris".equals(id) || "t_machinist_foundry".equals(id)
                || "t_chronomancer_clockwork".equals(id) || "t_pactmaker_notary".equals(id)
                || "t_stormcaller_pressure".equals(id) || "t_shadowdancer_vanish".equals(id)
                || "t_runeblade_stylus".equals(id) || "t_medium_oracle".equals(id)
                || "t_tactician_map".equals(id) || "t_prismist_lens".equals(id)
                || "t_dreamwalker_drift".equals(id) || "t_gardener_sprout".equals(id)
                || "t_chef_prep".equals(id) || "t_bard_chorus".equals(id)
                || "t_mirrorist_reflect".equals(id) || "t_puppeteer_rehearse".equals(id)
                || "t_scavenger_market".equals(id) || "t_plaguedoctor_lancet".equals(id)
                || "t_plaguedoctor_grand".equals(id) || "t_archivist_index".equals(id)
                || "t_archivist_grand".equals(id) || "t_voidnavigator_beacon".equals(id)
                || "t_voidnavigator_grand".equals(id) || "t_relicsmith_key".equals(id)
                || "t_relicsmith_grand".equals(id) || "t_beastmaster_claw".equals(id)
                || "t_beastmaster_grand".equals(id) || "t_dragonbinder_spark".equals(id)
                || "t_dragonbinder_grand".equals(id) || "t_soulbinder_thread".equals(id)
                || "t_soulbinder_grand".equals(id) || "t_starforger_spark".equals(id)
                || "t_starforger_grand".equals(id) || "t_pathfinder_mark".equals(id)
                || "t_pathfinder_grand".equals(id) || "t_arrayist_glyph".equals(id)
                || "t_arrayist_grand".equals(id) || "t_gambiter_pawn".equals(id)
                || "t_gambiter_grand".equals(id) || "t_gravekeeper_lantern".equals(id)
                || "t_gravekeeper_grand".equals(id) || "t_treasurer_entry".equals(id)
                || "t_treasurer_grand".equals(id) || "t_drifter_scout".equals(id)
                || "t_drifter_grand".equals(id) || "t_oathkeeper_vow".equals(id)
                || "t_oathkeeper_grand".equals(id) || "t_moonsinger_newmoon".equals(id)
                || "t_moonsinger_grand".equals(id) || "t_spy_contact".equals(id)
                || "t_spy_grand".equals(id) || "t_perfumer_note".equals(id)
                || "t_perfumer_grand".equals(id) || "t_clocksmith_tick".equals(id)
                || "t_clocksmith_grand".equals(id) || "t_mintsmith_spark".equals(id)
                || "t_mintsmith_grand".equals(id));
    }

    private static int buildCoreFocus(String id) {
        if ("t_core_overload".equals(id)) return BUILD_OVERLOAD;
        if ("t_core_echo".equals(id)) return BUILD_ECHO;
        if ("t_core_brew".equals(id)) return BUILD_BREW;
        if ("t_core_gold".equals(id)) return BUILD_GOLD;
        if ("t_core_blood".equals(id)) return BUILD_BLOOD;
        if ("t_core_forge".equals(id)) return BUILD_FORGE;
        if ("t_core_status".equals(id)) return BUILD_STATUS;
        if ("t_core_cycle".equals(id)) return BUILD_CYCLE;
        if ("t_core_guard".equals(id)) return BUILD_GUARD;
        return -1;
    }

    private static boolean hasBuildCoreTalent(GameCore.State s) {
        return activeBuildCoreFocus(s) >= 0;
    }

    private static int activeBuildCoreFocus(GameCore.State s) {
        if (s == null || s.talents == null) {
            return -1;
        }
        int bestFocus = -1;
        int bestScore = -9999;
        for (String id : s.talents) {
            int focus = buildCoreFocus(id);
            if (focus < 0) continue;
            int score = buildCoreDeckSignal(s, focus);
            if (score > bestScore) {
                bestScore = score;
                bestFocus = focus;
            }
        }
        return bestFocus;
    }

    private static int buildCoreDeckSignal(GameCore.State s, int focus) {
        int score = 0;
        if (s == null) return score;
        for (GameCore.Card c : s.deck) {
            GameCore.CardDef d = GameCore.card(c.id);
            score += buildCoreCardSignal(focus, d);
            if (c.upgraded && focus == BUILD_FORGE) score += 2;
        }
        if (focus == BUILD_GOLD) score += Math.min(12, s.gold / 25);
        if (focus == BUILD_BLOOD && s.hp < s.maxHp) score += 6;
        if (focus == BUILD_GUARD) score += Math.min(8, s.maxHp / 12);
        return score;
    }

    private static int buildCoreCardSignal(GameCore.State s, GameCore.CardDef d) {
        int focus = activeBuildCoreFocus(s);
        if (focus < 0) {
            return 0;
        }
        return buildCoreCardSignal(focus, d);
    }

    private static int buildCoreCardSignal(int focus, GameCore.CardDef d) {
        if (d == null) {
            return 0;
        }
        if (focus == BUILD_OVERLOAD) {
            return d.skillChargeGain * 4 + (d.energyGain > 0 ? 3 : 0) + (d.draw > 0 ? 2 : 0)
                    + (isAny(d.id, "overload_conduit", "tuner_overclock", "adjudicator_overrule",
                    "astrologer_overstar", "machinist_overdrive", "chronomancer_overloop",
                    "pactmaker_overdeal", "stormcaller_overstorm", "shadowdancer_overstrike",
                    "runeblade_overglyph", "medium_overtrance", "tactician_overplan",
                    "prismist_overbeam", "dreamwalker_overdream", "gardener_overgrowth",
                    "chef_overcook", "bard_overcrescendo", "mirrorist_overimage",
                    "puppeteer_overpull", "scavenger_overhaul", "geomancer_overquake", "witch_overbrew",
                    "shifter_overblink", "fateseer_overfate", "tidecaller_overtide", "frostbinder_overfreeze",
                    "plaguedoctor_overdose", "plaguedoctor_grand_plague",
                    "archivist_overfile", "archivist_grand_archive", "fusion_spark",
                    "prism_guard_matrix", "apex_resonance") ? 12 : 0)
                    + (isAny(d.id, "hybrid_guard_conduit", "hybrid_bloodcharge", "hybrid_rift_engine",
                    "confluence_chord", "prism_anchor", "apex_confluence") ? 8 : 0);
        }
        if (focus == BUILD_ECHO) {
            return (d.createEcho ? 9 : 0) + (d.exhaust ? 5 : 0) + (d.exhaustTopDiscard ? 6 : 0)
                    + (d.exhaustForDamage ? 6 : 0)
                    + (isAny(d.id, "echo_matrix", "hybrid_echo_step", "hybrid_echo_vial",
                    "hybrid_spirit_anvil", "tuner_loop", "astrologer_orbit", "machinist_cogcall",
                    "chronomancer_loop", "medium_whisper", "medium_veil", "medium_oracle",
                    "medium_overtrance", "medium_grand_seance", "shadowdancer_veil", "bard_chorus",
                    "mirrorist_reflect", "puppeteer_rehearse", "echo_forge_loop",
                    "shifter_slip", "shifter_screen", "shifter_anchor", "shifter_grand_paradox",
                    "fateseer_omen", "fateseer_grand_design", "tidecaller_ripple", "tidecaller_current",
                    "frostbinder_shard", "frostbinder_rime", "frostbinder_grand_winter",
                    "plaguedoctor_culture", "plaguedoctor_grand_plague",
                    "archivist_index", "archivist_catalog", "archivist_grand_archive",
                    "apex_resonance") ? 10 : 0);
        }
        if (focus == BUILD_BREW) {
            return (d.createPotion ? 10 : 0) + d.burn * 2 + d.bind * 2 + (d.spreadStatus ? 6 : 0)
                    + (d.gainBurnPower + d.gainBindPower) * 3
                    + (isAny(d.id, "brew_crucible", "hybrid_plague_brew", "hybrid_echo_vial",
                    "chef_prep", "chef_spice", "chef_sizzle", "chef_overcook", "chef_grand_banquet",
                    "alchemist_sunsteel", "witch_brew", "witch_charm", "witch_overbrew",
                    "witch_grand_cauldron", "plaguedoctor_lancet", "plaguedoctor_culture",
                    "plaguedoctor_overdose", "plaguedoctor_grand_plague", "fusion_spark",
                    "bloodcoin_catalyst") ? 10 : 0);
        }
        if (focus == BUILD_GOLD) {
            return d.goldGain / 2 + (d.goldDamage ? 9 : 0) + (d.goldBlock ? 9 : 0)
                    + (isAny(d.id, "golden_engine", "hybrid_blood_tithe", "hybrid_coinwall",
                    "pactmaker_collection", "pactmaker_bloodnote", "pactmaker_overdeal",
                    "pactmaker_grand_contract", "merchant_kingmaker", "cursed_coin",
                    "void_tithe", "scavenger_pick", "scavenger_sort", "bloodcoin_catalyst",
                    "apex_resonance") ? 10 : 0);
        }
        if (focus == BUILD_BLOOD) {
            return d.hpLoss * 4 + d.heal * 2 + (d.createWound ? 9 : 0) + ("wound".equals(d.id) ? 5 : 0)
                    + (isAny(d.id, "crimson_loop", "hybrid_blood_tithe", "hybrid_bloodcharge",
                    "pactmaker_bloodnote", "blood_apotheosis", "gardener_sprout", "chef_stew", "witch_ward",
                    "scavenger_patch", "bloodcoin_catalyst", "apex_resonance") ? 10 : 0);
        }
        if (focus == BUILD_FORGE) {
            return (d.upgradeRandom ? 10 : 0) + d.scry * 2 + d.upgradeCostDrop * 3 + (d.rarity == 2 ? 2 : 0)
                    + (isAny(d.id, "forge_blueprint", "hybrid_forgebrand", "hybrid_rift_engine",
                    "hybrid_spirit_anvil", "prism_anchor", "machinist_blueprint", "machinist_grand_engine",
                    "runeblade_inscribe", "tactician_map", "tactician_grand_strategy",
                    "mirrorist_shard", "mirrorist_reflect", "geomancer_rune", "geomancer_geode",
                    "geomancer_grand_fault", "witch_charm", "echo_forge_loop",
                    "fateseer_omen", "fateseer_veil", "fateseer_wheel", "fateseer_overfate",
                    "fateseer_grand_design",
                    "archivist_index", "archivist_seal", "archivist_catalog",
                    "archivist_overfile", "archivist_grand_archive",
                    "prism_guard_matrix", "apex_resonance") ? 10 : 0);
        }
        if (focus == BUILD_STATUS) {
            return d.burn * 2 + d.bind * 2 + d.vulnerable * 5 + (d.addStatusToEnemy ? 7 : 0)
                    + (d.spreadStatus ? 8 : 0) + (d.createWound ? 4 : 0)
                    + (isAny(d.id, "plague_vector", "hybrid_plague_brew", "hybrid_hexdance",
                    "hybrid_forgebrand", "tuner_harmonic", "adjudicator_clause", "pactmaker_witness",
                    "stormcaller_chain", "shadowdancer_mark", "runeblade_glyphcut", "medium_binding",
                    "tactician_flank", "prismist_spill", "dreamwalker_bind", "gardener_compost",
                    "chef_spice", "bard_discord", "puppeteer_needle", "geomancer_quake",
                    "geomancer_overquake", "geomancer_grand_fault", "witch_brew", "witch_curse",
                    "witch_overbrew", "witch_grand_cauldron", "shifter_lance", "shifter_overblink",
                    "shifter_grand_paradox", "fateseer_thread", "fateseer_overfate",
                    "fateseer_grand_design", "tidecaller_ripple", "tidecaller_surge",
                    "tidecaller_overtide", "tidecaller_grand_tide", "frostbinder_shard",
                    "frostbinder_shatter", "frostbinder_overfreeze", "frostbinder_grand_winter",
                    "plaguedoctor_lancet", "plaguedoctor_quarantine", "plaguedoctor_overdose",
                    "plaguedoctor_grand_plague", "archivist_index", "archivist_redline",
                    "archivist_overfile", "archivist_grand_archive", "fusion_spark",
                    "bloodcoin_catalyst", "apex_resonance") ? 10 : 0);
        }
        if (focus == BUILD_CYCLE) {
            return d.draw * 6 + d.energyGain * 8 + (d.cost == 0 ? 5 : 0) + d.comboDamage / 2
                    + (isAny(d.id, "cycle_metronome", "hybrid_echo_step", "hybrid_hexdance",
                    "hybrid_rift_engine", "confluence_chord", "tuner_note", "tuner_pulse",
                    "tuner_loop", "adjudicator_writ", "astrologer_chart", "astrologer_ephemeris",
                    "machinist_spanner", "machinist_cogcall", "chronomancer_tick", "chronomancer_loop",
                    "shadowdancer_step", "bard_note", "bard_chorus", "mirrorist_shard",
                    "puppeteer_thread", "scavenger_sort", "geomancer_rune", "geomancer_geode",
                    "geomancer_overquake", "witch_brew", "witch_charm", "witch_overbrew",
                    "shifter_slip", "shifter_screen", "shifter_anchor", "shifter_overblink",
                    "fateseer_omen", "fateseer_wheel", "fateseer_overfate",
                    "tidecaller_ripple", "tidecaller_current", "tidecaller_overtide",
                    "frostbinder_shard", "frostbinder_rime", "frostbinder_overfreeze",
                    "plaguedoctor_lancet", "plaguedoctor_culture", "plaguedoctor_overdose",
                    "plaguedoctor_grand_plague", "archivist_index", "archivist_catalog",
                    "archivist_overfile", "archivist_grand_archive", "fusion_spark", "echo_forge_loop",
                    "apex_resonance") ? 10 : 0);
        }
        if (focus == BUILD_GUARD) {
            return d.block * 2 + (d.type == 1 ? 3 : 0) + (d.blockToDamage ? 8 : 0) + (d.retainBlock ? 6 : 0)
                    + d.gainSteelEngine * 5 + (d.burnToBlock ? 4 : 0)
                    + (isAny(d.id, "aegis_engine", "hybrid_guard_conduit", "hybrid_coinwall",
                    "hybrid_spirit_anvil", "prism_anchor", "adjudicator_bond", "machinist_spanner",
                    "machinist_overdrive", "chronomancer_anchor", "pactmaker_collection",
                    "warden_aegisline", "tactician_bulwark", "gardener_rootwall", "chef_stew",
                    "bard_ballad", "mirrorist_guard", "puppeteer_screen", "scavenger_patch",
                    "geomancer_mantle", "geomancer_geode", "geomancer_grand_fault",
                    "witch_ward", "witch_charm", "witch_grand_cauldron",
                    "shifter_screen", "shifter_anchor", "shifter_grand_paradox",
                    "fateseer_veil", "fateseer_wheel", "fateseer_overfate", "fateseer_grand_design",
                    "tidecaller_breaker", "tidecaller_current", "tidecaller_overtide", "tidecaller_grand_tide",
                    "frostbinder_ward", "frostbinder_rime", "frostbinder_overfreeze", "frostbinder_grand_winter",
                    "plaguedoctor_mask", "plaguedoctor_culture", "plaguedoctor_overdose",
                    "plaguedoctor_grand_plague", "archivist_seal", "archivist_catalog",
                    "archivist_overfile", "archivist_grand_archive",
                    "echo_forge_loop", "prism_guard_matrix", "apex_resonance") ? 10 : 0);
        }
        return 0;
    }

    private static int buildCoreRelicSignal(int focus, String id) {
        if (focus < 0 || id == null) {
            return 0;
        }
        if (focus == BUILD_OVERLOAD) {
            return isAny(id, "sapphire_cell", "amber_quill", "tempo_metronome", "stormglass_seal",
                    "mastery_badge", "resonance_lens", "bulwark_core", "tuning_fork", "conductor_baton", "overload_etch",
                    "pressure_gauge", "storm_rod", "tempest_crown", "ability_crown", "contract_stamp",
                    "grand_ledger", "confluence_map", "prism_gear", "starforge_lens",
                    "resonance_prism", "faultline_core", "tectonic_crown",
                    "witch_bottle", "witch_moon_crown", "phase_lens", "phase_crown",
                    "fate_lantern", "fate_crown", "tide_shell", "tide_crown",
                    "frost_chain", "frost_crown", "plague_case", "plague_crown",
                    "archive_key", "archive_crown") ? 3 : 0;
        }
        if (focus == BUILD_ECHO) {
            return isAny(id, "void_lens", "arcane_ink", "void_abacus", "echo_prism", "singularity_orb",
                    "echo_ledger", "void_anchor", "echoflow_charm", "echo_crown", "spirit_planchette",
                    "ancestral_planchette", "dreamcatcher_charm", "oneiric_crown", "songbook",
                    "finale_crown", "mirror_lens", "mirror_crown", "string_spool", "marionette_crown",
                    "resonance_prism", "witch_bottle", "witch_moon_crown", "phase_lens", "phase_crown",
                    "tide_shell", "tide_crown", "frost_chain", "frost_crown", "plague_case", "plague_crown",
                    "archive_key", "archive_crown") ? 3 : 0;
        }
        if (focus == BUILD_BREW) {
            return isAny(id, "ember_core", "charcoal_sigil", "cinder_spoon", "green_bell",
                    "alchemist_case", "glass_vials", "catalyst_pump", "solar_crucible",
                    "emberroot_charm", "split_anvil", "bloodspark_contract", "recipe_book",
                    "banquet_crown", "seed_satchel", "verdant_crown", "resonance_prism",
                    "witch_bottle", "witch_moon_crown", "phase_crown", "plague_case", "plague_crown") ? 3 : 0;
        }
        if (focus == BUILD_GOLD) {
            return isAny(id, "hunter_mark", "empty_coin", "merchant_key", "merchant_scale", "tithe_box",
                    "ledger_stamp", "kingmaker_seal", "bloodcoin_broach", "contract_stamp",
                    "grand_ledger", "golden_throne", "runic_shackle", "salvage_hook",
                    "scrap_magnet", "scrap_king_crown", "resonance_prism") ? 3 : 0;
        }
        if (focus == BUILD_BLOOD) {
            return isAny(id, "silver_suture", "cup_of_mist", "scar_talisman", "bloodcoin_broach",
                    "bloodspark_contract", "crimson_seal", "blood_crown", "blood_contract",
                    "contract_stamp", "grand_ledger", "hex_moon", "vital_sprout",
                    "resonance_prism", "witch_moon_crown", "phase_crown", "plague_crown") ? 3 : 0;
        }
        if (focus == BUILD_FORGE) {
            return isAny(id, "glass_anvil", "polished_cog", "loom_shuttle", "mirror_anvil",
                    "split_anvil", "pattern_spool", "engraver_stylus", "gyro_wrench", "clockwork_core",
                    "assembly_frame", "clockwork_loom", "living_codex", "forge_heart",
                    "confluence_map", "prism_gear", "mosaic_core", "starforge_lens", "resonance_prism",
                    "war_table", "grand_war_room", "refraction_dial", "spectrum_crown",
                    "faultline_core", "tectonic_crown", "witch_moon_crown", "phase_lens", "phase_crown",
                    "fate_lantern", "fate_crown", "plague_crown", "archive_key", "archive_crown") ? 3 : 0;
        }
        if (focus == BUILD_STATUS) {
            return isAny(id, "thorn_ring", "charcoal_sigil", "root_drum", "cinder_spoon", "green_bell",
                    "ranger_map", "glass_vials", "emberroot_charm", "stormglass_seal", "curse_censer",
                    "split_anvil", "bloodspark_contract", "hawk_fletching", "solar_crucible",
                    "apex_compass", "fallen_crown", "hex_moon", "markchain_seal", "pressure_gauge",
                    "storm_rod", "tempest_crown", "war_table", "grand_war_room",
                    "recipe_book", "banquet_crown", "songbook", "finale_crown",
                    "resonance_prism", "faultline_core", "tectonic_crown",
                    "witch_bottle", "witch_moon_crown", "phase_lens", "phase_crown",
                    "fate_lantern", "fate_crown", "tide_shell", "tide_crown",
                    "frost_chain", "frost_crown", "plague_case", "plague_crown",
                    "archive_key", "archive_crown") ? 3 : 0;
        }
        if (focus == BUILD_CYCLE) {
            return isAny(id, "void_lens", "amber_quill", "ink_fountain", "root_drum",
                    "cracked_compass", "moon_lantern", "tempo_metronome", "tempo_spindle",
                    "flash_heel", "finale_rapier", "tuning_fork", "conductor_baton",
                    "hourglass_charm", "time_engine", "echo_ledger", "confluence_map",
                    "prism_gear", "mosaic_core", "starforge_lens", "resonance_prism",
                    "songbook", "finale_crown", "faultline_core", "tectonic_crown",
                    "witch_bottle", "witch_moon_crown", "phase_lens", "phase_crown",
                    "fate_lantern", "fate_crown", "tide_shell", "tide_crown",
                    "frost_chain", "frost_crown", "plague_case", "plague_crown",
                    "archive_key", "archive_crown") ? 3 : 0;
        }
        if (focus == BUILD_GUARD) {
            return isAny(id, "steel_oath", "bone_mask", "thorn_ring", "opal_scar", "warden_plate",
                    "vital_sprout", "polished_cog", "stormglass_seal", "bloodcoin_broach",
                    "mirror_anvil", "command_banner", "aegis_throne", "vigil_bloom", "bulwark_core",
                    "forge_heart", "discipline_chart", "trial_ledger", "war_table",
                    "grand_war_room", "seed_satchel", "verdant_crown", "string_spool",
                    "marionette_crown", "confluence_map", "prism_gear", "mosaic_core",
                    "starforge_lens", "resonance_prism", "faultline_core", "tectonic_crown",
                    "witch_bottle", "witch_moon_crown", "phase_lens", "phase_crown",
                    "fate_lantern", "fate_crown", "tide_shell", "tide_crown",
                    "frost_chain", "frost_crown", "plague_case", "plague_crown",
                    "archive_key", "archive_crown") ? 3 : 0;
        }
        return 0;
    }

    private static boolean isAny(String id, String... values) {
        if (id == null) {
            return false;
        }
        for (String value : values) {
            if (id.equals(value)) {
                return true;
            }
        }
        return false;
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
                || s.relics.contains("mosaic_core") || s.relics.contains("starforge_lens") || s.relics.contains("resonance_prism")
                || s.relics.contains("tuning_fork") || s.relics.contains("conductor_baton")
                || s.relics.contains("verdict_seal") || s.relics.contains("judgment_codex")
                || s.relics.contains("star_compass") || s.relics.contains("celestial_orrery")
                || s.relics.contains("gyro_wrench") || s.relics.contains("clockwork_core")
                || s.relics.contains("hourglass_charm") || s.relics.contains("time_engine")
                || s.relics.contains("contract_stamp") || s.relics.contains("grand_ledger")
                || s.relics.contains("storm_rod") || s.relics.contains("tempest_crown")
                || s.relics.contains("shadow_sash") || s.relics.contains("eclipse_mask")
                || s.relics.contains("rune_stylus") || s.relics.contains("grand_rune_blade")
                || s.relics.contains("spirit_planchette") || s.relics.contains("ancestral_planchette")
                || s.relics.contains("war_table") || s.relics.contains("grand_war_room")
                || s.relics.contains("refraction_dial") || s.relics.contains("spectrum_crown")
                || s.relics.contains("dreamcatcher_charm") || s.relics.contains("oneiric_crown")
                || s.relics.contains("seed_satchel") || s.relics.contains("verdant_crown")
                || s.relics.contains("recipe_book") || s.relics.contains("banquet_crown")
                || s.relics.contains("songbook") || s.relics.contains("finale_crown")
                || s.relics.contains("mirror_lens") || s.relics.contains("mirror_crown")
                || s.relics.contains("string_spool") || s.relics.contains("marionette_crown")
                || s.relics.contains("scrap_magnet") || s.relics.contains("scrap_king_crown")
                || s.relics.contains("faultline_core") || s.relics.contains("tectonic_crown")
                || s.relics.contains("witch_bottle") || s.relics.contains("witch_moon_crown")
                || s.relics.contains("phase_lens") || s.relics.contains("phase_crown")
                || s.relics.contains("fate_lantern") || s.relics.contains("fate_crown")
                || s.relics.contains("tide_shell") || s.relics.contains("tide_crown")
                || s.relics.contains("frost_chain") || s.relics.contains("frost_crown")
                || s.relics.contains("plague_case") || s.relics.contains("plague_crown")
                || s.relics.contains("archive_key") || s.relics.contains("archive_crown")
                || s.relics.contains("void_compass") || s.relics.contains("void_crown")
                || s.relics.contains("relic_chisel") || s.relics.contains("vault_crown")
                || s.relics.contains("beast_whistle") || s.relics.contains("alpha_crown")
                || s.relics.contains("dragon_sigil") || s.relics.contains("elder_dragon_crown")
                || s.relics.contains("soul_lantern") || s.relics.contains("soul_crown")
                || s.relics.contains("star_hammer") || s.relics.contains("star_crown")
                || s.relics.contains("pathfinder_compass") || s.relics.contains("route_crown")
                || s.relics.contains("array_disc") || s.relics.contains("array_crown")
                || s.relics.contains("gambit_clock") || s.relics.contains("checkmate_crown")
                || s.relics.contains("grave_lantern") || s.relics.contains("requiem_crown")
                || s.relics.contains("treasury_key") || s.relics.contains("audit_crown")
                || s.relics.contains("rift_pass") || s.relics.contains("junction_crown")
                || s.relics.contains("oath_seal") || s.relics.contains("judgment_crown")
                || s.relics.contains("moon_lyre") || s.relics.contains("eclipse_crown")
                || s.relics.contains("cipher_ring") || s.relics.contains("mastermind_crown")
                || s.relics.contains("scent_vial") || s.relics.contains("bouquet_crown")
                || s.relics.contains("clockwork_key") || s.relics.contains("chrono_crown")
                || s.relics.contains("mint_tongs") || s.relics.contains("mint_crown")
                || s.relics.contains("bulwark_core") || s.relics.contains("salvage_hook")
                || s.relics.contains("hybrid_keystone") || s.relics.contains("cascade_lattice");
    }

    private static boolean isStormcallerSignal(GameCore.CardDef d) {
        return d != null && (d.burn > 0 || d.vulnerable > 0 || d.aoe || d.draw > 0
                || d.energyGain > 0 || d.skillChargeGain > 0 || d.cost == 0
                || GameCore.PROF_STORMCALLER.equals(d.profession));
    }

    private static boolean isStormcallerCard(GameCore.CardDef d) {
        return d != null && ("stormcaller_sparkline".equals(d.id) || "stormcaller_barrier".equals(d.id)
                || "stormcaller_gust".equals(d.id) || "stormcaller_chain".equals(d.id)
                || "stormcaller_overstorm".equals(d.id) || "stormcaller_tempest_crown".equals(d.id));
    }

    private static boolean isShadowdancerSignal(GameCore.CardDef d) {
        return d != null && (d.cost == 0 || d.draw > 0 || d.energyGain > 0 || d.createEcho
                || d.exhaust || d.skillChargeGain > 0 || d.comboDamage > 0 || d.vulnerable > 0
                || GameCore.PROF_SHADOWDANCER.equals(d.profession));
    }

    private static boolean isShadowdancerCard(GameCore.CardDef d) {
        return d != null && ("shadowdancer_blade".equals(d.id) || "shadowdancer_veil".equals(d.id)
                || "shadowdancer_step".equals(d.id) || "shadowdancer_mark".equals(d.id)
                || "shadowdancer_overstrike".equals(d.id) || "shadowdancer_eclipse".equals(d.id));
    }

    private static boolean isRunebladeSignal(GameCore.CardDef d) {
        return d != null && (d.upgradeRandom || d.scry > 0 || d.skillChargeGain > 0
                || d.vulnerable > 0 || d.block > 0 || (d.damage > 0 && d.block > 0)
                || d.draw > 0 || GameCore.PROF_RUNEBLADE.equals(d.profession));
    }

    private static boolean isRunebladeCard(GameCore.CardDef d) {
        return d != null && ("runeblade_glyphcut".equals(d.id) || "runeblade_ward".equals(d.id)
                || "runeblade_inscribe".equals(d.id) || "runeblade_cleave".equals(d.id)
                || "runeblade_overglyph".equals(d.id) || "runeblade_grand_seal".equals(d.id));
    }

    private static boolean isMediumSignal(GameCore.CardDef d) {
        return d != null && (d.createEcho || d.exhaust || d.scry > 0 || d.draw > 0
                || d.bind > 0 || d.skillChargeGain > 0 || d.cost == 0
                || GameCore.PROF_MEDIUM.equals(d.profession));
    }

    private static boolean isMediumCard(GameCore.CardDef d) {
        return d != null && ("medium_whisper".equals(d.id) || "medium_veil".equals(d.id)
                || "medium_oracle".equals(d.id) || "medium_binding".equals(d.id)
                || "medium_overtrance".equals(d.id) || "medium_grand_seance".equals(d.id));
    }

    private static boolean isTacticianSignal(GameCore.CardDef d) {
        return d != null && (d.block > 0 || d.scry > 0 || d.upgradeRandom
                || d.skillChargeGain > 0 || d.vulnerable > 0 || d.draw > 0
                || isHybridCore(d) || isConfluenceCore(d)
                || GameCore.PROF_TACTICIAN.equals(d.profession));
    }

    private static boolean isTacticianCard(GameCore.CardDef d) {
        return d != null && ("tactician_probe".equals(d.id) || "tactician_bulwark".equals(d.id)
                || "tactician_map".equals(d.id) || "tactician_flank".equals(d.id)
                || "tactician_overplan".equals(d.id) || "tactician_grand_strategy".equals(d.id));
    }

    private static boolean isPrismistSignal(GameCore.CardDef d) {
        return d != null && (isHybridCore(d) || isConfluenceCore(d) || d.scry > 0 || d.upgradeRandom
                || d.skillChargeGain > 0 || d.draw > 0 || d.vulnerable > 0 || d.bind > 0 || d.burn > 0
                || GameCore.PROF_PRISMIST.equals(d.profession));
    }

    private static boolean isPrismistCard(GameCore.CardDef d) {
        return d != null && ("prismist_ray".equals(d.id) || "prismist_lens".equals(d.id)
                || "prismist_anchor".equals(d.id) || "prismist_spill".equals(d.id)
                || "prismist_overbeam".equals(d.id) || "prismist_grand_spectrum".equals(d.id));
    }

    private static boolean isDreamwalkerSignal(GameCore.CardDef d) {
        return d != null && (d.scry > 0 || d.exhaust || d.createEcho || d.draw > 0
                || d.skillChargeGain > 0 || d.bind > 0 || d.vulnerable > 0
                || "wound".equals(d.id) || "daze".equals(d.id)
                || GameCore.PROF_DREAMWALKER.equals(d.profession));
    }

    private static boolean isDreamwalkerCard(GameCore.CardDef d) {
        return d != null && ("dreamwalker_drift".equals(d.id) || "dreamwalker_veil".equals(d.id)
                || "dreamwalker_lucid".equals(d.id) || "dreamwalker_bind".equals(d.id)
                || "dreamwalker_overdream".equals(d.id) || "dreamwalker_grand_dream".equals(d.id));
    }

    private static boolean isGardenerSignal(GameCore.CardDef d) {
        return d != null && (d.heal > 0 || d.gainWildEngine > 0 || d.bind > 0 || d.block > 0
                || d.draw > 0 || d.skillChargeGain > 0 || d.upgradeRandom || d.createWound
                || d.vulnerable > 0 || "wound".equals(d.id) || "daze".equals(d.id)
                || GameCore.PROF_GARDENER.equals(d.profession));
    }

    private static boolean isGardenerCard(GameCore.CardDef d) {
        return d != null && ("gardener_sprout".equals(d.id) || "gardener_rootwall".equals(d.id)
                || "gardener_compost".equals(d.id) || "gardener_thornbloom".equals(d.id)
                || "gardener_overgrowth".equals(d.id) || "gardener_grand_grove".equals(d.id));
    }

    private static boolean isChefSignal(GameCore.CardDef d) {
        return d != null && (d.heal > 0 || d.createPotion || d.burn > 0 || d.bind > 0
                || d.draw > 0 || d.skillChargeGain > 0 || d.createEcho || d.vulnerable > 0
                || "wound".equals(d.id) || "daze".equals(d.id)
                || GameCore.PROF_CHEF.equals(d.profession));
    }

    private static boolean isChefCard(GameCore.CardDef d) {
        return d != null && ("chef_prep".equals(d.id) || "chef_stew".equals(d.id)
                || "chef_spice".equals(d.id) || "chef_sizzle".equals(d.id)
                || "chef_overcook".equals(d.id) || "chef_grand_banquet".equals(d.id));
    }

    private static boolean isBardSignal(GameCore.CardDef d) {
        return d != null && (d.cost == 0 || d.draw > 0 || d.energyGain > 0 || d.createEcho
                || d.skillChargeGain > 0 || d.vulnerable > 0 || d.bind > 0 || d.comboDamage > 0
                || GameCore.PROF_BARD.equals(d.profession));
    }

    private static boolean isBardCard(GameCore.CardDef d) {
        return d != null && ("bard_note".equals(d.id) || "bard_ballad".equals(d.id)
                || "bard_chorus".equals(d.id) || "bard_discord".equals(d.id)
                || "bard_overcrescendo".equals(d.id) || "bard_grand_finale".equals(d.id));
    }

    private static boolean isMirroristSignal(GameCore.CardDef d) {
        return d != null && (d.scry > 0 || d.upgradeRandom || d.createEcho || d.draw > 0
                || d.skillChargeGain > 0 || d.vulnerable > 0 || isHybridCore(d) || isConfluenceCore(d)
                || GameCore.PROF_MIRRORIST.equals(d.profession));
    }

    private static boolean isMirroristCard(GameCore.CardDef d) {
        return d != null && ("mirrorist_shard".equals(d.id) || "mirrorist_guard".equals(d.id)
                || "mirrorist_reflect".equals(d.id) || "mirrorist_prismcut".equals(d.id)
                || "mirrorist_overimage".equals(d.id) || "mirrorist_grand_mirror".equals(d.id));
    }

    private static boolean isPuppeteerSignal(GameCore.CardDef d) {
        return d != null && (d.bind > 0 || d.createEcho || d.draw > 0 || d.block > 0
                || d.skillChargeGain > 0 || d.type == 1 || d.vulnerable > 0
                || GameCore.PROF_PUPPETEER.equals(d.profession));
    }

    private static boolean isPuppeteerCard(GameCore.CardDef d) {
        return d != null && ("puppeteer_thread".equals(d.id) || "puppeteer_screen".equals(d.id)
                || "puppeteer_rehearse".equals(d.id) || "puppeteer_needle".equals(d.id)
                || "puppeteer_overpull".equals(d.id) || "puppeteer_grand_stage".equals(d.id));
    }

    private static boolean isSalvageSignal(GameCore.CardDef d) {
        return d != null && (d.draw > 0 || d.goldGain > 0 || d.exhaust || d.exhaustTopDiscard
                || d.createWound || d.heal > 0 || d.block > 0 || d.skillChargeGain > 0
                || d.energyGain > 0 || d.cost == 0 || "wound".equals(d.id) || "daze".equals(d.id)
                || isScavengerCard(d) || "cursed_coin".equals(d.id) || "void_tithe".equals(d.id)
                || "void_draw".equals(d.id) || "void_hunger".equals(d.id) || "void_glimpse".equals(d.id)
                || "cycle_metronome".equals(d.id) || "golden_engine".equals(d.id)
                || "hybrid_blood_tithe".equals(d.id) || "hybrid_coinwall".equals(d.id));
    }

    private static boolean isScavengerSignal(GameCore.CardDef d) {
        return d != null && (d.draw > 0 || d.goldGain > 0 || d.exhaust || d.exhaustTopDiscard
                || d.createWound || d.heal > 0 || d.block > 0 || d.skillChargeGain > 0
                || d.vulnerable > 0 || d.bind > 0 || "wound".equals(d.id) || "daze".equals(d.id)
                || GameCore.PROF_SCAVENGER.equals(d.profession));
    }

    private static boolean isScavengerCard(GameCore.CardDef d) {
        return d != null && ("scavenger_pick".equals(d.id) || "scavenger_sort".equals(d.id)
                || "scavenger_patch".equals(d.id) || "scavenger_magnet".equals(d.id)
                || "scavenger_overhaul".equals(d.id) || "scavenger_grand_foundry".equals(d.id));
    }

    private static boolean isLightkeeperSignal(GameCore.CardDef d) {
        return d != null && (d.exhaust || d.createEcho || d.scry > 0 || d.upgradeRandom
                || d.draw > 0 || d.block > 0 || d.skillChargeGain > 0 || d.vulnerable > 0
                || d.bind > 0 || d.cost == 0 || "wound".equals(d.id) || "daze".equals(d.id)
                || GameCore.PROF_LIGHTKEEPER.equals(d.profession));
    }

    private static boolean isLightkeeperCard(GameCore.CardDef d) {
        return d != null && ("lightkeeper_glimmer".equals(d.id) || "lightkeeper_vigil".equals(d.id)
                || "lightkeeper_prism".equals(d.id) || "lightkeeper_brand".equals(d.id)
                || "lightkeeper_overflare".equals(d.id) || "lightkeeper_grand_beacon".equals(d.id));
    }

    private static boolean isGeomancerSignal(GameCore.CardDef d) {
        return d != null && (d.block > 0 || d.burn > 0 || d.bind > 0 || d.vulnerable > 0
                || d.upgradeRandom || d.scry > 0 || d.skillChargeGain > 0
                || isHybridCore(d) || isConfluenceCore(d) || GameCore.PROF_GEOMANCER.equals(d.profession));
    }

    private static boolean isGeomancerCard(GameCore.CardDef d) {
        return d != null && ("geomancer_rune".equals(d.id) || "geomancer_mantle".equals(d.id)
                || "geomancer_quake".equals(d.id) || "geomancer_geode".equals(d.id)
                || "geomancer_overquake".equals(d.id) || "geomancer_grand_fault".equals(d.id));
    }

    private static boolean isWitchSignal(GameCore.CardDef d) {
        return d != null && (d.createPotion || d.burn > 0 || d.bind > 0 || d.vulnerable > 0
                || d.createEcho || d.exhaust || d.draw > 0 || d.heal > 0 || d.block > 0
                || d.skillChargeGain > 0 || "wound".equals(d.id) || "daze".equals(d.id)
                || GameCore.PROF_WITCH.equals(d.profession));
    }

    private static boolean isWitchCard(GameCore.CardDef d) {
        return d != null && ("witch_brew".equals(d.id) || "witch_ward".equals(d.id)
                || "witch_curse".equals(d.id) || "witch_charm".equals(d.id)
                || "witch_overbrew".equals(d.id) || "witch_grand_cauldron".equals(d.id));
    }

    private static boolean isShifterSignal(GameCore.CardDef d) {
        return d != null && (d.cost == 0 || d.draw > 0 || d.energyGain > 0 || d.createEcho
                || d.exhaust || d.exhaustTopDiscard || d.skillChargeGain > 0 || d.vulnerable > 0
                || d.bind > 0 || d.block > 0 || GameCore.PROF_SHIFTER.equals(d.profession));
    }

    private static boolean isShifterCard(GameCore.CardDef d) {
        return d != null && ("shifter_slip".equals(d.id) || "shifter_screen".equals(d.id)
                || "shifter_anchor".equals(d.id) || "shifter_lance".equals(d.id)
                || "shifter_overblink".equals(d.id) || "shifter_grand_paradox".equals(d.id));
    }

    private static boolean isFateseerSignal(GameCore.CardDef d) {
        return d != null && (d.scry > 0 || d.upgradeRandom || d.draw > 0 || d.skillChargeGain > 0
                || d.vulnerable > 0 || d.bind > 0 || d.block > 0 || isHybridCore(d)
                || GameCore.PROF_FATESEER.equals(d.profession));
    }

    private static boolean isFateseerCard(GameCore.CardDef d) {
        return d != null && ("fateseer_omen".equals(d.id) || "fateseer_veil".equals(d.id)
                || "fateseer_wheel".equals(d.id) || "fateseer_thread".equals(d.id)
                || "fateseer_overfate".equals(d.id) || "fateseer_grand_design".equals(d.id));
    }

    private static boolean isTidecallerSignal(GameCore.CardDef d) {
        return d != null && (d.block > 0 || d.type == 1 || d.bind > 0 || d.draw > 0
                || d.energyGain > 0 || d.skillChargeGain > 0 || d.cost == 0 || d.vulnerable > 0
                || isHybridCore(d) || GameCore.PROF_TIDECALLER.equals(d.profession));
    }

    private static boolean isTidecallerCard(GameCore.CardDef d) {
        return d != null && ("tidecaller_ripple".equals(d.id) || "tidecaller_breaker".equals(d.id)
                || "tidecaller_current".equals(d.id) || "tidecaller_surge".equals(d.id)
                || "tidecaller_overtide".equals(d.id) || "tidecaller_grand_tide".equals(d.id));
    }

    private static boolean isFrostbinderSignal(GameCore.CardDef d) {
        return d != null && (d.bind > 0 || d.block > 0 || d.draw > 0 || d.exhaust
                || d.exhaustTopDiscard || d.createEcho || d.skillChargeGain > 0
                || d.vulnerable > 0 || d.createWound || "wound".equals(d.id) || "daze".equals(d.id)
                || isHybridCore(d) || GameCore.PROF_FROSTBINDER.equals(d.profession));
    }

    private static boolean isFrostbinderCard(GameCore.CardDef d) {
        return d != null && ("frostbinder_shard".equals(d.id) || "frostbinder_ward".equals(d.id)
                || "frostbinder_rime".equals(d.id) || "frostbinder_shatter".equals(d.id)
                || "frostbinder_overfreeze".equals(d.id) || "frostbinder_grand_winter".equals(d.id));
    }

    private static boolean isPlaguedoctorSignal(GameCore.CardDef d) {
        return d != null && (d.createPotion || d.burn > 0 || d.bind > 0 || d.vulnerable > 0
                || d.heal > 0 || d.block > 0 || d.draw > 0 || d.createEcho || d.skillChargeGain > 0
                || d.createWound || "wound".equals(d.id) || "daze".equals(d.id)
                || isHybridCore(d) || GameCore.PROF_PLAGUEDOCTOR.equals(d.profession));
    }

    private static boolean isPlaguedoctorCard(GameCore.CardDef d) {
        return d != null && ("plaguedoctor_lancet".equals(d.id) || "plaguedoctor_mask".equals(d.id)
                || "plaguedoctor_culture".equals(d.id) || "plaguedoctor_quarantine".equals(d.id)
                || "plaguedoctor_overdose".equals(d.id) || "plaguedoctor_grand_plague".equals(d.id));
    }

    private static boolean isArchivistSignal(GameCore.CardDef d) {
        return d != null && (d.scry > 0 || d.upgradeRandom || d.draw > 0 || d.block > 0
                || d.createEcho || d.exhaustTopDiscard || d.skillChargeGain > 0
                || d.vulnerable > 0 || d.bind > 0 || "wound".equals(d.id) || "daze".equals(d.id)
                || isHybridCore(d) || GameCore.PROF_ARCHIVIST.equals(d.profession));
    }

    private static boolean isArchivistCard(GameCore.CardDef d) {
        return d != null && ("archivist_index".equals(d.id) || "archivist_seal".equals(d.id)
                || "archivist_catalog".equals(d.id) || "archivist_redline".equals(d.id)
                || "archivist_overfile".equals(d.id) || "archivist_grand_archive".equals(d.id));
    }

    private static boolean isVoidnavigatorSignal(GameCore.CardDef d) {
        return d != null && (d.createEcho || d.exhaust || d.exhaustTopDiscard || d.scry > 0
                || d.draw > 0 || d.energyGain > 0 || d.skillChargeGain > 0
                || d.vulnerable > 0 || d.bind > 0 || isHybridCore(d)
                || GameCore.PROF_VOIDNAVIGATOR.equals(d.profession) || GameCore.PROF_ARCANIST.equals(d.profession));
    }

    private static boolean isVoidnavigatorCard(GameCore.CardDef d) {
        return d != null && ("voidnavigator_beacon".equals(d.id) || "voidnavigator_anchor".equals(d.id)
                || "voidnavigator_chart".equals(d.id) || "voidnavigator_rift".equals(d.id)
                || "voidnavigator_overjump".equals(d.id) || "voidnavigator_grand_jump".equals(d.id));
    }

    private static int voidnavigatorEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 3 + e.bind * 2 + e.burn;
            }
        }
        pressure += Math.min(4, s.confluenceChain);
        pressure += Math.min(5, s.exhaust.size() / 2);
        return pressure;
    }

    private static boolean isRelicsmithSignal(GameCore.CardDef d) {
        return d != null && (d.goldGain > 0 || d.goldDamage || d.goldBlock || d.upgradeRandom
                || d.block > 0 || d.draw > 0 || d.scry > 0 || d.skillChargeGain > 0
                || d.vulnerable > 0 || d.bind > 0 || isHybridCore(d)
                || GameCore.PROF_RELICSMITH.equals(d.profession) || GameCore.PROF_MERCHANT.equals(d.profession));
    }

    private static boolean isRelicsmithCard(GameCore.CardDef d) {
        return d != null && ("relicsmith_key".equals(d.id) || "relicsmith_lock".equals(d.id)
                || "relicsmith_gauge".equals(d.id) || "relicsmith_unlock".equals(d.id)
                || "relicsmith_overvault".equals(d.id) || "relicsmith_grand_vault".equals(d.id));
    }

    private static int relicsmithEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 3 + e.bind * 2 + e.burn;
            }
        }
        pressure += Math.min(5, s.relics.size());
        pressure += Math.min(5, s.gold / 40);
        return pressure;
    }

    private static boolean isBeastmasterSignal(GameCore.CardDef d) {
        return d != null && (d.createEcho || d.bind > 0 || d.heal > 0 || d.block > 0
                || d.draw > 0 || d.skillChargeGain > 0 || d.vulnerable > 0 || d.cost == 0
                || isHybridCore(d) || GameCore.PROF_BEASTMASTER.equals(d.profession)
                || GameCore.PROF_GARDENER.equals(d.profession));
    }

    private static boolean isBeastmasterCard(GameCore.CardDef d) {
        return d != null && ("beastmaster_claw".equals(d.id) || "beastmaster_hide".equals(d.id)
                || "beastmaster_call".equals(d.id) || "beastmaster_pounce".equals(d.id)
                || "beastmaster_overpack".equals(d.id) || "beastmaster_grand_hunt".equals(d.id));
    }

    private static int beastmasterEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 3 + e.bind * 3 + e.burn;
            }
        }
        pressure += Math.min(5, tempOrEchoHandCards(s));
        pressure += Math.min(4, s.block / 6);
        return pressure;
    }

    private static boolean isDragonbinderSignal(GameCore.CardDef d) {
        return d != null && (d.burn > 0 || d.createEcho || d.heal > 0 || d.block > 0
                || d.draw > 0 || d.skillChargeGain > 0 || d.vulnerable > 0 || d.cost == 0
                || isHybridCore(d) || GameCore.PROF_DRAGONBINDER.equals(d.profession)
                || GameCore.PROF_ALCHEMIST.equals(d.profession) || GameCore.PROF_CHEF.equals(d.profession)
                || GameCore.PROF_WITCH.equals(d.profession));
    }

    private static boolean isDragonbinderCard(GameCore.CardDef d) {
        return d != null && ("dragonbinder_spark".equals(d.id) || "dragonbinder_scale".equals(d.id)
                || "dragonbinder_hatch".equals(d.id) || "dragonbinder_talon".equals(d.id)
                || "dragonbinder_overflame".equals(d.id) || "dragonbinder_grand_oath".equals(d.id));
    }

    private static int dragonbinderEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 3 + e.burn * 3 + e.bind;
            }
        }
        pressure += Math.min(5, tempOrEchoHandCards(s));
        pressure += Math.min(4, s.block / 6);
        return pressure;
    }

    private static boolean isSoulbinderSignal(GameCore.CardDef d) {
        return d != null && (d.createEcho || d.exhaust || d.exhaustTopDiscard || d.heal > 0
                || d.block > 0 || d.draw > 0 || d.skillChargeGain > 0 || d.vulnerable > 0
                || d.bind > 0 || d.createWound || "wound".equals(d.id) || "daze".equals(d.id)
                || isHybridCore(d) || GameCore.PROF_SOULBINDER.equals(d.profession)
                || GameCore.PROF_MEDIUM.equals(d.profession) || GameCore.PROF_DREAMWALKER.equals(d.profession)
                || GameCore.PROF_PLAGUEDOCTOR.equals(d.profession));
    }

    private static boolean isSoulbinderCard(GameCore.CardDef d) {
        return d != null && ("soulbinder_thread".equals(d.id) || "soulbinder_veil".equals(d.id)
                || "soulbinder_pact".equals(d.id) || "soulbinder_lash".equals(d.id)
                || "soulbinder_overbind".equals(d.id) || "soulbinder_grand_pact".equals(d.id));
    }

    private static int soulbinderEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 3 + e.bind * 3 + e.burn;
            }
        }
        pressure += Math.min(5, tempOrEchoHandCards(s));
        pressure += Math.min(5, s.exhaust.size() / 2);
        return pressure;
    }

    private static boolean isStarforgerSignal(GameCore.CardDef d) {
        return d != null && (d.upgradeRandom || d.scry > 0 || d.burn > 0 || d.block > 0
                || d.draw > 0 || d.skillChargeGain > 0 || d.vulnerable > 0 || isHybridCore(d)
                || GameCore.PROF_STARFORGER.equals(d.profession) || GameCore.PROF_RUNEBLADE.equals(d.profession)
                || GameCore.PROF_MACHINIST.equals(d.profession) || GameCore.PROF_GEOMANCER.equals(d.profession));
    }

    private static boolean isStarforgerCard(GameCore.CardDef d) {
        return d != null && ("starforger_spark".equals(d.id) || "starforger_guard".equals(d.id)
                || "starforger_crucible".equals(d.id) || "starforger_hammer".equals(d.id)
                || "starforger_overforge".equals(d.id) || "starforger_grand_star".equals(d.id));
    }

    private static int starforgerEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 3 + e.burn * 3 + e.bind;
            }
        }
        pressure += Math.min(6, upgradedDeckCards(s) / 2);
        pressure += Math.min(4, s.block / 6);
        return pressure;
    }

    private static boolean isPathfinderSignal(GameCore.CardDef d) {
        return d != null && (d.scry > 0 || d.draw > 0 || d.block > 0 || d.skillChargeGain > 0
                || d.vulnerable > 0 || d.bind > 0 || d.upgradeRandom || isHybridCore(d)
                || GameCore.PROF_PATHFINDER.equals(d.profession) || GameCore.PROF_TACTICIAN.equals(d.profession)
                || GameCore.PROF_FATESEER.equals(d.profession) || GameCore.PROF_VOIDNAVIGATOR.equals(d.profession));
    }

    private static boolean isPathfinderCard(GameCore.CardDef d) {
        return d != null && ("pathfinder_mark".equals(d.id) || "pathfinder_shelter".equals(d.id)
                || "pathfinder_survey".equals(d.id) || "pathfinder_shortcut".equals(d.id)
                || "pathfinder_overroute".equals(d.id) || "pathfinder_grand_route".equals(d.id));
    }

    private static int pathfinderEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 3 + e.bind * 3 + e.burn;
            }
        }
        pressure += Math.min(5, upgradedDeckCards(s) / 2);
        pressure += Math.min(4, s.confluenceChain);
        pressure += Math.min(4, s.block / 6);
        return pressure;
    }

    private static boolean isArrayistSignal(GameCore.CardDef d) {
        return d != null && (d.cost == 0 || d.draw > 0 || d.energyGain > 0 || d.createEcho
                || d.block > 0 || d.skillChargeGain > 0 || d.upgradeRandom || isHybridCore(d)
                || GameCore.PROF_ARRAYIST.equals(d.profession) || GameCore.PROF_BARD.equals(d.profession)
                || GameCore.PROF_DUELIST.equals(d.profession) || GameCore.PROF_CHRONOMANCER.equals(d.profession));
    }

    private static boolean isArrayistCard(GameCore.CardDef d) {
        return d != null && ("arrayist_glyph".equals(d.id) || "arrayist_bastion".equals(d.id)
                || "arrayist_pivot".equals(d.id) || "arrayist_lance".equals(d.id)
                || "arrayist_overarray".equals(d.id) || "arrayist_grand_array".equals(d.id));
    }

    private static int arrayistEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 3 + e.bind * 3 + e.burn;
            }
        }
        pressure += Math.min(5, tempOrEchoHandCards(s));
        pressure += Math.min(4, s.confluenceChain);
        pressure += Math.min(4, s.block / 6);
        return pressure;
    }

    private static boolean isGambiterSignal(GameCore.CardDef d) {
        return d != null && (d.cost == 0 || d.draw > 0 || d.block > 0 || d.skillChargeGain > 0
                || d.vulnerable > 0 || d.bind > 0 || d.upgradeRandom || isHybridCore(d)
                || d.type == 0 || d.type == 1 || GameCore.PROF_GAMBITER.equals(d.profession)
                || GameCore.PROF_DUELIST.equals(d.profession) || GameCore.PROF_TACTICIAN.equals(d.profession)
                || GameCore.PROF_ADJUDICATOR.equals(d.profession));
    }

    private static boolean isGambiterCard(GameCore.CardDef d) {
        return d != null && ("gambiter_pawn".equals(d.id) || "gambiter_castle".equals(d.id)
                || "gambiter_gambit".equals(d.id) || "gambiter_fork".equals(d.id)
                || "gambiter_overmate".equals(d.id) || "gambiter_grand_endgame".equals(d.id));
    }

    private static int gambiterEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 3 + e.bind * 3 + e.burn;
            }
        }
        pressure += Math.min(4, s.block / 6);
        pressure += Math.min(4, s.cardsPlayedThisTurn);
        pressure += Math.min(4, s.confluenceChain);
        return pressure;
    }

    private static boolean isGravekeeperSignal(GameCore.CardDef d) {
        return d != null && (d.exhaust || d.exhaustTopDiscard || d.createWound || d.heal > 0 || d.block > 0
                || d.draw > 0 || d.skillChargeGain > 0 || d.bind > 0 || d.vulnerable > 0
                || d.type == 1 || GameCore.PROF_GRAVEKEEPER.equals(d.profession)
                || GameCore.PROF_SOULBINDER.equals(d.profession) || GameCore.PROF_PLAGUEDOCTOR.equals(d.profession)
                || GameCore.PROF_FROSTBINDER.equals(d.profession));
    }

    private static boolean isGravekeeperCard(GameCore.CardDef d) {
        return d != null && ("gravekeeper_lantern".equals(d.id) || "gravekeeper_shroud".equals(d.id)
                || "gravekeeper_interment".equals(d.id) || "gravekeeper_dirge".equals(d.id)
                || "gravekeeper_overwake".equals(d.id) || "gravekeeper_grand_requiem".equals(d.id));
    }

    private static int gravekeeperEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 3 + e.bind * 4 + e.burn;
            }
        }
        pressure += Math.min(4, s.exhaust.size());
        pressure += Math.min(4, statusDeckCards(s) + statusHandCards(s));
        pressure += Math.min(4, s.block / 6);
        return pressure;
    }

    private static boolean isTreasurerSignal(GameCore.CardDef d) {
        return d != null && (d.goldGain > 0 || d.goldDamage || d.goldBlock || d.block > 0
                || d.draw > 0 || d.upgradeRandom || d.skillChargeGain > 0 || d.vulnerable > 0
                || d.bind > 0 || isHybridCore(d) || GameCore.PROF_TREASURER.equals(d.profession)
                || GameCore.PROF_MERCHANT.equals(d.profession) || GameCore.PROF_PACTMAKER.equals(d.profession)
                || GameCore.PROF_RELICSMITH.equals(d.profession));
    }

    private static boolean isTreasurerCard(GameCore.CardDef d) {
        return d != null && ("treasurer_entry".equals(d.id) || "treasurer_vault".equals(d.id)
                || "treasurer_audit".equals(d.id) || "treasurer_collection".equals(d.id)
                || "treasurer_overledger".equals(d.id) || "treasurer_grand_balance".equals(d.id));
    }

    private static int treasurerEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 3 + e.bind * 3 + e.burn;
            }
        }
        pressure += Math.min(6, s.gold / 35);
        pressure += Math.min(5, upgradedDeckCards(s) / 2);
        pressure += Math.min(4, s.block / 6);
        return pressure;
    }

    private static boolean isDrifterSignal(GameCore.State s, GameCore.CardDef d) {
        return d != null && (isOffPoolCard(s, d) || d.createEcho || d.draw > 0 || d.block > 0
                || d.upgradeRandom || d.skillChargeGain > 0 || d.vulnerable > 0 || d.bind > 0
                || isHybridCore(d) || GameCore.PROF_DRIFTER.equals(d.profession)
                || GameCore.PROF_VOIDNAVIGATOR.equals(d.profession) || GameCore.PROF_PRISMIST.equals(d.profession)
                || GameCore.PROF_SHIFTER.equals(d.profession) || GameCore.PROF_PATHFINDER.equals(d.profession));
    }

    private static boolean isDrifterCard(GameCore.CardDef d) {
        return d != null && ("drifter_scout".equals(d.id) || "drifter_hideout".equals(d.id)
                || "drifter_patch".equals(d.id) || "drifter_raid".equals(d.id)
                || "drifter_overcross".equals(d.id) || "drifter_grand_junction".equals(d.id));
    }

    private static int drifterCrossPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 3 + e.bind * 3 + e.burn;
            }
        }
        pressure += Math.min(6, offPoolDeckCards(s) + offPoolHandCards(s));
        pressure += Math.min(5, tempOrEchoHandCards(s));
        pressure += Math.min(4, s.confluenceChain);
        return pressure;
    }

    private static boolean isOathkeeperSignal(GameCore.CardDef d) {
        return d != null && (d.block > 0 || d.heal > 0 || d.retainBlock || d.blockToDamage
                || d.draw > 0 || d.upgradeRandom || d.skillChargeGain > 0 || d.vulnerable > 0
                || d.bind > 0 || isHybridCore(d) || GameCore.PROF_OATHKEEPER.equals(d.profession)
                || GameCore.PROF_WARDEN.equals(d.profession) || GameCore.PROF_LIGHTKEEPER.equals(d.profession)
                || GameCore.PROF_ADJUDICATOR.equals(d.profession) || GameCore.PROF_TACTICIAN.equals(d.profession));
    }

    private static boolean isOathkeeperCard(GameCore.CardDef d) {
        return d != null && ("oathkeeper_vow".equals(d.id) || "oathkeeper_guard".equals(d.id)
                || "oathkeeper_smite".equals(d.id) || "oathkeeper_sanctuary".equals(d.id)
                || "oathkeeper_overedict".equals(d.id) || "oathkeeper_grand_judgment".equals(d.id));
    }

    private static int oathkeeperPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 4 + e.bind * 3 + e.burn;
            }
        }
        pressure += Math.min(6, s.block / 6);
        pressure += Math.min(5, healingDeckCards(s));
        pressure += Math.min(5, upgradedDeckCards(s) / 2);
        return pressure;
    }

    private static boolean isMoonsingerSignal(GameCore.CardDef d) {
        return d != null && (d.scry > 0 || d.draw > 0 || d.createEcho || d.energyGain > 0
                || d.upgradeRandom || d.skillChargeGain > 0 || d.vulnerable > 0 || d.bind > 0
                || d.heal > 0 || isHybridCore(d) || GameCore.PROF_MOONSINGER.equals(d.profession)
                || GameCore.PROF_ASTROLOGER.equals(d.profession) || GameCore.PROF_BARD.equals(d.profession)
                || GameCore.PROF_DREAMWALKER.equals(d.profession) || GameCore.PROF_FATESEER.equals(d.profession));
    }

    private static boolean isMoonsingerCard(GameCore.CardDef d) {
        return d != null && ("moonsinger_newmoon".equals(d.id) || "moonsinger_crescent".equals(d.id)
                || "moonsinger_eclipse".equals(d.id) || "moonsinger_tide".equals(d.id)
                || "moonsinger_overmoon".equals(d.id) || "moonsinger_grand_eclipse".equals(d.id));
    }

    private static int moonsingerPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 4 + e.bind * 3 + e.burn;
            }
        }
        pressure += Math.min(6, scryDeckCards(s));
        pressure += Math.min(5, tempOrEchoHandCards(s) + tempOrEchoDeckCards(s) / 2);
        pressure += Math.min(5, upgradedDeckCards(s) / 2);
        return pressure;
    }

    private static boolean isSpySignal(GameCore.CardDef d) {
        return d != null && (d.cost == 0 || d.draw > 0 || d.goldGain > 0 || d.goldDamage
                || d.goldBlock || d.createEcho || d.energyGain > 0 || d.upgradeRandom
                || d.skillChargeGain > 0 || d.vulnerable > 0 || d.bind > 0 || isHybridCore(d)
                || GameCore.PROF_SPY.equals(d.profession) || GameCore.PROF_SHADOWDANCER.equals(d.profession)
                || GameCore.PROF_MERCHANT.equals(d.profession) || GameCore.PROF_DUELIST.equals(d.profession)
                || GameCore.PROF_SCAVENGER.equals(d.profession));
    }

    private static boolean isSpyCard(GameCore.CardDef d) {
        return d != null && ("spy_contact".equals(d.id) || "spy_smoke".equals(d.id)
                || "spy_blackmail".equals(d.id) || "spy_falseflag".equals(d.id)
                || "spy_overcover".equals(d.id) || "spy_grand_heist".equals(d.id));
    }

    private static int spyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 4 + e.bind * 3 + e.burn;
            }
        }
        pressure += Math.min(6, s.gold / 35);
        pressure += Math.min(5, tempOrEchoHandCards(s) + tempOrEchoDeckCards(s) / 2);
        pressure += Math.min(5, upgradedDeckCards(s) / 2);
        return pressure;
    }

    private static boolean isPerfumerSignal(GameCore.CardDef d) {
        return d != null && (d.createPotion || d.heal > 0 || d.burn > 0 || d.bind > 0
                || d.vulnerable > 0 || d.draw > 0 || d.upgradeRandom || d.skillChargeGain > 0
                || d.exhaust || isHybridCore(d) || GameCore.PROF_PERFUMER.equals(d.profession)
                || GameCore.PROF_ALCHEMIST.equals(d.profession) || GameCore.PROF_CHEF.equals(d.profession)
                || GameCore.PROF_WITCH.equals(d.profession) || GameCore.PROF_PLAGUEDOCTOR.equals(d.profession));
    }

    private static boolean isPerfumerCard(GameCore.CardDef d) {
        return d != null && ("perfumer_note".equals(d.id) || "perfumer_mist".equals(d.id)
                || "perfumer_caustic".equals(d.id) || "perfumer_distill".equals(d.id)
                || "perfumer_overaroma".equals(d.id) || "perfumer_grand_bloom".equals(d.id));
    }

    private static int perfumerPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.burn * 3 + e.bind * 4 + e.vulnerable * 3 + e.mark * 2;
            }
        }
        pressure += Math.min(6, potionCards(s) + s.potions.size());
        pressure += Math.min(5, healingDeckCards(s));
        pressure += Math.min(5, upgradedDeckCards(s) / 2);
        return pressure;
    }

    private static boolean isClocksmithSignal(GameCore.CardDef d) {
        return d != null && (d.cost == 0 || d.draw > 0 || d.energyGain > 0 || d.createEcho
                || d.upgradeRandom || d.skillChargeGain > 0 || d.vulnerable > 0 || d.bind > 0
                || d.block > 0 || isHybridCore(d) || GameCore.PROF_CLOCKSMITH.equals(d.profession)
                || GameCore.PROF_TUNER.equals(d.profession) || GameCore.PROF_CHRONOMANCER.equals(d.profession)
                || GameCore.PROF_MACHINIST.equals(d.profession) || GameCore.PROF_ARRAYIST.equals(d.profession));
    }

    private static boolean isClocksmithCard(GameCore.CardDef d) {
        return d != null && ("clocksmith_tick".equals(d.id) || "clocksmith_spring".equals(d.id)
                || "clocksmith_gear".equals(d.id) || "clocksmith_rewind".equals(d.id)
                || "clocksmith_overclock".equals(d.id) || "clocksmith_grand_chronogear".equals(d.id));
    }

    private static int clocksmithPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 4 + e.bind * 3 + e.burn;
            }
        }
        pressure += Math.min(6, zeroCostDeckCards(s));
        pressure += Math.min(5, tempOrEchoHandCards(s) + tempOrEchoDeckCards(s) / 2);
        pressure += Math.min(5, upgradedDeckCards(s) / 2);
        return pressure;
    }

    private static boolean isMintsmithSignal(GameCore.CardDef d) {
        return d != null && (d.goldGain > 0 || d.goldDamage || d.goldBlock || d.burn > 0
                || d.detonateBurn || d.burnToBlock || d.block > 0 || d.upgradeRandom
                || d.skillChargeGain > 0 || d.vulnerable > 0 || d.bind > 0 || isHybridCore(d)
                || GameCore.PROF_MINTSMITH.equals(d.profession) || GameCore.PROF_MERCHANT.equals(d.profession)
                || GameCore.PROF_TREASURER.equals(d.profession) || GameCore.PROF_ALCHEMIST.equals(d.profession)
                || GameCore.PROF_PERFUMER.equals(d.profession) || GameCore.PROF_STARFORGER.equals(d.profession));
    }

    private static boolean isMintsmithCard(GameCore.CardDef d) {
        return d != null && ("mintsmith_spark".equals(d.id) || "mintsmith_mold".equals(d.id)
                || "mintsmith_tax".equals(d.id) || "mintsmith_assay".equals(d.id)
                || "mintsmith_overmint".equals(d.id) || "mintsmith_grand_mintage".equals(d.id));
    }

    private static int mintsmithPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.burn * 2 + e.vulnerable * 4 + e.bind * 3 + e.mark * 2;
            }
        }
        pressure += Math.min(6, s.gold / 25);
        pressure += Math.min(5, burnDeckCards(s));
        pressure += Math.min(5, upgradedDeckCards(s) / 2);
        return pressure;
    }

    private static int archivistEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 3 + e.bind * 2 + e.burn;
            }
        }
        return pressure;
    }

    private static int plaguedoctorEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.burn * 3 + e.bind * 3 + e.vulnerable * 2 + e.mark * 2;
            }
        }
        return pressure;
    }

    private static int frostbinderEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.bind * 4 + e.vulnerable * 3 + e.mark * 2 + e.burn;
            }
        }
        return pressure;
    }

    private static int tidecallerEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.bind * 4 + e.vulnerable * 3 + e.mark * 2 + e.burn;
            }
        }
        return pressure;
    }

    private static int stormcallerEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.burn + e.vulnerable + e.bind + e.mark * 2;
            }
        }
        return pressure;
    }

    private static int lightkeeperEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 2 + e.vulnerable * 3 + e.bind * 2 + e.burn;
            }
        }
        return pressure;
    }

    private static int witchEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.burn * 3 + e.bind * 3 + e.vulnerable * 2 + e.mark * 2;
            }
        }
        return pressure;
    }

    private static int shifterEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 3 + e.bind * 2 + e.burn;
            }
        }
        return pressure;
    }

    private static int fateseerEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 3 + e.vulnerable * 3 + e.bind * 2 + e.burn;
            }
        }
        return pressure;
    }

    private static int geomancerEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.bind * 3 + e.burn * 2 + e.vulnerable * 2 + e.mark * 2;
            }
        }
        return pressure;
    }

    private static int shadowdancerEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 2 + e.vulnerable * 2 + e.bind + e.burn;
            }
        }
        return pressure;
    }

    private static int runebladeEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 2 + e.vulnerable * 3 + e.bind + e.burn;
            }
        }
        return pressure;
    }

    private static int mediumEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.bind * 2 + e.mark * 2 + e.vulnerable * 2 + e.burn;
            }
        }
        return pressure;
    }

    private static int tacticianEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 2 + e.vulnerable * 2 + e.bind + e.burn;
            }
        }
        return pressure;
    }

    private static int prismistEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 2 + e.vulnerable * 2 + e.bind + e.burn;
            }
        }
        return pressure;
    }

    private static int dreamwalkerEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.bind * 2 + e.mark * 2 + e.vulnerable * 2 + e.burn;
            }
        }
        return pressure;
    }

    private static int gardenerEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.bind * 2 + e.mark * 2 + e.vulnerable * 2 + e.burn;
            }
        }
        return pressure;
    }

    private static int chefEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.burn * 2 + e.bind * 2 + e.mark * 2 + e.vulnerable * 2;
            }
        }
        return pressure;
    }

    private static int bardEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 2 + e.vulnerable * 2 + e.bind * 2 + e.burn;
            }
        }
        return pressure;
    }

    private static int mirroristEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 2 + e.vulnerable * 3 + e.bind + e.burn;
            }
        }
        return pressure;
    }

    private static int puppeteerEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.bind * 3 + e.mark * 2 + e.vulnerable * 2 + e.burn;
            }
        }
        return pressure;
    }

    private static int scavengerEnemyPressure(GameCore.State s) {
        int pressure = 0;
        for (GameCore.Enemy e : s.enemies) {
            if (e.hp > 0) {
                pressure += e.mark * 2 + e.vulnerable * 2 + e.bind * 2 + e.burn;
            }
        }
        return pressure;
    }

    private static int upgradedDeckCards(GameCore.State s) {
        int count = 0;
        for (GameCore.Card c : s.deck) {
            if (c.upgraded) count++;
        }
        return count;
    }

    private static int zeroCostDeckCards(GameCore.State s) {
        int count = 0;
        for (GameCore.Card c : s.deck) {
            GameCore.CardDef d = GameCore.card(c.id);
            if (d != null && d.cost == 0) count++;
        }
        return count;
    }

    private static int burnDeckCards(GameCore.State s) {
        int count = 0;
        for (GameCore.Card c : s.deck) {
            GameCore.CardDef d = GameCore.card(c.id);
            if (d != null && (d.burn > 0 || d.detonateBurn || d.burnToBlock)) count++;
        }
        return count;
    }

    private static int tempOrEchoDeckCards(GameCore.State s) {
        int count = 0;
        for (GameCore.Card c : s.deck) {
            GameCore.CardDef d = GameCore.card(c.id);
            if (c.temp || (d != null && d.createEcho)) count++;
        }
        return count;
    }

    private static int scryDeckCards(GameCore.State s) {
        int count = 0;
        for (GameCore.Card c : s.deck) {
            GameCore.CardDef d = GameCore.card(c.id);
            if (d != null && d.scry > 0) count++;
        }
        return count;
    }

    private static int tempOrEchoHandCards(GameCore.State s) {
        int count = 0;
        for (GameCore.Card c : s.hand) {
            GameCore.CardDef d = GameCore.card(c.id);
            if (c.temp || (d != null && d.createEcho)) count++;
        }
        return count;
    }

    private static int statusDeckCards(GameCore.State s) {
        int count = 0;
        for (GameCore.Card c : s.deck) {
            if ("wound".equals(c.id) || "daze".equals(c.id)) count++;
        }
        return count;
    }

    private static int statusHandCards(GameCore.State s) {
        int count = 0;
        for (GameCore.Card c : s.hand) {
            if ("wound".equals(c.id) || "daze".equals(c.id)) count++;
        }
        return count;
    }

    private static int bindDeckCards(GameCore.State s) {
        int count = 0;
        for (GameCore.Card c : s.deck) {
            GameCore.CardDef d = GameCore.card(c.id);
            if (d != null && d.bind > 0) count++;
        }
        return count;
    }

    private static int healingDeckCards(GameCore.State s) {
        int count = 0;
        for (GameCore.Card c : s.deck) {
            GameCore.CardDef d = GameCore.card(c.id);
            if (d != null && d.heal > 0) count++;
        }
        return count;
    }

    private static int potionCards(GameCore.State s) {
        int count = 0;
        for (GameCore.Card c : s.deck) {
            GameCore.CardDef d = GameCore.card(c.id);
            if (d != null && d.createPotion) count++;
        }
        return count;
    }

    private static boolean isOffPoolCard(GameCore.State s, GameCore.CardDef d) {
        boolean offOrigin = !"通用".equals(d.origin) && !d.origin.equals(s.origin);
        boolean offProfession = d.profession.length() > 0 && !d.profession.equals(s.profession);
        return offOrigin || offProfession;
    }

    private static int offPoolDeckCards(GameCore.State s) {
        return offPoolCardsInPile(s, s.deck);
    }

    private static int offPoolHandCards(GameCore.State s) {
        return offPoolCardsInPile(s, s.hand);
    }

    private static int offPoolCardsInPile(GameCore.State s, java.util.List<GameCore.Card> pile) {
        int count = 0;
        for (GameCore.Card c : pile) {
            GameCore.CardDef d = GameCore.card(c.id);
            if (d != null && isOffPoolCard(s, d)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isResonanceBridgeCard(GameCore.CardDef d) {
        return d != null && ("fusion_spark".equals(d.id) || "echo_forge_loop".equals(d.id)
                || "bloodcoin_catalyst".equals(d.id) || "prism_guard_matrix".equals(d.id)
                || "apex_resonance".equals(d.id));
    }

    private static boolean isHybridCore(GameCore.CardDef d) {
        return d != null && ("hybrid_forgebrand".equals(d.id) || "hybrid_echo_step".equals(d.id)
                || "hybrid_blood_tithe".equals(d.id) || "hybrid_guard_conduit".equals(d.id)
                || "hybrid_plague_brew".equals(d.id) || "hybrid_coinwall".equals(d.id)
                || "hybrid_bloodcharge".equals(d.id) || "hybrid_echo_vial".equals(d.id)
                || "hybrid_hexdance".equals(d.id) || "hybrid_spirit_anvil".equals(d.id)
                || "hybrid_rift_engine".equals(d.id) || "fusion_spark".equals(d.id)
                || "echo_forge_loop".equals(d.id) || "bloodcoin_catalyst".equals(d.id)
                || "prism_guard_matrix".equals(d.id) || "apex_resonance".equals(d.id));
    }

    private static boolean isConfluenceCore(GameCore.CardDef d) {
        return d != null && ("confluence_chord".equals(d.id) || "prism_anchor".equals(d.id)
                || "apex_confluence".equals(d.id) || "fusion_spark".equals(d.id)
                || "echo_forge_loop".equals(d.id) || "bloodcoin_catalyst".equals(d.id)
                || "prism_guard_matrix".equals(d.id) || "apex_resonance".equals(d.id));
    }

    private static boolean isCascadeSignal(GameCore.CardDef d) {
        return d != null && ("cascade_probe".equals(d.id) || "cascade_apex".equals(d.id)
                || d.draw > 0 || d.createEcho || d.skillChargeGain > 0 || d.energyGain > 0
                || d.upgradeRandom || d.vulnerable > 0 || d.bind > 0
                || isHybridCore(d) || isConfluenceCore(d));
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
