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
    private GameCore.Card detailCard;
    private int deckPage;
    private int codexPage;
    private int codexTab;
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
        } else if (s.mode == GameCore.MODE_CLASS) {
            drawClasses(c);
        } else if (s.mode == GameCore.MODE_BOON) {
            drawBoons(c);
        } else if (s.mode == GameCore.MODE_PACT) {
            drawPacts(c);
        } else if (s.mode == GameCore.MODE_SKILL_SPEC) {
            drawSkillSpecs(c);
        } else if (s.mode == GameCore.MODE_TALENT) {
            drawTalents(c);
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
        if (detailCard != null) {
            drawCardDetail(c, detailCard);
        }
        invalidateSoon();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() != MotionEvent.ACTION_UP) {
            return true;
        }
        float x = e.getX();
        float y = e.getY();
        if (detailCard != null) {
            detailCard = null;
            invalidate();
            return true;
        }
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
                    if (s.deckView == 0 && s.pendingAction != null && s.pendingAction.length() > 0) {
                        GameCore.deckPick(s, h.index);
                        save();
                    } else {
                        List<GameCore.Card> list = deckList();
                        if (h.index >= 0 && h.index < list.size()) {
                            detailCard = list.get(h.index);
                        }
                    }
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
            } else if (s.profession == null || s.profession.length() == 0) {
                s.mode = GameCore.MODE_CLASS;
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
        } else if ("profession".equals(action)) {
            if (index >= 0 && index < GameCore.PROFESSIONS.length) {
                GameCore.chooseProfession(s, GameCore.PROFESSIONS[index]);
            }
        } else if ("boon".equals(action)) {
            GameCore.chooseBoon(s, index);
        } else if ("pact".equals(action)) {
            GameCore.choosePact(s, index);
        } else if ("skill_spec".equals(action)) {
            GameCore.chooseSkillSpec(s, index);
        } else if ("talent".equals(action)) {
            GameCore.chooseTalent(s, index);
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
        } else if ("profession_skill".equals(action)) {
            GameCore.useProfessionSkill(s, currentEnemyTarget());
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
        } else if ("shop_scout".equals(action)) {
            GameCore.shopScoutBuild(s);
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
        } else if ("rest_attune".equals(action)) {
            GameCore.restAttuneBuild(s);
        } else if ("event".equals(action)) {
            GameCore.eventChoose(s, index);
        } else if ("title".equals(action)) {
            s.mode = GameCore.MODE_TITLE;
        } else if ("decktab".equals(action)) {
            s.deckView = index;
            deckPage = 0;
            detailCard = null;
        } else if ("deckpage".equals(action)) {
            deckPage = Math.max(0, deckPage + index);
        } else if ("codextab".equals(action)) {
            codexTab = index;
            codexPage = 0;
        } else if ("codexpage".equals(action)) {
            codexPage = Math.max(0, codexPage + index);
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
        if (s.mode == GameCore.MODE_TITLE || s.mode == GameCore.MODE_ORIGIN || s.mode == GameCore.MODE_CLASS || s.mode == GameCore.MODE_BOON || s.mode == GameCore.MODE_PACT || s.mode == GameCore.MODE_TALENT) {
            return;
        }
        int w = getWidth();
        p.setColor(0xdd0b1018);
        c.drawRoundRect(new RectF(dp(10), dp(8), w - dp(10), dp(54)), dp(6), dp(6), p);
        drawText(c, "深渊牌旅", dp(20), dp(36), 20, 0xfff3d486, true);
        String job = s.profession == null || s.profession.length() == 0 ? "" : "  " + s.profession;
        String pact = s.pact == null || s.pact.length() == 0 ? "" : "  " + GameCore.pactProgressText(s);
        String info = "Act " + s.act + "  层 " + s.floor + "  HP " + s.hp + "/" + s.maxHp + "  金 " + s.gold + job + pact;
        drawText(c, info, dp(128), dp(36), 15, 0xffe9ddc7, false);
        addButton(w - dp(92), dp(16), dp(76), dp(30), "牌组", "deck", 0);
    }

    private void drawTitle(Canvas c) {
        int w = getWidth();
        int h = getHeight();
        drawText(c, "深渊牌旅", dp(30), h * 0.18f, 42, 0xfff6d780, true);
        drawText(c, "原创暗潮爬塔卡牌游戏", dp(32), h * 0.18f + dp(38), 17, 0xffd6cdbd, false);
        drawText(c, "四种起源乘" + GameCore.PROFESSIONS.length + "种职业，卡组、遗物、药剂与事件共同塑造每一局。", dp(32), h * 0.18f + dp(72), 15, 0xffb9c7cf, false);
        drawText(c, "旅程 " + s.meta.runs + "  胜利 " + s.meta.wins + "  最深 " + s.meta.highestFloor + "层  成就 " + s.meta.achievements.size(), dp(32), h * 0.18f + dp(100), 14, 0xffd7c994, true);
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
        drawText(c, "难度阶层：" + GameCore.depthName(s.ascension), dp(24), dp(592), 18, 0xffe9d7a1, true);
        drawWrapped(c, GameCore.depthText(s.ascension), dp(24), dp(648), w - dp(48), 12, 0xffb9c7cf);
        int[] val = {0, 3, 6, 10};
        for (int i = 0; i < val.length; i++) {
            String label = GameCore.depthName(val[i]) + (s.ascension == val[i] ? "*" : "");
            addButton(dp(24) + i * dp(86), dp(604), dp(76), dp(34), label, "depth", val[i]);
        }
    }

    private void drawClasses(Canvas c) {
        int w = getWidth();
        drawText(c, "选择职业", dp(24), dp(76), 30, 0xfff4d580, true);
        drawText(c, s.origin + " 起源会与职业被动叠加，形成不同构筑路线。", dp(26), dp(104), 15, 0xffc9d2d2, false);
        int cols = 2;
        float gap = dp(10);
        float cardW = (w - dp(48) - gap * (cols - 1)) / cols;
        String[] desc = {
                "格挡反击 / 技能充能",
                "低费连击 / 穿透追击",
                "药剂扩散 / 异常爆发",
                "束缚控制 / 群体压制",
                "消耗回流 / 临时牌循环",
                "金币运营 / 商店折扣",
                "自损裂伤 / 压血爆发",
                "预视升级 / 牌序重织",
                "灵体临牌 / 召唤控场",
                "状态诅咒 / 易伤压制",
                "升级刻印 / 异常过载",
                "低费调律 / 印记爆发",
                "目标裁令 / 誓约过载",
                "观测星轨 / 汇流循环"
        };
        int rows = (GameCore.PROFESSIONS.length + cols - 1) / cols;
        float startY = dp(126);
        float bottom = getHeight() - dp(70);
        float rowStep = Math.min(dp(106), Math.max(dp(82), (bottom - startY) / Math.max(1, rows)));
        float cardH = Math.min(dp(106), rowStep - dp(6));
        for (int i = 0; i < GameCore.PROFESSIONS.length; i++) {
            int row = i / cols;
            int col = i % cols;
            float x = dp(24) + col * (cardW + gap);
            float y = startY + row * rowStep;
            String name = GameCore.PROFESSIONS[i];
            p.setColor(0xcc121923);
            c.drawRoundRect(new RectF(x, y, x + cardW, y + cardH), dp(8), dp(8), p);
            p.setColor(GameCore.professionColor(name));
            c.drawRoundRect(new RectF(x + dp(8), y + dp(10), x + dp(50), y + dp(52)), dp(7), dp(7), p);
            drawProfessionMark(c, name, x + dp(29), y + dp(31), dp(17));
            drawText(c, name, x + dp(60), y + dp(30), 20, 0xfff5ead2, true);
            int mastery = GameCore.professionMasteryLevel(s, name);
            String progress = GameCore.professionWins(s, name) + "胜 " + GameCore.professionMasteryName(mastery) + " " + GameCore.nextProfessionMasteryText(s, name);
            drawWrapped(c, desc[i] + " / " + progress, x + dp(10), y + cardH - dp(30), cardW - dp(20), 10, mastery > 0 ? 0xfff5d276 : 0xffc5d0cf);
            addButton(x + cardW - dp(58), y + dp(16), dp(48), dp(30), "选", "profession", i);
            addButton(x, y, cardW, cardH, "", "profession", i);
        }
        addButton(dp(24), getHeight() - dp(56), dp(98), dp(38), "返回起源", "new", 0);
    }

    private void drawBoons(Canvas c) {
        int w = getWidth();
        drawText(c, "选择开局赐印", dp(24), dp(86), 30, 0xfff4d580, true);
        drawText(c, s.origin + " / " + s.profession + " / " + GameCore.depthName(s.ascension) + "阶层", dp(26), dp(116), 15, 0xffc9d2d2, false);
        drawText(c, shortText(GameCore.depthText(s.ascension), 42), dp(26), dp(136), 12, 0xffaebdc0, false);
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

    private void drawPacts(Canvas c) {
        int w = getWidth();
        drawText(c, "选择构筑誓约", dp(24), dp(86), 30, 0xfff4d580, true);
        drawText(c, s.origin + " / " + s.profession + " / 赐印已定", dp(26), dp(116), 15, 0xffc9d2d2, false);
        drawText(c, "誓约会在战斗中追踪玩法目标，达成后兑现长期奖励。", dp(26), dp(136), 12, 0xffaebdc0, false);
        for (int i = 0; i < s.pactChoices.size(); i++) {
            GameCore.PactDef pDef = GameCore.pact(s.pactChoices.get(i));
            float y = dp(160) + i * dp(122);
            p.setColor(0xcc121923);
            c.drawRoundRect(new RectF(dp(24), y, w - dp(24), y + dp(98)), dp(8), dp(8), p);
            p.setColor(0xff78c7b2);
            c.drawCircle(dp(58), y + dp(49), dp(24), p);
            drawText(c, "誓", dp(49), y + dp(57), 18, 0xff111923, true);
            drawText(c, pDef == null ? "" : pDef.name, dp(96), y + dp(36), 22, 0xfff5ead2, true);
            drawWrappedLines(c, pDef == null ? "" : pDef.text, dp(96), y + dp(58), w - dp(218), 12, 0xffc5d0cf, 2);
            addButton(w - dp(112), y + dp(32), dp(86), dp(38), "立誓", "pact", i);
        }
    }

    private void drawSkillSpecs(Canvas c) {
        int w = getWidth();
        drawText(c, "选择职业技专修", dp(24), dp(86), 30, 0xfff4d580, true);
        drawText(c, s.origin + " / " + s.profession + " / " + GameCore.pactName(s), dp(26), dp(116), 15, 0xffc9d2d2, false);
        drawText(c, "专修会改变职业技释放后的分支收益，Boss后随职业专精晋阶。", dp(26), dp(136), 12, 0xffaebdc0, false);
        for (int i = 0; i < s.skillSpecChoices.size(); i++) {
            GameCore.SkillSpecDef spec = GameCore.skillSpec(s.skillSpecChoices.get(i));
            float y = dp(160) + i * dp(122);
            p.setColor(0xcc121923);
            c.drawRoundRect(new RectF(dp(24), y, w - dp(24), y + dp(98)), dp(8), dp(8), p);
            p.setColor(0xff9fd5c5);
            c.drawCircle(dp(58), y + dp(49), dp(24), p);
            drawProfessionMark(c, s.profession, dp(58), y + dp(49), dp(15));
            drawText(c, spec == null ? "" : spec.name, dp(96), y + dp(36), 22, 0xfff5ead2, true);
            drawWrappedLines(c, spec == null ? "" : spec.text, dp(96), y + dp(58), w - dp(218), 12, 0xffc5d0cf, 2);
            addButton(w - dp(112), y + dp(32), dp(86), dp(38), "专修", "skill_spec", i);
        }
    }

    private void drawTalents(Canvas c) {
        int w = getWidth();
        drawText(c, "选择职业专精", dp(24), dp(86), 30, 0xfff4d580, true);
        drawText(c, s.origin + " / " + s.profession + " / 已领悟 " + s.talents.size() + " 项", dp(26), dp(116), 15, 0xffc9d2d2, false);
        for (int i = 0; i < s.talentChoices.size(); i++) {
            GameCore.TalentDef t = GameCore.talent(s.talentChoices.get(i));
            float y = dp(154) + i * dp(128);
            String prof = t == null ? "" : t.profession;
            int color = prof == null || prof.length() == 0 ? 0xffd9b85f : GameCore.professionColor(prof);
            p.setColor(0xcc121923);
            c.drawRoundRect(new RectF(dp(24), y, w - dp(24), y + dp(104)), dp(8), dp(8), p);
            p.setColor(color);
            c.drawCircle(dp(58), y + dp(52), dp(25), p);
            if (prof != null && prof.length() > 0) {
                drawProfessionMark(c, prof, dp(58), y + dp(52), dp(17));
            } else {
                drawText(c, "专", dp(49), y + dp(59), 18, 0xff16130d, true);
            }
            drawText(c, t == null ? "" : t.name, dp(96), y + dp(34), 22, 0xfff5ead2, true);
            String hint = t == null ? "" : GameCore.talentSynergyHint(s, t.id);
            drawText(c, fitText(hint, 11, w - dp(218)), dp(96), y + dp(52), 11, 0xff9fd5c5, true);
            drawWrappedLines(c, t == null ? "" : t.text, dp(96), y + dp(70), w - dp(218), 12, 0xffc5d0cf, 2);
            addButton(w - dp(112), y + dp(34), dp(86), dp(38), "领悟", "talent", i);
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
            if (n.route != GameCore.ROUTE_NONE) {
                p.setColor(GameCore.routeColor(n.route));
                c.drawCircle(x + dp(15), y - dp(15), dp(9), p);
                drawText(c, GameCore.routeShort(n.route), x + dp(10), y - dp(11), 10, 0xff111419, true);
            }
            RectF r = new RectF(x - dp(24), y - dp(24), x + dp(24), y + dp(24));
            mapHits.add(new MapHit(r, i));
        }
        String hint = s.relics.contains("night_map") ? "夜航图生效：下一层任意节点均可选择。" : "可选节点以金色显示。分支路线会改变资源、风险与奖励密度。";
        drawText(c, hint, dp(24), h - dp(92), 14, 0xffc9d2d2, false);
        drawRoutePreview(c, dp(24), h - dp(70), w - dp(48));
    }

    private void drawRoutePreview(Canvas c, float x, float y, float width) {
        ArrayList<GameCore.MapNode> choices = new ArrayList<>();
        for (GameCore.MapNode n : s.map) {
            boolean nightReachable = s.relics.contains("night_map") && n.floor == s.floor + 1;
            if (n.available || nightReachable) {
                choices.add(n);
            }
        }
        if (choices.isEmpty()) {
            return;
        }
        int shown = Math.min(3, choices.size());
        float gap = dp(6);
        float itemW = (width - gap * (shown - 1)) / shown;
        for (int i = 0; i < shown; i++) {
            GameCore.MapNode n = choices.get(i);
            float ix = x + i * (itemW + gap);
            RectF r = new RectF(ix, y, ix + itemW, y + dp(50));
            p.setColor(0xaa151c24);
            c.drawRoundRect(r, dp(6), dp(6), p);
            int route = n.route;
            p.setColor(GameCore.routeColor(route));
            c.drawCircle(ix + dp(17), y + dp(17), dp(9), p);
            String title = GameCore.nodeName(n.type) + " " + (route == GameCore.ROUTE_NONE ? "普通" : GameCore.routeName(route));
            drawText(c, title, ix + dp(32), y + dp(18), 12, 0xfff0e2c8, true);
            String text = GameCore.routeText(route);
            drawText(c, text.length() > 18 ? text.substring(0, 18) : text, ix + dp(10), y + dp(38), 10, 0xffaebdc0, false);
        }
    }

    private void drawCombat(Canvas c) {
        int w = getWidth();
        int h = getHeight();
        drawText(c, "能量 " + s.energy + "   格挡 " + s.block + "   回合 " + s.turn, dp(22), dp(82), 22, 0xfff2d373, true);
        String job = s.profession == null || s.profession.length() == 0 ? "未定" : s.profession;
        int overload = GameCore.professionSkillOverload(s);
        String engines = job + "  " + GameCore.skillSpecName(s) + "  专精 " + s.talents.size() + "  职技 " + GameCore.professionSkillName(s.profession)
                + " " + s.professionSkillCharge + "/" + GameCore.PROF_SKILL_MAX
                + (overload > 0 ? " 过载+" + overload : "") + (GameCore.professionSkillReady(s) ? " 可释放" : "");
        drawText(c, fitText(engines, 13, w - dp(44)), dp(22), dp(104), 13, GameCore.professionSkillReady(s) ? 0xfff5d276 : 0xffd6dfda, true);
        String powers = "被动 " + s.professionCharge + "  守势 " + s.steelEngine + "  热度 " + s.ashEngine + "  再生 " + s.wildEngine + "  回声势 " + s.voidEngine;
        if (s.burnPower > 0 || s.bindPower > 0) {
            powers += "  燃势 " + s.burnPower + "  束缚势 " + s.bindPower;
        }
        drawText(c, powers, dp(22), dp(122), 12, 0xffbfcfcc, false);
        String skillTip = GameCore.professionSkillText(s);
        drawText(c, fitText(skillTip, 12, w - dp(44)), dp(22), dp(140), 12, 0xffaebdc0, false);
        String resonance = GameCore.buildResonanceText(s);
        String buildLine = "构筑 " + GameCore.buildSummaryText(s) + (resonance.length() > 0 ? "  " + resonance : "");
        drawText(c, shortText(buildLine, 56), dp(22), dp(158), 12, 0xff9fd5c5, true);
        String context = "";
        if (s.encounterModifier != GameCore.MOD_NONE) {
            context += "词缀 " + GameCore.modifierName(s.encounterModifier);
        }
        if (s.currentRoute != GameCore.ROUTE_NONE) {
            context += (context.length() > 0 ? "  " : "") + "路线 " + GameCore.routeName(s.currentRoute);
        }
        if (s.pact != null && s.pact.length() > 0) {
            context += (context.length() > 0 ? "  " : "") + GameCore.pactProgressText(s);
        }
        if (s.combatQuest != GameCore.QUEST_NONE) {
            context += (context.length() > 0 ? "  " : "") + "目标 " + GameCore.questName(s.combatQuest) + " " + questProgressText();
        }
        String milestone = GameCore.milestoneProgressText(s);
        if (milestone.length() > 0) {
            context += (context.length() > 0 ? "  " : "") + milestone;
        }
        if (context.length() > 0) {
            drawText(c, shortText(context, 52), dp(22), dp(176), 12, s.questComplete ? 0xfff5d276 : 0xffc9d7d3, true);
        }
        if (s.vulnerable > 0) {
            drawText(c, "易伤 " + s.vulnerable, dp(22), dp(194), 15, 0xffff8d75, true);
        }
        float enemyTop = dp(172);
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
        String skill = GameCore.professionSkillName(s.profession);
        String skillLabel = skill + " " + s.professionSkillCharge + "/" + GameCore.PROF_SKILL_MAX;
        if (GameCore.professionSkillReady(s)) {
            skillLabel = overload > 0 ? skill + "+" + overload : skill + "!";
        } else if (s.professionSkillUsedThisTurn) {
            skillLabel = skill + "*";
        }
        addButton(w - dp(224), h - dp(52), dp(104), dp(40), skillLabel, "profession_skill", 0);
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
        int attackPreview = Math.max(0, e.intentValue + e.strength + e.mark - (e.bind > 0 ? 3 : 0));
        if (s.vulnerable > 0) {
            attackPreview = Math.round(attackPreview * 1.35f);
        }
        String intent = "攻 " + attackPreview;
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
        String mechanics = GameCore.enemyMechanicText(e);
        if (mechanics.length() > 0) {
            tags += (tags.length() > 0 ? " " : "") + mechanics;
        }
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
        boolean shopScout = GameCore.shopScoutDraftActive(s);
        boolean restAttune = GameCore.restAttuneDraftActive(s);
        drawText(c, shopScout ? "商栈寻路" : restAttune ? "营地调校" : "战利品", dp(24), dp(88), 30, 0xfff4d580, true);
        if (!shopScout && !restAttune && s.encounterModifier != GameCore.MOD_NONE) {
            drawText(c, "本场词缀：" + GameCore.modifierName(s.encounterModifier), dp(126), dp(88), 14, 0xffc7d4d0, false);
        }
        int rewardCards = Math.max(1, s.cardRewards.size());
        float cardW = Math.min(dp(126), (w - dp(44)) / rewardCards - dp(8));
        float cardH = cardW * 1.42f;
        if (s.cardRewards.isEmpty()) {
            String status = s.cardRewardSkipped ? "卡牌奖励已跳过" : "没有卡牌奖励";
            drawText(c, status, dp(24), dp(136), 17, 0xffc9d2d2, true);
        } else if (shopScout) {
            drawText(c, "选择一张定向补强牌，拿完返回商店", dp(24), dp(112), 13, 0xffc9d2d2, false);
        } else if (restAttune) {
            drawText(c, "选择一张营地调校牌，拿完继续路线", dp(24), dp(112), 13, 0xffc9d2d2, false);
        } else {
            drawText(c, "选择一张卡牌，或跳过获得金币", dp(24), dp(112), 13, 0xffc9d2d2, false);
        }
        for (int i = 0; i < s.cardRewards.size(); i++) {
            GameCore.Card card = new GameCore.Card(s.cardRewards.get(i).id);
            RectF r = new RectF(dp(22) + i * (cardW + dp(8)), dp(120), dp(22) + i * (cardW + dp(8)) + cardW, dp(120) + cardH);
            drawCard(c, card, r, false);
            String hint = s.cardRewards.get(i).hint == null ? "" : s.cardRewards.get(i).hint;
            if (hint.length() > 0) {
                drawWrapped(c, shortText(hint, 22), r.left, r.bottom + dp(15), r.width(), 10, 0xff9fd5c5);
            }
            addButton(r.left, r.bottom + dp(28), r.width(), dp(34), "收下", "reward_card", i);
        }
        float y = dp(120) + cardH + dp(94);
        if (!s.relicRewards.isEmpty()) {
            drawText(c, s.combatKind == 'B' ? "Boss 遗物选择" : "遗物", dp(24), y - dp(12), 17, 0xffe9d7a1, true);
        }
        for (int i = 0; i < s.relicRewards.size(); i++) {
            GameCore.RelicDef r = GameCore.relic(s.relicRewards.get(i));
            drawRelicRow(c, r, dp(24), y + i * dp(70), w - dp(48));
            addButton(w - dp(104), y + i * dp(70) + dp(14), dp(82), dp(36), "拿取", "reward_relic", i);
        }
        if (!shopScout && !restAttune && !s.cardRewards.isEmpty() && !s.cardRewardSkipped) {
            int gold = s.relics.contains("cracked_compass") ? 14 : 10;
            addButton(dp(24), getHeight() - dp(62), w - dp(48), dp(42), "跳过卡牌奖励 +" + gold + "金", "skip", 0);
        }
    }

    private void drawShop(Canvas c) {
        int w = getWidth();
        drawText(c, "裂隙商栈", dp(24), dp(86), 30, 0xfff4d580, true);
        drawText(c, "金币 " + s.gold, w - dp(118), dp(84), 20, 0xffffdb7a, true);
        if (GameCore.PROF_MERCHANT.equals(s.profession)) {
            drawText(c, "行商折扣生效", dp(24), dp(106), 13, 0xffffdf86, true);
        }
        drawText(c, "药剂 " + s.potions.size() + "/" + GameCore.potionLimit(s), dp(132), dp(106), 13, 0xffc9d2d2, true);
        drawText(c, shortText("当前构筑 " + GameCore.buildSummaryText(s), 46), dp(24), dp(124), 12, 0xff9fd5c5, true);
        float cardW = Math.min(dp(104), (w - dp(34)) / 5f - dp(6));
        float cardH = cardW * 1.42f;
        for (int i = 0; i < s.shopCards.size(); i++) {
            GameCore.Card card = new GameCore.Card(s.shopCards.get(i));
            RectF r = new RectF(dp(18) + i * (cardW + dp(6)), dp(134), dp(18) + i * (cardW + dp(6)) + cardW, dp(134) + cardH);
            drawCard(c, card, r, false);
            int price = GameCore.shopCardPrice(s, GameCore.card(card.id));
            addButton(r.left, r.bottom + dp(6), r.width(), dp(30), price + "金", "shop_card", i);
        }
        float y = dp(134) + cardH + dp(50);
        drawText(c, "遗物", dp(24), y, 18, 0xffe9d7a1, true);
        for (int i = 0; i < s.shopRelics.size(); i++) {
            GameCore.RelicDef r = GameCore.relic(s.shopRelics.get(i));
            drawRelicRow(c, r, dp(24), y + dp(18) + i * dp(60), w - dp(48));
            addButton(w - dp(96), y + dp(30) + i * dp(60), dp(74), dp(32), GameCore.shopRelicPrice(s) + "金", "shop_relic", i);
        }
        float py = y + dp(202);
        drawText(c, "药剂", dp(24), py, 18, 0xffe9d7a1, true);
        for (int i = 0; i < s.shopPotions.size(); i++) {
            GameCore.PotionDef po = GameCore.potion(s.shopPotions.get(i));
            addButton(dp(24) + i * dp(108), py + dp(18), dp(98), dp(36), po.name + " " + GameCore.shopPotionPrice(s), "shop_potion", i);
        }
        float sy = py + dp(76);
        addButton(dp(24), sy, dp(98), dp(40), "删牌" + GameCore.shopServicePrice(s, "shop_remove"), "shop_remove", 0);
        addButton(dp(132), sy, dp(98), dp(40), "升级" + GameCore.shopServicePrice(s, "shop_upgrade"), "shop_upgrade", 0);
        addButton(dp(240), sy, dp(108), dp(40), "转化" + GameCore.shopServicePrice(s, "shop_transform"), "shop_transform", 0);
        drawText(c, GameCore.shopScoutText(s), dp(24), sy + dp(66), 12, 0xffa9c9c2, false);
        if (s.shopScoutUsed) {
            drawText(c, "商栈寻路已使用", dp(24), sy + dp(100), 15, 0xff8faaa5, true);
        } else {
            addButton(dp(24), sy + dp(76), w - dp(48), dp(38), "商栈寻路 " + GameCore.shopServicePrice(s, "shop_scout") + "金", "shop_scout", 0);
        }
        addButton(dp(24), getHeight() - dp(58), w - dp(48), dp(42), "离开商店", "leave_shop", 0);
    }

    private void drawRest(Canvas c) {
        int w = getWidth();
        drawText(c, "静火营地", dp(24), dp(88), 30, 0xfff4d580, true);
        drawText(c, "火光不问你来处，只问你下一层要付出什么。", dp(24), dp(120), 15, 0xffc8d2ce, false);
        drawText(c, shortText("当前构筑 " + GameCore.buildSummaryText(s), 50), dp(24), dp(144), 13, 0xff9fd5c5, true);
        addButton(dp(28), dp(166), w - dp(56), dp(48), "休整：恢复" + GameCore.restHealAmount(s) + "生命", "rest_heal", 0);
        addButton(dp(28), dp(224), w - dp(56), dp(48), "锻造：升级一张牌", "rest_upgrade", 0);
        addButton(dp(28), dp(282), w - dp(56), dp(48), "净化：移除一张牌", "rest_remove", 0);
        addButton(dp(28), dp(340), w - dp(56), dp(48), "转化：重塑一张牌", "rest_transform", 0);
        addButton(dp(28), dp(398), w - dp(56), dp(48), "调校：构筑补强三选一", "rest_attune", 0);
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
        int rows = Math.max(1, (int) ((getHeight() - dp(238)) / Math.max(1, cardH + dp(12))));
        int perPage = Math.max(3, cols * rows);
        int maxPage = Math.max(0, (list.size() - 1) / perPage);
        deckPage = Math.max(0, Math.min(deckPage, maxPage));
        int startIndex = deckPage * perPage;
        int end = Math.min(list.size(), startIndex + perPage);
        drawText(c, "第 " + (deckPage + 1) + "/" + (maxPage + 1) + " 页  共 " + list.size() + " 张", dp(24), dp(144), 13, 0xffc9d2d2, true);
        if (s.deckView == 0) {
            drawText(c, shortText("构筑 " + GameCore.buildSummaryText(s), 54), dp(24), dp(162), 13, 0xff9fd5c5, true);
            drawText(c, shortText(GameCore.buildSummaryDetail(s), 58), dp(24), dp(180), 11, 0xffaebdc0, false);
        }
        for (int i = startIndex; i < end; i++) {
            int local = i - startIndex;
            int row = local / cols;
            int col = local % cols;
            float x = dp(18) + col * (cardW + dp(8));
            float y = dp(s.deckView == 0 ? 198 : 162) + row * (cardH + dp(12));
            RectF r = new RectF(x, y, x + cardW, y + cardH);
            drawCard(c, list.get(i), r, false);
            cardHits.add(new CardHit(r, i));
        }
        addButton(dp(18), getHeight() - dp(52), dp(74), dp(34), "上一页", "deckpage", -1);
        addButton(dp(102), getHeight() - dp(52), dp(74), dp(34), "下一页", "deckpage", 1);
        addButton(w - dp(94), dp(102), dp(76), dp(34), "返回", "close", 0);
    }

    private void drawCodex(Canvas c) {
        int w = getWidth();
        drawText(c, "图鉴", dp(24), dp(86), 30, 0xfff4d580, true);
        drawText(c, "卡牌 " + GameCore.CARD_LIBRARY.size() + "   遗物 " + GameCore.RELIC_LIBRARY.size() + "   药剂 " + GameCore.POTION_LIBRARY.size() + "   专精 " + GameCore.TALENT_LIBRARY.size() + "   誓约 " + GameCore.PACT_LIBRARY.size(), dp(24), dp(118), 16, 0xffc9d2d2, false);
        drawText(c, "记录：旅程 " + s.meta.runs + " / 胜利 " + s.meta.wins + " / 目标 " + s.meta.questCompletions + " / 最大金币 " + s.meta.maxGold, dp(24), dp(142), 13, 0xffd7c994, true);
        String[] tabs = {"卡牌", "遗物", "专精", "誓约", "职业", "成就"};
        for (int i = 0; i < tabs.length; i++) {
            addButton(dp(18) + i * dp(61), dp(154), dp(56), dp(32), tabs[i] + (codexTab == i ? "*" : ""), "codextab", i);
        }
        drawCodexList(c, dp(24), dp(202), w - dp(48));
        addButton(dp(24), getHeight() - dp(52), dp(74), dp(34), "上一页", "codexpage", -1);
        addButton(dp(108), getHeight() - dp(52), dp(74), dp(34), "下一页", "codexpage", 1);
        addButton(w - dp(94), dp(102), dp(76), dp(34), "返回", "close", 0);
    }

    private void drawCodexList(Canvas c, float x, float y, float width) {
        int lines = Math.max(5, (int) ((getHeight() - y - dp(74)) / dp(34)));
        int total = codexTotal();
        int maxPage = Math.max(0, (total - 1) / lines);
        codexPage = Math.max(0, Math.min(codexPage, maxPage));
        drawText(c, "第 " + (codexPage + 1) + "/" + (maxPage + 1) + " 页", x, y - dp(10), 13, 0xffc9d2d2, true);
        int start = codexPage * lines;
        int end = Math.min(total, start + lines);
        for (int i = start; i < end; i++) {
            float rowY = y + (i - start) * dp(34);
            if (codexTab == 0) {
                GameCore.CardDef d = GameCore.CARD_LIBRARY.get(i);
                String prof = d.profession.length() > 0 ? " / " + d.profession : "";
                drawText(c, d.name + " / " + d.origin + prof + " / " + rarity(d.rarity), x, rowY, 14, 0xfff0e2c8, true);
                drawText(c, d.text, x, rowY + dp(17), 11, 0xffaebdc0, false);
            } else if (codexTab == 1) {
                GameCore.RelicDef r = GameCore.RELIC_LIBRARY.get(i);
                drawText(c, r.name + (r.boss ? " / Boss" : ""), x, rowY, 14, 0xfff0e2c8, true);
                drawText(c, r.text, x, rowY + dp(17), 11, 0xffaebdc0, false);
            } else if (codexTab == 2) {
                GameCore.TalentDef t = GameCore.TALENT_LIBRARY.get(i);
                String prof = t.profession.length() == 0 ? "通用" : t.profession;
                drawText(c, t.name + " / " + prof, x, rowY, 14, 0xfff0e2c8, true);
                drawText(c, t.text, x, rowY + dp(17), 11, 0xffaebdc0, false);
            } else if (codexTab == 3) {
                GameCore.PactDef pDef = GameCore.PACT_LIBRARY.get(i);
                drawText(c, pDef.name, x, rowY, 14, 0xfff0e2c8, true);
                drawText(c, pDef.text, x, rowY + dp(17), 11, 0xffaebdc0, false);
            } else if (codexTab == 4) {
                String prof = GameCore.PROFESSIONS[i];
                String title = prof + " / " + GameCore.professionSkillName(prof) + " / 胜" + GameCore.professionWins(s, prof)
                        + " / " + GameCore.professionMasteryName(GameCore.professionMasteryLevel(s, prof));
                drawText(c, title, x, rowY, 14, GameCore.professionColor(prof), true);
                drawText(c, shortText(GameCore.professionMasteryText(s, prof), 52), x, rowY + dp(17), 11, 0xffaebdc0, false);
            } else {
                String id = achievementId(i);
                String mark = GameCore.hasAchievement(s, id) ? "*" : "-";
                drawText(c, mark + " " + GameCore.achievementName(id), x, rowY, 14, GameCore.hasAchievement(s, id) ? 0xfff5d276 : 0xff87929a, true);
            }
        }
    }

    private int codexTotal() {
        if (codexTab == 0) return GameCore.CARD_LIBRARY.size();
        if (codexTab == 1) return GameCore.RELIC_LIBRARY.size();
        if (codexTab == 2) return GameCore.TALENT_LIBRARY.size();
        if (codexTab == 3) return GameCore.PACT_LIBRARY.size();
        if (codexTab == 4) return GameCore.PROFESSIONS.length;
        return achievementCount();
    }

    private int achievementCount() {
        return 11;
    }

    private String achievementId(int index) {
        String[] ids = {"first_run", "first_win", "all_professions", "profession_adept", "profession_master", "all_mastery", "collector", "high_depth", "talent_master", "rich", "quest_hunter"};
        return ids[Math.max(0, Math.min(ids.length - 1, index))];
    }

    private void drawEnd(Canvas c, boolean victory) {
        int w = getWidth();
        int h = getHeight();
        drawText(c, victory ? "抵达无光尽头" : "旅程终止", dp(30), h * 0.25f, 34, victory ? 0xfff6d780 : 0xffff8d75, true);
        drawText(c, s.origin + " " + s.profession + " / Act " + s.act + " / 层 " + s.floor + " / 牌组 " + s.deck.size() + " 张 / 遗物 " + s.relics.size(), dp(32), h * 0.25f + dp(44), 16, 0xffd8ded8, false);
        drawText(c, s.lastRunSummary, dp(32), h * 0.25f + dp(68), 14, 0xffd7c994, true);
        drawText(c, "历史：旅程 " + s.meta.runs + "  胜利 " + s.meta.wins + "  最深 " + s.meta.highestFloor + "层  目标 " + s.meta.questCompletions, dp(32), h * 0.25f + dp(92), 13, 0xffc3d0cc, false);
        float ay = h * 0.25f + dp(122);
        if (!s.newAchievements.isEmpty()) {
            drawText(c, "新成就", dp(32), ay, 17, 0xfff0d486, true);
            for (int i = 0; i < Math.min(4, s.newAchievements.size()); i++) {
                drawText(c, GameCore.achievementName(s.newAchievements.get(i)), dp(44), ay + dp(24) + i * dp(20), 14, 0xfff5d276, true);
            }
        } else {
            drawText(c, "已解锁成就 " + s.meta.achievements.size(), dp(32), ay, 15, 0xffc3d0cc, true);
        }
        addButton(dp(30), h * 0.57f, w - dp(60), dp(52), "再来一局", "new", 0);
        addButton(dp(30), h * 0.66f, w - dp(60), dp(46), "返回标题", "title", 0);
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
        p.setColor(d.profession.length() > 0 ? GameCore.professionColor(d.profession) : GameCore.originColor(d.origin));
        c.drawRoundRect(new RectF(inner.left, inner.top, inner.right, inner.top + dp(20)), dp(5), dp(5), p);
        drawText(c, d.name + (card.upgraded ? "+" : ""), inner.left + dp(5), inner.top + dp(15), Math.max(10, inner.width() / 8.2f), 0xff121416, true);
        p.setColor(0xffe8d27a);
        c.drawCircle(inner.left + dp(14), inner.top + dp(32), dp(12), p);
        int cost = card.upgraded ? Math.max(0, d.cost - d.upgradeCostDrop) : d.cost;
        drawText(c, String.valueOf(cost), inner.left + dp(10), inner.top + dp(37), 14, 0xff1b150b, true);
        drawWrapped(c, GameCore.cardText(card), inner.left + dp(7), inner.top + inner.height() * 0.62f, inner.width() - dp(14), Math.max(9, inner.width() / 10.5f), 0xfff3ead7);
        String tag = d.profession.length() > 0 ? d.profession + " " + rarity(d.rarity) : rarity(d.rarity);
        String compactTags = compactCardBuildTags(card, d);
        if (compactTags.length() > 0) {
            tag += " " + compactTags;
        }
        drawText(c, shortText(tag, 13), inner.left + dp(7), inner.bottom - dp(6), 10, 0xffc8d0d0, false);
    }

    private void drawCardDetail(Canvas c, GameCore.Card card) {
        GameCore.CardDef d = GameCore.card(card.id);
        if (d == null) return;
        int w = getWidth();
        int h = getHeight();
        p.setColor(0xdd05080d);
        c.drawRect(0, 0, w, h, p);
        float panelW = Math.min(dp(360), w - dp(36));
        float panelH = Math.min(dp(560), h - dp(60));
        float left = (w - panelW) / 2f;
        float top = (h - panelH) / 2f;
        RectF panel = new RectF(left, top, left + panelW, top + panelH);
        p.setColor(0xff121923);
        c.drawRoundRect(panel, dp(8), dp(8), p);
        p.setColor(d.profession.length() > 0 ? GameCore.professionColor(d.profession) : GameCore.originColor(d.origin));
        c.drawRoundRect(new RectF(panel.left, panel.top, panel.right, panel.top + dp(42)), dp(8), dp(8), p);
        drawText(c, d.name + (card.upgraded ? "+" : ""), panel.left + dp(16), panel.top + dp(28), 24, 0xff15120c, true);
        drawText(c, "费用 " + GameCore.costOf(s, card, d), panel.right - dp(72), panel.top + dp(28), 18, 0xff15120c, true);

        RectF art = new RectF(panel.left + dp(18), panel.top + dp(58), panel.right - dp(18), panel.top + dp(200));
        drawCardArt(c, d, art);
        float y = panel.top + dp(226);
        drawText(c, "类型 " + cardType(d) + "   稀有度 " + rarity(d.rarity), panel.left + dp(18), y, 15, 0xfff0d486, true);
        y += dp(26);
        String origin = d.profession.length() > 0 ? d.profession + "职业牌" : d.origin;
        drawText(c, "派系 " + origin + (card.temp ? "   临时" : ""), panel.left + dp(18), y, 14, 0xffc8d0d0, false);
        y += dp(24);
        String buildTags = cardBuildTags(card, d);
        if (buildTags.length() > 0) {
            drawText(c, "构筑 " + buildTags, panel.left + dp(18), y, 13, 0xff9fd5c5, false);
            y += dp(24);
        } else {
            y += dp(8);
        }
        drawWrapped(c, GameCore.cardText(card), panel.left + dp(18), y, panelW - dp(36), 15, 0xfff3ead7);
        y += dp(96);
        if (!card.upgraded && d.upText != null && d.upText.length() > 0 && !d.upText.equals(d.text)) {
            drawText(c, "升级后", panel.left + dp(18), y, 15, 0xfff0d486, true);
            drawWrapped(c, d.upText, panel.left + dp(18), y + dp(22), panelW - dp(36), 13, 0xffb8c6c5);
        }
        drawCentered(c, "点任意位置关闭", new RectF(panel.left, panel.bottom - dp(42), panel.right, panel.bottom - dp(12)), 13, 0xff9fb0ba, true);
    }

    private String cardType(GameCore.CardDef d) {
        if (d.type == 0) return "攻击";
        if (d.type == 1) return "技能";
        if (d.type == 2) return "能力";
        return "状态";
    }

    private String cardBuildTags(GameCore.Card card, GameCore.CardDef d) {
        String tags = "";
        if (d.skillChargeGain > 0) tags = appendTag(tags, "过载");
        if (d.createEcho || d.exhaust || card.temp) tags = appendTag(tags, "回声");
        if (d.createPotion) tags = appendTag(tags, "炼调");
        if (d.goldGain > 0 || d.goldDamage || d.goldBlock) tags = appendTag(tags, "金币");
        if (d.hpLoss > 0 || d.createWound || "wound".equals(card.id)) tags = appendTag(tags, "血契");
        if (d.upgradeRandom || d.scry > 0 || card.upgraded) tags = appendTag(tags, "工坊");
        if (d.burn > 0 || d.bind > 0 || d.vulnerable > 0 || d.addStatusToEnemy || d.spreadStatus) tags = appendTag(tags, "异常");
        if (d.draw > 0 || d.energyGain > 0) tags = appendTag(tags, "循环");
        return tags;
    }

    private String compactCardBuildTags(GameCore.Card card, GameCore.CardDef d) {
        String tags = "";
        if (d.skillChargeGain > 0) tags = appendCompactTag(tags, "载");
        if (d.createEcho || d.exhaust || card.temp) tags = appendCompactTag(tags, "回");
        if (d.createPotion) tags = appendCompactTag(tags, "炼");
        if (d.goldGain > 0 || d.goldDamage || d.goldBlock) tags = appendCompactTag(tags, "金");
        if (d.hpLoss > 0 || d.createWound || "wound".equals(card.id)) tags = appendCompactTag(tags, "血");
        if (d.upgradeRandom || d.scry > 0) tags = appendCompactTag(tags, "锻");
        if (d.burn > 0 || d.bind > 0 || d.vulnerable > 0 || d.addStatusToEnemy || d.spreadStatus) tags = appendCompactTag(tags, "异");
        if (d.draw > 0 || d.energyGain > 0) tags = appendCompactTag(tags, "循");
        return tags;
    }

    private String appendTag(String tags, String tag) {
        return tags.length() == 0 ? tag : tags + " / " + tag;
    }

    private String appendCompactTag(String tags, String tag) {
        return tags.length() == 0 ? tag : tags + "/" + tag;
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
        int color = d.profession.length() > 0 ? GameCore.professionColor(d.profession) : GameCore.originColor(d.origin);
        p.setShader(new LinearGradient(r.left, r.top, r.right, r.bottom, color, 0xff111721, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, dp(4), dp(4), p);
        p.setShader(null);
        if (d.profession.length() > 0) {
            drawProfessionMark(c, d.profession, r.centerX(), r.centerY(), r.width() * 0.22f);
        } else {
            drawSigil(c, d.origin, r.centerX(), r.centerY(), r.width() * 0.22f);
        }
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

    private void drawProfessionMark(Canvas c, String profession, float cx, float cy, float size) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2, size / 7));
        p.setColor(0xee11151b);
        if (GameCore.PROF_WARDEN.equals(profession)) {
            Path shield = new Path();
            shield.moveTo(cx, cy - size);
            shield.lineTo(cx + size, cy - size * 0.35f);
            shield.lineTo(cx + size * 0.7f, cy + size);
            shield.lineTo(cx, cy + size * 1.2f);
            shield.lineTo(cx - size * 0.7f, cy + size);
            shield.lineTo(cx - size, cy - size * 0.35f);
            shield.close();
            c.drawPath(shield, p);
        } else if (GameCore.PROF_DUELIST.equals(profession)) {
            c.drawLine(cx - size, cy + size, cx + size, cy - size, p);
            c.drawLine(cx - size * 0.45f, cy + size, cx + size, cy - size * 0.45f, p);
        } else if (GameCore.PROF_ALCHEMIST.equals(profession)) {
            c.drawCircle(cx, cy + size * 0.15f, size * 0.72f, p);
            c.drawLine(cx - size * 0.35f, cy - size, cx + size * 0.35f, cy - size, p);
            c.drawLine(cx - size * 0.22f, cy - size, cx - size * 0.22f, cy - size * 0.45f, p);
            c.drawLine(cx + size * 0.22f, cy - size, cx + size * 0.22f, cy - size * 0.45f, p);
        } else if (GameCore.PROF_RANGER.equals(profession)) {
            c.drawOval(new RectF(cx - size, cy - size * 0.9f, cx + size, cy + size * 0.9f), p);
            c.drawLine(cx - size, cy, cx + size, cy, p);
            c.drawLine(cx, cy - size, cx, cy + size, p);
        } else if (GameCore.PROF_ARCANIST.equals(profession)) {
            c.drawCircle(cx, cy, size, p);
            c.drawCircle(cx, cy, size * 0.45f, p);
            c.drawLine(cx - size, cy, cx + size, cy, p);
        } else if (GameCore.PROF_MERCHANT.equals(profession)) {
            c.drawCircle(cx, cy, size * 0.9f, p);
            c.drawLine(cx - size * 0.55f, cy - size * 0.2f, cx + size * 0.55f, cy - size * 0.2f, p);
            c.drawLine(cx - size * 0.55f, cy + size * 0.25f, cx + size * 0.55f, cy + size * 0.25f, p);
        } else if (GameCore.PROF_BLOODBOUND.equals(profession)) {
            Path drop = new Path();
            drop.moveTo(cx, cy - size);
            drop.cubicTo(cx + size * 0.8f, cy - size * 0.15f, cx + size * 0.45f, cy + size, cx, cy + size);
            drop.cubicTo(cx - size * 0.45f, cy + size, cx - size * 0.8f, cy - size * 0.15f, cx, cy - size);
            c.drawPath(drop, p);
            c.drawLine(cx - size * 0.45f, cy + size * 0.15f, cx + size * 0.45f, cy - size * 0.15f, p);
        } else if (GameCore.PROF_SUMMONER.equals(profession)) {
            c.drawCircle(cx, cy, size * 0.75f, p);
            c.drawCircle(cx - size * 0.72f, cy + size * 0.28f, size * 0.32f, p);
            c.drawCircle(cx + size * 0.72f, cy - size * 0.28f, size * 0.32f, p);
            c.drawLine(cx - size * 0.45f, cy + size * 0.08f, cx + size * 0.45f, cy - size * 0.08f, p);
        } else if (GameCore.PROF_HEXER.equals(profession)) {
            c.drawCircle(cx, cy, size, p);
            c.drawLine(cx, cy - size, cx, cy + size, p);
            c.drawLine(cx - size * 0.85f, cy - size * 0.45f, cx + size * 0.85f, cy + size * 0.45f, p);
            c.drawLine(cx - size * 0.85f, cy + size * 0.45f, cx + size * 0.85f, cy - size * 0.45f, p);
        } else if (GameCore.PROF_INSCRIBER.equals(profession)) {
            c.drawRect(cx - size * 0.72f, cy - size * 0.86f, cx + size * 0.72f, cy + size * 0.86f, p);
            c.drawLine(cx - size * 0.45f, cy - size * 0.42f, cx + size * 0.45f, cy - size * 0.42f, p);
            c.drawLine(cx - size * 0.45f, cy, cx + size * 0.45f, cy, p);
            c.drawLine(cx - size * 0.45f, cy + size * 0.42f, cx + size * 0.45f, cy + size * 0.42f, p);
            c.drawLine(cx + size * 0.15f, cy - size * 0.86f, cx - size * 0.48f, cy + size * 0.86f, p);
        } else if (GameCore.PROF_TUNER.equals(profession)) {
            c.drawCircle(cx, cy, size * 0.95f, p);
            c.drawLine(cx - size * 0.8f, cy, cx + size * 0.8f, cy, p);
            c.drawLine(cx - size * 0.45f, cy - size * 0.55f, cx + size * 0.45f, cy + size * 0.55f, p);
            c.drawLine(cx - size * 0.45f, cy + size * 0.55f, cx + size * 0.45f, cy - size * 0.55f, p);
            c.drawCircle(cx, cy, size * 0.24f, p);
        } else if (GameCore.PROF_ADJUDICATOR.equals(profession)) {
            c.drawLine(cx - size * 0.9f, cy - size * 0.55f, cx + size * 0.9f, cy - size * 0.55f, p);
            c.drawLine(cx - size * 0.65f, cy + size * 0.75f, cx + size * 0.65f, cy + size * 0.75f, p);
            c.drawLine(cx, cy - size * 0.55f, cx, cy + size * 0.75f, p);
            c.drawLine(cx - size * 0.55f, cy - size * 0.15f, cx + size * 0.55f, cy - size * 0.15f, p);
            c.drawLine(cx - size * 0.42f, cy + size * 0.25f, cx + size * 0.42f, cy + size * 0.25f, p);
            c.drawCircle(cx, cy - size * 0.85f, size * 0.18f, p);
        } else if (GameCore.PROF_ASTROLOGER.equals(profession)) {
            c.drawCircle(cx, cy, size * 0.95f, p);
            c.drawCircle(cx, cy, size * 0.36f, p);
            c.drawLine(cx - size * 0.95f, cy, cx + size * 0.95f, cy, p);
            c.drawLine(cx, cy - size * 0.95f, cx, cy + size * 0.95f, p);
            c.drawLine(cx - size * 0.65f, cy - size * 0.65f, cx + size * 0.65f, cy + size * 0.65f, p);
            c.drawCircle(cx + size * 0.62f, cy - size * 0.42f, size * 0.18f, p);
        } else if (GameCore.PROF_MACHINIST.equals(profession)) {
            c.drawCircle(cx, cy, size * 0.92f, p);
            c.drawCircle(cx, cy, size * 0.38f, p);
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8.0;
                float x1 = cx + (float) Math.cos(a) * size * 0.62f;
                float y1 = cy + (float) Math.sin(a) * size * 0.62f;
                float x2 = cx + (float) Math.cos(a) * size * 1.05f;
                float y2 = cy + (float) Math.sin(a) * size * 1.05f;
                c.drawLine(x1, y1, x2, y2, p);
            }
        } else if (GameCore.PROF_CHRONOMANCER.equals(profession)) {
            c.drawLine(cx - size * 0.65f, cy - size, cx + size * 0.65f, cy - size, p);
            c.drawLine(cx - size * 0.65f, cy + size, cx + size * 0.65f, cy + size, p);
            c.drawLine(cx - size * 0.65f, cy - size, cx + size * 0.65f, cy + size, p);
            c.drawLine(cx + size * 0.65f, cy - size, cx - size * 0.65f, cy + size, p);
            c.drawLine(cx - size * 0.32f, cy, cx + size * 0.32f, cy, p);
            c.drawCircle(cx, cy, size * 0.18f, p);
        } else if (GameCore.PROF_PACTMAKER.equals(profession)) {
            c.drawRect(cx - size * 0.62f, cy - size * 0.82f, cx + size * 0.62f, cy + size * 0.82f, p);
            c.drawLine(cx - size * 0.42f, cy - size * 0.35f, cx + size * 0.42f, cy - size * 0.35f, p);
            c.drawLine(cx - size * 0.42f, cy, cx + size * 0.42f, cy, p);
            c.drawLine(cx - size * 0.42f, cy + size * 0.35f, cx + size * 0.42f, cy + size * 0.35f, p);
            c.drawCircle(cx + size * 0.46f, cy + size * 0.46f, size * 0.25f, p);
            c.drawLine(cx + size * 0.28f, cy + size * 0.28f, cx + size * 0.62f, cy + size * 0.62f, p);
        } else if (GameCore.PROF_STORMCALLER.equals(profession)) {
            c.drawArc(new RectF(cx - size, cy - size, cx + size, cy + size), 215, 250, false, p);
            Path bolt = new Path();
            bolt.moveTo(cx + size * 0.18f, cy - size);
            bolt.lineTo(cx - size * 0.28f, cy - size * 0.08f);
            bolt.lineTo(cx + size * 0.08f, cy - size * 0.08f);
            bolt.lineTo(cx - size * 0.18f, cy + size);
            bolt.lineTo(cx + size * 0.52f, cy - size * 0.25f);
            bolt.lineTo(cx + size * 0.12f, cy - size * 0.25f);
            bolt.close();
            c.drawPath(bolt, p);
            c.drawLine(cx - size * 0.82f, cy + size * 0.52f, cx - size * 0.24f, cy + size * 0.52f, p);
            c.drawLine(cx + size * 0.34f, cy + size * 0.52f, cx + size * 0.82f, cy + size * 0.52f, p);
        } else if (GameCore.PROF_SHADOWDANCER.equals(profession)) {
            c.drawArc(new RectF(cx - size, cy - size, cx + size, cy + size), 35, 290, false, p);
            Path blade = new Path();
            blade.moveTo(cx - size * 0.18f, cy - size * 0.88f);
            blade.lineTo(cx + size * 0.38f, cy - size * 0.12f);
            blade.lineTo(cx + size * 0.1f, cy + size * 0.92f);
            blade.lineTo(cx - size * 0.28f, cy + size * 0.08f);
            blade.close();
            c.drawPath(blade, p);
            c.drawCircle(cx - size * 0.45f, cy - size * 0.1f, size * 0.22f, p);
            c.drawLine(cx - size * 0.72f, cy + size * 0.55f, cx + size * 0.48f, cy + size * 0.55f, p);
        } else if (GameCore.PROF_RUNEBLADE.equals(profession)) {
            Path blade = new Path();
            blade.moveTo(cx, cy - size);
            blade.lineTo(cx + size * 0.28f, cy - size * 0.08f);
            blade.lineTo(cx + size * 0.14f, cy + size * 0.95f);
            blade.lineTo(cx - size * 0.14f, cy + size * 0.95f);
            blade.lineTo(cx - size * 0.28f, cy - size * 0.08f);
            blade.close();
            c.drawPath(blade, p);
            c.drawLine(cx - size * 0.78f, cy + size * 0.58f, cx + size * 0.78f, cy - size * 0.58f, p);
            c.drawLine(cx - size * 0.78f, cy - size * 0.58f, cx + size * 0.78f, cy + size * 0.58f, p);
            Path rune = new Path();
            rune.moveTo(cx, cy - size * 0.46f);
            rune.lineTo(cx + size * 0.34f, cy);
            rune.lineTo(cx, cy + size * 0.46f);
            rune.lineTo(cx - size * 0.34f, cy);
            rune.close();
            c.drawPath(rune, p);
        } else if (GameCore.PROF_MEDIUM.equals(profession)) {
            c.drawCircle(cx, cy, size * 0.72f, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(2));
            c.drawCircle(cx, cy, size * 0.38f, p);
            c.drawArc(new RectF(cx - size * 0.92f, cy - size * 0.92f, cx + size * 0.92f, cy + size * 0.92f), 210, 120, false, p);
            c.drawArc(new RectF(cx - size * 0.92f, cy - size * 0.92f, cx + size * 0.92f, cy + size * 0.92f), 30, 120, false, p);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(cx - size * 0.72f, cy + size * 0.42f, size * 0.13f, p);
            c.drawCircle(cx + size * 0.72f, cy + size * 0.42f, size * 0.13f, p);
            Path planchette = new Path();
            planchette.moveTo(cx, cy - size * 0.42f);
            planchette.lineTo(cx + size * 0.34f, cy + size * 0.35f);
            planchette.lineTo(cx - size * 0.34f, cy + size * 0.35f);
            planchette.close();
            c.drawPath(planchette, p);
        } else if (GameCore.PROF_TACTICIAN.equals(profession)) {
            c.drawLine(cx - size * 0.78f, cy + size * 0.78f, cx - size * 0.78f, cy - size * 0.85f, p);
            Path banner = new Path();
            banner.moveTo(cx - size * 0.72f, cy - size * 0.82f);
            banner.lineTo(cx + size * 0.62f, cy - size * 0.62f);
            banner.lineTo(cx + size * 0.22f, cy - size * 0.1f);
            banner.lineTo(cx + size * 0.62f, cy + size * 0.32f);
            banner.lineTo(cx - size * 0.72f, cy + size * 0.16f);
            banner.close();
            c.drawPath(banner, p);
            c.drawLine(cx - size * 0.58f, cy + size * 0.58f, cx + size * 0.78f, cy + size * 0.58f, p);
            c.drawLine(cx - size * 0.38f, cy + size * 0.2f, cx + size * 0.38f, cy + size * 0.82f, p);
            c.drawLine(cx + size * 0.38f, cy + size * 0.2f, cx - size * 0.38f, cy + size * 0.82f, p);
            c.drawCircle(cx + size * 0.58f, cy + size * 0.58f, size * 0.16f, p);
        } else if (GameCore.PROF_PRISMIST.equals(profession)) {
            Path gem = new Path();
            gem.moveTo(cx, cy - size * 0.95f);
            gem.lineTo(cx + size * 0.72f, cy - size * 0.18f);
            gem.lineTo(cx + size * 0.36f, cy + size * 0.88f);
            gem.lineTo(cx - size * 0.36f, cy + size * 0.88f);
            gem.lineTo(cx - size * 0.72f, cy - size * 0.18f);
            gem.close();
            c.drawPath(gem, p);
            c.drawLine(cx, cy - size * 0.9f, cx, cy + size * 0.82f, p);
            c.drawLine(cx - size * 0.7f, cy - size * 0.18f, cx + size * 0.36f, cy + size * 0.82f, p);
            c.drawLine(cx + size * 0.7f, cy - size * 0.18f, cx - size * 0.36f, cy + size * 0.82f, p);
            c.drawLine(cx - size * 0.92f, cy + size * 0.16f, cx - size * 0.46f, cy + size * 0.52f, p);
            c.drawLine(cx + size * 0.92f, cy + size * 0.16f, cx + size * 0.46f, cy + size * 0.52f, p);
        } else if (GameCore.PROF_DREAMWALKER.equals(profession)) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(3));
            c.drawArc(new RectF(cx - size * 0.72f, cy - size * 0.92f, cx + size * 0.72f, cy + size * 0.92f), 105, 250, false, p);
            c.drawArc(new RectF(cx - size * 0.22f, cy - size * 0.78f, cx + size * 0.98f, cy + size * 0.58f), 118, 215, false, p);
            c.drawLine(cx - size * 0.78f, cy + size * 0.5f, cx + size * 0.55f, cy - size * 0.22f, p);
            c.drawLine(cx - size * 0.52f, cy + size * 0.78f, cx + size * 0.78f, cy + size * 0.18f, p);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(cx + size * 0.48f, cy - size * 0.48f, size * 0.13f, p);
            c.drawCircle(cx - size * 0.16f, cy + size * 0.22f, size * 0.1f, p);
        } else if (GameCore.PROF_GARDENER.equals(profession)) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(3));
            c.drawLine(cx, cy + size * 0.82f, cx, cy - size * 0.42f, p);
            c.drawLine(cx, cy + size * 0.34f, cx - size * 0.46f, cy + size * 0.72f, p);
            c.drawLine(cx, cy + size * 0.34f, cx + size * 0.46f, cy + size * 0.72f, p);
            c.drawLine(cx, cy + size * 0.46f, cx - size * 0.22f, cy + size * 0.88f, p);
            c.drawLine(cx, cy + size * 0.46f, cx + size * 0.22f, cy + size * 0.88f, p);
            p.setStyle(Paint.Style.FILL);
            Path leftLeaf = new Path();
            leftLeaf.moveTo(cx, cy - size * 0.28f);
            leftLeaf.cubicTo(cx - size * 0.78f, cy - size * 0.72f, cx - size * 0.96f, cy + size * 0.08f, cx - size * 0.18f, cy + size * 0.08f);
            leftLeaf.cubicTo(cx - size * 0.08f, cy - size * 0.02f, cx, cy - size * 0.16f, cx, cy - size * 0.28f);
            c.drawPath(leftLeaf, p);
            Path rightLeaf = new Path();
            rightLeaf.moveTo(cx + size * 0.04f, cy - size * 0.5f);
            rightLeaf.cubicTo(cx + size * 0.88f, cy - size * 0.9f, cx + size * 0.98f, cy - size * 0.02f, cx + size * 0.18f, cy - size * 0.06f);
            rightLeaf.cubicTo(cx + size * 0.1f, cy - size * 0.18f, cx + size * 0.04f, cy - size * 0.36f, cx + size * 0.04f, cy - size * 0.5f);
            c.drawPath(rightLeaf, p);
            c.drawCircle(cx, cy + size * 0.28f, size * 0.16f, p);
        } else if (GameCore.PROF_CHEF.equals(profession)) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(3));
            c.drawRoundRect(new RectF(cx - size * 0.72f, cy - size * 0.05f, cx + size * 0.72f, cy + size * 0.68f), size * 0.18f, size * 0.18f, p);
            c.drawLine(cx - size * 0.82f, cy - size * 0.12f, cx + size * 0.82f, cy - size * 0.12f, p);
            c.drawLine(cx - size * 0.48f, cy + size * 0.68f, cx + size * 0.48f, cy + size * 0.68f, p);
            c.drawArc(new RectF(cx - size * 1.08f, cy, cx - size * 0.52f, cy + size * 0.48f), 90, 160, false, p);
            c.drawArc(new RectF(cx + size * 0.52f, cy, cx + size * 1.08f, cy + size * 0.48f), -70, 160, false, p);
            c.drawArc(new RectF(cx - size * 0.56f, cy - size * 0.95f, cx - size * 0.1f, cy - size * 0.18f), 100, 120, false, p);
            c.drawArc(new RectF(cx - size * 0.1f, cy - size * 1.02f, cx + size * 0.38f, cy - size * 0.22f), 100, 120, false, p);
            c.drawLine(cx + size * 0.46f, cy - size * 0.8f, cx + size * 0.86f, cy - size * 0.38f, p);
            c.drawLine(cx + size * 0.86f, cy - size * 0.38f, cx + size * 0.46f, cy + size * 0.1f, p);
            p.setStyle(Paint.Style.FILL);
            Path flame = new Path();
            flame.moveTo(cx, cy + size * 0.5f);
            flame.cubicTo(cx - size * 0.35f, cy + size * 0.18f, cx - size * 0.12f, cy - size * 0.1f, cx, cy - size * 0.36f);
            flame.cubicTo(cx + size * 0.38f, cy - size * 0.02f, cx + size * 0.25f, cy + size * 0.3f, cx, cy + size * 0.5f);
            c.drawPath(flame, p);
        } else if (GameCore.PROF_BARD.equals(profession)) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(3));
            c.drawArc(new RectF(cx - size * 0.78f, cy - size * 0.92f, cx + size * 0.78f, cy + size * 0.72f), 210, 250, false, p);
            c.drawLine(cx + size * 0.18f, cy - size * 0.92f, cx + size * 0.18f, cy + size * 0.54f, p);
            c.drawLine(cx + size * 0.18f, cy - size * 0.92f, cx + size * 0.62f, cy - size * 0.72f, p);
            c.drawLine(cx - size * 0.46f, cy - size * 0.26f, cx + size * 0.18f, cy - size * 0.08f, p);
            c.drawLine(cx - size * 0.34f, cy + size * 0.18f, cx + size * 0.18f, cy + size * 0.34f, p);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(cx - size * 0.58f, cy + size * 0.42f, size * 0.22f, p);
            c.drawCircle(cx + size * 0.18f, cy + size * 0.66f, size * 0.22f, p);
        } else if (GameCore.PROF_MIRRORIST.equals(profession)) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(3));
            Path mirror = new Path();
            mirror.moveTo(cx, cy - size * 0.96f);
            mirror.lineTo(cx + size * 0.72f, cy - size * 0.34f);
            mirror.lineTo(cx + size * 0.48f, cy + size * 0.82f);
            mirror.lineTo(cx - size * 0.48f, cy + size * 0.82f);
            mirror.lineTo(cx - size * 0.72f, cy - size * 0.34f);
            mirror.close();
            c.drawPath(mirror, p);
            c.drawOval(new RectF(cx - size * 0.42f, cy - size * 0.58f, cx + size * 0.42f, cy + size * 0.48f), p);
            c.drawLine(cx - size * 0.58f, cy - size * 0.2f, cx + size * 0.42f, cy - size * 0.62f, p);
            c.drawLine(cx - size * 0.34f, cy + size * 0.48f, cx + size * 0.56f, cy + size * 0.1f, p);
            c.drawLine(cx, cy - size * 0.96f, cx, cy + size * 0.82f, p);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(cx + size * 0.58f, cy - size * 0.52f, size * 0.12f, p);
            c.drawCircle(cx - size * 0.58f, cy + size * 0.54f, size * 0.1f, p);
        } else if (GameCore.PROF_PUPPETEER.equals(profession)) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(3));
            c.drawLine(cx - size * 0.62f, cy - size * 0.92f, cx - size * 0.18f, cy - size * 0.2f, p);
            c.drawLine(cx + size * 0.62f, cy - size * 0.92f, cx + size * 0.18f, cy - size * 0.2f, p);
            c.drawLine(cx, cy - size * 0.98f, cx, cy - size * 0.08f, p);
            c.drawOval(new RectF(cx - size * 0.38f, cy - size * 0.34f, cx + size * 0.38f, cy + size * 0.36f), p);
            c.drawLine(cx - size * 0.3f, cy + size * 0.2f, cx - size * 0.62f, cy + size * 0.76f, p);
            c.drawLine(cx + size * 0.3f, cy + size * 0.2f, cx + size * 0.62f, cy + size * 0.76f, p);
            c.drawLine(cx - size * 0.56f, cy - size * 0.02f, cx + size * 0.56f, cy - size * 0.02f, p);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(cx - size * 0.62f, cy - size * 0.92f, size * 0.11f, p);
            c.drawCircle(cx, cy - size * 0.98f, size * 0.11f, p);
            c.drawCircle(cx + size * 0.62f, cy - size * 0.92f, size * 0.11f, p);
        } else {
            c.drawArc(new RectF(cx - size, cy - size, cx + size, cy + size), 210, 300, false, p);
            c.drawLine(cx - size * 0.7f, cy - size * 0.15f, cx + size * 0.7f, cy - size * 0.15f, p);
            c.drawLine(cx - size * 0.7f, cy + size * 0.25f, cx + size * 0.7f, cy + size * 0.25f, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    private void drawRelicRow(Canvas c, GameCore.RelicDef r, float x, float y, float width) {
        if (r == null) return;
        p.setColor(0xaa111923);
        c.drawRoundRect(new RectF(x, y, x + width, y + dp(56)), dp(6), dp(6), p);
        p.setColor(0xffd0b66e);
        c.drawCircle(x + dp(22), y + dp(28), dp(14), p);
        float textWidth = Math.max(dp(120), width - dp(154));
        drawText(c, fitText(r.name, 15, textWidth), x + dp(44), y + dp(17), 15, 0xfff2ead7, true);
        String hint = GameCore.relicSynergyHint(s, r.id);
        drawText(c, fitText(hint, 10, textWidth), x + dp(44), y + dp(33), 10, 0xff9fd5c5, true);
        drawText(c, fitText(r.text, 10, textWidth), x + dp(44), y + dp(49), 10, 0xffb8c6c5, false);
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

    private void drawWrappedLines(Canvas c, String body, float x, float y, float width, float size, int color, int maxLines) {
        if (body == null || maxLines <= 0) return;
        text.setTextSize(size);
        text.setColor(color);
        text.setFakeBoldText(false);
        String line = "";
        float yy = y;
        int lines = 0;
        for (int i = 0; i < body.length(); i++) {
            String next = line + body.charAt(i);
            if (text.measureText(next) > width && line.length() > 0) {
                boolean last = lines + 1 >= maxLines;
                c.drawText(last ? fitText(line + body.substring(i), size, width) : line, x, yy, text);
                lines++;
                if (last) {
                    return;
                }
                yy += size * 1.22f;
                line = String.valueOf(body.charAt(i));
            } else {
                line = next;
            }
        }
        if (line.length() > 0 && lines < maxLines) {
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
        if ("event_transform_bonus".equals(s.pendingAction)) return "选择重塑牌：变为升级随机牌";
        if ("rest_transform".equals(s.pendingAction) || "shop_transform".equals(s.pendingAction)) return "选择要转化的牌";
        return "牌组浏览";
    }

    private String rarity(int r) {
        return r == 2 ? "稀有" : r == 1 ? "进阶" : "普通";
    }

    private String questProgressText() {
        if (s.combatQuest == GameCore.QUEST_SWIFT) return "回合 " + s.turn + "/" + s.questTarget;
        if (s.combatQuest == GameCore.QUEST_UNHURT) return "受伤 " + s.questProgress + "/" + s.questTarget;
        if (s.combatQuest == GameCore.QUEST_COMBO) return "连携 " + s.questProgress + "/" + s.questTarget;
        if (s.combatQuest == GameCore.QUEST_GUARD) return "格挡 " + s.questProgress + "/" + s.questTarget;
        if (s.combatQuest == GameCore.QUEST_HEX) return "异常 " + s.questProgress + "/" + s.questTarget;
        if (s.combatQuest == GameCore.QUEST_LEAN) return "出牌 " + s.totalCardsPlayed + "/" + s.questTarget;
        if (s.combatQuest == GameCore.QUEST_BREW) return "炼调 " + s.questProgress + "/" + s.questTarget;
        if (s.combatQuest == GameCore.QUEST_SKILL) return "权能 " + s.questProgress + "/" + s.questTarget;
        if (s.combatQuest == GameCore.QUEST_ECHO) return "回声 " + s.questProgress + "/" + s.questTarget;
        if (s.combatQuest == GameCore.QUEST_BLOODCOIN) return "血币 " + s.questProgress + "/" + s.questTarget;
        if (s.combatQuest == GameCore.QUEST_FORGE) return "工坊 " + s.questProgress + "/" + s.questTarget;
        if (s.combatQuest == GameCore.QUEST_TREASURE) return "寻宝 " + s.questProgress + "/" + s.questTarget;
        if (s.combatQuest == GameCore.QUEST_CONFLUENCE) return "汇流 " + s.questProgress + "/" + s.questTarget;
        if (s.combatQuest == GameCore.QUEST_MARK) return "标记 " + s.questProgress + "/" + s.questTarget;
        if (s.combatQuest == GameCore.QUEST_OVERLOAD) return "过载 " + s.questProgress + "/" + s.questTarget;
        return "";
    }

    private String shortText(String value, int max) {
        if (value == null || value.length() <= max) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(0, max));
    }

    private String fitText(String value, float size, float width) {
        if (value == null || value.length() == 0) {
            return "";
        }
        text.setTextSize(size);
        if (text.measureText(value) <= width) {
            return value;
        }
        String result = value;
        while (result.length() > 0 && text.measureText(result + "...") > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result.length() == 0 ? "" : result + "...";
    }

    private String eventTitle() {
        String[] t = {
                "黑水圣龛", "裂币喷泉", "会呼吸的书页", "无名旅人",
                "石刻囚笼", "炼金雨棚", "无灯影市", "旧王雾门",
                "职业导师", "深渊账本", "药剂实验台", "镜面牌桌",
                "过载训练场", "裂隙交换会", "工坊合约", "汇流棱庭",
                "专修讲堂"
        };
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
                "雾门后传来旧王的低语。你能提前拿走王座旁的遗物，但门会取走一部分生命。",
                "一位认得你职业徽记的导师坐在路边。他只教一次，要么给招式，要么给批注。",
                "账本摊开，数字像活物一样爬动。你可以借深渊的钱，也可以赎回一部分名字。",
                "实验台上排着无标签药剂。你能装满腰带，也能把所有药剂熔成一张稳定牌。",
                "牌桌镜面映出另一副牌组。它愿意复制一张强牌，也愿意重塑你的旧牌。",
                "训练场的刻线专为职业技而生。导师要求你把满溢的力量压进牌面。",
                "交换会的摊位来自不同派系与职业。越不像你的东西，越可能打开新路线。",
                "工坊合约写着两种报酬：稳定锻造，或带着伤口拿走一张能力牌。",
                "棱庭里漂浮着不同构筑的碎片。它们可以拼成一张新核心，也能被压成一件危险遗物。",
                "讲堂里没有老师，只有一圈会自动批改牌面的黑板。它们认识你的职业技专修，也索取代价。"
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
        if (s.eventId == 7) return new String[]{"最大生命-8，获得Boss遗物", "升级两张牌，加入1张眩光"};
        if (s.eventId == 8) return new String[]{"获得升级职业牌", "获得35金币并升级1张牌"};
        if (s.eventId == 9) return new String[]{"借款：获得金币，加入1张眩光", "支付55金币，最大生命+7并升级"};
        if (s.eventId == 10) return new String[]{"装满药剂，加入1张裂伤", "清空药剂，获得升级随机牌"};
        if (s.eventId == 11) return new String[]{"复制一张升级随机牌，加入1张眩光", "重塑一张牌为升级随机牌，获得25金币"};
        if (s.eventId == 12) return new String[]{"支付35金币，获得升级过载职业牌", "获得职业技遗物，加入1张眩光"};
        if (s.eventId == 13) return new String[]{"最大生命-4，获得升级跨池牌", "获得裂隙罗盘，加入1张眩光"};
        if (s.eventId == 14) return new String[]{"升级两张牌，获得过载职业牌", "获得45金币和升级能力牌，加入1张裂伤"};
        if (s.eventId == 15) return new String[]{"获得升级混搭核心牌并升级1张牌", "获得混搭遗物，加入眩光和裂伤"};
        return new String[]{"失去生命，晋阶专修并获得升级适配牌", "获得专修遗物和适配牌，加入状态牌"};
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
