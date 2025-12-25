package client;

import common.Formulas;
import common.SocketManager;
import database.Database;
import fight.spells.Effect;
import fight.spells.SpellGrade;
import game.GameServer;
import game.world.World;
import kernel.Constant;

import java.util.HashMap;
import java.util.Map;

public class SpellBook {
    private final Player player;
    int _spellPts;
    Map<Integer, SpellGrade> _sorts = new HashMap<Integer, SpellGrade>();
    Map<Integer, Character> _sortsPlaces = new HashMap<Integer, Character>();
    Map<Integer, HashMap<Integer, Integer>> objectsClassSpell = new HashMap<Integer, HashMap<Integer, Integer>>();
    Map<Integer, SpellGrade> _saveSorts = new HashMap<Integer, SpellGrade>();
    Map<Integer, Character> _saveSortsPlaces = new HashMap<Integer, Character>();
    int _saveSpellPts;
    Map<Integer, Effect> buffs = new HashMap<Integer, Effect>();

    public SpellBook(Player player) {
        this.player = player;
    }

    public String parseSpellToDB() {
        StringBuilder sorts = new StringBuilder();

        if (player._morphMode) {
            if (_saveSorts.isEmpty())
                return "";
            for (int key : _saveSorts.keySet()) {
                //3;1;a,4;3;b
                SpellGrade SS = _saveSorts.get(key);
                if (SS == null)
                    continue;
                sorts.append(SS.getSpellID()).append(";").append(SS.getLevel()).append(";");
                if (_saveSortsPlaces.get(key) != null)
                    sorts.append(_saveSortsPlaces.get(key));
                else
                    sorts.append("_");
                sorts.append(",");
            }
        } else {
            if (_sorts.isEmpty())
                return "";
            for (int key : _sorts.keySet()) {
                //3;1;a,4;3;b
                SpellGrade SS = _sorts.get(key);
                if (SS == null)
                    continue;
                sorts.append(SS.getSpellID()).append(";").append(SS.getLevel()).append(";");
                if (_sortsPlaces.get(key) != null)
                    sorts.append(_sortsPlaces.get(key));
                else
                    sorts.append("_");
                sorts.append(",");
            }
        }
        return sorts.substring(0, sorts.length() - 1);
    }

    void parseSpells(String str) {
        if (!str.equalsIgnoreCase("")) {
            if (player._morphMode) {
                String[] spells = str.split(",");
                _saveSorts.clear();
                _saveSortsPlaces.clear();
                for (String e : spells) {
                    try {
                        int id = Integer.parseInt(e.split(";")[0]);
                        int lvl = Integer.parseInt(e.split(";")[1]);
                        char place = e.split(";")[2].charAt(0);
                        learnSpell(id, lvl);
                        this._saveSortsPlaces.put(id, place);
                    } catch (NumberFormatException e1) {
                        e1.printStackTrace();
                    }
                }
            } else {
                String[] spells = str.split(",");
                _sorts.clear();
                _sortsPlaces.clear();
                for (String e : spells) {
                    try {
                        int id = Integer.parseInt(e.split(";")[0]);
                        int lvl = Integer.parseInt(e.split(";")[1]);
                        char place = e.split(";")[2].charAt(0);
                        if (!player._morphMode)
                            learnSpell(id, lvl, false, false, false);
                        else
                            learnSpell(id, lvl, false, true, false);
                        _sortsPlaces.put(id, place);
                    } catch (NumberFormatException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    public Map<Integer, Effect> get_buff() {
        return buffs;
    }

    public int get_spellPts() {
        if (player._morphMode)
            return _saveSpellPts;
        else
            return _spellPts;
    }

    public void set_spellPts(int pts) {
        if (player._morphMode)
            _saveSpellPts = pts;
        else
            _spellPts = pts;
    }

    public void setSpellsPlace(boolean ok) {
        if (ok)
            _sortsPlaces = Constant.getStartSortsPlaces(player.getClasseID());
        else
            _sortsPlaces.clear();
        SocketManager.GAME_SEND_SPELL_LIST(player);
    }

    /**
     * @return next free sort place, or '\0' if none is available
     */

    public char getNextFreeSortPlace() {
        for (char c : Constant.SPELL_PLACES) {

            // Avoid CAC place
            if (c == 'a') {
                continue;
            }

            if (!_sortsPlaces.containsValue(c)) {
                return c;
            }
        }
        return '\0';
    }

    public void learnSpell(int spell, int level, char pos) {
        if (World.world.getSort(spell).getStatsByLevel(level) == null) {
            GameServer.a("LearnSpell " + spell + " level " + level);
            return;
        }

        if (!_sorts.containsKey(spell)) {
            _sorts.put(spell, World.world.getSort(spell).getStatsByLevel(level));
            replace_SpellInBook(pos);
            _sortsPlaces.remove(spell);
            _sortsPlaces.put(spell, pos);
            SocketManager.GAME_SEND_SPELL_LIST(player);
            SocketManager.GAME_SEND_Im_PACKET(player, "03;" + spell);
        }
    }

    public boolean learnSpell(int spellID, int level, boolean save,
                              boolean send, boolean learn) {

        if (World.world.getSort(spellID).getStatsByLevel(level) == null) {
            GameServer.a("Learn Spell " + spellID + " level " + level + "/ Pas définie");
            return false;
        }

        if (_sorts.containsKey(Integer.valueOf(spellID)) && learn) {
            SocketManager.GAME_SEND_MESSAGE(player, "Tu posséde déjà ce sort.");
            return false;
        } else {
            _sorts.put(Integer.valueOf(spellID), World.world.getSort(spellID).getStatsByLevel(level));
            if (send) {
                SocketManager.GAME_SEND_SPELL_LIST(player);
                SocketManager.GAME_SEND_Im_PACKET(player, "03;" + spellID);
            }
            if (save)
                Database.getStatics().getPlayerData().update(player);
            return true;
        }
    }

    public boolean learnSpell(int spellID, int level) {
        if (World.world.getSort(spellID).getStatsByLevel(level) == null) {
            GameServer.a("Learn Spell " + spellID + " level " + level + "/ Pas définie");
            return false;
        }

        if (_saveSorts.containsKey(Integer.valueOf(spellID))) {
            return false;
        } else {
            _saveSorts.put(Integer.valueOf(spellID), World.world.getSort(spellID).getStatsByLevel(level));
            return true;
        }
    }

    public boolean unlearnSpell(int spell) {
        if (World.world.getSort(spell) == null) {
            GameServer.a("Learn Spell " + spell + "/ Pas définie");
            return false;
        }

        _sorts.remove(spell);
        this._sortsPlaces.remove(spell);
        SocketManager.GAME_SEND_SPELL_LIST(player);
        SocketManager.GAME_SEND_STATS_PACKET(player);
        Database.getStatics().getPlayerData().update(player);
        return true;
    }

    public boolean unlearnSpell(Player perso, int spellID, int level,
                                int ancLevel, boolean save, boolean send) {
        int spellPoint = 1;
        if (ancLevel == 2)
            spellPoint = 1;
        if (ancLevel == 3)
            spellPoint = 2 + 1;
        if (ancLevel == 4)
            spellPoint = 3 + 3;
        if (ancLevel == 5)
            spellPoint = 4 + 6;
        if (ancLevel == 6)
            spellPoint = 5 + 10;

        if (World.world.getSort(spellID).getStatsByLevel(level) == null) {
            GameServer.a("Learn Spell " + spellID + " level " + level + "/ Pas définie");
            return false;
        }

        _sorts.put(Integer.valueOf(spellID), World.world.getSort(spellID).getStatsByLevel(level));
        if (send) {
            SocketManager.GAME_SEND_SPELL_LIST(player);
            SocketManager.GAME_SEND_Im_PACKET(player, "0154;" + "<b>" + ancLevel
                    + "</b>" + "~" + "<b>" + spellPoint + "</b>");
            addSpellPoint(spellPoint);
            SocketManager.GAME_SEND_STATS_PACKET(perso);
        }
        if (save)
            Database.getStatics().getPlayerData().update(player);
        return true;
    }

    public boolean boostSpell(int spellID) {
        if (getSortStatBySortIfHas(spellID) == null)
            return false;
        int AncLevel = getSortStatBySortIfHas(spellID).getLevel();
        if (AncLevel == 6)
            return false;
        if (_spellPts >= AncLevel && World.world.getSort(spellID).getStatsByLevel(AncLevel + 1).getReqLevel() <= player.getLevel()) {
            if (learnSpell(spellID, AncLevel + 1, true, false, false)) {
                _spellPts -= AncLevel;
                Database.getStatics().getPlayerData().update(player);
                return true;
            } else {
                return false;
            }
        } else
        //Pas le niveau ou pas les Points
        {
            if (_spellPts < AncLevel)
                if (World.world.getSort(spellID).getStatsByLevel(AncLevel + 1).getReqLevel() > player.getLevel())
                    return false;
        }
        return player.isAway();
    }

    public void boostSpellIncarnation() {
        for (Map.Entry<Integer, SpellGrade> i : _sorts.entrySet()) {
            if (getSortStatBySortIfHas(i.getValue().getSpell().getSpellID()) == null)
                continue;
            if (learnSpell(i.getValue().getSpell().getSpellID(), i.getValue().getLevel() + 1, true, false, false))
                Database.getStatics().getPlayerData().update(player);
        }
    }

    public boolean forgetSpell(int spellID) {
        if (getSortStatBySortIfHas(spellID) == null) {
            return false;
        }
        int AncLevel = getSortStatBySortIfHas(spellID).getLevel();
        if (AncLevel <= 1)
            return false;

        if (learnSpell(spellID, 1, true, false, false)) {
            _spellPts += Formulas.spellCost(AncLevel);
            Database.getStatics().getPlayerData().update(player);
            return true;
        } else {
            return false;
        }
    }

    public void set_SpellPlace(int SpellID, char Place) {
        replace_SpellInBook(Place);
        _sortsPlaces.remove(SpellID);
        _sortsPlaces.put(SpellID, Place);
        Database.getStatics().getPlayerData().update(player);
    }

    void replace_SpellInBook(char Place) {
        for (int key : _sorts.keySet())
            if (_sortsPlaces.get(key) != null)
                if (_sortsPlaces.get(key).equals(Place))
                    _sortsPlaces.remove(key);
    }

    public SpellGrade getSortStatBySortIfHas(int spellID) {
        return _sorts.get(spellID);
    }

    public void checkAndLearnSpell() {
        if (player.classe.GetSorts().containsKey(player.getLevel())) {
            char c = getNextFreeSortPlace();
            learnSpell(player.classe.GetSorts().get(player.getLevel()), 1, c);
        }
    }

    public void checkAndLearnSpell(int level) {
        if (player.classe.GetSorts().containsKey(level)) {
            char c = getNextFreeSortPlace();
            learnSpell(player.classe.GetSorts().get(level), 1, c);
        }
    }

    public boolean NerfSpell(int spellID) {
        if (player.getFight() != null)
            return false;
        int antNivel = getSortStatBySortIfHas(spellID).getLevel();
        if (antNivel <= 1)
            return false;
        if (learnSpell(spellID, (antNivel - 1), true, false, false)) {
            int total = 0;
            for (int i = (antNivel - 1); i < antNivel; i++)
                total += i;
            _spellPts += total;
            Database.getStatics().getPlayerData().update(player);
            SocketManager.GAME_SEND_STATS_PACKET(player);
            SocketManager.GAME_SEND_SPELL_LIST(player);
            return true;
        }
        return false;
    }

    public void addSpellPoint(int pts) {
        if (player._morphMode)
            _saveSpellPts += pts;
        else
            _spellPts += pts;
    }

    public boolean hasSpell(int spellID) {
        return (getSortStatBySortIfHas(spellID) != null);
    }
}