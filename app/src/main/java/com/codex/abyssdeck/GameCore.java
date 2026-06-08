package com.codex.abyssdeck;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class GameCore {
    public static final int MODE_TITLE = 0;
    public static final int MODE_ORIGIN = 1;
    public static final int MODE_MAP = 2;
    public static final int MODE_COMBAT = 3;
    public static final int MODE_REWARD = 4;
    public static final int MODE_SHOP = 5;
    public static final int MODE_REST = 6;
    public static final int MODE_EVENT = 7;
    public static final int MODE_DECK = 8;
    public static final int MODE_GAME_OVER = 9;
    public static final int MODE_VICTORY = 10;
    public static final int MODE_CODEX = 11;
    public static final int MODE_BOON = 12;
    public static final int MODE_CLASS = 13;
    public static final int MODE_TALENT = 14;

    public static final int ENEMY_ATTACK = 0;
    public static final int ENEMY_BUFF = 1;
    public static final int ENEMY_DEBUFF = 2;
    public static final int ENEMY_GUARD = 3;
    public static final int ENEMY_SPECIAL = 4;

    public static final int MOD_NONE = 0;
    public static final int MOD_ARMORED = 1;
    public static final int MOD_FRENZY = 2;
    public static final int MOD_POLLUTED = 3;
    public static final int MOD_BOUNTY = 4;
    public static final int MOD_TURBULENT = 5;

    public static final int QUEST_NONE = 0;
    public static final int QUEST_SWIFT = 1;
    public static final int QUEST_UNHURT = 2;
    public static final int QUEST_COMBO = 3;
    public static final int QUEST_GUARD = 4;
    public static final int QUEST_HEX = 5;
    public static final int QUEST_LEAN = 6;

    public static final String ORIGIN_STEEL = "钢律";
    public static final String ORIGIN_ASH = "烬火";
    public static final String ORIGIN_WILD = "森息";
    public static final String ORIGIN_VOID = "虚空";

    public static final String PROF_WARDEN = "守卫";
    public static final String PROF_DUELIST = "决斗者";
    public static final String PROF_ALCHEMIST = "炼金师";
    public static final String PROF_RANGER = "游侠";
    public static final String PROF_ARCANIST = "秘术师";
    public static final String PROF_MERCHANT = "行商";
    public static final String PROF_BLOODBOUND = "血契者";
    public static final String PROF_WEAVER = "织牌师";
    public static final String[] PROFESSIONS = {
            PROF_WARDEN, PROF_DUELIST, PROF_ALCHEMIST, PROF_RANGER,
            PROF_ARCANIST, PROF_MERCHANT, PROF_BLOODBOUND, PROF_WEAVER
    };

    public static final ArrayList<CardDef> CARD_LIBRARY = new ArrayList<>();
    public static final ArrayList<RelicDef> RELIC_LIBRARY = new ArrayList<>();
    public static final ArrayList<PotionDef> POTION_LIBRARY = new ArrayList<>();
    public static final ArrayList<BoonDef> BOON_LIBRARY = new ArrayList<>();
    public static final ArrayList<TalentDef> TALENT_LIBRARY = new ArrayList<>();

    static {
        seedCards();
        seedRelics();
        seedPotions();
        seedBoons();
        seedTalents();
    }

    private GameCore() {
    }

    public static State fresh() {
        State s = new State();
        s.mode = MODE_TITLE;
        s.rngSeed = System.currentTimeMillis();
        s.run = new Random(s.rngSeed);
        return s;
    }

    public static void start(State s) {
        resetRun(s);
        s.mode = MODE_ORIGIN;
    }

    public static void chooseOrigin(State s, String origin) {
        int chosenDepth = s.ascension;
        resetRun(s);
        s.origin = origin;
        s.profession = "";
        s.maxHp = 76;
        s.hp = s.maxHp;
        s.gold = 68;
        s.act = 1;
        s.floor = 0;
        s.ascension = chosenDepth;
        s.potions.clear();
        s.deck.clear();
        s.relics.clear();
        s.seenCards.clear();
        s.seenRelics.clear();
        addStarterDeck(s, origin);
        addRelic(s, starterRelic(origin));
        s.mode = MODE_CLASS;
        log(s, "你带着" + origin + "印记进入深渊，选择这局的职业。");
    }

    public static void chooseProfession(State s, String profession) {
        if (s.mode != MODE_CLASS || s.origin.length() == 0 || !isProfession(profession)) {
            return;
        }
        s.profession = profession;
        addProfessionStarter(s, profession);
        generateMap(s);
        rollBoons(s);
        s.mode = MODE_BOON;
        log(s, s.origin + " " + profession + "整备完成，等待赐印。");
    }

    public static void chooseBoon(State s, int index) {
        if (s.mode != MODE_BOON || index < 0 || index >= s.boonChoices.size()) {
            return;
        }
        BoonDef b = boon(s.boonChoices.get(index));
        if (b == null) {
            s.mode = MODE_MAP;
            return;
        }
        if ("gold".equals(b.id)) {
            s.gold += 90;
        } else if ("maxhp".equals(b.id)) {
            s.maxHp += 10;
            s.hp += 10;
        } else if ("upgrade".equals(b.id)) {
            upgradeRandomDeckCard(s);
            upgradeRandomDeckCard(s);
        } else if ("remove".equals(b.id)) {
            removeStarterJunk(s);
        } else if ("rare".equals(b.id)) {
            CardDef d = randomCard(s, s.origin, true);
            Card c = new Card(d.id);
            c.upgraded = true;
            s.deck.add(c);
        } else if ("relic".equals(b.id)) {
            addRelic(s, randomRelic(s).id);
        } else if ("potion".equals(b.id)) {
            while (s.potions.size() < 2) {
                s.potions.add(POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size())).id);
            }
        } else if ("risk".equals(b.id)) {
            s.gold += 140;
            addStatusCard(s, "wound");
        }
        s.boonChoices.clear();
        s.mode = MODE_MAP;
        log(s, "选择赐印：" + b.name);
    }

    public static void chooseTalent(State s, int index) {
        if (s.mode != MODE_TALENT || index < 0 || index >= s.talentChoices.size()) {
            return;
        }
        TalentDef t = talent(s.talentChoices.get(index));
        if (t == null) {
            s.mode = MODE_MAP;
            return;
        }
        s.talents.add(t.id);
        s.talentChoices.clear();
        applyTalentPickup(s, t.id);
        log(s, "领悟专精：" + t.name);
        nextAct(s);
    }

    public static void chooseDepth(State s, int depth) {
        s.ascension = depth;
        s.mode = MODE_ORIGIN;
    }

    public static void openCodex(State s) {
        s.previousMode = s.mode;
        s.mode = MODE_CODEX;
    }

    public static void closePanel(State s) {
        if (s.mode == MODE_DECK || s.mode == MODE_CODEX) {
            s.mode = s.previousMode == 0 ? MODE_MAP : s.previousMode;
            s.pendingAction = "";
            s.previousMode = 0;
        }
    }

    public static void openDeck(State s, int previousMode) {
        s.previousMode = previousMode;
        s.deckView = 0;
        s.mode = MODE_DECK;
    }

    public static void openDeckView(State s, int previousMode, int deckView) {
        s.previousMode = previousMode;
        s.deckView = Math.max(0, Math.min(3, deckView));
        s.mode = MODE_DECK;
    }

    public static void mapChoose(State s, int nodeIndex) {
        if (s.mode != MODE_MAP || nodeIndex < 0 || nodeIndex >= s.map.size()) {
            return;
        }
        MapNode n = s.map.get(nodeIndex);
        if (n.floor != s.floor + 1 || (!n.available && !hasRelic(s, "night_map"))) {
            return;
        }
        for (MapNode m : s.map) {
            m.available = false;
        }
        s.currentNode = nodeIndex;
        s.floor = n.floor;
        for (Integer next : n.next) {
            if (next >= 0 && next < s.map.size()) {
                s.map.get(next).available = true;
            }
        }
        if (hasRelic(s, "night_map")) {
            for (MapNode m : s.map) {
                if (m.floor == s.floor + 1) {
                    m.available = true;
                }
            }
        }
        if (n.type == 'C' || n.type == 'E' || n.type == 'B') {
            startCombat(s, n.type);
        } else if (n.type == '$') {
            openShop(s);
        } else if (n.type == 'R') {
            openRest(s);
        } else {
            openEvent(s);
        }
    }

    public static void playCard(State s, int handIndex, int enemyIndex) {
        if (s.mode != MODE_COMBAT || !s.playerTurn || handIndex < 0 || handIndex >= s.hand.size()) {
            return;
        }
        Card c = s.hand.get(handIndex);
        CardDef d = card(c.id);
        if (d == null || s.energy < costOf(s, c, d)) {
            return;
        }
        if (d.targetEnemy && livingEnemies(s).isEmpty()) {
            return;
        }
        Enemy target = null;
        if (d.targetEnemy) {
            ArrayList<Enemy> living = livingEnemies(s);
            if (enemyIndex >= 0 && enemyIndex < s.enemies.size() && s.enemies.get(enemyIndex).hp > 0) {
                target = s.enemies.get(enemyIndex);
            } else {
                target = living.get(0);
            }
        }

        s.hand.remove(handIndex);
        s.energy -= costOf(s, c, d);
        s.cardsPlayedThisTurn++;
        s.totalCardsPlayed++;
        updateQuestProgress(s);
        boolean exhaust = c.temp || d.exhaust;
        applyCard(s, c, d, target);
        triggerAfterPlay(s, c, d);
        if (exhaust) {
            s.exhaust.add(c);
        } else {
            s.discard.add(c);
        }
        if (checkPlayerDefeated(s)) {
            return;
        }
        if (allEnemiesDead(s)) {
            winCombat(s);
        }
    }

    public static void endTurn(State s) {
        if (s.mode != MODE_COMBAT || !s.playerTurn) {
            return;
        }
        s.discard.addAll(s.hand);
        s.hand.clear();
        s.playerTurn = false;
        enemyTurn(s);
        if (checkPlayerDefeated(s)) {
            return;
        }
        beginPlayerTurn(s);
    }

    public static void pickRewardCard(State s, int index) {
        if (s.mode != MODE_REWARD || index < 0 || index >= s.cardRewards.size()) {
            return;
        }
        s.deck.add(new Card(s.cardRewards.get(index).id));
        s.seenCards.add(s.cardRewards.get(index).id);
        log(s, "获得卡牌：" + card(s.cardRewards.get(index).id).name);
        clearRewards(s);
        if (s.relicRewards.isEmpty()) {
            afterReward(s);
        }
    }

    public static void skipReward(State s) {
        if (s.mode == MODE_REWARD && !s.cardRewardSkipped && !s.cardRewards.isEmpty()) {
            clearRewards(s);
            s.cardRewardSkipped = true;
            int gold = hasRelic(s, "cracked_compass") ? 14 : 10;
            s.gold += gold;
            log(s, "跳过卡牌奖励，获得 " + gold + " 金币。");
            if (s.relicRewards.isEmpty()) {
                afterReward(s);
            }
        }
    }

    public static void pickRelicReward(State s, int index) {
        if (s.mode != MODE_REWARD || index < 0 || index >= s.relicRewards.size()) {
            return;
        }
        addRelic(s, s.relicRewards.get(index));
        s.relicRewards.clear();
        if (s.cardRewards.isEmpty()) {
            afterReward(s);
        }
    }

    public static void usePotion(State s, int potionIndex, int enemyIndex) {
        if (s.mode != MODE_COMBAT || potionIndex < 0 || potionIndex >= s.potions.size()) {
            return;
        }
        PotionDef p = potion(s.potions.remove(potionIndex));
        if (p == null) {
            return;
        }
        Enemy target = livingEnemies(s).isEmpty() ? null : livingEnemies(s).get(0);
        if (enemyIndex >= 0 && enemyIndex < s.enemies.size() && s.enemies.get(enemyIndex).hp > 0) {
            target = s.enemies.get(enemyIndex);
        }
        if ("blood".equals(p.id)) {
            s.hp = Math.min(s.maxHp, s.hp + 18);
        } else if ("fire".equals(p.id) && target != null) {
            damageEnemy(s, target, 24, false);
        } else if ("guard".equals(p.id)) {
            gainBlock(s, 18);
        } else if ("draw".equals(p.id)) {
            draw(s, 3);
            s.energy++;
        } else if ("bind".equals(p.id) && target != null) {
            target.bind += 2;
            target.vulnerable += 2;
        } else if ("mirror".equals(p.id)) {
            if (!s.discard.isEmpty()) {
                Card copy = copyCard(s.discard.get(s.discard.size() - 1));
                copy.temp = true;
                addToHand(s, copy);
            }
        } else if ("temper".equals(p.id)) {
            int upgraded = 0;
            for (Card h : s.hand) {
                CardDef d = card(h.id);
                if (d != null && d.type != 3 && !h.upgraded) {
                    h.upgraded = true;
                    upgraded++;
                    if (upgraded >= 2) {
                        break;
                    }
                }
            }
        } else if ("surge".equals(p.id)) {
            s.energy += 3;
        } else if ("ember".equals(p.id)) {
            for (Enemy e : livingEnemies(s)) {
                e.burn += 5 + s.burnPower;
            }
        } else if ("root".equals(p.id)) {
            for (Enemy e : livingEnemies(s)) {
                e.bind += 4 + s.bindPower;
            }
            gainBlock(s, 8);
        } else if ("exhaust_draw".equals(p.id)) {
            if (!s.discard.isEmpty()) {
                s.exhaust.add(s.discard.remove(s.discard.size() - 1));
            }
            draw(s, 4);
        } else if ("coin".equals(p.id) && target != null) {
            damageEnemy(s, target, 12 + Math.min(30, s.gold / 10), false);
            s.gold += 20;
        }
        if (PROF_ALCHEMIST.equals(s.profession)) {
            draw(s, 1);
            s.energy += s.professionUsedThisTurn == 0 ? 1 : 0;
            s.professionUsedThisTurn++;
            if (target != null) {
                target.burn += 2 + s.burnPower;
                target.bind += 2 + s.bindPower;
            }
        }
        if (hasRelic(s, "alchemist_case") && target != null) {
            target.vulnerable += 1;
        }
        if (hasRelic(s, "glass_vials")) {
            s.energy++;
            if (target != null) {
                target.burn += 1 + s.burnPower;
                target.bind += 1 + s.bindPower;
            }
        }
        if (hasTalent(s, "t_alchemist_plague") && target != null) {
            target.burn += 2 + s.burnPower;
            target.bind += 2 + s.bindPower;
        }
        log(s, "使用药剂：" + p.name);
        if (checkPlayerDefeated(s)) {
            return;
        }
        if (allEnemiesDead(s)) {
            winCombat(s);
        }
    }

    public static void shopBuyCard(State s, int index) {
        if (s.mode != MODE_SHOP || index < 0 || index >= s.shopCards.size()) {
            return;
        }
        CardDef d = card(s.shopCards.get(index));
        int price = shopCardPrice(s, d);
        if (d == null || s.gold < price) {
            return;
        }
        s.gold -= price;
        s.deck.add(new Card(d.id));
        s.seenCards.add(d.id);
        s.shopCards.remove(index);
        log(s, "购买卡牌：" + d.name);
    }

    public static void shopBuyRelic(State s, int index) {
        if (s.mode != MODE_SHOP || index < 0 || index >= s.shopRelics.size()) {
            return;
        }
        RelicDef r = relic(s.shopRelics.get(index));
        int price = shopRelicPrice(s);
        if (r == null || s.gold < price) {
            return;
        }
        s.gold -= price;
        addRelic(s, r.id);
        s.shopRelics.remove(index);
    }

    public static void shopBuyPotion(State s, int index) {
        if (s.mode != MODE_SHOP || index < 0 || index >= s.shopPotions.size() || s.potions.size() >= potionLimit(s)) {
            return;
        }
        PotionDef p = potion(s.shopPotions.get(index));
        int price = shopPotionPrice(s);
        if (p == null || s.gold < price) {
            return;
        }
        s.gold -= price;
        s.potions.add(p.id);
        s.shopPotions.remove(index);
        log(s, "购买药剂：" + p.name);
    }

    public static void shopChoose(State s, String action) {
        if (s.mode != MODE_SHOP) {
            return;
        }
        s.pendingAction = action;
        openDeck(s, MODE_SHOP);
    }

    public static void deckPick(State s, int deckIndex) {
        if (s.mode != MODE_DECK || deckIndex < 0 || deckIndex >= s.deck.size()) {
            return;
        }
        String action = s.pendingAction == null ? "" : s.pendingAction;
        if (action.length() == 0) {
            return;
        }
        if ("rest_upgrade".equals(action)) {
            Card c = s.deck.get(deckIndex);
            if (!c.upgraded) {
                c.upgraded = true;
                log(s, "锻造：" + card(c.id).name);
                s.pendingAction = "";
                s.mode = MODE_REST;
            }
        } else if ("rest_remove".equals(action)) {
            Card c = s.deck.remove(deckIndex);
            log(s, "净化：" + card(c.id).name);
            s.pendingAction = "";
            s.mode = MODE_REST;
        } else if ("rest_transform".equals(action)) {
            Card old = s.deck.get(deckIndex);
            CardDef d = randomCard(s, s.origin, false);
            old.id = d.id;
            old.upgraded = false;
            log(s, "转化为：" + d.name);
            s.pendingAction = "";
            s.mode = MODE_REST;
        } else if ("shop_remove".equals(action) && s.gold >= shopServicePrice(s, action)) {
            Card c = s.deck.remove(deckIndex);
            s.gold -= shopServicePrice(s, action);
            log(s, "商店移除：" + card(c.id).name);
            s.pendingAction = "";
            s.mode = MODE_SHOP;
        } else if ("shop_upgrade".equals(action) && s.gold >= shopServicePrice(s, action)) {
            Card c = s.deck.get(deckIndex);
            if (!c.upgraded) {
                c.upgraded = true;
                s.gold -= shopServicePrice(s, action);
                log(s, "商店升级：" + card(c.id).name);
                s.pendingAction = "";
                s.mode = MODE_SHOP;
            }
        } else if ("shop_transform".equals(action) && s.gold >= shopServicePrice(s, action)) {
            Card old = s.deck.get(deckIndex);
            CardDef d = randomCard(s, s.origin, false);
            old.id = d.id;
            old.upgraded = false;
            s.gold -= shopServicePrice(s, action);
            log(s, "商店转化为：" + d.name);
            s.pendingAction = "";
            s.mode = MODE_SHOP;
        } else if ("event_upgrade_wound".equals(action)) {
            Card c = s.deck.get(deckIndex);
            if (!c.upgraded) {
                c.upgraded = true;
                addStatusCard(s, "wound");
                log(s, "石刻升级：" + card(c.id).name + "，裂伤随之入牌组。");
                s.pendingAction = "";
                s.mode = MODE_MAP;
            }
        } else if ("event_remove_hp".equals(action)) {
            Card c = s.deck.remove(deckIndex);
            s.hp = Math.max(1, s.hp - 10);
            log(s, "影市吞走：" + card(c.id).name + "，你失去10生命。");
            s.pendingAction = "";
            s.mode = MODE_MAP;
        }
    }

    public static void leaveShop(State s) {
        if (s.mode == MODE_SHOP) {
            s.mode = MODE_MAP;
        }
    }

    public static void restHeal(State s) {
        if (s.mode != MODE_REST) {
            return;
        }
        int amount = Math.max(18, s.maxHp / 3);
        s.hp = Math.min(s.maxHp, s.hp + amount);
        log(s, "营火恢复 " + amount + " 点生命。");
        s.mode = MODE_MAP;
    }

    public static void restChoose(State s, String action) {
        if (s.mode != MODE_REST) {
            return;
        }
        s.pendingAction = action;
        openDeck(s, MODE_REST);
    }

    public static void eventChoose(State s, int choice) {
        if (s.mode != MODE_EVENT) {
            return;
        }
        int e = s.eventId;
        if (e == 0) {
            if (choice == 0) {
                s.hp = Math.max(1, s.hp - 8);
                addRelic(s, randomRelic(s).id);
                log(s, "你献出鲜血，换来沉默的遗物。");
            } else {
                s.deck.add(new Card("void_echo"));
                log(s, "一张回声卡牌落入牌组。");
            }
        } else if (e == 1) {
            if (choice == 0) {
                s.gold += 95;
                addStatusCard(s, "wound");
                addStatusCard(s, "wound");
                log(s, "拿走金币，也背上裂伤。");
            } else {
                s.hp = Math.min(s.maxHp, s.hp + 16);
            }
        } else if (e == 2) {
            if (choice == 0) {
                CardDef d = randomCard(s, s.origin, true);
                Card c = new Card(d.id);
                c.upgraded = true;
                s.deck.add(c);
                log(s, "获得升级卡牌：" + d.name);
            } else {
                s.maxHp += 6;
                s.hp += 6;
            }
        } else if (e == 3) {
            if (choice == 0 && s.gold >= 45) {
                s.gold -= 45;
                s.maxHp += 5;
                s.hp += 5;
            } else {
                s.gold += 30;
            }
        } else if (e == 4) {
            if (choice == 0 && hasUpgradableDeckCard(s)) {
                s.pendingAction = "event_upgrade_wound";
                openDeck(s, MODE_EVENT);
                return;
            } else {
                addStatusCard(s, "daze");
                addStatusCard(s, "daze");
                s.gold += 65;
                log(s, "你拿走石匣中的金币，眩光随之扩散。");
            }
        } else if (e == 5) {
            if (choice == 0 && s.potions.size() < potionLimit(s)) {
                PotionDef p = POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size()));
                s.potions.add(p.id);
                s.hp = Math.max(1, s.hp - 6);
                log(s, "你饮下试剂，获得药剂：" + p.name);
            } else {
                s.gold += 45;
            }
        } else if (e == 6) {
            if (choice == 0 && !s.deck.isEmpty()) {
                s.pendingAction = "event_remove_hp";
                openDeck(s, MODE_EVENT);
                return;
            } else {
                CardDef d = randomCard(s, s.origin, true);
                s.deck.add(new Card(d.id));
                s.gold += 35;
                log(s, "影市赠予：" + d.name + "，外加35金币。");
            }
        } else {
            if (choice == 0) {
                addRelic(s, randomBossRelic(s, Collections.<String>emptySet()).id);
                s.maxHp = Math.max(30, s.maxHp - 8);
                s.hp = Math.min(s.hp, s.maxHp);
                log(s, "你提前取得一件高阶遗物，最大生命被削去。");
            } else {
                upgradeRandomDeckCard(s);
                upgradeRandomDeckCard(s);
                addStatusCard(s, "daze");
                log(s, "两张牌被雾中力量重铸。");
            }
        }
        s.mode = MODE_MAP;
    }

    public static void nextAct(State s) {
        if (s.act >= 3) {
            s.mode = MODE_VICTORY;
            finishRun(s, true);
            return;
        }
        s.act++;
        s.floor = 0;
        generateMap(s);
        s.mode = MODE_MAP;
    }

    public static CardDef card(String id) {
        for (CardDef c : CARD_LIBRARY) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }

    public static RelicDef relic(String id) {
        for (RelicDef r : RELIC_LIBRARY) {
            if (r.id.equals(id)) {
                return r;
            }
        }
        return null;
    }

    public static PotionDef potion(String id) {
        for (PotionDef p : POTION_LIBRARY) {
            if (p.id.equals(id)) {
                return p;
            }
        }
        return null;
    }

    public static BoonDef boon(String id) {
        for (BoonDef b : BOON_LIBRARY) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        return null;
    }

    public static TalentDef talent(String id) {
        for (TalentDef t : TALENT_LIBRARY) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return null;
    }

    public static int costOf(State s, Card c, CardDef d) {
        int cost = c.upgraded ? Math.max(0, d.cost - d.upgradeCostDrop) : d.cost;
        if (PROF_BLOODBOUND.equals(s.profession) && "wound".equals(c.id)) {
            cost = 1;
        }
        if (hasRelic(s, "amber_quill") && s.cardsPlayedThisTurn == 0) {
            cost = Math.max(0, cost - 1);
        }
        return cost;
    }

    public static String cardText(Card c) {
        CardDef d = card(c.id);
        if (d == null) {
            return "";
        }
        return c.upgraded ? d.upText : d.text;
    }

    public static int cardDamage(Card c) {
        CardDef d = card(c.id);
        return d == null ? 0 : (c.upgraded ? d.damageUp : d.damage);
    }

    public static int cardBlock(Card c) {
        CardDef d = card(c.id);
        return d == null ? 0 : (c.upgraded ? d.blockUp : d.block);
    }

    public static int cardPrice(CardDef d) {
        if (d == null) {
            return 0;
        }
        if (d.rarity == 2) {
            return 118;
        }
        if (d.rarity == 1) {
            return 76;
        }
        return 48;
    }

    public static int shopCardPrice(State s, CardDef d) {
        int price = cardPrice(d);
        if (PROF_MERCHANT.equals(s.profession)) {
            price -= 12;
        }
        if (hasRelic(s, "merchant_scale")) {
            price -= 10;
        }
        if (hasTalent(s, "t_merchant_interest")) {
            price -= 6;
        }
        return Math.max(18, price);
    }

    public static int shopRelicPrice(State s) {
        int price = 165;
        if (PROF_MERCHANT.equals(s.profession)) {
            price -= 22;
        }
        if (hasRelic(s, "merchant_scale")) {
            price -= 18;
        }
        if (hasTalent(s, "t_merchant_interest")) {
            price -= 10;
        }
        return Math.max(90, price);
    }

    public static int shopPotionPrice(State s) {
        int price = 42;
        if (PROF_MERCHANT.equals(s.profession)) {
            price -= 7;
        }
        if (hasRelic(s, "merchant_scale")) {
            price -= 5;
        }
        if (hasTalent(s, "t_merchant_interest")) {
            price -= 3;
        }
        return Math.max(18, price);
    }

    public static int shopServicePrice(State s, String action) {
        int price = "shop_remove".equals(action) ? 85 : "shop_upgrade".equals(action) ? 75 : 105;
        if (PROF_MERCHANT.equals(s.profession)) {
            price -= 12;
        }
        if (hasRelic(s, "merchant_scale")) {
            price -= 8;
        }
        if (hasTalent(s, "t_merchant_interest")) {
            price -= 5;
        }
        return Math.max(35, price);
    }

    public static int potionLimit(State s) {
        int limit = 3;
        if (PROF_ALCHEMIST.equals(s.profession)) {
            limit++;
        }
        if (hasRelic(s, "alchemist_case")) {
            limit++;
        }
        return limit;
    }

    public static String nodeName(char type) {
        if (type == 'C') {
            return "战";
        }
        if (type == 'E') {
            return "精";
        }
        if (type == 'B') {
            return "王";
        }
        if (type == '$') {
            return "店";
        }
        if (type == 'R') {
            return "息";
        }
        return "事";
    }

    public static String modifierName(int modifier) {
        if (modifier == MOD_ARMORED) {
            return "坚甲";
        }
        if (modifier == MOD_FRENZY) {
            return "狂热";
        }
        if (modifier == MOD_POLLUTED) {
            return "污染";
        }
        if (modifier == MOD_BOUNTY) {
            return "悬赏";
        }
        if (modifier == MOD_TURBULENT) {
            return "紊流";
        }
        return "无";
    }

    public static String modifierText(int modifier) {
        if (modifier == MOD_ARMORED) {
            return "敌人开局带护甲，奖励金币提高。";
        }
        if (modifier == MOD_FRENZY) {
            return "敌人力量逐回合上涨，稀有牌权重提高。";
        }
        if (modifier == MOD_POLLUTED) {
            return "牌堆会被状态牌干扰，胜利后有额外卡牌选择。";
        }
        if (modifier == MOD_BOUNTY) {
            return "敌人更厚，但金币和遗物概率提高。";
        }
        if (modifier == MOD_TURBULENT) {
            return "奇数回合能量+1，偶数回合额外抽牌。";
        }
        return "标准遭遇。";
    }

    public static String questName(int quest) {
        if (quest == QUEST_SWIFT) return "速战";
        if (quest == QUEST_UNHURT) return "无创";
        if (quest == QUEST_COMBO) return "连携";
        if (quest == QUEST_GUARD) return "铁壁";
        if (quest == QUEST_HEX) return "控场";
        if (quest == QUEST_LEAN) return "精算";
        return "无";
    }

    public static String questText(State s) {
        if (s == null || s.combatQuest == QUEST_NONE) return "无额外目标。";
        if (s.combatQuest == QUEST_SWIFT) return "在" + s.questTarget + "回合内获胜。";
        if (s.combatQuest == QUEST_UNHURT) return "本场累计受伤不超过" + s.questTarget + "。";
        if (s.combatQuest == QUEST_COMBO) return "单回合打出至少" + s.questTarget + "张牌。";
        if (s.combatQuest == QUEST_GUARD) return "单回合格挡达到" + s.questTarget + "。";
        if (s.combatQuest == QUEST_HEX) return "让任一敌人同时拥有燃/缚/易合计" + s.questTarget + "层。";
        if (s.combatQuest == QUEST_LEAN) return "胜利时总出牌不超过" + s.questTarget + "张。";
        return "完成特殊目标。";
    }

    public static String questRewardText(State s) {
        if (s == null || s.combatQuest == QUEST_NONE) return "";
        return "奖励：金币、奖励质量和少量长期记录。";
    }

    public static String enemyMechanicText(Enemy e) {
        if (e == null) {
            return "";
        }
        String text = "";
        if (e.phase > 1) text += "相" + e.phase + " ";
        if (e.enraged) text += "怒 ";
        if (e.thorns > 0) text += "刺" + e.thorns + " ";
        if (e.shieldPulse > 0) text += "雾" + e.shieldPulse + " ";
        if (e.doom > 0) text += "厄" + e.doom + " ";
        if (e.mark > 0) text += "印" + e.mark + " ";
        if (e.stolenGold > 0) text += "赃" + e.stolenGold + " ";
        return text.trim();
    }

    public static int originColor(String origin) {
        if (ORIGIN_STEEL.equals(origin)) {
            return 0xff9fb0ba;
        }
        if (ORIGIN_ASH.equals(origin)) {
            return 0xfff06d3a;
        }
        if (ORIGIN_WILD.equals(origin)) {
            return 0xff74c66a;
        }
        if (ORIGIN_VOID.equals(origin)) {
            return 0xff9b7cff;
        }
        return 0xffd6c07a;
    }

    public static String professionText(String profession) {
        if (PROF_WARDEN.equals(profession)) {
            return "高生命与格挡节奏，技能牌会触发额外护甲。适合钢律、森息的稳健构筑。";
        }
        if (PROF_DUELIST.equals(profession)) {
            return "连打越多越锋利，每三张攻击触发穿透追击。适合低费、抽牌和回声套牌。";
        }
        if (PROF_ALCHEMIST.equals(profession)) {
            return "药剂上限提高，使用药剂会抽牌并扩散异常。适合燃灼、束缚和资源爆发。";
        }
        if (PROF_RANGER.equals(profession)) {
            return "攻击会追加束缚，对受束缚敌人更高效。适合控制、群攻和多敌路线。";
        }
        if (PROF_ARCANIST.equals(profession)) {
            return "消耗与临时牌会返还资源，首回合牌序更稳定。适合虚空、过牌和高技巧构筑。";
        }
        if (PROF_MERCHANT.equals(profession)) {
            return "金币更多，商店更便宜，金币牌能把经济转成伤害或防御。适合长线运营。";
        }
        if (PROF_BLOODBOUND.equals(profession)) {
            return "把生命与裂伤转成爆发，受伤后获得反击窗口。适合高风险压血、治疗和状态牌构筑。";
        }
        if (PROF_WEAVER.equals(profession)) {
            return "预视牌序并升级手牌，第三张技能牌会重织资源。适合抽牌、临时牌和精密循环。";
        }
        return "尚未选择职业。";
    }

    public static int professionColor(String profession) {
        if (PROF_WARDEN.equals(profession)) {
            return 0xffa8c2d0;
        }
        if (PROF_DUELIST.equals(profession)) {
            return 0xfff19968;
        }
        if (PROF_ALCHEMIST.equals(profession)) {
            return 0xff83d39b;
        }
        if (PROF_RANGER.equals(profession)) {
            return 0xffd4b85f;
        }
        if (PROF_ARCANIST.equals(profession)) {
            return 0xffb095ff;
        }
        if (PROF_MERCHANT.equals(profession)) {
            return 0xffffcf66;
        }
        if (PROF_BLOODBOUND.equals(profession)) {
            return 0xffd66b7a;
        }
        if (PROF_WEAVER.equals(profession)) {
            return 0xff7ed1d6;
        }
        return 0xffd6c07a;
    }

    public static boolean isProfession(String profession) {
        for (String p : PROFESSIONS) {
            if (p.equals(profession)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasTalent(State s, String id) {
        return s.talents.contains(id);
    }

    public static boolean hasAchievement(State s, String id) {
        return s.meta != null && s.meta.achievements.contains(id);
    }

    public static String achievementName(String id) {
        if ("first_run".equals(id)) return "初入深渊";
        if ("first_win".equals(id)) return "抵达无光尽头";
        if ("all_professions".equals(id)) return PROFESSIONS.length + "职巡礼";
        if ("collector".equals(id)) return "旧物收藏家";
        if ("high_depth".equals(id)) return "无光行者";
        if ("talent_master".equals(id)) return "专精大师";
        if ("rich".equals(id)) return "裂币盈囊";
        if ("quest_hunter".equals(id)) return "悬赏老手";
        return id;
    }

    private static void resetRun(State s) {
        long seed = System.currentTimeMillis();
        s.rngSeed = seed;
        s.run = new Random(seed);
        s.mode = MODE_TITLE;
        s.previousMode = 0;
        s.pendingAction = "";
        s.deckView = 0;
        s.combatKind = 'C';
        s.encounterModifier = MOD_NONE;
        s.combatQuest = QUEST_NONE;
        s.questTarget = 0;
        s.questProgress = 0;
        s.questComplete = false;
        s.currentNode = -1;
        s.runFinished = false;
        s.lastRunSummary = "";
        s.newAchievements.clear();
        s.log.clear();
        s.cardRewards.clear();
        s.cardRewardSkipped = false;
        s.relicRewards.clear();
        s.boonChoices.clear();
        s.talentChoices.clear();
        s.shopCards.clear();
        s.shopRelics.clear();
        s.shopPotions.clear();
        s.map.clear();
        s.hand.clear();
        s.draw.clear();
        s.discard.clear();
        s.exhaust.clear();
        s.enemies.clear();
    }

    private static void addStarterDeck(State s, String origin) {
        for (int i = 0; i < 5; i++) {
            s.deck.add(new Card("strike"));
        }
        for (int i = 0; i < 4; i++) {
            s.deck.add(new Card("guard"));
        }
        if (ORIGIN_STEEL.equals(origin)) {
            s.deck.add(new Card("steel_counter"));
            s.deck.add(new Card("steel_wall"));
        } else if (ORIGIN_ASH.equals(origin)) {
            s.deck.add(new Card("ash_spark"));
            s.deck.add(new Card("ash_overload"));
        } else if (ORIGIN_WILD.equals(origin)) {
            s.deck.add(new Card("wild_root"));
            s.deck.add(new Card("wild_growth"));
        } else {
            s.deck.add(new Card("void_draw"));
            s.deck.add(new Card("void_echo"));
        }
    }

    private static void addProfessionStarter(State s, String profession) {
        if (PROF_WARDEN.equals(profession)) {
            s.maxHp += 8;
            s.hp += 8;
            s.deck.add(new Card("warden_oath"));
        } else if (PROF_DUELIST.equals(profession)) {
            s.maxHp = Math.max(40, s.maxHp - 4);
            s.hp = Math.min(s.hp, s.maxHp);
            s.deck.add(new Card("duelist_flurry"));
            s.deck.add(new Card("quick_cut"));
        } else if (PROF_ALCHEMIST.equals(profession)) {
            s.deck.add(new Card("alchemist_mix"));
            while (s.potions.size() < 2) {
                s.potions.add(POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size())).id);
            }
        } else if (PROF_RANGER.equals(profession)) {
            s.maxHp += 2;
            s.hp += 2;
            s.deck.add(new Card("ranger_trap"));
            s.gold += 15;
        } else if (PROF_ARCANIST.equals(profession)) {
            s.deck.add(new Card("arcanist_glyph"));
            s.deck.add(new Card("void_glimpse"));
        } else if (PROF_MERCHANT.equals(profession)) {
            s.maxHp = Math.max(40, s.maxHp - 6);
            s.hp = Math.min(s.hp, s.maxHp);
            s.gold += 70;
            s.deck.add(new Card("merchant_haggle"));
        } else if (PROF_BLOODBOUND.equals(profession)) {
            s.maxHp += 6;
            s.hp += 6;
            s.deck.add(new Card("blood_pact"));
            s.deck.add(new Card("wound"));
        } else if (PROF_WEAVER.equals(profession)) {
            s.deck.add(new Card("weaver_thread"));
            s.deck.add(new Card("void_glimpse"));
            upgradeRandomDeckCard(s);
        }
    }

    private static void applyTalentPickup(State s, String id) {
        if ("t_shared_masterwork".equals(id)) {
            upgradeRandomDeckCard(s);
            upgradeRandomDeckCard(s);
        } else if ("t_shared_hunter".equals(id)) {
            s.gold += 60;
        } else if ("t_shared_longnight".equals(id)) {
            s.maxHp += 6;
            s.hp += 6;
        } else if ("t_warden_bastion".equals(id)) {
            s.maxHp += 8;
            s.hp += 8;
        } else if ("t_warden_counter".equals(id)) {
            Card c = new Card("warden_slam");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_duelist_tempo".equals(id)) {
            Card c = new Card("duelist_flurry");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_duelist_execution".equals(id)) {
            Card c = new Card("duelist_finish");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_alchemist_reserve".equals(id)) {
            while (s.potions.size() < Math.min(potionLimit(s), 3)) {
                s.potions.add(POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size())).id);
            }
        } else if ("t_alchemist_plague".equals(id)) {
            Card c = new Card("alchemist_cloud");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_ranger_quarry".equals(id)) {
            Card c = new Card("ranger_volley");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_ranger_net".equals(id)) {
            Card c = new Card("ranger_trap");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_arcanist_rewrite".equals(id)) {
            Card c = new Card("arcanist_glyph");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_arcanist_overflow".equals(id)) {
            Card c = new Card("arcanist_loop");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_merchant_interest".equals(id)) {
            s.gold += 100;
        } else if ("t_merchant_contract".equals(id)) {
            s.gold += 120;
            Card c = new Card("merchant_liquidate");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_bloodbound_scar".equals(id)) {
            s.maxHp += 10;
            s.hp += 10;
            Card c = new Card("blood_rite");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_bloodbound_feast".equals(id)) {
            Card c = new Card("blood_feast");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_weaver_setup".equals(id)) {
            Card c = new Card("weaver_lattice");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_weaver_mastery".equals(id)) {
            upgradeRandomDeckCard(s);
            upgradeRandomDeckCard(s);
            Card c = new Card("weaver_pattern");
            c.upgraded = true;
            s.deck.add(c);
        }
    }

    private static void rollBoons(State s) {
        s.boonChoices.clear();
        HashSet<String> offered = new HashSet<>();
        for (int i = 0; i < 3 && i < BOON_LIBRARY.size(); i++) {
            BoonDef b;
            do {
                b = BOON_LIBRARY.get(s.run.nextInt(BOON_LIBRARY.size()));
            } while (offered.contains(b.id));
            offered.add(b.id);
            s.boonChoices.add(b.id);
        }
    }

    private static void removeStarterJunk(State s) {
        for (int i = 0; i < s.deck.size(); i++) {
            String id = s.deck.get(i).id;
            if ("strike".equals(id) || "guard".equals(id)) {
                s.deck.remove(i);
                return;
            }
        }
        if (!s.deck.isEmpty()) {
            s.deck.remove(0);
        }
    }

    private static String starterRelic(String origin) {
        if (ORIGIN_STEEL.equals(origin)) {
            return "steel_oath";
        }
        if (ORIGIN_ASH.equals(origin)) {
            return "ember_core";
        }
        if (ORIGIN_WILD.equals(origin)) {
            return "leaf_charm";
        }
        return "void_lens";
    }

    private static void generateMap(State s) {
        s.map.clear();
        int lanes = 5;
        int floors = 12;
        for (int f = 1; f <= floors; f++) {
            for (int lane = 0; lane < lanes; lane++) {
                MapNode n = new MapNode();
                n.floor = f;
                n.lane = lane;
                if (f == floors) {
                    n.type = 'B';
                } else if (f == 6) {
                    n.type = 'R';
                } else if (f == 3 || f == 8) {
                    n.type = s.run.nextBoolean() ? '?' : '$';
                } else {
                    int roll = s.run.nextInt(100);
                    if (roll < 58) {
                        n.type = 'C';
                    } else if (roll < 72) {
                        n.type = 'E';
                    } else if (roll < 84) {
                        n.type = '?';
                    } else if (roll < 92) {
                        n.type = '$';
                    } else {
                        n.type = 'R';
                    }
                }
                n.available = f == 1;
                s.map.add(n);
            }
        }
        for (int f = 1; f < floors; f++) {
            for (int lane = 0; lane < lanes; lane++) {
                MapNode n = s.map.get((f - 1) * lanes + lane);
                int a = Math.max(0, lane - 1 + s.run.nextInt(2));
                int b = Math.min(lanes - 1, lane + s.run.nextInt(2));
                addEdge(n, f * lanes + a);
                addEdge(n, f * lanes + b);
                if (s.run.nextInt(100) < 35) {
                    int c = Math.max(0, Math.min(lanes - 1, lane - 1 + s.run.nextInt(3)));
                    addEdge(n, f * lanes + c);
                }
            }
        }
    }

    private static void addEdge(MapNode n, int target) {
        if (!n.next.contains(target)) {
            n.next.add(target);
        }
    }

    private static void startCombat(State s, char kind) {
        s.mode = MODE_COMBAT;
        s.combatKind = kind;
        s.encounterModifier = chooseEncounterModifier(s, kind);
        s.turn = 0;
        s.energy = 3;
        s.block = 0;
        s.burnPower = 0;
        s.bindPower = 0;
        s.steelEngine = 0;
        s.ashEngine = 0;
        s.wildEngine = 0;
        s.voidEngine = 0;
        s.vulnerable = 0;
        s.nextEnergyPenalty = 0;
        s.professionCharge = 0;
        s.professionUsedThisTurn = 0;
        s.combatQuest = chooseCombatQuest(s, kind);
        s.questTarget = questTargetFor(s, s.combatQuest, kind);
        s.questProgress = 0;
        s.questComplete = false;
        s.playerTurn = true;
        s.cardsPlayedThisTurn = 0;
        s.totalCardsPlayed = 0;
        s.hand.clear();
        s.draw.clear();
        s.discard.clear();
        s.exhaust.clear();
        s.enemies.clear();
        for (Card c : s.deck) {
            s.draw.add(copyCard(c));
        }
        Collections.shuffle(s.draw, s.run);
        spawnEnemies(s, kind);
        applyEncounterStart(s);
        applyEnemyStartMechanics(s);
        if (hasRelic(s, "bone_mask")) {
            gainBlock(s, 8);
        }
        if (hasRelic(s, "storm_shell") && s.enemies.size() > 1) {
            gainBlock(s, 5 + s.act * 2);
        }
        if (hasTalent(s, "t_alchemist_reserve") && s.potions.size() < potionLimit(s)) {
            PotionDef p = POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size()));
            s.potions.add(p.id);
            log(s, "备用试剂补充：" + p.name);
        }
        beginPlayerTurn(s);
        if (hasTalent(s, "t_arcanist_rewrite")) {
            Card glyph = new Card("arcanist_glyph");
            glyph.temp = true;
            addToHand(s, glyph);
        }
        log(s, (kind == 'B' ? "深渊领主现身。" : kind == 'E' ? "精英挡住去路。" : "遭遇敌群。") + " 词缀：" + modifierName(s.encounterModifier));
        if (s.combatQuest != QUEST_NONE) {
            log(s, "战斗目标：" + questName(s.combatQuest) + " - " + questText(s));
        }
    }

    private static int chooseCombatQuest(State s, char kind) {
        if (kind == 'B') {
            int[] quests = {QUEST_SWIFT, QUEST_UNHURT, QUEST_COMBO, QUEST_GUARD, QUEST_HEX};
            return quests[s.run.nextInt(quests.length)];
        }
        if (kind == 'E') {
            int[] quests = {QUEST_SWIFT, QUEST_UNHURT, QUEST_COMBO, QUEST_GUARD, QUEST_HEX, QUEST_LEAN};
            return quests[s.run.nextInt(quests.length)];
        }
        if (s.run.nextInt(100) < 82) {
            int[] quests = {QUEST_SWIFT, QUEST_UNHURT, QUEST_COMBO, QUEST_GUARD, QUEST_HEX, QUEST_LEAN};
            return quests[s.run.nextInt(quests.length)];
        }
        return QUEST_NONE;
    }

    private static int questTargetFor(State s, int quest, char kind) {
        int boss = kind == 'B' ? 1 : 0;
        int elite = kind == 'E' ? 1 : 0;
        if (quest == QUEST_SWIFT) return 5 + boss + elite;
        if (quest == QUEST_UNHURT) return 8 + s.act * 3 + boss * 8 + elite * 4;
        if (quest == QUEST_COMBO) return 5 + Math.min(2, s.act / 2);
        if (quest == QUEST_GUARD) return 22 + s.act * 6 + boss * 12 + elite * 6;
        if (quest == QUEST_HEX) return 8 + s.act * 3 + boss * 6 + elite * 3;
        if (quest == QUEST_LEAN) return 11 + boss * 7 + elite * 3;
        return 0;
    }

    private static int chooseEncounterModifier(State s, char kind) {
        if (kind == 'B') {
            int[] bossMods = {MOD_ARMORED, MOD_FRENZY, MOD_BOUNTY, MOD_TURBULENT};
            return bossMods[s.run.nextInt(bossMods.length)];
        }
        if (kind == 'E') {
            int[] eliteMods = {MOD_ARMORED, MOD_FRENZY, MOD_POLLUTED, MOD_BOUNTY, MOD_TURBULENT};
            return eliteMods[s.run.nextInt(eliteMods.length)];
        }
        if (s.run.nextInt(100) < 72) {
            int[] mods = {MOD_ARMORED, MOD_FRENZY, MOD_POLLUTED, MOD_BOUNTY, MOD_TURBULENT};
            return mods[s.run.nextInt(mods.length)];
        }
        return MOD_NONE;
    }

    private static void applyEncounterStart(State s) {
        if (s.encounterModifier == MOD_ARMORED) {
            for (Enemy e : s.enemies) {
                e.block += 8 + s.act * 3;
            }
        } else if (s.encounterModifier == MOD_FRENZY) {
            for (Enemy e : s.enemies) {
                e.strength += 1 + s.act / 2;
            }
        } else if (s.encounterModifier == MOD_POLLUTED) {
            s.discard.add(new Card("daze"));
            if (s.act >= 2) {
                s.discard.add(new Card("wound"));
            }
        } else if (s.encounterModifier == MOD_BOUNTY) {
            for (Enemy e : s.enemies) {
                int extra = Math.max(4, e.maxHp / 8);
                e.maxHp += extra;
                e.hp += extra;
            }
        }
    }

    private static void applyEnemyStartMechanics(State s) {
        for (Enemy e : s.enemies) {
            if (e.kind == 10 || e.kind == 11 || e.kind == 12) {
                e.phase = Math.max(1, e.phase);
            }
            if (e.kind == 25 && e.thorns == 0) {
                e.thorns = 2 + Math.min(3, s.act);
            } else if (e.kind == 24 && e.doom == 0) {
                e.doom = 4;
            } else if (e.kind == 27 && e.shieldPulse == 0) {
                e.shieldPulse = 2;
            }
        }
    }

    private static void applyEnemyStartOfPlayerTurnMechanics(State s) {
        updateEnemyPhases(s);
        if (s.turn <= 1) {
            return;
        }
        ArrayList<Enemy> acting = new ArrayList<>(s.enemies);
        for (Enemy e : acting) {
            if (e.hp <= 0) {
                continue;
            }
            if (e.doom > 0) {
                e.doom--;
                if (e.doom <= 0) {
                    addStatusCard(s, s.act >= 3 ? "wound" : "daze");
                    s.vulnerable += 1;
                    e.doom = Math.max(3, 5 - s.act);
                    log(s, e.name + " 的蚀骨仪式污染牌堆。");
                }
            }
            if (e.shieldPulse > 0) {
                e.shieldPulse--;
                if (e.shieldPulse <= 0) {
                    int guard = 5 + s.act * 2;
                    for (Enemy other : livingEnemies(s)) {
                        other.block += guard;
                    }
                    e.shieldPulse = 2;
                    log(s, e.name + " 展开雾盾，敌群获得护甲。");
                }
            }
        }
    }

    private static void updateEnemyPhases(State s) {
        ArrayList<Enemy> copy = new ArrayList<>(s.enemies);
        for (Enemy e : copy) {
            if (e.hp <= 0 || e.phase >= 2 || e.maxHp <= 0 || e.hp > e.maxHp / 2) {
                continue;
            }
            if (e.kind == 10) {
                e.phase = 2;
                e.enraged = true;
                e.strength += 2 + s.act;
                e.block += 16 + s.act * 4;
                if (livingEnemies(s).size() < 4) {
                    Enemy shard = enemy("王冠裂影", 22 + s.act * 8 + s.ascension, 4);
                    shard.intent = ENEMY_DEBUFF;
                    shard.intentValue = 1;
                    s.enemies.add(shard);
                }
                log(s, e.name + " 进入二相，王冠裂开并召来影卫。");
            } else if (e.kind == 11) {
                e.phase = 2;
                e.enraged = true;
                e.strength += 2 + s.act;
                s.vulnerable += 1;
                addStatusCard(s, "wound");
                addStatusCard(s, "daze");
                for (Enemy other : livingEnemies(s)) {
                    other.burn += 2 + s.act;
                    other.strength += 1;
                }
                log(s, e.name + " 炉心破裂，战场升温并污染牌堆。");
            } else if (e.kind == 12) {
                e.phase = 2;
                e.enraged = true;
                e.strength += 3 + s.act;
                e.block += 14 + s.act * 3;
                if (!s.draw.isEmpty()) {
                    Card top = s.draw.remove(s.draw.size() - 1);
                    s.exhaust.add(top);
                }
                s.nextEnergyPenalty = Math.max(s.nextEnergyPenalty, 1);
                addStatusCard(s, "daze");
                log(s, e.name + " 展开终审二相，牌序被撕开。");
            }
        }
    }

    private static void spawnEnemies(State s, char kind) {
        int act = s.act;
        int depth = s.ascension;
        if (kind == 'B') {
            int bossType = Math.max(0, Math.min(2, act - 1));
            if (bossType == 0) {
                Enemy boss = enemy("渊冠守望", 92 + act * 30 + depth * 4, 10);
                boss.intent = ENEMY_ATTACK;
                boss.intentValue = 12 + act * 4;
                s.enemies.add(boss);
                Enemy shard = enemy("裂影", 28 + act * 8, 4);
                shard.intent = ENEMY_DEBUFF;
                shard.intentValue = 2;
                s.enemies.add(shard);
            } else if (bossType == 1) {
                Enemy boss = enemy("熔脉女王", 118 + act * 30 + depth * 4, 11);
                boss.intent = ENEMY_DEBUFF;
                boss.intentValue = 3;
                s.enemies.add(boss);
            } else {
                Enemy boss = enemy("空冠审判者", 136 + act * 32 + depth * 5, 12);
                boss.intent = ENEMY_SPECIAL;
                boss.intentValue = 2;
                s.enemies.add(boss);
                s.enemies.add(enemy("空冠副面", 34 + act * 8 + depth, 8));
            }
            return;
        }
        if (kind == 'E') {
            int elite = s.run.nextInt(6);
            if (elite == 0) {
                s.enemies.add(enemy("铁脊裁决者", 58 + act * 16 + depth * 2, 1));
            } else if (elite == 1) {
                s.enemies.add(enemy("双面猎手", 48 + act * 14 + depth * 2, 2));
                s.enemies.add(enemy("暗潮侍从", 28 + act * 8 + depth, 4));
            } else if (elite == 2) {
                s.enemies.add(enemy("烬鳞术士", 54 + act * 15 + depth * 2, 5));
                s.enemies.add(enemy("磷火虫", 26 + act * 8 + depth, 21));
            } else if (elite == 3) {
                s.enemies.add(enemy("根牢卫士", 64 + act * 17 + depth * 2, 7));
            } else if (elite == 4) {
                s.enemies.add(enemy("雾钟敲击者", 58 + act * 16 + depth * 2, 13));
                s.enemies.add(enemy("空面盗", 28 + act * 8 + depth, 23));
            } else {
                s.enemies.add(enemy("血契巡礼者", 66 + act * 18 + depth * 2, 14));
            }
            return;
        }
        int templateMax = act >= 3 ? 8 : act >= 2 ? 7 : 5;
        int template = s.run.nextInt(templateMax);
        int base = 18 + act * 8 + depth;
        if (template == 0) {
            s.enemies.add(enemy("锈刃徒", base + 18 + s.run.nextInt(9), 20));
        } else if (template == 1) {
            s.enemies.add(enemy("磷火虫", base + 5 + s.run.nextInt(7), 21));
            s.enemies.add(enemy("锈刃徒", base + 9 + s.run.nextInt(8), 20));
        } else if (template == 2) {
            s.enemies.add(enemy("藤壳兽", base + 16 + s.run.nextInt(9), 22));
            s.enemies.add(enemy("空面盗", base + 3 + s.run.nextInt(7), 23));
        } else if (template == 3) {
            s.enemies.add(enemy("琥珀哨兵", base + 24 + s.run.nextInt(10), 25));
        } else if (template == 4) {
            s.enemies.add(enemy("蚀骨祭司", base + 12 + s.run.nextInt(8), 24));
            if (act >= 2 || s.run.nextBoolean()) {
                s.enemies.add(enemy("磷火虫", base + 1 + s.run.nextInt(6), 21));
            }
        } else if (template == 5) {
            s.enemies.add(enemy("钩爪游民", base + 18 + s.run.nextInt(9), 26));
            s.enemies.add(enemy("藤壳兽", base + 10 + s.run.nextInt(7), 22));
        } else if (template == 6) {
            s.enemies.add(enemy("雾匠", base + 15 + s.run.nextInt(8), 27));
            s.enemies.add(enemy("空面盗", base + 7 + s.run.nextInt(8), 23));
        } else {
            s.enemies.add(enemy("锈刃徒", base + 6 + s.run.nextInt(7), 20));
            s.enemies.add(enemy("磷火虫", base + 2 + s.run.nextInt(6), 21));
            s.enemies.add(enemy("空面盗", base + 1 + s.run.nextInt(6), 23));
        }
    }

    private static Enemy enemy(String name, int hp) {
        return enemy(name, hp, 0);
    }

    private static Enemy enemy(String name, int hp, int enemyKind) {
        Enemy e = new Enemy();
        e.name = name;
        e.maxHp = hp;
        e.hp = hp;
        e.kind = enemyKind;
        e.intent = ENEMY_ATTACK;
        e.intentValue = 8;
        return e;
    }

    private static void beginPlayerTurn(State s) {
        s.playerTurn = true;
        s.turn++;
        s.block = 0;
        s.energy = 3;
        s.cardsPlayedThisTurn = 0;
        s.professionUsedThisTurn = 0;
        s.relicTriggersThisTurn = 0;
        if (hasRelic(s, "amber_quill") && s.turn == 1) {
            s.energy++;
        }
        if (hasRelic(s, "sapphire_cell")) {
            s.energy++;
        }
        if (hasRelic(s, "obsidian_core")) {
            s.energy++;
        }
        if (s.nextEnergyPenalty > 0) {
            s.energy = Math.max(0, s.energy - s.nextEnergyPenalty);
            log(s, "雾锁使本回合能量 -" + s.nextEnergyPenalty + "。");
            s.nextEnergyPenalty = 0;
        }
        if (hasRelic(s, "mirror_sun") && s.turn == 1) {
            s.energy += 2;
        }
        if (hasRelic(s, "deep_totem") && s.combatKind == 'B' && s.turn == 1) {
            s.energy++;
            gainBlock(s, 10);
        }
        if (hasTalent(s, "t_shared_longnight") && s.turn == 4) {
            s.energy++;
            draw(s, 1);
        }
        if (s.encounterModifier == MOD_TURBULENT && s.turn % 2 == 1) {
            s.energy++;
        }
        if (PROF_WARDEN.equals(s.profession)) {
            gainBlock(s, 2 + Math.min(6, s.act + s.steelEngine));
        }
        if (hasTalent(s, "t_warden_bastion") && s.turn == 1) {
            s.steelEngine++;
            gainBlock(s, 8 + s.act * 2);
        }
        if (PROF_RANGER.equals(s.profession) && firstLiving(s) != null) {
            firstLiving(s).bind += 1 + s.bindPower / 2;
        }
        if (hasTalent(s, "t_ranger_quarry") && s.turn == 1 && firstLiving(s) != null) {
            firstLiving(s).bind += 3;
            firstLiving(s).vulnerable += 2;
        }
        if (PROF_ARCANIST.equals(s.profession) && s.turn == 1) {
            s.voidEngine++;
        }
        if (PROF_MERCHANT.equals(s.profession) && s.turn == 1) {
            gainBlock(s, Math.min(16, Math.max(3, s.gold / 35)));
        }
        if (PROF_BLOODBOUND.equals(s.profession) && s.turn == 1) {
            s.professionCharge = 0;
            if (s.hp <= s.maxHp / 2) {
                s.energy++;
                gainBlock(s, 6 + s.act * 2);
                log(s, "血契低鸣，压低血线换来资源。");
            }
        }
        if (PROF_WEAVER.equals(s.profession) && s.turn == 1) {
            s.professionCharge = 0;
            s.energy += hasTalent(s, "t_weaver_setup") ? 1 : 0;
        }
        if (hasRelic(s, "steel_oath") && s.turn == 1) {
            gainBlock(s, 6);
        }
        if (hasRelic(s, "ember_core")) {
            s.burnPower++;
        }
        if (hasRelic(s, "leaf_charm")) {
            s.hp = Math.min(s.maxHp, s.hp + 1);
        }
        if (s.wildEngine > 0) {
            s.hp = Math.min(s.maxHp, s.hp + s.wildEngine);
            if (firstLiving(s) != null) {
                firstLiving(s).bind += s.wildEngine;
            }
        }
        if (hasRelic(s, "silver_suture") && s.hp <= s.maxHp / 2) {
            gainBlock(s, 4);
        }
        if (hasRelic(s, "void_lens") && s.turn == 1) {
            draw(s, 1);
        }
        if (hasRelic(s, "warden_plate") && s.turn == 1) {
            gainBlock(s, 8);
        }
        if (hasRelic(s, "duelist_sash") && s.turn == 1) {
            s.energy++;
        }
        if (hasRelic(s, "ranger_map") && s.turn == 1 && firstLiving(s) != null) {
            firstLiving(s).bind += 3;
        }
        applyEnemyStartOfPlayerTurnMechanics(s);
        ArrayList<Enemy> statusTick = new ArrayList<>(s.enemies);
        for (Enemy e : statusTick) {
            if (e.hp > 0) {
                if (e.burn > 0) {
                    damageEnemy(s, e, e.burn, true);
                    e.burn = Math.max(0, e.burn - 1);
                }
                if (e.bind > 0) {
                    e.bind--;
                }
                if (e.vulnerable > 0) {
                    e.vulnerable--;
                }
            }
        }
        int baseDraw = hasRelic(s, "ink_fountain") ? 6 : 5;
        if (hasRelic(s, "runic_shackle")) {
            baseDraw = Math.max(1, baseDraw - 1);
        }
        if (PROF_ARCANIST.equals(s.profession) && s.turn == 1) {
            baseDraw++;
        }
        draw(s, baseDraw);
        if (hasTalent(s, "t_shared_masterwork") && s.turn == 1) {
            draw(s, 1);
        }
        if (hasRelic(s, "deep_totem") && s.combatKind == 'B' && s.turn == 1) {
            draw(s, 2);
        }
        if (hasRelic(s, "void_anchor") && s.turn == 1) {
            Card anchor = new Card("void_nova");
            anchor.temp = true;
            addToHand(s, anchor);
        }
        if (hasRelic(s, "blood_contract") && s.turn == 1) {
            s.hp = Math.max(1, s.hp - 2);
            draw(s, 2);
        }
        if (hasRelic(s, "moon_lantern") && s.turn == 1) {
            draw(s, 1);
        }
        if (hasRelic(s, "clock_seed") && s.turn % 4 == 0) {
            s.energy++;
            draw(s, 1);
        }
        if (s.encounterModifier == MOD_TURBULENT && s.turn % 2 == 0) {
            draw(s, 1);
        }
        if (s.encounterModifier == MOD_POLLUTED && s.turn > 1 && s.turn % 3 == 0) {
            s.discard.add(new Card("daze"));
        }
        rollEnemyIntents(s);
        if (s.turn > 18) {
            int fatigue = (s.turn - 18) * 2;
            s.hp -= fatigue;
            log(s, "深渊压迫造成 " + fatigue + " 点伤害。");
            if (checkPlayerDefeated(s)) {
                return;
            }
        }
        if (allEnemiesDead(s)) {
            winCombat(s);
        }
    }

    private static void rollEnemyIntents(State s) {
        for (Enemy e : s.enemies) {
            if (e.hp <= 0) {
                continue;
            }
            int r = s.run.nextInt(100);
            if (e.kind == 10) {
                if (e.phase >= 2 && r < 30) {
                    e.intent = ENEMY_GUARD;
                    e.intentValue = 14 + s.act * 4;
                } else if (r < 54) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 11 + s.act * 4 + (e.phase >= 2 ? 4 : 0);
                } else if (r < 76) {
                    e.intent = ENEMY_DEBUFF;
                    e.intentValue = 2;
                } else if (livingEnemies(s).size() < 3) {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 1;
                } else {
                    e.intent = ENEMY_BUFF;
                    e.intentValue = 4;
                }
            } else if (e.kind == 11) {
                if (e.phase >= 2 && r < 28) {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 6 + s.act;
                } else if (r < 46) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 13 + s.act * 4 + (e.phase >= 2 ? 3 : 0);
                } else if (r < 78) {
                    e.intent = ENEMY_DEBUFF;
                    e.intentValue = 3 + s.act;
                } else {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 5 + s.act;
                }
            } else if (e.kind == 12) {
                if (e.phase >= 2 && r < 34) {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 3;
                } else if (r < 42) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 15 + s.act * 4 + (e.phase >= 2 ? 4 : 0);
                } else if (r < 68) {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 2;
                } else if (r < 86) {
                    e.intent = ENEMY_DEBUFF;
                    e.intentValue = 2;
                } else {
                    e.intent = ENEMY_BUFF;
                    e.intentValue = 5;
                }
            } else if (e.kind == 1) {
                if (r < 55) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 10 + s.act * 3;
                } else {
                    e.intent = ENEMY_GUARD;
                    e.intentValue = 12;
                }
            } else if (e.kind == 2) {
                if (r < 70) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 8 + s.act * 3;
                } else {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 1;
                }
            } else if (e.kind == 5) {
                if (r < 45) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 7 + s.act * 3;
                } else if (r < 80) {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 3 + s.act;
                } else {
                    e.intent = ENEMY_BUFF;
                    e.intentValue = 2;
                }
            } else if (e.kind == 7) {
                if (r < 38) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 9 + s.act * 3;
                } else if (r < 70) {
                    e.intent = ENEMY_GUARD;
                    e.intentValue = 14 + s.act * 2;
                } else {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 2 + s.act;
                }
            } else if (e.kind == 13) {
                if (r < 42) {
                    e.intent = ENEMY_DEBUFF;
                    e.intentValue = 2;
                } else if (r < 76) {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 1;
                } else {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 10 + s.act * 3;
                }
            } else if (e.kind == 14) {
                if (r < 38) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 12 + s.act * 3;
                } else if (r < 68) {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 3 + s.act;
                } else {
                    e.intent = ENEMY_BUFF;
                    e.intentValue = 2 + s.act;
                }
            } else if (e.kind == 20) {
                if (r < 62) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 7 + s.act * 3;
                } else if (r < 82) {
                    e.intent = ENEMY_GUARD;
                    e.intentValue = 7 + s.act * 2;
                } else {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 1 + s.act;
                }
            } else if (e.kind == 21) {
                if (r < 48) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 5 + s.act * 2;
                } else if (r < 84) {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 2 + s.act;
                } else {
                    e.intent = ENEMY_BUFF;
                    e.intentValue = 1;
                }
            } else if (e.kind == 22) {
                if (r < 34) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 6 + s.act * 2;
                } else if (r < 74) {
                    e.intent = ENEMY_GUARD;
                    e.intentValue = 10 + s.act * 3;
                } else {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 2 + s.act;
                }
            } else if (e.kind == 23) {
                if (r < 48) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 6 + s.act * 2;
                } else if (r < 78) {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 1;
                } else {
                    e.intent = ENEMY_DEBUFF;
                    e.intentValue = 1;
                }
            } else if (e.kind == 24) {
                if (r < 32) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 5 + s.act * 2;
                } else if (r < 72) {
                    e.intent = ENEMY_DEBUFF;
                    e.intentValue = 1 + s.act / 2;
                } else {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 1;
                }
            } else if (e.kind == 25) {
                if (r < 42) {
                    e.intent = ENEMY_GUARD;
                    e.intentValue = 12 + s.act * 3;
                } else if (r < 76) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 10 + s.act * 3;
                } else {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 2;
                }
            } else if (e.kind == 26) {
                if (r < 55) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 9 + s.act * 3;
                } else if (r < 82) {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 1;
                } else {
                    e.intent = ENEMY_BUFF;
                    e.intentValue = 2;
                }
            } else if (e.kind == 27) {
                if (r < 36) {
                    e.intent = ENEMY_DEBUFF;
                    e.intentValue = 1;
                } else if (r < 72) {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 1;
                } else {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 7 + s.act * 2;
                }
            } else if (r < 68) {
                e.intent = ENEMY_ATTACK;
                e.intentValue = 5 + s.act * 2 + s.ascension / 4 + e.strength;
            } else if (r < 84) {
                e.intent = ENEMY_DEBUFF;
                e.intentValue = 1;
            } else {
                e.intent = ENEMY_BUFF;
                e.intentValue = 2;
            }
        }
    }

    private static void enemyTurn(State s) {
        ArrayList<Enemy> acting = new ArrayList<>(s.enemies);
        for (Enemy e : acting) {
            if (e.hp <= 0) {
                continue;
            }
            if (e.intent == ENEMY_ATTACK) {
                int damage = Math.max(0, e.intentValue + e.strength + e.mark - (e.bind > 0 ? 3 : 0));
                if (s.vulnerable > 0) {
                    damage = Math.round(damage * 1.35f);
                }
                int taken = dealPlayerDamage(s, damage);
                if (e.mark > 0) {
                    e.mark--;
                }
                log(s, e.name + " 造成 " + taken + " 点伤害。");
            } else if (e.intent == ENEMY_BUFF) {
                e.strength += e.intentValue;
                e.block += 8 + s.act * 2;
            } else if (e.intent == ENEMY_DEBUFF) {
                s.vulnerable += e.intentValue;
                addStatusCard(s, s.run.nextBoolean() ? "daze" : "wound");
                log(s, e.name + " 施加压迫。");
            } else if (e.intent == ENEMY_GUARD) {
                e.block += e.intentValue;
            } else if (e.intent == ENEMY_SPECIAL) {
                enemySpecial(s, e);
            }
            if (s.encounterModifier == MOD_FRENZY) {
                e.strength++;
            }
        }
        s.vulnerable = Math.max(0, s.vulnerable - 1);
    }

    private static void enemySpecial(State s, Enemy e) {
        if (e.kind == 10) {
            if (livingEnemies(s).size() < 3) {
                Enemy shard = enemy("裂影", 24 + s.act * 8 + s.ascension, 4);
                shard.intent = ENEMY_DEBUFF;
                shard.intentValue = 1;
                s.enemies.add(shard);
                log(s, e.name + " 召出裂影。");
            } else {
                e.strength += 3;
            }
        } else if (e.kind == 11) {
            addStatusCard(s, "daze");
            addStatusCard(s, "wound");
            s.vulnerable += 1;
            for (Enemy other : s.enemies) {
                if (other.hp > 0) {
                    other.burn += e.intentValue;
                }
            }
            log(s, e.name + " 点燃战场。");
        } else if (e.kind == 12) {
            int exhausts = e.phase >= 2 ? 2 : 1;
            for (int i = 0; i < exhausts && !s.draw.isEmpty(); i++) {
                s.exhaust.add(s.draw.remove(s.draw.size() - 1));
            }
            s.energy = Math.max(0, s.energy - 1);
            addStatusCard(s, "daze");
            log(s, e.name + " 扭曲你的牌序。");
        } else if (e.kind == 2) {
            e.block += 10 + s.act * 2;
            e.strength += 1;
        } else if (e.kind == 5) {
            Enemy target = firstLiving(s);
            if (target != null) {
                target.burn += e.intentValue;
            }
            s.vulnerable += 1;
        } else if (e.kind == 7) {
            for (Enemy other : s.enemies) {
                if (other.hp > 0) {
                    other.block += 6 + s.act * 2;
                    other.bind += e.intentValue;
                }
            }
        } else if (e.kind == 23) {
            int stolen = Math.min(s.gold, 8 + s.act * 4);
            if (stolen > 0) {
                s.gold -= stolen;
                e.stolenGold += stolen;
                e.mark += 1 + s.act / 2;
                log(s, e.name + " 偷走 " + stolen + " 金币并留下空印。");
            } else {
                e.mark += 2;
                addStatusCard(s, "daze");
                log(s, e.name + " 留下空印。");
            }
        } else if (e.kind == 24) {
            e.doom = Math.max(1, e.doom - 1);
            s.vulnerable += 1;
            addStatusCard(s, "wound");
            log(s, e.name + " 催动蚀骨仪式。");
        } else if (e.kind == 25) {
            e.thorns += 1;
            e.block += 10 + s.act * 3;
            log(s, e.name + " 收拢琥珀刺甲。");
        } else if (e.kind == 27) {
            for (Enemy other : livingEnemies(s)) {
                other.block += 7 + s.act * 2;
            }
            s.nextEnergyPenalty = Math.max(s.nextEnergyPenalty, 1);
            log(s, e.name + " 锁住雾线，下回合能量受压。");
        } else {
            e.strength += 2;
            e.block += 8;
        }
    }

    private static void applyCard(State s, Card c, CardDef d, Enemy target) {
        int damage = c.upgraded ? d.damageUp : d.damage;
        int block = c.upgraded ? d.blockUp : d.block;
        int draw = c.upgraded ? d.drawUp : d.draw;
        int burn = c.upgraded ? d.burnUp : d.burn;
        int bind = c.upgraded ? d.bindUp : d.bind;
        int heal = c.upgraded ? d.healUp : d.heal;
        if (s.steelEngine > 0 && block > 0) {
            block += s.steelEngine;
        }
        if (s.ashEngine > 0 && burn > 0) {
            burn += s.ashEngine;
        }
        if (s.wildEngine > 0 && bind > 0) {
            bind += s.wildEngine;
        }
        if ("steel_spear".equals(d.id) && s.block >= 10) {
            damage += c.upgraded ? 8 : 5;
        }
        if ("wild_thorn".equals(d.id) && target != null && target.bind > 0) {
            damage += c.upgraded ? 8 : 5;
            block += c.upgraded ? 4 : 3;
        }
        if (PROF_BLOODBOUND.equals(s.profession) && d.hpLoss > 0) {
            damage += 3 + s.act + Math.max(0, s.maxHp - s.hp) / 12;
            block += hasTalent(s, "t_bloodbound_scar") ? 4 + s.act : 0;
        }
        if (PROF_BLOODBOUND.equals(s.profession) && "wound".equals(c.id)) {
            draw += 1;
            s.professionCharge++;
        }
        if (hasRelic(s, "scar_talisman") && "wound".equals(c.id)) {
            damage += 5 + s.act * 2;
            block += 4 + s.act;
            target = target != null ? target : firstLiving(s);
        }
        if (hasTalent(s, "t_bloodbound_feast") && target != null && damage > 0 && s.hp <= s.maxHp / 2) {
            heal += 1;
        }
        if (PROF_WEAVER.equals(s.profession) && (d.scry > 0 || d.upgradeRandom || d.draw > 0)) {
            block += 1 + Math.min(5, s.professionCharge);
        }
        if (hasRelic(s, "loom_shuttle") && d.scry > 0) {
            block += Math.min(10, 2 + d.scry);
        }
        if (hasTalent(s, "t_weaver_mastery") && c.upgraded && (d.type == 1 || d.type == 2)) {
            draw += s.professionUsedThisTurn == 0 ? 1 : 0;
            s.professionUsedThisTurn++;
        }
        if (hasRelic(s, "polished_cog") && c.upgraded && s.relicTriggersThisTurn < 2) {
            block += 3;
            s.relicTriggersThisTurn++;
        }
        if (d.comboDamage > 0) {
            damage += d.comboDamage * Math.max(0, s.cardsPlayedThisTurn - 1);
        }
        if (hasTalent(s, "t_duelist_tempo") && d.cost == 0 && s.professionUsedThisTurn < 2) {
            damage += 4 + s.act;
            s.professionUsedThisTurn++;
        }
        if (hasTalent(s, "t_duelist_execution") && target != null && (target.vulnerable > 0 || target.hp <= target.maxHp / 2)) {
            damage += 6 + s.act * 2;
        }
        if (d.goldDamage) {
            damage += Math.min(c.upgraded ? 42 : 28, Math.max(0, s.gold / (c.upgraded ? 8 : 11)));
        }
        if (d.goldBlock) {
            block += Math.min(c.upgraded ? 24 : 16, Math.max(0, s.gold / (c.upgraded ? 12 : 16)));
        }
        if (d.burnToBlock) {
            int burnTotal = 0;
            for (Enemy e : livingEnemies(s)) {
                burnTotal += e.burn;
            }
            block += Math.min(c.upgraded ? 28 : 20, burnTotal);
        }
        if (d.bindToDraw) {
            int bindTotal = 0;
            for (Enemy e : livingEnemies(s)) {
                bindTotal += e.bind;
            }
            draw += Math.min(c.upgraded ? 3 : 2, bindTotal / 4);
        }

        if (damage > 0) {
            if (d.aoe) {
                ArrayList<Enemy> copy = livingEnemies(s);
                for (Enemy e : copy) {
                    damageEnemy(s, e, damage, false);
                }
            } else if (target != null) {
                damageEnemy(s, target, damage, false);
            }
        }
        if (block > 0) {
            gainBlock(s, block);
        }
        if (draw > 0) {
            draw(s, draw);
        }
        if (d.energyGain > 0) {
            s.energy += c.upgraded ? d.energyGain + d.energyGainUp : d.energyGain;
        }
        if (burn > 0) {
            if (d.aoe) {
                for (Enemy e : livingEnemies(s)) {
                    e.burn += burn + s.burnPower;
                }
            } else if (target != null) {
                target.burn += burn + s.burnPower;
            }
        }
        if (bind > 0) {
            if (d.aoe) {
                for (Enemy e : livingEnemies(s)) {
                    e.bind += bind + s.bindPower;
                }
            } else if (target != null) {
                target.bind += bind + s.bindPower;
            }
        }
        if (d.vulnerable > 0) {
            int vuln = c.upgraded ? d.vulnerable + 1 : d.vulnerable;
            if (d.aoe) {
                for (Enemy e : livingEnemies(s)) {
                    e.vulnerable += vuln;
                }
            } else if (target != null) {
                target.vulnerable += vuln;
            }
        }
        if (heal > 0) {
            s.hp = Math.min(s.maxHp, s.hp + heal);
        }
        if (d.hpLoss > 0) {
            int loss = c.upgraded && d.hpLoss > 1 ? d.hpLoss - 1 : d.hpLoss;
            s.hp = Math.max(1, s.hp - loss);
        }
        if (d.gainBurnPower > 0) {
            s.burnPower += c.upgraded ? d.gainBurnPower + 1 : d.gainBurnPower;
        }
        if (d.gainBindPower > 0) {
            s.bindPower += c.upgraded ? d.gainBindPower + 1 : d.gainBindPower;
        }
        if (d.gainSteelEngine > 0) {
            s.steelEngine += c.upgraded ? d.gainSteelEngine + 1 : d.gainSteelEngine;
        }
        if (d.gainAshEngine > 0) {
            s.ashEngine += c.upgraded ? d.gainAshEngine + 1 : d.gainAshEngine;
        }
        if (d.gainWildEngine > 0) {
            s.wildEngine += c.upgraded ? d.gainWildEngine + 1 : d.gainWildEngine;
        }
        if (d.gainVoidEngine > 0) {
            s.voidEngine += c.upgraded ? d.gainVoidEngine + 1 : d.gainVoidEngine;
        }
        if (d.blockToDamage && target != null) {
            damageEnemy(s, target, s.block, false);
        }
        if (d.detonateBurn && target != null) {
            int boom = target.burn * (c.upgraded ? 3 : 2);
            target.burn = 0;
            damageEnemy(s, target, boom, true);
        }
        if (d.exhaustForDamage && target != null) {
            if (!s.discard.isEmpty()) {
                Card ex = s.discard.remove(s.discard.size() - 1);
                s.exhaust.add(ex);
                damageEnemy(s, target, c.upgraded ? 16 : 11, false);
            }
        }
        if (d.exhaustTopDiscard && !s.discard.isEmpty()) {
            s.exhaust.add(s.discard.remove(s.discard.size() - 1));
        }
        if (d.createEcho) {
            Card echo = new Card(d.echoCardId.length() > 0 ? d.echoCardId : c.id);
            echo.temp = true;
            addToHand(s, echo);
        }
        if (d.createPotion && s.potions.size() < potionLimit(s)) {
            PotionDef p = POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size()));
            s.potions.add(p.id);
            log(s, "调制药剂：" + p.name);
        }
        if (d.createWound) {
            addStatusCard(s, "wound");
        }
        if (d.goldGain > 0) {
            int gain = c.upgraded ? d.goldGain + 5 : d.goldGain;
            if (hasTalent(s, "t_merchant_interest")) {
                gain += 5;
            }
            s.gold += gain;
        }
        if (d.spreadStatus && target != null) {
            for (Enemy e : livingEnemies(s)) {
                if (e != target) {
                    e.burn += Math.min(c.upgraded ? 5 : 3, Math.max(0, target.burn / 2));
                    e.bind += Math.min(c.upgraded ? 5 : 3, Math.max(0, target.bind / 2));
                    if (hasTalent(s, "t_alchemist_plague")) {
                        e.burn += 2 + s.burnPower;
                        e.bind += 2 + s.bindPower;
                    }
                    if (target.vulnerable > 0) {
                        e.vulnerable += 1;
                    }
                }
            }
        }
        if (d.retainBlock) {
            s.block += s.turn;
        }
        if (d.upgradeRandom && !s.hand.isEmpty()) {
            ArrayList<Card> choices = new ArrayList<>();
            for (Card h : s.hand) {
                if (!h.upgraded && card(h.id) != null && card(h.id).type != 3) {
                    choices.add(h);
                }
            }
            if (!choices.isEmpty()) {
                choices.get(s.run.nextInt(choices.size())).upgraded = true;
            }
        }
        if (d.addStatusToEnemy && target != null) {
            target.vulnerable += 1;
            target.bind += 1;
        }
        if (d.scry > 0) {
            int remove = Math.min(d.scry, s.draw.size());
            for (int i = 0; i < remove; i++) {
                if (s.run.nextBoolean()) {
                    s.discard.add(s.draw.remove(s.draw.size() - 1));
                }
            }
        }
    }

    private static void triggerAfterPlay(State s, Card c, CardDef d) {
        if (hasRelic(s, "thorn_ring") && d.type == 1) {
            Enemy e = firstLiving(s);
            if (e != null) {
                damageEnemy(s, e, 3, true);
            }
        }
        if (hasRelic(s, "charcoal_sigil") && d.burn > 0) {
            s.energy++;
        }
        if (hasRelic(s, "root_drum") && d.bind > 0) {
            draw(s, 1);
        }
        if (hasRelic(s, "cinder_spoon") && d.burn > 0) {
            Enemy e = firstLiving(s);
            if (e != null) {
                e.burn += 1;
            }
        }
        if (hasRelic(s, "green_bell") && d.bind > 0) {
            s.hp = Math.min(s.maxHp, s.hp + 1);
        }
        if (hasRelic(s, "hollow_crown") && d.exhaust) {
            draw(s, 1);
        }
        if (hasRelic(s, "empty_coin") && d.exhaust) {
            s.gold += 1;
        }
        if (hasRelic(s, "void_abacus") && d.exhaust) {
            gainBlock(s, 3 + s.voidEngine);
            if (s.cardsPlayedThisTurn % 3 == 0) {
                draw(s, 1);
            }
        }
        if (hasRelic(s, "opal_scar") && s.cardsPlayedThisTurn == 3) {
            gainBlock(s, 7);
        }
        if (hasRelic(s, "tempo_metronome") && s.cardsPlayedThisTurn == 4) {
            s.energy++;
            Enemy e = firstLiving(s);
            if (e != null) {
                damageEnemy(s, e, 6 + s.act * 2, true);
            }
        }
        if (hasRelic(s, "warden_plate") && d.type == 1 && s.cardsPlayedThisTurn <= 2) {
            gainBlock(s, 3);
        }
        if (hasRelic(s, "duelist_sash") && d.type == 0 && s.cardsPlayedThisTurn == 2) {
            Card cut = new Card("quick_cut");
            cut.temp = true;
            addToHand(s, cut);
        }
        if (hasRelic(s, "ranger_map") && d.bind > 0) {
            Enemy e = firstLiving(s);
            if (e != null) {
                e.vulnerable += 1;
            }
        }
        if (hasTalent(s, "t_ranger_net") && d.bind > 0) {
            gainBlock(s, 4 + Math.min(8, d.bind + s.bindPower));
        }
        if (hasRelic(s, "arcane_ink") && d.exhaust) {
            s.energy++;
        }
        if (PROF_WARDEN.equals(s.profession) && d.type == 1) {
            s.professionCharge++;
            if (s.professionCharge >= 2) {
                gainBlock(s, 5);
                Enemy e = firstLiving(s);
                if (e != null) {
                    damageEnemy(s, e, 5 + s.steelEngine + (hasTalent(s, "t_warden_counter") ? 6 : 0), true);
                }
                if (hasTalent(s, "t_warden_counter")) {
                    draw(s, 1);
                }
                s.professionCharge = 0;
            }
        }
        if (PROF_DUELIST.equals(s.profession) && d.type == 0) {
            s.professionCharge++;
            if (s.professionCharge >= 3) {
                Enemy e = firstLiving(s);
                if (e != null) {
                    int extra = hasTalent(s, "t_duelist_execution") && (e.vulnerable > 0 || e.hp <= e.maxHp / 2) ? 8 : 0;
                    damageEnemy(s, e, 8 + Math.min(10, s.cardsPlayedThisTurn) + extra, true);
                }
                s.professionCharge = 0;
            }
        }
        if (PROF_RANGER.equals(s.profession) && d.type == 0) {
            Enemy e = firstLiving(s);
            if (e != null) {
                e.bind += 1 + s.bindPower / 2;
            }
        }
        if (PROF_ARCANIST.equals(s.profession) && (d.exhaust || c.temp)) {
            s.professionCharge++;
            if (s.professionCharge >= 2) {
                draw(s, 1);
                s.energy++;
                if (hasTalent(s, "t_arcanist_overflow")) {
                    Card echo = new Card("quick_cut");
                    echo.temp = true;
                    addToHand(s, echo);
                }
                s.professionCharge = 0;
            }
        }
        if (PROF_MERCHANT.equals(s.profession) && d.goldGain > 0) {
            gainBlock(s, 4 + Math.min(8, s.gold / 80));
        }
        if (hasRelic(s, "tithe_box") && d.goldGain > 0) {
            Enemy e = firstLiving(s);
            if (e != null) {
                damageEnemy(s, e, 4 + Math.min(12, s.gold / 90), true);
            }
        }
        if (hasRelic(s, "vital_sprout") && d.heal > 0) {
            gainBlock(s, 3 + s.wildEngine);
            Enemy e = firstLiving(s);
            if (e != null) {
                e.bind += 1 + s.bindPower / 2;
            }
        }
        if (PROF_BLOODBOUND.equals(s.profession)) {
            if (d.hpLoss > 0 || "wound".equals(c.id)) {
                s.professionCharge++;
            }
            if (s.professionCharge >= 3) {
                Enemy e = firstLiving(s);
                if (e != null) {
                    damageEnemy(s, e, 9 + s.act * 2 + Math.max(0, s.maxHp - s.hp) / 8, true);
                    e.vulnerable += hasTalent(s, "t_bloodbound_scar") ? 1 : 0;
                }
                s.hp = Math.min(s.maxHp, s.hp + (hasTalent(s, "t_bloodbound_feast") ? 5 : 2));
                s.professionCharge = 0;
            }
        }
        if (PROF_WEAVER.equals(s.profession) && d.type == 1) {
            s.professionCharge++;
            if (s.professionCharge >= 3) {
                draw(s, hasTalent(s, "t_weaver_mastery") ? 2 : 1);
                s.energy += 1;
                upgradeRandomHandCard(s);
                s.professionCharge = 0;
            }
        }
        if (s.steelEngine > 0 && d.type == 1) {
            Enemy e = firstLiving(s);
            if (e != null) {
                damageEnemy(s, e, s.steelEngine * 2, true);
            }
        }
        if (s.ashEngine > 0 && d.burn > 0) {
            Enemy e = firstLiving(s);
            if (e != null) {
                damageEnemy(s, e, s.ashEngine, true);
            }
        }
        if (s.voidEngine > 0 && (d.exhaust || c.temp)) {
            if (s.run.nextInt(100) < Math.min(80, 25 + s.voidEngine * 15)) {
                Card echo = new Card("quick_cut");
                echo.temp = true;
                addToHand(s, echo);
            }
        }
    }

    private static void damageEnemy(State s, Enemy e, int raw, boolean piercing) {
        if (e == null || e.hp <= 0 || raw <= 0) {
            return;
        }
        int dmg = raw;
        if (e.vulnerable > 0) {
            dmg = Math.round(dmg * 1.5f);
        }
        if (!piercing) {
            int b = Math.min(e.block, dmg);
            e.block -= b;
            dmg -= b;
        }
        boolean triggersContact = !piercing && dmg > 0 && e.thorns > 0;
        e.hp -= dmg;
        if (triggersContact) {
            int prick = Math.min(7 + s.act, e.thorns);
            int taken = dealPlayerDamage(s, prick);
            if (taken > 0) {
                log(s, e.name + " 的刺甲反伤 " + taken + "。");
            }
        }
        if (e.hp <= 0) {
            e.hp = 0;
            e.block = 0;
            if (e.stolenGold > 0) {
                int recovered = e.stolenGold + Math.min(8, e.stolenGold / 2);
                s.gold += recovered;
                log(s, "夺回空面赃款 " + recovered + " 金币。");
            }
            if (hasRelic(s, "hunter_mark")) {
                s.gold += 7;
            }
            if (hasTalent(s, "t_ranger_quarry")) {
                Enemy next = firstLiving(s);
                if (next != null) {
                    next.bind += 2 + s.bindPower;
                    next.vulnerable += 1;
                }
            }
        }
        updateEnemyPhases(s);
    }

    private static int dealPlayerDamage(State s, int damage) {
        int blocked = Math.min(s.block, Math.max(0, damage));
        s.block -= blocked;
        int taken = Math.max(0, damage - blocked);
        if (taken > 0) {
            s.hp -= taken;
            if (s.combatQuest == QUEST_UNHURT) {
                s.questProgress += taken;
            }
        }
        return taken;
    }

    private static boolean checkPlayerDefeated(State s) {
        if (s.hp > 0 || s.mode == MODE_GAME_OVER) {
            return false;
        }
        s.mode = MODE_GAME_OVER;
        s.playerTurn = false;
        finishRun(s, false);
        return true;
    }

    private static void gainBlock(State s, int amount) {
        s.block += amount;
        if (hasRelic(s, "steel_oath") && amount >= 10) {
            s.block += 2;
        }
        updateQuestProgress(s);
    }

    private static void draw(State s, int count) {
        for (int i = 0; i < count; i++) {
            if (s.draw.isEmpty()) {
                if (s.discard.isEmpty()) {
                    return;
                }
                s.draw.addAll(s.discard);
                s.discard.clear();
                Collections.shuffle(s.draw, s.run);
            }
            if (s.hand.size() >= 10) {
                s.discard.add(s.draw.remove(s.draw.size() - 1));
            } else {
                addToHand(s, s.draw.remove(s.draw.size() - 1));
            }
        }
    }

    private static void addToHand(State s, Card c) {
        if (s.hand.size() >= 10) {
            s.discard.add(c);
        } else {
            s.hand.add(c);
        }
    }

    private static void updateQuestProgress(State s) {
        if (s.combatQuest == QUEST_NONE || s.questComplete) {
            return;
        }
        if (s.combatQuest == QUEST_COMBO) {
            s.questProgress = Math.max(s.questProgress, s.cardsPlayedThisTurn);
            if (s.questProgress >= s.questTarget) {
                s.questComplete = true;
            }
        } else if (s.combatQuest == QUEST_GUARD) {
            s.questProgress = Math.max(s.questProgress, s.block);
            if (s.questProgress >= s.questTarget) {
                s.questComplete = true;
            }
        } else if (s.combatQuest == QUEST_HEX) {
            int best = 0;
            for (Enemy e : livingEnemies(s)) {
                best = Math.max(best, e.burn + e.bind + e.vulnerable);
            }
            s.questProgress = Math.max(s.questProgress, best);
            if (s.questProgress >= s.questTarget) {
                s.questComplete = true;
            }
        } else if (s.combatQuest == QUEST_UNHURT) {
            s.questComplete = s.questProgress <= s.questTarget;
        } else if (s.combatQuest == QUEST_SWIFT) {
            s.questProgress = s.turn;
            s.questComplete = s.turn <= s.questTarget;
        } else if (s.combatQuest == QUEST_LEAN) {
            s.questProgress = s.totalCardsPlayed;
            s.questComplete = s.totalCardsPlayed <= s.questTarget;
        }
    }

    private static boolean questSucceeded(State s) {
        if (s.combatQuest == QUEST_NONE) {
            return false;
        }
        updateQuestProgress(s);
        if (s.combatQuest == QUEST_SWIFT) return s.turn <= s.questTarget;
        if (s.combatQuest == QUEST_UNHURT) return s.questProgress <= s.questTarget;
        if (s.combatQuest == QUEST_LEAN) return s.totalCardsPlayed <= s.questTarget;
        return s.questComplete;
    }

    private static void awardQuest(State s) {
        if (!questSucceeded(s)) {
            log(s, "战斗目标未完成：" + questName(s.combatQuest));
            return;
        }
        int bonus = 16 + s.act * 6 + (s.combatKind == 'B' ? 30 : s.combatKind == 'E' ? 18 : 0);
        s.gold += bonus;
        s.meta.questCompletions++;
        if (s.meta.questCompletions >= 10) {
            unlockAchievement(s, "quest_hunter");
        }
        log(s, "完成目标：" + questName(s.combatQuest) + "，额外获得 " + bonus + " 金币。");
        if (s.combatKind == 'E' || s.combatKind == 'B' || s.run.nextInt(100) < 22) {
            upgradeRandomDeckCard(s);
            log(s, "目标奖励升级了一张牌。");
        }
    }

    private static void finishRun(State s, boolean victory) {
        if (s.runFinished) {
            return;
        }
        s.ensureRandom();
        s.runFinished = true;
        s.newAchievements.clear();
        int reached = (Math.max(1, s.act) - 1) * 12 + Math.max(0, s.floor);
        s.meta.runs++;
        if (victory) {
            s.meta.wins++;
            s.meta.highestDepth = Math.max(s.meta.highestDepth, s.ascension);
            int p = professionIndex(s.profession);
            if (p >= 0 && p < s.meta.professionWins.length) {
                s.meta.professionWins[p]++;
            }
        }
        s.meta.highestFloor = Math.max(s.meta.highestFloor, reached);
        s.meta.maxGold = Math.max(s.meta.maxGold, s.gold);
        s.meta.maxDeck = Math.max(s.meta.maxDeck, s.deck.size());
        s.lastRunSummary = (victory ? "胜利" : "止步") + " / 层数 " + reached + " / 金币 " + s.gold
                + " / 牌组 " + s.deck.size() + " / 专精 " + s.talents.size();
        unlockAchievement(s, "first_run");
        if (victory) {
            unlockAchievement(s, "first_win");
        }
        if (allProfessionWins(s)) {
            unlockAchievement(s, "all_professions");
        }
        if (s.relics.size() >= 10 || s.meta.maxDeck >= 36) {
            unlockAchievement(s, "collector");
        }
        if (victory && s.ascension >= 10) {
            unlockAchievement(s, "high_depth");
        }
        if (s.talents.size() >= 2) {
            unlockAchievement(s, "talent_master");
        }
        if (s.gold >= 300 || s.meta.maxGold >= 500) {
            unlockAchievement(s, "rich");
        }
    }

    private static void unlockAchievement(State s, String id) {
        if (!s.meta.achievements.contains(id)) {
            s.meta.achievements.add(id);
            s.newAchievements.add(id);
            log(s, "解锁成就：" + achievementName(id));
        }
    }

    private static boolean allProfessionWins(State s) {
        if (s.meta.professionWins.length < PROFESSIONS.length) {
            return false;
        }
        for (int i = 0; i < PROFESSIONS.length; i++) {
            if (s.meta.professionWins[i] <= 0) {
                return false;
            }
        }
        return true;
    }

    private static int professionIndex(String profession) {
        for (int i = 0; i < PROFESSIONS.length; i++) {
            if (PROFESSIONS[i].equals(profession)) {
                return i;
            }
        }
        return -1;
    }

    private static void winCombat(State s) {
        int gold = 18 + s.act * 5 + s.run.nextInt(13);
        if (s.combatKind == 'E') {
            gold += 24;
        }
        if (s.combatKind == 'B') {
            gold += 55;
        }
        if (hasTalent(s, "t_shared_hunter") && (s.combatKind == 'E' || s.combatKind == 'B')) {
            gold += 18 + s.act * 6;
        }
        if (s.encounterModifier == MOD_ARMORED) {
            gold += 12 + s.act * 3;
        } else if (s.encounterModifier == MOD_BOUNTY) {
            gold += 32 + s.act * 8;
        }
        s.gold += gold;
        awardQuest(s);
        if (hasRelic(s, "cup_of_mist")) {
            s.hp = Math.min(s.maxHp, s.hp + 4);
        }
        if (hasRelic(s, "iron_tea") && s.combatKind == 'E') {
            s.hp = Math.min(s.maxHp, s.hp + 10);
        }
        log(s, "胜利，获得 " + gold + " 金币。");
        s.cardRewards.clear();
        s.cardRewardSkipped = false;
        int rewardCount = hasRelic(s, "cracked_compass") ? 4 : 3;
        if (hasRelic(s, "runic_shackle")) {
            rewardCount++;
        }
        if (s.encounterModifier == MOD_POLLUTED || s.encounterModifier == MOD_TURBULENT) {
            rewardCount++;
        }
        HashSet<String> offeredCards = new HashSet<>();
        for (int i = 0; i < rewardCount; i++) {
            CardDef d = randomCard(s, s.origin, s.combatKind != 'C' || s.encounterModifier == MOD_FRENZY || hasRelic(s, "scarlet_dice"), offeredCards);
            offeredCards.add(d.id);
            RewardCard rc = new RewardCard();
            rc.id = d.id;
            s.cardRewards.add(rc);
        }
        s.relicRewards.clear();
        if (s.combatKind == 'B') {
            int choices = hasRelic(s, "hollow_crown") ? 4 : 3;
            HashSet<String> offered = new HashSet<>();
            for (int i = 0; i < choices; i++) {
                RelicDef r = randomBossRelic(s, offered);
                offered.add(r.id);
                s.relicRewards.add(r.id);
            }
        } else if (s.combatKind == 'E' || s.run.nextInt(100) < (s.encounterModifier == MOD_BOUNTY ? 34 : 18)) {
            s.relicRewards.add(randomRelic(s).id);
        }
        if (s.potions.size() < potionLimit(s) && s.run.nextInt(100) < 34) {
            PotionDef p = POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size()));
            s.potions.add(p.id);
            log(s, "发现药剂：" + p.name);
        }
        s.mode = MODE_REWARD;
        s.playerTurn = false;
    }

    private static void clearRewards(State s) {
        s.cardRewards.clear();
    }

    private static void afterReward(State s) {
        if (s.combatKind == 'B') {
            openTalentChoice(s);
        } else {
            s.mode = MODE_MAP;
        }
    }

    private static void openTalentChoice(State s) {
        if (s.act >= 3) {
            nextAct(s);
            return;
        }
        s.talentChoices.clear();
        ArrayList<TalentDef> pool = new ArrayList<>();
        for (TalentDef t : TALENT_LIBRARY) {
            if (!s.talents.contains(t.id) && (t.profession.length() == 0 || t.profession.equals(s.profession))) {
                pool.add(t);
            }
        }
        Collections.shuffle(pool, s.run);
        for (int i = 0; i < 3 && i < pool.size(); i++) {
            s.talentChoices.add(pool.get(i).id);
        }
        if (s.talentChoices.isEmpty()) {
            nextAct(s);
        } else {
            s.mode = MODE_TALENT;
        }
    }

    private static void openShop(State s) {
        s.mode = MODE_SHOP;
        s.shopCards.clear();
        s.shopRelics.clear();
        s.shopPotions.clear();
        if (hasTalent(s, "t_merchant_interest")) {
            int income = 20 + s.act * 10;
            s.gold += income;
            log(s, "复利账本入账 " + income + " 金币。");
        }
        HashSet<String> offeredCards = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            CardDef d = randomCard(s, s.origin, true, offeredCards);
            offeredCards.add(d.id);
            s.shopCards.add(d.id);
        }
        for (int i = 0; i < 3; i++) {
            s.shopRelics.add(randomRelic(s).id);
        }
        for (int i = 0; i < 3; i++) {
            s.shopPotions.add(POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size())).id);
        }
        log(s, "抵达裂隙商栈。");
    }

    private static void openRest(State s) {
        s.mode = MODE_REST;
        if (hasRelic(s, "dawn_pin")) {
            s.hp = Math.min(s.maxHp, s.hp + 5);
        }
        log(s, "找到一处静火营地。");
    }

    private static void openEvent(State s) {
        s.mode = MODE_EVENT;
        s.eventId = s.run.nextInt(8);
    }

    private static CardDef randomCard(State s, String origin, boolean allowRare) {
        return randomCard(s, origin, allowRare, Collections.<String>emptySet());
    }

    private static CardDef randomCard(State s, String origin, boolean allowRare, Set<String> excluded) {
        ArrayList<CardDef> pool = new ArrayList<>();
        for (CardDef d : CARD_LIBRARY) {
            if (d.type == 3) {
                continue;
            }
            if (excluded.contains(d.id)) {
                continue;
            }
            if (!"通用".equals(d.origin) && !d.origin.equals(origin)) {
                if (s.run.nextInt(100) > 18) {
                    continue;
                }
            }
            if (d.profession.length() > 0 && !d.profession.equals(s.profession)) {
                if (s.run.nextInt(100) > 8) {
                    continue;
                }
            }
            if (!allowRare && d.rarity == 2) {
                continue;
            }
            int weight = d.rarity == 0 ? 7 : d.rarity == 1 ? 4 : 2;
            if (hasRelic(s, "scarlet_dice") && d.rarity == 2) {
                weight += 2;
            }
            if (d.origin.equals(origin)) {
                weight += 3;
            }
            if (d.profession.equals(s.profession)) {
                weight += 7;
            }
            weight += professionCardBonus(s, d);
            for (int i = 0; i < weight; i++) {
                pool.add(d);
            }
        }
        if (pool.isEmpty()) {
            for (CardDef d : CARD_LIBRARY) {
                if (d.type != 3 && !excluded.contains(d.id)) {
                    return d;
                }
            }
            return CARD_LIBRARY.get(0);
        }
        return pool.get(s.run.nextInt(pool.size()));
    }

    private static int professionCardBonus(State s, CardDef d) {
        if (PROF_WARDEN.equals(s.profession) && (d.block > 0 || d.gainSteelEngine > 0 || d.blockToDamage)) {
            return 3;
        }
        if (PROF_DUELIST.equals(s.profession) && (d.cost == 0 || d.draw > 0 || d.comboDamage > 0)) {
            return 3;
        }
        if (PROF_ALCHEMIST.equals(s.profession) && (d.burn > 0 || d.bind > 0 || d.createPotion || d.spreadStatus)) {
            return 3;
        }
        if (PROF_RANGER.equals(s.profession) && (d.bind > 0 || d.aoe || d.bindToDraw)) {
            return 3;
        }
        if (PROF_ARCANIST.equals(s.profession) && (d.exhaust || d.createEcho || d.exhaustTopDiscard || d.exhaustForDamage)) {
            return 3;
        }
        if (PROF_MERCHANT.equals(s.profession) && (d.goldGain > 0 || d.goldDamage || d.goldBlock)) {
            return 4;
        }
        if (PROF_BLOODBOUND.equals(s.profession) && (d.hpLoss > 0 || d.heal > 0 || "wound".equals(d.id))) {
            return 4;
        }
        if (PROF_WEAVER.equals(s.profession) && (d.scry > 0 || d.upgradeRandom || d.draw > 0 || d.createEcho)) {
            return 4;
        }
        if (hasTalent(s, "t_shared_hunter") && d.profession.equals(s.profession)) {
            return 3;
        }
        if (hasTalent(s, "t_shared_masterwork") && d.rarity == 2) {
            return 1;
        }
        return 0;
    }

    private static RelicDef randomRelic(State s) {
        return randomRelic(s, Collections.<String>emptySet());
    }

    private static RelicDef randomRelic(State s, Set<String> excluded) {
        ArrayList<RelicDef> pool = new ArrayList<>();
        for (RelicDef r : RELIC_LIBRARY) {
            if (!r.boss && !s.relics.contains(r.id) && !excluded.contains(r.id)) {
                int weight = 1 + professionRelicBonus(s, r.id);
                for (int i = 0; i < weight; i++) {
                    pool.add(r);
                }
            }
        }
        if (pool.isEmpty()) {
            return RELIC_LIBRARY.get(0);
        }
        return pool.get(s.run.nextInt(pool.size()));
    }

    private static int professionRelicBonus(State s, String id) {
        if (PROF_WARDEN.equals(s.profession) && ("warden_plate".equals(id) || "steel_oath".equals(id) || "thorn_ring".equals(id))) {
            return 2;
        }
        if (PROF_DUELIST.equals(s.profession) && ("duelist_sash".equals(id) || "amber_quill".equals(id) || "opal_scar".equals(id) || "tempo_metronome".equals(id))) {
            return 2;
        }
        if (PROF_ALCHEMIST.equals(s.profession) && ("alchemist_case".equals(id) || "cinder_spoon".equals(id) || "green_bell".equals(id) || "glass_vials".equals(id))) {
            return 2;
        }
        if (PROF_RANGER.equals(s.profession) && ("ranger_map".equals(id) || "root_drum".equals(id) || "green_bell".equals(id))) {
            return 2;
        }
        if (PROF_ARCANIST.equals(s.profession) && ("arcane_ink".equals(id) || "hollow_crown".equals(id) || "void_lens".equals(id) || "void_abacus".equals(id))) {
            return 2;
        }
        if (PROF_MERCHANT.equals(s.profession) && ("merchant_scale".equals(id) || "merchant_key".equals(id) || "cracked_compass".equals(id) || "tithe_box".equals(id))) {
            return 2;
        }
        if (PROF_BLOODBOUND.equals(s.profession) && ("blood_contract".equals(id) || "silver_suture".equals(id) || "cup_of_mist".equals(id) || "scar_talisman".equals(id))) {
            return 2;
        }
        if (PROF_WEAVER.equals(s.profession) && ("ink_fountain".equals(id) || "glass_anvil".equals(id) || "amber_quill".equals(id) || "loom_shuttle".equals(id) || "polished_cog".equals(id))) {
            return 2;
        }
        if (ORIGIN_WILD.equals(s.origin) && "vital_sprout".equals(id)) {
            return 2;
        }
        return 0;
    }

    private static RelicDef randomBossRelic(State s, Set<String> excluded) {
        ArrayList<RelicDef> pool = new ArrayList<>();
        for (RelicDef r : RELIC_LIBRARY) {
            if (r.boss && !s.relics.contains(r.id) && !excluded.contains(r.id)) {
                pool.add(r);
            }
        }
        if (pool.isEmpty()) {
            return randomRelic(s, excluded);
        }
        return pool.get(s.run.nextInt(pool.size()));
    }

    private static void addRelic(State s, String id) {
        RelicDef r = relic(id);
        if (r == null || s.relics.contains(id)) {
            return;
        }
        s.relics.add(id);
        s.seenRelics.add(id);
        if ("ruby_branch".equals(id)) {
            s.maxHp += 8;
            s.hp += 8;
        } else if ("black_bread".equals(id)) {
            s.maxHp += 14;
            s.hp = Math.min(s.maxHp, s.hp + 14);
        } else if ("merchant_key".equals(id)) {
            s.gold += 80;
        } else if ("glass_anvil".equals(id)) {
            upgradeRandomDeckCard(s);
        } else if ("polished_cog".equals(id)) {
            upgradeRandomDeckCard(s);
        } else if ("scar_talisman".equals(id)) {
            s.deck.add(new Card("wound"));
        } else if ("tithe_box".equals(id)) {
            s.gold += 35;
        } else if ("obsidian_core".equals(id)) {
            s.maxHp = Math.max(30, s.maxHp - 10);
            s.hp = Math.min(s.hp, s.maxHp);
        } else if ("runic_shackle".equals(id)) {
            s.gold += 120;
        } else if ("blood_contract".equals(id)) {
            s.maxHp += 18;
            s.hp += 18;
        } else if ("mirror_sun".equals(id)) {
            addStatusCard(s, "daze");
            addStatusCard(s, "daze");
        }
        log(s, "获得遗物：" + r.name);
    }

    private static void upgradeRandomDeckCard(State s) {
        ArrayList<Card> pool = new ArrayList<>();
        for (Card c : s.deck) {
            CardDef d = card(c.id);
            if (d != null && d.type != 3 && !c.upgraded) {
                pool.add(c);
            }
        }
        if (!pool.isEmpty()) {
            pool.get(s.run.nextInt(pool.size())).upgraded = true;
        }
    }

    private static void upgradeRandomHandCard(State s) {
        ArrayList<Card> pool = new ArrayList<>();
        for (Card c : s.hand) {
            CardDef d = card(c.id);
            if (d != null && d.type != 3 && !c.upgraded) {
                pool.add(c);
            }
        }
        if (!pool.isEmpty()) {
            pool.get(s.run.nextInt(pool.size())).upgraded = true;
        }
    }

    private static boolean hasUpgradableDeckCard(State s) {
        for (Card c : s.deck) {
            CardDef d = card(c.id);
            if (d != null && d.type != 3 && !c.upgraded) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRelic(State s, String id) {
        return s.relics.contains(id);
    }

    private static void addStatusCard(State s, String id) {
        s.discard.add(new Card(id));
    }

    private static ArrayList<Enemy> livingEnemies(State s) {
        ArrayList<Enemy> list = new ArrayList<>();
        for (Enemy e : s.enemies) {
            if (e.hp > 0) {
                list.add(e);
            }
        }
        return list;
    }

    private static Enemy firstLiving(State s) {
        for (Enemy e : s.enemies) {
            if (e.hp > 0) {
                return e;
            }
        }
        return null;
    }

    private static boolean allEnemiesDead(State s) {
        for (Enemy e : s.enemies) {
            if (e.hp > 0) {
                return false;
            }
        }
        return true;
    }

    private static Card copyCard(Card c) {
        Card n = new Card(c.id);
        n.upgraded = c.upgraded;
        n.temp = c.temp;
        return n;
    }

    private static void log(State s, String text) {
        s.log.add(text);
        while (s.log.size() > 8) {
            s.log.remove(0);
        }
    }

    private static void seedCards() {
        addCard("strike", "裂击", "通用", 0, 1, 0, 6, 9, 0, 0, "造成6点伤害。", "造成9点伤害。");
        addCard("guard", "护身", "通用", 0, 1, 1, 0, 0, 5, 8, "获得5点格挡。", "获得8点格挡。");
        addCard("wound", "裂伤", "通用", 0, 3, 3, 0, 0, 0, 0, "状态：抽到时占据手牌。", "状态。").exhaust = false;
        addCard("daze", "眩光", "通用", 0, 3, 3, 0, 0, 0, 0, "虚无状态，会被打出消耗。", "虚无状态。").exhaust = true;

        CardDef c;
        c = addCard("quick_cut", "疾切", "通用", 0, 0, 0, 4, 7, 0, 0, "造成4点伤害。抽1张。", "造成7点伤害。抽1张。");
        c.draw = c.drawUp = 1;
        c = addCard("heavy_line", "重线", "通用", 1, 2, 0, 15, 20, 0, 0, "造成15点伤害。", "造成20点伤害。");
        c = addCard("focus_breath", "凝息", "通用", 1, 1, 1, 0, 0, 7, 10, "获得7点格挡，抽1张。", "获得10点格挡，抽1张。");
        c.draw = c.drawUp = 1;
        c = addCard("battle_trance", "战潮", "通用", 1, 0, 2, 0, 0, 0, 0, "抽2张牌。", "抽3张牌。");
        c.draw = 2; c.drawUp = 3;
        c = addCard("clean_arc", "净弧", "通用", 1, 1, 0, 8, 11, 0, 0, "造成8点伤害，施加1层易伤。", "造成11点伤害，施加2层易伤。");
        c.vulnerable = 1;
        c = addCard("double_step", "双步", "通用", 1, 0, 2, 0, 0, 0, 0, "获得1点能量，抽1张。", "获得2点能量，抽1张。");
        c.energyGain = 1; c.energyGainUp = 1; c.draw = c.drawUp = 1; c.exhaust = true;
        c = addCard("patient_cut", "守势切", "通用", 0, 1, 0, 8, 11, 5, 7, "造成8点伤害，获得5点格挡。", "造成11点伤害，获得7点格挡。");
        c = addCard("wide_sweep", "横扫", "通用", 0, 1, 0, 5, 8, 0, 0, "对所有敌人造成5点伤害。", "对所有敌人造成8点伤害。");
        c.aoe = true;
        c = addCard("golden_focus", "金色专注", "通用", 1, 1, 1, 0, 0, 4, 6, "获得4点格挡，抽2张。", "获得6点格挡，抽2张。");
        c.draw = c.drawUp = 2;
        c = addCard("fracture_ray", "裂隙射线", "通用", 1, 1, 0, 9, 13, 0, 0, "造成9点伤害，施加易伤。", "造成13点伤害，施加更多易伤。");
        c.vulnerable = 1;
        c = addCard("last_light", "终灯", "通用", 2, 2, 2, 0, 0, 10, 15, "获得10点格挡，抽2张，获得1能量。", "获得15点格挡，抽2张，获得1能量。");
        c.draw = c.drawUp = 2; c.energyGain = 1;
        c = addCard("ember_barrier", "烬障", "通用", 1, 1, 1, 0, 0, 6, 9, "获得格挡，场上燃灼越多格挡越高。", "更多格挡，燃灼转防御更强。");
        c.burnToBlock = true;
        c = addCard("snare_survey", "缚痕勘察", "通用", 1, 1, 2, 0, 0, 0, 0, "束缚总量越高，额外抽牌。", "最多额外抽3张。");
        c.bindToDraw = true; c.draw = 1; c.drawUp = 1;
        c = addCard("status_spill", "异常扩散", "通用", 1, 1, 2, 0, 0, 0, 0, "把目标的部分燃灼、束缚与易伤扩散给其他敌人。", "扩散更多异常。");
        c.spreadStatus = true; c.targetEnemy = true;
        c = addCard("coin_edge", "金刃", "通用", 1, 1, 0, 6, 9, 0, 0, "造成伤害，金币越多伤害越高。", "更高基础伤害与金币加成。");
        c.goldDamage = true;
        c = addCard("vault_guard", "库藏护符", "通用", 1, 1, 1, 0, 0, 7, 10, "获得格挡，金币越多格挡越高。", "更高基础格挡与金币加成。");
        c.goldBlock = true;

        c = addCard("warden_oath", "坚守誓言", "通用", 0, 1, 1, 0, 0, 10, 14, "获得格挡。守卫：推动护卫计数。", "更多格挡。");
        c.profession = PROF_WARDEN;
        c = addCard("warden_slam", "壁垒重击", "通用", 1, 2, 0, 10, 14, 8, 12, "造成伤害并格挡。若已有格挡，伤害更高。", "更高伤害与格挡。");
        c.profession = PROF_WARDEN; c.blockToDamage = true;
        c = addCard("warden_stand", "不退阵", "通用", 2, 2, 1, 0, 0, 16, 22, "获得大量格挡，抽1张，守势+1。", "更多格挡，守势+2。");
        c.profession = PROF_WARDEN; c.draw = c.drawUp = 1; c.gainSteelEngine = 1;

        c = addCard("duelist_flurry", "连步刺", "通用", 0, 0, 0, 3, 5, 0, 0, "造成伤害，本回合打牌越多越强。", "更高伤害与连击成长。");
        c.profession = PROF_DUELIST; c.comboDamage = 2;
        c = addCard("duelist_feint", "佯攻换位", "通用", 1, 0, 2, 0, 0, 3, 5, "获得格挡，抽2张，消耗。", "更多格挡，抽3张，消耗。");
        c.profession = PROF_DUELIST; c.draw = 2; c.drawUp = 3; c.exhaust = true;
        c = addCard("duelist_finish", "终式", "通用", 2, 1, 0, 8, 12, 0, 0, "造成伤害，本回合打牌越多越强。", "更高伤害与连击成长。");
        c.profession = PROF_DUELIST; c.comboDamage = 4;

        c = addCard("alchemist_mix", "试剂调和", "通用", 0, 1, 2, 0, 0, 4, 6, "获得格挡并调制随机药剂，消耗。", "更多格挡并调制随机药剂，消耗。");
        c.profession = PROF_ALCHEMIST; c.createPotion = true; c.exhaust = true;
        c = addCard("alchemist_cloud", "腐蚀云", "通用", 1, 1, 2, 0, 0, 0, 0, "目标获得燃灼与束缚，并扩散异常。", "更多异常扩散。");
        c.profession = PROF_ALCHEMIST; c.burn = 3; c.burnUp = 5; c.bind = 2; c.bindUp = 3; c.spreadStatus = true; c.targetEnemy = true;
        c = addCard("alchemist_catalyst", "催化剂", "通用", 2, 1, 2, 0, 0, 0, 0, "抽2张，获得燃势与束缚势，消耗。", "抽3张，获得更多势能，消耗。");
        c.profession = PROF_ALCHEMIST; c.draw = 2; c.drawUp = 3; c.gainBurnPower = 1; c.gainBindPower = 1; c.exhaust = true;

        c = addCard("ranger_trap", "踏影陷阱", "通用", 0, 1, 1, 0, 0, 6, 9, "获得格挡，施加束缚。", "更多格挡与束缚。");
        c.profession = PROF_RANGER; c.bind = 2; c.bindUp = 3; c.targetEnemy = true;
        c = addCard("ranger_volley", "猎线齐射", "通用", 1, 1, 0, 5, 8, 0, 0, "对所有敌人造成伤害并施加束缚。", "更高伤害与束缚。");
        c.profession = PROF_RANGER; c.aoe = true; c.bind = 1; c.bindUp = 2;
        c = addCard("ranger_patience", "潜伏耐心", "通用", 2, 1, 2, 0, 0, 0, 0, "束缚总量越高抽牌越多，获得束缚势。", "抽牌上限更高，获得更多束缚势。");
        c.profession = PROF_RANGER; c.bindToDraw = true; c.gainBindPower = 1;

        c = addCard("arcanist_glyph", "秘文摹写", "通用", 0, 0, 2, 0, 0, 0, 0, "制造一张临时疾切，消耗。", "制造一张临时疾切，消耗。");
        c.profession = PROF_ARCANIST; c.createEcho = true; c.echoCardId = "quick_cut"; c.exhaust = true;
        c = addCard("arcanist_rift", "裂门术", "通用", 1, 1, 1, 0, 0, 7, 10, "获得格挡，消耗弃牌堆顶牌，抽2张。", "更多格挡与抽牌。");
        c.profession = PROF_ARCANIST; c.exhaustTopDiscard = true; c.draw = 2; c.drawUp = 3;
        c = addCard("arcanist_loop", "秘环回流", "通用", 2, 1, 2, 0, 0, 0, 0, "回声势+1，抽2张，获得1能量，消耗。", "回声势+2，抽3张，获得1能量，消耗。");
        c.profession = PROF_ARCANIST; c.gainVoidEngine = 1; c.draw = 2; c.drawUp = 3; c.energyGain = 1; c.exhaust = true;

        c = addCard("merchant_haggle", "讨价还价", "通用", 0, 0, 2, 0, 0, 0, 0, "获得金币，抽1张，消耗。", "获得更多金币，抽1张，消耗。");
        c.profession = PROF_MERCHANT; c.goldGain = 12; c.draw = c.drawUp = 1; c.exhaust = true;
        c = addCard("merchant_invest", "风险投资", "通用", 1, 1, 1, 0, 0, 5, 8, "获得格挡和金币，金币越多格挡越高。", "更多格挡与金币。");
        c.profession = PROF_MERCHANT; c.goldGain = 8; c.goldBlock = true;
        c = addCard("merchant_liquidate", "清算", "通用", 2, 2, 0, 10, 15, 0, 0, "造成伤害，金币越多伤害越高。获得金币。", "更高伤害与金币收益。");
        c.profession = PROF_MERCHANT; c.goldDamage = true; c.goldGain = 10;

        c = addCard("blood_pact", "血契刻印", "通用", 0, 1, 0, 9, 13, 0, 0, "失去生命造成伤害。血契者会把失去生命转为额外爆发。", "失去更少生命，造成更高伤害。");
        c.profession = PROF_BLOODBOUND; c.hpLoss = 2; c.vulnerable = 1;
        c = addCard("blood_rite", "裂伤仪式", "通用", 1, 1, 2, 0, 0, 6, 9, "抽牌并加入裂伤；裂伤可推动血契者反击。", "更多格挡与抽牌。");
        c.profession = PROF_BLOODBOUND; c.draw = 2; c.drawUp = 3; c.createWound = true; c.exhaust = true;
        c = addCard("blood_feast", "赤宴", "通用", 2, 2, 0, 12, 18, 0, 0, "造成伤害并治疗，低血线构筑收益更高。", "更高伤害和治疗。");
        c.profession = PROF_BLOODBOUND; c.heal = 4; c.healUp = 7;

        c = addCard("weaver_thread", "织线", "通用", 0, 0, 1, 0, 0, 4, 6, "获得格挡，检视牌库并抽1张。织牌师偏好牌序工程。", "更多格挡，抽2张。");
        c.profession = PROF_WEAVER; c.scry = 3; c.draw = 1; c.drawUp = 2;
        c = addCard("weaver_lattice", "格纹阵", "通用", 1, 1, 1, 0, 0, 8, 12, "获得格挡，升级手中一张牌并抽牌。", "更多格挡与抽牌。");
        c.profession = PROF_WEAVER; c.upgradeRandom = true; c.draw = c.drawUp = 1;
        c = addCard("weaver_pattern", "定式改写", "通用", 2, 1, 2, 0, 0, 0, 0, "制造临时疾切，检视牌库，获得能量，消耗。", "额外抽牌并获得能量。");
        c.profession = PROF_WEAVER; c.createEcho = true; c.echoCardId = "quick_cut"; c.scry = 4; c.energyGain = 1; c.draw = 1; c.drawUp = 2; c.exhaust = true;

        c = addCard("steel_counter", "回锋", ORIGIN_STEEL, 0, 1, 0, 7, 9, 3, 5, "造成7点伤害，获得3点格挡。", "造成9点伤害，获得5点格挡。");
        c = addCard("steel_wall", "铸壁", ORIGIN_STEEL, 0, 1, 1, 0, 0, 9, 12, "获得9点格挡。", "获得12点格挡。");
        c = addCard("steel_bash", "盾压", ORIGIN_STEEL, 1, 2, 0, 10, 14, 8, 11, "造成10点伤害，获得8点格挡。", "造成14点伤害，获得11点格挡。");
        c = addCard("steel_riposte", "逆鳞阵", ORIGIN_STEEL, 1, 1, 0, 0, 0, 6, 8, "获得格挡，并将当前格挡转为伤害。", "更高格挡，并将格挡转为伤害。");
        c.blockToDamage = true;
        c = addCard("steel_fort", "不坠堡垒", ORIGIN_STEEL, 2, 2, 1, 0, 0, 18, 24, "获得18点格挡。格挡随回合微增。", "获得24点格挡。格挡随回合微增。");
        c.retainBlock = true;
        c = addCard("steel_temper", "淬炼", ORIGIN_STEEL, 1, 1, 1, 0, 0, 6, 9, "获得格挡，升级手中一张牌。", "更多格挡，升级手中一张牌。");
        c.upgradeRandom = true;
        c = addCard("steel_spear", "破阵枪", ORIGIN_STEEL, 1, 1, 0, 11, 15, 0, 0, "造成11点伤害，若有10+格挡更强。", "造成15点伤害，若有10+格挡更强。");
        c = addCard("steel_mirror", "镜盾", ORIGIN_STEEL, 2, 1, 1, 0, 0, 11, 15, "获得11点格挡，抽1张。", "获得15点格挡，抽1张。");
        c.draw = c.drawUp = 1;
        c = addCard("steel_anchor", "沉锚", ORIGIN_STEEL, 0, 0, 1, 0, 0, 4, 7, "获得4点格挡。抽1张。", "获得7点格挡。抽1张。");
        c.draw = c.drawUp = 1;
        c = addCard("steel_bulwark", "垒城", ORIGIN_STEEL, 1, 2, 1, 0, 0, 20, 28, "获得20点格挡。", "获得28点格挡。");
        c = addCard("steel_judgment", "裁断盾", ORIGIN_STEEL, 2, 2, 0, 9, 12, 9, 12, "造成伤害，获得格挡，并将格挡转为伤害。", "更高伤害与格挡，并将格挡转为伤害。");
        c.blockToDamage = true;
        c = addCard("steel_relay", "阵线传递", ORIGIN_STEEL, 1, 1, 1, 0, 0, 8, 11, "获得格挡，抽2张。", "更多格挡，抽2张。");
        c.draw = c.drawUp = 2;
        c = addCard("steel_kingline", "王线", ORIGIN_STEEL, 2, 3, 0, 18, 24, 18, 24, "造成18点伤害，获得18点格挡。", "造成24点伤害，获得24点格挡。");
        c = addCard("steel_doctrine", "钢律教条", ORIGIN_STEEL, 2, 1, 2, 0, 0, 8, 12, "获得格挡。守势+1：技能牌触发穿透反击。", "更多格挡。守势+2。");
        c.gainSteelEngine = 1;
        c = addCard("steel_foundry", "活体铸炉", ORIGIN_STEEL, 2, 2, 1, 0, 0, 14, 20, "获得格挡。守势+1。抽1张。", "更多格挡。守势+2。抽1张。");
        c.gainSteelEngine = 1; c.draw = c.drawUp = 1;

        c = addCard("ash_spark", "火星", ORIGIN_ASH, 0, 1, 0, 5, 7, 0, 0, "造成伤害，附加2燃灼。", "造成伤害，附加3燃灼。");
        c.burn = 2; c.burnUp = 3;
        c = addCard("ash_overload", "过载", ORIGIN_ASH, 0, 0, 2, 0, 0, 0, 0, "获得燃势，抽1张。", "获得更多燃势，抽1张。");
        c.gainBurnPower = 1; c.draw = c.drawUp = 1;
        c = addCard("ash_burst", "焚爆", ORIGIN_ASH, 1, 1, 0, 7, 10, 0, 0, "造成伤害，并引爆燃灼。", "更高伤害，引爆燃灼更强。");
        c.detonateBurn = true;
        c = addCard("ash_wave", "赤浪", ORIGIN_ASH, 1, 2, 0, 8, 11, 0, 0, "对所有敌人造成伤害，附加燃灼。", "更高群体伤害与燃灼。");
        c.aoe = true; c.burn = 1; c.burnUp = 2;
        c = addCard("ash_scar", "烬痕", ORIGIN_ASH, 1, 1, 1, 0, 0, 5, 8, "获得格挡与燃势。", "更多格挡与燃势。");
        c.gainBurnPower = 1;
        c = addCard("ash_lance", "熔矛", ORIGIN_ASH, 2, 2, 0, 18, 25, 0, 0, "造成高伤害，附加燃灼。", "造成更高伤害，附加燃灼。");
        c.burn = 3; c.burnUp = 4;
        c = addCard("ash_cycle", "炉心循环", ORIGIN_ASH, 2, 1, 2, 0, 0, 0, 0, "抽2张，获得1能量，消耗。", "抽3张，获得1能量，消耗。");
        c.draw = 2; c.drawUp = 3; c.energyGain = 1; c.exhaust = true;
        c = addCard("ash_rain", "余烬雨", ORIGIN_ASH, 2, 3, 0, 6, 8, 0, 0, "对所有敌人造成伤害三次感。", "更强群体焚烧。");
        c.aoe = true; c.burn = 4; c.burnUp = 6;
        c = addCard("ash_kindling", "添薪", ORIGIN_ASH, 0, 0, 2, 0, 0, 0, 0, "获得燃势，抽1张，消耗。", "获得更多燃势，抽1张，消耗。");
        c.gainBurnPower = 1; c.draw = c.drawUp = 1; c.exhaust = true;
        c = addCard("ash_coil", "火舌缠绕", ORIGIN_ASH, 0, 1, 0, 6, 9, 0, 0, "造成伤害，附加燃灼和易伤。", "更高伤害，更多燃灼和易伤。");
        c.burn = 2; c.burnUp = 4; c.vulnerable = 1;
        c = addCard("ash_smoke", "护炉烟", ORIGIN_ASH, 1, 1, 1, 0, 0, 8, 12, "获得格挡，并给目标附加燃灼。", "更多格挡与燃灼。");
        c.burn = 2; c.burnUp = 3; c.targetEnemy = true;
        c = addCard("ash_inferno", "地脉焚潮", ORIGIN_ASH, 2, 2, 0, 10, 14, 0, 0, "对所有敌人造成伤害并附加燃灼。", "更强群体伤害与燃灼。");
        c.aoe = true; c.burn = 3; c.burnUp = 5;
        c = addCard("ash_backdraft", "倒卷热风", ORIGIN_ASH, 2, 1, 0, 4, 7, 0, 0, "造成伤害，并引爆燃灼。", "更高伤害，引爆燃灼更强。");
        c.detonateBurn = true;
        c = addCard("ash_furnace_oath", "炉誓", ORIGIN_ASH, 2, 1, 2, 0, 0, 0, 0, "热度+1：燃灼牌更强并造成穿透伤害。", "热度+2。");
        c.gainAshEngine = 1;
        c = addCard("ash_phoenix_line", "凤线", ORIGIN_ASH, 2, 2, 0, 12, 16, 0, 0, "造成伤害，附加燃灼。热度+1。", "更高伤害和燃灼。热度+2。");
        c.burn = 4; c.burnUp = 6; c.gainAshEngine = 1;

        c = addCard("wild_root", "缠根", ORIGIN_WILD, 0, 1, 0, 6, 8, 0, 0, "造成伤害，附加2束缚。", "造成伤害，附加3束缚。");
        c.bind = 2; c.bindUp = 3;
        c = addCard("wild_growth", "生长", ORIGIN_WILD, 0, 1, 1, 0, 0, 6, 9, "获得格挡与1点治疗。", "更多格挡与2点治疗。");
        c.heal = 1; c.healUp = 2;
        c = addCard("wild_bloom", "盛放", ORIGIN_WILD, 1, 1, 2, 0, 0, 0, 0, "抽2张，获得束缚势。", "抽3张，获得束缚势。");
        c.draw = 2; c.drawUp = 3; c.gainBindPower = 1;
        c = addCard("wild_thorn", "荆刺", ORIGIN_WILD, 1, 1, 0, 9, 13, 4, 6, "造成伤害并格挡，若敌人被束缚收益更高。", "更高伤害与格挡。");
        c = addCard("wild_sleep", "林眠", ORIGIN_WILD, 1, 2, 1, 0, 0, 13, 18, "获得格挡，治疗4点。", "更多格挡，治疗6点。");
        c.heal = 4; c.healUp = 6;
        c = addCard("wild_snare", "猎藤圈", ORIGIN_WILD, 1, 1, 0, 5, 8, 0, 0, "造成伤害，束缚并易伤。", "更高伤害，更多束缚。");
        c.bind = 2; c.bindUp = 4; c.vulnerable = 1;
        c = addCard("wild_canopy", "冠层", ORIGIN_WILD, 2, 2, 1, 0, 0, 16, 22, "获得大量格挡，抽1张。", "更多格挡，抽1张。");
        c.draw = c.drawUp = 1;
        c = addCard("wild_devour", "绿潮吞没", ORIGIN_WILD, 2, 2, 0, 15, 21, 0, 0, "造成伤害，治疗等同一部分伤害。", "造成更高伤害并治疗。");
        c.heal = 4; c.healUp = 7;
        c = addCard("wild_mend", "芽愈", ORIGIN_WILD, 0, 1, 1, 0, 0, 8, 11, "获得格挡，治疗2点。", "更多格挡，治疗3点。");
        c.heal = 2; c.healUp = 3;
        c = addCard("wild_grip", "绞索藤", ORIGIN_WILD, 0, 1, 1, 0, 0, 6, 8, "获得格挡，附加3束缚。", "更多格挡，附加5束缚。");
        c.bind = 3; c.bindUp = 5; c.targetEnemy = true;
        c = addCard("wild_spiral", "螺旋生息", ORIGIN_WILD, 1, 0, 2, 0, 0, 0, 0, "抽2张，获得束缚势，消耗。", "抽3张，获得束缚势，消耗。");
        c.draw = 2; c.drawUp = 3; c.gainBindPower = 1; c.exhaust = true;
        c = addCard("wild_stag", "鹿角突进", ORIGIN_WILD, 1, 2, 0, 16, 22, 0, 0, "造成16点伤害，治疗3点。", "造成22点伤害，治疗5点。");
        c.heal = 3; c.healUp = 5;
        c = addCard("wild_worldroot", "世界根", ORIGIN_WILD, 2, 3, 1, 0, 0, 24, 32, "获得大量格挡，附加大量束缚。", "更多格挡与束缚。");
        c.bind = 6; c.bindUp = 9; c.targetEnemy = true;
        c = addCard("wild_heartseed", "心种", ORIGIN_WILD, 2, 1, 2, 0, 0, 4, 7, "再生+1：每回合治疗并追加束缚。", "更多格挡。再生+2。");
        c.gainWildEngine = 1;
        c = addCard("wild_green_court", "青庭", ORIGIN_WILD, 2, 2, 1, 0, 0, 12, 16, "获得格挡，治疗。再生+1，抽1张。", "更多格挡治疗。再生+2，抽1张。");
        c.heal = 4; c.healUp = 6; c.gainWildEngine = 1; c.draw = c.drawUp = 1;

        c = addCard("void_draw", "窥隙", ORIGIN_VOID, 0, 0, 2, 0, 0, 0, 0, "抽2张，弃牌堆顶牌消耗。", "抽3张，弃牌堆顶牌消耗。");
        c.draw = 2; c.drawUp = 3; c.exhaustTopDiscard = true;
        c = addCard("void_echo", "回声", ORIGIN_VOID, 0, 1, 2, 0, 0, 0, 0, "复制一张临时回声到手牌。", "复制一张临时回声到手牌。");
        c.createEcho = true; c.echoCardId = "strike";
        c = addCard("void_cut", "空刃", ORIGIN_VOID, 1, 1, 0, 8, 12, 0, 0, "造成伤害，消耗弃牌堆顶牌追加伤害。", "更高伤害，追加更高。");
        c.exhaustForDamage = true;
        c = addCard("void_gate", "折门", ORIGIN_VOID, 1, 1, 1, 0, 0, 7, 10, "获得格挡，抽2张，消耗。", "更多格挡，抽2张，消耗。");
        c.draw = c.drawUp = 2; c.exhaust = true;
        c = addCard("void_mark", "虚印", ORIGIN_VOID, 1, 1, 0, 7, 10, 0, 0, "造成伤害，施加易伤与束缚。", "更高伤害，施加更多控制。");
        c.addStatusToEnemy = true;
        c = addCard("void_sift", "筛影", ORIGIN_VOID, 1, 0, 2, 0, 0, 0, 0, "检视牌库，抽1张，获得1能量。", "检视更多，抽1张，获得1能量。");
        c.scry = 3; c.draw = c.drawUp = 1; c.energyGain = 1;
        c = addCard("void_nova", "无光新星", ORIGIN_VOID, 2, 2, 0, 12, 17, 0, 0, "对所有敌人造成伤害，消耗。", "更强群体伤害，消耗。");
        c.aoe = true; c.exhaust = true;
        c = addCard("void_contract", "空契", ORIGIN_VOID, 2, 1, 2, 0, 0, 0, 0, "失去3生命，抽3张并获得2能量。", "失去3生命，抽4张并获得2能量。");
        c.draw = 3; c.drawUp = 4; c.energyGain = 2; c.hpLoss = 3; c.exhaust = true;
        c = addCard("void_glimpse", "短暂窥见", ORIGIN_VOID, 0, 0, 2, 0, 0, 0, 0, "检视牌库，抽1张。", "检视更多，抽2张。");
        c.scry = 3; c.draw = 1; c.drawUp = 2;
        c = addCard("void_hunger", "空腹", ORIGIN_VOID, 1, 1, 0, 7, 10, 0, 0, "造成伤害，消耗弃牌堆顶牌追加伤害。", "更高伤害与追加伤害。");
        c.exhaustForDamage = true;
        c = addCard("void_clone", "影摹", ORIGIN_VOID, 1, 1, 2, 0, 0, 0, 0, "制造一张临时疾切。", "制造一张临时疾切。");
        c.createEcho = true; c.echoCardId = "quick_cut";
        c = addCard("void_veil", "夜幕帷", ORIGIN_VOID, 1, 1, 1, 0, 0, 9, 13, "获得格挡，抽2张，消耗。", "更多格挡，抽2张，消耗。");
        c.draw = c.drawUp = 2; c.exhaust = true;
        c = addCard("void_singularity", "奇点", ORIGIN_VOID, 2, 3, 0, 18, 25, 0, 0, "对所有敌人造成伤害，施加易伤，消耗。", "更强群体伤害与易伤，消耗。");
        c.aoe = true; c.vulnerable = 1; c.exhaust = true;
        c = addCard("void_paradox", "悖论", ORIGIN_VOID, 2, 1, 2, 0, 0, 0, 0, "回声势+1：消耗或临时牌有概率生成临时疾切。抽2张，消耗。", "回声势+2。抽2张，消耗。");
        c.gainVoidEngine = 1; c.draw = c.drawUp = 2; c.exhaust = true;
        c = addCard("void_orbit", "空轨", ORIGIN_VOID, 2, 2, 2, 0, 0, 6, 9, "获得格挡，制造临时疾切。回声势+1。", "更多格挡。回声势+2。");
        c.createEcho = true; c.echoCardId = "quick_cut"; c.gainVoidEngine = 1;
    }

    private static CardDef addCard(String id, String name, String origin, int rarity, int cost, int type, int damage, int damageUp, int block, int blockUp, String text, String upText) {
        CardDef d = new CardDef();
        d.id = id;
        d.name = name;
        d.origin = origin;
        d.rarity = rarity;
        d.cost = cost;
        d.type = type;
        d.damage = damage;
        d.damageUp = damageUp;
        d.block = block;
        d.blockUp = blockUp;
        d.text = text;
        d.upText = upText;
        d.targetEnemy = type == 0 || damage > 0;
        CARD_LIBRARY.add(d);
        return d;
    }

    private static void seedRelics() {
        addRelicDef("steel_oath", "钢律誓章", "每场战斗首回合获得额外格挡，厚重防御更强。");
        addRelicDef("ember_core", "余烬核心", "每回合获得燃势。");
        addRelicDef("leaf_charm", "森息挂坠", "每回合恢复1点生命。");
        addRelicDef("void_lens", "空镜", "首回合额外抽牌。");
        addRelicDef("amber_quill", "琥珀羽笔", "每回合第一张牌费用-1。");
        addRelicDef("bone_mask", "骨白面具", "战斗开始获得8点格挡。");
        addRelicDef("sapphire_cell", "蓝晶电池", "每回合能量+1。");
        addRelicDef("ink_fountain", "墨泉", "每回合多抽1张。");
        addRelicDef("thorn_ring", "棘环", "打出技能牌时对首个敌人造成3点穿透伤害。");
        addRelicDef("charcoal_sigil", "焦炭印", "打出附带燃灼的牌后获得1能量。");
        addRelicDef("root_drum", "根鼓", "打出束缚牌后抽1张。");
        addRelicDef("empty_coin", "空面币", "消耗牌时获得金币。");
        addRelicDef("opal_scar", "蛋白石裂痕", "每回合第3张牌获得7点格挡。");
        addRelicDef("hunter_mark", "猎痕", "击杀敌人获得额外金币。");
        addRelicDef("cup_of_mist", "雾杯", "战斗胜利恢复4点生命。");
        addRelicDef("cracked_compass", "裂纹罗盘", "卡牌奖励变为4选1。");
        addRelicDef("ruby_branch", "红枝", "最大生命+8。");
        addRelicDef("black_bread", "黑面包", "最大生命+14。");
        addRelicDef("merchant_key", "商钥", "立即获得80金币。");
        addRelicDef("silver_suture", "银缝线", "让低血量局面更容易翻盘。");
        addRelicDef("clock_seed", "钟种", "长期作战时提高资源密度。");
        addRelicDef("storm_shell", "岚壳", "降低多敌战压力。");
        addRelicDef("glass_anvil", "玻璃砧", "升级牌收益更高。");
        addRelicDef("night_map", "夜航图", "地图选择更灵活。");
        addRelicDef("dawn_pin", "晨针", "休息点额外收益。");
        addRelicDef("scarlet_dice", "猩红骰", "奖励更容易出现稀有牌。");
        addRelicDef("iron_tea", "铁茶", "精英战后恢复生命。");
        addRelicDef("deep_totem", "渊图腾", "Boss 战首回合更强。");
        addRelicDef("moon_lantern", "月灯", "让抽牌流派更稳定。");
        addRelicDef("cinder_spoon", "烬匙", "燃灼流派获得额外爆发。");
        addRelicDef("green_bell", "青铃", "束缚流派获得额外续航。");
        addRelicDef("hollow_crown", "空冠", "消耗流派更容易转化为资源。");
        addRelicDef("warden_plate", "守卫胸甲", "技能牌前两次额外获得格挡，首回合获得格挡。");
        addRelicDef("duelist_sash", "决斗绶带", "首回合能量+1；每回合第二张攻击生成临时疾切。");
        addRelicDef("alchemist_case", "炼金匣", "药剂上限+1；药剂额外施加易伤。");
        addRelicDef("ranger_map", "猎路图", "首回合束缚首个敌人；束缚牌额外施加易伤。");
        addRelicDef("arcane_ink", "秘术墨水", "打出消耗牌后获得1能量。");
        addRelicDef("merchant_scale", "行商天平", "商店进一步降价。");
        addRelicDef("scar_talisman", "血疤护符", "获得时加入1张裂伤；裂伤可造成伤害并提供格挡。");
        addRelicDef("loom_shuttle", "织梭", "检视牌库的牌额外提供格挡。");
        addRelicDef("glass_vials", "连锁玻璃瓶", "使用药剂获得能量，并额外施加少量燃灼与束缚。");
        addRelicDef("tempo_metronome", "节拍器", "每回合第4张牌获得能量并触发穿透伤害。");
        addRelicDef("void_abacus", "空算盘", "打出消耗牌获得格挡；每3张消耗/临时牌抽1张。");
        addRelicDef("vital_sprout", "活芽", "治疗牌额外获得格挡，并给首个敌人施加束缚。");
        addRelicDef("tithe_box", "什一匣", "获得时得到金币；金币牌追加穿透伤害。");
        addRelicDef("polished_cog", "抛光齿轮", "获得时升级一张牌；每回合前两张升级牌额外提供格挡。");
        addBossRelicDef("obsidian_core", "黑曜核心", "每回合能量+1。获得时最大生命-10。");
        addBossRelicDef("runic_shackle", "符文镣铐", "卡牌奖励+1，立即获得120金币；每回合少抽1张。");
        addBossRelicDef("blood_contract", "血契杯", "最大生命+18；每场战斗首回合失去2生命并抽2张。");
        addBossRelicDef("void_anchor", "虚空锚", "每场战斗首回合获得一张临时无光新星。");
        addBossRelicDef("mirror_sun", "镜日", "每场战斗首回合能量+2；获得时加入2张眩光。");
    }

    private static void addRelicDef(String id, String name, String text) {
        RelicDef r = new RelicDef();
        r.id = id;
        r.name = name;
        r.text = text;
        RELIC_LIBRARY.add(r);
    }

    private static void addBossRelicDef(String id, String name, String text) {
        RelicDef r = new RelicDef();
        r.id = id;
        r.name = name;
        r.text = text;
        r.boss = true;
        RELIC_LIBRARY.add(r);
    }

    private static void seedPotions() {
        addPotion("blood", "血露", "恢复18点生命。");
        addPotion("fire", "烈焰瓶", "造成24点伤害。");
        addPotion("guard", "护盾瓶", "获得18点格挡。");
        addPotion("draw", "清醒瓶", "抽3张并获得1能量。");
        addPotion("bind", "藤锁瓶", "施加束缚与易伤。");
        addPotion("mirror", "镜雾瓶", "复制弃牌堆顶牌到手牌。");
        addPotion("temper", "淬火瓶", "升级手中至多2张牌。");
        addPotion("surge", "潮涌瓶", "本回合获得3能量。");
        addPotion("ember", "烬雨瓶", "所有敌人获得大量燃灼。");
        addPotion("root", "根牢瓶", "所有敌人获得束缚，并获得格挡。");
        addPotion("exhaust_draw", "空抽瓶", "消耗弃牌堆顶牌，抽4张。");
        addPotion("coin", "金刃瓶", "造成基于金币的伤害，并获得20金币。");
    }

    private static void addPotion(String id, String name, String text) {
        PotionDef p = new PotionDef();
        p.id = id;
        p.name = name;
        p.text = text;
        POTION_LIBRARY.add(p);
    }

    private static void seedBoons() {
        addBoon("gold", "裂隙资粮", "开局获得90金币。");
        addBoon("maxhp", "强健血脉", "最大生命+10。");
        addBoon("upgrade", "旧炉火花", "随机升级2张牌。");
        addBoon("remove", "净牌仪式", "移除一张基础牌。");
        addBoon("rare", "深渊赠牌", "获得一张升级的随机稀有倾向卡牌。");
        addBoon("relic", "遗物低语", "获得一个普通遗物。");
        addBoon("potion", "药剂腰带", "获得2瓶随机药剂。");
        addBoon("risk", "贪婪契印", "获得140金币，并加入1张裂伤。");
    }

    private static void addBoon(String id, String name, String text) {
        BoonDef b = new BoonDef();
        b.id = id;
        b.name = name;
        b.text = text;
        BOON_LIBRARY.add(b);
    }

    private static void seedTalents() {
        addTalent("t_shared_masterwork", "", "匠心牌组", "获得时升级2张牌；之后每幕首场战斗首回合抽1张。");
        addTalent("t_shared_hunter", "", "猎取路线", "精英和Boss额外金币；卡牌奖励更偏向当前职业。");
        addTalent("t_shared_longnight", "", "长夜储备", "每场战斗第4回合获得1能量并抽1张。");
        addTalent("t_warden_bastion", PROF_WARDEN, "不破壁垒", "每场战斗首回合获得守势与额外格挡。");
        addTalent("t_warden_counter", PROF_WARDEN, "反击纪律", "每两张技能牌的反击更强，并额外抽1张。");
        addTalent("t_duelist_tempo", PROF_DUELIST, "快节奏", "每回合前两张0费牌获得额外伤害。");
        addTalent("t_duelist_execution", PROF_DUELIST, "处决窗口", "连击追击对易伤或低生命敌人更强。");
        addTalent("t_alchemist_reserve", PROF_ALCHEMIST, "备用试剂", "战斗开始若药剂未满，补充一瓶随机药剂。");
        addTalent("t_alchemist_plague", PROF_ALCHEMIST, "瘟疫催化", "药剂和异常扩散会额外附加燃灼与束缚。");
        addTalent("t_ranger_quarry", PROF_RANGER, "标记猎物", "首个敌人开局获得易伤与束缚，击杀后转移标记。");
        addTalent("t_ranger_net", PROF_RANGER, "复合陷网", "束缚牌同时提供格挡。");
        addTalent("t_arcanist_rewrite", PROF_ARCANIST, "重写命运", "每场战斗首回合制造临时秘文摹写。");
        addTalent("t_arcanist_overflow", PROF_ARCANIST, "回声溢出", "触发秘术职业回流时额外制造临时疾切。");
        addTalent("t_merchant_interest", PROF_MERCHANT, "复利账本", "进入商店获得金币；金币牌奖励更多金币。");
        addTalent("t_merchant_contract", PROF_MERCHANT, "深渊契据", "Boss后额外获得金币和一张升级的金币牌。");
        addTalent("t_bloodbound_scar", PROF_BLOODBOUND, "猩红旧疤", "获得最大生命；血契自损牌提供格挡，血契反击施加易伤。");
        addTalent("t_bloodbound_feast", PROF_BLOODBOUND, "饥渴回响", "低血线伤害会少量治疗，血契反击治疗更多。");
        addTalent("t_weaver_setup", PROF_WEAVER, "先手织局", "每场战斗首回合额外能量，并获得升级格纹阵。");
        addTalent("t_weaver_mastery", PROF_WEAVER, "定式大师", "升级技能会额外抽牌；织牌师重织触发抽更多牌。");
    }

    private static void addTalent(String id, String profession, String name, String text) {
        TalentDef t = new TalentDef();
        t.id = id;
        t.profession = profession;
        t.name = name;
        t.text = text;
        TALENT_LIBRARY.add(t);
    }

    public static final class State implements Serializable {
        public int mode;
        public int previousMode;
        public String pendingAction = "";
        public String origin = "";
        public String profession = "";
        public String lastRunSummary = "";
        public int ascension;
        public int hp;
        public int maxHp;
        public int gold;
        public int act;
        public int floor;
        public int currentNode;
        public int eventId;
        public int deckView;
        public long rngSeed;
        public transient Random run = new Random();
        public final ArrayList<Card> deck = new ArrayList<>();
        public final ArrayList<Card> hand = new ArrayList<>();
        public final ArrayList<Card> draw = new ArrayList<>();
        public final ArrayList<Card> discard = new ArrayList<>();
        public final ArrayList<Card> exhaust = new ArrayList<>();
        public final ArrayList<Enemy> enemies = new ArrayList<>();
        public final ArrayList<MapNode> map = new ArrayList<>();
        public final ArrayList<RewardCard> cardRewards = new ArrayList<>();
        public final ArrayList<String> relicRewards = new ArrayList<>();
        public final ArrayList<String> boonChoices = new ArrayList<>();
        public ArrayList<String> talentChoices = new ArrayList<>();
        public final ArrayList<String> relics = new ArrayList<>();
        public ArrayList<String> talents = new ArrayList<>();
        public final ArrayList<String> potions = new ArrayList<>();
        public final ArrayList<String> shopCards = new ArrayList<>();
        public final ArrayList<String> shopRelics = new ArrayList<>();
        public final ArrayList<String> shopPotions = new ArrayList<>();
        public final Set<String> seenCards = new HashSet<>();
        public final Set<String> seenRelics = new HashSet<>();
        public final ArrayList<String> log = new ArrayList<>();
        public ArrayList<String> newAchievements = new ArrayList<>();
        public MetaProgress meta = new MetaProgress();
        public boolean cardRewardSkipped;
        public boolean playerTurn;
        public boolean runFinished;
        public boolean questComplete;
        public char combatKind;
        public int encounterModifier;
        public int combatQuest;
        public int questTarget;
        public int questProgress;
        public int turn;
        public int energy;
        public int block;
        public int vulnerable;
        public int nextEnergyPenalty;
        public int burnPower;
        public int bindPower;
        public int steelEngine;
        public int ashEngine;
        public int wildEngine;
        public int voidEngine;
        public int professionCharge;
        public int professionUsedThisTurn;
        public int relicTriggersThisTurn;
        public int cardsPlayedThisTurn;
        public int totalCardsPlayed;

        public void ensureRandom() {
            if (run == null) {
                run = new Random(rngSeed ^ System.nanoTime());
            }
            if (pendingAction == null) {
                pendingAction = "";
            }
            if (origin == null) {
                origin = "";
            }
            if (profession == null) {
                profession = "";
            }
            if (talentChoices == null) {
                talentChoices = new ArrayList<>();
            }
            if (talents == null) {
                talents = new ArrayList<>();
            }
            if (newAchievements == null) {
                newAchievements = new ArrayList<>();
            }
            if (lastRunSummary == null) {
                lastRunSummary = "";
            }
            if (meta == null) {
                meta = new MetaProgress();
            }
            meta.ensure();
        }
    }

    public static final class MetaProgress implements Serializable {
        public int runs;
        public int wins;
        public int highestFloor;
        public int highestDepth;
        public int maxGold;
        public int maxDeck;
        public int questCompletions;
        public int[] professionWins = new int[PROFESSIONS.length];
        public ArrayList<String> achievements = new ArrayList<>();

        public void ensure() {
            if (professionWins == null || professionWins.length < PROFESSIONS.length) {
                int[] next = new int[PROFESSIONS.length];
                if (professionWins != null) {
                    System.arraycopy(professionWins, 0, next, 0, Math.min(professionWins.length, next.length));
                }
                professionWins = next;
            }
            if (achievements == null) {
                achievements = new ArrayList<>();
            }
        }
    }

    public static final class Card implements Serializable {
        public String id;
        public boolean upgraded;
        public boolean temp;

        public Card(String id) {
            this.id = id;
        }
    }

    public static final class CardDef {
        public String id;
        public String name;
        public String origin;
        public String profession = "";
        public int rarity;
        public int cost;
        public int type;
        public int damage;
        public int damageUp;
        public int block;
        public int blockUp;
        public int draw;
        public int drawUp;
        public int burn;
        public int burnUp;
        public int bind;
        public int bindUp;
        public int heal;
        public int healUp;
        public int energyGain;
        public int energyGainUp;
        public int hpLoss;
        public int vulnerable;
        public int gainBurnPower;
        public int gainBindPower;
        public int gainSteelEngine;
        public int gainAshEngine;
        public int gainWildEngine;
        public int gainVoidEngine;
        public int upgradeCostDrop;
        public int scry;
        public int comboDamage;
        public int goldGain;
        public boolean targetEnemy;
        public boolean exhaust;
        public boolean aoe;
        public boolean blockToDamage;
        public boolean detonateBurn;
        public boolean exhaustForDamage;
        public boolean exhaustTopDiscard;
        public boolean createEcho;
        public boolean createPotion;
        public boolean createWound;
        public boolean goldDamage;
        public boolean goldBlock;
        public boolean burnToBlock;
        public boolean bindToDraw;
        public boolean spreadStatus;
        public boolean retainBlock;
        public boolean upgradeRandom;
        public boolean addStatusToEnemy;
        public String echoCardId = "";
        public String text;
        public String upText;
    }

    public static final class Enemy implements Serializable {
        public String name;
        public int hp;
        public int maxHp;
        public int kind;
        public int block;
        public int strength;
        public int intent;
        public int intentValue;
        public int burn;
        public int bind;
        public int vulnerable;
        public int phase;
        public int mark;
        public int thorns;
        public int shieldPulse;
        public int doom;
        public int stolenGold;
        public boolean enraged;
    }

    public static final class MapNode implements Serializable {
        public int floor;
        public int lane;
        public char type;
        public boolean available;
        public final ArrayList<Integer> next = new ArrayList<>();
    }

    public static final class RewardCard implements Serializable {
        public String id;
    }

    public static final class RelicDef implements Serializable {
        public String id;
        public String name;
        public String text;
        public boolean boss;
    }

    public static final class PotionDef implements Serializable {
        public String id;
        public String name;
        public String text;
    }

    public static final class BoonDef implements Serializable {
        public String id;
        public String name;
        public String text;
    }

    public static final class TalentDef implements Serializable {
        public String id;
        public String name;
        public String profession = "";
        public String text;
    }
}
