package com.codex.abyssdeck;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    public static final int MODE_PACT = 15;
    public static final int MODE_SKILL_SPEC = 16;

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
    public static final int QUEST_BREW = 7;
    public static final int QUEST_SKILL = 8;
    public static final int QUEST_ECHO = 9;
    public static final int QUEST_BLOODCOIN = 10;
    public static final int QUEST_FORGE = 11;
    public static final int QUEST_TREASURE = 12;
    public static final int EVENT_COUNT = 15;
    private static final int MILESTONE_GUARD = 1;
    private static final int MILESTONE_COMBO = 1 << 1;
    private static final int MILESTONE_HEX = 1 << 2;
    private static final int MILESTONE_ECHO = 1 << 3;
    private static final int MILESTONE_BLOODCOIN = 1 << 4;
    private static final int MILESTONE_FORGE = 1 << 5;

    public static final int ROUTE_NONE = 0;
    public static final int ROUTE_RICH = 1;
    public static final int ROUTE_DANGER = 2;
    public static final int ROUTE_SECRET = 3;
    public static final int ROUTE_SUPPLY = 4;
    public static final int ROUTE_AMBUSH = 5;
    public static final int ROUTE_FORGE = 6;
    public static final int PROF_SKILL_MAX = 10;
    public static final int PROF_SKILL_OVERLOAD_MAX = 6;
    private static final int BUILD_OVERLOAD = 0;
    private static final int BUILD_ECHO = 1;
    private static final int BUILD_BREW = 2;
    private static final int BUILD_GOLD = 3;
    private static final int BUILD_BLOOD = 4;
    private static final int BUILD_FORGE = 5;
    private static final int BUILD_STATUS = 6;
    private static final int BUILD_CYCLE = 7;
    private static final int BUILD_GUARD = 8;
    private static final String[] BUILD_FOCUS_NAMES = {"过载", "回声", "炼调", "金币", "血契", "工坊", "异常", "循环", "守势"};

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
    public static final String PROF_SUMMONER = "唤灵师";
    public static final String PROF_HEXER = "咒术师";
    public static final String PROF_INSCRIBER = "刻印师";
    public static final String[] PROFESSIONS = {
            PROF_WARDEN, PROF_DUELIST, PROF_ALCHEMIST, PROF_RANGER,
            PROF_ARCANIST, PROF_MERCHANT, PROF_BLOODBOUND, PROF_WEAVER,
            PROF_SUMMONER, PROF_HEXER, PROF_INSCRIBER
    };

    public static final ArrayList<CardDef> CARD_LIBRARY = new ArrayList<>();
    public static final ArrayList<RelicDef> RELIC_LIBRARY = new ArrayList<>();
    public static final ArrayList<PotionDef> POTION_LIBRARY = new ArrayList<>();
    public static final ArrayList<BoonDef> BOON_LIBRARY = new ArrayList<>();
    public static final ArrayList<PactDef> PACT_LIBRARY = new ArrayList<>();
    public static final ArrayList<SkillSpecDef> SKILL_SPEC_LIBRARY = new ArrayList<>();
    public static final ArrayList<TalentDef> TALENT_LIBRARY = new ArrayList<>();

    static {
        seedCards();
        seedRelics();
        seedPotions();
        seedBoons();
        seedPacts();
        seedSkillSpecs();
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
        applyProfessionMastery(s, profession);
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
        } else if ("profession_pack".equals(b.id)) {
            CardDef d = randomProfessionCard(s, true);
            Card c = new Card(d.id);
            c.upgraded = true;
            s.deck.add(c);
            s.gold += 25;
        } else if ("skill_seed".equals(b.id)) {
            CardDef d = randomProfessionCard(s, true);
            s.deck.add(new Card(d.id));
            addRelic(s, randomSkillRelicFor(s));
        } else if ("route_cache".equals(b.id)) {
            addRelic(s, "night_map");
            s.gold += 35;
        } else if ("thin_start".equals(b.id)) {
            removeStarterJunk(s);
            removeStarterJunk(s);
            s.maxHp = Math.max(40, s.maxHp - 4);
            s.hp = Math.min(s.hp, s.maxHp);
        } else if ("brew_start".equals(b.id)) {
            while (s.potions.size() < potionLimit(s)) {
                s.potions.add(POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size())).id);
            }
            s.deck.add(new Card("alchemist_mix"));
        } else if ("rare_relic_risk".equals(b.id)) {
            addRelic(s, randomRelic(s).id);
            addRelic(s, randomRelic(s).id);
            addStatusCard(s, "daze");
        } else if ("forge_start".equals(b.id)) {
            upgradeRandomDeckCard(s);
            upgradeRandomDeckCard(s);
            upgradeRandomDeckCard(s);
            s.gold = Math.max(0, s.gold - 30);
        } else if ("blood_start".equals(b.id)) {
            s.maxHp += 8;
            s.hp = Math.max(1, s.hp - 8);
            Card c = new Card("blood_pact");
            c.upgraded = true;
            s.deck.add(c);
        }
        applyDepthStartPenalty(s);
        s.boonChoices.clear();
        rollPacts(s);
        s.mode = MODE_PACT;
        log(s, "选择赐印：" + b.name);
    }

    public static void choosePact(State s, int index) {
        if (s.mode != MODE_PACT || index < 0 || index >= s.pactChoices.size()) {
            return;
        }
        PactDef p = pact(s.pactChoices.get(index));
        if (p == null) {
            s.mode = MODE_MAP;
            return;
        }
        s.pact = p.id;
        s.pactChoices.clear();
        s.pactFulfilled = 0;
        applyPactStart(s, p.id);
        rollSkillSpecs(s);
        s.mode = MODE_SKILL_SPEC;
        log(s, "立下誓约：" + p.name);
    }

    public static void chooseSkillSpec(State s, int index) {
        if (s.mode != MODE_SKILL_SPEC || index < 0 || index >= s.skillSpecChoices.size()) {
            return;
        }
        SkillSpecDef spec = skillSpec(s.skillSpecChoices.get(index));
        if (spec == null) {
            s.mode = MODE_MAP;
            return;
        }
        s.skillSpec = spec.id;
        s.skillSpecLevel = 1;
        s.skillSpecChoices.clear();
        applySkillSpecPickup(s, spec.id);
        s.mode = MODE_MAP;
        log(s, "职业技专修：" + spec.name);
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
        advanceSkillSpec(s);
        log(s, "领悟专精：" + t.name);
        nextAct(s);
    }

    public static void chooseDepth(State s, int depth) {
        s.ascension = depth;
        s.mode = MODE_ORIGIN;
    }

    public static String depthName(int depth) {
        if (depth >= 10) return "无光";
        if (depth >= 6) return "噩梦";
        if (depth >= 3) return "暗潮";
        return "浅层";
    }

    public static String depthText(int depth) {
        if (depth >= 10) return "敌人更厚；精英和Boss压迫；开局加入眩光与裂伤；休息缩水；商店涨价。";
        if (depth >= 6) return "敌人更厚；精英开局强化；开局加入眩光；休息缩水；商店涨价。";
        if (depth >= 3) return "敌人更厚；精英开局强化；开局加入眩光。";
        return "标准深渊规则。";
    }

    public static String pactName(State s) {
        PactDef p = s == null ? null : pact(s.pact);
        return p == null ? "未立誓" : p.name;
    }

    public static String pactProgressText(State s) {
        PactDef p = s == null ? null : pact(s.pact);
        if (p == null) {
            return "选择誓约后显示构筑目标。";
        }
        return p.name + " " + s.pactFulfilled + "/3";
    }

    public static SkillSpecDef skillSpec(String id) {
        if (id == null) {
            return null;
        }
        for (SkillSpecDef spec : SKILL_SPEC_LIBRARY) {
            if (id.equals(spec.id)) {
                return spec;
            }
        }
        return null;
    }

    public static String skillSpecName(State s) {
        SkillSpecDef spec = s == null ? null : skillSpec(s.skillSpec);
        return spec == null ? "未专修" : spec.name + skillSpecLevelSuffix(s);
    }

    public static String skillSpecText(State s) {
        SkillSpecDef spec = s == null ? null : skillSpec(s.skillSpec);
        if (spec == null) {
            return "选择一条职业技专修后，职业技会获得长期分支效果。";
        }
        return spec.text + skillSpecRankText(s);
    }

    private static String skillSpecLevelSuffix(State s) {
        int level = s == null ? 0 : Math.max(1, s.skillSpecLevel);
        if (level >= 3) return " III";
        if (level == 2) return " II";
        return " I";
    }

    private static String skillSpecRankText(State s) {
        int level = s == null ? 0 : Math.max(1, s.skillSpecLevel);
        if (level >= 3) return " 当前III阶。";
        if (level == 2) return " 当前II阶。";
        return " 当前I阶。";
    }

    private static void applyDepthStartPenalty(State s) {
        if (s.ascension < 3) {
            return;
        }
        addStatusCard(s, "daze");
        if (s.ascension >= 10) {
            addStatusCard(s, "wound");
            log(s, "无光阶层污染牌组：加入眩光与裂伤。");
        } else {
            log(s, depthName(s.ascension) + "阶层污染牌组：加入眩光。");
        }
    }

    private static void applyPactStart(State s, String id) {
        if ("pact_guardian".equals(id)) {
            s.maxHp += 4;
            s.hp += 4;
        } else if ("pact_sprinter".equals(id)) {
            s.gold += 30;
        } else if ("pact_brewer".equals(id)) {
            while (s.potions.size() < Math.min(potionLimit(s), 2)) {
                s.potions.add(POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size())).id);
            }
        } else if ("pact_hunter".equals(id)) {
            Card c = new Card("clean_arc");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("pact_void".equals(id)) {
            Card c = new Card("void_glimpse");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("pact_blood".equals(id)) {
            s.maxHp += 6;
            s.hp += 6;
            s.deck.add(new Card("wound"));
        } else if ("pact_summon".equals(id)) {
            Card c = new Card("echo_bait");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("pact_hex".equals(id)) {
            s.deck.add(new Card("daze"));
            Card c = new Card("cursed_coin");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("pact_forge".equals(id)) {
            upgradeRandomDeckCard(s);
            Card c = new Card("forge_signal");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("pact_merchant".equals(id)) {
            s.gold += 45;
            Card c = new Card("coin_edge");
            c.upgraded = true;
            s.deck.add(c);
        }
    }

    private static boolean pactSucceeded(State s) {
        if ("pact_guardian".equals(s.pact)) {
            return s.pactMaxBlock >= 24 + s.act * 8;
        }
        if ("pact_sprinter".equals(s.pact)) {
            return s.turn <= (s.combatKind == 'B' ? 7 : s.combatKind == 'E' ? 6 : 5)
                    || s.pactMaxCardsTurn >= 6;
        }
        if ("pact_brewer".equals(s.pact)) {
            return s.pactPotionsUsed > 0 || s.burnPower + s.bindPower >= 3;
        }
        if ("pact_hunter".equals(s.pact)) {
            return s.combatKind == 'E' || s.combatKind == 'B' || s.pactKills >= 2;
        }
        if ("pact_void".equals(s.pact)) {
            return s.pactExhaustedCards >= 3 || s.voidEngine >= 2;
        }
        if ("pact_blood".equals(s.pact)) {
            return s.pactSelfDamage >= 3 || s.hp <= s.maxHp / 2;
        }
        if ("pact_summon".equals(s.pact)) {
            return s.pactTempCards >= 5 || s.runEchoMilestone >= 5;
        }
        if ("pact_hex".equals(s.pact)) {
            return s.pactStatusCards >= 3 || s.runHexMilestone >= 1;
        }
        if ("pact_forge".equals(s.pact)) {
            return s.pactForgeCards >= 3 || s.runForgeMilestone >= 4;
        }
        if ("pact_merchant".equals(s.pact)) {
            return s.pactGoldCards >= 2 || s.gold >= 160 + s.act * 20;
        }
        return false;
    }

    private static void awardPact(State s) {
        if (s.pact == null || s.pact.length() == 0 || !pactSucceeded(s)) {
            return;
        }
        PactDef p = pact(s.pact);
        if (s.pactFulfilled >= 3) {
            s.gold += 8 + s.act * 3;
            log(s, "誓约余响：" + (p == null ? "目标" : p.name) + "，获得少量金币。");
            return;
        }
        s.pactFulfilled++;
        int gold = 18 + s.act * 8 + s.pactFulfilled * 5;
        s.gold += gold;
        if ("pact_guardian".equals(s.pact)) {
            s.hp = Math.min(s.maxHp, s.hp + 4 + s.act * 2);
        } else if ("pact_sprinter".equals(s.pact)) {
            upgradeRandomDeckCard(s);
        } else if ("pact_brewer".equals(s.pact)) {
            if (s.potions.size() < potionLimit(s)) {
                PotionDef po = POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size()));
                s.potions.add(po.id);
                log(s, "誓约调和药剂：" + po.name);
            }
        } else if ("pact_hunter".equals(s.pact)) {
            s.gold += s.combatKind == 'E' || s.combatKind == 'B' ? 20 : 8;
        } else if ("pact_void".equals(s.pact)) {
            Card c = randomUpgradeableCard(s);
            if (c != null) {
                c.upgraded = true;
            }
        } else if ("pact_blood".equals(s.pact)) {
            s.maxHp += 2;
            s.hp = Math.min(s.maxHp, s.hp + 6);
        } else if ("pact_summon".equals(s.pact)) {
            if (s.pactFulfilled == 1) {
                Card c = new Card("echo_bait");
                c.upgraded = true;
                s.deck.add(c);
            } else {
                upgradeRandomDeckCard(s);
            }
        } else if ("pact_hex".equals(s.pact)) {
            removeStatusCard(s);
            if (s.pactFulfilled >= 2) {
                Card c = new Card("cursed_coin");
                c.upgraded = true;
                s.deck.add(c);
            }
        } else if ("pact_forge".equals(s.pact)) {
            upgradeRandomDeckCard(s);
            if (s.pactFulfilled >= 2) {
                upgradeRandomDeckCard(s);
            }
        } else if ("pact_merchant".equals(s.pact)) {
            s.gold += 20 + s.act * 5;
            s.hp = Math.min(s.maxHp, s.hp + 3);
        }
        log(s, "完成誓约：" + (p == null ? s.pact : p.name) + " " + s.pactFulfilled + "/3，获得 " + gold + " 金币。");
        if (s.pactFulfilled == 3) {
            awardPactCompletion(s);
        }
    }

    private static void awardPactCompletion(State s) {
        if ("pact_guardian".equals(s.pact)) {
            s.maxHp += 6;
            s.hp = Math.min(s.maxHp, s.hp + 12);
            addUpgradedDeckCard(s, "aegis_engine");
        } else if ("pact_sprinter".equals(s.pact)) {
            addUpgradedDeckCard(s, "cycle_metronome");
            upgradeRandomDeckCard(s);
        } else if ("pact_brewer".equals(s.pact)) {
            addUpgradedDeckCard(s, "brew_crucible");
            while (s.potions.size() < potionLimit(s)) {
                s.potions.add(POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size())).id);
            }
        } else if ("pact_hunter".equals(s.pact)) {
            addUpgradedDeckCard(s, "plague_vector");
            addRelic(s, "hunter_mark");
        } else if ("pact_void".equals(s.pact)) {
            addUpgradedDeckCard(s, "echo_matrix");
            addRelic(s, "hollow_crown");
        } else if ("pact_blood".equals(s.pact)) {
            s.maxHp += 8;
            s.hp = Math.min(s.maxHp, s.hp + 14);
            addUpgradedDeckCard(s, "crimson_loop");
        } else if ("pact_summon".equals(s.pact)) {
            addUpgradedDeckCard(s, "echo_matrix");
            addUpgradedDeckCard(s, "summoner_wisp");
        } else if ("pact_hex".equals(s.pact)) {
            addUpgradedDeckCard(s, "plague_vector");
            removeStatusCard(s);
            removeStatusCard(s);
        } else if ("pact_forge".equals(s.pact)) {
            addUpgradedDeckCard(s, "forge_blueprint");
            upgradeRandomDeckCard(s);
            upgradeRandomDeckCard(s);
        } else if ("pact_merchant".equals(s.pact)) {
            s.gold += 120;
            addUpgradedDeckCard(s, "golden_engine");
        }
        log(s, "誓约圆满：" + pactName(s) + "给予终局馈赠。");
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
        s.currentRoute = n.route;
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
        chargeProfessionSkill(s, d);
        updateQuestProgress(s);
        boolean exhaust = c.temp || d.exhaust;
        applyCard(s, c, d, target);
        triggerAfterPlay(s, c, d);
        trackPactAfterPlay(s, c, d, exhaust);
        trackQuestAfterPlay(s, c, d, exhaust);
        trackRunMilestones(s, c, d, exhaust);
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

    public static boolean professionSkillReady(State s) {
        return s.mode == MODE_COMBAT && s.playerTurn && !s.professionSkillUsedThisTurn
                && s.professionSkillCharge >= PROF_SKILL_MAX && firstLiving(s) != null;
    }

    public static int professionSkillOverload(State s) {
        if (s == null) {
            return 0;
        }
        return Math.max(0, Math.min(PROF_SKILL_OVERLOAD_MAX, s.professionSkillCharge - PROF_SKILL_MAX));
    }

    public static String professionSkillName(String profession) {
        if (PROF_WARDEN.equals(profession)) return "盾阵";
        if (PROF_DUELIST.equals(profession)) return "终步";
        if (PROF_ALCHEMIST.equals(profession)) return "催化";
        if (PROF_RANGER.equals(profession)) return "猎标";
        if (PROF_ARCANIST.equals(profession)) return "回流";
        if (PROF_MERCHANT.equals(profession)) return "投机";
        if (PROF_BLOODBOUND.equals(profession)) return "血誓";
        if (PROF_WEAVER.equals(profession)) return "织局";
        if (PROF_SUMMONER.equals(profession)) return "唤潮";
        if (PROF_HEXER.equals(profession)) return "咒环";
        if (PROF_INSCRIBER.equals(profession)) return "刻痕";
        return "职业技";
    }

    public static String professionSkillText(State s) {
        String profession = s == null ? "" : s.profession;
        String text = professionSkillTextFor(profession);
        int overload = professionSkillOverload(s);
        String resonance = professionSkillResonanceText(s);
        SkillSpecDef specDef = s == null ? null : skillSpec(s.skillSpec);
        String spec = specDef == null ? "" : specDef.text;
        if (overload > 0) {
            return text + " 当前过载+" + overload + "。" + (spec.length() > 0 ? " " + spec : "") + (resonance.length() > 0 ? " " + resonance : "");
        }
        return text + " 满充能后可继续积蓄过载。" + (spec.length() > 0 ? " " + spec : "") + (resonance.length() > 0 ? " " + resonance : "");
    }

    public static String professionSkillResonanceText(State s) {
        if (s == null || skillResonanceTier(s) <= 0) {
            return "";
        }
        int focus = s.buildResonanceFocus;
        String rank = buildFocusRank(s.buildResonanceScore);
        if (focus == BUILD_OVERLOAD) return "技共鸣：" + BUILD_FOCUS_NAMES[focus] + rank + "，返还充能并追加穿透。";
        if (focus == BUILD_ECHO) return "技共鸣：" + BUILD_FOCUS_NAMES[focus] + rank + "，制造临时牌并续抽。";
        if (focus == BUILD_BREW) return "技共鸣：" + BUILD_FOCUS_NAMES[focus] + rank + "，扩散燃灼束缚。";
        if (focus == BUILD_GOLD) return "技共鸣：" + BUILD_FOCUS_NAMES[focus] + rank + "，回收金币并转为格挡。";
        if (focus == BUILD_BLOOD) return "技共鸣：" + BUILD_FOCUS_NAMES[focus] + rank + "，按失血治疗并反击。";
        if (focus == BUILD_FORGE) return "技共鸣：" + BUILD_FOCUS_NAMES[focus] + rank + "，升级手牌并加固。";
        if (focus == BUILD_STATUS) return "技共鸣：" + BUILD_FOCUS_NAMES[focus] + rank + "，放大异常并穿透。";
        if (focus == BUILD_CYCLE) return "技共鸣：" + BUILD_FOCUS_NAMES[focus] + rank + "，抽牌，高阶返能。";
        if (focus == BUILD_GUARD) return "技共鸣：" + BUILD_FOCUS_NAMES[focus] + rank + "，获得格挡并反击。";
        return "";
    }

    public static String professionSkillTextFor(String profession) {
        if (PROF_WARDEN.equals(profession)) return "满充能：获得格挡，按当前格挡穿透反击。";
        if (PROF_DUELIST.equals(profession)) return "满充能：对目标穿透斩击，本回合出牌越多越强。";
        if (PROF_ALCHEMIST.equals(profession)) return "满充能：向敌群施加燃灼与束缚，若药剂未满补1瓶。";
        if (PROF_RANGER.equals(profession)) return "满充能：标记目标，施加束缚与易伤并抽牌。";
        if (PROF_ARCANIST.equals(profession)) return "满充能：抽牌、获得能量并制造临时疾切。";
        if (PROF_MERCHANT.equals(profession)) return "满充能：花少量金币换格挡并用金币打击目标。";
        if (PROF_BLOODBOUND.equals(profession)) return "满充能：失去少量生命，治疗并穿透打击。";
        if (PROF_WEAVER.equals(profession)) return "满充能：升级手牌、抽牌并获得能量。";
        if (PROF_SUMMONER.equals(profession)) return "满充能：召来灵潮，制造临时牌并让敌人承受束缚。";
        if (PROF_HEXER.equals(profession)) return "满充能：施加群体诅咒，污染牌组但抽牌并获得能量。";
        if (PROF_INSCRIBER.equals(profession)) return "满充能：铭刻手牌，施加易伤与束缚，并把状态转为资源。";
        return "选择职业后可用。";
    }

    public static void useProfessionSkill(State s, int enemyIndex) {
        if (!professionSkillReady(s)) {
            return;
        }
        Enemy target = firstLiving(s);
        if (enemyIndex >= 0 && enemyIndex < s.enemies.size() && s.enemies.get(enemyIndex).hp > 0) {
            target = s.enemies.get(enemyIndex);
        }
        int overload = professionSkillOverload(s);
        s.professionSkillCharge = 0;
        s.professionSkillUsedThisTurn = true;
        addQuestProgress(s, QUEST_SKILL, 1);
        if (PROF_WARDEN.equals(s.profession)) {
            gainBlock(s, 10 + s.act * 2 + s.steelEngine + overload * 4);
            damageEnemy(s, target, 8 + Math.min(40, s.block / 2 + overload * 4), true);
        } else if (PROF_DUELIST.equals(s.profession)) {
            int damage = 14 + s.act * 3 + Math.min(24, s.cardsPlayedThisTurn * 3 + overload * 5);
            damageEnemy(s, target, damage, true);
            if (target.hp <= 0) {
                s.energy++;
                draw(s, 1);
            } else if (overload >= 4) {
                draw(s, 1);
            }
        } else if (PROF_ALCHEMIST.equals(s.profession)) {
            for (Enemy e : livingEnemies(s)) {
                e.burn += 3 + s.burnPower + s.act + overload;
                e.bind += 2 + s.bindPower + overload / 2;
                e.vulnerable += 1 + overload / 4;
            }
            if (s.potions.size() < potionLimit(s)) {
                PotionDef p = POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size()));
                s.potions.add(p.id);
                addQuestProgress(s, QUEST_BREW, 1);
                log(s, "职业技调制药剂：" + p.name);
            }
            if (overload >= 4 && s.potions.size() < potionLimit(s)) {
                PotionDef p = POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size()));
                s.potions.add(p.id);
                addQuestProgress(s, QUEST_BREW, 1);
                log(s, "过载调制药剂：" + p.name);
            }
        } else if (PROF_RANGER.equals(s.profession)) {
            target.bind += 5 + s.bindPower + s.act + overload * 2;
            target.vulnerable += 2 + overload / 3;
            target.mark += 2 + overload / 2;
            draw(s, 1 + overload / 4);
        } else if (PROF_ARCANIST.equals(s.profession)) {
            draw(s, 2 + Math.min(2, s.voidEngine) + overload / 3);
            s.energy += 1 + overload / 5;
            Card cut = new Card("quick_cut");
            cut.temp = true;
            addToHand(s, cut);
            if (overload >= 3) {
                Card glyph = new Card("arcanist_glyph");
                glyph.temp = true;
                addToHand(s, glyph);
            }
        } else if (PROF_MERCHANT.equals(s.profession)) {
            int spend = Math.min(s.gold, 18 + s.act * 4 + overload * 5);
            s.gold -= spend;
            gainBlock(s, 8 + spend / 2 + overload * 3);
            damageEnemy(s, target, 10 + spend + overload * 4, false);
        } else if (PROF_BLOODBOUND.equals(s.profession)) {
            int loss = Math.min(4, Math.max(1, s.hp - 1));
            s.hp = Math.max(1, s.hp - loss);
            int missing = Math.max(0, s.maxHp - s.hp);
            damageEnemy(s, target, 12 + s.act * 3 + missing / 5 + overload * 5, true);
            s.hp = Math.min(s.maxHp, s.hp + 5 + overload * 2 + (hasTalent(s, "t_bloodbound_feast") ? 3 : 0));
        } else if (PROF_WEAVER.equals(s.profession)) {
            int upgrades = 2 + overload / 2;
            for (int i = 0; i < upgrades; i++) {
                upgradeRandomHandCard(s);
            }
            draw(s, 1 + overload / 4 + (hasTalent(s, "t_weaver_mastery") ? 1 : 0));
            s.energy += 1 + overload / 5;
        } else if (PROF_SUMMONER.equals(s.profession)) {
            int spirits = 1 + overload / 3;
            for (int i = 0; i < spirits; i++) {
                Card spirit = new Card("summoner_sprite");
                spirit.temp = true;
                addToHand(s, spirit);
            }
            Card echo = new Card(hasTalent(s, "t_summoner_court") ? "summoner_wisp" : "quick_cut");
            echo.temp = true;
            addToHand(s, echo);
            for (Enemy e : livingEnemies(s)) {
                e.bind += 2 + s.bindPower + overload / 2;
            }
            gainBlock(s, 7 + s.act * 2 + Math.min(8, s.professionCharge) + overload * 3);
        } else if (PROF_HEXER.equals(s.profession)) {
            for (Enemy e : livingEnemies(s)) {
                e.vulnerable += 2 + overload / 3;
                e.bind += 2 + s.bindPower + overload / 2;
                e.mark += 1 + overload / 4;
            }
            addStatusCard(s, hasTalent(s, "t_hexer_darkdeal") ? "wound" : "daze");
            if (overload >= 3) {
                addStatusCard(s, "wound");
            }
            draw(s, 2 + overload / 4);
            s.energy += 1 + overload / 5;
        } else if (PROF_INSCRIBER.equals(s.profession)) {
            int upgrades = 1 + overload / 3;
            for (int i = 0; i < upgrades; i++) {
                upgradeRandomHandCard(s);
            }
            for (Enemy e : livingEnemies(s)) {
                e.vulnerable += 1 + overload / 4;
                e.bind += 1 + s.bindPower + overload / 3;
                e.mark += 1 + upgradedCardCount(s) / 12;
            }
            if (target != null) {
                damageEnemy(s, target, 8 + s.act * 2 + Math.min(18, upgradedCardCount(s)) + overload * 3, true);
            }
            draw(s, 1 + overload / 4);
            if (overload >= 3) {
                s.energy++;
            }
            addStatusCard(s, overload >= 4 ? "wound" : "daze");
        }
        applyProfessionSkillResonance(s, target, overload);
        applySkillSpecOnUse(s, target, overload);
        applyProfessionSkillRelics(s, target);
        log(s, "释放职业技：" + professionSkillName(s.profession) + (overload > 0 ? " 过载+" + overload : ""));
        updateQuestProgress(s);
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
        boolean shopScout = shopScoutDraftActive(s);
        boolean restAttune = restAttuneDraftActive(s);
        String id = s.cardRewards.get(index).id;
        s.deck.add(new Card(id));
        s.seenCards.add(id);
        log(s, (shopScout ? "商栈寻路获得：" : restAttune ? "营地调校获得：" : "获得卡牌：") + card(id).name);
        clearRewards(s);
        if (shopScout) {
            s.pendingAction = "";
            s.cardRewardSkipped = false;
            s.mode = MODE_SHOP;
            return;
        }
        if (restAttune) {
            s.pendingAction = "";
            s.cardRewardSkipped = false;
            s.mode = MODE_MAP;
            return;
        }
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
        s.pactPotionsUsed++;
        addQuestProgress(s, QUEST_BREW, 1);
        if (PROF_ALCHEMIST.equals(s.profession)) {
            addProfessionSkillCharge(s, 2);
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
        if (hasTalent(s, "t_shared_apothecary")) {
            draw(s, 1);
            gainBlock(s, 5 + s.act);
        }
        if (hasTalent(s, "t_alchemist_distiller") && target != null) {
            target.vulnerable += 1;
            target.burn += 1 + s.burnPower;
            target.bind += 1 + s.bindPower;
        }
        if (hasTalent(s, "t_alchemist_plague") && target != null) {
            target.burn += 2 + s.burnPower;
            target.bind += 2 + s.bindPower;
        }
        if (hasTalent(s, "t_alchemist_grandbrew") && target != null) {
            target.vulnerable += 1;
            for (Enemy e : livingEnemies(s)) {
                e.burn += 1 + s.burnPower / 2;
                e.bind += 1 + s.bindPower / 2;
            }
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

    public static void shopScoutBuild(State s) {
        if (s.mode != MODE_SHOP || s.shopScoutUsed) {
            return;
        }
        int price = shopServicePrice(s, "shop_scout");
        if (s.gold < price) {
            return;
        }
        s.gold -= price;
        s.cardRewards.clear();
        s.relicRewards.clear();
        s.cardRewardSkipped = false;
        int focus = buildScoutFocus(s);
        int choices = s.currentRoute == ROUTE_SECRET || hasTalent(s, "t_merchant_blackmarket") ? 4 : 3;
        HashSet<String> offered = new HashSet<>();
        for (int i = 0; i < choices; i++) {
            CardDef d = randomBuildScoutCard(s, true, offered);
            if (d == null) {
                break;
            }
            offered.add(d.id);
            RewardCard rc = new RewardCard();
            rc.id = d.id;
            rc.hint = rewardCardHint(s, d);
            s.cardRewards.add(rc);
        }
        if (s.cardRewards.isEmpty()) {
            s.gold += price;
            return;
        }
        s.shopScoutUsed = true;
        s.pendingAction = "shop_scout";
        s.mode = MODE_REWARD;
        log(s, "商栈寻路锁定：" + BUILD_FOCUS_NAMES[focus] + " / " + skillSpecName(s) + "。");
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
        } else if ("event_transform_bonus".equals(action)) {
            Card old = s.deck.get(deckIndex);
            CardDef d = randomCard(s, s.origin, true);
            old.id = d.id;
            old.upgraded = true;
            s.gold += 25;
            log(s, "镜桌重塑为：" + d.name + "，并返还25金币。");
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
        int amount = restHealAmount(s);
        s.hp = Math.min(s.maxHp, s.hp + amount);
        log(s, "营火恢复 " + amount + " 点生命。");
        s.mode = MODE_MAP;
    }

    public static int restHealAmount(State s) {
        int amount = Math.max(18, s.maxHp / 3);
        if (s.ascension >= 10) {
            amount -= 8;
        } else if (s.ascension >= 6) {
            amount -= 5;
        }
        return Math.max(10, amount);
    }

    public static void restChoose(State s, String action) {
        if (s.mode != MODE_REST) {
            return;
        }
        s.pendingAction = action;
        openDeck(s, MODE_REST);
    }

    public static void restAttuneBuild(State s) {
        if (s.mode != MODE_REST) {
            return;
        }
        s.cardRewards.clear();
        s.relicRewards.clear();
        s.cardRewardSkipped = false;
        int focus = buildScoutFocus(s);
        HashSet<String> offered = new HashSet<>();
        int choices = s.currentRoute == ROUTE_FORGE ? 4 : 3;
        for (int i = 0; i < choices; i++) {
            CardDef d = randomBuildScoutCard(s, s.act >= 2 || s.currentRoute == ROUTE_FORGE, offered);
            if (d == null) {
                break;
            }
            offered.add(d.id);
            RewardCard rc = new RewardCard();
            rc.id = d.id;
            rc.hint = rewardCardHint(s, d);
            s.cardRewards.add(rc);
        }
        if (s.cardRewards.isEmpty()) {
            return;
        }
        s.pendingAction = "rest_attune";
        s.mode = MODE_REWARD;
        log(s, "营地调校锁定：" + BUILD_FOCUS_NAMES[focus] + " / " + skillSpecName(s) + "。");
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
        } else if (e == 7) {
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
        } else if (e == 8) {
            if (choice == 0) {
                CardDef d = randomProfessionCard(s, true);
                Card c = new Card(d.id);
                c.upgraded = true;
                s.deck.add(c);
                log(s, "职业导师授予：" + d.name);
            } else {
                s.gold += 35;
                upgradeRandomDeckCard(s);
                log(s, "导师只留下路费和一处关键批注。");
            }
        } else if (e == 9) {
            if (choice == 0) {
                int payout = Math.min(160, 45 + s.gold / 3);
                s.gold += payout;
                addStatusCard(s, "daze");
                log(s, "深渊账本吐出 " + payout + " 金币，也落下一张眩光。");
            } else {
                s.gold = Math.max(0, s.gold - 55);
                s.maxHp += 7;
                s.hp += 7;
                upgradeRandomDeckCard(s);
                log(s, "你赎回旧名，生命与牌面都更稳。");
            }
        } else if (e == 10) {
            if (choice == 0) {
                while (s.potions.size() < potionLimit(s)) {
                    s.potions.add(POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size())).id);
                }
                addStatusCard(s, "wound");
                log(s, "实验台灌满药剂腰带，也留下一道裂伤。");
            } else {
                s.potions.clear();
                CardDef d = randomCard(s, s.origin, true);
                Card c = new Card(d.id);
                c.upgraded = true;
                s.deck.add(c);
                log(s, "你倾空药剂，换来升级牌：" + d.name);
            }
        } else if (e == 11) {
            if (choice == 0) {
                CardDef d = randomCard(s, s.origin, true);
                Card copy = new Card(d.id);
                copy.upgraded = true;
                copy.temp = false;
                s.deck.add(copy);
                addStatusCard(s, "daze");
                log(s, "镜面牌桌复制出升级牌：" + d.name);
            } else {
                s.pendingAction = "event_transform_bonus";
                openDeck(s, MODE_EVENT);
                return;
            }
        } else if (e == 12) {
            if (choice == 0) {
                CardDef d = randomOverloadCard(s, true);
                Card c = new Card(d.id);
                c.upgraded = true;
                s.deck.add(c);
                s.gold = Math.max(0, s.gold - 35);
                log(s, "过载训练完成，获得：" + d.name);
            } else {
                addRelic(s, randomSkillRelicFor(s));
                addStatusCard(s, "daze");
                log(s, "训练场留下职业技遗物，也让牌组沾上眩光。");
            }
        } else if (e == 13) {
            if (choice == 0) {
                CardDef d = randomOffPoolCard(s, true);
                Card c = new Card(d.id);
                c.upgraded = true;
                s.deck.add(c);
                s.maxHp = Math.max(30, s.maxHp - 4);
                s.hp = Math.min(s.hp, s.maxHp);
                log(s, "裂隙交换带来异派升级牌：" + d.name);
            } else {
                addRelic(s, "rift_compass");
                addStatusCard(s, "daze");
                log(s, "你接过裂隙罗盘，混搭路线被打开。");
            }
        } else {
            if (choice == 0) {
                upgradeRandomDeckCard(s);
                upgradeRandomDeckCard(s);
                CardDef d = randomOverloadCard(s, false);
                s.deck.add(new Card(d.id));
                log(s, "工坊合约升级牌组，并交付：" + d.name);
            } else {
                s.gold += 45;
                addStatusCard(s, "wound");
                CardDef d = randomTypeCard(s, 2, true);
                if (d != null) {
                    Card c = new Card(d.id);
                    c.upgraded = true;
                    s.deck.add(c);
                    log(s, "你拿走合约金与升级能力牌：" + d.name);
                }
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

    public static PactDef pact(String id) {
        for (PactDef p : PACT_LIBRARY) {
            if (p.id.equals(id)) {
                return p;
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
        price += depthShopSurcharge(s, price);
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
        price += depthShopSurcharge(s, price);
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
        price += depthShopSurcharge(s, price);
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
        int price = 95;
        if ("shop_remove".equals(action)) {
            price = 85;
        } else if ("shop_upgrade".equals(action)) {
            price = 75;
        } else if ("shop_transform".equals(action)) {
            price = 105;
        } else if ("shop_scout".equals(action)) {
            price = 92;
        }
        price += depthShopSurcharge(s, price);
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

    public static boolean shopScoutDraftActive(State s) {
        return s != null && "shop_scout".equals(s.pendingAction);
    }

    public static boolean restAttuneDraftActive(State s) {
        return s != null && "rest_attune".equals(s.pendingAction);
    }

    public static String shopScoutText(State s) {
        if (s != null && s.shopScoutUsed) {
            return "本商店已完成一次寻路。";
        }
        int focus = buildScoutFocus(s);
        return "寻路偏向：" + BUILD_FOCUS_NAMES[focus] + "，付费后从定向补强牌中三选一。";
    }

    public static String buildSummaryText(State s) {
        if (s == null || s.deck.isEmpty()) {
            return "";
        }
        int[] top = topBuildFocuses(s, 3);
        String summary = "";
        for (int i = 0; i < top.length; i++) {
            int focus = top[i];
            int score = buildScoutFocusScore(s, focus);
            if (score <= 0) {
                continue;
            }
            String part = BUILD_FOCUS_NAMES[focus] + " " + buildFocusRank(score);
            summary += (summary.length() == 0 ? "" : "  ") + part;
        }
        return summary.length() == 0 ? "构筑尚未成型" : summary;
    }

    public static String buildSummaryDetail(State s) {
        if (s == null || s.deck.isEmpty()) {
            return "";
        }
        int[] top = topBuildFocuses(s, 3);
        String detail = "";
        for (int i = 0; i < top.length; i++) {
            int focus = top[i];
            int cards = buildFocusDeckCards(s, focus);
            if (cards <= 0 && buildScoutFocusScore(s, focus) <= 0) {
                continue;
            }
            String part = BUILD_FOCUS_NAMES[focus] + "牌" + cards + "张";
            detail += (detail.length() == 0 ? "" : " / ") + part;
        }
        return detail.length() == 0 ? "多拿带构筑标签的牌会逐步形成方向。" : detail;
    }

    public static String buildResonanceText(State s) {
        if (s == null || s.buildResonanceScore < 35 || s.buildResonanceFocus < 0 || s.buildResonanceFocus >= BUILD_FOCUS_NAMES.length) {
            return "";
        }
        return BUILD_FOCUS_NAMES[s.buildResonanceFocus] + "共鸣 " + buildFocusRank(s.buildResonanceScore);
    }

    public static String talentSynergyHint(State s, String id) {
        TalentDef t = talent(id);
        if (s == null || t == null) {
            return "";
        }
        String tags = "";
        int[] top = topBuildFocuses(s, 3);
        for (int i = 0; i < top.length; i++) {
            int focus = top[i];
            if (talentFocusValue(id, focus) > 0) {
                tags = appendHint(tags, BUILD_FOCUS_NAMES[focus]);
            }
        }
        String hint = tags.length() > 0 ? "契合 " + tags : "";
        if (t.profession.length() > 0 && t.profession.equals(s.profession)) {
            hint = appendHint(hint, "职业专精");
        } else if (t.profession.length() == 0) {
            hint = appendHint(hint, "通用专精");
        }
        if (isAdvancedTalent(id)) {
            hint = appendHint(hint, "进阶");
        }
        String reason = talentReason(s, id);
        if (reason.length() > 0) {
            hint = appendHint(hint, reason);
        }
        return hint.length() == 0 ? "泛用补强" : hint;
    }

    private static int depthShopSurcharge(State s, int basePrice) {
        if (s == null || s.ascension < 6 || basePrice <= 0) {
            return 0;
        }
        float rate = s.ascension >= 10 ? 0.18f : 0.10f;
        return Math.max(s.ascension >= 10 ? 6 : 4, Math.round(basePrice * rate));
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

    public static String routeName(int route) {
        if (route == ROUTE_RICH) return "富矿";
        if (route == ROUTE_DANGER) return "险路";
        if (route == ROUTE_SECRET) return "秘径";
        if (route == ROUTE_SUPPLY) return "补给";
        if (route == ROUTE_AMBUSH) return "伏击";
        if (route == ROUTE_FORGE) return "工坊";
        return "";
    }

    public static String routeShort(int route) {
        if (route == ROUTE_RICH) return "矿";
        if (route == ROUTE_DANGER) return "险";
        if (route == ROUTE_SECRET) return "秘";
        if (route == ROUTE_SUPPLY) return "补";
        if (route == ROUTE_AMBUSH) return "伏";
        if (route == ROUTE_FORGE) return "锻";
        return "";
    }

    public static String routeText(int route) {
        if (route == ROUTE_RICH) return "金币更丰厚，精英/首领更容易掉落遗物。";
        if (route == ROUTE_DANGER) return "敌人更强，但战斗奖励更多，适合强势构筑压榨收益。";
        if (route == ROUTE_SECRET) return "事件更容易遇到稀有分支，奖励和代价都会更极端。";
        if (route == ROUTE_SUPPLY) return "进点获得治疗，商店和战后药剂更宽裕。";
        if (route == ROUTE_AMBUSH) return "首回合承压更高，胜利额外获得金币和卡牌选择。";
        if (route == ROUTE_FORGE) return "营地锻造更强，战斗后有机会自动升级一张牌。";
        return "普通路线。";
    }

    public static int routeColor(int route) {
        if (route == ROUTE_RICH) return 0xffd7b44a;
        if (route == ROUTE_DANGER) return 0xffd45a52;
        if (route == ROUTE_SECRET) return 0xff8b77d9;
        if (route == ROUTE_SUPPLY) return 0xff67b77c;
        if (route == ROUTE_AMBUSH) return 0xffd48655;
        if (route == ROUTE_FORGE) return 0xff77a7c9;
        return 0xff5b6470;
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
        if (quest == QUEST_BREW) return "炼调";
        if (quest == QUEST_SKILL) return "权能";
        if (quest == QUEST_ECHO) return "回声";
        if (quest == QUEST_BLOODCOIN) return "血币";
        if (quest == QUEST_FORGE) return "工坊";
        if (quest == QUEST_TREASURE) return "寻宝";
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
        if (s.combatQuest == QUEST_BREW) return "使用药剂或打出制药牌累计" + s.questTarget + "次。";
        if (s.combatQuest == QUEST_SKILL) return "释放职业技" + s.questTarget + "次。";
        if (s.combatQuest == QUEST_ECHO) return "打出临时牌或消耗牌累计" + s.questTarget + "张。";
        if (s.combatQuest == QUEST_BLOODCOIN) return "打出自损、裂伤或金币牌累计" + s.questTarget + "张。";
        if (s.combatQuest == QUEST_FORGE) return "打出升级、检视或锻造牌累计" + s.questTarget + "张。";
        if (s.combatQuest == QUEST_TREASURE) return "打出金币牌累计" + s.questTarget + "次。";
        return "完成特殊目标。";
    }

    public static String questRewardText(State s) {
        if (s == null || s.combatQuest == QUEST_NONE) return "";
        return "奖励：金币、奖励质量和少量长期记录。";
    }

    public static String milestoneProgressText(State s) {
        if (s == null) {
            return "";
        }
        String done = doneMilestoneText(s);
        if (done.length() > 0) {
            return done;
        }
        String bestName = "铁壁";
        int bestNow = Math.min(2, s.runGuardMilestone);
        int bestTarget = 2;
        int bestScaled = bestNow * 100 / bestTarget;
        int comboNow = Math.min(2, s.runComboMilestone);
        int comboScaled = comboNow * 100 / 2;
        if (comboScaled > bestScaled) {
            bestName = "连打";
            bestNow = comboNow;
            bestTarget = 2;
            bestScaled = comboScaled;
        }
        int hexNow = Math.min(2, s.runHexMilestone);
        int hexScaled = hexNow * 100 / 2;
        if (hexScaled > bestScaled) {
            bestName = "异常";
            bestNow = hexNow;
            bestTarget = 2;
            bestScaled = hexScaled;
        }
        int echoNow = Math.min(12, s.runEchoMilestone);
        int echoScaled = echoNow * 100 / 12;
        if (echoScaled > bestScaled) {
            bestName = "回声";
            bestNow = echoNow;
            bestTarget = 12;
            bestScaled = echoScaled;
        }
        int bloodcoinNow = Math.min(10, s.runBloodcoinMilestone);
        int bloodcoinScaled = bloodcoinNow * 100 / 10;
        if (bloodcoinScaled > bestScaled) {
            bestName = "血币";
            bestNow = bloodcoinNow;
            bestTarget = 10;
            bestScaled = bloodcoinScaled;
        }
        int forgeNow = Math.min(11, s.runForgeMilestone);
        int forgeScaled = forgeNow * 100 / 11;
        if (forgeScaled > bestScaled) {
            bestName = "工坊";
            bestNow = forgeNow;
            bestTarget = 11;
        }
        return "里程碑 " + bestName + " " + bestNow + "/" + bestTarget;
    }

    private static String doneMilestoneText(State s) {
        int flags = s.runMilestoneFlags;
        if (flags == 0) {
            return "";
        }
        String text = "里程碑";
        if ((flags & MILESTONE_GUARD) != 0) text += " 铁壁";
        if ((flags & MILESTONE_COMBO) != 0) text += " 连打";
        if ((flags & MILESTONE_HEX) != 0) text += " 异常";
        if ((flags & MILESTONE_ECHO) != 0) text += " 回声";
        if ((flags & MILESTONE_BLOODCOIN) != 0) text += " 血币";
        if ((flags & MILESTONE_FORGE) != 0) text += " 工坊";
        return text;
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
        if (PROF_SUMMONER.equals(profession)) {
            return "制造临时灵体牌并用束缚保护节奏，召唤越多越能转化为格挡和资源。适合回声、控制和多敌战。";
        }
        if (PROF_HEXER.equals(profession)) {
            return "主动接纳状态牌，把眩光与裂伤转化为诅咒、抽牌和穿透压制。适合高风险控制和消耗构筑。";
        }
        if (PROF_INSCRIBER.equals(profession)) {
            return "用升级牌刻写节奏，把易伤、束缚和状态牌转成充能与资源。适合锻造、异常和过载混合构筑。";
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
        if (PROF_SUMMONER.equals(profession)) {
            return 0xff8fd0ff;
        }
        if (PROF_HEXER.equals(profession)) {
            return 0xffc781d9;
        }
        if (PROF_INSCRIBER.equals(profession)) {
            return 0xff72d7b4;
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

    public static int professionWins(State s, String profession) {
        if (s == null || s.meta == null) {
            return 0;
        }
        s.meta.ensure();
        int index = professionIndex(profession);
        if (index < 0 || index >= s.meta.professionWins.length) {
            return 0;
        }
        return s.meta.professionWins[index];
    }

    public static int professionMasteryLevel(State s, String profession) {
        int wins = professionWins(s, profession);
        if (wins >= 5) {
            return 3;
        }
        if (wins >= 3) {
            return 2;
        }
        if (wins >= 1) {
            return 1;
        }
        return 0;
    }

    public static String professionMasteryName(int level) {
        if (level >= 3) return "宗师";
        if (level == 2) return "熟练";
        if (level == 1) return "入门";
        return "未精通";
    }

    public static String professionMasteryText(State s, String profession) {
        int level = professionMasteryLevel(s, profession);
        if (level >= 3) {
            return "精通3：升级起始牌、职业启动奖励、升级过载牌，每场战斗职业技预充。";
        }
        if (level == 2) {
            return "精通2：升级起始牌，获得职业风格启动奖励，每场战斗职业技预充。";
        }
        if (level == 1) {
            return "精通1：开局升级一张职业起始牌。";
        }
        return "首次胜利后解锁该职业精通奖励。";
    }

    public static String nextProfessionMasteryText(State s, String profession) {
        int wins = professionWins(s, profession);
        if (wins >= 5) return "已满";
        if (wins >= 3) return "5胜升宗师";
        if (wins >= 1) return "3胜升熟练";
        return "1胜入门";
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
        if ("profession_adept".equals(id)) return "一职成名";
        if ("profession_master".equals(id)) return "宗师徽记";
        if ("all_mastery".equals(id)) return "十职熟练";
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
        s.currentRoute = ROUTE_NONE;
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
        s.shopScoutUsed = false;
        s.relicRewards.clear();
        s.boonChoices.clear();
        s.pactChoices.clear();
        s.pact = "";
        s.skillSpec = "";
        s.skillSpecLevel = 0;
        s.skillSpecChoices.clear();
        s.pactFulfilled = 0;
        s.masterySkillCharge = 0;
        s.runGuardMilestone = 0;
        s.runComboMilestone = 0;
        s.runHexMilestone = 0;
        s.runEchoMilestone = 0;
        s.runBloodcoinMilestone = 0;
        s.runForgeMilestone = 0;
        s.runMilestoneFlags = 0;
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
        } else if (PROF_SUMMONER.equals(profession)) {
            s.maxHp += 3;
            s.hp += 3;
            s.deck.add(new Card("summoner_sprite"));
            s.deck.add(new Card("summoner_wisp"));
        } else if (PROF_HEXER.equals(profession)) {
            s.deck.add(new Card("hexer_hexmark"));
            s.deck.add(new Card("daze"));
            s.gold += 25;
        } else if (PROF_INSCRIBER.equals(profession)) {
            s.deck.add(new Card("inscriber_mark"));
            s.deck.add(new Card("inscriber_tablet"));
            upgradeRandomDeckCard(s);
        }
    }

    private static void applyProfessionMastery(State s, String profession) {
        int level = professionMasteryLevel(s, profession);
        if (level <= 0) {
            return;
        }
        upgradeProfessionStarter(s, profession);
        if (level >= 2) {
            applyProfessionMasteryKit(s, profession);
            s.masterySkillCharge = 2;
        }
        if (level >= 3) {
            addUpgradedDeckCard(s, masteryOverloadCard(profession));
            s.masterySkillCharge = 3;
        }
        log(s, profession + "精通" + level + "：" + professionMasteryName(level) + "奖励已生效。");
    }

    private static void upgradeProfessionStarter(State s, String profession) {
        if (PROF_WARDEN.equals(profession)) upgradeDeckCard(s, "warden_oath");
        else if (PROF_DUELIST.equals(profession)) upgradeDeckCard(s, "duelist_flurry");
        else if (PROF_ALCHEMIST.equals(profession)) upgradeDeckCard(s, "alchemist_mix");
        else if (PROF_RANGER.equals(profession)) upgradeDeckCard(s, "ranger_trap");
        else if (PROF_ARCANIST.equals(profession)) upgradeDeckCard(s, "arcanist_glyph");
        else if (PROF_MERCHANT.equals(profession)) upgradeDeckCard(s, "merchant_haggle");
        else if (PROF_BLOODBOUND.equals(profession)) upgradeDeckCard(s, "blood_pact");
        else if (PROF_WEAVER.equals(profession)) upgradeDeckCard(s, "weaver_thread");
        else if (PROF_SUMMONER.equals(profession)) upgradeDeckCard(s, "summoner_wisp");
        else if (PROF_HEXER.equals(profession)) upgradeDeckCard(s, "hexer_hexmark");
        else if (PROF_INSCRIBER.equals(profession)) upgradeDeckCard(s, "inscriber_mark");
    }

    private static void applyProfessionMasteryKit(State s, String profession) {
        if (PROF_WARDEN.equals(profession)) {
            s.maxHp += 4;
            s.hp += 4;
            upgradeRandomDeckCard(s);
        } else if (PROF_DUELIST.equals(profession)) {
            addUpgradedDeckCard(s, "quick_cut");
            s.gold += 15;
        } else if (PROF_ALCHEMIST.equals(profession)) {
            while (s.potions.size() < Math.min(potionLimit(s), 3)) {
                s.potions.add(POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size())).id);
            }
        } else if (PROF_RANGER.equals(profession)) {
            addUpgradedDeckCard(s, "snare_survey");
            s.gold += 20;
        } else if (PROF_ARCANIST.equals(profession)) {
            addUpgradedDeckCard(s, "void_glimpse");
        } else if (PROF_MERCHANT.equals(profession)) {
            s.gold += 55;
        } else if (PROF_BLOODBOUND.equals(profession)) {
            s.maxHp += 5;
            s.hp += 5;
            addUpgradedDeckCard(s, "blood_rite");
        } else if (PROF_WEAVER.equals(profession)) {
            upgradeRandomDeckCard(s);
            upgradeRandomDeckCard(s);
        } else if (PROF_SUMMONER.equals(profession)) {
            addUpgradedDeckCard(s, "summoner_sprite");
        } else if (PROF_HEXER.equals(profession)) {
            addUpgradedDeckCard(s, "hexer_pact");
            removeStatusCard(s);
        } else if (PROF_INSCRIBER.equals(profession)) {
            addUpgradedDeckCard(s, "inscriber_tablet");
            upgradeRandomDeckCard(s);
        }
    }

    private static String masteryOverloadCard(String profession) {
        if (PROF_WARDEN.equals(profession)) return "warden_overguard";
        if (PROF_DUELIST.equals(profession)) return "duelist_overtempo";
        if (PROF_ALCHEMIST.equals(profession)) return "alchemist_overbrew";
        if (PROF_RANGER.equals(profession)) return "ranger_overmark";
        if (PROF_ARCANIST.equals(profession)) return "arcanist_overflow";
        if (PROF_MERCHANT.equals(profession)) return "merchant_overdraft";
        if (PROF_BLOODBOUND.equals(profession)) return "blood_overflow";
        if (PROF_WEAVER.equals(profession)) return "weaver_overthread";
        if (PROF_SUMMONER.equals(profession)) return "summoner_overcall";
        if (PROF_HEXER.equals(profession)) return "hexer_overcurse";
        if (PROF_INSCRIBER.equals(profession)) return "inscriber_overseal";
        return "forge_signal";
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
        } else if ("t_shared_wayfarer".equals(id)) {
            s.gold += 40;
            s.maxHp += 4;
            s.hp += 4;
        } else if ("t_shared_apothecary".equals(id)) {
            while (s.potions.size() < potionLimit(s)) {
                s.potions.add(POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size())).id);
            }
        } else if ("t_warden_bastion".equals(id)) {
            s.maxHp += 8;
            s.hp += 8;
        } else if ("t_warden_counter".equals(id)) {
            Card c = new Card("warden_slam");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_warden_armory".equals(id)) {
            Card c = new Card("steel_bulwark");
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
        } else if ("t_duelist_gambit".equals(id)) {
            Card c = new Card("duelist_feint");
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
        } else if ("t_alchemist_distiller".equals(id)) {
            Card c = new Card("alchemist_catalyst");
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
        } else if ("t_ranger_wildpath".equals(id)) {
            Card c = new Card("ranger_patience");
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
        } else if ("t_arcanist_archive".equals(id)) {
            Card c = new Card("void_paradox");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_merchant_interest".equals(id)) {
            s.gold += 100;
        } else if ("t_merchant_contract".equals(id)) {
            s.gold += 120;
            Card c = new Card("merchant_liquidate");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_merchant_blackmarket".equals(id)) {
            s.gold += 70;
            addRelic(s, randomRelic(s).id);
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
        } else if ("t_bloodbound_crimson".equals(id)) {
            Card c = new Card("blood_pact");
            c.upgraded = true;
            s.deck.add(c);
            addStatusCard(s, "wound");
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
        } else if ("t_weaver_quicksilver".equals(id)) {
            Card c = new Card("weaver_thread");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_summoner_court".equals(id)) {
            Card c = new Card("summoner_court");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_summoner_bond".equals(id)) {
            s.maxHp += 6;
            s.hp += 6;
            Card c = new Card("summoner_guardian");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_summoner_swarm".equals(id)) {
            Card c = new Card("summoner_wisp");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_hexer_darkdeal".equals(id)) {
            s.gold += 80;
            Card c = new Card("hexer_pact");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_hexer_malediction".equals(id)) {
            Card c = new Card("hexer_maledict");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_hexer_cleanse".equals(id)) {
            removeStatusCard(s);
            Card c = new Card("hexer_purge");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("t_inscriber_rubbing".equals(id)) {
            addUpgradedDeckCard(s, "inscriber_tablet");
            upgradeRandomDeckCard(s);
        } else if ("t_inscriber_etching".equals(id)) {
            addUpgradedDeckCard(s, "inscriber_glyphstorm");
            addStatusCard(s, "daze");
        } else if ("t_inscriber_archive".equals(id)) {
            addUpgradedDeckCard(s, "inscriber_palimp");
            removeStatusCard(s);
        } else if ("t_warden_vanguard".equals(id)) {
            s.maxHp += 5;
            s.hp += 5;
            addUpgradedDeckCard(s, "warden_command");
        } else if ("t_duelist_masterstep".equals(id)) {
            addUpgradedDeckCard(s, "duelist_flashstep");
        } else if ("t_alchemist_grandbrew".equals(id)) {
            addUpgradedDeckCard(s, "alchemist_reactor");
            if (s.potions.size() < potionLimit(s)) {
                s.potions.add(POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size())).id);
            }
        } else if ("t_ranger_apex".equals(id)) {
            addUpgradedDeckCard(s, "ranger_killzone");
        } else if ("t_arcanist_singularity".equals(id)) {
            addUpgradedDeckCard(s, "arcanist_convergence");
        } else if ("t_merchant_monopoly".equals(id)) {
            s.gold += 90;
            addUpgradedDeckCard(s, "merchant_speculate");
        } else if ("t_bloodbound_hemocraft".equals(id)) {
            s.maxHp += 5;
            s.hp += 5;
            addUpgradedDeckCard(s, "blood_sigilstorm");
        } else if ("t_weaver_grandpattern".equals(id)) {
            upgradeRandomDeckCard(s);
            addUpgradedDeckCard(s, "weaver_overpattern");
        } else if ("t_summoner_overflow".equals(id)) {
            addUpgradedDeckCard(s, "summoner_court");
        } else if ("t_hexer_abysscurse".equals(id)) {
            addUpgradedDeckCard(s, "hexer_maledict");
            addStatusCard(s, "daze");
        } else if ("t_inscriber_grandcodex".equals(id)) {
            s.maxHp += 4;
            s.hp += 4;
            addUpgradedDeckCard(s, "inscriber_codex");
            upgradeRandomDeckCard(s);
        }
    }

    private static void addUpgradedDeckCard(State s, String id) {
        Card c = new Card(id);
        c.upgraded = true;
        s.deck.add(c);
    }

    private static boolean isAdvancedTalent(String id) {
        return "t_warden_vanguard".equals(id) || "t_duelist_masterstep".equals(id)
                || "t_alchemist_grandbrew".equals(id) || "t_ranger_apex".equals(id)
                || "t_arcanist_singularity".equals(id) || "t_merchant_monopoly".equals(id)
                || "t_bloodbound_hemocraft".equals(id) || "t_weaver_grandpattern".equals(id)
                || "t_summoner_overflow".equals(id) || "t_hexer_abysscurse".equals(id)
                || "t_inscriber_grandcodex".equals(id);
    }

    private static boolean isCapstoneCard(String id) {
        return "warden_aegisline".equals(id) || "duelist_bladesong".equals(id)
                || "alchemist_sunsteel".equals(id) || "ranger_predator".equals(id)
                || "arcanist_eventhorizon".equals(id) || "merchant_kingmaker".equals(id)
                || "blood_apotheosis".equals(id) || "weaver_clockwork".equals(id)
                || "summoner_procession".equals(id) || "hexer_crownfall".equals(id)
                || "inscriber_codex".equals(id);
    }

    private static boolean isCapstoneRelic(String id) {
        return "aegis_throne".equals(id) || "finale_rapier".equals(id)
                || "solar_crucible".equals(id) || "apex_compass".equals(id)
                || "singularity_orb".equals(id) || "kingmaker_seal".equals(id)
                || "blood_crown".equals(id) || "clockwork_loom".equals(id)
                || "spirit_processional".equals(id) || "fallen_crown".equals(id)
                || "living_codex".equals(id);
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

    private static void rollPacts(State s) {
        s.pactChoices.clear();
        HashSet<String> offered = new HashSet<>();
        for (int i = 0; i < 3 && i < PACT_LIBRARY.size(); i++) {
            PactDef p;
            do {
                p = PACT_LIBRARY.get(s.run.nextInt(PACT_LIBRARY.size()));
            } while (offered.contains(p.id));
            offered.add(p.id);
            s.pactChoices.add(p.id);
        }
    }

    private static void rollSkillSpecs(State s) {
        s.skillSpecChoices.clear();
        HashSet<String> offered = new HashSet<>();
        for (int i = 0; i < 3 && i < SKILL_SPEC_LIBRARY.size(); i++) {
            SkillSpecDef spec;
            do {
                spec = SKILL_SPEC_LIBRARY.get(s.run.nextInt(SKILL_SPEC_LIBRARY.size()));
            } while (offered.contains(spec.id));
            offered.add(spec.id);
            s.skillSpecChoices.add(spec.id);
        }
    }

    private static void applySkillSpecPickup(State s, String id) {
        if ("spec_burst".equals(id)) {
            s.masterySkillCharge = Math.max(s.masterySkillCharge, 2);
            s.gold += 18;
        } else if ("spec_tempo".equals(id)) {
            Card c = new Card("quick_cut");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("spec_sustain".equals(id)) {
            s.maxHp += 5;
            s.hp += 5;
        } else if ("spec_resonance".equals(id)) {
            Card c = new Card("overload_conduit");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("spec_mastery".equals(id)) {
            addUpgradedDeckCard(s, masteryOverloadCard(s.profession));
        }
    }

    private static void advanceSkillSpec(State s) {
        SkillSpecDef spec = skillSpec(s.skillSpec);
        if (spec == null || s.skillSpecLevel >= 3) {
            return;
        }
        s.skillSpecLevel = Math.max(1, s.skillSpecLevel) + 1;
        if ("spec_burst".equals(spec.id)) {
            s.masterySkillCharge = Math.max(s.masterySkillCharge, 1 + s.skillSpecLevel);
            Card c = new Card("heavy_line");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("spec_tempo".equals(spec.id)) {
            Card c = new Card("double_step");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("spec_sustain".equals(spec.id)) {
            s.maxHp += 4 + s.skillSpecLevel;
            s.hp += 4 + s.skillSpecLevel;
        } else if ("spec_resonance".equals(spec.id)) {
            CardDef d = buildFocusRewardCard(s);
            if (d != null) {
                Card c = new Card(d.id);
                c.upgraded = true;
                s.deck.add(c);
            }
        } else if ("spec_mastery".equals(spec.id)) {
            Card c = new Card(masteryOverloadCard(s.profession));
            c.upgraded = true;
            s.deck.add(c);
            upgradeRandomDeckCard(s);
        }
        log(s, "职业技专修晋阶：" + spec.name + " " + skillSpecLevelSuffix(s));
    }

    private static CardDef buildFocusRewardCard(State s) {
        int focus = buildScoutFocus(s);
        if (focus == BUILD_OVERLOAD) return card("overload_conduit");
        if (focus == BUILD_ECHO) return card("echo_matrix");
        if (focus == BUILD_BREW) return card("brew_crucible");
        if (focus == BUILD_GOLD) return card("golden_engine");
        if (focus == BUILD_BLOOD) return card("crimson_loop");
        if (focus == BUILD_FORGE) return card("forge_blueprint");
        if (focus == BUILD_STATUS) return card("plague_vector");
        if (focus == BUILD_CYCLE) return card("cycle_metronome");
        if (focus == BUILD_GUARD) return card("aegis_engine");
        return randomOverloadCard(s, true);
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

    private static void removeStatusCard(State s) {
        for (int i = 0; i < s.deck.size(); i++) {
            String id = s.deck.get(i).id;
            if ("wound".equals(id) || "daze".equals(id)) {
                s.deck.remove(i);
                return;
            }
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
                n.route = routeForNode(s, n.type, f, floors);
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

    private static int routeForNode(State s, char type, int floor, int floors) {
        if (type == 'B') {
            return s.run.nextInt(100) < 38 ? ROUTE_DANGER : ROUTE_NONE;
        }
        if (floor <= 1 && s.run.nextInt(100) < 45) {
            return ROUTE_SUPPLY;
        }
        int chance = 34 + Math.min(12, s.act * 4);
        if (type == 'E') {
            chance += 12;
        } else if (type == '?' || type == '$' || type == 'R') {
            chance += 6;
        }
        if (s.run.nextInt(100) >= chance) {
            return ROUTE_NONE;
        }
        if (type == '$') {
            int[] routes = {ROUTE_RICH, ROUTE_SUPPLY, ROUTE_SECRET};
            return routes[s.run.nextInt(routes.length)];
        }
        if (type == 'R') {
            int[] routes = {ROUTE_FORGE, ROUTE_SUPPLY, ROUTE_SECRET};
            return routes[s.run.nextInt(routes.length)];
        }
        if (type == '?') {
            int[] routes = {ROUTE_SECRET, ROUTE_RICH, ROUTE_SUPPLY, ROUTE_FORGE};
            return routes[s.run.nextInt(routes.length)];
        }
        if (type == 'E') {
            int[] routes = {ROUTE_DANGER, ROUTE_RICH, ROUTE_AMBUSH, ROUTE_FORGE};
            return routes[s.run.nextInt(routes.length)];
        }
        int[] routes = {ROUTE_RICH, ROUTE_DANGER, ROUTE_SECRET, ROUTE_SUPPLY, ROUTE_AMBUSH, ROUTE_FORGE};
        return routes[s.run.nextInt(routes.length)];
    }

    private static void applyRouteArrival(State s, char type) {
        if (s.currentRoute == ROUTE_SUPPLY) {
            int heal = 3 + s.act;
            s.hp = Math.min(s.maxHp, s.hp + heal);
            log(s, "补给路线恢复 " + heal + " 生命。");
            if (type == '$') {
                s.gold += 8 + s.act * 3;
                log(s, "补给商队赠予少量裂币。");
            }
        } else if (s.currentRoute == ROUTE_RICH && type != 'C' && type != 'E' && type != 'B') {
            int gold = 10 + s.act * 4;
            s.gold += gold;
            log(s, "富矿路线搜得 " + gold + " 金币。");
        } else if (s.currentRoute == ROUTE_SECRET && type == '?') {
            s.gold += 6 + s.act * 2;
            log(s, "秘径线索让事件前多获得一些筹码。");
        }
    }

    private static void applyRouteCombatStart(State s) {
        if (s.currentRoute == ROUTE_DANGER) {
            for (Enemy e : s.enemies) {
                int extra = 4 + s.act * 3;
                e.maxHp += extra;
                e.hp += extra;
                e.strength += 1;
            }
            log(s, "险路：敌人生命和力量提升。");
        } else if (s.currentRoute == ROUTE_AMBUSH) {
            s.vulnerable += 1;
            for (Enemy e : s.enemies) {
                e.block += 5 + s.act * 2;
            }
            log(s, "伏击：你开局易伤，敌人带着护甲。");
        } else if (s.currentRoute == ROUTE_SUPPLY && s.potions.size() < potionLimit(s) && s.run.nextInt(100) < 18) {
            PotionDef p = POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size()));
            s.potions.add(p.id);
            log(s, "补给箱翻出药剂：" + p.name);
        }
    }

    private static void applyRouteFirstTurn(State s) {
        if (s.turn != 1) {
            return;
        }
        if (s.currentRoute == ROUTE_FORGE) {
            s.energy++;
            draw(s, 1);
            log(s, "工坊路线让首回合能量+1并抽牌。");
        } else if (s.currentRoute == ROUTE_SECRET && s.run.nextInt(100) < 35) {
            Card echo = new Card("void_echo");
            echo.temp = true;
            addToHand(s, echo);
            log(s, "秘径回声制造了一张临时回响。");
        }
    }

    private static int routeRewardCards(State s) {
        if (s.currentRoute == ROUTE_DANGER || s.currentRoute == ROUTE_AMBUSH) {
            return 1;
        }
        if (s.currentRoute == ROUTE_SECRET && s.combatKind != 'C') {
            return 1;
        }
        return 0;
    }

    private static boolean routeAllowsRare(State s) {
        return s.currentRoute == ROUTE_DANGER || s.currentRoute == ROUTE_SECRET || s.currentRoute == ROUTE_AMBUSH;
    }

    private static int routeGoldBonus(State s) {
        if (s.currentRoute == ROUTE_RICH) return 16 + s.act * 7;
        if (s.currentRoute == ROUTE_DANGER) return 12 + s.act * 5;
        if (s.currentRoute == ROUTE_AMBUSH) return 10 + s.act * 4;
        if (s.currentRoute == ROUTE_SECRET && s.combatKind != 'C') return 12 + s.act * 4;
        return 0;
    }

    private static int routeRelicChanceBonus(State s) {
        if (s.currentRoute == ROUTE_RICH) return 12;
        if (s.currentRoute == ROUTE_DANGER) return 8;
        if (s.currentRoute == ROUTE_SECRET && s.combatKind != 'C') return 8;
        return 0;
    }

    private static void maybeRouteUpgrade(State s) {
        if (s.currentRoute == ROUTE_FORGE && s.run.nextInt(100) < 28) {
            Card c = randomUpgradeableCard(s);
            if (c != null) {
                c.upgraded = true;
                log(s, "工坊余火升级了：" + card(c.id).name);
            }
        }
    }

    private static Card randomUpgradeableCard(State s) {
        ArrayList<Card> pool = new ArrayList<>();
        for (Card c : s.deck) {
            CardDef d = card(c.id);
            if (d != null && d.type != 3 && !c.upgraded) {
                pool.add(c);
            }
        }
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(s.run.nextInt(pool.size()));
    }

    private static void chargeProfessionSkill(State s, CardDef d) {
        int amount = 1;
        if (d != null && d.profession.equals(s.profession)) {
            amount++;
        } else if (PROF_WARDEN.equals(s.profession) && d != null && (d.type == 1 || d.block > 0 || d.blockToDamage)) amount++;
        else if (PROF_DUELIST.equals(s.profession) && d != null && (d.type == 0 || d.cost == 0 || d.comboDamage > 0)) amount++;
        else if (PROF_ALCHEMIST.equals(s.profession) && d != null && (d.burn > 0 || d.bind > 0 || d.createPotion || d.spreadStatus)) amount++;
        else if (PROF_RANGER.equals(s.profession) && d != null && (d.bind > 0 || d.aoe || d.bindToDraw)) amount++;
        else if (PROF_ARCANIST.equals(s.profession) && d != null && (d.exhaust || d.createEcho || d.exhaustTopDiscard || d.exhaustForDamage)) amount++;
        else if (PROF_MERCHANT.equals(s.profession) && d != null && (d.goldGain > 0 || d.goldDamage || d.goldBlock)) amount++;
        else if (PROF_BLOODBOUND.equals(s.profession) && d != null && (d.hpLoss > 0 || d.heal > 0 || "wound".equals(d.id))) amount++;
        else if (PROF_WEAVER.equals(s.profession) && d != null && (d.scry > 0 || d.upgradeRandom || d.draw > 0 || d.createEcho)) amount++;
        else if (PROF_SUMMONER.equals(s.profession) && d != null && (d.createEcho || d.bind > 0 || d.aoe || d.type == 1)) amount++;
        else if (PROF_HEXER.equals(s.profession) && d != null && (d.vulnerable > 0 || d.addStatusToEnemy || d.createWound || "wound".equals(d.id) || "daze".equals(d.id))) amount++;
        else if (PROF_INSCRIBER.equals(s.profession) && d != null && (d.upgradeRandom || d.scry > 0 || d.vulnerable > 0 || d.bind > 0
                || d.addStatusToEnemy || d.createWound || d.skillChargeGain > 0 || "wound".equals(d.id) || "daze".equals(d.id))) amount++;
        addProfessionSkillCharge(s, amount);
    }

    private static void applyProfessionSkillResonance(State s, Enemy chosenTarget, int overload) {
        int tier = skillResonanceTier(s);
        if (tier <= 0) {
            return;
        }
        int focus = s.buildResonanceFocus;
        Enemy target = chosenTarget != null && chosenTarget.hp > 0 ? chosenTarget : firstLiving(s);
        if (focus == BUILD_OVERLOAD) {
            addProfessionSkillCharge(s, 1 + tier + overload / 3);
            if (target != null) {
                damageEnemy(s, target, 5 + tier * 4 + overload * 2, true);
            }
        } else if (focus == BUILD_ECHO) {
            Card echo = new Card(PROF_SUMMONER.equals(s.profession) ? "summoner_sprite" : "quick_cut");
            echo.temp = true;
            addToHand(s, echo);
            if (tier >= 2) {
                Card glimpse = new Card("void_glimpse");
                glimpse.temp = true;
                addToHand(s, glimpse);
            }
            if (tier >= 3) {
                draw(s, 1);
            }
        } else if (focus == BUILD_BREW) {
            for (Enemy e : livingEnemies(s)) {
                e.burn += 1 + tier + s.burnPower / 2;
                e.bind += tier + s.bindPower / 2;
                if (tier >= 3) {
                    e.vulnerable++;
                }
            }
            addQuestProgress(s, QUEST_BREW, 1);
        } else if (focus == BUILD_GOLD) {
            int refund = 5 + tier * 5 + Math.min(12, s.gold / 60) + overload;
            s.gold += refund;
            gainBlock(s, 4 + tier * 3 + Math.min(8, s.gold / 90));
            if (target != null && tier >= 2) {
                damageEnemy(s, target, 4 + tier * 3 + Math.min(10, refund / 2), false);
            }
        } else if (focus == BUILD_BLOOD) {
            int missing = Math.max(0, s.maxHp - s.hp);
            int heal = 2 + tier * 2 + Math.min(8, missing / 8) + overload / 2;
            s.hp = Math.min(s.maxHp, s.hp + heal);
            if (target != null) {
                damageEnemy(s, target, 5 + tier * 4 + missing / 8 + overload * 2, true);
            }
        } else if (focus == BUILD_FORGE) {
            upgradeRandomHandCard(s);
            if (tier >= 3) {
                upgradeRandomHandCard(s);
            }
            gainBlock(s, 5 + tier * 3);
            addQuestProgress(s, QUEST_FORGE, 1);
        } else if (focus == BUILD_STATUS) {
            for (Enemy e : livingEnemies(s)) {
                e.vulnerable += 1;
                e.bind += 1 + tier + s.bindPower / 2;
                e.mark += tier;
                damageEnemy(s, e, 2 + tier * 3 + overload, true);
            }
        } else if (focus == BUILD_CYCLE) {
            draw(s, tier >= 3 ? 2 : 1);
            if (tier >= 2) {
                s.energy++;
            }
            if (tier >= 3) {
                addProfessionSkillCharge(s, 1);
            }
        } else if (focus == BUILD_GUARD) {
            gainBlock(s, 6 + tier * 4 + s.steelEngine);
            if (tier >= 3) {
                s.steelEngine++;
            }
            if (target != null) {
                damageEnemy(s, target, Math.min(30, s.block / 4 + tier * 4 + overload * 2), true);
            }
        }
        log(s, "职业技共鸣：" + BUILD_FOCUS_NAMES[focus] + " " + buildFocusRank(s.buildResonanceScore) + "。");
    }

    private static int skillResonanceTier(State s) {
        if (s == null || s.mode != MODE_COMBAT || s.buildResonanceFocus < 0
                || s.buildResonanceFocus >= BUILD_FOCUS_NAMES.length || s.buildResonanceScore < 35) {
            return 0;
        }
        int tier = s.buildResonanceScore >= 80 ? 3 : s.buildResonanceScore >= 55 ? 2 : 1;
        if ("spec_resonance".equals(s.skillSpec)) {
            tier = Math.min(3, tier + 1);
        }
        return tier;
    }

    private static void applySkillSpecOnUse(State s, Enemy chosenTarget, int overload) {
        SkillSpecDef spec = skillSpec(s.skillSpec);
        if (spec == null) {
            return;
        }
        int level = Math.max(1, s.skillSpecLevel);
        Enemy target = chosenTarget != null && chosenTarget.hp > 0 ? chosenTarget : firstLiving(s);
        if ("spec_burst".equals(spec.id)) {
            if (target != null) {
                int damage = 6 + level * 4 + s.act * 3 + overload * (2 + level) + Math.min(12 + level * 3, s.cardsPlayedThisTurn * 2);
                damageEnemy(s, target, damage, true);
                if (target.hp <= 0) {
                    s.energy++;
                    addProfessionSkillCharge(s, 1 + level);
                }
            }
        } else if ("spec_tempo".equals(spec.id)) {
            draw(s, 1 + overload / 4 + (level >= 3 ? 1 : 0));
            Card cut = new Card("quick_cut");
            cut.temp = true;
            addToHand(s, cut);
            if (level >= 2) {
                s.energy++;
            }
            addProfessionSkillCharge(s, level + overload / 3);
        } else if ("spec_sustain".equals(spec.id)) {
            int low = s.hp <= s.maxHp / 2 ? 3 + level * 2 : 0;
            gainBlock(s, 6 + level * 4 + s.act * 2 + overload * 2 + low);
            s.hp = Math.min(s.maxHp, s.hp + 2 + level * 2 + overload + low / 2);
        } else if ("spec_resonance".equals(spec.id)) {
            addProfessionSkillCharge(s, 1 + level + overload / 2);
            int focus = s.buildResonanceFocus;
            if (focus >= 0 && focus < BUILD_FOCUS_NAMES.length) {
                if (focus == BUILD_ECHO || focus == BUILD_CYCLE) {
                    draw(s, level >= 3 ? 2 : 1);
                } else if (focus == BUILD_GUARD || focus == BUILD_FORGE) {
                    gainBlock(s, 4 + level * 3 + s.act);
                } else if (focus == BUILD_BREW || focus == BUILD_STATUS) {
                    for (Enemy e : livingEnemies(s)) {
                        e.bind += level + s.bindPower / 2;
                        e.vulnerable += 1;
                    }
                } else if (focus == BUILD_GOLD) {
                    s.gold += 6 + level * 4 + s.act * 2;
                } else if (focus == BUILD_BLOOD) {
                    s.hp = Math.min(s.maxHp, s.hp + 2 + level * 3 + overload);
                } else if (target != null) {
                    damageEnemy(s, target, 5 + level * 4 + overload * 2, true);
                }
            }
        } else if ("spec_mastery".equals(spec.id)) {
            applyMasterySkillSpec(s, target, overload, level);
        }
        applySkillSpecRelics(s, target, overload, level, spec.id);
        log(s, "专修触发：" + spec.name + "。");
    }

    private static void applySkillSpecRelics(State s, Enemy target, int overload, int level, String specId) {
        if ("spec_burst".equals(specId) && hasRelic(s, "razor_pactstone") && target != null) {
            damageEnemy(s, target, 8 + level * 4 + overload * 2, true);
            if (target.hp <= 0) {
                draw(s, 1);
                addProfessionSkillCharge(s, 2);
            }
        } else if ("spec_tempo".equals(specId) && hasRelic(s, "tempo_spindle")) {
            draw(s, 1);
            s.energy++;
            addProfessionSkillCharge(s, 1);
            if (level >= 3) {
                Card cut = new Card("quick_cut");
                cut.temp = true;
                addToHand(s, cut);
            }
        } else if ("spec_sustain".equals(specId) && hasRelic(s, "vigil_bloom")) {
            int low = s.hp <= s.maxHp / 2 ? 2 : 1;
            gainBlock(s, (5 + level * 3) * low);
            s.hp = Math.min(s.maxHp, s.hp + (2 + level) * low);
        } else if ("spec_resonance".equals(specId) && hasRelic(s, "resonance_lens")) {
            addProfessionSkillCharge(s, 2 + level);
            if (s.buildResonanceFocus == BUILD_ECHO || s.buildResonanceFocus == BUILD_CYCLE) {
                draw(s, 1);
            } else if (s.buildResonanceFocus == BUILD_GUARD || s.buildResonanceFocus == BUILD_FORGE) {
                gainBlock(s, 5 + level * 2);
            } else if (target != null) {
                damageEnemy(s, target, 6 + level * 3 + overload, true);
            }
        } else if ("spec_mastery".equals(specId) && hasRelic(s, "mastery_badge")) {
            CardDef d = randomOverloadCard(s, true);
            Card c = new Card(d.id);
            c.temp = true;
            if (level >= 3) {
                c.upgraded = true;
            }
            addToHand(s, c);
            addProfessionSkillCharge(s, 2);
        }
    }

    private static void applyMasterySkillSpec(State s, Enemy target, int overload, int level) {
        if (PROF_WARDEN.equals(s.profession)) {
            s.steelEngine++;
            gainBlock(s, 4 + level * 3 + s.steelEngine * 2 + overload);
            if (target != null) {
                damageEnemy(s, target, Math.min(18 + level * 6, s.block / 5 + overload * 2), true);
            }
        } else if (PROF_DUELIST.equals(s.profession)) {
            draw(s, 1);
            s.energy += s.cardsPlayedThisTurn >= 4 ? 1 : 0;
            if (target != null) {
                damageEnemy(s, target, 4 + level * 3 + s.cardsPlayedThisTurn * 2 + overload * 2, true);
            }
        } else if (PROF_ALCHEMIST.equals(s.profession)) {
            s.burnPower++;
            s.bindPower++;
            if (s.potions.size() < potionLimit(s) && (overload >= 2 || level >= 3)) {
                s.potions.add(POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size())).id);
            }
        } else if (PROF_RANGER.equals(s.profession)) {
            if (target != null) {
                target.mark += level + 1 + overload / 2;
                target.bind += level + s.bindPower;
                damageEnemy(s, target, 3 + level * 2 + target.mark * 2, true);
            }
        } else if (PROF_ARCANIST.equals(s.profession)) {
            s.voidEngine++;
            Card glyph = new Card("arcanist_glyph");
            glyph.temp = true;
            addToHand(s, glyph);
            if (overload >= 3 || level >= 3) {
                s.energy++;
            }
        } else if (PROF_MERCHANT.equals(s.profession)) {
            int income = 8 + level * 5 + s.act * 4 + overload * 2;
            s.gold += income;
            gainBlock(s, Math.min(18, 5 + income / 2));
        } else if (PROF_BLOODBOUND.equals(s.profession)) {
            addStatusCard(s, "wound");
            s.hp = Math.min(s.maxHp, s.hp + 2 + level * 3 + overload);
            if (target != null) {
                target.vulnerable++;
            }
        } else if (PROF_WEAVER.equals(s.profession)) {
            upgradeRandomHandCard(s);
            draw(s, 1);
            gainBlock(s, 3 + level * 3 + upgradedCardCount(s) / 3);
        } else if (PROF_SUMMONER.equals(s.profession)) {
            Card spirit = new Card("summoner_sprite");
            spirit.temp = true;
            addToHand(s, spirit);
            if (level >= 3) {
                Card wisp = new Card("summoner_wisp");
                wisp.temp = true;
                addToHand(s, wisp);
            }
            for (Enemy e : livingEnemies(s)) {
                e.bind += level + s.bindPower;
            }
            gainBlock(s, 3 + level * 3 + s.act);
        } else if (PROF_HEXER.equals(s.profession)) {
            addStatusCard(s, "daze");
            for (Enemy e : livingEnemies(s)) {
                e.vulnerable++;
                e.mark += level + overload / 3;
            }
        } else if (PROF_INSCRIBER.equals(s.profession)) {
            upgradeRandomHandCard(s);
            if (level >= 2) {
                upgradeRandomHandCard(s);
            }
            gainBlock(s, 3 + level * 3 + Math.min(10, upgradedCardCount(s)));
            for (Enemy e : livingEnemies(s)) {
                e.vulnerable += 1;
                e.bind += level + overload / 3;
                e.mark += 1 + level / 2;
            }
            if (level >= 3 || overload >= 3) {
                draw(s, 1);
                addProfessionSkillCharge(s, 1);
            }
        }
    }

    private static void applyProfessionSkillRelics(State s, Enemy target) {
        if (hasRelic(s, "command_banner") && PROF_WARDEN.equals(s.profession)) {
            gainBlock(s, 6 + s.act * 2);
            if (target != null) {
                damageEnemy(s, target, 6 + s.steelEngine * 2, true);
            }
        }
        if (hasRelic(s, "flash_heel") && PROF_DUELIST.equals(s.profession)) {
            draw(s, 1);
            if (s.cardsPlayedThisTurn >= 5) {
                s.energy++;
            }
        }
        if (hasRelic(s, "catalyst_pump") && PROF_ALCHEMIST.equals(s.profession)) {
            for (Enemy e : livingEnemies(s)) {
                e.burn += 2 + s.burnPower;
                e.bind += 1 + s.bindPower / 2;
            }
        }
        if (hasRelic(s, "hawk_fletching") && PROF_RANGER.equals(s.profession) && target != null) {
            target.bind += 2 + s.bindPower;
            damageEnemy(s, target, 5 + target.bind / 2, true);
        }
        if (hasRelic(s, "echo_prism") && PROF_ARCANIST.equals(s.profession)) {
            Card echo = new Card("void_glimpse");
            echo.temp = true;
            addToHand(s, echo);
            s.energy++;
        }
        if (hasRelic(s, "ledger_stamp") && PROF_MERCHANT.equals(s.profession)) {
            int refund = 12 + s.act * 3;
            s.gold += refund;
            gainBlock(s, Math.min(18, Math.max(4, s.gold / 30)));
        }
        if (hasRelic(s, "crimson_seal") && PROF_BLOODBOUND.equals(s.profession) && target != null) {
            s.hp = Math.min(s.maxHp, s.hp + 3);
            target.vulnerable += 1;
            addProfessionSkillCharge(s, 2);
        }
        if (hasRelic(s, "pattern_spool") && PROF_WEAVER.equals(s.profession)) {
            upgradeRandomHandCard(s);
            addProfessionSkillCharge(s, 2);
            draw(s, 1);
        }
        if (hasRelic(s, "spirit_bell") && PROF_SUMMONER.equals(s.profession)) {
            Card spirit = new Card("summoner_sprite");
            spirit.temp = true;
            addToHand(s, spirit);
            gainBlock(s, 5 + s.act);
            addProfessionSkillCharge(s, 2);
        }
        if (hasRelic(s, "hex_tablet") && PROF_HEXER.equals(s.profession) && target != null) {
            target.vulnerable += 1;
            target.bind += 1 + s.bindPower / 2;
            damageEnemy(s, target, 6 + s.act * 2, true);
            addProfessionSkillCharge(s, 2);
        }
        if (hasRelic(s, "engraver_stylus") && PROF_INSCRIBER.equals(s.profession)) {
            upgradeRandomHandCard(s);
            draw(s, 1);
            if (target != null) {
                target.vulnerable += 1;
                target.bind += 1 + s.bindPower / 2;
            }
            addProfessionSkillCharge(s, 2);
        }
    }

    private static void addProfessionSkillCharge(State s, int amount) {
        if (s == null || s.mode != MODE_COMBAT || s.profession == null || s.profession.length() == 0) {
            return;
        }
        if (hasRelic(s, "command_banner") && PROF_WARDEN.equals(s.profession) && amount > 0) amount++;
        if (hasRelic(s, "flash_heel") && PROF_DUELIST.equals(s.profession) && s.cardsPlayedThisTurn >= 3 && amount > 0) amount++;
        if (hasRelic(s, "catalyst_pump") && PROF_ALCHEMIST.equals(s.profession) && s.potions.size() > 0 && amount > 0) amount++;
        if (hasRelic(s, "hawk_fletching") && PROF_RANGER.equals(s.profession) && firstLiving(s) != null && firstLiving(s).bind > 0 && amount > 0) amount++;
        if (hasRelic(s, "echo_prism") && PROF_ARCANIST.equals(s.profession) && s.voidEngine > 0 && amount > 0) amount++;
        if (hasRelic(s, "ledger_stamp") && PROF_MERCHANT.equals(s.profession) && s.gold >= 120 && amount > 0) amount++;
        if (hasRelic(s, "crimson_seal") && PROF_BLOODBOUND.equals(s.profession) && s.hp <= s.maxHp / 2 && amount > 0) amount++;
        if (hasRelic(s, "pattern_spool") && PROF_WEAVER.equals(s.profession) && s.hand.size() >= 5 && amount > 0) amount++;
        if (hasRelic(s, "spirit_bell") && PROF_SUMMONER.equals(s.profession) && amount > 0 && s.hand.size() >= 5) amount++;
        if (hasRelic(s, "hex_tablet") && PROF_HEXER.equals(s.profession) && amount > 0 && firstLiving(s) != null && firstLiving(s).vulnerable > 0) amount++;
        if (hasRelic(s, "engraver_stylus") && PROF_INSCRIBER.equals(s.profession) && amount > 0 && upgradedCardCount(s) >= 3) amount++;
        s.professionSkillCharge = Math.max(0, Math.min(PROF_SKILL_MAX + PROF_SKILL_OVERLOAD_MAX, s.professionSkillCharge + amount));
    }

    private static void startCombat(State s, char kind) {
        s.mode = MODE_COMBAT;
        s.combatKind = kind;
        applyRouteArrival(s, kind);
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
        s.professionSkillCharge = 0;
        s.professionSkillUsedThisTurn = false;
        s.professionUsedThisTurn = 0;
        s.combatQuest = chooseCombatQuest(s, kind);
        s.questTarget = questTargetFor(s, s.combatQuest, kind);
        int resonance = buildScoutFocus(s);
        s.buildResonanceFocus = resonance;
        s.buildResonanceScore = buildScoutFocusScore(s, resonance);
        s.questProgress = 0;
        s.questComplete = false;
        s.playerTurn = true;
        s.cardsPlayedThisTurn = 0;
        s.totalCardsPlayed = 0;
        s.pactMaxBlock = 0;
        s.pactMaxCardsTurn = 0;
        s.pactPotionsUsed = 0;
        s.pactKills = 0;
        s.pactExhaustedCards = 0;
        s.pactSelfDamage = 0;
        s.pactTempCards = 0;
        s.pactStatusCards = 0;
        s.pactForgeCards = 0;
        s.pactGoldCards = 0;
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
        applyDepthCombatRules(s, kind);
        applyRouteCombatStart(s);
        applyEncounterStart(s);
        applyEnemyStartMechanics(s);
        int startCharge = s.masterySkillCharge + skillSpecStartCharge(s);
        if (startCharge > 0) {
            s.professionSkillCharge = Math.min(PROF_SKILL_MAX + PROF_SKILL_OVERLOAD_MAX, startCharge);
        }
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
        String route = s.currentRoute == ROUTE_NONE ? "" : " 路线：" + routeName(s.currentRoute);
        log(s, (kind == 'B' ? "深渊领主现身。" : kind == 'E' ? "精英挡住去路。" : "遭遇敌群。") + " 词缀：" + modifierName(s.encounterModifier) + route);
        if (s.combatQuest != QUEST_NONE) {
            log(s, "战斗目标：" + questName(s.combatQuest) + " - " + questText(s));
        }
    }

    private static int skillSpecStartCharge(State s) {
        SkillSpecDef spec = skillSpec(s.skillSpec);
        if (spec == null) {
            return 0;
        }
        int level = Math.max(1, s.skillSpecLevel);
        if ("spec_burst".equals(spec.id)) {
            return level;
        }
        if ("spec_resonance".equals(spec.id) || "spec_mastery".equals(spec.id)) {
            return Math.max(0, level - 1);
        }
        return 0;
    }

    private static int chooseCombatQuest(State s, char kind) {
        ArrayList<Integer> quests = new ArrayList<>();
        quests.add(QUEST_SWIFT);
        quests.add(QUEST_UNHURT);
        quests.add(QUEST_COMBO);
        quests.add(QUEST_GUARD);
        quests.add(QUEST_HEX);
        if (kind == 'B') {
            addSupportedCombatQuests(s, quests);
            return quests.get(s.run.nextInt(quests.size()));
        }
        quests.add(QUEST_LEAN);
        addSupportedCombatQuests(s, quests);
        if (kind == 'E' || s.run.nextInt(100) < 82) {
            return quests.get(s.run.nextInt(quests.size()));
        }
        return QUEST_NONE;
    }

    private static void addSupportedCombatQuests(State s, ArrayList<Integer> quests) {
        addSupportedCombatQuest(s, quests, QUEST_BREW);
        addSupportedCombatQuest(s, quests, QUEST_SKILL);
        addSupportedCombatQuest(s, quests, QUEST_ECHO);
        addSupportedCombatQuest(s, quests, QUEST_BLOODCOIN);
        addSupportedCombatQuest(s, quests, QUEST_FORGE);
        addSupportedCombatQuest(s, quests, QUEST_TREASURE);
    }

    private static void addSupportedCombatQuest(State s, ArrayList<Integer> quests, int quest) {
        if (supportsCombatQuest(s, quest)) {
            quests.add(quest);
        }
    }

    private static boolean supportsCombatQuest(State s, int quest) {
        if (quest == QUEST_SKILL) {
            return s.profession != null && s.profession.length() > 0;
        }
        if (quest == QUEST_BREW && (PROF_ALCHEMIST.equals(s.profession) || !s.potions.isEmpty()
                || hasTalent(s, "t_alchemist_grandbrew") || hasRelic(s, "alchemist_case") || hasRelic(s, "glass_vials"))) {
            return true;
        }
        if (quest == QUEST_ECHO && (PROF_ARCANIST.equals(s.profession) || PROF_SUMMONER.equals(s.profession)
                || hasTalent(s, "t_arcanist_singularity") || hasTalent(s, "t_summoner_overflow")
                || hasRelic(s, "echo_prism") || hasRelic(s, "singularity_orb") || hasRelic(s, "spirit_processional"))) {
            return true;
        }
        if (quest == QUEST_BLOODCOIN && (PROF_MERCHANT.equals(s.profession) || PROF_BLOODBOUND.equals(s.profession)
                || PROF_HEXER.equals(s.profession) || PROF_INSCRIBER.equals(s.profession)
                || hasTalent(s, "t_merchant_monopoly") || hasTalent(s, "t_bloodbound_hemocraft")
                || hasTalent(s, "t_hexer_darkdeal") || hasRelic(s, "bloodcoin_broach") || hasRelic(s, "kingmaker_seal"))) {
            return true;
        }
        if (quest == QUEST_FORGE && (PROF_WEAVER.equals(s.profession) || PROF_INSCRIBER.equals(s.profession)
                || hasTalent(s, "t_weaver_grandpattern") || hasTalent(s, "t_inscriber_grandcodex")
                || hasRelic(s, "mirror_anvil") || hasRelic(s, "clockwork_loom") || hasRelic(s, "polished_cog"))) {
            return true;
        }
        if (quest == QUEST_TREASURE && (PROF_MERCHANT.equals(s.profession) || hasTalent(s, "t_merchant_interest")
                || hasTalent(s, "t_merchant_monopoly") || hasRelic(s, "tithe_box") || hasRelic(s, "kingmaker_seal"))) {
            return true;
        }
        for (Card c : s.deck) {
            CardDef d = card(c.id);
            if (d == null) {
                continue;
            }
            if (quest == QUEST_BREW && d.createPotion) return true;
            if (quest == QUEST_ECHO && (d.exhaust || d.createEcho || c.temp)) return true;
            if (quest == QUEST_BLOODCOIN && (d.hpLoss > 0 || d.goldGain > 0 || d.goldDamage || d.goldBlock || "wound".equals(c.id))) return true;
            if (quest == QUEST_FORGE && (c.upgraded || d.upgradeRandom || d.scry > 0)) return true;
            if (quest == QUEST_TREASURE && (d.goldGain > 0 || d.goldDamage || d.goldBlock)) return true;
        }
        return false;
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
        if (quest == QUEST_BREW) return 1 + boss + elite;
        if (quest == QUEST_SKILL) return 1 + boss;
        if (quest == QUEST_ECHO) return 3 + s.act + boss * 2 + elite;
        if (quest == QUEST_BLOODCOIN) return 3 + s.act / 2 + boss * 2 + elite;
        if (quest == QUEST_FORGE) return 3 + s.act / 2 + boss * 2 + elite;
        if (quest == QUEST_TREASURE) return 2 + boss + elite;
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

    private static void applyDepthCombatRules(State s, char kind) {
        if (s.ascension < 3) {
            return;
        }
        if (kind == 'E') {
            int guard = 4 + s.act * 2 + s.ascension / 2;
            for (Enemy e : s.enemies) {
                e.block += guard;
                if (s.ascension >= 6) {
                    e.strength += 1;
                    int extra = 4 + s.act * 2 + s.ascension;
                    e.maxHp += extra;
                    e.hp += extra;
                }
            }
            log(s, s.ascension >= 6 ? "噩梦阶层：精英生命、力量与护甲提升。" : "暗潮阶层：精英带着护甲开局。");
        } else if (kind == 'B' && s.ascension >= 10) {
            int guard = 10 + s.act * 4;
            for (Enemy e : s.enemies) {
                e.block += guard;
                e.strength += 1 + s.act / 2;
            }
            s.vulnerable += 1;
            s.nextEnergyPenalty = Math.max(s.nextEnergyPenalty, 1);
            log(s, "无光阶层：Boss压迫开局，你易伤且首回合能量-1。");
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
            } else if (e.kind == 29 && e.doom == 0) {
                e.doom = 3;
            } else if (e.kind == 30 && e.thorns == 0) {
                e.thorns = 1 + s.act / 2;
            } else if (e.kind == 31 && e.doom == 0) {
                e.doom = 3;
            } else if (e.kind == 33 && e.shieldPulse == 0) {
                e.shieldPulse = 3;
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
            if (e.kind == 29 && e.doom > 0) {
                e.doom--;
                if (e.doom <= 0) {
                    e.hp = Math.min(e.maxHp, e.hp + 6 + s.act * 2);
                    e.vulnerable = Math.max(0, e.vulnerable - 1);
                    e.doom = 3;
                    log(s, e.name + " 裂疫回流，恢复生命。");
                }
            }
            if (e.kind == 31 && e.doom > 0) {
                e.doom--;
                if (e.doom <= 0) {
                    int drained = Math.min(s.professionSkillCharge, 2 + s.act);
                    s.professionSkillCharge -= drained;
                    e.block += 6 + drained * 3;
                    e.doom = 3;
                    log(s, e.name + " 锁蚀职业技充能，化作护甲。");
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
            int elite = s.run.nextInt(9);
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
            } else if (elite == 5) {
                s.enemies.add(enemy("血契巡礼者", 66 + act * 18 + depth * 2, 14));
            } else if (elite == 6) {
                s.enemies.add(enemy("回响督军", 62 + act * 17 + depth * 2, 15));
                s.enemies.add(enemy("雾页抄手", 26 + act * 8 + depth, 28));
            } else if (elite == 7) {
                s.enemies.add(enemy("静印审判官", 70 + act * 18 + depth * 2, 16));
            } else {
                s.enemies.add(enemy("过载缄默者", 72 + act * 18 + depth * 2, 31));
                s.enemies.add(enemy("雾页抄手", 24 + act * 8 + depth, 28));
            }
            return;
        }
        int templateMax = act >= 3 ? 14 : act >= 2 ? 11 : 6;
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
        } else if (template == 7) {
            s.enemies.add(enemy("锈刃徒", base + 6 + s.run.nextInt(7), 20));
            s.enemies.add(enemy("磷火虫", base + 2 + s.run.nextInt(6), 21));
            s.enemies.add(enemy("空面盗", base + 1 + s.run.nextInt(6), 23));
        } else if (template == 8) {
            s.enemies.add(enemy("雾页抄手", base + 10 + s.run.nextInt(8), 28));
            s.enemies.add(enemy("磷火虫", base + 2 + s.run.nextInt(6), 21));
        } else if (template == 9) {
            s.enemies.add(enemy("裂疫汲取者", base + 16 + s.run.nextInt(9), 29));
            s.enemies.add(enemy("藤壳兽", base + 8 + s.run.nextInt(7), 22));
        } else if (template == 10) {
            s.enemies.add(enemy("镜壳卫", base + 26 + s.run.nextInt(10), 30));
        } else if (template == 11) {
            s.enemies.add(enemy("过载缄默者", base + 20 + s.run.nextInt(8), 31));
            s.enemies.add(enemy("磷火虫", base + 1 + s.run.nextInt(6), 21));
        } else if (template == 12) {
            s.enemies.add(enemy("裂币蛀客", base + 14 + s.run.nextInt(8), 32));
            s.enemies.add(enemy("空面盗", base + 5 + s.run.nextInt(7), 23));
        } else {
            s.enemies.add(enemy("铸雾整备师", base + 22 + s.run.nextInt(9), 33));
            s.enemies.add(enemy("镜壳卫", base + 10 + s.run.nextInt(7), 30));
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
        s.bossRelicTriggersThisTurn = 0;
        s.professionSkillUsedThisTurn = false;
        if (hasRelic(s, "amber_quill") && s.turn == 1) {
            s.energy++;
        }
        if (hasRelic(s, "sapphire_cell")) {
            s.energy++;
        }
        if (hasRelic(s, "obsidian_core")) {
            s.energy++;
        }
        if (hasRelic(s, "ability_crown") && s.turn == 1) {
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
        applyBuildResonanceStart(s);
        if (hasTalent(s, "t_shared_longnight") && s.turn == 4) {
            s.energy++;
            draw(s, 1);
        }
        if (hasRelic(s, "forge_heart") && s.turn == 1) {
            s.energy = Math.max(0, s.energy - 1);
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
        if (hasTalent(s, "t_warden_armory") && s.turn == 1) {
            s.steelEngine++;
            upgradeRandomHandCard(s);
        }
        if (hasTalent(s, "t_warden_vanguard") && s.turn == 1) {
            s.steelEngine++;
            gainBlock(s, 6 + s.act * 2);
        }
        if (PROF_RANGER.equals(s.profession) && firstLiving(s) != null) {
            firstLiving(s).bind += 1 + s.bindPower / 2;
        }
        if (hasTalent(s, "t_ranger_quarry") && s.turn == 1 && firstLiving(s) != null) {
            firstLiving(s).bind += 3;
            firstLiving(s).vulnerable += 2;
        }
        if (hasTalent(s, "t_ranger_wildpath") && firstLiving(s) != null) {
            firstLiving(s).bind += 1 + s.act / 2;
        }
        if (hasTalent(s, "t_ranger_apex") && s.turn == 1 && firstLiving(s) != null) {
            firstLiving(s).bind += 2 + s.bindPower / 2;
            firstLiving(s).vulnerable += 1;
        }
        if (PROF_ARCANIST.equals(s.profession) && s.turn == 1) {
            s.voidEngine++;
        }
        if (hasTalent(s, "t_arcanist_archive") && s.turn == 1) {
            s.voidEngine++;
            draw(s, 1);
        }
        if (hasTalent(s, "t_arcanist_singularity") && s.turn == 1) {
            s.voidEngine++;
        }
        if (PROF_MERCHANT.equals(s.profession) && s.turn == 1) {
            gainBlock(s, Math.min(16, Math.max(3, s.gold / 35)));
        }
        if (hasTalent(s, "t_merchant_monopoly") && s.turn == 1) {
            gainBlock(s, Math.min(18, 4 + s.gold / 40));
        }
        if (PROF_BLOODBOUND.equals(s.profession) && s.turn == 1) {
            s.professionCharge = 0;
            if (s.hp <= s.maxHp / 2) {
                s.energy++;
                gainBlock(s, 6 + s.act * 2);
                log(s, "血契低鸣，压低血线换来资源。");
            }
            if (hasTalent(s, "t_bloodbound_crimson")) {
                s.hp = Math.max(1, s.hp - 2);
                s.energy++;
                gainBlock(s, 5 + s.act);
            }
        }
        if (PROF_WEAVER.equals(s.profession) && s.turn == 1) {
            s.professionCharge = 0;
            s.energy += hasTalent(s, "t_weaver_setup") ? 1 : 0;
            if (hasTalent(s, "t_weaver_quicksilver")) {
                Card thread = new Card("weaver_thread");
                thread.temp = true;
                addToHand(s, thread);
            }
        }
        if (PROF_SUMMONER.equals(s.profession) && s.turn == 1) {
            Card spirit = new Card("summoner_sprite");
            spirit.temp = true;
            addToHand(s, spirit);
            if (hasTalent(s, "t_summoner_bond")) {
                gainBlock(s, 7 + s.act);
            }
            if (hasTalent(s, "t_summoner_overflow")) {
                Card wisp = new Card("summoner_wisp");
                wisp.temp = true;
                addToHand(s, wisp);
            }
        }
        if (PROF_HEXER.equals(s.profession) && s.turn == 1 && firstLiving(s) != null) {
            firstLiving(s).vulnerable += 1;
            if (hasTalent(s, "t_hexer_malediction")) {
                firstLiving(s).bind += 2;
            }
            if (hasTalent(s, "t_hexer_abysscurse")) {
                for (Enemy e : livingEnemies(s)) {
                    e.vulnerable += 1;
                    e.bind += 1 + s.bindPower / 2;
                }
            }
        }
        if (PROF_INSCRIBER.equals(s.profession) && s.turn == 1) {
            if (hasTalent(s, "t_inscriber_rubbing")) {
                upgradeRandomHandCard(s);
            }
            if (firstLiving(s) != null && hasTalent(s, "t_inscriber_etching")) {
                firstLiving(s).vulnerable += 1;
                firstLiving(s).bind += 1 + s.bindPower / 2;
            }
            if (hasTalent(s, "t_inscriber_archive")) {
                draw(s, 1);
            }
            if (hasTalent(s, "t_inscriber_grandcodex")) {
                addProfessionSkillCharge(s, 2);
            }
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
        if (hasRelic(s, "hex_moon") && firstLiving(s) != null) {
            firstLiving(s).burn += 1 + s.act / 2 + s.burnPower / 2;
            firstLiving(s).bind += 1 + s.bindPower / 2;
            if (s.turn == 1) {
                firstLiving(s).vulnerable += 1;
            }
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
        if (hasRelic(s, "echo_crown") && s.turn == 1) {
            Card echo = new Card(PROF_SUMMONER.equals(s.profession) ? "summoner_wisp" : "quick_cut");
            echo.temp = true;
            addToHand(s, echo);
            Card bait = new Card("echo_bait");
            bait.temp = true;
            addToHand(s, bait);
        }
        if (hasRelic(s, "ability_crown") && s.turn == 1) {
            CardDef power = randomTypeCard(s, 2, true);
            if (power != null) {
                Card c = new Card(power.id);
                c.temp = true;
                addToHand(s, c);
            }
        }
        if (hasRelic(s, "blood_contract") && s.turn == 1) {
            s.hp = Math.max(1, s.hp - 2);
            draw(s, 2);
        }
        if (hasRelic(s, "forge_heart") && s.turn == 1) {
            upgradeRandomHandCard(s);
            upgradeRandomHandCard(s);
        }
        if (hasRelic(s, "hex_moon") && s.turn > 1 && s.turn % 2 == 0) {
            addStatusCard(s, "daze");
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
        applyRouteFirstTurn(s);
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

    private static void applyBuildResonanceStart(State s) {
        if (s.turn != 1 || s.buildResonanceFocus < 0 || s.buildResonanceScore < 35) {
            return;
        }
        int focus = s.buildResonanceFocus;
        int tier = s.buildResonanceScore >= 80 ? 3 : s.buildResonanceScore >= 55 ? 2 : 1;
        if (focus == BUILD_OVERLOAD) {
            addProfessionSkillCharge(s, 2 + tier);
        } else if (focus == BUILD_ECHO) {
            Card echo = new Card(PROF_SUMMONER.equals(s.profession) ? "summoner_sprite" : "quick_cut");
            echo.temp = true;
            addToHand(s, echo);
            if (tier >= 2) {
                s.voidEngine++;
            }
        } else if (focus == BUILD_BREW) {
            if (s.potions.size() < potionLimit(s)) {
                PotionDef p = POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size()));
                s.potions.add(p.id);
            }
            if (firstLiving(s) != null) {
                firstLiving(s).burn += 1 + tier + s.burnPower / 2;
                firstLiving(s).bind += 1 + s.bindPower / 2;
            }
        } else if (focus == BUILD_GOLD) {
            int income = 4 + tier * 3 + s.act;
            s.gold += income;
            gainBlock(s, 2 + tier + Math.min(6, s.gold / 80));
        } else if (focus == BUILD_BLOOD) {
            gainBlock(s, 3 + tier * 2);
            if (s.hp <= s.maxHp / 2) {
                s.energy++;
            } else {
                s.hp = Math.max(1, s.hp - 1);
            }
        } else if (focus == BUILD_FORGE) {
            upgradeRandomHandCard(s);
            if (tier >= 2) {
                gainBlock(s, 4 + tier);
            }
        } else if (focus == BUILD_STATUS) {
            for (Enemy e : livingEnemies(s)) {
                e.vulnerable += 1;
                e.bind += tier;
            }
        } else if (focus == BUILD_CYCLE) {
            draw(s, 1);
            if (tier >= 2) {
                s.energy++;
            }
        } else if (focus == BUILD_GUARD) {
            s.steelEngine++;
            gainBlock(s, 6 + tier * 3);
        }
        log(s, "构筑共鸣：" + BUILD_FOCUS_NAMES[focus] + " " + buildFocusRank(s.buildResonanceScore) + "。");
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
            } else if (e.kind == 15) {
                if (r < 34) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 10 + s.act * 3;
                } else if (r < 72) {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 1;
                } else {
                    e.intent = ENEMY_GUARD;
                    e.intentValue = 10 + s.act * 2;
                }
            } else if (e.kind == 16) {
                if (r < 38) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 11 + s.act * 3;
                } else if (r < 76) {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 1;
                } else {
                    e.intent = ENEMY_DEBUFF;
                    e.intentValue = 1 + s.act / 2;
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
            } else if (e.kind == 28) {
                if (r < 36) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 6 + s.act * 2;
                } else if (r < 76) {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 1;
                } else {
                    e.intent = ENEMY_DEBUFF;
                    e.intentValue = 1;
                }
            } else if (e.kind == 29) {
                if (r < 40) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 7 + s.act * 2;
                } else if (r < 78) {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 1;
                } else {
                    e.intent = ENEMY_BUFF;
                    e.intentValue = 2;
                }
            } else if (e.kind == 30) {
                if (r < 46) {
                    e.intent = ENEMY_GUARD;
                    e.intentValue = 11 + s.act * 3;
                } else if (r < 78) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 8 + s.act * 3;
                } else {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 1;
                }
            } else if (e.kind == 31) {
                if (r < 34) {
                    e.intent = ENEMY_DEBUFF;
                    e.intentValue = 1 + s.act / 2;
                } else if (r < 72) {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 2 + s.act;
                } else {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 8 + s.act * 3;
                }
            } else if (e.kind == 32) {
                if (r < 40) {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 7 + s.act * 2;
                } else if (r < 78) {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 1;
                } else {
                    e.intent = ENEMY_BUFF;
                    e.intentValue = 2;
                }
            } else if (e.kind == 33) {
                if (r < 42) {
                    e.intent = ENEMY_GUARD;
                    e.intentValue = 10 + s.act * 3;
                } else if (r < 74) {
                    e.intent = ENEMY_SPECIAL;
                    e.intentValue = 1;
                } else {
                    e.intent = ENEMY_ATTACK;
                    e.intentValue = 9 + s.act * 3;
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
        } else if (e.kind == 15) {
            int pressure = Math.max(0, s.cardsPlayedThisTurn - 3);
            if (pressure > 0) {
                int taken = dealPlayerDamage(s, pressure * (2 + s.act / 2));
                log(s, e.name + " 折回连打余波，造成 " + taken + " 点伤害。");
            } else {
                e.block += 8 + s.act * 2;
            }
            Card echo = new Card("daze");
            echo.temp = true;
            addToHand(s, echo);
        } else if (e.kind == 16) {
            int drained = 0;
            for (Enemy other : livingEnemies(s)) {
                int burn = Math.min(other.burn, 2 + s.act);
                int bind = Math.min(other.bind, 2 + s.act);
                other.burn -= burn;
                other.bind -= bind;
                drained += burn + bind;
            }
            e.block += 10 + drained;
            e.strength += drained >= 6 ? 1 : 0;
            addStatusCard(s, "daze");
            log(s, e.name + " 封存异常并转为护甲。");
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
        } else if (e.kind == 28) {
            int moved = 0;
            while (!s.draw.isEmpty() && moved < 2) {
                s.discard.add(s.draw.remove(s.draw.size() - 1));
                moved++;
            }
            if (s.hand.size() >= 5) {
                addStatusCard(s, "daze");
            }
            e.block += 5 + s.act * 2 + moved * 2;
            log(s, e.name + " 抄乱牌页。");
        } else if (e.kind == 29) {
            int drained = 0;
            for (Enemy other : livingEnemies(s)) {
                int burn = Math.min(other.burn, 2);
                int vuln = Math.min(other.vulnerable, 1);
                other.burn -= burn;
                other.vulnerable -= vuln;
                drained += burn + vuln;
            }
            e.hp = Math.min(e.maxHp, e.hp + 4 + drained * 2);
            e.strength += drained >= 3 ? 1 : 0;
            s.vulnerable += drained >= 3 ? 1 : 0;
            log(s, e.name + " 汲取裂疫恢复生命。");
        } else if (e.kind == 30) {
            int crack = Math.min(s.block, 8 + s.act * 4);
            s.block -= crack;
            e.block += 8 + s.act * 3 + crack / 2;
            if (crack >= 10) {
                s.nextEnergyPenalty = Math.max(s.nextEnergyPenalty, 1);
            }
            log(s, e.name + " 折射你的护势。");
        } else if (e.kind == 31) {
            int drained = Math.min(s.professionSkillCharge, 3 + s.act);
            s.professionSkillCharge -= drained;
            e.block += 8 + drained * 3;
            if (drained >= 3) {
                s.vulnerable += 1;
            }
            log(s, e.name + " 缄默过载，吸走 " + drained + " 点职业技充能。");
        } else if (e.kind == 32) {
            int stolen = Math.min(s.gold, 6 + s.act * 3 + s.hand.size());
            if (stolen > 0) {
                s.gold -= stolen;
                e.stolenGold += stolen;
            }
            int temps = 0;
            for (Card h : s.hand) {
                if (h.temp) {
                    temps++;
                }
            }
            e.block += 6 + s.act * 2 + temps * 3;
            if (temps > 0) {
                addStatusCard(s, "daze");
            }
            log(s, e.name + " 啃食裂币与临时牌势，偷走 " + stolen + " 金币。");
        } else if (e.kind == 33) {
            int crack = Math.min(s.block, 6 + s.act * 3);
            int upgrades = upgradedCardCount(s);
            s.block -= crack;
            e.block += 10 + s.act * 2 + crack / 2 + upgrades / 3;
            if (upgrades >= 6) {
                e.strength += 1;
            }
            log(s, e.name + " 整备铸雾，折走护甲并借用你的升级牌势。");
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
        if ("warden_aegisline".equals(d.id) && s.block >= 24) {
            damage += Math.min(c.upgraded ? 24 : 18, s.block / 2);
            block += 4 + s.steelEngine;
        }
        if ("wild_thorn".equals(d.id) && target != null && target.bind > 0) {
            damage += c.upgraded ? 8 : 5;
            block += c.upgraded ? 4 : 3;
        }
        if ("duelist_bladesong".equals(d.id) && s.cardsPlayedThisTurn >= 4) {
            draw += 1;
            damage += 4 * Math.max(0, s.cardsPlayedThisTurn - 3);
        }
        if ("ranger_predator".equals(d.id) && target != null) {
            damage += Math.min(c.upgraded ? 28 : 20, target.bind * (c.upgraded ? 3 : 2));
            if (target.vulnerable > 0) {
                draw += 1;
            }
        }
        if ("merchant_kingmaker".equals(d.id)) {
            block += Math.min(c.upgraded ? 20 : 14, s.gold / 18);
        }
        if ("blood_apotheosis".equals(d.id)) {
            damage += Math.max(0, s.maxHp - s.hp) / (c.upgraded ? 4 : 5);
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
        if (hasTalent(s, "t_weaver_quicksilver") && c.temp) {
            block += 3;
            draw += 1;
        }
        if (hasTalent(s, "t_weaver_grandpattern") && c.upgraded && (d.type == 1 || d.upgradeRandom || d.scry > 0)) {
            block += 3 + s.act;
            addProfessionSkillCharge(s, 1);
        }
        if (PROF_INSCRIBER.equals(s.profession) && (c.upgraded || d.upgradeRandom || d.vulnerable > 0 || d.bind > 0 || d.addStatusToEnemy)) {
            block += 1 + Math.min(6, upgradedCardCount(s) / 2);
        }
        if (hasTalent(s, "t_inscriber_rubbing") && (c.upgraded || d.upgradeRandom)) {
            block += 2 + s.act;
            if (s.professionUsedThisTurn == 0) {
                addProfessionSkillCharge(s, 1);
                s.professionUsedThisTurn++;
            }
        }
        if (hasTalent(s, "t_inscriber_etching") && target != null && (d.vulnerable > 0 || d.bind > 0 || d.addStatusToEnemy)) {
            damage += 3 + s.act * 2 + Math.min(10, target.vulnerable + target.bind);
        }
        if (hasTalent(s, "t_inscriber_grandcodex") && (c.upgraded || d.upgradeRandom || d.vulnerable > 0 || d.bind > 0 || d.addStatusToEnemy)) {
            block += 3 + s.act;
            addProfessionSkillCharge(s, 1);
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
        if (hasTalent(s, "t_duelist_gambit") && s.cardsPlayedThisTurn >= 4) {
            damage += 5 + s.act * 2;
            draw += d.type == 0 ? 1 : 0;
        }
        if (hasTalent(s, "t_duelist_masterstep") && s.cardsPlayedThisTurn >= 5) {
            damage += 7 + s.act * 2;
            if (d.type == 0) {
                draw += 1;
            }
        }
        if (hasTalent(s, "t_alchemist_distiller") && d.createPotion) {
            block += 5 + s.act;
            draw += 1;
        }
        if ("alchemist_sunsteel".equals(d.id)) {
            block += Math.min(c.upgraded ? 18 : 12, s.burnPower * 3 + s.bindPower * 3);
        }
        if (hasTalent(s, "t_alchemist_grandbrew") && d.createPotion) {
            block += 4 + s.act;
            s.burnPower++;
            s.bindPower++;
        }
        if (hasTalent(s, "t_ranger_apex") && target != null && (target.bind >= 5 || target.vulnerable >= 2)) {
            damage += 5 + s.act * 2 + Math.min(10, target.bind);
            draw += d.type == 0 ? 1 : 0;
        }
        if (hasTalent(s, "t_bloodbound_hemocraft") && (d.hpLoss > 0 || "wound".equals(c.id))) {
            block += 4 + s.act;
            if (target != null && s.hp <= s.maxHp / 2) {
                damage += 5 + s.act * 2;
            }
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
        if ("weaver_clockwork".equals(d.id)) {
            block += Math.min(c.upgraded ? 18 : 12, upgradedCardCount(s) * 2);
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
            int before = s.hp;
            s.hp = Math.max(1, s.hp - loss);
            if (s.mode == MODE_COMBAT) {
                s.pactSelfDamage += Math.max(0, before - s.hp);
            }
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
        if (d.skillChargeGain > 0) {
            addProfessionSkillCharge(s, c.upgraded ? d.skillChargeGain + 1 : d.skillChargeGain);
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
            Card ex = s.discard.remove(s.discard.size() - 1);
            s.exhaust.add(ex);
            if ("inscriber_palimp".equals(d.id) && ("wound".equals(ex.id) || "daze".equals(ex.id))) {
                addProfessionSkillCharge(s, 1);
                if (c.upgraded) {
                    draw(s, 1);
                }
            }
            if (hasTalent(s, "t_inscriber_archive") && ("wound".equals(ex.id) || "daze".equals(ex.id))) {
                s.energy++;
                draw(s, 1);
                addProfessionSkillCharge(s, 1);
            }
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
        if ("alchemist_sunsteel".equals(d.id) && target != null) {
            int surge = c.upgraded ? 3 : 2;
            target.burn += surge + s.burnPower;
            target.bind += surge + s.bindPower;
            if (s.potions.size() < potionLimit(s) && s.run.nextInt(100) < (c.upgraded ? 70 : 45)) {
                PotionDef p = POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size()));
                s.potions.add(p.id);
                log(s, "日钢炼台追加药剂：" + p.name);
            }
        }
        if ("arcanist_eventhorizon".equals(d.id)) {
            s.voidEngine += c.upgraded ? 2 : 1;
            for (int i = 0; i < (c.upgraded ? 2 : 1); i++) {
                Card echo = new Card("arcanist_glyph");
                echo.temp = true;
                addToHand(s, echo);
            }
        }
        if ("summoner_procession".equals(d.id)) {
            int count = c.upgraded ? 3 : 2;
            for (int i = 0; i < count; i++) {
                Card spirit = new Card("summoner_sprite");
                spirit.temp = true;
                addToHand(s, spirit);
            }
        }
        if ("hexer_crownfall".equals(d.id)) {
            for (Enemy e : livingEnemies(s)) {
                e.vulnerable += 1;
                e.bind += 1 + s.bindPower / 2;
            }
            addStatusCard(s, c.upgraded ? "wound" : "daze");
        }
        if ("inscriber_glyphstorm".equals(d.id)) {
            addStatusCard(s, "daze");
        }
        if ("inscriber_codex".equals(d.id)) {
            upgradeRandomHandCard(s);
            for (Enemy e : livingEnemies(s)) {
                e.vulnerable += 1;
                e.bind += 1 + s.bindPower / 2;
                e.mark += c.upgraded ? 2 : 1;
            }
            if (c.upgraded) {
                draw(s, 1);
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
        if (hasRelic(s, "emberroot_charm") && (d.burn > 0 || d.bind > 0)) {
            Enemy e = firstLiving(s);
            if (e != null) {
                if (d.burn > 0) {
                    e.bind += 1 + s.bindPower / 2;
                }
                if (d.bind > 0) {
                    e.burn += 1 + s.burnPower / 2;
                }
            }
        }
        if (hasRelic(s, "stormglass_seal")) {
            if (d.draw > 0 && d.block > 0) {
                gainBlock(s, 2 + s.act);
                addProfessionSkillCharge(s, 1);
            }
            if (d.damage > 0 && (d.burn > 0 || d.bind > 0 || d.vulnerable > 0 || d.addStatusToEnemy)) {
                gainBlock(s, 3 + s.act);
            }
        }
        if (hasRelic(s, "curse_censer") && (d.exhaust || c.temp || d.type == 3 || d.createWound)) {
            Enemy e = firstLiving(s);
            if (e != null) {
                e.vulnerable += 1;
                damageEnemy(s, e, 4 + s.act * 2, true);
            }
            if (d.type == 3) {
                gainBlock(s, 3 + s.act);
            }
        }
        if (hasRelic(s, "bloodcoin_broach")) {
            if (d.hpLoss > 0 || "wound".equals(c.id)) {
                s.gold += 3 + s.act;
                gainBlock(s, 2 + s.act);
            }
            if (d.goldGain > 0 || d.goldDamage || d.goldBlock) {
                s.hp = Math.min(s.maxHp, s.hp + 1);
                addProfessionSkillCharge(s, 1);
            }
        }
        if (hasRelic(s, "mirror_anvil") && c.upgraded) {
            if (d.type == 0) {
                Enemy e = firstLiving(s);
                if (e != null) {
                    damageEnemy(s, e, 4 + s.act * 2, true);
                }
            } else if (d.type == 1) {
                gainBlock(s, 4 + s.act);
            } else if (d.type == 2) {
                draw(s, 1);
                addProfessionSkillCharge(s, 1);
            }
        }
        if (hasRelic(s, "mirror_anvil") && d.upgradeRandom) {
            addProfessionSkillCharge(s, 1);
        }
        if (hasRelic(s, "rift_compass")) {
            boolean offOrigin = !"通用".equals(d.origin) && !d.origin.equals(s.origin);
            boolean offProfession = d.profession.length() > 0 && !d.profession.equals(s.profession);
            if (offOrigin || offProfession) {
                gainBlock(s, 3 + s.act);
                if (offOrigin) {
                    draw(s, 1);
                }
                if (offProfession) {
                    addProfessionSkillCharge(s, 1);
                }
            }
        }
        if (hasRelic(s, "echo_crown") && (c.temp || d.createEcho)) {
            addProfessionSkillCharge(s, 1);
            if (s.cardsPlayedThisTurn == 3) {
                draw(s, 1);
            }
        }
        if (hasRelic(s, "hex_moon") && (d.burn > 0 || d.bind > 0 || d.vulnerable > 0 || d.createWound || "wound".equals(c.id) || "daze".equals(c.id))) {
            Enemy e = firstLiving(s);
            if (e != null) {
                e.vulnerable += 1;
            }
        }
        if (hasRelic(s, "forge_heart") && (c.upgraded || d.upgradeRandom || d.scry > 0) && s.bossRelicTriggersThisTurn < 3) {
            gainBlock(s, 3 + s.act);
            s.bossRelicTriggersThisTurn++;
        }
        if (hasRelic(s, "golden_throne") && (d.goldGain > 0 || d.goldDamage || d.goldBlock)) {
            s.gold += 3 + s.act;
            gainBlock(s, 2 + s.act);
        }
        if (hasRelic(s, "ability_crown") && d.type == 2) {
            addProfessionSkillCharge(s, 2);
            if (s.cardsPlayedThisTurn <= 2) {
                s.energy++;
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
        if (hasRelic(s, "finale_rapier") && d.type == 0 && s.cardsPlayedThisTurn >= 4) {
            Enemy e = firstLiving(s);
            if (e != null) {
                damageEnemy(s, e, 4 + s.act * 2 + Math.min(8, s.cardsPlayedThisTurn), true);
            }
            if (s.cardsPlayedThisTurn == 5) {
                draw(s, 1);
            }
        }
        if (hasRelic(s, "ranger_map") && d.bind > 0) {
            Enemy e = firstLiving(s);
            if (e != null) {
                e.vulnerable += 1;
            }
        }
        if (hasRelic(s, "aegis_throne") && d.type == 1 && s.block >= 26 && s.relicTriggersThisTurn < 3) {
            gainBlock(s, 4 + s.act);
            Enemy e = firstLiving(s);
            if (e != null) {
                damageEnemy(s, e, 5 + s.steelEngine + Math.min(10, s.block / 10), true);
            }
            s.relicTriggersThisTurn++;
        }
        if (hasRelic(s, "solar_crucible") && (d.createPotion || d.burn > 0 || d.bind > 0)) {
            Enemy e = firstLiving(s);
            if (e != null) {
                e.burn += 1 + s.burnPower / 2;
                e.bind += 1 + s.bindPower / 2;
            }
            if (d.createPotion) {
                gainBlock(s, 4 + s.act);
            }
        }
        if (hasRelic(s, "apex_compass") && d.bind > 0) {
            Enemy e = firstLiving(s);
            if (e != null && e.bind >= 5) {
                e.vulnerable += 1;
                damageEnemy(s, e, 4 + s.act * 2 + Math.min(10, e.bind / 2), true);
            }
        }
        if (hasTalent(s, "t_ranger_net") && d.bind > 0) {
            gainBlock(s, 4 + Math.min(8, d.bind + s.bindPower));
        }
        if (hasRelic(s, "arcane_ink") && d.exhaust) {
            s.energy++;
        }
        if (hasRelic(s, "singularity_orb") && (d.exhaust || c.temp || d.createEcho)) {
            gainBlock(s, 3 + s.voidEngine);
            if (s.cardsPlayedThisTurn % 3 == 0) {
                Card echo = new Card("arcanist_glyph");
                echo.temp = true;
                addToHand(s, echo);
            }
        }
        if (PROF_WARDEN.equals(s.profession) && d.type == 1) {
            addProfessionSkillCharge(s, 1);
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
        if (hasTalent(s, "t_warden_armory") && d.type == 1 && s.block >= 18) {
            Enemy e = firstLiving(s);
            if (e != null) {
                damageEnemy(s, e, 4 + Math.min(12, s.block / 6), true);
            }
        }
        if (hasTalent(s, "t_warden_vanguard") && d.type == 1 && s.block >= 22) {
            addProfessionSkillCharge(s, 1);
            gainBlock(s, 2 + s.act);
            Enemy e = firstLiving(s);
            if (e != null) {
                damageEnemy(s, e, 4 + s.steelEngine + Math.min(10, s.block / 8), true);
            }
        }
        if (PROF_DUELIST.equals(s.profession) && d.type == 0) {
            addProfessionSkillCharge(s, s.cardsPlayedThisTurn >= 4 ? 2 : 1);
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
        if (hasTalent(s, "t_duelist_masterstep") && s.cardsPlayedThisTurn == 5) {
            s.energy++;
            addProfessionSkillCharge(s, 2);
            Enemy e = firstLiving(s);
            if (e != null) {
                damageEnemy(s, e, 7 + s.act * 3, true);
            }
        }
        if (PROF_RANGER.equals(s.profession) && d.type == 0) {
            addProfessionSkillCharge(s, 1);
            Enemy e = firstLiving(s);
            if (e != null) {
                e.bind += 1 + s.bindPower / 2;
            }
        }
        if (hasTalent(s, "t_ranger_apex") && d.bind > 0) {
            Enemy e = firstLiving(s);
            if (e != null && e.bind >= 6) {
                e.vulnerable += 1;
                damageEnemy(s, e, 5 + s.act * 2 + Math.min(8, e.bind / 2), true);
                addProfessionSkillCharge(s, 1);
            }
        }
        if (PROF_ARCANIST.equals(s.profession) && (d.exhaust || c.temp)) {
            addProfessionSkillCharge(s, 1);
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
        if (hasTalent(s, "t_arcanist_singularity") && (d.exhaust || c.temp || d.createEcho)) {
            addProfessionSkillCharge(s, 1);
            if (s.cardsPlayedThisTurn % 3 == 0) {
                Card echo = new Card("arcanist_glyph");
                echo.temp = true;
                addToHand(s, echo);
                gainBlock(s, 3 + s.voidEngine);
            }
        }
        if (PROF_MERCHANT.equals(s.profession) && d.goldGain > 0) {
            addProfessionSkillCharge(s, 1);
            gainBlock(s, 4 + Math.min(8, s.gold / 80));
        }
        if (hasTalent(s, "t_merchant_monopoly") && (d.goldGain > 0 || d.goldDamage || d.goldBlock)) {
            s.gold += 4 + s.act * 2;
            gainBlock(s, 3 + Math.min(9, s.gold / 90));
            if (s.gold >= 200) {
                addProfessionSkillCharge(s, 1);
            }
        }
        if (hasRelic(s, "kingmaker_seal") && (d.goldGain > 0 || d.goldDamage || d.goldBlock)) {
            s.gold += 3 + s.act * 2;
            gainBlock(s, 3 + Math.min(10, s.gold / 100));
            if (s.gold >= 180) {
                addProfessionSkillCharge(s, 1);
            }
        }
        if (hasTalent(s, "t_merchant_blackmarket") && s.cardsPlayedThisTurn == 2) {
            s.gold += 4 + s.act * 2;
        }
        if (hasTalent(s, "t_alchemist_distiller") && d.createPotion && s.potions.size() < potionLimit(s)) {
            PotionDef p = POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size()));
            s.potions.add(p.id);
            log(s, "蒸馏追加药剂：" + p.name);
        }
        if (hasTalent(s, "t_shared_apothecary") && d.createPotion) {
            s.energy++;
        }
        if (hasTalent(s, "t_alchemist_grandbrew") && (d.createPotion || d.burn > 0 || d.bind > 0 || d.spreadStatus)) {
            Enemy e = firstLiving(s);
            if (e != null) {
                e.burn += 1 + s.burnPower / 2;
                e.bind += 1 + s.bindPower / 2;
            }
            addProfessionSkillCharge(s, 1);
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
                addProfessionSkillCharge(s, 1);
                s.professionCharge++;
            }
            if (s.professionCharge >= 3) {
                Enemy e = firstLiving(s);
                if (e != null) {
                    damageEnemy(s, e, 9 + s.act * 2 + Math.max(0, s.maxHp - s.hp) / 8, true);
                    e.vulnerable += hasTalent(s, "t_bloodbound_scar") ? 1 : 0;
                }
                s.hp = Math.min(s.maxHp, s.hp + (hasTalent(s, "t_bloodbound_feast") ? 5 : 2));
                if (hasTalent(s, "t_bloodbound_hemocraft")) {
                    gainBlock(s, 5 + s.act);
                    addStatusCard(s, "wound");
                }
                s.professionCharge = 0;
            }
        }
        if (hasRelic(s, "blood_crown") && (d.hpLoss > 0 || "wound".equals(c.id))) {
            gainBlock(s, 3 + s.act);
            if (s.hp <= s.maxHp / 2) {
                s.hp = Math.min(s.maxHp, s.hp + 2);
            }
        }
        if (PROF_WEAVER.equals(s.profession) && d.type == 1) {
            addProfessionSkillCharge(s, 1);
            s.professionCharge++;
            if (s.professionCharge >= 3) {
                draw(s, hasTalent(s, "t_weaver_mastery") ? 2 : 1);
                s.energy += 1;
                upgradeRandomHandCard(s);
                s.professionCharge = 0;
            }
        }
        if (hasRelic(s, "clockwork_loom") && (c.upgraded || d.upgradeRandom || d.scry > 0)) {
            gainBlock(s, 3 + s.act);
            if (s.cardsPlayedThisTurn == 2 || s.cardsPlayedThisTurn == 5) {
                upgradeRandomHandCard(s);
            }
        }
        if (hasTalent(s, "t_weaver_grandpattern") && (c.upgraded || d.upgradeRandom || d.scry > 0)) {
            s.professionCharge++;
            if (s.professionCharge >= 4) {
                upgradeRandomHandCard(s);
                draw(s, 1);
                gainBlock(s, 4 + s.act);
                s.professionCharge = 0;
            }
        }
        if (PROF_SUMMONER.equals(s.profession) && (c.temp || d.createEcho || "summoner_sprite".equals(d.id) || "summoner_wisp".equals(d.id))) {
            addProfessionSkillCharge(s, 1);
            s.professionCharge++;
            gainBlock(s, 2 + Math.min(6, s.professionCharge));
            if (s.professionCharge >= 4) {
                Enemy e = firstLiving(s);
                if (e != null) {
                    e.bind += 2 + s.bindPower / 2;
                    damageEnemy(s, e, 6 + s.act * 2, true);
                }
                if (hasTalent(s, "t_summoner_swarm")) {
                    Card spirit = new Card("summoner_sprite");
                    spirit.temp = true;
                    addToHand(s, spirit);
                }
                s.professionCharge = 0;
            }
        }
        if (hasTalent(s, "t_summoner_overflow") && (c.temp || d.createEcho || "summoner_sprite".equals(d.id) || "summoner_wisp".equals(d.id))) {
            Enemy e = firstLiving(s);
            if (e != null) {
                e.bind += 1 + s.bindPower / 2;
            }
            if (s.cardsPlayedThisTurn % 3 == 0) {
                Card spirit = new Card("summoner_sprite");
                spirit.temp = true;
                addToHand(s, spirit);
                addProfessionSkillCharge(s, 1);
            }
        }
        if (hasRelic(s, "spirit_processional") && (c.temp || d.createEcho || "summoner_sprite".equals(d.id) || "summoner_wisp".equals(d.id))) {
            gainBlock(s, 2 + s.act);
            Enemy e = firstLiving(s);
            if (e != null) {
                e.bind += 1 + s.bindPower / 2;
            }
            if (s.cardsPlayedThisTurn == 3) {
                Card spirit = new Card("summoner_sprite");
                spirit.temp = true;
                addToHand(s, spirit);
            }
        }
        if (PROF_INSCRIBER.equals(s.profession) && (c.upgraded || d.upgradeRandom || d.vulnerable > 0 || d.bind > 0
                || d.addStatusToEnemy || d.createWound || "wound".equals(c.id) || "daze".equals(c.id))) {
            addProfessionSkillCharge(s, 1);
            s.professionCharge++;
            Enemy e = firstLiving(s);
            if (e != null) {
                if (d.vulnerable > 0 || d.bind > 0 || d.addStatusToEnemy) {
                    e.mark += 1;
                }
                if ("wound".equals(c.id) || "daze".equals(c.id)) {
                    e.vulnerable += 1;
                    damageEnemy(s, e, 4 + s.act * 2 + Math.min(10, upgradedCardCount(s)), true);
                }
            }
            if (hasTalent(s, "t_inscriber_archive") && ("wound".equals(c.id) || "daze".equals(c.id))) {
                s.energy++;
                draw(s, 1);
            }
            if (s.professionCharge >= 3) {
                upgradeRandomHandCard(s);
                gainBlock(s, 4 + s.act);
                if (hasTalent(s, "t_inscriber_rubbing")) {
                    draw(s, 1);
                }
                s.professionCharge = 0;
            }
        }
        if (hasTalent(s, "t_inscriber_etching") && (d.vulnerable > 0 || d.bind > 0 || d.addStatusToEnemy)) {
            for (Enemy e : livingEnemies(s)) {
                e.vulnerable += 1;
                if (s.cardsPlayedThisTurn % 2 == 0) {
                    e.bind += 1 + s.bindPower / 2;
                }
            }
        }
        if (hasTalent(s, "t_inscriber_grandcodex") && (c.upgraded || d.upgradeRandom || d.vulnerable > 0 || d.bind > 0
                || d.addStatusToEnemy || "wound".equals(c.id) || "daze".equals(c.id))) {
            if (s.cardsPlayedThisTurn % 3 == 0) {
                draw(s, 1);
                addProfessionSkillCharge(s, 1);
            }
        }
        if (PROF_HEXER.equals(s.profession) && ("wound".equals(c.id) || "daze".equals(c.id) || d.vulnerable > 0 || d.addStatusToEnemy)) {
            addProfessionSkillCharge(s, 1);
            Enemy e = firstLiving(s);
            if (e != null) {
                e.vulnerable += "wound".equals(c.id) || "daze".equals(c.id) ? 1 : 0;
                damageEnemy(s, e, 3 + s.act + Math.min(8, e.vulnerable * 2), true);
            }
            if (hasTalent(s, "t_hexer_darkdeal") && ("wound".equals(c.id) || "daze".equals(c.id))) {
                s.gold += 3 + s.act;
                draw(s, 1);
            }
        }
        if (hasTalent(s, "t_hexer_abysscurse") && ("wound".equals(c.id) || "daze".equals(c.id) || d.vulnerable > 0 || d.createWound || d.addStatusToEnemy)) {
            for (Enemy e : livingEnemies(s)) {
                e.vulnerable += 1;
                if ("wound".equals(c.id) || "daze".equals(c.id)) {
                    e.bind += 1 + s.bindPower / 2;
                }
                damageEnemy(s, e, 3 + s.act + Math.min(10, e.vulnerable * 2), true);
            }
            if ("wound".equals(c.id) || "daze".equals(c.id)) {
                gainBlock(s, 3 + s.act);
                addProfessionSkillCharge(s, 1);
            }
        }
        if (hasRelic(s, "fallen_crown") && ("wound".equals(c.id) || "daze".equals(c.id) || d.vulnerable > 0 || d.createWound || d.addStatusToEnemy)) {
            for (Enemy e : livingEnemies(s)) {
                e.vulnerable += 1;
            }
            if ("wound".equals(c.id) || "daze".equals(c.id)) {
                gainBlock(s, 4 + s.act);
            }
        }
        if (hasRelic(s, "living_codex") && (c.upgraded || d.upgradeRandom || d.vulnerable > 0 || d.bind > 0
                || d.addStatusToEnemy || d.createWound || "wound".equals(c.id) || "daze".equals(c.id))) {
            gainBlock(s, 3 + s.act);
            for (Enemy e : livingEnemies(s)) {
                e.vulnerable += 1;
                if ("wound".equals(c.id) || "daze".equals(c.id) || d.addStatusToEnemy) {
                    e.bind += 1 + s.bindPower / 2;
                }
            }
            if (s.cardsPlayedThisTurn == 3 || s.cardsPlayedThisTurn == 6) {
                upgradeRandomHandCard(s);
                addProfessionSkillCharge(s, 1);
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

    private static void trackPactAfterPlay(State s, Card c, CardDef d, boolean exhausted) {
        if (s.mode != MODE_COMBAT) {
            return;
        }
        s.pactMaxCardsTurn = Math.max(s.pactMaxCardsTurn, s.cardsPlayedThisTurn);
        if (exhausted || c.temp) {
            s.pactExhaustedCards++;
        }
        if (d != null && "wound".equals(d.id)) {
            s.pactSelfDamage++;
        }
        if (d != null && (c.temp || d.createEcho)) {
            s.pactTempCards++;
        }
        if (d != null && ("wound".equals(c.id) || "daze".equals(c.id) || d.createWound || d.vulnerable > 0 || d.addStatusToEnemy)) {
            s.pactStatusCards++;
        }
        if (d != null && (c.upgraded || d.upgradeRandom || d.scry > 0)) {
            s.pactForgeCards++;
        }
        if (d != null && (d.goldGain > 0 || d.goldDamage || d.goldBlock)) {
            s.pactGoldCards++;
        }
    }

    private static void trackQuestAfterPlay(State s, Card c, CardDef d, boolean exhausted) {
        if (s.mode != MODE_COMBAT || d == null) {
            return;
        }
        if (exhausted || c.temp || d.createEcho) {
            addQuestProgress(s, QUEST_ECHO, 1);
        }
        if (d.hpLoss > 0 || d.goldGain > 0 || d.goldDamage || d.goldBlock || "wound".equals(c.id)) {
            addQuestProgress(s, QUEST_BLOODCOIN, 1);
        }
        if (c.upgraded || d.upgradeRandom || d.scry > 0) {
            addQuestProgress(s, QUEST_FORGE, 1);
        }
        if (d.goldGain > 0 || d.goldDamage || d.goldBlock) {
            addQuestProgress(s, QUEST_TREASURE, 1);
        }
        if (d.createPotion) {
            addQuestProgress(s, QUEST_BREW, 1);
        }
    }

    private static void trackRunMilestones(State s, Card c, CardDef d, boolean exhausted) {
        if (s.mode != MODE_COMBAT) {
            return;
        }
        if (s.block >= 24 + s.act * 8) {
            s.runGuardMilestone++;
            if (s.runGuardMilestone >= 2) {
                grantRunMilestone(s, MILESTONE_GUARD, "铁壁成型");
            }
        }
        if (s.cardsPlayedThisTurn >= 6) {
            s.runComboMilestone++;
            if (s.runComboMilestone >= 2) {
                grantRunMilestone(s, MILESTONE_COMBO, "连打成型");
            }
        }
        int bestStatus = 0;
        for (Enemy e : livingEnemies(s)) {
            bestStatus = Math.max(bestStatus, e.burn + e.bind + e.vulnerable);
        }
        if (bestStatus >= 10 + s.act * 3) {
            s.runHexMilestone++;
            if (s.runHexMilestone >= 2) {
                grantRunMilestone(s, MILESTONE_HEX, "异常成型");
            }
        }
        if (exhausted || c.temp || d.createEcho) {
            s.runEchoMilestone++;
            if (s.runEchoMilestone >= 12) {
                grantRunMilestone(s, MILESTONE_ECHO, "回声成型");
            }
        }
        if (d.hpLoss > 0 || d.goldGain > 0 || d.goldDamage || d.goldBlock || "wound".equals(c.id)) {
            s.runBloodcoinMilestone++;
            if (s.runBloodcoinMilestone >= 10) {
                grantRunMilestone(s, MILESTONE_BLOODCOIN, "血币成型");
            }
        }
        if (c.upgraded || d.upgradeRandom || d.scry > 0) {
            s.runForgeMilestone++;
            if (s.runForgeMilestone >= 11) {
                grantRunMilestone(s, MILESTONE_FORGE, "工坊成型");
            }
        }
    }

    private static void grantRunMilestone(State s, int flag, String name) {
        if ((s.runMilestoneFlags & flag) != 0) {
            return;
        }
        s.runMilestoneFlags |= flag;
        int gold = 12 + s.act * 4;
        s.gold += gold;
        if (flag == MILESTONE_GUARD) {
            gainBlock(s, 8 + s.act * 3);
        } else if (flag == MILESTONE_COMBO) {
            s.energy++;
            draw(s, 1);
        } else if (flag == MILESTONE_HEX) {
            Enemy e = firstLiving(s);
            if (e != null) {
                e.vulnerable += 1;
                e.bind += 2 + s.bindPower / 2;
            }
        } else if (flag == MILESTONE_ECHO) {
            Card echo = new Card(PROF_SUMMONER.equals(s.profession) ? "summoner_sprite" : "quick_cut");
            echo.temp = true;
            addToHand(s, echo);
            addProfessionSkillCharge(s, 2);
        } else if (flag == MILESTONE_BLOODCOIN) {
            s.hp = Math.min(s.maxHp, s.hp + 5 + s.act);
            addProfessionSkillCharge(s, 2);
        } else if (flag == MILESTONE_FORGE) {
            upgradeRandomHandCard(s);
            upgradeRandomDeckCard(s);
        }
        log(s, "构筑里程碑：" + name + "，获得 " + gold + " 金币。");
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
            s.pactKills++;
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
        if (s.mode == MODE_COMBAT) {
            s.pactMaxBlock = Math.max(s.pactMaxBlock, s.block);
        }
        if (hasRelic(s, "steel_oath") && amount >= 10) {
            s.block += 2;
            if (s.mode == MODE_COMBAT) {
                s.pactMaxBlock = Math.max(s.pactMaxBlock, s.block);
            }
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
        } else if (isCumulativeQuest(s.combatQuest) && s.questProgress >= s.questTarget) {
            s.questComplete = true;
        }
    }

    private static boolean isCumulativeQuest(int quest) {
        return quest == QUEST_BREW || quest == QUEST_SKILL || quest == QUEST_ECHO
                || quest == QUEST_BLOODCOIN || quest == QUEST_FORGE || quest == QUEST_TREASURE;
    }

    private static void addQuestProgress(State s, int quest, int amount) {
        if (s == null || s.mode != MODE_COMBAT || s.combatQuest != quest || s.questComplete || amount <= 0) {
            return;
        }
        s.questProgress += amount;
        if (s.questProgress >= s.questTarget) {
            s.questComplete = true;
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
        if (anyProfessionWinsAtLeast(s, 3)) {
            unlockAchievement(s, "profession_adept");
        }
        if (anyProfessionWinsAtLeast(s, 5)) {
            unlockAchievement(s, "profession_master");
        }
        if (allProfessionWinsAtLeast(s, 3)) {
            unlockAchievement(s, "all_mastery");
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
        return allProfessionWinsAtLeast(s, 1);
    }

    private static boolean anyProfessionWinsAtLeast(State s, int wins) {
        if (s.meta.professionWins.length < PROFESSIONS.length) {
            return false;
        }
        for (int i = 0; i < PROFESSIONS.length; i++) {
            if (s.meta.professionWins[i] >= wins) {
                return true;
            }
        }
        return false;
    }

    private static boolean allProfessionWinsAtLeast(State s, int wins) {
        if (s.meta.professionWins.length < PROFESSIONS.length) {
            return false;
        }
        for (int i = 0; i < PROFESSIONS.length; i++) {
            if (s.meta.professionWins[i] < wins) {
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
        gold += routeGoldBonus(s);
        s.gold += gold;
        awardQuest(s);
        awardPact(s);
        maybeRouteUpgrade(s);
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
        if (hasRelic(s, "golden_throne") && s.gold >= 160) {
            rewardCount++;
        }
        if (hasTalent(s, "t_shared_wayfarer") && s.combatKind == 'C' && s.run.nextInt(100) < 35) {
            rewardCount++;
        }
        if (s.encounterModifier == MOD_POLLUTED || s.encounterModifier == MOD_TURBULENT) {
            rewardCount++;
        }
        rewardCount += routeRewardCards(s);
        HashSet<String> offeredCards = new HashSet<>();
        for (int i = 0; i < rewardCount; i++) {
            CardDef d = randomCard(s, s.origin, s.combatKind != 'C' || s.encounterModifier == MOD_FRENZY || hasRelic(s, "scarlet_dice") || routeAllowsRare(s), offeredCards);
            offeredCards.add(d.id);
            RewardCard rc = new RewardCard();
            rc.id = d.id;
            rc.hint = rewardCardHint(s, d);
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
        } else if (s.combatKind == 'E' || s.run.nextInt(100) < (s.encounterModifier == MOD_BOUNTY ? 34 : 18) + routeRelicChanceBonus(s)) {
            s.relicRewards.add(randomRelic(s).id);
        }
        int potionChance = hasTalent(s, "t_shared_apothecary") ? 54 : 34;
        if (s.currentRoute == ROUTE_SUPPLY) {
            potionChance += 18;
        }
        if (s.potions.size() < potionLimit(s) && s.run.nextInt(100) < potionChance) {
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
            if (s.act < 2 && isAdvancedTalent(t.id)) {
                continue;
            }
            if (!s.talents.contains(t.id) && (t.profession.length() == 0 || t.profession.equals(s.profession))) {
                pool.add(t);
            }
        }
        Collections.shuffle(pool, s.run);
        Collections.sort(pool, new Comparator<TalentDef>() {
            @Override
            public int compare(TalentDef a, TalentDef b) {
                return talentSynergyScore(s, b.id) - talentSynergyScore(s, a.id);
            }
        });
        for (int i = 0; i < 3 && i < pool.size(); i++) {
            s.talentChoices.add(pool.get(i).id);
        }
        if (s.talentChoices.isEmpty()) {
            advanceSkillSpec(s);
            nextAct(s);
        } else {
            s.mode = MODE_TALENT;
        }
    }

    private static void openShop(State s) {
        s.mode = MODE_SHOP;
        applyRouteArrival(s, '$');
        s.shopCards.clear();
        s.shopRelics.clear();
        s.shopPotions.clear();
        s.shopScoutUsed = false;
        if (hasRelic(s, "golden_throne")) {
            int income = 18 + s.act * 8;
            s.gold += income;
            log(s, "金座裂币入账 " + income + "。");
        }
        if (hasTalent(s, "t_merchant_interest")) {
            int income = 20 + s.act * 10;
            s.gold += income;
            log(s, "复利账本入账 " + income + " 金币。");
        }
        HashSet<String> offeredCards = new HashSet<>();
        int shopCardCount = hasTalent(s, "t_merchant_blackmarket") ? 6 : 5;
        if (s.currentRoute == ROUTE_SECRET) {
            shopCardCount++;
        }
        for (int i = 0; i < shopCardCount; i++) {
            CardDef d = randomCard(s, s.origin, true, offeredCards);
            offeredCards.add(d.id);
            s.shopCards.add(d.id);
        }
        int relicCount = s.currentRoute == ROUTE_RICH ? 4 : 3;
        for (int i = 0; i < relicCount; i++) {
            s.shopRelics.add(randomRelic(s).id);
        }
        int potionCount = s.currentRoute == ROUTE_SUPPLY ? 4 : 3;
        for (int i = 0; i < potionCount; i++) {
            s.shopPotions.add(POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size())).id);
        }
        log(s, "抵达裂隙商栈。" + (s.currentRoute == ROUTE_NONE ? "" : " 路线：" + routeName(s.currentRoute)));
    }

    private static void openRest(State s) {
        s.mode = MODE_REST;
        applyRouteArrival(s, 'R');
        if (hasRelic(s, "dawn_pin")) {
            s.hp = Math.min(s.maxHp, s.hp + 5);
        }
        if (s.currentRoute == ROUTE_FORGE) {
            Card c = randomUpgradeableCard(s);
            if (c != null) {
                c.upgraded = true;
                log(s, "工坊营地预先升级了：" + card(c.id).name);
            }
        }
        log(s, "找到一处静火营地。" + (s.currentRoute == ROUTE_NONE ? "" : " 路线：" + routeName(s.currentRoute)));
    }

    private static void openEvent(State s) {
        s.mode = MODE_EVENT;
        applyRouteArrival(s, '?');
        if (s.currentRoute == ROUTE_SECRET && s.run.nextInt(100) < 45) {
            int[] rareEvents = {2, 4, 7, 10, 11};
            s.eventId = rareEvents[s.run.nextInt(rareEvents.length)];
        } else {
            s.eventId = s.run.nextInt(EVENT_COUNT);
        }
        if (hasTalent(s, "t_shared_wayfarer")) {
            s.gold += 12 + s.act * 3;
            s.hp = Math.min(s.maxHp, s.hp + 3);
            log(s, "旅人直觉让你在事件前整理资源。");
        }
    }

    private static CardDef randomCard(State s, String origin, boolean allowRare) {
        return randomCard(s, origin, allowRare, Collections.<String>emptySet());
    }

    private static CardDef randomProfessionCard(State s, boolean allowRare) {
        ArrayList<CardDef> pool = new ArrayList<>();
        for (CardDef d : CARD_LIBRARY) {
            if (d.type == 3 || !d.profession.equals(s.profession)) {
                continue;
            }
            if (!allowRare && d.rarity == 2) {
                continue;
            }
            if (s.act < 2 && isCapstoneCard(d.id)) {
                continue;
            }
            int weight = d.rarity == 0 ? 7 : d.rarity == 1 ? 4 : 2;
            for (int i = 0; i < weight; i++) {
                pool.add(d);
            }
        }
        if (pool.isEmpty()) {
            return randomCard(s, s.origin, allowRare);
        }
        return pool.get(s.run.nextInt(pool.size()));
    }

    private static CardDef randomTypeCard(State s, int type, boolean allowRare) {
        ArrayList<CardDef> pool = new ArrayList<>();
        for (CardDef d : CARD_LIBRARY) {
            if (d.type != type) {
                continue;
            }
            if (!allowRare && d.rarity == 2) {
                continue;
            }
            if (s.act < 2 && isCapstoneCard(d.id)) {
                continue;
            }
            int weight = d.rarity == 0 ? 6 : d.rarity == 1 ? 4 : 2;
            if (d.origin.equals(s.origin)) {
                weight += 3;
            }
            if (d.profession.equals(s.profession)) {
                weight += 5;
            }
            weight += professionCardBonus(s, d);
            weight += skillSpecCardBonus(s, d);
            for (int i = 0; i < weight; i++) {
                pool.add(d);
            }
        }
        return pool.isEmpty() ? null : pool.get(s.run.nextInt(pool.size()));
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
            if (s.act < 2 && isCapstoneCard(d.id)) {
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
            weight += relicCardBonus(s, d);
            weight += skillSpecCardBonus(s, d);
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

    private static CardDef randomBuildScoutCard(State s, boolean allowRare, Set<String> excluded) {
        int focus = buildScoutFocus(s);
        ArrayList<CardDef> pool = new ArrayList<>();
        for (CardDef d : CARD_LIBRARY) {
            if (d.type == 3 || excluded.contains(d.id)) {
                continue;
            }
            if (!allowRare && d.rarity == 2) {
                continue;
            }
            if (s.act < 2 && isCapstoneCard(d.id)) {
                continue;
            }
            int focusValue = buildFocusCardValue(d, focus);
            if (focusValue <= 0) {
                continue;
            }
            boolean offOrigin = !"通用".equals(d.origin) && !d.origin.equals(s.origin);
            boolean offProfession = d.profession.length() > 0 && !d.profession.equals(s.profession);
            int weight = 2 + d.rarity + Math.min(22, focusValue);
            if (d.origin.equals(s.origin) || "通用".equals(d.origin)) {
                weight += 4;
            } else if (offOrigin) {
                weight -= 3;
            }
            if (d.profession.equals(s.profession)) {
                weight += 8;
            } else if (offProfession) {
                weight -= 6;
            }
            weight += professionCardBonus(s, d);
            weight += relicCardBonus(s, d);
            weight += skillSpecCardBonus(s, d);
            if (weight <= 0) {
                continue;
            }
            for (int i = 0; i < weight; i++) {
                pool.add(d);
            }
        }
        if (pool.isEmpty()) {
            return randomCard(s, s.origin, allowRare, excluded);
        }
        return pool.get(s.run.nextInt(pool.size()));
    }

    private static int buildScoutFocus(State s) {
        if (s == null) {
            return BUILD_CYCLE;
        }
        return topBuildFocuses(s, 1)[0];
    }

    private static int[] topBuildFocuses(State s, int count) {
        int size = Math.max(1, Math.min(count, BUILD_FOCUS_NAMES.length));
        int[] top = new int[size];
        int[] scores = new int[size];
        for (int i = 0; i < size; i++) {
            top[i] = BUILD_CYCLE;
            scores[i] = -9999;
        }
        for (int focus = 0; focus < BUILD_FOCUS_NAMES.length; focus++) {
            int score = buildScoutFocusScore(s, focus);
            for (int slot = 0; slot < size; slot++) {
                if (score > scores[slot]) {
                    for (int move = size - 1; move > slot; move--) {
                        scores[move] = scores[move - 1];
                        top[move] = top[move - 1];
                    }
                    scores[slot] = score;
                    top[slot] = focus;
                    break;
                }
            }
        }
        return top;
    }

    private static int buildScoutFocusScore(State s, int focus) {
        int score = professionFocusBonus(s, focus) + pactFocusBonus(s, focus) + questFocusBonus(s, focus);
        for (Card c : s.deck) {
            CardDef d = card(c.id);
            int value = buildFocusCardValue(d, focus);
            if (value > 0) {
                score += Math.min(7, value);
                if (c.upgraded) {
                    score++;
                }
            }
        }
        if (focus == BUILD_OVERLOAD) {
            score += Math.min(8, s.professionSkillCharge / 2);
            if (hasSkillRelic(s)) {
                score += 5;
            }
        } else if (focus == BUILD_GOLD) {
            score += Math.min(12, s.gold / 35);
            if (hasRelic(s, "tithe_box") || hasRelic(s, "kingmaker_seal")) {
                score += 5;
            }
        } else if (focus == BUILD_FORGE && s.runForgeMilestone > 0) {
            score += Math.min(8, s.runForgeMilestone);
        } else if (focus == BUILD_ECHO && s.runEchoMilestone > 0) {
            score += Math.min(8, s.runEchoMilestone);
        } else if (focus == BUILD_BLOOD && s.runBloodcoinMilestone > 0) {
            score += Math.min(8, s.runBloodcoinMilestone);
        }
        return score;
    }

    private static String buildFocusRank(int score) {
        if (score >= 80) return "S";
        if (score >= 55) return "A";
        if (score >= 35) return "B";
        return "C";
    }

    private static int buildFocusDeckCards(State s, int focus) {
        int count = 0;
        for (Card c : s.deck) {
            CardDef d = card(c.id);
            if (buildFocusCardValue(d, focus) > 0) {
                count++;
            }
        }
        return count;
    }

    private static int professionFocusBonus(State s, int focus) {
        if (PROF_WARDEN.equals(s.profession)) {
            return focus == BUILD_GUARD ? 18 : focus == BUILD_OVERLOAD || focus == BUILD_FORGE ? 6 : 0;
        }
        if (PROF_DUELIST.equals(s.profession)) {
            return focus == BUILD_CYCLE ? 18 : focus == BUILD_OVERLOAD ? 8 : 0;
        }
        if (PROF_ALCHEMIST.equals(s.profession)) {
            return focus == BUILD_BREW ? 18 : focus == BUILD_STATUS ? 10 : 0;
        }
        if (PROF_RANGER.equals(s.profession)) {
            return focus == BUILD_STATUS ? 18 : focus == BUILD_CYCLE || focus == BUILD_GUARD ? 5 : 0;
        }
        if (PROF_ARCANIST.equals(s.profession)) {
            return focus == BUILD_ECHO ? 18 : focus == BUILD_CYCLE || focus == BUILD_OVERLOAD ? 8 : 0;
        }
        if (PROF_MERCHANT.equals(s.profession)) {
            return focus == BUILD_GOLD ? 20 : focus == BUILD_CYCLE ? 6 : 0;
        }
        if (PROF_BLOODBOUND.equals(s.profession)) {
            return focus == BUILD_BLOOD ? 20 : focus == BUILD_STATUS ? 8 : 0;
        }
        if (PROF_WEAVER.equals(s.profession)) {
            return focus == BUILD_FORGE ? 18 : focus == BUILD_CYCLE || focus == BUILD_ECHO ? 8 : 0;
        }
        if (PROF_SUMMONER.equals(s.profession)) {
            return focus == BUILD_ECHO ? 16 : focus == BUILD_STATUS || focus == BUILD_CYCLE ? 7 : 0;
        }
        if (PROF_HEXER.equals(s.profession)) {
            return focus == BUILD_STATUS ? 20 : focus == BUILD_BLOOD || focus == BUILD_ECHO ? 8 : 0;
        }
        if (PROF_INSCRIBER.equals(s.profession)) {
            return focus == BUILD_FORGE ? 18 : focus == BUILD_STATUS ? 14 : focus == BUILD_OVERLOAD || focus == BUILD_CYCLE ? 7 : 0;
        }
        return 0;
    }

    private static int pactFocusBonus(State s, int focus) {
        if ("pact_guardian".equals(s.pact)) return focus == BUILD_GUARD ? 14 : 0;
        if ("pact_sprinter".equals(s.pact)) return focus == BUILD_CYCLE ? 14 : 0;
        if ("pact_brewer".equals(s.pact)) return focus == BUILD_BREW ? 14 : focus == BUILD_STATUS ? 5 : 0;
        if ("pact_hunter".equals(s.pact)) return focus == BUILD_STATUS ? 12 : 0;
        if ("pact_void".equals(s.pact)) return focus == BUILD_ECHO ? 14 : focus == BUILD_CYCLE ? 5 : 0;
        if ("pact_blood".equals(s.pact)) return focus == BUILD_BLOOD ? 15 : 0;
        if ("pact_summon".equals(s.pact)) return focus == BUILD_ECHO ? 15 : 0;
        if ("pact_hex".equals(s.pact)) return focus == BUILD_STATUS ? 15 : focus == BUILD_BLOOD ? 5 : 0;
        if ("pact_forge".equals(s.pact)) return focus == BUILD_FORGE ? 15 : 0;
        if ("pact_merchant".equals(s.pact)) return focus == BUILD_GOLD ? 15 : 0;
        return 0;
    }

    private static int questFocusBonus(State s, int focus) {
        if (s.combatQuest == QUEST_GUARD) return focus == BUILD_GUARD ? 10 : 0;
        if (s.combatQuest == QUEST_COMBO || s.combatQuest == QUEST_LEAN || s.combatQuest == QUEST_SWIFT) return focus == BUILD_CYCLE ? 10 : 0;
        if (s.combatQuest == QUEST_HEX) return focus == BUILD_STATUS ? 10 : 0;
        if (s.combatQuest == QUEST_BREW) return focus == BUILD_BREW ? 12 : focus == BUILD_STATUS ? 4 : 0;
        if (s.combatQuest == QUEST_SKILL) return focus == BUILD_OVERLOAD ? 12 : 0;
        if (s.combatQuest == QUEST_ECHO) return focus == BUILD_ECHO ? 12 : 0;
        if (s.combatQuest == QUEST_BLOODCOIN) return focus == BUILD_BLOOD || focus == BUILD_GOLD ? 10 : 0;
        if (s.combatQuest == QUEST_FORGE) return focus == BUILD_FORGE ? 12 : 0;
        if (s.combatQuest == QUEST_TREASURE) return focus == BUILD_GOLD ? 10 : 0;
        return 0;
    }

    private static int buildFocusCardValue(CardDef d, int focus) {
        if (d == null) {
            return 0;
        }
        if (focus == BUILD_OVERLOAD) {
            return d.skillChargeGain * 4 + (d.energyGain > 0 ? 3 : 0) + (d.draw > 0 ? 2 : 0)
                    + ("overload_conduit".equals(d.id) ? 10 : 0) + ("cycle_metronome".equals(d.id) ? 4 : 0)
                    + ("inscriber_overseal".equals(d.id) ? 4 : 0) + ("inscriber_codex".equals(d.id) ? 5 : 0);
        }
        if (focus == BUILD_ECHO) {
            return (d.createEcho ? 9 : 0) + (d.exhaust ? 5 : 0) + (d.exhaustTopDiscard ? 6 : 0)
                    + (d.exhaustForDamage ? 6 : 0) + ("summoner_sprite".equals(d.echoCardId) ? 3 : 0)
                    + ("echo_matrix".equals(d.id) ? 10 : 0);
        }
        if (focus == BUILD_BREW) {
            return (d.createPotion ? 10 : 0) + d.burn * 2 + d.bind * 2 + (d.spreadStatus ? 5 : 0)
                    + (d.gainBurnPower + d.gainBindPower) * 3 + ("brew_crucible".equals(d.id) ? 10 : 0);
        }
        if (focus == BUILD_GOLD) {
            return d.goldGain / 2 + (d.goldDamage ? 9 : 0) + (d.goldBlock ? 9 : 0)
                    + ("golden_engine".equals(d.id) ? 10 : 0);
        }
        if (focus == BUILD_BLOOD) {
            return d.hpLoss * 4 + d.heal * 2 + (d.createWound ? 9 : 0) + ("wound".equals(d.id) ? 5 : 0)
                    + ("crimson_loop".equals(d.id) ? 10 : 0);
        }
        if (focus == BUILD_FORGE) {
            return (d.upgradeRandom ? 10 : 0) + d.scry * 2 + d.upgradeCostDrop * 3 + (d.rarity == 2 ? 2 : 0)
                    + ("forge_blueprint".equals(d.id) ? 10 : 0) + ("inscriber_codex".equals(d.id) ? 5 : 0);
        }
        if (focus == BUILD_STATUS) {
            return d.burn * 2 + d.bind * 2 + d.vulnerable * 5 + (d.addStatusToEnemy ? 7 : 0)
                    + (d.spreadStatus ? 8 : 0) + (d.createWound ? 4 : 0) + ("plague_vector".equals(d.id) ? 10 : 0)
                    + ("inscriber_glyphstorm".equals(d.id) ? 5 : 0) + ("inscriber_codex".equals(d.id) ? 5 : 0);
        }
        if (focus == BUILD_CYCLE) {
            return d.draw * 6 + d.energyGain * 8 + (d.cost == 0 ? 5 : 0) + d.comboDamage / 2
                    + ("cycle_metronome".equals(d.id) ? 10 : 0) + ("inscriber_palimp".equals(d.id) ? 4 : 0);
        }
        if (focus == BUILD_GUARD) {
            return d.block * 2 + (d.blockToDamage ? 8 : 0) + (d.retainBlock ? 6 : 0) + d.gainSteelEngine * 5
                    + (d.burnToBlock ? 4 : 0) + ("aegis_engine".equals(d.id) ? 10 : 0);
        }
        return 0;
    }

    public static int talentSynergyScore(State s, String id) {
        if (s == null || id == null) {
            return 0;
        }
        TalentDef t = talent(id);
        if (t == null) {
            return 0;
        }
        int score = 8;
        if (t.profession.length() == 0) {
            score += 10;
        } else if (t.profession.equals(s.profession)) {
            score += isAdvancedTalent(id) ? 24 : 18;
        } else {
            score -= 20;
        }
        int[] top = topBuildFocuses(s, 3);
        for (int i = 0; i < top.length; i++) {
            int focus = top[i];
            int value = talentFocusValue(id, focus);
            if (value <= 0) {
                continue;
            }
            score += value * (i == 0 ? 7 : i == 1 ? 5 : 3);
        }
        score += talentContextBonus(s, id);
        if (isAdvancedTalent(id)) {
            score += 5 + s.act * 2;
        }
        return score;
    }

    private static int talentContextBonus(State s, String id) {
        int bonus = 0;
        int upgraded = upgradedDeckCards(s);
        int status = statusDeckCards(s);
        int zeroCost = zeroCostDeckCards(s);
        int professionCards = professionDeckCards(s);
        int potionsOpen = potionLimit(s) - s.potions.size();
        if ("t_shared_masterwork".equals(id)) bonus += upgraded < 4 ? 12 : 5;
        else if ("t_shared_hunter".equals(id)) bonus += professionCards >= 5 ? 9 : 4;
        else if ("t_shared_longnight".equals(id)) bonus += s.deck.size() >= 24 ? 10 : 3;
        else if ("t_shared_wayfarer".equals(id)) bonus += s.hp < s.maxHp * 0.7f ? 8 : 5;
        else if ("t_shared_apothecary".equals(id)) bonus += potionsOpen > 0 ? 10 + potionsOpen * 3 : 2;
        else if ("t_warden_bastion".equals(id)) bonus += s.hp < s.maxHp * 0.7f ? 10 : 4;
        else if ("t_warden_counter".equals(id)) bonus += buildFocusDeckCards(s, BUILD_GUARD) * 2;
        else if ("t_warden_armory".equals(id)) bonus += upgraded < 5 ? 8 : 3;
        else if ("t_duelist_tempo".equals(id)) bonus += zeroCost * 4;
        else if ("t_duelist_execution".equals(id)) bonus += buildFocusDeckCards(s, BUILD_STATUS) * 2;
        else if ("t_duelist_gambit".equals(id)) bonus += buildFocusDeckCards(s, BUILD_CYCLE) * 2;
        else if ("t_alchemist_reserve".equals(id)) bonus += potionsOpen > 0 ? 12 : 2;
        else if ("t_alchemist_plague".equals(id)) bonus += buildFocusDeckCards(s, BUILD_STATUS) * 2;
        else if ("t_alchemist_distiller".equals(id)) bonus += potionCards(s) * 4 + potionsOpen * 2;
        else if ("t_ranger_quarry".equals(id)) bonus += buildFocusDeckCards(s, BUILD_STATUS) * 2;
        else if ("t_ranger_net".equals(id)) bonus += bindDeckCards(s) * 3;
        else if ("t_ranger_wildpath".equals(id)) bonus += s.deck.size() >= 24 ? 6 : 3;
        else if ("t_arcanist_rewrite".equals(id)) bonus += buildFocusDeckCards(s, BUILD_ECHO) * 2;
        else if ("t_arcanist_overflow".equals(id)) bonus += buildFocusDeckCards(s, BUILD_CYCLE) * 2;
        else if ("t_arcanist_archive".equals(id)) bonus += exhaustDeckCards(s) * 3;
        else if ("t_merchant_interest".equals(id)) bonus += s.gold >= 120 ? 10 : 4;
        else if ("t_merchant_contract".equals(id)) bonus += s.gold >= 100 ? 8 : 4;
        else if ("t_merchant_blackmarket".equals(id)) bonus += s.gold >= 140 ? 8 : 5;
        else if ("t_bloodbound_scar".equals(id)) bonus += s.hp < s.maxHp * 0.75f ? 10 : 4;
        else if ("t_bloodbound_feast".equals(id)) bonus += s.hp < s.maxHp * 0.65f ? 12 : 4;
        else if ("t_bloodbound_crimson".equals(id)) bonus += status <= 2 ? 6 : 2;
        else if ("t_weaver_setup".equals(id)) bonus += upgraded < 5 ? 8 : 4;
        else if ("t_weaver_mastery".equals(id)) bonus += upgraded * 2;
        else if ("t_weaver_quicksilver".equals(id)) bonus += buildFocusDeckCards(s, BUILD_CYCLE) * 2;
        else if ("t_summoner_court".equals(id)) bonus += buildFocusDeckCards(s, BUILD_ECHO) * 2;
        else if ("t_summoner_bond".equals(id)) bonus += s.hp < s.maxHp * 0.75f ? 9 : 4;
        else if ("t_summoner_swarm".equals(id)) bonus += tempOrEchoDeckCards(s) * 3;
        else if ("t_hexer_darkdeal".equals(id)) bonus += status * 3 + Math.min(8, s.gold / 40);
        else if ("t_hexer_malediction".equals(id)) bonus += buildFocusDeckCards(s, BUILD_STATUS) * 2;
        else if ("t_hexer_cleanse".equals(id)) bonus += status * 5;
        else if ("t_inscriber_rubbing".equals(id)) bonus += upgraded < 5 ? 10 : 4;
        else if ("t_inscriber_etching".equals(id)) bonus += buildFocusDeckCards(s, BUILD_STATUS) * 2;
        else if ("t_inscriber_archive".equals(id)) bonus += status * 4 + exhaustDeckCards(s) * 2;
        else if ("t_warden_vanguard".equals(id)) bonus += buildFocusDeckCards(s, BUILD_GUARD) * 2;
        else if ("t_duelist_masterstep".equals(id)) bonus += zeroCost * 3 + buildFocusDeckCards(s, BUILD_CYCLE);
        else if ("t_alchemist_grandbrew".equals(id)) bonus += potionCards(s) * 4 + buildFocusDeckCards(s, BUILD_STATUS);
        else if ("t_ranger_apex".equals(id)) bonus += bindDeckCards(s) * 3;
        else if ("t_arcanist_singularity".equals(id)) bonus += exhaustDeckCards(s) * 3 + tempOrEchoDeckCards(s) * 2;
        else if ("t_merchant_monopoly".equals(id)) bonus += Math.min(14, s.gold / 25) + buildFocusDeckCards(s, BUILD_GOLD) * 2;
        else if ("t_bloodbound_hemocraft".equals(id)) bonus += status * 2 + buildFocusDeckCards(s, BUILD_BLOOD) * 2;
        else if ("t_weaver_grandpattern".equals(id)) bonus += upgraded * 2 + buildFocusDeckCards(s, BUILD_FORGE) * 2;
        else if ("t_summoner_overflow".equals(id)) bonus += tempOrEchoDeckCards(s) * 3;
        else if ("t_hexer_abysscurse".equals(id)) bonus += status * 3 + buildFocusDeckCards(s, BUILD_STATUS) * 2;
        else if ("t_inscriber_grandcodex".equals(id)) bonus += upgraded * 2 + status * 2 + buildFocusDeckCards(s, BUILD_FORGE) * 2;
        return Math.min(36, bonus);
    }

    private static int talentFocusValue(String id, int focus) {
        if (focus == BUILD_OVERLOAD) {
            return isAny(id, "t_warden_vanguard", "t_duelist_masterstep", "t_alchemist_grandbrew",
                    "t_arcanist_singularity", "t_merchant_monopoly", "t_weaver_grandpattern",
                    "t_inscriber_grandcodex", "t_shared_longnight") ? 3 : 0;
        }
        if (focus == BUILD_ECHO) {
            return isAny(id, "t_arcanist_rewrite", "t_arcanist_overflow", "t_arcanist_archive",
                    "t_arcanist_singularity", "t_summoner_court", "t_summoner_swarm",
                    "t_summoner_overflow", "t_weaver_quicksilver") ? 3 : 0;
        }
        if (focus == BUILD_BREW) {
            return isAny(id, "t_shared_apothecary", "t_alchemist_reserve", "t_alchemist_plague",
                    "t_alchemist_distiller", "t_alchemist_grandbrew") ? 3 : 0;
        }
        if (focus == BUILD_GOLD) {
            return isAny(id, "t_shared_hunter", "t_shared_wayfarer", "t_merchant_interest",
                    "t_merchant_contract", "t_merchant_blackmarket", "t_merchant_monopoly",
                    "t_hexer_darkdeal") ? 3 : 0;
        }
        if (focus == BUILD_BLOOD) {
            return isAny(id, "t_bloodbound_scar", "t_bloodbound_feast", "t_bloodbound_crimson",
                    "t_bloodbound_hemocraft", "t_hexer_darkdeal", "t_hexer_abysscurse") ? 3 : 0;
        }
        if (focus == BUILD_FORGE) {
            return isAny(id, "t_shared_masterwork", "t_warden_armory", "t_weaver_setup",
                    "t_weaver_mastery", "t_weaver_grandpattern", "t_inscriber_rubbing",
                    "t_inscriber_grandcodex") ? 3 : 0;
        }
        if (focus == BUILD_STATUS) {
            return isAny(id, "t_alchemist_plague", "t_alchemist_distiller", "t_alchemist_grandbrew",
                    "t_ranger_quarry", "t_ranger_net", "t_ranger_wildpath", "t_ranger_apex",
                    "t_hexer_darkdeal", "t_hexer_malediction", "t_hexer_cleanse",
                    "t_hexer_abysscurse", "t_inscriber_etching", "t_inscriber_archive",
                    "t_inscriber_grandcodex", "t_duelist_execution") ? 3 : 0;
        }
        if (focus == BUILD_CYCLE) {
            return isAny(id, "t_duelist_tempo", "t_duelist_gambit", "t_duelist_masterstep",
                    "t_arcanist_overflow", "t_weaver_setup", "t_weaver_quicksilver",
                    "t_inscriber_archive", "t_shared_longnight") ? 3 : 0;
        }
        if (focus == BUILD_GUARD) {
            return isAny(id, "t_warden_bastion", "t_warden_counter", "t_warden_armory",
                    "t_warden_vanguard", "t_ranger_net", "t_bloodbound_scar",
                    "t_summoner_bond", "t_weaver_grandpattern") ? 3 : 0;
        }
        return 0;
    }

    private static String talentReason(State s, String id) {
        if (isAny(id, "t_shared_masterwork", "t_weaver_mastery", "t_weaver_grandpattern",
                "t_inscriber_rubbing", "t_inscriber_grandcodex") && upgradedDeckCards(s) >= 4) {
            return "升级牌多";
        }
        if (isAny(id, "t_hexer_cleanse", "t_hexer_darkdeal", "t_hexer_abysscurse",
                "t_inscriber_archive", "t_inscriber_grandcodex") && statusDeckCards(s) > 0) {
            return "状态牌可转化";
        }
        if (isAny(id, "t_duelist_tempo", "t_duelist_masterstep") && zeroCostDeckCards(s) >= 2) {
            return "低费连打";
        }
        if (isAny(id, "t_shared_apothecary", "t_alchemist_reserve", "t_alchemist_distiller", "t_alchemist_grandbrew")
                && s.potions.size() < potionLimit(s)) {
            return "药剂位可用";
        }
        if (isAny(id, "t_merchant_interest", "t_merchant_contract", "t_merchant_blackmarket", "t_merchant_monopoly")
                && s.gold >= 120) {
            return "金币充足";
        }
        if (isAny(id, "t_bloodbound_scar", "t_bloodbound_feast", "t_summoner_bond", "t_shared_wayfarer")
                && s.hp < s.maxHp * 0.75f) {
            return "补足生存";
        }
        if (isAny(id, "t_arcanist_archive", "t_arcanist_singularity") && exhaustDeckCards(s) >= 2) {
            return "消耗牌多";
        }
        if (isAny(id, "t_summoner_swarm", "t_summoner_overflow", "t_summoner_court") && tempOrEchoDeckCards(s) >= 2) {
            return "临时牌多";
        }
        return "";
    }

    private static int upgradedDeckCards(State s) {
        int count = 0;
        for (Card c : s.deck) {
            if (c.upgraded) {
                count++;
            }
        }
        return count;
    }

    private static int statusDeckCards(State s) {
        int count = 0;
        for (Card c : s.deck) {
            if ("wound".equals(c.id) || "daze".equals(c.id)) {
                count++;
            }
        }
        return count;
    }

    private static int zeroCostDeckCards(State s) {
        int count = 0;
        for (Card c : s.deck) {
            CardDef d = card(c.id);
            if (d != null && d.cost == 0) {
                count++;
            }
        }
        return count;
    }

    private static int professionDeckCards(State s) {
        int count = 0;
        for (Card c : s.deck) {
            CardDef d = card(c.id);
            if (d != null && d.profession.equals(s.profession)) {
                count++;
            }
        }
        return count;
    }

    private static int potionCards(State s) {
        int count = 0;
        for (Card c : s.deck) {
            CardDef d = card(c.id);
            if (d != null && d.createPotion) {
                count++;
            }
        }
        return count;
    }

    private static int bindDeckCards(State s) {
        int count = 0;
        for (Card c : s.deck) {
            CardDef d = card(c.id);
            if (d != null && (d.bind > 0 || d.bindToDraw)) {
                count++;
            }
        }
        return count;
    }

    private static int exhaustDeckCards(State s) {
        int count = 0;
        for (Card c : s.deck) {
            CardDef d = card(c.id);
            if (d != null && (d.exhaust || d.exhaustTopDiscard || d.exhaustForDamage)) {
                count++;
            }
        }
        return count;
    }

    private static int tempOrEchoDeckCards(State s) {
        int count = 0;
        for (Card c : s.deck) {
            CardDef d = card(c.id);
            if (c.temp || (d != null && (d.createEcho || "summoner_sprite".equals(d.echoCardId)))) {
                count++;
            }
        }
        return count;
    }

    private static String rewardCardHint(State s, CardDef d) {
        if (d == null) {
            return "";
        }
        int[] top = topBuildFocuses(s, 3);
        String tags = "";
        for (int i = 0; i < top.length; i++) {
            int focus = top[i];
            int value = buildFocusCardValue(d, focus);
            if (value > 0) {
                tags += (tags.length() == 0 ? "" : "/") + BUILD_FOCUS_NAMES[focus];
            }
        }
        String hint = tags.length() > 0 ? "契合 " + tags : "";
        if (d.profession.equals(s.profession)) {
            hint += (hint.length() == 0 ? "" : "  ") + "职业牌";
        }
        if (d.origin.equals(s.origin)) {
            hint += (hint.length() == 0 ? "" : "  ") + "同源";
        } else if ("通用".equals(d.origin)) {
            hint += (hint.length() == 0 ? "" : "  ") + "通用";
        }
        if (d.skillChargeGain > 0) {
            hint += (hint.length() == 0 ? "" : "  ") + "充能+" + d.skillChargeGain;
        }
        if (skillSpecCardBonus(s, d) >= 4) {
            hint += (hint.length() == 0 ? "" : "  ") + skillSpecName(s) + "适配";
        }
        if (d.rarity == 2) {
            hint += (hint.length() == 0 ? "" : "  ") + "稀有";
        }
        if (hint.length() == 0) {
            hint = d.cost == 0 ? "低费节奏" : d.draw > 0 ? "补充循环" : d.block > 0 ? "补足防线" : "泛用补强";
        }
        return hint;
    }

    public static String relicSynergyHint(State s, String id) {
        RelicDef r = relic(id);
        if (s == null || r == null) {
            return "";
        }
        String tags = "";
        int[] top = topBuildFocuses(s, 3);
        for (int i = 0; i < top.length; i++) {
            int focus = top[i];
            if (relicFocusValue(id, focus) > 0) {
                tags = appendHint(tags, BUILD_FOCUS_NAMES[focus]);
            }
        }
        String hint = tags.length() > 0 ? "契合 " + tags : "";
        int professionBonus = professionRelicBonus(s, id);
        if (professionBonus >= 4 || (professionBonus > 0 && isCapstoneRelic(id))) {
            hint = appendHint(hint, "职业终局");
        } else if (professionBonus > 0) {
            hint = appendHint(hint, "职业契合");
        }
        if (isSkillRelicForProfession(s, id)) {
            hint = appendHint(hint, "职业技");
        }
        if (skillSpecRelicBonus(s, id) > 0) {
            hint = appendHint(hint, "专修契合");
        }
        if (r.boss) {
            hint = appendHint(hint, "Boss");
        }
        if (s.relics.contains(id)) {
            hint = appendHint(hint, "已拥有");
        }
        if (hint.length() == 0) {
            hint = fallbackRelicHint(id);
        }
        return hint;
    }

    private static String appendHint(String hint, String part) {
        if (part == null || part.length() == 0) {
            return hint == null ? "" : hint;
        }
        if (hint == null || hint.length() == 0) {
            return part;
        }
        return hint + "  " + part;
    }

    private static int relicFocusValue(String id, int focus) {
        if (focus == BUILD_OVERLOAD) {
            return isAny(id, "sapphire_cell", "amber_quill", "tempo_metronome", "stormglass_seal",
                    "command_banner", "flash_heel", "catalyst_pump", "hawk_fletching", "echo_prism",
                    "ledger_stamp", "crimson_seal", "pattern_spool", "spirit_bell", "hex_tablet",
                    "engraver_stylus", "razor_pactstone", "tempo_spindle", "resonance_lens", "mastery_badge",
                    "ability_crown") ? 3 : 0;
        }
        if (focus == BUILD_ECHO) {
            return isAny(id, "void_lens", "arcane_ink", "hollow_crown", "void_abacus", "echo_prism",
                    "singularity_orb", "rift_compass", "spirit_bell", "spirit_processional", "void_anchor",
                    "echo_crown") ? 3 : 0;
        }
        if (focus == BUILD_BREW) {
            return isAny(id, "ember_core", "charcoal_sigil", "cinder_spoon", "green_bell", "alchemist_case",
                    "glass_vials", "emberroot_charm", "catalyst_pump", "solar_crucible", "hex_moon") ? 3 : 0;
        }
        if (focus == BUILD_GOLD) {
            return isAny(id, "hunter_mark", "empty_coin", "merchant_key", "merchant_scale", "tithe_box",
                    "ledger_stamp", "kingmaker_seal", "bloodcoin_broach", "runic_shackle", "golden_throne") ? 3 : 0;
        }
        if (focus == BUILD_BLOOD) {
            return isAny(id, "silver_suture", "cup_of_mist", "scar_talisman", "bloodcoin_broach",
                    "crimson_seal", "blood_crown", "blood_contract", "hex_moon") ? 3 : 0;
        }
        if (focus == BUILD_FORGE) {
            return isAny(id, "glass_anvil", "polished_cog", "loom_shuttle", "mirror_anvil",
                    "pattern_spool", "engraver_stylus", "clockwork_loom", "living_codex", "forge_heart",
                    "ability_crown") ? 3 : 0;
        }
        if (focus == BUILD_STATUS) {
            return isAny(id, "thorn_ring", "charcoal_sigil", "root_drum", "cinder_spoon", "green_bell",
                    "ranger_map", "glass_vials", "emberroot_charm", "stormglass_seal", "curse_censer",
                    "hawk_fletching", "solar_crucible", "apex_compass", "spirit_processional",
                    "fallen_crown", "engraver_stylus", "living_codex", "hex_moon") ? 3 : 0;
        }
        if (focus == BUILD_CYCLE) {
            return isAny(id, "void_lens", "amber_quill", "ink_fountain", "root_drum", "cracked_compass",
                    "moon_lantern", "tempo_metronome", "void_abacus", "flash_heel", "pattern_spool",
                    "tempo_spindle", "finale_rapier", "echo_crown") ? 3 : 0;
        }
        if (focus == BUILD_GUARD) {
            return isAny(id, "steel_oath", "bone_mask", "thorn_ring", "opal_scar", "warden_plate",
                    "vital_sprout", "polished_cog", "stormglass_seal", "bloodcoin_broach", "mirror_anvil",
                    "vigil_bloom", "command_banner", "aegis_throne", "forge_heart") ? 3 : 0;
        }
        return 0;
    }

    private static boolean isSkillRelicForProfession(State s, String id) {
        return (PROF_WARDEN.equals(s.profession) && "command_banner".equals(id))
                || (PROF_DUELIST.equals(s.profession) && "flash_heel".equals(id))
                || (PROF_ALCHEMIST.equals(s.profession) && "catalyst_pump".equals(id))
                || (PROF_RANGER.equals(s.profession) && "hawk_fletching".equals(id))
                || (PROF_ARCANIST.equals(s.profession) && "echo_prism".equals(id))
                || (PROF_MERCHANT.equals(s.profession) && "ledger_stamp".equals(id))
                || (PROF_BLOODBOUND.equals(s.profession) && "crimson_seal".equals(id))
                || (PROF_WEAVER.equals(s.profession) && "pattern_spool".equals(id))
                || (PROF_SUMMONER.equals(s.profession) && "spirit_bell".equals(id))
                || (PROF_HEXER.equals(s.profession) && "hex_tablet".equals(id))
                || (PROF_INSCRIBER.equals(s.profession) && "engraver_stylus".equals(id));
    }

    private static String fallbackRelicHint(String id) {
        if (isAny(id, "sapphire_cell", "ink_fountain", "amber_quill", "void_lens", "moon_lantern")) {
            return "泛用资源";
        }
        if (isAny(id, "ruby_branch", "black_bread", "leaf_charm", "cup_of_mist", "iron_tea")) {
            return "续航补强";
        }
        if (isAny(id, "cracked_compass", "scarlet_dice", "night_map", "dawn_pin")) {
            return "路线收益";
        }
        return "泛用补强";
    }

    private static boolean isAny(String id, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSkillRelic(State s) {
        return hasRelic(s, "command_banner") || hasRelic(s, "flash_heel") || hasRelic(s, "catalyst_pump")
                || hasRelic(s, "hawk_fletching") || hasRelic(s, "echo_prism") || hasRelic(s, "ledger_stamp")
                || hasRelic(s, "crimson_seal") || hasRelic(s, "pattern_spool") || hasRelic(s, "spirit_bell")
                || hasRelic(s, "hex_tablet") || hasRelic(s, "engraver_stylus");
    }

    private static CardDef randomOverloadCard(State s, boolean allowRare) {
        ArrayList<CardDef> pool = new ArrayList<>();
        for (CardDef d : CARD_LIBRARY) {
            if (d.type == 3 || d.skillChargeGain <= 0 || !d.profession.equals(s.profession)) {
                continue;
            }
            if (!allowRare && d.rarity == 2) {
                continue;
            }
            if (s.act < 2 && isCapstoneCard(d.id)) {
                continue;
            }
            int weight = d.rarity == 0 ? 8 : d.rarity == 1 ? 6 : 2;
            if (d.id.indexOf("over") >= 0) {
                weight += 5;
            }
            for (int i = 0; i < weight; i++) {
                pool.add(d);
            }
        }
        if (pool.isEmpty()) {
            return randomProfessionCard(s, allowRare);
        }
        return pool.get(s.run.nextInt(pool.size()));
    }

    private static CardDef randomOffPoolCard(State s, boolean allowRare) {
        ArrayList<CardDef> pool = new ArrayList<>();
        for (CardDef d : CARD_LIBRARY) {
            if (d.type == 3) {
                continue;
            }
            if (!allowRare && d.rarity == 2) {
                continue;
            }
            if (s.act < 2 && isCapstoneCard(d.id)) {
                continue;
            }
            boolean offOrigin = !"通用".equals(d.origin) && !d.origin.equals(s.origin);
            boolean offProfession = d.profession.length() > 0 && !d.profession.equals(s.profession);
            if (!offOrigin && !offProfession) {
                continue;
            }
            int weight = d.rarity == 0 ? 5 : d.rarity == 1 ? 4 : 2;
            if (d.createEcho || d.skillChargeGain > 0 || d.draw > 0) {
                weight += 2;
            }
            for (int i = 0; i < weight; i++) {
                pool.add(d);
            }
        }
        if (pool.isEmpty()) {
            return randomCard(s, s.origin, allowRare);
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
        if (PROF_SUMMONER.equals(s.profession) && (d.createEcho || d.bind > 0 || d.aoe || d.type == 1)) {
            return 4;
        }
        if (PROF_HEXER.equals(s.profession) && (d.vulnerable > 0 || d.addStatusToEnemy || d.createWound || "wound".equals(d.id) || "daze".equals(d.id))) {
            return 4;
        }
        if (PROF_INSCRIBER.equals(s.profession) && (d.upgradeRandom || d.scry > 0 || d.vulnerable > 0 || d.bind > 0
                || d.addStatusToEnemy || d.createWound || d.exhaustTopDiscard || d.skillChargeGain > 0)) {
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

    public static int skillSpecCardBonus(State s, CardDef d) {
        if (s == null || d == null) {
            return 0;
        }
        SkillSpecDef spec = skillSpec(s.skillSpec);
        if (spec == null) {
            return 0;
        }
        int bonus = 0;
        if ("spec_burst".equals(spec.id)) {
            if (d.damage > 0 || d.comboDamage > 0 || d.aoe) bonus += 3;
            if (d.vulnerable > 0 || d.skillChargeGain > 0) bonus += 2;
            if ("heavy_line".equals(d.id) || "clean_arc".equals(d.id)) bonus += 4;
        } else if ("spec_tempo".equals(spec.id)) {
            if (d.cost == 0 || d.draw > 0 || d.energyGain > 0) bonus += 3;
            if (d.createEcho || d.exhaust || d.skillChargeGain > 0) bonus += 2;
            if ("quick_cut".equals(d.id) || "double_step".equals(d.id) || "battle_trance".equals(d.id)) bonus += 4;
        } else if ("spec_sustain".equals(spec.id)) {
            if (d.block > 0 || d.heal > 0 || d.burnToBlock || d.goldBlock) bonus += 3;
            if (d.gainSteelEngine > 0 || d.retainBlock || d.type == 1) bonus += 2;
            if ("focus_breath".equals(d.id) || "last_light".equals(d.id) || "blood_suture".equals(d.id)) bonus += 4;
        } else if ("spec_resonance".equals(spec.id)) {
            int focus = buildScoutFocus(s);
            int value = buildFocusCardValue(d, focus);
            if (value > 0) bonus += Math.min(8, 2 + value / 4);
            if (d.skillChargeGain > 0) bonus += 2;
        } else if ("spec_mastery".equals(spec.id)) {
            if (d.profession.equals(s.profession)) bonus += 5;
            if (d.skillChargeGain > 0 || masteryOverloadCard(s.profession).equals(d.id)) bonus += 4;
        }
        if (bonus > 0) {
            bonus += Math.max(0, s.skillSpecLevel - 1);
        }
        return bonus;
    }

    private static int relicCardBonus(State s, CardDef d) {
        int bonus = 0;
        if (hasRelic(s, "emberroot_charm") && (d.burn > 0 || d.bind > 0)) {
            bonus += 3;
        }
        if (hasRelic(s, "stormglass_seal") && ((d.draw > 0 && d.block > 0) || (d.damage > 0 && (d.burn > 0 || d.bind > 0 || d.vulnerable > 0 || d.addStatusToEnemy)))) {
            bonus += 3;
        }
        if (hasRelic(s, "curse_censer") && (d.exhaust || d.createEcho || d.createWound || d.type == 3)) {
            bonus += 3;
        }
        if (hasRelic(s, "bloodcoin_broach") && (d.hpLoss > 0 || d.goldGain > 0 || d.goldDamage || d.goldBlock || d.createWound)) {
            bonus += 3;
        }
        if (hasRelic(s, "mirror_anvil") && (d.upgradeRandom || d.rarity == 2)) {
            bonus += 2;
        }
        if (hasRelic(s, "rift_compass")) {
            boolean offOrigin = !"通用".equals(d.origin) && !d.origin.equals(s.origin);
            boolean offProfession = d.profession.length() > 0 && !d.profession.equals(s.profession);
            if (offOrigin || offProfession || d.createEcho) {
                bonus += 3;
            }
        }
        return bonus;
    }

    private static RelicDef randomRelic(State s) {
        return randomRelic(s, Collections.<String>emptySet());
    }

    private static RelicDef randomRelic(State s, Set<String> excluded) {
        ArrayList<RelicDef> pool = new ArrayList<>();
        for (RelicDef r : RELIC_LIBRARY) {
            if (!r.boss && !s.relics.contains(r.id) && !excluded.contains(r.id)) {
                if (s.act < 2 && isCapstoneRelic(r.id)) {
                    continue;
                }
                int weight = 1 + professionRelicBonus(s, r.id) + skillSpecRelicBonus(s, r.id);
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

    private static String randomSkillRelicFor(State s) {
        String[] ids = {"command_banner", "flash_heel", "catalyst_pump", "hawk_fletching", "echo_prism", "ledger_stamp", "crimson_seal", "pattern_spool", "spirit_bell", "hex_tablet", "engraver_stylus"};
        if (PROF_WARDEN.equals(s.profession)) return "command_banner";
        if (PROF_DUELIST.equals(s.profession)) return "flash_heel";
        if (PROF_ALCHEMIST.equals(s.profession)) return "catalyst_pump";
        if (PROF_RANGER.equals(s.profession)) return "hawk_fletching";
        if (PROF_ARCANIST.equals(s.profession)) return "echo_prism";
        if (PROF_MERCHANT.equals(s.profession)) return "ledger_stamp";
        if (PROF_BLOODBOUND.equals(s.profession)) return "crimson_seal";
        if (PROF_WEAVER.equals(s.profession)) return "pattern_spool";
        if (PROF_SUMMONER.equals(s.profession)) return "spirit_bell";
        if (PROF_HEXER.equals(s.profession)) return "hex_tablet";
        if (PROF_INSCRIBER.equals(s.profession)) return "engraver_stylus";
        return ids[s.run.nextInt(ids.length)];
    }

    private static int professionRelicBonus(State s, String id) {
        if (PROF_WARDEN.equals(s.profession) && ("warden_plate".equals(id) || "steel_oath".equals(id) || "thorn_ring".equals(id) || "command_banner".equals(id))) {
            return 2;
        }
        if (PROF_WARDEN.equals(s.profession) && "aegis_throne".equals(id)) {
            return 4;
        }
        if (PROF_DUELIST.equals(s.profession) && ("duelist_sash".equals(id) || "amber_quill".equals(id) || "opal_scar".equals(id) || "tempo_metronome".equals(id) || "flash_heel".equals(id))) {
            return 2;
        }
        if (PROF_DUELIST.equals(s.profession) && "finale_rapier".equals(id)) {
            return 4;
        }
        if (PROF_ALCHEMIST.equals(s.profession) && ("alchemist_case".equals(id) || "cinder_spoon".equals(id) || "green_bell".equals(id) || "glass_vials".equals(id) || "catalyst_pump".equals(id))) {
            return 2;
        }
        if (PROF_ALCHEMIST.equals(s.profession) && "solar_crucible".equals(id)) {
            return 4;
        }
        if (PROF_RANGER.equals(s.profession) && ("ranger_map".equals(id) || "root_drum".equals(id) || "green_bell".equals(id) || "hawk_fletching".equals(id))) {
            return 2;
        }
        if (PROF_RANGER.equals(s.profession) && "apex_compass".equals(id)) {
            return 4;
        }
        if (PROF_ARCANIST.equals(s.profession) && ("arcane_ink".equals(id) || "hollow_crown".equals(id) || "void_lens".equals(id) || "void_abacus".equals(id) || "echo_prism".equals(id))) {
            return 2;
        }
        if (PROF_ARCANIST.equals(s.profession) && "singularity_orb".equals(id)) {
            return 4;
        }
        if (PROF_MERCHANT.equals(s.profession) && ("merchant_scale".equals(id) || "merchant_key".equals(id) || "cracked_compass".equals(id) || "tithe_box".equals(id) || "ledger_stamp".equals(id))) {
            return 2;
        }
        if (PROF_MERCHANT.equals(s.profession) && "kingmaker_seal".equals(id)) {
            return 4;
        }
        if (PROF_BLOODBOUND.equals(s.profession) && ("blood_contract".equals(id) || "silver_suture".equals(id) || "cup_of_mist".equals(id) || "scar_talisman".equals(id) || "crimson_seal".equals(id))) {
            return 2;
        }
        if (PROF_BLOODBOUND.equals(s.profession) && "blood_crown".equals(id)) {
            return 4;
        }
        if (PROF_WEAVER.equals(s.profession) && ("ink_fountain".equals(id) || "glass_anvil".equals(id) || "amber_quill".equals(id) || "loom_shuttle".equals(id) || "polished_cog".equals(id) || "pattern_spool".equals(id))) {
            return 2;
        }
        if (PROF_WEAVER.equals(s.profession) && "clockwork_loom".equals(id)) {
            return 4;
        }
        if (PROF_SUMMONER.equals(s.profession) && ("spirit_bell".equals(id) || "root_drum".equals(id) || "void_lens".equals(id) || "storm_shell".equals(id) || "loom_shuttle".equals(id))) {
            return 2;
        }
        if (PROF_SUMMONER.equals(s.profession) && "spirit_processional".equals(id)) {
            return 4;
        }
        if (PROF_HEXER.equals(s.profession) && ("hex_tablet".equals(id) || "scar_talisman".equals(id) || "hollow_crown".equals(id) || "arcane_ink".equals(id) || "void_abacus".equals(id))) {
            return 2;
        }
        if (PROF_HEXER.equals(s.profession) && "fallen_crown".equals(id)) {
            return 4;
        }
        if (PROF_INSCRIBER.equals(s.profession) && ("engraver_stylus".equals(id) || "mirror_anvil".equals(id)
                || "polished_cog".equals(id) || "curse_censer".equals(id) || "stormglass_seal".equals(id))) {
            return 2;
        }
        if (PROF_INSCRIBER.equals(s.profession) && "living_codex".equals(id)) {
            return 4;
        }
        if ((PROF_ALCHEMIST.equals(s.profession) || PROF_RANGER.equals(s.profession) || PROF_SUMMONER.equals(s.profession))
                && ("emberroot_charm".equals(id) || "stormglass_seal".equals(id))) {
            return 2;
        }
        if ((PROF_ARCANIST.equals(s.profession) || PROF_HEXER.equals(s.profession) || PROF_BLOODBOUND.equals(s.profession)
                || PROF_INSCRIBER.equals(s.profession))
                && ("curse_censer".equals(id) || "bloodcoin_broach".equals(id))) {
            return 2;
        }
        if ((PROF_WEAVER.equals(s.profession) || PROF_WARDEN.equals(s.profession) || PROF_DUELIST.equals(s.profession)
                || PROF_INSCRIBER.equals(s.profession))
                && "mirror_anvil".equals(id)) {
            return 2;
        }
        if ("rift_compass".equals(id)) {
            return 1;
        }
        if (ORIGIN_WILD.equals(s.origin) && "vital_sprout".equals(id)) {
            return 2;
        }
        if ((ORIGIN_ASH.equals(s.origin) || ORIGIN_WILD.equals(s.origin)) && "emberroot_charm".equals(id)) {
            return 2;
        }
        if ((ORIGIN_STEEL.equals(s.origin) || ORIGIN_VOID.equals(s.origin)) && "stormglass_seal".equals(id)) {
            return 2;
        }
        return 0;
    }

    public static int skillSpecRelicBonus(State s, String id) {
        if (s == null || id == null) {
            return 0;
        }
        SkillSpecDef spec = skillSpec(s.skillSpec);
        if (spec == null) {
            return 0;
        }
        if ("spec_burst".equals(spec.id) && "razor_pactstone".equals(id)) return 5;
        if ("spec_tempo".equals(spec.id) && "tempo_spindle".equals(id)) return 5;
        if ("spec_sustain".equals(spec.id) && "vigil_bloom".equals(id)) return 5;
        if ("spec_resonance".equals(spec.id) && "resonance_lens".equals(id)) return 5;
        if ("spec_mastery".equals(spec.id) && "mastery_badge".equals(id)) return 5;
        if ("spec_burst".equals(spec.id) && isAny(id, "tempo_metronome", "flash_heel", "hunter_mark")) return 1;
        if ("spec_tempo".equals(spec.id) && isAny(id, "amber_quill", "ink_fountain", "moon_lantern", "echo_crown")) return 1;
        if ("spec_sustain".equals(spec.id) && isAny(id, "bone_mask", "ruby_branch", "black_bread", "cup_of_mist")) return 1;
        if ("spec_resonance".equals(spec.id) && isAny(id, "cracked_compass", "rift_compass", "ability_crown")) return 1;
        if ("spec_mastery".equals(spec.id) && isSkillRelicForProfession(s, id)) return 2;
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
        } else if ("echo_crown".equals(id)) {
            s.maxHp = Math.max(30, s.maxHp - 6);
            s.hp = Math.min(s.hp, s.maxHp);
            Card c = new Card("echo_bait");
            c.upgraded = true;
            s.deck.add(c);
        } else if ("hex_moon".equals(id)) {
            addStatusCard(s, "daze");
            addStatusCard(s, "wound");
        } else if ("forge_heart".equals(id)) {
            upgradeRandomDeckCard(s);
            upgradeRandomDeckCard(s);
            upgradeRandomDeckCard(s);
        } else if ("golden_throne".equals(id)) {
            s.gold += 160;
            addStatusCard(s, "daze");
        } else if ("ability_crown".equals(id)) {
            CardDef c = randomTypeCard(s, 2, true);
            if (c != null) {
                Card add = new Card(c.id);
                add.upgraded = true;
                s.deck.add(add);
            }
            addStatusCard(s, "daze");
        } else if ("aegis_throne".equals(id)) {
            addUpgradedDeckCard(s, "warden_aegisline");
            s.maxHp += 4;
            s.hp += 4;
        } else if ("finale_rapier".equals(id)) {
            addUpgradedDeckCard(s, "duelist_bladesong");
            upgradeRandomDeckCard(s);
        } else if ("solar_crucible".equals(id)) {
            addUpgradedDeckCard(s, "alchemist_sunsteel");
            if (s.potions.size() < potionLimit(s)) {
                s.potions.add(POTION_LIBRARY.get(s.run.nextInt(POTION_LIBRARY.size())).id);
            }
        } else if ("apex_compass".equals(id)) {
            addUpgradedDeckCard(s, "ranger_predator");
            s.gold += 35;
        } else if ("singularity_orb".equals(id)) {
            addUpgradedDeckCard(s, "arcanist_eventhorizon");
            upgradeRandomDeckCard(s);
        } else if ("kingmaker_seal".equals(id)) {
            addUpgradedDeckCard(s, "merchant_kingmaker");
            s.gold += 90;
        } else if ("blood_crown".equals(id)) {
            addUpgradedDeckCard(s, "blood_apotheosis");
            addStatusCard(s, "wound");
            s.maxHp += 6;
            s.hp += 6;
        } else if ("clockwork_loom".equals(id)) {
            addUpgradedDeckCard(s, "weaver_clockwork");
            upgradeRandomDeckCard(s);
            upgradeRandomDeckCard(s);
        } else if ("spirit_processional".equals(id)) {
            addUpgradedDeckCard(s, "summoner_procession");
            s.maxHp += 4;
            s.hp += 4;
        } else if ("fallen_crown".equals(id)) {
            addUpgradedDeckCard(s, "hexer_crownfall");
            addStatusCard(s, "daze");
        } else if ("living_codex".equals(id)) {
            addUpgradedDeckCard(s, "inscriber_codex");
            upgradeRandomDeckCard(s);
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

    private static void upgradeDeckCard(State s, String id) {
        for (Card c : s.deck) {
            CardDef d = card(c.id);
            if (d != null && d.type != 3 && id.equals(c.id) && !c.upgraded) {
                c.upgraded = true;
                return;
            }
        }
        upgradeRandomDeckCard(s);
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

    private static int upgradedCardCount(State s) {
        int count = 0;
        for (Card c : s.deck) {
            CardDef d = card(c.id);
            if (d != null && d.type != 3 && c.upgraded) {
                count++;
            }
        }
        return count;
    }

    private static boolean hasRelic(State s, String id) {
        return s.relics.contains(id);
    }

    private static void addStatusCard(State s, String id) {
        Card card = new Card(id);
        if (s.mode == MODE_COMBAT) {
            s.discard.add(card);
        } else {
            s.deck.add(card);
            s.seenCards.add(id);
        }
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
        c = addCard("ember_snare", "烬缚", "通用", 0, 1, 0, 6, 9, 0, 0, "造成伤害，施加燃灼与束缚。", "更高伤害，更多燃灼与束缚。");
        c.burn = 1; c.burnUp = 2; c.bind = 1; c.bindUp = 2; c.targetEnemy = true;
        c = addCard("glass_sprint", "玻璃疾行", "通用", 1, 0, 1, 0, 0, 3, 5, "获得格挡，抽1张，消耗。", "更多格挡，抽2张，消耗。");
        c.draw = 1; c.drawUp = 2; c.exhaust = true;
        c = addCard("echo_bait", "回声诱饵", "通用", 1, 1, 1, 0, 0, 4, 7, "获得格挡，制造临时灵火，抽1张。", "更多格挡，制造临时灵火，抽1张。");
        c.createEcho = true; c.echoCardId = "summoner_sprite"; c.draw = c.drawUp = 1;
        c = addCard("forge_signal", "锻痕信标", "通用", 1, 1, 1, 0, 0, 8, 11, "获得格挡，升级手牌，职业技充能+1。", "更多格挡，职业技充能+2。");
        c.upgradeRandom = true; c.skillChargeGain = 1;
        c = addCard("cursed_coin", "咒币", "通用", 1, 0, 2, 0, 0, 0, 0, "获得金币，加入1张裂伤，抽1张，消耗。", "获得更多金币，抽2张，消耗。");
        c.goldGain = 8; c.createWound = true; c.draw = 1; c.drawUp = 2; c.exhaust = true;
        c = addCard("blood_suture", "血缝", "通用", 1, 1, 1, 0, 0, 7, 10, "失去生命，获得格挡、治疗并加入裂伤。", "失去更少生命，更多格挡与治疗。");
        c.hpLoss = 2; c.heal = 2; c.healUp = 3; c.createWound = true;
        c = addCard("void_tithe", "空契税", "通用", 2, 1, 2, 0, 0, 0, 0, "失去生命，获得金币、抽牌和能量，消耗。", "抽更多牌，获得更多金币。");
        c.hpLoss = 2; c.goldGain = 18; c.draw = 2; c.drawUp = 3; c.energyGain = 1; c.exhaust = true;
        c = addCard("prism_barrage", "棱镜齐射", "通用", 2, 2, 0, 6, 9, 0, 0, "对所有敌人造成伤害，施加燃灼、束缚与易伤。", "更高伤害与异常。");
        c.aoe = true; c.burn = 1; c.burnUp = 2; c.bind = 1; c.bindUp = 2; c.vulnerable = 1;
        c = addCard("overload_conduit", "权能导管", "通用", 2, 1, 2, 0, 0, 6, 9, "获得格挡，抽1张，职业技充能+3。", "更多格挡与抽牌，职业技充能+4。");
        c.draw = 1; c.drawUp = 2; c.skillChargeGain = 3;
        c = addCard("echo_matrix", "回声矩阵", "通用", 2, 1, 2, 0, 0, 4, 7, "制造临时疾切，回声势+1，消耗。", "更多格挡，回声势+2。");
        c.createEcho = true; c.echoCardId = "quick_cut"; c.gainVoidEngine = 1; c.exhaust = true;
        c = addCard("brew_crucible", "炼调坩埚", "通用", 2, 1, 2, 0, 0, 5, 8, "调制药剂，获得燃势与束缚势，消耗。", "更多格挡，调制药剂并获得更多势能。");
        c.createPotion = true; c.gainBurnPower = 1; c.gainBindPower = 1; c.exhaust = true;
        c = addCard("golden_engine", "裂币引擎", "通用", 2, 1, 1, 0, 0, 6, 9, "获得金币和格挡；金币越多格挡越高。", "更多金币与格挡。");
        c.goldGain = 14; c.goldBlock = true; c.draw = c.drawUp = 1;
        c = addCard("crimson_loop", "猩红回路", "通用", 2, 1, 0, 9, 13, 0, 0, "失去生命造成伤害，加入裂伤并治疗。", "更高伤害，损耗更低，治疗更多。");
        c.hpLoss = 2; c.heal = 3; c.healUp = 5; c.createWound = true;
        c = addCard("forge_blueprint", "工坊蓝图", "通用", 2, 1, 1, 0, 0, 8, 12, "获得格挡，升级手牌，检视牌库。", "更多格挡，升级手牌并更深检视。");
        c.upgradeRandom = true; c.scry = 3; c.draw = c.drawUp = 1;
        c = addCard("plague_vector", "疫变向量", "通用", 2, 1, 2, 0, 0, 0, 0, "施加燃灼、束缚与易伤，并扩散异常。", "更多异常并扩散。");
        c.burn = 2; c.burnUp = 4; c.bind = 2; c.bindUp = 4; c.vulnerable = 1; c.spreadStatus = true; c.targetEnemy = true;
        c = addCard("cycle_metronome", "循环节拍", "通用", 2, 0, 2, 0, 0, 0, 0, "抽2张，获得1能量，职业技充能+1，消耗。", "抽3张，获得1能量，职业技充能+2。");
        c.draw = 2; c.drawUp = 3; c.energyGain = 1; c.skillChargeGain = 1; c.exhaust = true;
        c = addCard("aegis_engine", "圣盾引擎", "通用", 2, 1, 1, 0, 0, 11, 16, "获得格挡，守势+1。", "更多格挡，守势+2。");
        c.gainSteelEngine = 1;

        c = addCard("warden_oath", "坚守誓言", "通用", 0, 1, 1, 0, 0, 10, 14, "获得格挡。守卫：推动护卫计数。", "更多格挡。");
        c.profession = PROF_WARDEN;
        c = addCard("warden_slam", "壁垒重击", "通用", 1, 2, 0, 10, 14, 8, 12, "造成伤害并格挡。若已有格挡，伤害更高。", "更高伤害与格挡。");
        c.profession = PROF_WARDEN; c.blockToDamage = true;
        c = addCard("warden_stand", "不退阵", "通用", 2, 2, 1, 0, 0, 16, 22, "获得大量格挡，抽1张，守势+1。", "更多格挡，守势+2。");
        c.profession = PROF_WARDEN; c.draw = c.drawUp = 1; c.gainSteelEngine = 1;
        c = addCard("warden_command", "盾阵号令", "通用", 2, 1, 1, 0, 0, 9, 13, "获得格挡，抽1张，职业技充能+3。", "更多格挡，职业技充能+4。");
        c.profession = PROF_WARDEN; c.skillChargeGain = 3; c.draw = c.drawUp = 1;
        c = addCard("warden_overguard", "蓄势盾环", "通用", 1, 1, 1, 0, 0, 8, 12, "获得格挡，守势+1，职业技充能+2。", "更多格挡，职业技充能+3。");
        c.profession = PROF_WARDEN; c.gainSteelEngine = 1; c.skillChargeGain = 2;
        c = addCard("warden_aegisline", "圣盾战线", "通用", 2, 2, 0, 10, 14, 16, 22, "造成伤害并获得格挡；若已有高格挡，追加爆发。守势+1。", "更高伤害与格挡，守势+2。");
        c.profession = PROF_WARDEN; c.blockToDamage = true; c.gainSteelEngine = 1;

        c = addCard("duelist_flurry", "连步刺", "通用", 0, 0, 0, 3, 5, 0, 0, "造成伤害，本回合打牌越多越强。", "更高伤害与连击成长。");
        c.profession = PROF_DUELIST; c.comboDamage = 2;
        c = addCard("duelist_feint", "佯攻换位", "通用", 1, 0, 2, 0, 0, 3, 5, "获得格挡，抽2张，消耗。", "更多格挡，抽3张，消耗。");
        c.profession = PROF_DUELIST; c.draw = 2; c.drawUp = 3; c.exhaust = true;
        c = addCard("duelist_finish", "终式", "通用", 2, 1, 0, 8, 12, 0, 0, "造成伤害，本回合打牌越多越强。", "更高伤害与连击成长。");
        c.profession = PROF_DUELIST; c.comboDamage = 4;
        c = addCard("duelist_flashstep", "闪步终拍", "通用", 2, 0, 0, 2, 4, 0, 0, "造成低伤害，抽1张，职业技充能+2。", "更高伤害，职业技充能+3。");
        c.profession = PROF_DUELIST; c.draw = c.drawUp = 1; c.comboDamage = 2; c.skillChargeGain = 2;
        c = addCard("duelist_overtempo", "过载闪击", "通用", 1, 0, 0, 2, 4, 0, 0, "造成伤害，本回合打牌越多越强，职业技充能+1。", "更高伤害，职业技充能+2。");
        c.profession = PROF_DUELIST; c.draw = c.drawUp = 1; c.comboDamage = 2; c.skillChargeGain = 1;
        c = addCard("duelist_bladesong", "万刃终谱", "通用", 2, 1, 0, 7, 10, 0, 0, "造成伤害，本回合打牌越多越强；连打足够时抽牌。", "更高伤害和连击成长。");
        c.profession = PROF_DUELIST; c.comboDamage = 5; c.draw = c.drawUp = 1; c.skillChargeGain = 1;

        c = addCard("alchemist_mix", "试剂调和", "通用", 0, 1, 2, 0, 0, 4, 6, "获得格挡并调制随机药剂，消耗。", "更多格挡并调制随机药剂，消耗。");
        c.profession = PROF_ALCHEMIST; c.createPotion = true; c.exhaust = true;
        c = addCard("alchemist_cloud", "腐蚀云", "通用", 1, 1, 2, 0, 0, 0, 0, "目标获得燃灼与束缚，并扩散异常。", "更多异常扩散。");
        c.profession = PROF_ALCHEMIST; c.burn = 3; c.burnUp = 5; c.bind = 2; c.bindUp = 3; c.spreadStatus = true; c.targetEnemy = true;
        c = addCard("alchemist_catalyst", "催化剂", "通用", 2, 1, 2, 0, 0, 0, 0, "抽2张，获得燃势与束缚势，消耗。", "抽3张，获得更多势能，消耗。");
        c.profession = PROF_ALCHEMIST; c.draw = 2; c.drawUp = 3; c.gainBurnPower = 1; c.gainBindPower = 1; c.exhaust = true;
        c = addCard("alchemist_reactor", "连锁反应釜", "通用", 2, 1, 2, 0, 0, 5, 8, "获得格挡并调制药剂，职业技充能+2。", "更多格挡，职业技充能+3。");
        c.profession = PROF_ALCHEMIST; c.createPotion = true; c.exhaust = true; c.skillChargeGain = 2;
        c = addCard("alchemist_overbrew", "过载滴管", "通用", 1, 1, 2, 0, 0, 4, 7, "获得格挡，调制药剂，职业技充能+2，消耗。", "更多格挡，职业技充能+3。");
        c.profession = PROF_ALCHEMIST; c.createPotion = true; c.exhaust = true; c.skillChargeGain = 2;
        c = addCard("alchemist_sunsteel", "日钢终釜", "通用", 2, 2, 2, 0, 0, 9, 13, "获得格挡，施加燃灼与束缚并调制药剂；势能会提高格挡。", "更多格挡、异常和制药稳定性。");
        c.profession = PROF_ALCHEMIST; c.burn = 3; c.burnUp = 5; c.bind = 3; c.bindUp = 5; c.createPotion = true; c.targetEnemy = true; c.skillChargeGain = 2;

        c = addCard("ranger_trap", "踏影陷阱", "通用", 0, 1, 1, 0, 0, 6, 9, "获得格挡，施加束缚。", "更多格挡与束缚。");
        c.profession = PROF_RANGER; c.bind = 2; c.bindUp = 3; c.targetEnemy = true;
        c = addCard("ranger_volley", "猎线齐射", "通用", 1, 1, 0, 5, 8, 0, 0, "对所有敌人造成伤害并施加束缚。", "更高伤害与束缚。");
        c.profession = PROF_RANGER; c.aoe = true; c.bind = 1; c.bindUp = 2;
        c = addCard("ranger_patience", "潜伏耐心", "通用", 2, 1, 2, 0, 0, 0, 0, "束缚总量越高抽牌越多，获得束缚势。", "抽牌上限更高，获得更多束缚势。");
        c.profession = PROF_RANGER; c.bindToDraw = true; c.gainBindPower = 1;
        c = addCard("ranger_killzone", "猎场封锁", "通用", 2, 1, 1, 0, 0, 8, 11, "获得格挡，对目标施加束缚和易伤，职业技充能+2。", "更多格挡、束缚和充能。");
        c.profession = PROF_RANGER; c.bind = 3; c.bindUp = 5; c.vulnerable = 1; c.skillChargeGain = 2; c.targetEnemy = true;
        c = addCard("ranger_overmark", "过载猎标", "通用", 1, 1, 0, 7, 10, 0, 0, "造成伤害，施加束缚，职业技充能+2。", "更高伤害、更多束缚，职业技充能+3。");
        c.profession = PROF_RANGER; c.bind = 2; c.bindUp = 4; c.targetEnemy = true; c.skillChargeGain = 2;
        c = addCard("ranger_predator", "猎王收束", "通用", 2, 2, 0, 12, 17, 0, 0, "造成伤害，目标束缚越高伤害越高；施加束缚与易伤。", "更高伤害，并更善于收束猎物。");
        c.profession = PROF_RANGER; c.bind = 4; c.bindUp = 6; c.vulnerable = 1; c.targetEnemy = true; c.skillChargeGain = 2;

        c = addCard("arcanist_glyph", "秘文摹写", "通用", 0, 0, 2, 0, 0, 0, 0, "制造一张临时疾切，消耗。", "制造一张临时疾切，消耗。");
        c.profession = PROF_ARCANIST; c.createEcho = true; c.echoCardId = "quick_cut"; c.exhaust = true;
        c = addCard("arcanist_rift", "裂门术", "通用", 1, 1, 1, 0, 0, 7, 10, "获得格挡，消耗弃牌堆顶牌，抽2张。", "更多格挡与抽牌。");
        c.profession = PROF_ARCANIST; c.exhaustTopDiscard = true; c.draw = 2; c.drawUp = 3;
        c = addCard("arcanist_loop", "秘环回流", "通用", 2, 1, 2, 0, 0, 0, 0, "回声势+1，抽2张，获得1能量，消耗。", "回声势+2，抽3张，获得1能量，消耗。");
        c.profession = PROF_ARCANIST; c.gainVoidEngine = 1; c.draw = 2; c.drawUp = 3; c.energyGain = 1; c.exhaust = true;
        c = addCard("arcanist_convergence", "秘能汇流", "通用", 2, 1, 2, 0, 0, 0, 0, "抽1张，制造临时疾切，职业技充能+3，消耗。", "抽2张，职业技充能+4。");
        c.profession = PROF_ARCANIST; c.draw = 1; c.drawUp = 2; c.createEcho = true; c.echoCardId = "quick_cut"; c.skillChargeGain = 3; c.exhaust = true;
        c = addCard("arcanist_overflow", "过载秘钥", "通用", 1, 1, 2, 0, 0, 0, 0, "抽1张，制造临时疾切，职业技充能+2，消耗。", "抽2张，职业技充能+3。");
        c.profession = PROF_ARCANIST; c.draw = 1; c.drawUp = 2; c.createEcho = true; c.echoCardId = "quick_cut"; c.skillChargeGain = 2; c.exhaust = true;
        c = addCard("arcanist_eventhorizon", "事件视界", "通用", 2, 1, 2, 0, 0, 5, 8, "获得格挡，抽牌，制造临时秘文并提高回声势，消耗。", "更多格挡、抽牌和回声势。");
        c.profession = PROF_ARCANIST; c.draw = 2; c.drawUp = 3; c.exhaust = true; c.skillChargeGain = 2; c.gainVoidEngine = 1;

        c = addCard("merchant_haggle", "讨价还价", "通用", 0, 0, 2, 0, 0, 0, 0, "获得金币，抽1张，消耗。", "获得更多金币，抽1张，消耗。");
        c.profession = PROF_MERCHANT; c.goldGain = 12; c.draw = c.drawUp = 1; c.exhaust = true;
        c = addCard("merchant_invest", "风险投资", "通用", 1, 1, 1, 0, 0, 5, 8, "获得格挡和金币，金币越多格挡越高。", "更多格挡与金币。");
        c.profession = PROF_MERCHANT; c.goldGain = 8; c.goldBlock = true;
        c = addCard("merchant_liquidate", "清算", "通用", 2, 2, 0, 10, 15, 0, 0, "造成伤害，金币越多伤害越高。获得金币。", "更高伤害与金币收益。");
        c.profession = PROF_MERCHANT; c.goldDamage = true; c.goldGain = 10;
        c = addCard("merchant_speculate", "期货契据", "通用", 2, 1, 2, 0, 0, 4, 7, "获得格挡和金币，职业技充能+2。", "更多格挡和金币，职业技充能+3。");
        c.profession = PROF_MERCHANT; c.goldGain = 18; c.goldBlock = true; c.draw = c.drawUp = 1; c.skillChargeGain = 2;
        c = addCard("merchant_overdraft", "过载账单", "通用", 1, 1, 2, 0, 0, 4, 7, "获得金币和格挡，职业技充能+2。", "更多金币与格挡，职业技充能+3。");
        c.profession = PROF_MERCHANT; c.goldGain = 12; c.goldBlock = true; c.draw = c.drawUp = 1; c.skillChargeGain = 2;
        c = addCard("merchant_kingmaker", "金座点王", "通用", 2, 2, 0, 12, 17, 8, 12, "造成伤害并获得格挡；金币越多伤害和格挡越高，获得金币。", "更高基础收益与金币转化。");
        c.profession = PROF_MERCHANT; c.goldDamage = true; c.goldBlock = true; c.goldGain = 18; c.skillChargeGain = 2;

        c = addCard("blood_pact", "血契刻印", "通用", 0, 1, 0, 9, 13, 0, 0, "失去生命造成伤害。血契者会把失去生命转为额外爆发。", "失去更少生命，造成更高伤害。");
        c.profession = PROF_BLOODBOUND; c.hpLoss = 2; c.vulnerable = 1;
        c = addCard("blood_rite", "裂伤仪式", "通用", 1, 1, 2, 0, 0, 6, 9, "抽牌并加入裂伤；裂伤可推动血契者反击。", "更多格挡与抽牌。");
        c.profession = PROF_BLOODBOUND; c.draw = 2; c.drawUp = 3; c.createWound = true; c.exhaust = true;
        c = addCard("blood_feast", "赤宴", "通用", 2, 2, 0, 12, 18, 0, 0, "造成伤害并治疗，低血线构筑收益更高。", "更高伤害和治疗。");
        c.profession = PROF_BLOODBOUND; c.heal = 4; c.healUp = 7;
        c = addCard("blood_sigilstorm", "血印风暴", "通用", 2, 1, 0, 8, 12, 0, 0, "失去生命造成伤害，职业技充能+3并加入裂伤。", "更高伤害，职业技充能+4。");
        c.profession = PROF_BLOODBOUND; c.hpLoss = 2; c.createWound = true; c.skillChargeGain = 3;
        c = addCard("blood_overflow", "过载血契", "通用", 1, 1, 0, 8, 12, 0, 0, "失去生命造成伤害，职业技充能+2并加入裂伤。", "更高伤害，职业技充能+3。");
        c.profession = PROF_BLOODBOUND; c.hpLoss = 2; c.createWound = true; c.skillChargeGain = 2;
        c = addCard("blood_apotheosis", "血冕化生", "通用", 2, 2, 0, 14, 20, 0, 0, "失去生命造成伤害并治疗；失去生命越多，伤害越高，加入裂伤。", "更高伤害与治疗，损耗更低。");
        c.profession = PROF_BLOODBOUND; c.hpLoss = 4; c.heal = 5; c.healUp = 8; c.createWound = true; c.skillChargeGain = 2;

        c = addCard("weaver_thread", "织线", "通用", 0, 0, 1, 0, 0, 4, 6, "获得格挡，检视牌库并抽1张。织牌师偏好牌序工程。", "更多格挡，抽2张。");
        c.profession = PROF_WEAVER; c.scry = 3; c.draw = 1; c.drawUp = 2;
        c = addCard("weaver_lattice", "格纹阵", "通用", 1, 1, 1, 0, 0, 8, 12, "获得格挡，升级手中一张牌并抽牌。", "更多格挡与抽牌。");
        c.profession = PROF_WEAVER; c.upgradeRandom = true; c.draw = c.drawUp = 1;
        c = addCard("weaver_pattern", "定式改写", "通用", 2, 1, 2, 0, 0, 0, 0, "制造临时疾切，检视牌库，获得能量，消耗。", "额外抽牌并获得能量。");
        c.profession = PROF_WEAVER; c.createEcho = true; c.echoCardId = "quick_cut"; c.scry = 4; c.energyGain = 1; c.draw = 1; c.drawUp = 2; c.exhaust = true;
        c = addCard("weaver_overpattern", "超定式", "通用", 2, 1, 1, 0, 0, 7, 10, "获得格挡，升级手牌，职业技充能+2。", "更多格挡，职业技充能+3。");
        c.profession = PROF_WEAVER; c.upgradeRandom = true; c.draw = c.drawUp = 1; c.skillChargeGain = 2;
        c = addCard("weaver_overthread", "过载织线", "通用", 1, 1, 1, 0, 0, 6, 9, "获得格挡，升级手牌，职业技充能+2。", "更多格挡，职业技充能+3。");
        c.profession = PROF_WEAVER; c.upgradeRandom = true; c.scry = 2; c.skillChargeGain = 2;
        c = addCard("weaver_clockwork", "万象织钟", "通用", 2, 2, 1, 0, 0, 12, 16, "获得格挡，抽牌并升级手牌；升级牌越多格挡越高。", "更多格挡和抽牌，升级牌收益更高。");
        c.profession = PROF_WEAVER; c.draw = 2; c.drawUp = 3; c.upgradeRandom = true; c.scry = 3; c.skillChargeGain = 2;

        c = addCard("summoner_sprite", "灵火", "通用", 0, 0, 0, 4, 6, 3, 5, "造成伤害并获得格挡。唤灵师：推动灵潮。", "更高伤害与格挡。");
        c.profession = PROF_SUMMONER; c.bind = 1; c.targetEnemy = true;
        c = addCard("summoner_wisp", "游魂引", "通用", 1, 1, 2, 0, 0, 5, 8, "制造临时灵火，抽1张。", "更多格挡，抽2张。");
        c.profession = PROF_SUMMONER; c.createEcho = true; c.echoCardId = "summoner_sprite"; c.draw = 1; c.drawUp = 2;
        c = addCard("summoner_guardian", "守灵", "通用", 1, 1, 1, 0, 0, 10, 14, "获得格挡，施加束缚，制造临时灵火。", "更多格挡与束缚。");
        c.profession = PROF_SUMMONER; c.bind = 2; c.bindUp = 3; c.targetEnemy = true; c.createEcho = true; c.echoCardId = "summoner_sprite";
        c = addCard("summoner_court", "灵庭", "通用", 2, 1, 2, 0, 0, 4, 7, "制造临时灵火，职业技充能+3，获得束缚势。", "更多格挡，职业技充能+4。");
        c.profession = PROF_SUMMONER; c.createEcho = true; c.echoCardId = "summoner_sprite"; c.skillChargeGain = 3; c.gainBindPower = 1; c.draw = c.drawUp = 1;
        c = addCard("summoner_overcall", "过载唤潮", "通用", 1, 1, 2, 0, 0, 4, 7, "制造临时灵火，抽1张，职业技充能+2。", "更多格挡，职业技充能+3。");
        c.profession = PROF_SUMMONER; c.createEcho = true; c.echoCardId = "summoner_sprite"; c.draw = c.drawUp = 1; c.skillChargeGain = 2;
        c = addCard("summoner_procession", "百灵巡游", "通用", 2, 2, 2, 0, 0, 10, 14, "获得格挡，召唤多张临时灵火，施加束缚并推动灵潮。", "更多格挡、灵火与束缚。");
        c.profession = PROF_SUMMONER; c.createEcho = true; c.echoCardId = "summoner_sprite"; c.bind = 3; c.bindUp = 5; c.targetEnemy = true; c.skillChargeGain = 3;

        c = addCard("hexer_hexmark", "咒印", "通用", 0, 1, 0, 6, 9, 0, 0, "造成伤害，施加易伤与束缚。", "更高伤害，施加更多控制。");
        c.profession = PROF_HEXER; c.vulnerable = 1; c.bind = 1; c.bindUp = 2; c.targetEnemy = true;
        c = addCard("hexer_pact", "暗契", "通用", 1, 0, 2, 0, 0, 0, 0, "加入1张裂伤，抽2张并获得1能量。", "抽3张并获得1能量。");
        c.profession = PROF_HEXER; c.draw = 2; c.drawUp = 3; c.energyGain = 1; c.createWound = true; c.exhaust = true;
        c = addCard("hexer_maledict", "恶咒", "通用", 2, 1, 2, 0, 0, 0, 0, "目标获得易伤与束缚，扩散异常，职业技充能+2。", "更多控制，职业技充能+3。");
        c.profession = PROF_HEXER; c.vulnerable = 1; c.bind = 2; c.bindUp = 4; c.spreadStatus = true; c.targetEnemy = true; c.skillChargeGain = 2;
        c = addCard("hexer_overcurse", "过载咒环", "通用", 1, 1, 2, 0, 0, 0, 0, "施加易伤与束缚，加入裂伤，职业技充能+2。", "更多控制，职业技充能+3。");
        c.profession = PROF_HEXER; c.vulnerable = 1; c.bind = 2; c.bindUp = 3; c.createWound = true; c.targetEnemy = true; c.skillChargeGain = 2;
        c = addCard("hexer_purge", "以咒净咒", "通用", 2, 1, 2, 0, 0, 6, 9, "消耗弃牌堆顶牌，抽2张。状态牌会推动咒术师。", "更多格挡与抽牌。");
        c.profession = PROF_HEXER; c.exhaustTopDiscard = true; c.draw = 2; c.drawUp = 3;
        c = addCard("hexer_crownfall", "咒冠坠落", "通用", 2, 1, 2, 0, 0, 6, 9, "敌群获得易伤与束缚，扩散异常并加入状态牌。", "更多格挡，加入裂伤并加强异常扩散。");
        c.profession = PROF_HEXER; c.vulnerable = 1; c.bind = 2; c.bindUp = 4; c.spreadStatus = true; c.targetEnemy = true; c.skillChargeGain = 2;

        c = addCard("inscriber_mark", "裂纹刻印", "通用", 0, 1, 0, 7, 10, 0, 0, "造成伤害，施加易伤，职业技充能+1。", "更高伤害与易伤，职业技充能+2。");
        c.profession = PROF_INSCRIBER; c.vulnerable = 1; c.skillChargeGain = 1; c.targetEnemy = true;
        c = addCard("inscriber_tablet", "刻板抄录", "通用", 0, 1, 1, 0, 0, 6, 9, "获得格挡，升级手牌并抽1张。", "更多格挡，升级手牌并抽2张。");
        c.profession = PROF_INSCRIBER; c.upgradeRandom = true; c.draw = 1; c.drawUp = 2;
        c = addCard("inscriber_glyphstorm", "符雨", "通用", 1, 1, 2, 0, 0, 0, 0, "敌群获得束缚和易伤，加入1张眩光，职业技充能+2。", "更多控制，职业技充能+3。");
        c.profession = PROF_INSCRIBER; c.aoe = true; c.bind = 1; c.bindUp = 2; c.vulnerable = 1; c.skillChargeGain = 2; c.addStatusToEnemy = true;
        c = addCard("inscriber_overseal", "过载印封", "通用", 1, 1, 1, 0, 0, 7, 10, "获得格挡，升级手牌，职业技充能+2，加入裂伤。", "更多格挡，职业技充能+3。");
        c.profession = PROF_INSCRIBER; c.upgradeRandom = true; c.skillChargeGain = 2; c.createWound = true;
        c = addCard("inscriber_palimp", "重写羊皮卷", "通用", 2, 1, 2, 0, 0, 0, 0, "抽2张，消耗弃牌堆顶牌；若消耗状态牌，职业技充能+1。", "抽3张；若消耗状态牌，再抽1张并充能。");
        c.profession = PROF_INSCRIBER; c.draw = 2; c.drawUp = 3; c.exhaustTopDiscard = true; c.energyGain = 1;
        c = addCard("inscriber_codex", "无名刻典", "通用", 2, 2, 2, 0, 0, 9, 13, "获得格挡，升级手牌，敌群易伤束缚，职业技充能+3。", "更多格挡与控制，职业技充能+4。");
        c.profession = PROF_INSCRIBER; c.upgradeRandom = true; c.vulnerable = 1; c.bind = 2; c.bindUp = 3; c.skillChargeGain = 3; c.targetEnemy = true;

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
        addRelicDef("emberroot_charm", "烬根符", "燃灼牌追加束缚，束缚牌追加燃灼，连接火焰与控制构筑。");
        addRelicDef("stormglass_seal", "岚玻印", "抽牌并格挡的牌追加格挡和职业技充能；带异常的攻击追加格挡。");
        addRelicDef("curse_censer", "咒香炉", "消耗、临时、状态与造伤牌会施加易伤并触发穿透伤害。");
        addRelicDef("bloodcoin_broach", "血金币针", "自损和裂伤牌提供金币与格挡；金币牌少量治疗并给职业技充能。");
        addRelicDef("mirror_anvil", "镜砧", "升级牌按类型追加收益；升级手牌的牌也会推动职业技。");
        addRelicDef("rift_compass", "裂隙罗盘", "打出非本派系或其他职业牌时获得资源，鼓励跨池混搭。");
        addRelicDef("razor_pactstone", "裂锋誓石", "裂锋专修释放职业技时追加穿透斩击；击杀后抽牌。");
        addRelicDef("tempo_spindle", "疾调纺锤", "疾调专修释放职业技时抽牌并获得能量；高阶时额外制造疾切。");
        addRelicDef("vigil_bloom", "续战夜花", "续战专修释放职业技时获得格挡和治疗，低血线翻倍部分收益。");
        addRelicDef("resonance_lens", "共鸣透镜", "共鸣专修释放职业技时提高职业技充能，并复制当前构筑共鸣收益。");
        addRelicDef("mastery_badge", "本职徽章", "本职专修释放职业技时追加职业牌、升级或资源，并推动充能。");
        addRelicDef("command_banner", "号令战旗", "守卫职业技更快充能；释放后追加格挡与穿透反击。");
        addRelicDef("flash_heel", "闪击鞋钉", "决斗者连打后职业技更快充能；释放后抽牌，连打足够时获得能量。");
        addRelicDef("catalyst_pump", "催化泵", "炼金师持有药剂时职业技更快充能；释放后追加异常。");
        addRelicDef("hawk_fletching", "鹰羽箭尾", "游侠对束缚敌人更快充能；释放职业技后追加束缚穿透伤害。");
        addRelicDef("echo_prism", "回声棱镜", "秘术师拥有回声势时职业技更快充能；释放后获得临时窥见和能量。");
        addRelicDef("ledger_stamp", "账本印章", "行商富裕时职业技更快充能；释放后返还金币并获得格挡。");
        addRelicDef("crimson_seal", "猩红封蜡", "血契者低血线时职业技更快充能；释放后治疗、施加易伤并保留少量充能。");
        addRelicDef("pattern_spool", "定式线轴", "织牌师手牌充足时职业技更快充能；释放后升级手牌、抽牌并保留少量充能。");
        addRelicDef("spirit_bell", "灵铃", "唤灵师手牌充足时职业技更快充能；释放后制造临时灵火并获得格挡。");
        addRelicDef("hex_tablet", "咒碑", "咒术师面对易伤敌人时职业技更快充能；释放后追加控制与穿透伤害。");
        addRelicDef("engraver_stylus", "活刻笔", "刻印师升级牌较多时职业技更快充能；释放后升级手牌、抽牌并追加刻痕控制。");
        addRelicDef("aegis_throne", "圣盾王座", "获得升级圣盾战线；高格挡技能追加格挡、充能与穿透反击。");
        addRelicDef("finale_rapier", "终曲细剑", "获得升级万刃终谱；连打后攻击追加穿透伤害，第5张牌抽牌。");
        addRelicDef("solar_crucible", "日钢坩埚", "获得升级日钢终釜；制药和异常牌追加燃灼、束缚与格挡。");
        addRelicDef("apex_compass", "顶点罗盘", "获得升级猎王收束；束缚牌会加深标记并对重度束缚敌人追击。");
        addRelicDef("singularity_orb", "奇点珠", "获得升级事件视界；消耗、临时和回声牌获得格挡，并循环临时秘文。");
        addRelicDef("kingmaker_seal", "点王印", "获得升级金座点王；金币牌追加金币、格挡，富裕时推动职业技。");
        addRelicDef("blood_crown", "血冕", "获得升级血冕化生；自损和裂伤提供格挡，低血线时少量治疗。");
        addRelicDef("clockwork_loom", "万象织机", "获得升级万象织钟；升级、检视和锻造牌提供格挡，并定期升级手牌。");
        addRelicDef("spirit_processional", "百灵仪仗", "获得升级百灵巡游；召唤和临时牌提供格挡、束缚并续接灵火。");
        addRelicDef("fallen_crown", "坠落咒冠", "获得升级咒冠坠落；异常与状态牌扩散易伤，状态牌额外格挡。");
        addRelicDef("living_codex", "活页刻典", "获得升级无名刻典；升级、刻印和状态牌扩散易伤束缚，并定期升级手牌。");
        addBossRelicDef("obsidian_core", "黑曜核心", "每回合能量+1。获得时最大生命-10。");
        addBossRelicDef("runic_shackle", "符文镣铐", "卡牌奖励+1，立即获得120金币；每回合少抽1张。");
        addBossRelicDef("blood_contract", "血契杯", "最大生命+18；每场战斗首回合失去2生命并抽2张。");
        addBossRelicDef("void_anchor", "虚空锚", "每场战斗首回合获得一张临时无光新星。");
        addBossRelicDef("mirror_sun", "镜日", "每场战斗首回合能量+2；获得时加入2张眩光。");
        addBossRelicDef("echo_crown", "回声王冠", "最大生命降低；获得升级回声诱饵。临时/召唤牌推动职业技并稳定抽牌。");
        addBossRelicDef("hex_moon", "咒月", "获得时加入眩光与裂伤；每回合施加异常，异常牌追加易伤。");
        addBossRelicDef("forge_heart", "锻炉心", "获得时升级3张牌；首回合能量-1，但升级/检视牌提供格挡。");
        addBossRelicDef("golden_throne", "裂币王座", "获得大量金币并加入眩光；商店入账，富裕时战后多一张卡牌奖励。");
        addBossRelicDef("ability_crown", "权能冠冕", "获得升级能力牌并加入眩光；首回合获得临时能力牌，能力牌推动职业技。");
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
        addBoon("profession_pack", "职业秘匣", "获得一张升级职业牌和少量金币。");
        addBoon("skill_seed", "技能源印", "获得一张职业牌和对应职业技遗物。");
        addBoon("route_cache", "路网密函", "获得夜航图和35金币，路线选择更自由。");
        addBoon("thin_start", "轻装开局", "移除2张基础牌，最大生命-4。");
        addBoon("brew_start", "随身炼台", "装满药剂，并加入试剂调和。");
        addBoon("rare_relic_risk", "双遗物赌注", "获得2个普通遗物，并加入1张眩光。");
        addBoon("forge_start", "开局锻炉", "随机升级3张牌，失去30金币。");
        addBoon("blood_start", "血色预付款", "最大生命+8，当前生命-8，获得升级血契刻印。");
    }

    private static void addBoon(String id, String name, String text) {
        BoonDef b = new BoonDef();
        b.id = id;
        b.name = name;
        b.text = text;
        BOON_LIBRARY.add(b);
    }

    private static void seedPacts() {
        addPact("pact_guardian", "护卫誓约", "格挡峰值足够时兑现金币和治疗；3次圆满获得圣盾引擎与生命。");
        addPact("pact_sprinter", "疾行誓约", "快速结束战斗或单回合连打6张兑现升级；3次圆满获得循环节拍。");
        addPact("pact_brewer", "炼调誓约", "用药或积累燃势/束缚势兑现补给；3次圆满获得炼调坩埚并装满药剂。");
        addPact("pact_hunter", "猎杀誓约", "击败精英/Boss或多目标战兑现额外金币；3次圆满获得疫变向量和猎痕。");
        addPact("pact_void", "回声誓约", "消耗/临时牌足够多时兑现升级；3次圆满获得回声矩阵和空冠。");
        addPact("pact_blood", "血誓约", "自损或低血线胜利兑现金币、治疗和生命；3次圆满获得猩红回路。");
        addPact("pact_summon", "唤灵誓约", "临时牌或召唤足够多时强化牌组；3次圆满获得回声矩阵和游魂引。");
        addPact("pact_hex", "咒环誓约", "状态/易伤路线达标兑现净化和咒币；3次圆满获得疫变向量并净化。");
        addPact("pact_forge", "工坊誓约", "升级/检视路线达标持续锻造牌组；3次圆满获得工坊蓝图和额外升级。");
        addPact("pact_merchant", "裂币誓约", "金币牌或富裕达标兑现金币和治疗；3次圆满获得裂币引擎和大量金币。");
    }

    private static void addPact(String id, String name, String text) {
        PactDef p = new PactDef();
        p.id = id;
        p.name = name;
        p.text = text;
        PACT_LIBRARY.add(p);
    }

    private static void seedSkillSpecs() {
        addSkillSpec("spec_burst", "裂锋专修", "职业技造成额外穿透伤害；若击杀敌人，返还能量并少量充能。");
        addSkillSpec("spec_tempo", "疾调专修", "职业技抽牌并制造临时疾切；释放后保留少量节奏充能。");
        addSkillSpec("spec_sustain", "续战专修", "职业技获得格挡并治疗；低血线时治疗与格挡更强。");
        addSkillSpec("spec_resonance", "共鸣专修", "职业技强化当前构筑共鸣，释放时额外获得充能并提高共鸣收益。");
        addSkillSpec("spec_mastery", "本职专修", "职业技追加一段职业身份效果，并获得对应职业过载牌。");
    }

    private static void addSkillSpec(String id, String name, String text) {
        SkillSpecDef spec = new SkillSpecDef();
        spec.id = id;
        spec.name = name;
        spec.text = text;
        SKILL_SPEC_LIBRARY.add(spec);
    }

    private static void seedTalents() {
        addTalent("t_shared_masterwork", "", "匠心牌组", "获得时升级2张牌；之后每幕首场战斗首回合抽1张。");
        addTalent("t_shared_hunter", "", "猎取路线", "精英和Boss额外金币；卡牌奖励更偏向当前职业。");
        addTalent("t_shared_longnight", "", "长夜储备", "每场战斗第4回合获得1能量并抽1张。");
        addTalent("t_shared_wayfarer", "", "旅人本能", "事件前整理资源；普通战斗偶尔多一张卡牌奖励。");
        addTalent("t_shared_apothecary", "", "随行药师", "获得时装满药剂；药剂与制药牌提供额外资源。");
        addTalent("t_warden_bastion", PROF_WARDEN, "不破壁垒", "每场战斗首回合获得守势与额外格挡。");
        addTalent("t_warden_counter", PROF_WARDEN, "反击纪律", "每两张技能牌的反击更强，并额外抽1张。");
        addTalent("t_warden_armory", PROF_WARDEN, "移动军械", "首回合获得守势并升级手牌；高格挡技能追加穿透伤害。");
        addTalent("t_duelist_tempo", PROF_DUELIST, "快节奏", "每回合前两张0费牌获得额外伤害。");
        addTalent("t_duelist_execution", PROF_DUELIST, "处决窗口", "连击追击对易伤或低生命敌人更强。");
        addTalent("t_duelist_gambit", PROF_DUELIST, "第四步赌招", "每回合第4张及之后的牌追加伤害，攻击牌额外抽牌。");
        addTalent("t_alchemist_reserve", PROF_ALCHEMIST, "备用试剂", "战斗开始若药剂未满，补充一瓶随机药剂。");
        addTalent("t_alchemist_plague", PROF_ALCHEMIST, "瘟疫催化", "药剂和异常扩散会额外附加燃灼与束缚。");
        addTalent("t_alchemist_distiller", PROF_ALCHEMIST, "现场蒸馏", "制药牌提供格挡和抽牌，并有机会追加药剂；用药额外施加异常。");
        addTalent("t_ranger_quarry", PROF_RANGER, "标记猎物", "首个敌人开局获得易伤与束缚，击杀后转移标记。");
        addTalent("t_ranger_net", PROF_RANGER, "复合陷网", "束缚牌同时提供格挡。");
        addTalent("t_ranger_wildpath", PROF_RANGER, "野径追猎", "每回合给首个敌人追加束缚，获得升级潜伏耐心。");
        addTalent("t_arcanist_rewrite", PROF_ARCANIST, "重写命运", "每场战斗首回合制造临时秘文摹写。");
        addTalent("t_arcanist_overflow", PROF_ARCANIST, "回声溢出", "触发秘术职业回流时额外制造临时疾切。");
        addTalent("t_arcanist_archive", PROF_ARCANIST, "秘库索引", "首回合回声势+1并抽牌，获得升级悖论。");
        addTalent("t_merchant_interest", PROF_MERCHANT, "复利账本", "进入商店获得金币；金币牌奖励更多金币。");
        addTalent("t_merchant_contract", PROF_MERCHANT, "深渊契据", "Boss后额外获得金币和一张升级的金币牌。");
        addTalent("t_merchant_blackmarket", PROF_MERCHANT, "黑市通行", "获得金币和遗物；商店多一张牌，每回合第二张牌产生金币。");
        addTalent("t_bloodbound_scar", PROF_BLOODBOUND, "猩红旧疤", "获得最大生命；血契自损牌提供格挡，血契反击施加易伤。");
        addTalent("t_bloodbound_feast", PROF_BLOODBOUND, "饥渴回响", "低血线伤害会少量治疗，血契反击治疗更多。");
        addTalent("t_bloodbound_crimson", PROF_BLOODBOUND, "猩红起誓", "首回合失去少量生命换取能量和格挡，并获得升级血契刻印。");
        addTalent("t_weaver_setup", PROF_WEAVER, "先手织局", "每场战斗首回合额外能量，并获得升级格纹阵。");
        addTalent("t_weaver_mastery", PROF_WEAVER, "定式大师", "升级技能会额外抽牌；织牌师重织触发抽更多牌。");
        addTalent("t_weaver_quicksilver", PROF_WEAVER, "流银线", "首回合制造临时织线；临时牌提供格挡并抽牌。");
        addTalent("t_summoner_court", PROF_SUMMONER, "灵庭扩建", "职业技会额外制造游魂引；获得升级灵庭。");
        addTalent("t_summoner_bond", PROF_SUMMONER, "守灵羁绊", "首回合获得格挡；获得最大生命和升级守灵。");
        addTalent("t_summoner_swarm", PROF_SUMMONER, "群灵回响", "灵潮触发后制造临时灵火；获得升级游魂引。");
        addTalent("t_hexer_darkdeal", PROF_HEXER, "暗账交易", "状态牌提供金币和抽牌；职业技加入裂伤，获得升级暗契。");
        addTalent("t_hexer_malediction", PROF_HEXER, "恶咒连环", "首个敌人额外受束缚；获得升级恶咒。");
        addTalent("t_hexer_cleanse", PROF_HEXER, "以咒净咒", "移除一张状态牌，并获得升级以咒净咒。");
        addTalent("t_inscriber_rubbing", PROF_INSCRIBER, "拓印工法", "获得升级刻板抄录并升级牌组；升级牌提供格挡和职业技充能。");
        addTalent("t_inscriber_etching", PROF_INSCRIBER, "裂纹蚀刻", "获得升级符雨；刻印异常会追加伤害，并把易伤与束缚扩散到敌群。");
        addTalent("t_inscriber_archive", PROF_INSCRIBER, "残页归档", "获得升级重写羊皮卷并净化状态；消耗状态牌时获得能量、抽牌和充能。");
        addTalent("t_warden_vanguard", PROF_WARDEN, "先锋壁阵", "获得最大生命和升级盾阵号令；高格挡技能追加充能、格挡与穿透反击。");
        addTalent("t_duelist_masterstep", PROF_DUELIST, "宗师终步", "获得升级闪步终拍；每回合第5张牌获得能量、充能与穿透追击。");
        addTalent("t_alchemist_grandbrew", PROF_ALCHEMIST, "大师炼台", "获得升级连锁反应釜和药剂；制药与异常牌强化势能，用药扩散异常。");
        addTalent("t_ranger_apex", PROF_RANGER, "顶点猎场", "获得升级猎场封锁；攻击重度束缚或易伤敌人更强，束缚牌追加标记追击。");
        addTalent("t_arcanist_singularity", PROF_ARCANIST, "奇点回路", "获得升级秘能汇流；消耗、临时与回声牌加速充能，每3张制造临时秘文。");
        addTalent("t_merchant_monopoly", PROF_MERCHANT, "垄断契账", "获得金币和升级期货契据；金币牌追加收益、格挡，富裕时推动职业技。");
        addTalent("t_bloodbound_hemocraft", PROF_BLOODBOUND, "血工秘艺", "获得最大生命和升级血印风暴；自损与裂伤提供格挡，血契反击会补裂伤。");
        addTalent("t_weaver_grandpattern", PROF_WEAVER, "大定式", "升级牌和牌序工程提供格挡与充能；连续定式会升级手牌、抽牌并加固防线。");
        addTalent("t_summoner_overflow", PROF_SUMMONER, "满溢灵潮", "获得升级灵庭；首回合临时游魂引，召唤与临时牌持续束缚并续接灵火。");
        addTalent("t_hexer_abysscurse", PROF_HEXER, "深渊咒冠", "获得升级恶咒并加入眩光；异常与状态牌会向敌群扩散易伤、束缚和穿透伤害。");
        addTalent("t_inscriber_grandcodex", PROF_INSCRIBER, "活页宗典", "获得最大生命和升级无名刻典；升级、刻印与状态牌持续给职业技、格挡和抽牌。");
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
        public String pact = "";
        public String skillSpec = "";
        public String lastRunSummary = "";
        public int ascension;
        public int hp;
        public int maxHp;
        public int gold;
        public int act;
        public int floor;
        public int currentNode;
        public int currentRoute;
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
        public final ArrayList<String> pactChoices = new ArrayList<>();
        public ArrayList<String> skillSpecChoices = new ArrayList<>();
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
        public boolean shopScoutUsed;
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
        public int professionSkillCharge;
        public int professionUsedThisTurn;
        public int relicTriggersThisTurn;
        public int bossRelicTriggersThisTurn;
        public int cardsPlayedThisTurn;
        public int totalCardsPlayed;
        public int pactFulfilled;
        public int pactMaxBlock;
        public int pactMaxCardsTurn;
        public int pactPotionsUsed;
        public int pactKills;
        public int pactExhaustedCards;
        public int pactSelfDamage;
        public int pactTempCards;
        public int pactStatusCards;
        public int pactForgeCards;
        public int pactGoldCards;
        public int masterySkillCharge;
        public int skillSpecLevel;
        public int buildResonanceFocus = -1;
        public int buildResonanceScore;
        public int runGuardMilestone;
        public int runComboMilestone;
        public int runHexMilestone;
        public int runEchoMilestone;
        public int runBloodcoinMilestone;
        public int runForgeMilestone;
        public int runMilestoneFlags;
        public boolean professionSkillUsedThisTurn;

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
            if (pact == null) {
                pact = "";
            }
            if (skillSpec == null) {
                skillSpec = "";
            }
            if (skillSpec.length() > 0 && skillSpecLevel <= 0) {
                skillSpecLevel = 1;
            }
            if (skillSpecChoices == null) {
                skillSpecChoices = new ArrayList<>();
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
            if (mode != MODE_COMBAT && buildResonanceScore == 0) {
                buildResonanceFocus = -1;
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
        public int skillChargeGain;
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
        public int route;
        public boolean available;
        public final ArrayList<Integer> next = new ArrayList<>();
    }

    public static final class RewardCard implements Serializable {
        public String id;
        public String hint = "";
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

    public static final class PactDef implements Serializable {
        public String id;
        public String name;
        public String text;
    }

    public static final class SkillSpecDef implements Serializable {
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
