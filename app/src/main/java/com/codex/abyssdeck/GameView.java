package com.codex.abyssdeck;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public final class GameView extends View {
    private static final String SAVE = "abyss_deck_save";
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<Button> buttons = new ArrayList<>();
    private final ArrayList<CardHit> cardHits = new ArrayList<>();
    private final ArrayList<EnemyHit> enemyHits = new ArrayList<>();
    private final ArrayList<MapHit> mapHits = new ArrayList<>();
    private GameCore.State s;
    private int selectedHand = -1;
    private long lastFrame;
    private Bitmap[] atlases = new Bitmap[8];

    public GameView(Context context) {
        super(context);
        setFocusable(true);
        text.setColor(0xfff5ead2);
        text.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL));
        loadAtlases();
        s = load();
        if (s == null) {
            s = GameCore.fresh();
        }
        s.ensureRandom();
        lastFrame = System.currentTimeMillis();
    }

    public void saveNow() {
        save();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        s.ensureRandom();
        buttons.clear();
        cardHits.clear();
        enemyHits.clear();
        mapHits.clear();
        drawBackground(c);
        drawTopBar(c);
        if (s.mode == GameCore.MODE_TITLE) {
            drawTitle(c);
        } else if (s.mode == GameCore.MODE_ORIGIN) {
            drawOrigins(c);
        } else if (s.mode == GameCore.MODE_BOON) {
            drawBoons(c);
        } else if (s.mode == GameCore.MODE_MAP) {
            drawMap(c);
        } else if (s.mode == GameCore.MODE_COMBAT) {
            drawCombat(c);
        } else if (s.mode == GameCore.MODE_REWARD) {
            drawReward(c);
        } else if (s.mode == GameCore.MODE_SHOP) {
            drawShop(c);
        } else if (s.mode == GameCore.MODE_REST) {
            drawRest(c);
        } else if (s.mode == GameCore.MODE_EVENT) {
            drawEvent(c);
        } else if (s.mode == GameCore.MODE_DECK) {
            drawDeck(c);
        } else if (s.mode == GameCore.MODE_CODEX) {
            drawCodex(c);
        } else if (s.mode == GameCore.MODE_GAME_OVER) {
            drawEnd(c, false);
        } else if (s.mode == GameCore.MODE_VICTORY) {
            drawEnd(c, true);
        }
        drawLog(c);
        drawButtons(c);
        invalidateSoon();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() != MotionEvent.ACTION_UP) {
            return true;
        }
        float x = e.getX();
        float y = e.getY();
        for (int i = buttons.size() - 1; i >= 0; i--) {
            Button b = buttons.get(i);
            if (b.rect.contains(x, y)) {
                runAction(b.action, b.index);
                invalidate();
                return true;
            }
        }
        if (s.mode == GameCore.MODE_COMBAT) {
            handleCombatTouch(x, y);
        } else if (s.mode == GameCore.MODE_MAP) {
            for (MapHit h : mapHits) {
                if (h.rect.contains(x, y)) {
                    GameCore.mapChoose(s, h.index);
                    selectedHand = -1;
                    save();
                    break;
                }
            }
        } else if (s.mode == GameCore.MODE_DECK) {
            for (CardHit h : cardHits) {
                if (h.rect.contains(x, y)) {
                    GameCore.deckPick(s, h.index);
                    save();
                    break;
                }
            }
        }
        invalidate();
        return true;
    }

    private void runAction(String action, int index) {
        if ("new".equals(action)) {
            GameCore.start(s);
        } else if ("continue".equals(action)) {
            if (s.origin.length() == 0) {
                GameCore.start(s);
            } else {
                s.mode = GameCore.MODE_MAP;
            }
        } else if ("codex".equals(action)) {
            GameCore.openCodex(s);
        } else if ("clear".equals(action)) {
            getContext().getSharedPreferences("abyss", Context.MODE_PRIVATE).edit().clear().apply();
            s = GameCore.fresh();
        } else if ("origin".equals(action)) {
            String[] origins = {GameCore.ORIGIN_STEEL, GameCore.ORIGIN_ASH, GameCore.ORIGIN_WILD, GameCore.ORIGIN_VOID};
            GameCore.chooseOrigin(s, origins[index]);
        } else if ("boon".equals(action)) {
            GameCore.chooseBoon(s, index);
        } else if ("depth".equals(action)) {
            GameCore.chooseDepth(s, index);
        } else if ("deck".equals(action)) {
            GameCore.openDeck(s, s.mode);
        } else if ("pile".equals(action)) {
            GameCore.openDeckView(s, s.mode, index);
        } else if ("close".equals(action)) {
            GameCore.closePanel(s);
        } else if ("endturn".equals(action)) {
            selectedHand = -1;
            GameCore.endTurn(s);
        } else if ("potion".equals(action)) {
            GameCore.usePotion(s, index, currentEnemyTarget());
        } else if ("reward_card".equals(action)) {
            GameCore.pickRewardCard(s, index);
        } else if ("reward_relic".equals(action)) {
            GameCore.pickRelicReward(s, index);
        } else if ("skip".equals(action)) {
            GameCore.skipReward(s);
        } else if ("shop_card".equals(action)) {
            GameCore.shopBuyCard(s, index);
        } else if ("shop_relic".equals(action)) {
            GameCore.shopBuyRelic(s, index);
        } else if ("shop_potion".equals(action)) {
            GameCore.shopBuyPotion(s, index);
        } else if ("shop_remove".equals(action)) {
            GameCore.shopChoose(s, "shop_remove");
        } else if ("shop_upgrade".equals(action)) {
            GameCore.shopChoose(s, "shop_upgrade");
        } else if ("shop_transform".equals(action)) {
            GameCore.shopChoose(s, "shop_transform");
        } else if ("leave_shop".equals(action)) {
            GameCore.leaveShop(s);
        } else if ("rest_heal".equals(action)) {
            GameCore.restHeal(s);
        } else if ("rest_upgrade".equals(action)) {
            GameCore.restChoose(s, "rest_upgrade");
        } else if ("rest_remove".equals(action)) {
            GameCore.restChoose(s, "rest_remove");
        } else if ("rest_transform".equals(action)) {
            GameCore.restChoose(s, "rest_transform");
        } else if ("event".equals(action)) {
            GameCore.eventChoose(s, index);
        } else if ("title".equals(action)) {
            s.mode = GameCore.MODE_TITLE;
        } else if ("decktab".equals(action)) {
            s.deckView = index;
        }
        save();
    }

    private void handleCombatTouch(float x, float y) {
        for (CardHit h : cardHits) {
            if (h.rect.contains(x, y)) {
                if (selectedHand == h.index) {
                    selectedHand = -1;
                } else {
                    selectedHand = h.index;
                }
                return;
            }
        }
        if (selectedHand >= 0) {
            for (EnemyHit eh : enemyHits) {
                if (eh.rect.contains(x, y)) {
                    GameCore.playCard(s, selectedHand, eh.index);
                    selectedHand = -1;
                    save();
                    return;
                }
            }
            if (!needsEnemy(selectedHand)) {
                GameCore.playCard(s, selectedHand, -1);
                selectedHand = -1;
                save();
            }
        }
    }

    private int currentEnemyTarget() {
        for (int i = 0; i < s.enemies.size(); i++) {
            if (s.enemies.get(i).hp > 0) {
                return i;
            }
        }
        return -1;
    }

    private boolean needsEnemy(int handIndex) {
        if (handIndex < 0 || handIndex >= s.hand.size()) {
            return false;
        }
        GameCore.CardDef d = GameCore.card(s.hand.get(handIndex).id);
        return d != null && d.targetEnemy;
    }

    private void drawBackground(Canvas c) {
        int w = getWidth();
        int h = getHeight();
        p.setShader(new LinearGradient(0, 0, 0, h, 0xff090d13, 0xff1b1521, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);
        p.setColor(0x22d8b66a);
        for (int i = 0; i < 9; i++) {
            float y = (i * 137 + (System.currentTimeMillis() / 80) % 137) % Math.max(1, h);
            c.drawLine(0, y, w, y + h * 0.08f, p);
        }
    }

    private void drawTopBar(Canvas c) {
        if (s.mode == GameCore.MODE_TITLE || s.mode == GameCore.MODE_ORIGIN || s.mode == GameCore.MODE_BOON) {
            return;
        }
        int w = getWidth();
        p.setColor(0xdd0b1018);
        c.drawRoundRect(new RectF(dp(10), dp(8), w - dp(10), dp(54)), dp(6), dp(6), p);
        drawText(c, "深渊牌旅", dp(20), dp(36), 20, 0xfff3d486, true);
        String info = "Act " + s.act + "  层 " + s.floor + "  HP " + s.hp + "/" + s.maxHp + "  金 " + s.gold;
        drawText(c, info, dp(128), dp(36), 15, 0xffe9ddc7, false);
        addButton(w - dp(92), dp(16), dp(76), dp(30), "牌组", "deck", 0);
    }

    private void drawTitle(Canvas c) {
        int w = getWidth();
        int h = getHeight();
        drawText(c, "深渊牌旅", dp(30), h * 0.18f, 42, 0xfff6d780, true);
        drawText(c, "原创暗潮爬塔卡牌游戏", dp(32), h * 0.18f + dp(38), 17, 0xffd6cdbd, false);
        drawText(c, "四种起源，十二层分支地图，卡组、遗物、药剂与事件共同塑造每一局。", dp(32), h * 0.18f + dp(72), 15, 0xffb9c7cf, false);
        addButton(dp(30), h * 0.38f, w - dp(60), dp(54), "新旅程", "new", 0);
        addButton(dp(30), h * 0.47f, w - dp(60), dp(48), "继续", "continue", 0);
        addButton(dp(30), h * 0.55f, w - dp(60), dp(48), "图鉴", "codex", 0);
        addButton(dp(30), h * 0.63f, w - dp(60), dp(44), "清除存档", "clear", 0);
    }

    private void drawOrigins(Canvas c) {
        int w = getWidth();
        drawText(c, "选择起源", dp(24), dp(78), 30, 0xfff4d580, true);
        String[] names = {GameCore.ORIGIN_STEEL, GameCore.ORIGIN_ASH, GameCore.ORIGIN_WILD, GameCore.ORIGIN_VOID};
        String[] desc = {
                "格挡、反击、以守转攻，稳定推进。",
                "燃灼、过载、引爆，爆发上限极高。",
                "束缚、成长、恢复，强控制与续航。",
                "抽牌、消耗、回声，资源循环最灵动。"
        };
        for (int i = 0; i < names.length; i++) {
            float y = dp(104) + i * dp(118);
            p.setColor(0xcc121923);
            c.drawRoundRect(new RectF(dp(22), y, w - dp(22), y + dp(94)), dp(8), dp(8), p);
            p.setColor(GameCore.originColor(names[i]));
            c.drawRoundRect(new RectF(dp(28), y + dp(10), dp(88), y + dp(70)), dp(8), dp(8), p);
            drawSigil(c, names[i], dp(58), y + dp(40), dp(25));
            drawText(c, names[i], dp(104), y + dp(34), 24, 0xfff5ead2, true);
            drawText(c, desc[i], dp(104), y + dp(62), 14, 0xffc5d0cf, false);
            addButton(w - dp(106), y + dp(28), dp(80), dp(38), "选择", "origin", i);
        }
        drawText(c, "难度阶层", dp(24), dp(592), 18, 0xffe9d7a1, true);
        String[] depth = {"浅层", "暗潮", "噩梦", "无光"};
        int[] val = {0, 3, 6, 10};
        for (int i = 0; i < depth.length; i++) {
            addButton(dp(24) + i * dp(86), dp(610), dp(76), dp(36), depth[i] + (s.ascension == val[i] ? "*" : ""), "depth", val[i]);
        }
    }

    private void drawBoons(Canvas c) {
        int w = getWidth();
        drawText(c, "选择开局赐印", dp(24), dp(86), 30, 0xfff4d580, true);
        drawText(c, s.origin + " / 难度阶层 " + s.ascension, dp(26), dp(116), 15, 0xffc9d2d2, false);
        for (int i = 0; i < s.boonChoices.size(); i++) {
            GameCore.BoonDef b = GameCore.boon(s.boonChoices.get(i));
            float y = dp(154) + i * dp(118);
            p.setColor(0xcc121923);
            c.drawRoundRect(new RectF(dp(24), y, w - dp(24), y + dp(92)), dp(8), dp(8), p);
            p.setColor(0xffd9b85f);
            c.drawCircle(dp(58), y + dp(46), dp(24), p);
            drawText(c, b == null ? "" : b.name, dp(96), y + dp(36), 22, 0xfff5ead2, true);
            drawText(c, b == null ? "" : b.text, dp(96), y + dp(64), 14, 0xffc5d0cf, false);
            addButton(w - dp(112), y + dp(28), dp(86), dp(38), "选择", "boon", i);
        }
    }

    private void drawMap(Canvas c) {
        int w = getWidth();
        int h = getHeight();
        drawText(c, "第 " + s.act + " 幕路线", dp(24), dp(84), 28, 0xfff4d580, true);
        float left = dp(38);
        float top = dp(118);
        float row = (h - dp(220)) / 11f;
        float col = (w - dp(76)) / 4f;
        p.setStrokeWidth(dp(2));
        for (int i = 0; i < s.map.size(); i++) {
            GameCore.MapNode n = s.map.get(i);
            float x1 = left + n.lane * col;
            float y1 = top + (n.floor - 1) * row;
            for (Integer next : n.next) {
                if (next >= 0 && next < s.map.size()) {
                    GameCore.MapNode nn = s.map.get(next);
                    float x2 = left + nn.lane * col;
                    float y2 = top + (nn.floor - 1) * row;
                    p.setColor(0x55989baa);
                    c.drawLine(x1, y1, x2, y2, p);
                }
            }
        }
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < s.map.size(); i++) {
            GameCore.MapNode n = s.map.get(i);
            float x = left + n.lane * col;
            float y = top + (n.floor - 1) * row;
            boolean nightReachable = s.relics.contains("night_map") && n.floor == s.floor + 1;
            int color = (n.available || nightReachable) ? 0xffe4c464 : 0xff27303b;
            if (i == s.currentNode) {
                color = 0xff79c7ff;
            }
            p.setColor(color);
            c.drawCircle(x, y, dp(17), p);
            drawText(c, GameCore.nodeName(n.type), x - dp(8), y + dp(6), 15, (n.available || nightReachable) ? 0xff17140d : 0xffb8c0c8, true);
            RectF r = new RectF(x - dp(24), y - dp(24), x + dp(24), y + dp(24));
            mapHits.add(new MapHit(r, i));
        }
        String hint = s.relics.contains("night_map") ? "夜航图生效：下一层任意节点均可选择。" : "可选节点以金色显示。分支路线会改变资源、风险与奖励密度。";
        drawText(c, hint, dp(24), h - dp(74), 14, 0xffc9d2d2, false);
    }

    private void drawCombat(Canvas c) {
        int w = getWidth();
        int h = getHeight();
        drawText(c, "能量 " + s.energy + "   格挡 " + s.block + "   回合 " + s.turn, dp(22), dp(82), 22, 0xfff2d373, true);
        String engines = "守势 " + s.steelEngine + "  热度 " + s.ashEngine + "  再生 " + s.wildEngine + "  回声势 " + s.voidEngine;
        if (s.burnPower > 0 || s.bindPower > 0) {
            engines += "  燃势 " + s.burnPower + "  束缚势 " + s.bindPower;
        }
        drawText(c, engines, dp(22), dp(104), 13, 0xffd6dfda, true);
        if (s.encounterModifier != GameCore.MOD_NONE) {
            drawText(c, "词缀：" + GameCore.modifierName(s.encounterModifier) + " - " + GameCore.modifierText(s.encounterModifier), dp(22), dp(122), 13, 0xffc9d7d3, false);
        }
        if (s.vulnerable > 0) {
            drawText(c, "易伤 " + s.vulnerable, dp(22), dp(140), 15, 0xffff8d75, true);
        }
        float enemyTop = dp(144);
        float gap = w / (s.enemies.size() + 1f);
        for (int i = 0; i < s.enemies.size(); i++) {
            GameCore.Enemy e = s.enemies.get(i);
            float cx = gap * (i + 1);
            float cy = enemyTop + dp(80);
            drawEnemy(c, e, cx, cy, i);
        }
        drawPiles(c);
        float cardW = Math.min(dp(112), (w - dp(24)) / Math.max(1, s.hand.size()) - dp(4));
        float cardH = cardW * 1.42f;
        float start = (w - (cardW + dp(6)) * s.hand.size() + dp(6)) / 2f;
        float y = h - cardH - dp(70);
        for (int i = 0; i < s.hand.size(); i++) {
            float x = start + i * (cardW + dp(6));
            drawCard(c, s.hand.get(i), new RectF(x, y - (selectedHand == i ? dp(18) : 0), x + cardW, y + cardH - (selectedHand == i ? dp(18) : 0)), selectedHand == i);
            cardHits.add(new CardHit(new RectF(x, y - dp(28), x + cardW, y + cardH), i));
        }
        addButton(w - dp(112), h - dp(52), dp(96), dp(40), "结束回合", "endturn", 0);
        for (int i = 0; i < s.potions.size(); i++) {
            String name = GameCore.potion(s.potions.get(i)).name;
            if (name.length() > 3) {
                name = name.substring(0, 3);
            }
            addButton(dp(18) + i * dp(66), h - dp(52), dp(60), dp(38), name, "potion", i);
        }
    }

    private void drawEnemy(Canvas c, GameCore.Enemy e, float cx, float cy, int index) {
        RectF body = new RectF(cx - dp(56), cy - dp(48), cx + dp(56), cy + dp(56));
        p.setColor(e.hp > 0 ? 0xff2b3444 : 0xff11151b);
        c.drawRoundRect(body, dp(10), dp(10), p);
        p.setColor(0xff7d6a5d);
        c.drawOval(new RectF(cx - dp(36), cy - dp(36), cx + dp(36), cy + dp(38)), p);
        p.setColor(0xffe6d2b0);
        c.drawCircle(cx - dp(12), cy - dp(8), dp(4), p);
        c.drawCircle(cx + dp(12), cy - dp(8), dp(4), p);
        drawText(c, e.name, cx - dp(50), cy + dp(74), 13, 0xfff1e7d4, true);
        drawBar(c, cx - dp(48), cy + dp(84), dp(96), dp(10), e.hp, e.maxHp, 0xffd44c4c);
        String intent = "攻 " + e.intentValue;
        if (e.intent == GameCore.ENEMY_BUFF) intent = "强化 " + e.intentValue;
        if (e.intent == GameCore.ENEMY_DEBUFF) intent = "压迫";
        if (e.intent == GameCore.ENEMY_GUARD) intent = "护甲 " + e.intentValue;
        if (e.intent == GameCore.ENEMY_SPECIAL) intent = "异动 " + e.intentValue;
        drawText(c, intent, cx - dp(38), cy - dp(58), 14, 0xffffd166, true);
        String tags = "";
        if (e.block > 0) tags += "甲" + e.block + " ";
        if (e.burn > 0) tags += "燃" + e.burn + " ";
        if (e.bind > 0) tags += "缚" + e.bind + " ";
        if (e.vulnerable > 0) tags += "易" + e.vulnerable;
        drawText(c, tags, cx - dp(48), cy + dp(108), 12, 0xffbcd3d1, false);
        enemyHits.add(new EnemyHit(body, index));
    }

    private void drawPiles(Canvas c) {
        int w = getWidth();
        float y = dp(308);
        drawPileButton(c, "抽", s.draw.size(), 1, w - dp(106), y - dp(17));
        drawPileButton(c, "弃", s.discard.size(), 2, w - dp(106), y + dp(5));
        drawPileButton(c, "耗", s.exhaust.size(), 3, w - dp(106), y + dp(27));
    }

    private void drawPileButton(Canvas c, String label, int count, int deckView, float x, float y) {
        RectF r = new RectF(x, y, x + dp(86), y + dp(20));
        p.setColor(0x33111823);
        c.drawRoundRect(r, dp(5), dp(5), p);
        p.setColor(0x44d9b85f);
        c.drawRoundRect(new RectF(r.left, r.top, r.left + dp(4), r.bottom), dp(4), dp(4), p);
        drawText(c, label + " " + count, r.left + dp(10), r.top + dp(15), 13, 0xffc9d6d2, true);
        addButton(r.left, r.top, r.width(), r.height(), "", "pile", deckView);
    }

    private void drawReward(Canvas c) {
        int w = getWidth();
        drawText(c, "战利品", dp(24), dp(88), 30, 0xfff4d580, true);
        if (s.encounterModifier != GameCore.MOD_NONE) {
            drawText(c, "本场词缀：" + GameCore.modifierName(s.encounterModifier), dp(126), dp(88), 14, 0xffc7d4d0, false);
        }
        int rewardCards = Math.max(1, s.cardRewards.size());
        float cardW = Math.min(dp(126), (w - dp(44)) / rewardCards - dp(8));
        float cardH = cardW * 1.42f;
        if (s.cardRewards.isEmpty()) {
            String status = s.cardRewardSkipped ? "卡牌奖励已跳过" : "没有卡牌奖励";
            drawText(c, status, dp(24), dp(136), 17, 0xffc9d2d2, true);
        } else {
            drawText(c, "选择一张卡牌，或跳过获得金币", dp(24), dp(112), 13, 0xffc9d2d2, false);
        }
        for (int i = 0; i < s.cardRewards.size(); i++) {
            GameCore.Card card = new GameCore.Card(s.cardRewards.get(i).id);
            RectF r = new RectF(dp(22) + i * (cardW + dp(8)), dp(120), dp(22) + i * (cardW + dp(8)) + cardW, dp(120) + cardH);
            drawCard(c, card, r, false);
            addButton(r.left, r.bottom + dp(8), r.width(), dp(34), "收下", "reward_card", i);
        }
        float y = dp(120) + cardH + dp(72);
        if (!s.relicRewards.isEmpty()) {
            drawText(c, s.combatKind == 'B' ? "Boss 遗物选择" : "遗物", dp(24), y - dp(12), 17, 0xffe9d7a1, true);
        }
        for (int i = 0; i < s.relicRewards.size(); i++) {
            GameCore.RelicDef r = GameCore.relic(s.relicRewards.get(i));
            drawRelicRow(c, r, dp(24), y + i * dp(62), w - dp(48));
            addButton(w - dp(104), y + i * dp(62) + dp(10), dp(82), dp(36), "拿取", "reward_relic", i);
        }
        if (!s.cardRewards.isEmpty() && !s.cardRewardSkipped) {
            int gold = s.relics.contains("cracked_compass") ? 14 : 10;
            addButton(dp(24), getHeight() - dp(62), w - dp(48), dp(42), "跳过卡牌奖励 +" + gold + "金", "skip", 0);
        }
    }

    private void drawShop(Canvas c) {
        int w = getWidth();
        drawText(c, "裂隙商栈", dp(24), dp(86), 30, 0xfff4d580, true);
        drawText(c, "金币 " + s.gold, w - dp(118), dp(84), 20, 0xffffdb7a, true);
        float cardW = Math.min(dp(104), (w - dp(34)) / 5f - dp(6));
        float cardH = cardW * 1.42f;
        for (int i = 0; i < s.shopCards.size(); i++) {
            GameCore.Card card = new GameCore.Card(s.shopCards.get(i));
            RectF r = new RectF(dp(18) + i * (cardW + dp(6)), dp(112), dp(18) + i * (cardW + dp(6)) + cardW, dp(112) + cardH);
            drawCard(c, card, r, false);
            int price = GameCore.cardPrice(GameCore.card(card.id));
            addButton(r.left, r.bottom + dp(6), r.width(), dp(30), price + "金", "shop_card", i);
        }
        float y = dp(112) + cardH + dp(50);
        drawText(c, "遗物", dp(24), y, 18, 0xffe9d7a1, true);
        for (int i = 0; i < s.shopRelics.size(); i++) {
            GameCore.RelicDef r = GameCore.relic(s.shopRelics.get(i));
            drawRelicRow(c, r, dp(24), y + dp(18) + i * dp(52), w - dp(48));
            addButton(w - dp(96), y + dp(26) + i * dp(52), dp(74), dp(32), "165金", "shop_relic", i);
        }
        float py = y + dp(180);
        drawText(c, "药剂", dp(24), py, 18, 0xffe9d7a1, true);
        for (int i = 0; i < s.shopPotions.size(); i++) {
            GameCore.PotionDef po = GameCore.potion(s.shopPotions.get(i));
            addButton(dp(24) + i * dp(108), py + dp(18), dp(98), dp(36), po.name + " 42", "shop_potion", i);
        }
        float sy = py + dp(76);
        addButton(dp(24), sy, dp(98), dp(40), "删牌85", "shop_remove", 0);
        addButton(dp(132), sy, dp(98), dp(40), "升级75", "shop_upgrade", 0);
        addButton(dp(240), sy, dp(108), dp(40), "转化105", "shop_transform", 0);
        addButton(dp(24), getHeight() - dp(58), w - dp(48), dp(42), "离开商店", "leave_shop", 0);
    }

    private void drawRest(Canvas c) {
        int w = getWidth();
        drawText(c, "静火营地", dp(24), dp(88), 30, 0xfff4d580, true);
        drawText(c, "火光不问你来处，只问你下一层要付出什么。", dp(24), dp(120), 15, 0xffc8d2ce, false);
        addButton(dp(28), dp(170), w - dp(56), dp(54), "休整：恢复生命", "rest_heal", 0);
        addButton(dp(28), dp(240), w - dp(56), dp(54), "锻造：升级一张牌", "rest_upgrade", 0);
        addButton(dp(28), dp(310), w - dp(56), dp(54), "净化：移除一张牌", "rest_remove", 0);
        addButton(dp(28), dp(380), w - dp(56), dp(54), "转化：重塑一张牌", "rest_transform", 0);
    }

    private void drawEvent(Canvas c) {
        int w = getWidth();
        drawText(c, eventTitle(), dp(24), dp(88), 30, 0xfff4d580, true);
        drawWrapped(c, eventText(), dp(24), dp(126), w - dp(48), 16, 0xffd4ddd8);
        String[] choices = eventChoices();
        for (int i = 0; i < choices.length; i++) {
            addButton(dp(28), dp(260) + i * dp(66), w - dp(56), dp(48), choices[i], "event", i);
        }
    }

    private void drawDeck(Canvas c) {
        int w = getWidth();
        drawText(c, deckTitle(), dp(24), dp(86), 27, 0xfff4d580, true);
        String[] tabs = {"牌组", "抽牌", "弃牌", "消耗"};
        for (int i = 0; i < tabs.length; i++) {
            addButton(dp(18) + i * dp(82), dp(102), dp(74), dp(34), tabs[i] + (s.deckView == i ? "*" : ""), "decktab", i);
        }
        List<GameCore.Card> list = deckList();
        float cardW = Math.min(dp(104), (w - dp(34)) / 3f - dp(8));
        float cardH = cardW * 1.42f;
        int cols = 3;
        for (int i = 0; i < list.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            float x = dp(18) + col * (cardW + dp(8));
            float y = dp(150) + row * (cardH + dp(12));
            RectF r = new RectF(x, y, x + cardW, y + cardH);
            drawCard(c, list.get(i), r, false);
            if (s.deckView == 0 && s.pendingAction != null && s.pendingAction.length() > 0) {
                cardHits.add(new CardHit(r, i));
            }
        }
        addButton(w - dp(94), dp(102), dp(76), dp(34), "返回", "close", 0);
    }

    private void drawCodex(Canvas c) {
        int w = getWidth();
        drawText(c, "图鉴", dp(24), dp(86), 30, 0xfff4d580, true);
        drawText(c, "卡牌 " + GameCore.CARD_LIBRARY.size() + "   遗物 " + GameCore.RELIC_LIBRARY.size() + "   药剂 " + GameCore.POTION_LIBRARY.size(), dp(24), dp(118), 16, 0xffc9d2d2, false);
        float y = dp(152);
        for (int i = 0; i < Math.min(16, GameCore.CARD_LIBRARY.size()); i++) {
            GameCore.CardDef d = GameCore.CARD_LIBRARY.get(i);
            drawText(c, d.name + " / " + d.origin + " / " + rarity(d.rarity), dp(28), y + i * dp(24), 15, 0xffe8ddc9, false);
        }
        addButton(w - dp(94), dp(102), dp(76), dp(34), "返回", "close", 0);
    }

    private void drawEnd(Canvas c, boolean victory) {
        int w = getWidth();
        int h = getHeight();
        drawText(c, victory ? "抵达无光尽头" : "旅程终止", dp(30), h * 0.25f, 34, victory ? 0xfff6d780 : 0xffff8d75, true);
        drawText(c, "Act " + s.act + " / 层 " + s.floor + " / 牌组 " + s.deck.size() + " 张 / 遗物 " + s.relics.size(), dp(32), h * 0.25f + dp(44), 16, 0xffd8ded8, false);
        addButton(dp(30), h * 0.45f, w - dp(60), dp(52), "再来一局", "new", 0);
        addButton(dp(30), h * 0.54f, w - dp(60), dp(46), "返回标题", "title", 0);
    }

    private void drawCard(Canvas c, GameCore.Card card, RectF r, boolean selected) {
        GameCore.CardDef d = GameCore.card(card.id);
        if (d == null) {
            return;
        }
        p.setColor(selected ? 0xffffd36e : 0xff0d121a);
        c.drawRoundRect(r, dp(7), dp(7), p);
        RectF inner = new RectF(r.left + dp(3), r.top + dp(3), r.right - dp(3), r.bottom - dp(3));
        p.setColor(0xff202838);
        c.drawRoundRect(inner, dp(6), dp(6), p);
        drawCardArt(c, d, new RectF(inner.left + dp(5), inner.top + dp(22), inner.right - dp(5), inner.top + inner.height() * 0.55f));
        p.setColor(GameCore.originColor(d.origin));
        c.drawRoundRect(new RectF(inner.left, inner.top, inner.right, inner.top + dp(20)), dp(5), dp(5), p);
        drawText(c, d.name + (card.upgraded ? "+" : ""), inner.left + dp(5), inner.top + dp(15), Math.max(10, inner.width() / 8.2f), 0xff121416, true);
        p.setColor(0xffe8d27a);
        c.drawCircle(inner.left + dp(14), inner.top + dp(32), dp(12), p);
        int cost = card.upgraded ? Math.max(0, d.cost - d.upgradeCostDrop) : d.cost;
        drawText(c, String.valueOf(cost), inner.left + dp(10), inner.top + dp(37), 14, 0xff1b150b, true);
        drawWrapped(c, GameCore.cardText(card), inner.left + dp(7), inner.top + inner.height() * 0.62f, inner.width() - dp(14), Math.max(9, inner.width() / 10.5f), 0xfff3ead7);
        drawText(c, rarity(d.rarity), inner.left + dp(7), inner.bottom - dp(6), 10, 0xffc8d0d0, false);
    }

    private void drawCardArt(Canvas c, GameCore.CardDef d, RectF r) {
        int idx = Math.abs(d.id.hashCode());
        Bitmap atlas = atlases[(idx / 16) % atlases.length];
        if (atlas != null) {
            int cell = idx % 16;
            int col = cell % 4;
            int row = cell / 4;
            android.graphics.Rect src = new android.graphics.Rect(col * atlas.getWidth() / 4, row * atlas.getHeight() / 4, (col + 1) * atlas.getWidth() / 4, (row + 1) * atlas.getHeight() / 4);
            c.drawBitmap(atlas, src, r, p);
            p.setColor(0x44000000);
            c.drawRoundRect(r, dp(4), dp(4), p);
            return;
        }
        p.setShader(new LinearGradient(r.left, r.top, r.right, r.bottom, GameCore.originColor(d.origin), 0xff111721, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, dp(4), dp(4), p);
        p.setShader(null);
        drawSigil(c, d.origin, r.centerX(), r.centerY(), r.width() * 0.22f);
    }

    private void drawSigil(Canvas c, String origin, float cx, float cy, float size) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2, size / 8));
        p.setColor(0xee11151b);
        if (GameCore.ORIGIN_STEEL.equals(origin)) {
            c.drawRect(cx - size, cy - size, cx + size, cy + size, p);
            c.drawLine(cx - size, cy, cx + size, cy, p);
        } else if (GameCore.ORIGIN_ASH.equals(origin)) {
            Path path = new Path();
            path.moveTo(cx, cy - size);
            path.cubicTo(cx + size, cy, cx + size * 0.2f, cy + size, cx, cy + size);
            path.cubicTo(cx - size, cy + size * 0.3f, cx - size * 0.2f, cy, cx, cy - size);
            c.drawPath(path, p);
        } else if (GameCore.ORIGIN_WILD.equals(origin)) {
            c.drawOval(new RectF(cx - size, cy - size * 0.7f, cx + size, cy + size * 0.7f), p);
            c.drawLine(cx, cy - size, cx, cy + size, p);
        } else {
            c.drawCircle(cx, cy, size, p);
            c.drawLine(cx - size, cy - size, cx + size, cy + size, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    private void drawRelicRow(Canvas c, GameCore.RelicDef r, float x, float y, float width) {
        if (r == null) return;
        p.setColor(0xaa111923);
        c.drawRoundRect(new RectF(x, y, x + width, y + dp(46)), dp(6), dp(6), p);
        p.setColor(0xffd0b66e);
        c.drawCircle(x + dp(22), y + dp(23), dp(14), p);
        drawText(c, r.name, x + dp(44), y + dp(19), 15, 0xfff2ead7, true);
        drawText(c, r.text, x + dp(44), y + dp(37), 11, 0xffb8c6c5, false);
    }

    private void drawButtons(Canvas c) {
        for (Button b : buttons) {
            if (b.label == null || b.label.length() == 0) {
                continue;
            }
            p.setColor(0xffd9b85f);
            c.drawRoundRect(b.rect, dp(6), dp(6), p);
            p.setColor(0x33000000);
            c.drawRoundRect(new RectF(b.rect.left, b.rect.bottom - dp(5), b.rect.right, b.rect.bottom), dp(5), dp(5), p);
            drawCentered(c, b.label, b.rect, 14, 0xff15120c, true);
        }
    }

    private void drawLog(Canvas c) {
        if (s.log.isEmpty() || s.mode == GameCore.MODE_TITLE || s.mode == GameCore.MODE_ORIGIN) {
            return;
        }
        int h = getHeight();
        float y = h - dp(118);
        int start = Math.max(0, s.log.size() - 3);
        for (int i = start; i < s.log.size(); i++) {
            drawText(c, s.log.get(i), dp(18), y + (i - start) * dp(18), 12, 0xffaebdc0, false);
        }
    }

    private void drawBar(Canvas c, float x, float y, float w, float h, int value, int max, int color) {
        p.setColor(0xff171b22);
        c.drawRoundRect(new RectF(x, y, x + w, y + h), h / 2, h / 2, p);
        p.setColor(color);
        float fill = max <= 0 ? 0 : w * Math.max(0, Math.min(1f, value / (float) max));
        c.drawRoundRect(new RectF(x, y, x + fill, y + h), h / 2, h / 2, p);
        drawCentered(c, value + "/" + max, new RectF(x, y - dp(2), x + w, y + h + dp(2)), 9, 0xffffffff, true);
    }

    private void drawText(Canvas c, String s, float x, float y, float size, int color, boolean bold) {
        text.setTextSize(size);
        text.setColor(color);
        text.setFakeBoldText(bold);
        c.drawText(s == null ? "" : s, x, y, text);
        text.setFakeBoldText(false);
    }

    private void drawCentered(Canvas c, String s, RectF r, float size, int color, boolean bold) {
        text.setTextSize(size);
        text.setColor(color);
        text.setFakeBoldText(bold);
        Paint.FontMetrics fm = text.getFontMetrics();
        float x = r.centerX() - text.measureText(s) / 2f;
        float y = r.centerY() - (fm.ascent + fm.descent) / 2f;
        c.drawText(s, x, y, text);
        text.setFakeBoldText(false);
    }

    private void drawWrapped(Canvas c, String body, float x, float y, float width, float size, int color) {
        if (body == null) return;
        text.setTextSize(size);
        text.setColor(color);
        text.setFakeBoldText(false);
        String line = "";
        float yy = y;
        for (int i = 0; i < body.length(); i++) {
            String next = line + body.charAt(i);
            if (text.measureText(next) > width && line.length() > 0) {
                c.drawText(line, x, yy, text);
                yy += size * 1.22f;
                line = String.valueOf(body.charAt(i));
            } else {
                line = next;
            }
        }
        if (line.length() > 0) {
            c.drawText(line, x, yy, text);
        }
    }

    private void addButton(float x, float y, float w, float h, String label, String action, int index) {
        buttons.add(new Button(new RectF(x, y, x + w, y + h), label, action, index));
    }

    private List<GameCore.Card> deckList() {
        if (s.deckView == 1) return s.draw;
        if (s.deckView == 2) return s.discard;
        if (s.deckView == 3) return s.exhaust;
        return s.deck;
    }

    private String deckTitle() {
        if ("rest_upgrade".equals(s.pendingAction) || "shop_upgrade".equals(s.pendingAction)) return "选择要升级的牌";
        if ("event_upgrade_wound".equals(s.pendingAction)) return "选择升级牌：代价是一张裂伤";
        if ("rest_remove".equals(s.pendingAction) || "shop_remove".equals(s.pendingAction)) return "选择要移除的牌";
        if ("event_remove_hp".equals(s.pendingAction)) return "选择移除牌：失去10生命";
        if ("rest_transform".equals(s.pendingAction) || "shop_transform".equals(s.pendingAction)) return "选择要转化的牌";
        return "牌组浏览";
    }

    private String rarity(int r) {
        return r == 2 ? "稀有" : r == 1 ? "进阶" : "普通";
    }

    private String eventTitle() {
        String[] t = {"黑水圣龛", "裂币喷泉", "会呼吸的书页", "无名旅人", "石刻囚笼", "炼金雨棚", "无灯影市", "旧王雾门"};
        return t[Math.max(0, Math.min(t.length - 1, s.eventId))];
    }

    private String eventText() {
        String[] t = {
                "一座浸在黑水里的圣龛低声索取血液。它承诺回赠一件仍在跳动的旧物。",
                "喷泉里涌出的不是水，而是薄如刃口的金币。伸手越深，伤口越多。",
                "书页在你面前自行翻动，字迹像根须一样爬进你的牌组。",
                "旅人没有脸，只递来一枚还温热的硬币，等待你的交换。",
                "囚笼内侧刻满战斗姿势。每一道刻痕都能教会你一件事，也会留下血口。",
                "雨棚下滴落的不是雨，而是未命名的试剂。每瓶都有用，每瓶都不完全干净。",
                "影市只在没有灯的时候开张。摊主愿意吃掉你牌组里的一张旧牌，也愿意塞给你一张新货。",
                "雾门后传来旧王的低语。你能提前拿走王座旁的遗物，但门会取走一部分生命。"
        };
        return t[Math.max(0, Math.min(t.length - 1, s.eventId))];
    }

    private String[] eventChoices() {
        if (s.eventId == 0) return new String[]{"失去8生命，获得遗物", "接受一张回声牌"};
        if (s.eventId == 1) return new String[]{"获得95金币，加入2张裂伤", "恢复16生命"};
        if (s.eventId == 2) return new String[]{"获得一张升级牌", "最大生命+6"};
        if (s.eventId == 3) return new String[]{"支付45金币，最大生命+5", "收下30金币"};
        if (s.eventId == 4) return new String[]{"升级一张牌，加入1张裂伤", "获得65金币，加入2张眩光"};
        if (s.eventId == 5) return new String[]{"失去6生命，获得随机药剂", "获得45金币"};
        if (s.eventId == 6) return new String[]{"移除一张牌，失去10生命", "获得随机牌和35金币"};
        return new String[]{"最大生命-8，获得Boss遗物", "升级两张牌，加入1张眩光"};
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void invalidateSoon() {
        long now = System.currentTimeMillis();
        if (now - lastFrame > 33) {
            lastFrame = now;
            postInvalidateDelayed(33);
        }
    }

    private void loadAtlases() {
        int[] ids = {
                getResources().getIdentifier("card_atlas", "drawable", getContext().getPackageName()),
                getResources().getIdentifier("card_atlas_1", "drawable", getContext().getPackageName()),
                getResources().getIdentifier("card_atlas_2", "drawable", getContext().getPackageName()),
                getResources().getIdentifier("card_atlas_3", "drawable", getContext().getPackageName()),
                getResources().getIdentifier("card_atlas_4", "drawable", getContext().getPackageName()),
                getResources().getIdentifier("card_atlas_5", "drawable", getContext().getPackageName()),
                getResources().getIdentifier("card_atlas_6", "drawable", getContext().getPackageName()),
                getResources().getIdentifier("card_atlas_7", "drawable", getContext().getPackageName())
        };
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] != 0) {
                atlases[i] = BitmapFactory.decodeResource(getResources(), ids[i]);
            }
        }
    }

    private GameCore.State load() {
        try {
            String raw = getContext().getSharedPreferences("abyss", Context.MODE_PRIVATE).getString(SAVE, "");
            if (raw.length() == 0) return null;
            byte[] bytes = Base64.decode(raw, Base64.DEFAULT);
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Object obj = in.readObject();
            in.close();
            if (obj instanceof GameCore.State) {
                GameCore.State st = (GameCore.State) obj;
                st.ensureRandom();
                return st;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void save() {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bout);
            out.writeObject(s);
            out.close();
            String raw = Base64.encodeToString(bout.toByteArray(), Base64.DEFAULT);
            getContext().getSharedPreferences("abyss", Context.MODE_PRIVATE).edit().putString(SAVE, raw).apply();
        } catch (Exception ignored) {
        }
    }

    private static final class Button {
        final RectF rect;
        final String label;
        final String action;
        final int index;

        Button(RectF rect, String label, String action, int index) {
            this.rect = rect;
            this.label = label;
            this.action = action;
            this.index = index;
        }
    }

    private static final class CardHit {
        final RectF rect;
        final int index;

        CardHit(RectF rect, int index) {
            this.rect = rect;
            this.index = index;
        }
    }

    private static final class EnemyHit {
        final RectF rect;
        final int index;

        EnemyHit(RectF rect, int index) {
            this.rect = rect;
            this.index = index;
        }
    }

    private static final class MapHit {
        final RectF rect;
        final int index;

        MapHit(RectF rect, int index) {
            this.rect = rect;
            this.index = index;
        }
    }
}
