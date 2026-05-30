package comunio.nas.objects.community;

import org.json.JSONObject;

public class GroupSettings {

    private boolean isPrivate;
    private boolean secondHighestOffers;
    private boolean tradableChange;
    private int salesBanProOffers;
    private String creditFactor;
    private String description;
    private String language;
    private boolean creditFactorDisabled;
    private int maxDaysOffersArePending;
    private int buyoutClauseTradeLock;
    private boolean salaries;
    private String newMembers;
    private int members;
    private int salesBan;
    private String playersMemberPerClub;
    private boolean locked;
    private boolean buyoutClause;
    private String bonusPrediction;
    private String maxTradablesPerUser;
    private int injuredTradableOfferFactor;
    private int playersTradablesOnExchangemarket;
    private int buyoutClauseFactor;
    private String nextSeason;
    private int maxDaysOffersArePendingUsers;
    private boolean publicTransactionValues;
    private int tradablesOnExchangemarket;

    // --- Getter und Setter für alle Felder ---
    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }

    public boolean isSecondHighestOffers() { return secondHighestOffers; }
    public void setSecondHighestOffers(boolean secondHighestOffers) { this.secondHighestOffers = secondHighestOffers; }

    public boolean isTradableChange() { return tradableChange; }
    public void setTradableChange(boolean tradableChange) { this.tradableChange = tradableChange; }

    public int getSalesBanProOffers() { return salesBanProOffers; }
    public void setSalesBanProOffers(int salesBanProOffers) { this.salesBanProOffers = salesBanProOffers; }

    public String getCreditFactor() { return creditFactor; }
    public void setCreditFactor(String creditFactor) { this.creditFactor = creditFactor; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public boolean isCreditFactorDisabled() { return creditFactorDisabled; }
    public void setCreditFactorDisabled(boolean creditFactorDisabled) { this.creditFactorDisabled = creditFactorDisabled; }

    public int getMaxDaysOffersArePending() { return maxDaysOffersArePending; }
    public void setMaxDaysOffersArePending(int maxDaysOffersArePending) { this.maxDaysOffersArePending = maxDaysOffersArePending; }

    public int getBuyoutClauseTradeLock() { return buyoutClauseTradeLock; }
    public void setBuyoutClauseTradeLock(int buyoutClauseTradeLock) { this.buyoutClauseTradeLock = buyoutClauseTradeLock; }

    public boolean isSalaries() { return salaries; }
    public void setSalaries(boolean salaries) { this.salaries = salaries; }

    public String getNewMembers() { return newMembers; }
    public void setNewMembers(String newMembers) { this.newMembers = newMembers; }

    public int getMembers() { return members; }
    public void setMembers(int members) { this.members = members; }

    public int getSalesBan() { return salesBan; }
    public void setSalesBan(int salesBan) { this.salesBan = salesBan; }

    public String getPlayersMemberPerClub() { return playersMemberPerClub; }
    public void setPlayersMemberPerClub(String playersMemberPerClub) { this.playersMemberPerClub = playersMemberPerClub; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public boolean isBuyoutClause() { return buyoutClause; }
    public void setBuyoutClause(boolean buyoutClause) { this.buyoutClause = buyoutClause; }

    public String getBonusPrediction() { return bonusPrediction; }
    public void setBonusPrediction(String bonusPrediction) { this.bonusPrediction = bonusPrediction; }

    public String getMaxTradablesPerUser() { return maxTradablesPerUser; }
    public void setMaxTradablesPerUser(String maxTradablesPerUser) { this.maxTradablesPerUser = maxTradablesPerUser; }

    public int getInjuredTradableOfferFactor() { return injuredTradableOfferFactor; }
    public void setInjuredTradableOfferFactor(int injuredTradableOfferFactor) { this.injuredTradableOfferFactor = injuredTradableOfferFactor; }

    public int getPlayersTradablesOnExchangemarket() { return playersTradablesOnExchangemarket; }
    public void setPlayersTradablesOnExchangemarket(int playersTradablesOnExchangemarket) { this.playersTradablesOnExchangemarket = playersTradablesOnExchangemarket; }

    public int getBuyoutClauseFactor() { return buyoutClauseFactor; }
    public void setBuyoutClauseFactor(int buyoutClauseFactor) { this.buyoutClauseFactor = buyoutClauseFactor; }

    public String getNextSeason() { return nextSeason; }
    public void setNextSeason(String nextSeason) { this.nextSeason = nextSeason; }

    public int getMaxDaysOffersArePendingUsers() { return maxDaysOffersArePendingUsers; }
    public void setMaxDaysOffersArePendingUsers(int maxDaysOffersArePendingUsers) { this.maxDaysOffersArePendingUsers = maxDaysOffersArePendingUsers; }

    public boolean isPublicTransactionValues() { return publicTransactionValues; }
    public void setPublicTransactionValues(boolean publicTransactionValues) { this.publicTransactionValues = publicTransactionValues; }

    public int getTradablesOnExchangemarket() { return tradablesOnExchangemarket; }
    public void setTradablesOnExchangemarket(int tradablesOnExchangemarket) { this.tradablesOnExchangemarket = tradablesOnExchangemarket; }

    /**
     * Serialisiert die Instanz als JSONObject.
     */
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("private", isPrivate);
        obj.put("second_highest_offers", secondHighestOffers);
        obj.put("tradablechange", tradableChange);
        obj.put("sales_ban_pro_offers", salesBanProOffers);
        obj.put("creditfactor", creditFactor);
        obj.put("description", description);
        obj.put("language", language);
        obj.put("creditFactorDisabled", creditFactorDisabled);
        obj.put("max_days_offers_are_pending", maxDaysOffersArePending);
        obj.put("buyout_clause_trade_lock", buyoutClauseTradeLock);
        obj.put("salaries", salaries);
        obj.put("new_members", newMembers);
        obj.put("members", members);
        obj.put("sales_ban", salesBan);
        obj.put("players_member_per_club", playersMemberPerClub);
        obj.put("locked", locked);
        obj.put("buyout_clause", buyoutClause);
        obj.put("bonus_prediction", bonusPrediction);
        obj.put("max_tradables_per_user", maxTradablesPerUser);
        obj.put("injured_tradable_offer_factor", injuredTradableOfferFactor);
        obj.put("players_tradables_on_exchangemarket", playersTradablesOnExchangemarket);
        obj.put("buyout_clause_factor", buyoutClauseFactor);
        obj.put("next_season", nextSeason);
        obj.put("max_days_offers_are_pending_users", maxDaysOffersArePendingUsers);
        obj.put("public_transaction_values", publicTransactionValues);
        obj.put("tradables_on_exchangemarket", tradablesOnExchangemarket);
        return obj;
    }
    
    public void updateFromJson(JSONObject obj) {
        this.setPrivate(obj.optBoolean("private", false));
        this.setSecondHighestOffers(obj.optBoolean("second_highest_offers", false));
        this.setTradableChange(obj.optBoolean("tradablechange", false));
        this.setSalesBanProOffers(obj.optInt("sales_ban_pro_offers", 0));
        this.setCreditFactor(obj.optString("creditfactor", ""));
        this.setDescription(obj.optString("description", ""));
        this.setLanguage(obj.optString("language", ""));
        this.setCreditFactorDisabled(obj.optBoolean("creditFactorDisabled", false));
        this.setMaxDaysOffersArePending(obj.optInt("max_days_offers_are_pending", 0));
        this.setBuyoutClauseTradeLock(obj.optInt("buyout_clause_trade_lock", 0));
        this.setSalaries(obj.optBoolean("salaries", false));
        this.setNewMembers(obj.optString("new_members", ""));
        this.setMembers(obj.optInt("members", 0));
        this.setSalesBan(obj.optInt("sales_ban", 0));
        this.setPlayersMemberPerClub(obj.optString("players_member_per_club", ""));
        this.setLocked(obj.optBoolean("locked", false));
        this.setBuyoutClause(obj.optBoolean("buyout_clause", false));
        this.setBonusPrediction(obj.optString("bonus_prediction", ""));
        this.setMaxTradablesPerUser(obj.optString("max_tradables_per_user", ""));
        this.setInjuredTradableOfferFactor(obj.optInt("injured_tradable_offer_factor", 0));
        this.setPlayersTradablesOnExchangemarket(obj.optInt("players_tradables_on_exchangemarket", 0));
        this.setBuyoutClauseFactor(obj.optInt("buyout_clause_factor", 0));
        this.setNextSeason(obj.optString("next_season", ""));
        this.setMaxDaysOffersArePendingUsers(obj.optInt("max_days_offers_are_pending_users", 0));
        this.setPublicTransactionValues(obj.optBoolean("public_transaction_values", false));
        this.setTradablesOnExchangemarket(obj.optInt("tradables_on_exchangemarket", 0));
    }

    /**
     * Erstellt eine Instanz dieser Klasse aus einem JSONObject.
     */
    public static GroupSettings fromJson(JSONObject obj) {
        GroupSettings gs = new GroupSettings();
		gs.updateFromJson(obj);
        return gs;
    }
}

